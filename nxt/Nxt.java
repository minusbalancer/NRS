/*   1:    */ package nxt;
/*   2:    */ 
/*   3:    */ import java.io.IOException;
/*   4:    */ import java.io.PrintWriter;
/*   5:    */ import java.io.Writer;
/*   6:    */ import java.util.Calendar;
/*   7:    */ import java.util.Collections;
/*   8:    */ import java.util.HashSet;
/*   9:    */ import java.util.Set;
/*  10:    */ import javax.servlet.ServletConfig;
/*  11:    */ import javax.servlet.ServletException;
/*  12:    */ import javax.servlet.http.HttpServlet;
/*  13:    */ import javax.servlet.http.HttpServletRequest;
/*  14:    */ import javax.servlet.http.HttpServletResponse;
/*  15:    */ import nxt.http.HttpRequestHandler;
/*  16:    */ import nxt.peer.Hallmark;
/*  17:    */ import nxt.peer.HttpJSONRequestHandler;
/*  18:    */ import nxt.peer.Peer;
/*  19:    */ import nxt.user.User;
/*  20:    */ import nxt.user.UserRequestHandler;
/*  21:    */ import nxt.util.Logger;
/*  22:    */ import org.json.simple.JSONArray;
/*  23:    */ import org.json.simple.JSONObject;
/*  24:    */ 
/*  25:    */ public final class Nxt
/*  26:    */   extends HttpServlet
/*  27:    */ {
/*  28:    */   public static final String VERSION = "0.7.1";
/*  29:    */   public static final int BLOCK_HEADER_LENGTH = 224;
/*  30:    */   public static final int MAX_NUMBER_OF_TRANSACTIONS = 255;
/*  31:    */   public static final int MAX_PAYLOAD_LENGTH = 32640;
/*  32:    */   public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;
/*  33:    */   public static final int ALIAS_SYSTEM_BLOCK = 22000;
/*  34:    */   public static final int TRANSPARENT_FORGING_BLOCK = 30000;
/*  35:    */   public static final int ARBITRARY_MESSAGES_BLOCK = 40000;
/*  36:    */   public static final int TRANSPARENT_FORGING_BLOCK_2 = 47000;
/*  37:    */   public static final int TRANSPARENT_FORGING_BLOCK_3 = 51000;
/*  38:    */   public static final int TRANSPARENT_FORGING_BLOCK_4 = 64000;
/*  39:    */   public static final long MAX_BALANCE = 1000000000L;
/*  40:    */   public static final long initialBaseTarget = 153722867L;
/*  41:    */   public static final long maxBaseTarget = 153722867000000000L;
/*  42:    */   public static final long MAX_ASSET_QUANTITY = 1000000000L;
/*  43:    */   public static final int ASSET_ISSUANCE_FEE = 1000;
/*  44:    */   public static final int MAX_ALIAS_URI_LENGTH = 1000;
/*  45:    */   public static final int MAX_ALIAS_LENGTH = 100;
/*  46:    */   public static final long epochBeginning;
/*  47:    */   public static String myPlatform;
/*  48:    */   public static String myScheme;
/*  49:    */   public static String myAddress;
/*  50:    */   public static String myHallmark;
/*  51:    */   public static int myPort;
/*  52:    */   public static boolean shareMyAddress;
/*  53:    */   public static Set<String> allowedUserHosts;
/*  54:    */   public static Set<String> allowedBotHosts;
/*  55:    */   public static int blacklistingPeriod;
/*  56:    */   public static final int LOGGING_MASK_EXCEPTIONS = 1;
/*  57:    */   public static final int LOGGING_MASK_NON200_RESPONSES = 2;
/*  58:    */   public static final int LOGGING_MASK_200_RESPONSES = 4;
/*  59:    */   public static int communicationLoggingMask;
/*  60:    */   public static Set<String> wellKnownPeers;
/*  61:    */   public static int maxNumberOfConnectedPublicPeers;
/*  62:    */   public static int connectTimeout;
/*  63:    */   public static int readTimeout;
/*  64:    */   public static boolean enableHallmarkProtection;
/*  65:    */   public static int pushThreshold;
/*  66:    */   public static int pullThreshold;
/*  67:    */   public static int sendToPeersLimit;
/*  68:    */   
/*  69:    */   static
/*  70:    */   {
/*  71: 51 */     Calendar localCalendar = Calendar.getInstance();
/*  72: 52 */     localCalendar.set(15, 0);
/*  73: 53 */     localCalendar.set(1, 2013);
/*  74: 54 */     localCalendar.set(2, 10);
/*  75: 55 */     localCalendar.set(5, 24);
/*  76: 56 */     localCalendar.set(11, 12);
/*  77: 57 */     localCalendar.set(12, 0);
/*  78: 58 */     localCalendar.set(13, 0);
/*  79: 59 */     localCalendar.set(14, 0);
/*  80: 60 */     epochBeginning = localCalendar.getTimeInMillis();
/*  81:    */   }
/*  82:    */   
/*  83:    */   public void init(ServletConfig paramServletConfig)
/*  84:    */     throws ServletException
/*  85:    */   {
/*  86: 91 */     Logger.logMessage("NRS 0.7.1 starting...");
/*  87: 92 */     if (Logger.debug) {
/*  88: 93 */       Logger.logMessage("DEBUG logging enabled");
/*  89:    */     }
/*  90: 95 */     if (Logger.enableStackTraces) {
/*  91: 96 */       Logger.logMessage("logging of exception stack traces enabled");
/*  92:    */     }
/*  93:    */     try
/*  94:    */     {
/*  95:101 */       myPlatform = paramServletConfig.getInitParameter("myPlatform");
/*  96:102 */       Logger.logMessage("\"myPlatform\" = \"" + myPlatform + "\"");
/*  97:103 */       if (myPlatform == null) {
/*  98:105 */         myPlatform = "PC";
/*  99:    */       } else {
/* 100:109 */         myPlatform = myPlatform.trim();
/* 101:    */       }
/* 102:113 */       myScheme = paramServletConfig.getInitParameter("myScheme");
/* 103:114 */       Logger.logMessage("\"myScheme\" = \"" + myScheme + "\"");
/* 104:115 */       if (myScheme == null) {
/* 105:117 */         myScheme = "http";
/* 106:    */       } else {
/* 107:121 */         myScheme = myScheme.trim();
/* 108:    */       }
/* 109:125 */       String str1 = paramServletConfig.getInitParameter("myPort");
/* 110:126 */       Logger.logMessage("\"myPort\" = \"" + str1 + "\"");
/* 111:    */       try
/* 112:    */       {
/* 113:129 */         myPort = Integer.parseInt(str1);
/* 114:    */       }
/* 115:    */       catch (NumberFormatException localNumberFormatException1)
/* 116:    */       {
/* 117:133 */         myPort = myScheme.equals("https") ? 7875 : 7874;
/* 118:134 */         Logger.logMessage("Invalid value for myPort " + str1 + ", using default " + myPort);
/* 119:    */       }
/* 120:137 */       myAddress = paramServletConfig.getInitParameter("myAddress");
/* 121:138 */       Logger.logMessage("\"myAddress\" = \"" + myAddress + "\"");
/* 122:139 */       if (myAddress != null) {
/* 123:141 */         myAddress = myAddress.trim();
/* 124:    */       }
/* 125:145 */       String str2 = paramServletConfig.getInitParameter("shareMyAddress");
/* 126:146 */       Logger.logMessage("\"shareMyAddress\" = \"" + str2 + "\"");
/* 127:147 */       shareMyAddress = Boolean.parseBoolean(str2);
/* 128:    */       
/* 129:149 */       myHallmark = paramServletConfig.getInitParameter("myHallmark");
/* 130:150 */       Logger.logMessage("\"myHallmark\" = \"" + myHallmark + "\"");
/* 131:151 */       if ((myHallmark != null) && ((Nxt.myHallmark = myHallmark.trim()).length() > 0)) {
/* 132:    */         try
/* 133:    */         {
/* 134:154 */           Hallmark localHallmark = Hallmark.parseHallmark(myHallmark);
/* 135:155 */           if (!localHallmark.isValid()) {
/* 136:156 */             throw new RuntimeException();
/* 137:    */           }
/* 138:    */         }
/* 139:    */         catch (RuntimeException localRuntimeException)
/* 140:    */         {
/* 141:159 */           Logger.logMessage("Your hallmark is invalid: " + myHallmark);
/* 142:160 */           System.exit(1);
/* 143:    */         }
/* 144:    */       }
/* 145:165 */       String str3 = paramServletConfig.getInitParameter("wellKnownPeers");
/* 146:166 */       Logger.logMessage("\"wellKnownPeers\" = \"" + str3 + "\"");
/* 147:167 */       if (str3 != null)
/* 148:    */       {
/* 149:168 */         localObject1 = new HashSet();
/* 150:169 */         for (str7 : str3.split(";"))
/* 151:    */         {
/* 152:171 */           str7 = str7.trim();
/* 153:172 */           if (str7.length() > 0)
/* 154:    */           {
/* 155:174 */             ((Set)localObject1).add(str7);
/* 156:175 */             Peer.addPeer(str7, str7);
/* 157:    */           }
/* 158:    */         }
/* 159:180 */         wellKnownPeers = Collections.unmodifiableSet((Set)localObject1);
/* 160:    */       }
/* 161:    */       else
/* 162:    */       {
/* 163:182 */         wellKnownPeers = Collections.emptySet();
/* 164:183 */         Logger.logMessage("No wellKnownPeers defined, it is unlikely to work");
/* 165:    */       }
/* 166:186 */       Object localObject1 = paramServletConfig.getInitParameter("maxNumberOfConnectedPublicPeers");
/* 167:187 */       Logger.logMessage("\"maxNumberOfConnectedPublicPeers\" = \"" + (String)localObject1 + "\"");
/* 168:    */       try
/* 169:    */       {
/* 170:190 */         maxNumberOfConnectedPublicPeers = Integer.parseInt((String)localObject1);
/* 171:    */       }
/* 172:    */       catch (NumberFormatException localNumberFormatException2)
/* 173:    */       {
/* 174:194 */         maxNumberOfConnectedPublicPeers = 10;
/* 175:195 */         Logger.logMessage("Invalid value for maxNumberOfConnectedPublicPeers " + (String)localObject1 + ", using default " + maxNumberOfConnectedPublicPeers);
/* 176:    */       }
/* 177:198 */       String str4 = paramServletConfig.getInitParameter("connectTimeout");
/* 178:199 */       Logger.logMessage("\"connectTimeout\" = \"" + str4 + "\"");
/* 179:    */       try
/* 180:    */       {
/* 181:202 */         connectTimeout = Integer.parseInt(str4);
/* 182:    */       }
/* 183:    */       catch (NumberFormatException localNumberFormatException3)
/* 184:    */       {
/* 185:206 */         connectTimeout = 1000;
/* 186:207 */         Logger.logMessage("Invalid value for connectTimeout " + str4 + ", using default " + connectTimeout);
/* 187:    */       }
/* 188:210 */       String str5 = paramServletConfig.getInitParameter("readTimeout");
/* 189:211 */       Logger.logMessage("\"readTimeout\" = \"" + str5 + "\"");
/* 190:    */       try
/* 191:    */       {
/* 192:214 */         readTimeout = Integer.parseInt(str5);
/* 193:    */       }
/* 194:    */       catch (NumberFormatException localNumberFormatException4)
/* 195:    */       {
/* 196:218 */         readTimeout = 1000;
/* 197:219 */         Logger.logMessage("Invalid value for readTimeout " + str5 + ", using default " + readTimeout);
/* 198:    */       }
/* 199:222 */       String str6 = paramServletConfig.getInitParameter("enableHallmarkProtection");
/* 200:223 */       Logger.logMessage("\"enableHallmarkProtection\" = \"" + str6 + "\"");
/* 201:224 */       enableHallmarkProtection = Boolean.parseBoolean(str6);
/* 202:    */       
/* 203:226 */       String str7 = paramServletConfig.getInitParameter("pushThreshold");
/* 204:227 */       Logger.logMessage("\"pushThreshold\" = \"" + str7 + "\"");
/* 205:    */       try
/* 206:    */       {
/* 207:230 */         pushThreshold = Integer.parseInt(str7);
/* 208:    */       }
/* 209:    */       catch (NumberFormatException localNumberFormatException5)
/* 210:    */       {
/* 211:234 */         pushThreshold = 0;
/* 212:235 */         Logger.logMessage("Invalid value for pushThreshold " + str7 + ", using default " + pushThreshold);
/* 213:    */       }
/* 214:238 */       String str8 = paramServletConfig.getInitParameter("pullThreshold");
/* 215:239 */       Logger.logMessage("\"pullThreshold\" = \"" + str8 + "\"");
/* 216:    */       try
/* 217:    */       {
/* 218:242 */         pullThreshold = Integer.parseInt(str8);
/* 219:    */       }
/* 220:    */       catch (NumberFormatException localNumberFormatException6)
/* 221:    */       {
/* 222:246 */         pullThreshold = 0;
/* 223:247 */         Logger.logMessage("Invalid value for pullThreshold " + str8 + ", using default " + pullThreshold);
/* 224:    */       }
/* 225:251 */       String str9 = paramServletConfig.getInitParameter("allowedUserHosts");
/* 226:252 */       Logger.logMessage("\"allowedUserHosts\" = \"" + str9 + "\"");
/* 227:253 */       if (str9 != null) {
/* 228:255 */         if (!str9.trim().equals("*"))
/* 229:    */         {
/* 230:257 */           localObject2 = new HashSet();
/* 231:258 */           for (String str12 : str9.split(";"))
/* 232:    */           {
/* 233:260 */             str12 = str12.trim();
/* 234:261 */             if (str12.length() > 0) {
/* 235:263 */               ((Set)localObject2).add(str12);
/* 236:    */             }
/* 237:    */           }
/* 238:268 */           allowedUserHosts = Collections.unmodifiableSet((Set)localObject2);
/* 239:    */         }
/* 240:    */       }
/* 241:274 */       Object localObject2 = paramServletConfig.getInitParameter("allowedBotHosts");
/* 242:275 */       Logger.logMessage("\"allowedBotHosts\" = \"" + (String)localObject2 + "\"");
/* 243:276 */       if (localObject2 != null) {
/* 244:278 */         if (!((String)localObject2).trim().equals("*"))
/* 245:    */         {
/* 246:280 */           ??? = new HashSet();
/* 247:281 */           for (String str13 : ((String)localObject2).split(";"))
/* 248:    */           {
/* 249:283 */             str13 = str13.trim();
/* 250:284 */             if (str13.length() > 0) {
/* 251:286 */               ((Set)???).add(str13);
/* 252:    */             }
/* 253:    */           }
/* 254:291 */           allowedBotHosts = Collections.unmodifiableSet((Set)???);
/* 255:    */         }
/* 256:    */       }
/* 257:296 */       ??? = paramServletConfig.getInitParameter("blacklistingPeriod");
/* 258:297 */       Logger.logMessage("\"blacklistingPeriod\" = \"" + (String)??? + "\"");
/* 259:    */       try
/* 260:    */       {
/* 261:300 */         blacklistingPeriod = Integer.parseInt((String)???);
/* 262:    */       }
/* 263:    */       catch (NumberFormatException localNumberFormatException7)
/* 264:    */       {
/* 265:304 */         blacklistingPeriod = 300000;
/* 266:305 */         Logger.logMessage("Invalid value for blacklistingPeriod " + (String)??? + ", using default " + blacklistingPeriod);
/* 267:    */       }
/* 268:309 */       String str10 = paramServletConfig.getInitParameter("communicationLoggingMask");
/* 269:310 */       Logger.logMessage("\"communicationLoggingMask\" = \"" + str10 + "\"");
/* 270:    */       try
/* 271:    */       {
/* 272:313 */         communicationLoggingMask = Integer.parseInt(str10);
/* 273:    */       }
/* 274:    */       catch (NumberFormatException localNumberFormatException8)
/* 275:    */       {
/* 276:316 */         Logger.logMessage("Invalid value for communicationLogginMask " + str10 + ", using default 0");
/* 277:    */       }
/* 278:319 */       String str11 = paramServletConfig.getInitParameter("sendToPeersLimit");
/* 279:320 */       Logger.logMessage("\"sendToPeersLimit\" = \"" + str11 + "\"");
/* 280:    */       try
/* 281:    */       {
/* 282:323 */         sendToPeersLimit = Integer.parseInt(str11);
/* 283:    */       }
/* 284:    */       catch (NumberFormatException localNumberFormatException9)
/* 285:    */       {
/* 286:326 */         sendToPeersLimit = 10;
/* 287:327 */         Logger.logMessage("Invalid value for sendToPeersLimit " + str11 + ", using default " + sendToPeersLimit);
/* 288:    */       }
/* 289:330 */       Db.init();
/* 290:    */       
/* 291:332 */       Blockchain.init();
/* 292:    */       
/* 293:334 */       ThreadPools.start();
/* 294:    */       
/* 295:336 */       Logger.logMessage("NRS 0.7.1 started successfully.");
/* 296:    */     }
/* 297:    */     catch (Exception localException)
/* 298:    */     {
/* 299:340 */       Logger.logMessage("Error initializing Nxt servlet", localException);
/* 300:341 */       System.exit(1);
/* 301:    */     }
/* 302:    */   }
/* 303:    */   
/* 304:    */   public void doGet(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
/* 305:    */     throws ServletException, IOException
/* 306:    */   {
/* 307:349 */     paramHttpServletResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
/* 308:350 */     paramHttpServletResponse.setHeader("Pragma", "no-cache");
/* 309:351 */     paramHttpServletResponse.setDateHeader("Expires", 0L);
/* 310:    */     
/* 311:353 */     User localUser = null;
/* 312:    */     try
/* 313:    */     {
/* 314:357 */       String str = paramHttpServletRequest.getParameter("user");
/* 315:359 */       if (str == null)
/* 316:    */       {
/* 317:360 */         HttpRequestHandler.process(paramHttpServletRequest, paramHttpServletResponse);
/* 318:361 */         return;
/* 319:    */       }
/* 320:364 */       if ((allowedUserHosts != null) && (!allowedUserHosts.contains(paramHttpServletRequest.getRemoteHost())))
/* 321:    */       {
/* 322:365 */         JSONObject localJSONObject1 = new JSONObject();
/* 323:366 */         localJSONObject1.put("response", "denyAccess");
/* 324:367 */         JSONArray localJSONArray = new JSONArray();
/* 325:368 */         localJSONArray.add(localJSONObject1);
/* 326:369 */         JSONObject localJSONObject2 = new JSONObject();
/* 327:370 */         localJSONObject2.put("responses", localJSONArray);
/* 328:371 */         paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
/* 329:372 */         PrintWriter localPrintWriter = paramHttpServletResponse.getWriter();Object localObject1 = null;
/* 330:    */         try
/* 331:    */         {
/* 332:373 */           localJSONObject2.writeJSONString(localPrintWriter);
/* 333:    */         }
/* 334:    */         catch (Throwable localThrowable2)
/* 335:    */         {
/* 336:372 */           localObject1 = localThrowable2;throw localThrowable2;
/* 337:    */         }
/* 338:    */         finally
/* 339:    */         {
/* 340:374 */           if (localPrintWriter != null) {
/* 341:374 */             if (localObject1 != null) {
/* 342:    */               try
/* 343:    */               {
/* 344:374 */                 localPrintWriter.close();
/* 345:    */               }
/* 346:    */               catch (Throwable localThrowable3)
/* 347:    */               {
/* 348:374 */                 localObject1.addSuppressed(localThrowable3);
/* 349:    */               }
/* 350:    */             } else {
/* 351:374 */               localPrintWriter.close();
/* 352:    */             }
/* 353:    */           }
/* 354:    */         }
/* 355:375 */         return;
/* 356:    */       }
/* 357:378 */       localUser = User.getUser(str);
/* 358:379 */       UserRequestHandler.process(paramHttpServletRequest, localUser);
/* 359:    */     }
/* 360:    */     catch (Exception localException)
/* 361:    */     {
/* 362:382 */       if (localUser != null) {
/* 363:383 */         Logger.logMessage("Error processing GET request", localException);
/* 364:    */       } else {
/* 365:385 */         Logger.logDebugMessage("Error processing GET request", localException);
/* 366:    */       }
/* 367:    */     }
/* 368:389 */     if (localUser != null) {
/* 369:390 */       localUser.processPendingResponses(paramHttpServletRequest, paramHttpServletResponse);
/* 370:    */     }
/* 371:    */   }
/* 372:    */   
/* 373:    */   public void doPost(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
/* 374:    */     throws ServletException, IOException
/* 375:    */   {
/* 376:397 */     HttpJSONRequestHandler.process(paramHttpServletRequest, paramHttpServletResponse);
/* 377:    */   }
/* 378:    */   
/* 379:    */   public void destroy()
/* 380:    */   {
/* 381:404 */     ThreadPools.shutdown();
/* 382:    */     
/* 383:406 */     Db.shutdown();
/* 384:    */     
/* 385:408 */     Logger.logMessage("NRS 0.7.1 stopped.");
/* 386:    */   }
/* 387:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Nxt
 * JD-Core Version:    0.7.0.1
 */