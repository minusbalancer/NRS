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
/*  16:    */ import nxt.peer.HttpJSONRequestHandler;
/*  17:    */ import nxt.peer.Peer;
/*  18:    */ import nxt.user.User;
/*  19:    */ import nxt.user.UserRequestHandler;
/*  20:    */ import nxt.util.Convert;
/*  21:    */ import nxt.util.Logger;
/*  22:    */ import org.json.simple.JSONArray;
/*  23:    */ import org.json.simple.JSONObject;
/*  24:    */ 
/*  25:    */ public final class Nxt
/*  26:    */   extends HttpServlet
/*  27:    */ {
/*  28:    */   public static final String VERSION = "0.6.1";
/*  29:    */   public static final int BLOCK_HEADER_LENGTH = 224;
/*  30:    */   public static final int MAX_NUMBER_OF_TRANSACTIONS = 255;
/*  31:    */   public static final int MAX_PAYLOAD_LENGTH = 32640;
/*  32:    */   public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;
/*  33:    */   public static final int ALIAS_SYSTEM_BLOCK = 22000;
/*  34:    */   public static final int TRANSPARENT_FORGING_BLOCK = 30000;
/*  35:    */   public static final int ARBITRARY_MESSAGES_BLOCK = 40000;
/*  36:    */   public static final int TRANSPARENT_FORGING_BLOCK_2 = 47000;
/*  37:    */   public static final int TRANSPARENT_FORGING_BLOCK_3 = 51000;
/*  38:    */   public static final long MAX_BALANCE = 1000000000L;
/*  39:    */   public static final long initialBaseTarget = 153722867L;
/*  40:    */   public static final long maxBaseTarget = 153722867000000000L;
/*  41:    */   public static final long MAX_ASSET_QUANTITY = 1000000000L;
/*  42:    */   public static final int ASSET_ISSUANCE_FEE = 1000;
/*  43:    */   public static final int MAX_ALIAS_URI_LENGTH = 1000;
/*  44:    */   public static final int MAX_ALIAS_LENGTH = 100;
/*  45:    */   public static final long epochBeginning;
/*  46:    */   public static String myPlatform;
/*  47:    */   public static String myScheme;
/*  48:    */   public static String myAddress;
/*  49:    */   public static String myHallmark;
/*  50:    */   public static int myPort;
/*  51:    */   public static boolean shareMyAddress;
/*  52:    */   public static Set<String> allowedUserHosts;
/*  53:    */   public static Set<String> allowedBotHosts;
/*  54:    */   public static int blacklistingPeriod;
/*  55:    */   public static final int LOGGING_MASK_EXCEPTIONS = 1;
/*  56:    */   public static final int LOGGING_MASK_NON200_RESPONSES = 2;
/*  57:    */   public static final int LOGGING_MASK_200_RESPONSES = 4;
/*  58:    */   public static int communicationLoggingMask;
/*  59:    */   public static Set<String> wellKnownPeers;
/*  60:    */   public static int maxNumberOfConnectedPublicPeers;
/*  61:    */   public static int connectTimeout;
/*  62:    */   public static int readTimeout;
/*  63:    */   public static boolean enableHallmarkProtection;
/*  64:    */   public static int pushThreshold;
/*  65:    */   public static int pullThreshold;
/*  66:    */   public static int sendToPeersLimit;
/*  67:    */   
/*  68:    */   static
/*  69:    */   {
/*  70: 50 */     Calendar localCalendar = Calendar.getInstance();
/*  71: 51 */     localCalendar.set(15, 0);
/*  72: 52 */     localCalendar.set(1, 2013);
/*  73: 53 */     localCalendar.set(2, 10);
/*  74: 54 */     localCalendar.set(5, 24);
/*  75: 55 */     localCalendar.set(11, 12);
/*  76: 56 */     localCalendar.set(12, 0);
/*  77: 57 */     localCalendar.set(13, 0);
/*  78: 58 */     localCalendar.set(14, 0);
/*  79: 59 */     epochBeginning = localCalendar.getTimeInMillis();
/*  80:    */   }
/*  81:    */   
/*  82:    */   public void init(ServletConfig paramServletConfig)
/*  83:    */     throws ServletException
/*  84:    */   {
/*  85: 90 */     Logger.logMessage("NRS 0.6.1 starting...");
/*  86: 91 */     if (Logger.debug) {
/*  87: 92 */       Logger.logMessage("DEBUG logging enabled");
/*  88:    */     }
/*  89: 94 */     if (Logger.enableStackTraces) {
/*  90: 95 */       Logger.logMessage("logging of exception stack traces enabled");
/*  91:    */     }
/*  92:    */     try
/*  93:    */     {
/*  94:100 */       myPlatform = paramServletConfig.getInitParameter("myPlatform");
/*  95:101 */       Logger.logMessage("\"myPlatform\" = \"" + myPlatform + "\"");
/*  96:102 */       if (myPlatform == null) {
/*  97:104 */         myPlatform = "PC";
/*  98:    */       } else {
/*  99:108 */         myPlatform = myPlatform.trim();
/* 100:    */       }
/* 101:112 */       myScheme = paramServletConfig.getInitParameter("myScheme");
/* 102:113 */       Logger.logMessage("\"myScheme\" = \"" + myScheme + "\"");
/* 103:114 */       if (myScheme == null) {
/* 104:116 */         myScheme = "http";
/* 105:    */       } else {
/* 106:120 */         myScheme = myScheme.trim();
/* 107:    */       }
/* 108:124 */       String str1 = paramServletConfig.getInitParameter("myPort");
/* 109:125 */       Logger.logMessage("\"myPort\" = \"" + str1 + "\"");
/* 110:    */       try
/* 111:    */       {
/* 112:128 */         myPort = Integer.parseInt(str1);
/* 113:    */       }
/* 114:    */       catch (NumberFormatException localNumberFormatException1)
/* 115:    */       {
/* 116:132 */         myPort = myScheme.equals("https") ? 7875 : 7874;
/* 117:133 */         Logger.logMessage("Invalid value for myPort " + str1 + ", using default " + myPort);
/* 118:    */       }
/* 119:136 */       myAddress = paramServletConfig.getInitParameter("myAddress");
/* 120:137 */       Logger.logMessage("\"myAddress\" = \"" + myAddress + "\"");
/* 121:138 */       if (myAddress != null) {
/* 122:140 */         myAddress = myAddress.trim();
/* 123:    */       }
/* 124:144 */       String str2 = paramServletConfig.getInitParameter("shareMyAddress");
/* 125:145 */       Logger.logMessage("\"shareMyAddress\" = \"" + str2 + "\"");
/* 126:146 */       shareMyAddress = Boolean.parseBoolean(str2);
/* 127:    */       
/* 128:148 */       myHallmark = paramServletConfig.getInitParameter("myHallmark");
/* 129:149 */       Logger.logMessage("\"myHallmark\" = \"" + myHallmark + "\"");
/* 130:150 */       if (myHallmark != null)
/* 131:    */       {
/* 132:152 */         myHallmark = myHallmark.trim();
/* 133:    */         try
/* 134:    */         {
/* 135:155 */           Convert.convert(myHallmark);
/* 136:    */         }
/* 137:    */         catch (NumberFormatException localNumberFormatException2)
/* 138:    */         {
/* 139:157 */           Logger.logMessage("Your hallmark is invalid: " + myHallmark);
/* 140:158 */           System.exit(1);
/* 141:    */         }
/* 142:    */       }
/* 143:163 */       String str3 = paramServletConfig.getInitParameter("wellKnownPeers");
/* 144:164 */       Logger.logMessage("\"wellKnownPeers\" = \"" + str3 + "\"");
/* 145:165 */       if (str3 != null)
/* 146:    */       {
/* 147:166 */         localObject1 = new HashSet();
/* 148:167 */         for (str7 : str3.split(";"))
/* 149:    */         {
/* 150:169 */           str7 = str7.trim();
/* 151:170 */           if (str7.length() > 0)
/* 152:    */           {
/* 153:172 */             ((Set)localObject1).add(str7);
/* 154:173 */             Peer.addPeer(str7, str7);
/* 155:    */           }
/* 156:    */         }
/* 157:178 */         wellKnownPeers = Collections.unmodifiableSet((Set)localObject1);
/* 158:    */       }
/* 159:    */       else
/* 160:    */       {
/* 161:180 */         wellKnownPeers = Collections.emptySet();
/* 162:181 */         Logger.logMessage("No wellKnownPeers defined, it is unlikely to work");
/* 163:    */       }
/* 164:184 */       Object localObject1 = paramServletConfig.getInitParameter("maxNumberOfConnectedPublicPeers");
/* 165:185 */       Logger.logMessage("\"maxNumberOfConnectedPublicPeers\" = \"" + (String)localObject1 + "\"");
/* 166:    */       try
/* 167:    */       {
/* 168:188 */         maxNumberOfConnectedPublicPeers = Integer.parseInt((String)localObject1);
/* 169:    */       }
/* 170:    */       catch (NumberFormatException localNumberFormatException3)
/* 171:    */       {
/* 172:192 */         maxNumberOfConnectedPublicPeers = 10;
/* 173:193 */         Logger.logMessage("Invalid value for maxNumberOfConnectedPublicPeers " + (String)localObject1 + ", using default " + maxNumberOfConnectedPublicPeers);
/* 174:    */       }
/* 175:196 */       String str4 = paramServletConfig.getInitParameter("connectTimeout");
/* 176:197 */       Logger.logMessage("\"connectTimeout\" = \"" + str4 + "\"");
/* 177:    */       try
/* 178:    */       {
/* 179:200 */         connectTimeout = Integer.parseInt(str4);
/* 180:    */       }
/* 181:    */       catch (NumberFormatException localNumberFormatException4)
/* 182:    */       {
/* 183:204 */         connectTimeout = 1000;
/* 184:205 */         Logger.logMessage("Invalid value for connectTimeout " + str4 + ", using default " + connectTimeout);
/* 185:    */       }
/* 186:208 */       String str5 = paramServletConfig.getInitParameter("readTimeout");
/* 187:209 */       Logger.logMessage("\"readTimeout\" = \"" + str5 + "\"");
/* 188:    */       try
/* 189:    */       {
/* 190:212 */         readTimeout = Integer.parseInt(str5);
/* 191:    */       }
/* 192:    */       catch (NumberFormatException localNumberFormatException5)
/* 193:    */       {
/* 194:216 */         readTimeout = 1000;
/* 195:217 */         Logger.logMessage("Invalid value for readTimeout " + str5 + ", using default " + readTimeout);
/* 196:    */       }
/* 197:220 */       String str6 = paramServletConfig.getInitParameter("enableHallmarkProtection");
/* 198:221 */       Logger.logMessage("\"enableHallmarkProtection\" = \"" + str6 + "\"");
/* 199:222 */       enableHallmarkProtection = Boolean.parseBoolean(str6);
/* 200:    */       
/* 201:224 */       String str7 = paramServletConfig.getInitParameter("pushThreshold");
/* 202:225 */       Logger.logMessage("\"pushThreshold\" = \"" + str7 + "\"");
/* 203:    */       try
/* 204:    */       {
/* 205:228 */         pushThreshold = Integer.parseInt(str7);
/* 206:    */       }
/* 207:    */       catch (NumberFormatException localNumberFormatException6)
/* 208:    */       {
/* 209:232 */         pushThreshold = 0;
/* 210:233 */         Logger.logMessage("Invalid value for pushThreshold " + str7 + ", using default " + pushThreshold);
/* 211:    */       }
/* 212:236 */       String str8 = paramServletConfig.getInitParameter("pullThreshold");
/* 213:237 */       Logger.logMessage("\"pullThreshold\" = \"" + str8 + "\"");
/* 214:    */       try
/* 215:    */       {
/* 216:240 */         pullThreshold = Integer.parseInt(str8);
/* 217:    */       }
/* 218:    */       catch (NumberFormatException localNumberFormatException7)
/* 219:    */       {
/* 220:244 */         pullThreshold = 0;
/* 221:245 */         Logger.logMessage("Invalid value for pullThreshold " + str8 + ", using default " + pullThreshold);
/* 222:    */       }
/* 223:249 */       String str9 = paramServletConfig.getInitParameter("allowedUserHosts");
/* 224:250 */       Logger.logMessage("\"allowedUserHosts\" = \"" + str9 + "\"");
/* 225:251 */       if (str9 != null) {
/* 226:253 */         if (!str9.trim().equals("*"))
/* 227:    */         {
/* 228:255 */           localObject2 = new HashSet();
/* 229:256 */           for (String str12 : str9.split(";"))
/* 230:    */           {
/* 231:258 */             str12 = str12.trim();
/* 232:259 */             if (str12.length() > 0) {
/* 233:261 */               ((Set)localObject2).add(str12);
/* 234:    */             }
/* 235:    */           }
/* 236:266 */           allowedUserHosts = Collections.unmodifiableSet((Set)localObject2);
/* 237:    */         }
/* 238:    */       }
/* 239:272 */       Object localObject2 = paramServletConfig.getInitParameter("allowedBotHosts");
/* 240:273 */       Logger.logMessage("\"allowedBotHosts\" = \"" + (String)localObject2 + "\"");
/* 241:274 */       if (localObject2 != null) {
/* 242:276 */         if (!((String)localObject2).trim().equals("*"))
/* 243:    */         {
/* 244:278 */           ??? = new HashSet();
/* 245:279 */           for (String str13 : ((String)localObject2).split(";"))
/* 246:    */           {
/* 247:281 */             str13 = str13.trim();
/* 248:282 */             if (str13.length() > 0) {
/* 249:284 */               ((Set)???).add(str13);
/* 250:    */             }
/* 251:    */           }
/* 252:289 */           allowedBotHosts = Collections.unmodifiableSet((Set)???);
/* 253:    */         }
/* 254:    */       }
/* 255:294 */       ??? = paramServletConfig.getInitParameter("blacklistingPeriod");
/* 256:295 */       Logger.logMessage("\"blacklistingPeriod\" = \"" + (String)??? + "\"");
/* 257:    */       try
/* 258:    */       {
/* 259:298 */         blacklistingPeriod = Integer.parseInt((String)???);
/* 260:    */       }
/* 261:    */       catch (NumberFormatException localNumberFormatException8)
/* 262:    */       {
/* 263:302 */         blacklistingPeriod = 300000;
/* 264:303 */         Logger.logMessage("Invalid value for blacklistingPeriod " + (String)??? + ", using default " + blacklistingPeriod);
/* 265:    */       }
/* 266:307 */       String str10 = paramServletConfig.getInitParameter("communicationLoggingMask");
/* 267:308 */       Logger.logMessage("\"communicationLoggingMask\" = \"" + str10 + "\"");
/* 268:    */       try
/* 269:    */       {
/* 270:311 */         communicationLoggingMask = Integer.parseInt(str10);
/* 271:    */       }
/* 272:    */       catch (NumberFormatException localNumberFormatException9)
/* 273:    */       {
/* 274:314 */         Logger.logMessage("Invalid value for communicationLogginMask " + str10 + ", using default 0");
/* 275:    */       }
/* 276:317 */       String str11 = paramServletConfig.getInitParameter("sendToPeersLimit");
/* 277:318 */       Logger.logMessage("\"sendToPeersLimit\" = \"" + str11 + "\"");
/* 278:    */       try
/* 279:    */       {
/* 280:321 */         sendToPeersLimit = Integer.parseInt(str11);
/* 281:    */       }
/* 282:    */       catch (NumberFormatException localNumberFormatException10)
/* 283:    */       {
/* 284:324 */         sendToPeersLimit = 10;
/* 285:325 */         Logger.logMessage("Invalid value for sendToPeersLimit " + str11 + ", using default " + sendToPeersLimit);
/* 286:    */       }
/* 287:328 */       Blockchain.init();
/* 288:    */       
/* 289:330 */       ThreadPools.start();
/* 290:    */       
/* 291:332 */       Logger.logMessage("NRS 0.6.1 started successfully.");
/* 292:    */     }
/* 293:    */     catch (Exception localException)
/* 294:    */     {
/* 295:336 */       Logger.logMessage("Error initializing Nxt servlet", localException);
/* 296:337 */       System.exit(1);
/* 297:    */     }
/* 298:    */   }
/* 299:    */   
/* 300:    */   public void doGet(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
/* 301:    */     throws ServletException, IOException
/* 302:    */   {
/* 303:345 */     paramHttpServletResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
/* 304:346 */     paramHttpServletResponse.setHeader("Pragma", "no-cache");
/* 305:347 */     paramHttpServletResponse.setDateHeader("Expires", 0L);
/* 306:    */     
/* 307:349 */     User localUser = null;
/* 308:    */     try
/* 309:    */     {
/* 310:353 */       String str = paramHttpServletRequest.getParameter("user");
/* 311:355 */       if (str == null)
/* 312:    */       {
/* 313:356 */         HttpRequestHandler.process(paramHttpServletRequest, paramHttpServletResponse);
/* 314:357 */         return;
/* 315:    */       }
/* 316:360 */       if ((allowedUserHosts != null) && (!allowedUserHosts.contains(paramHttpServletRequest.getRemoteHost())))
/* 317:    */       {
/* 318:361 */         JSONObject localJSONObject1 = new JSONObject();
/* 319:362 */         localJSONObject1.put("response", "denyAccess");
/* 320:363 */         JSONArray localJSONArray = new JSONArray();
/* 321:364 */         localJSONArray.add(localJSONObject1);
/* 322:365 */         JSONObject localJSONObject2 = new JSONObject();
/* 323:366 */         localJSONObject2.put("responses", localJSONArray);
/* 324:367 */         paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
/* 325:368 */         PrintWriter localPrintWriter = paramHttpServletResponse.getWriter();Object localObject1 = null;
/* 326:    */         try
/* 327:    */         {
/* 328:369 */           localJSONObject2.writeJSONString(localPrintWriter);
/* 329:    */         }
/* 330:    */         catch (Throwable localThrowable2)
/* 331:    */         {
/* 332:368 */           localObject1 = localThrowable2;throw localThrowable2;
/* 333:    */         }
/* 334:    */         finally
/* 335:    */         {
/* 336:370 */           if (localPrintWriter != null) {
/* 337:370 */             if (localObject1 != null) {
/* 338:    */               try
/* 339:    */               {
/* 340:370 */                 localPrintWriter.close();
/* 341:    */               }
/* 342:    */               catch (Throwable localThrowable3)
/* 343:    */               {
/* 344:370 */                 localObject1.addSuppressed(localThrowable3);
/* 345:    */               }
/* 346:    */             } else {
/* 347:370 */               localPrintWriter.close();
/* 348:    */             }
/* 349:    */           }
/* 350:    */         }
/* 351:371 */         return;
/* 352:    */       }
/* 353:374 */       localUser = User.getUser(str);
/* 354:375 */       UserRequestHandler.process(paramHttpServletRequest, localUser);
/* 355:    */     }
/* 356:    */     catch (Exception localException)
/* 357:    */     {
/* 358:378 */       if (localUser != null) {
/* 359:379 */         Logger.logMessage("Error processing GET request", localException);
/* 360:    */       } else {
/* 361:381 */         Logger.logDebugMessage("Error processing GET request", localException);
/* 362:    */       }
/* 363:    */     }
/* 364:385 */     if (localUser != null) {
/* 365:386 */       localUser.processPendingResponses(paramHttpServletRequest, paramHttpServletResponse);
/* 366:    */     }
/* 367:    */   }
/* 368:    */   
/* 369:    */   public void doPost(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
/* 370:    */     throws ServletException, IOException
/* 371:    */   {
/* 372:393 */     HttpJSONRequestHandler.process(paramHttpServletRequest, paramHttpServletResponse);
/* 373:    */   }
/* 374:    */   
/* 375:    */   public void destroy()
/* 376:    */   {
/* 377:400 */     ThreadPools.shutdown();
/* 378:    */     
/* 379:402 */     Blockchain.shutdown();
/* 380:    */     
/* 381:404 */     Logger.logMessage("NRS 0.6.1 stopped.");
/* 382:    */   }
/* 383:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Nxt
 * JD-Core Version:    0.7.0.1
 */