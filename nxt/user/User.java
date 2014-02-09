/*   1:    */ package nxt.user;
/*   2:    */ 
/*   3:    */ import java.io.IOException;
/*   4:    */ import java.io.PrintWriter;
/*   5:    */ import java.io.Writer;
/*   6:    */ import java.math.BigInteger;
/*   7:    */ import java.security.MessageDigest;
/*   8:    */ import java.util.Arrays;
/*   9:    */ import java.util.Collection;
/*  10:    */ import java.util.Collections;
/*  11:    */ import java.util.concurrent.ConcurrentHashMap;
/*  12:    */ import java.util.concurrent.ConcurrentLinkedQueue;
/*  13:    */ import java.util.concurrent.ConcurrentMap;
/*  14:    */ import javax.servlet.AsyncContext;
/*  15:    */ import javax.servlet.AsyncEvent;
/*  16:    */ import javax.servlet.AsyncListener;
/*  17:    */ import javax.servlet.ServletException;
/*  18:    */ import javax.servlet.ServletResponse;
/*  19:    */ import javax.servlet.http.HttpServletRequest;
/*  20:    */ import javax.servlet.http.HttpServletResponse;
/*  21:    */ import nxt.Account;
/*  22:    */ import nxt.crypto.Crypto;
/*  23:    */ import nxt.util.JSON;
/*  24:    */ import nxt.util.Logger;
/*  25:    */ import org.json.simple.JSONArray;
/*  26:    */ import org.json.simple.JSONObject;
/*  27:    */ import org.json.simple.JSONStreamAware;
/*  28:    */ 
/*  29:    */ public final class User
/*  30:    */ {
/*  31: 29 */   private static final ConcurrentMap<String, User> users = new ConcurrentHashMap();
/*  32: 30 */   private static final Collection<User> allUsers = Collections.unmodifiableCollection(users.values());
/*  33:    */   private volatile String secretPhrase;
/*  34:    */   private volatile byte[] publicKey;
/*  35:    */   private volatile boolean isInactive;
/*  36:    */   
/*  37:    */   public static Collection<User> getAllUsers()
/*  38:    */   {
/*  39: 33 */     return allUsers;
/*  40:    */   }
/*  41:    */   
/*  42:    */   public static User getUser(String paramString)
/*  43:    */   {
/*  44: 37 */     Object localObject = (User)users.get(paramString);
/*  45: 38 */     if (localObject == null)
/*  46:    */     {
/*  47: 39 */       localObject = new User();
/*  48: 40 */       User localUser = (User)users.putIfAbsent(paramString, localObject);
/*  49: 41 */       if (localUser != null)
/*  50:    */       {
/*  51: 42 */         localObject = localUser;
/*  52: 43 */         ((User)localObject).isInactive = false;
/*  53:    */       }
/*  54:    */     }
/*  55:    */     else
/*  56:    */     {
/*  57: 46 */       ((User)localObject).isInactive = false;
/*  58:    */     }
/*  59: 48 */     return localObject;
/*  60:    */   }
/*  61:    */   
/*  62:    */   public static void sendToAll(JSONStreamAware paramJSONStreamAware)
/*  63:    */   {
/*  64: 52 */     for (User localUser : users.values()) {
/*  65: 53 */       localUser.send(paramJSONStreamAware);
/*  66:    */     }
/*  67:    */   }
/*  68:    */   
/*  69:    */   public static void updateUserUnconfirmedBalance(Account paramAccount)
/*  70:    */   {
/*  71: 58 */     JSONObject localJSONObject = new JSONObject();
/*  72: 59 */     localJSONObject.put("response", "setBalance");
/*  73: 60 */     localJSONObject.put("balance", Long.valueOf(paramAccount.getUnconfirmedBalance()));
/*  74: 61 */     byte[] arrayOfByte = paramAccount.getPublicKey();
/*  75: 62 */     for (User localUser : users.values()) {
/*  76: 63 */       if ((localUser.secretPhrase != null) && (Arrays.equals(localUser.publicKey, arrayOfByte))) {
/*  77: 64 */         localUser.send(localJSONObject);
/*  78:    */       }
/*  79:    */     }
/*  80:    */   }
/*  81:    */   
/*  82: 72 */   private final ConcurrentLinkedQueue<JSONStreamAware> pendingResponses = new ConcurrentLinkedQueue();
/*  83:    */   private AsyncContext asyncContext;
/*  84:    */   
/*  85:    */   public String getSecretPhrase()
/*  86:    */   {
/*  87: 78 */     return this.secretPhrase;
/*  88:    */   }
/*  89:    */   
/*  90:    */   public byte[] getPublicKey()
/*  91:    */   {
/*  92: 82 */     return this.publicKey;
/*  93:    */   }
/*  94:    */   
/*  95:    */   boolean isInactive()
/*  96:    */   {
/*  97: 86 */     return this.isInactive;
/*  98:    */   }
/*  99:    */   
/* 100:    */   public synchronized void send(JSONStreamAware paramJSONStreamAware)
/* 101:    */   {
/* 102: 90 */     if (this.asyncContext == null)
/* 103:    */     {
/* 104: 92 */       if (this.isInactive) {
/* 105: 94 */         return;
/* 106:    */       }
/* 107: 96 */       if (this.pendingResponses.size() > 1000)
/* 108:    */       {
/* 109: 97 */         this.pendingResponses.clear();
/* 110:    */         
/* 111: 99 */         this.isInactive = true;
/* 112:100 */         if (this.secretPhrase == null) {
/* 113:102 */           users.values().remove(this);
/* 114:    */         }
/* 115:104 */         return;
/* 116:    */       }
/* 117:107 */       this.pendingResponses.offer(paramJSONStreamAware);
/* 118:    */     }
/* 119:    */     else
/* 120:    */     {
/* 121:111 */       JSONArray localJSONArray = new JSONArray();
/* 122:    */       JSONStreamAware localJSONStreamAware;
/* 123:113 */       while ((localJSONStreamAware = (JSONStreamAware)this.pendingResponses.poll()) != null) {
/* 124:115 */         localJSONArray.add(localJSONStreamAware);
/* 125:    */       }
/* 126:118 */       localJSONArray.add(paramJSONStreamAware);
/* 127:    */       
/* 128:120 */       JSONObject localJSONObject = new JSONObject();
/* 129:121 */       localJSONObject.put("responses", localJSONArray);
/* 130:    */       
/* 131:123 */       this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
/* 132:    */       try
/* 133:    */       {
/* 134:125 */         PrintWriter localPrintWriter = this.asyncContext.getResponse().getWriter();Object localObject1 = null;
/* 135:    */         try
/* 136:    */         {
/* 137:126 */           localJSONObject.writeJSONString(localPrintWriter);
/* 138:    */         }
/* 139:    */         catch (Throwable localThrowable2)
/* 140:    */         {
/* 141:125 */           localObject1 = localThrowable2;throw localThrowable2;
/* 142:    */         }
/* 143:    */         finally
/* 144:    */         {
/* 145:127 */           if (localPrintWriter != null) {
/* 146:127 */             if (localObject1 != null) {
/* 147:    */               try
/* 148:    */               {
/* 149:127 */                 localPrintWriter.close();
/* 150:    */               }
/* 151:    */               catch (Throwable localThrowable3)
/* 152:    */               {
/* 153:127 */                 localObject1.addSuppressed(localThrowable3);
/* 154:    */               }
/* 155:    */             } else {
/* 156:127 */               localPrintWriter.close();
/* 157:    */             }
/* 158:    */           }
/* 159:    */         }
/* 160:    */       }
/* 161:    */       catch (IOException localIOException)
/* 162:    */       {
/* 163:128 */         Logger.logMessage("Error sending response to user", localIOException);
/* 164:    */       }
/* 165:131 */       this.asyncContext.complete();
/* 166:132 */       this.asyncContext = null;
/* 167:    */     }
/* 168:    */   }
/* 169:    */   
/* 170:    */   public synchronized void processPendingResponses(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
/* 171:    */     throws ServletException, IOException
/* 172:    */   {
/* 173:139 */     JSONArray localJSONArray = new JSONArray();
/* 174:    */     JSONStreamAware localJSONStreamAware;
/* 175:141 */     while ((localJSONStreamAware = (JSONStreamAware)this.pendingResponses.poll()) != null) {
/* 176:142 */       localJSONArray.add(localJSONStreamAware);
/* 177:    */     }
/* 178:    */     Object localObject1;
/* 179:    */     Object localObject2;
/* 180:144 */     if (localJSONArray.size() > 0)
/* 181:    */     {
/* 182:145 */       localObject1 = new JSONObject();
/* 183:146 */       ((JSONObject)localObject1).put("responses", localJSONArray);
/* 184:    */       Object localObject3;
/* 185:147 */       if (this.asyncContext != null)
/* 186:    */       {
/* 187:148 */         this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
/* 188:149 */         localObject2 = this.asyncContext.getResponse().getWriter();localObject3 = null;
/* 189:    */         try
/* 190:    */         {
/* 191:150 */           ((JSONObject)localObject1).writeJSONString((Writer)localObject2);
/* 192:    */         }
/* 193:    */         catch (Throwable localThrowable4)
/* 194:    */         {
/* 195:149 */           localObject3 = localThrowable4;throw localThrowable4;
/* 196:    */         }
/* 197:    */         finally
/* 198:    */         {
/* 199:151 */           if (localObject2 != null) {
/* 200:151 */             if (localObject3 != null) {
/* 201:    */               try
/* 202:    */               {
/* 203:151 */                 ((Writer)localObject2).close();
/* 204:    */               }
/* 205:    */               catch (Throwable localThrowable7)
/* 206:    */               {
/* 207:151 */                 localObject3.addSuppressed(localThrowable7);
/* 208:    */               }
/* 209:    */             } else {
/* 210:151 */               ((Writer)localObject2).close();
/* 211:    */             }
/* 212:    */           }
/* 213:    */         }
/* 214:152 */         this.asyncContext.complete();
/* 215:153 */         this.asyncContext = paramHttpServletRequest.startAsync();
/* 216:154 */         this.asyncContext.addListener(new UserAsyncListener(null));
/* 217:155 */         this.asyncContext.setTimeout(5000L);
/* 218:    */       }
/* 219:    */       else
/* 220:    */       {
/* 221:157 */         paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
/* 222:158 */         localObject2 = paramHttpServletResponse.getWriter();localObject3 = null;
/* 223:    */         try
/* 224:    */         {
/* 225:159 */           ((JSONObject)localObject1).writeJSONString((Writer)localObject2);
/* 226:    */         }
/* 227:    */         catch (Throwable localThrowable6)
/* 228:    */         {
/* 229:158 */           localObject3 = localThrowable6;throw localThrowable6;
/* 230:    */         }
/* 231:    */         finally
/* 232:    */         {
/* 233:160 */           if (localObject2 != null) {
/* 234:160 */             if (localObject3 != null) {
/* 235:    */               try
/* 236:    */               {
/* 237:160 */                 ((Writer)localObject2).close();
/* 238:    */               }
/* 239:    */               catch (Throwable localThrowable8)
/* 240:    */               {
/* 241:160 */                 localObject3.addSuppressed(localThrowable8);
/* 242:    */               }
/* 243:    */             } else {
/* 244:160 */               ((Writer)localObject2).close();
/* 245:    */             }
/* 246:    */           }
/* 247:    */         }
/* 248:    */       }
/* 249:    */     }
/* 250:    */     else
/* 251:    */     {
/* 252:163 */       if (this.asyncContext != null)
/* 253:    */       {
/* 254:164 */         this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
/* 255:165 */         localObject1 = this.asyncContext.getResponse().getWriter();localObject2 = null;
/* 256:    */         try
/* 257:    */         {
/* 258:166 */           JSON.emptyJSON.writeJSONString((Writer)localObject1);
/* 259:    */         }
/* 260:    */         catch (Throwable localThrowable2)
/* 261:    */         {
/* 262:165 */           localObject2 = localThrowable2;throw localThrowable2;
/* 263:    */         }
/* 264:    */         finally
/* 265:    */         {
/* 266:167 */           if (localObject1 != null) {
/* 267:167 */             if (localObject2 != null) {
/* 268:    */               try
/* 269:    */               {
/* 270:167 */                 ((Writer)localObject1).close();
/* 271:    */               }
/* 272:    */               catch (Throwable localThrowable9)
/* 273:    */               {
/* 274:167 */                 ((Throwable)localObject2).addSuppressed(localThrowable9);
/* 275:    */               }
/* 276:    */             } else {
/* 277:167 */               ((Writer)localObject1).close();
/* 278:    */             }
/* 279:    */           }
/* 280:    */         }
/* 281:168 */         this.asyncContext.complete();
/* 282:    */       }
/* 283:170 */       this.asyncContext = paramHttpServletRequest.startAsync();
/* 284:171 */       this.asyncContext.addListener(new UserAsyncListener(null));
/* 285:172 */       this.asyncContext.setTimeout(5000L);
/* 286:    */     }
/* 287:    */   }
/* 288:    */   
/* 289:    */   void enqueue(JSONStreamAware paramJSONStreamAware)
/* 290:    */   {
/* 291:177 */     this.pendingResponses.offer(paramJSONStreamAware);
/* 292:    */   }
/* 293:    */   
/* 294:    */   void deinitializeKeyPair()
/* 295:    */   {
/* 296:181 */     this.secretPhrase = null;
/* 297:182 */     this.publicKey = null;
/* 298:    */   }
/* 299:    */   
/* 300:    */   BigInteger initializeKeyPair(String paramString)
/* 301:    */   {
/* 302:186 */     this.publicKey = Crypto.getPublicKey(paramString);
/* 303:187 */     this.secretPhrase = paramString;
/* 304:188 */     byte[] arrayOfByte = Crypto.sha256().digest(this.publicKey);
/* 305:189 */     return new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
/* 306:    */   }
/* 307:    */   
/* 308:    */   private final class UserAsyncListener
/* 309:    */     implements AsyncListener
/* 310:    */   {
/* 311:    */     private UserAsyncListener() {}
/* 312:    */     
/* 313:    */     public void onComplete(AsyncEvent paramAsyncEvent)
/* 314:    */       throws IOException
/* 315:    */     {}
/* 316:    */     
/* 317:    */     public void onError(AsyncEvent paramAsyncEvent)
/* 318:    */       throws IOException
/* 319:    */     {
/* 320:202 */       synchronized (User.this)
/* 321:    */       {
/* 322:203 */         User.this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
/* 323:    */         
/* 324:205 */         PrintWriter localPrintWriter = User.this.asyncContext.getResponse().getWriter();Object localObject1 = null;
/* 325:    */         try
/* 326:    */         {
/* 327:206 */           JSON.emptyJSON.writeJSONString(localPrintWriter);
/* 328:    */         }
/* 329:    */         catch (Throwable localThrowable2)
/* 330:    */         {
/* 331:205 */           localObject1 = localThrowable2;throw localThrowable2;
/* 332:    */         }
/* 333:    */         finally
/* 334:    */         {
/* 335:207 */           if (localPrintWriter != null) {
/* 336:207 */             if (localObject1 != null) {
/* 337:    */               try
/* 338:    */               {
/* 339:207 */                 localPrintWriter.close();
/* 340:    */               }
/* 341:    */               catch (Throwable localThrowable3)
/* 342:    */               {
/* 343:207 */                 localObject1.addSuppressed(localThrowable3);
/* 344:    */               }
/* 345:    */             } else {
/* 346:207 */               localPrintWriter.close();
/* 347:    */             }
/* 348:    */           }
/* 349:    */         }
/* 350:209 */         User.this.asyncContext.complete();
/* 351:210 */         User.this.asyncContext = null;
/* 352:    */       }
/* 353:    */     }
/* 354:    */     
/* 355:    */     public void onStartAsync(AsyncEvent paramAsyncEvent)
/* 356:    */       throws IOException
/* 357:    */     {}
/* 358:    */     
/* 359:    */     public void onTimeout(AsyncEvent paramAsyncEvent)
/* 360:    */       throws IOException
/* 361:    */     {
/* 362:221 */       synchronized (User.this)
/* 363:    */       {
/* 364:222 */         User.this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
/* 365:    */         
/* 366:224 */         PrintWriter localPrintWriter = User.this.asyncContext.getResponse().getWriter();Object localObject1 = null;
/* 367:    */         try
/* 368:    */         {
/* 369:225 */           JSON.emptyJSON.writeJSONString(localPrintWriter);
/* 370:    */         }
/* 371:    */         catch (Throwable localThrowable2)
/* 372:    */         {
/* 373:224 */           localObject1 = localThrowable2;throw localThrowable2;
/* 374:    */         }
/* 375:    */         finally
/* 376:    */         {
/* 377:226 */           if (localPrintWriter != null) {
/* 378:226 */             if (localObject1 != null) {
/* 379:    */               try
/* 380:    */               {
/* 381:226 */                 localPrintWriter.close();
/* 382:    */               }
/* 383:    */               catch (Throwable localThrowable3)
/* 384:    */               {
/* 385:226 */                 localObject1.addSuppressed(localThrowable3);
/* 386:    */               }
/* 387:    */             } else {
/* 388:226 */               localPrintWriter.close();
/* 389:    */             }
/* 390:    */           }
/* 391:    */         }
/* 392:228 */         User.this.asyncContext.complete();
/* 393:229 */         User.this.asyncContext = null;
/* 394:    */       }
/* 395:    */     }
/* 396:    */   }
/* 397:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.User
 * JD-Core Version:    0.7.0.1
 */