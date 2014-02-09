/*   1:    */ package nxt.peer;
/*   2:    */ 
/*   3:    */ import java.io.BufferedReader;
/*   4:    */ import java.io.BufferedWriter;
/*   5:    */ import java.io.ByteArrayOutputStream;
/*   6:    */ import java.io.IOException;
/*   7:    */ import java.io.InputStream;
/*   8:    */ import java.io.InputStreamReader;
/*   9:    */ import java.io.OutputStreamWriter;
/*  10:    */ import java.io.Reader;
/*  11:    */ import java.io.StringWriter;
/*  12:    */ import java.io.UnsupportedEncodingException;
/*  13:    */ import java.io.Writer;
/*  14:    */ import java.net.HttpURLConnection;
/*  15:    */ import java.net.MalformedURLException;
/*  16:    */ import java.net.SocketException;
/*  17:    */ import java.net.SocketTimeoutException;
/*  18:    */ import java.net.URL;
/*  19:    */ import java.net.UnknownHostException;
/*  20:    */ import java.nio.ByteBuffer;
/*  21:    */ import java.nio.ByteOrder;
/*  22:    */ import java.util.ArrayList;
/*  23:    */ import java.util.Collection;
/*  24:    */ import java.util.Collections;
/*  25:    */ import java.util.Iterator;
/*  26:    */ import java.util.LinkedList;
/*  27:    */ import java.util.List;
/*  28:    */ import java.util.Set;
/*  29:    */ import java.util.concurrent.ConcurrentHashMap;
/*  30:    */ import java.util.concurrent.ConcurrentMap;
/*  31:    */ import java.util.concurrent.ExecutionException;
/*  32:    */ import java.util.concurrent.Future;
/*  33:    */ import java.util.concurrent.ThreadLocalRandom;
/*  34:    */ import java.util.concurrent.atomic.AtomicInteger;
/*  35:    */ import nxt.Account;
/*  36:    */ import nxt.Nxt;
/*  37:    */ import nxt.NxtException.ValidationException;
/*  38:    */ import nxt.ThreadPools;
/*  39:    */ import nxt.Transaction.NotYetEnabledException;
/*  40:    */ import nxt.crypto.Crypto;
/*  41:    */ import nxt.user.User;
/*  42:    */ import nxt.util.Convert;
/*  43:    */ import nxt.util.CountingInputStream;
/*  44:    */ import nxt.util.CountingOutputStream;
/*  45:    */ import nxt.util.JSON;
/*  46:    */ import nxt.util.Logger;
/*  47:    */ import org.json.simple.JSONArray;
/*  48:    */ import org.json.simple.JSONObject;
/*  49:    */ import org.json.simple.JSONStreamAware;
/*  50:    */ import org.json.simple.JSONValue;
/*  51:    */ 
/*  52:    */ public final class Peer
/*  53:    */   implements Comparable<Peer>
/*  54:    */ {
/*  55:    */   public static enum State
/*  56:    */   {
/*  57: 54 */     NON_CONNECTED,  CONNECTED,  DISCONNECTED;
/*  58:    */     
/*  59:    */     private State() {}
/*  60:    */   }
/*  61:    */   
/*  62: 57 */   private static final AtomicInteger peerCounter = new AtomicInteger();
/*  63: 58 */   private static final ConcurrentMap<String, Peer> peers = new ConcurrentHashMap();
/*  64: 59 */   private static final Collection<Peer> allPeers = Collections.unmodifiableCollection(peers.values());
/*  65: 61 */   public static final Runnable peerConnectingThread = new Runnable()
/*  66:    */   {
/*  67:    */     public void run()
/*  68:    */     {
/*  69:    */       try
/*  70:    */       {
/*  71:    */         try
/*  72:    */         {
/*  73: 69 */           if (Peer.access$000() < Nxt.maxNumberOfConnectedPublicPeers)
/*  74:    */           {
/*  75: 71 */             Peer localPeer = Peer.getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? Peer.State.NON_CONNECTED : Peer.State.DISCONNECTED, false);
/*  76: 72 */             if (localPeer != null) {
/*  77: 74 */               localPeer.connect();
/*  78:    */             }
/*  79:    */           }
/*  80:    */         }
/*  81:    */         catch (Exception localException)
/*  82:    */         {
/*  83: 81 */           Logger.logDebugMessage("Error connecting to peer", localException);
/*  84:    */         }
/*  85:    */       }
/*  86:    */       catch (Throwable localThrowable)
/*  87:    */       {
/*  88: 84 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  89: 85 */         localThrowable.printStackTrace();
/*  90: 86 */         System.exit(1);
/*  91:    */       }
/*  92:    */     }
/*  93:    */   };
/*  94: 93 */   public static final Runnable peerUnBlacklistingThread = new Runnable()
/*  95:    */   {
/*  96:    */     public void run()
/*  97:    */     {
/*  98:    */       try
/*  99:    */       {
/* 100:    */         try
/* 101:    */         {
/* 102:101 */           l = System.currentTimeMillis();
/* 103:103 */           for (Peer localPeer : Peer.peers.values()) {
/* 104:105 */             if ((localPeer.blacklistingTime > 0L) && (localPeer.blacklistingTime + Nxt.blacklistingPeriod <= l)) {
/* 105:107 */               localPeer.removeBlacklistedStatus();
/* 106:    */             }
/* 107:    */           }
/* 108:    */         }
/* 109:    */         catch (Exception localException)
/* 110:    */         {
/* 111:    */           long l;
/* 112:114 */           Logger.logDebugMessage("Error un-blacklisting peer", localException);
/* 113:    */         }
/* 114:    */       }
/* 115:    */       catch (Throwable localThrowable)
/* 116:    */       {
/* 117:117 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/* 118:118 */         localThrowable.printStackTrace();
/* 119:119 */         System.exit(1);
/* 120:    */       }
/* 121:    */     }
/* 122:    */   };
/* 123:126 */   public static final Runnable getMorePeersThread = new Runnable()
/* 124:    */   {
/* 125:    */     private final JSONStreamAware getPeersRequest;
/* 126:    */     
/* 127:    */     public void run()
/* 128:    */     {
/* 129:    */       try
/* 130:    */       {
/* 131:    */         try
/* 132:    */         {
/* 133:140 */           Peer localPeer = Peer.getAnyPeer(Peer.State.CONNECTED, true);
/* 134:141 */           if (localPeer != null)
/* 135:    */           {
/* 136:142 */             JSONObject localJSONObject = localPeer.send(this.getPeersRequest);
/* 137:143 */             if (localJSONObject != null)
/* 138:    */             {
/* 139:144 */               JSONArray localJSONArray = (JSONArray)localJSONObject.get("peers");
/* 140:145 */               for (Object localObject : localJSONArray)
/* 141:    */               {
/* 142:146 */                 String str = ((String)localObject).trim();
/* 143:147 */                 if (str.length() > 0) {
/* 144:148 */                   Peer.addPeer(str, str);
/* 145:    */                 }
/* 146:    */               }
/* 147:    */             }
/* 148:    */           }
/* 149:    */         }
/* 150:    */         catch (Exception localException)
/* 151:    */         {
/* 152:155 */           Logger.logDebugMessage("Error requesting peers from a peer", localException);
/* 153:    */         }
/* 154:    */       }
/* 155:    */       catch (Throwable localThrowable)
/* 156:    */       {
/* 157:158 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/* 158:159 */         localThrowable.printStackTrace();
/* 159:160 */         System.exit(1);
/* 160:    */       }
/* 161:    */     }
/* 162:    */   };
/* 163:    */   private final int index;
/* 164:    */   private final String peerAddress;
/* 165:    */   private String announcedAddress;
/* 166:    */   private boolean shareAddress;
/* 167:    */   private String hallmark;
/* 168:    */   private String platform;
/* 169:    */   private String application;
/* 170:    */   private String version;
/* 171:    */   private int weight;
/* 172:    */   private int date;
/* 173:    */   private Long accountId;
/* 174:    */   private long adjustedWeight;
/* 175:    */   private volatile long blacklistingTime;
/* 176:    */   private volatile State state;
/* 177:    */   private volatile long downloadedVolume;
/* 178:    */   private volatile long uploadedVolume;
/* 179:    */   
/* 180:    */   public static Collection<Peer> getAllPeers()
/* 181:    */   {
/* 182:168 */     return allPeers;
/* 183:    */   }
/* 184:    */   
/* 185:    */   public static Peer getPeer(String paramString)
/* 186:    */   {
/* 187:172 */     return (Peer)peers.get(paramString);
/* 188:    */   }
/* 189:    */   
/* 190:    */   public static Peer addPeer(String paramString1, String paramString2)
/* 191:    */   {
/* 192:    */     try
/* 193:    */     {
/* 194:178 */       new URL("http://" + paramString1);
/* 195:    */     }
/* 196:    */     catch (MalformedURLException localMalformedURLException1)
/* 197:    */     {
/* 198:180 */       Logger.logDebugMessage("malformed peer address " + paramString1, localMalformedURLException1);
/* 199:181 */       return null;
/* 200:    */     }
/* 201:    */     try
/* 202:    */     {
/* 203:185 */       new URL("http://" + paramString2);
/* 204:    */     }
/* 205:    */     catch (MalformedURLException localMalformedURLException2)
/* 206:    */     {
/* 207:187 */       Logger.logDebugMessage("malformed peer announced address " + paramString2, localMalformedURLException2);
/* 208:188 */       paramString2 = "";
/* 209:    */     }
/* 210:191 */     if ((paramString1.equals("localhost")) || (paramString1.equals("127.0.0.1")) || (paramString1.equals("0:0:0:0:0:0:0:1"))) {
/* 211:192 */       return null;
/* 212:    */     }
/* 213:195 */     if ((Nxt.myAddress != null) && (Nxt.myAddress.length() > 0) && (Nxt.myAddress.equalsIgnoreCase(paramString2))) {
/* 214:196 */       return null;
/* 215:    */     }
/* 216:199 */     String str = paramString2.length() > 0 ? paramString2 : paramString1;
/* 217:200 */     Peer localPeer = (Peer)peers.get(str);
/* 218:201 */     if (localPeer == null)
/* 219:    */     {
/* 220:202 */       localPeer = new Peer(str, paramString2);
/* 221:203 */       peers.put(str, localPeer);
/* 222:    */     }
/* 223:206 */     return localPeer;
/* 224:    */   }
/* 225:    */   
/* 226:    */   public static void updatePeerWeights(Account paramAccount)
/* 227:    */   {
/* 228:210 */     for (Peer localPeer : peers.values()) {
/* 229:211 */       if ((paramAccount.getId().equals(localPeer.accountId)) && (localPeer.adjustedWeight > 0L)) {
/* 230:212 */         localPeer.updateWeight();
/* 231:    */       }
/* 232:    */     }
/* 233:    */   }
/* 234:    */   
/* 235:    */   public static void sendToSomePeers(JSONObject paramJSONObject)
/* 236:    */   {
/* 237:219 */     JSONStreamAware localJSONStreamAware = JSON.prepareRequest(paramJSONObject);
/* 238:    */     
/* 239:221 */     int i = 0;
/* 240:222 */     ArrayList localArrayList = new ArrayList();
/* 241:223 */     for (Peer localPeer : peers.values()) {
/* 242:225 */       if ((!Nxt.enableHallmarkProtection) || (localPeer.getWeight() >= Nxt.pushThreshold))
/* 243:    */       {
/* 244:    */         Object localObject;
/* 245:229 */         if ((localPeer.blacklistingTime == 0L) && (localPeer.state == State.CONNECTED) && (localPeer.announcedAddress.length() > 0))
/* 246:    */         {
/* 247:230 */           localObject = ThreadPools.sendInParallel(localPeer, localJSONStreamAware);
/* 248:231 */           localArrayList.add(localObject);
/* 249:    */         }
/* 250:233 */         if (localArrayList.size() >= Nxt.sendToPeersLimit - i)
/* 251:    */         {
/* 252:234 */           for (localObject = localArrayList.iterator(); ((Iterator)localObject).hasNext();)
/* 253:    */           {
/* 254:234 */             Future localFuture = (Future)((Iterator)localObject).next();
/* 255:    */             try
/* 256:    */             {
/* 257:236 */               JSONObject localJSONObject = (JSONObject)localFuture.get();
/* 258:237 */               if ((localJSONObject != null) && (localJSONObject.get("error") == null)) {
/* 259:238 */                 i++;
/* 260:    */               }
/* 261:    */             }
/* 262:    */             catch (InterruptedException localInterruptedException)
/* 263:    */             {
/* 264:241 */               Thread.currentThread().interrupt();
/* 265:    */             }
/* 266:    */             catch (ExecutionException localExecutionException)
/* 267:    */             {
/* 268:243 */               Logger.logDebugMessage("Error in sendToSomePeers", localExecutionException);
/* 269:    */             }
/* 270:    */           }
/* 271:247 */           localArrayList.clear();
/* 272:    */         }
/* 273:249 */         if (i >= Nxt.sendToPeersLimit) {
/* 274:250 */           return;
/* 275:    */         }
/* 276:    */       }
/* 277:    */     }
/* 278:    */   }
/* 279:    */   
/* 280:    */   public static Peer getAnyPeer(State paramState, boolean paramBoolean)
/* 281:    */   {
/* 282:259 */     ArrayList localArrayList = new ArrayList();
/* 283:260 */     for (Peer localPeer1 : peers.values()) {
/* 284:261 */       if ((localPeer1.blacklistingTime <= 0L) && (localPeer1.state == paramState) && (localPeer1.announcedAddress.length() > 0) && ((!paramBoolean) || (!Nxt.enableHallmarkProtection) || (localPeer1.getWeight() >= Nxt.pullThreshold))) {
/* 285:263 */         localArrayList.add(localPeer1);
/* 286:    */       }
/* 287:    */     }
/* 288:    */     long l2;
/* 289:267 */     if (localArrayList.size() > 0)
/* 290:    */     {
/* 291:268 */       long l1 = 0L;
/* 292:269 */       for (Peer localPeer2 : localArrayList)
/* 293:    */       {
/* 294:270 */         long l3 = localPeer2.getWeight();
/* 295:271 */         if (l3 == 0L) {
/* 296:272 */           l3 = 1L;
/* 297:    */         }
/* 298:274 */         l1 += l3;
/* 299:    */       }
/* 300:277 */       l2 = ThreadLocalRandom.current().nextLong(l1);
/* 301:278 */       for (Peer localPeer3 : localArrayList)
/* 302:    */       {
/* 303:279 */         long l4 = localPeer3.getWeight();
/* 304:280 */         if (l4 == 0L) {
/* 305:281 */           l4 = 1L;
/* 306:    */         }
/* 307:283 */         if (l2 -= l4 < 0L) {
/* 308:284 */           return localPeer3;
/* 309:    */         }
/* 310:    */       }
/* 311:    */     }
/* 312:288 */     return null;
/* 313:    */   }
/* 314:    */   
/* 315:    */   private static int getNumberOfConnectedPublicPeers()
/* 316:    */   {
/* 317:292 */     int i = 0;
/* 318:293 */     for (Peer localPeer : peers.values()) {
/* 319:294 */       if ((localPeer.state == State.CONNECTED) && (localPeer.announcedAddress.length() > 0)) {
/* 320:295 */         i++;
/* 321:    */       }
/* 322:    */     }
/* 323:298 */     return i;
/* 324:    */   }
/* 325:    */   
/* 326:    */   private static String truncate(String paramString, int paramInt, boolean paramBoolean)
/* 327:    */   {
/* 328:302 */     return paramString.length() > paramInt ? paramString.substring(0, paramBoolean ? paramInt - 3 : paramInt) + (paramBoolean ? "..." : "") : paramString == null ? "?" : paramString;
/* 329:    */   }
/* 330:    */   
/* 331:    */   private Peer(String paramString1, String paramString2)
/* 332:    */   {
/* 333:325 */     this.peerAddress = paramString1;
/* 334:326 */     this.announcedAddress = paramString2;
/* 335:327 */     this.index = peerCounter.incrementAndGet();
/* 336:328 */     this.state = State.NON_CONNECTED;
/* 337:    */   }
/* 338:    */   
/* 339:    */   public int getIndex()
/* 340:    */   {
/* 341:332 */     return this.index;
/* 342:    */   }
/* 343:    */   
/* 344:    */   public String getPeerAddress()
/* 345:    */   {
/* 346:336 */     return this.peerAddress;
/* 347:    */   }
/* 348:    */   
/* 349:    */   public State getState()
/* 350:    */   {
/* 351:340 */     return this.state;
/* 352:    */   }
/* 353:    */   
/* 354:    */   public long getBlacklistingTime()
/* 355:    */   {
/* 356:344 */     return this.blacklistingTime;
/* 357:    */   }
/* 358:    */   
/* 359:    */   public long getDownloadedVolume()
/* 360:    */   {
/* 361:348 */     return this.downloadedVolume;
/* 362:    */   }
/* 363:    */   
/* 364:    */   public long getUploadedVolume()
/* 365:    */   {
/* 366:352 */     return this.uploadedVolume;
/* 367:    */   }
/* 368:    */   
/* 369:    */   public String getVersion()
/* 370:    */   {
/* 371:356 */     return this.version;
/* 372:    */   }
/* 373:    */   
/* 374:    */   void setVersion(String paramString)
/* 375:    */   {
/* 376:360 */     this.version = paramString;
/* 377:    */   }
/* 378:    */   
/* 379:    */   public String getApplication()
/* 380:    */   {
/* 381:364 */     return this.application;
/* 382:    */   }
/* 383:    */   
/* 384:    */   void setApplication(String paramString)
/* 385:    */   {
/* 386:368 */     this.application = paramString;
/* 387:    */   }
/* 388:    */   
/* 389:    */   public String getPlatform()
/* 390:    */   {
/* 391:372 */     return this.platform;
/* 392:    */   }
/* 393:    */   
/* 394:    */   void setPlatform(String paramString)
/* 395:    */   {
/* 396:376 */     this.platform = paramString;
/* 397:    */   }
/* 398:    */   
/* 399:    */   public String getHallmark()
/* 400:    */   {
/* 401:380 */     return this.hallmark;
/* 402:    */   }
/* 403:    */   
/* 404:    */   public boolean shareAddress()
/* 405:    */   {
/* 406:384 */     return this.shareAddress;
/* 407:    */   }
/* 408:    */   
/* 409:    */   void setShareAddress(boolean paramBoolean)
/* 410:    */   {
/* 411:388 */     this.shareAddress = paramBoolean;
/* 412:    */   }
/* 413:    */   
/* 414:    */   public String getAnnouncedAddress()
/* 415:    */   {
/* 416:392 */     return this.announcedAddress;
/* 417:    */   }
/* 418:    */   
/* 419:    */   void setAnnouncedAddress(String paramString)
/* 420:    */   {
/* 421:    */     try
/* 422:    */     {
/* 423:397 */       new URL("http://" + paramString);
/* 424:    */     }
/* 425:    */     catch (MalformedURLException localMalformedURLException)
/* 426:    */     {
/* 427:400 */       paramString = "";
/* 428:    */     }
/* 429:402 */     this.announcedAddress = paramString;
/* 430:    */   }
/* 431:    */   
/* 432:    */   public int compareTo(Peer paramPeer)
/* 433:    */   {
/* 434:407 */     long l1 = getWeight();long l2 = paramPeer.getWeight();
/* 435:408 */     if (l1 > l2) {
/* 436:409 */       return -1;
/* 437:    */     }
/* 438:410 */     if (l1 < l2) {
/* 439:411 */       return 1;
/* 440:    */     }
/* 441:413 */     return this.index - paramPeer.index;
/* 442:    */   }
/* 443:    */   
/* 444:    */   public void blacklist(NxtException.ValidationException paramValidationException)
/* 445:    */   {
/* 446:418 */     if ((paramValidationException instanceof Transaction.NotYetEnabledException)) {
/* 447:421 */       return;
/* 448:    */     }
/* 449:423 */     Logger.logDebugMessage("Blacklisting " + this.peerAddress + " because of: " + paramValidationException.getMessage(), paramValidationException);
/* 450:424 */     blacklist();
/* 451:    */   }
/* 452:    */   
/* 453:    */   public void blacklist()
/* 454:    */   {
/* 455:429 */     this.blacklistingTime = System.currentTimeMillis();
/* 456:    */     
/* 457:431 */     JSONObject localJSONObject1 = new JSONObject();
/* 458:432 */     localJSONObject1.put("response", "processNewData");
/* 459:    */     
/* 460:434 */     JSONArray localJSONArray1 = new JSONArray();
/* 461:435 */     JSONObject localJSONObject2 = new JSONObject();
/* 462:436 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 463:437 */     localJSONArray1.add(localJSONObject2);
/* 464:438 */     localJSONObject1.put("removedKnownPeers", localJSONArray1);
/* 465:    */     
/* 466:440 */     JSONArray localJSONArray2 = new JSONArray();
/* 467:441 */     JSONObject localJSONObject3 = new JSONObject();
/* 468:442 */     localJSONObject3.put("index", Integer.valueOf(this.index));
/* 469:443 */     localJSONObject3.put("announcedAddress", truncate(this.announcedAddress, 25, true));
/* 470:444 */     if (Nxt.wellKnownPeers.contains(this.announcedAddress)) {
/* 471:445 */       localJSONObject3.put("wellKnown", Boolean.valueOf(true));
/* 472:    */     }
/* 473:447 */     localJSONArray2.add(localJSONObject3);
/* 474:448 */     localJSONObject1.put("addedBlacklistedPeers", localJSONArray2);
/* 475:    */     
/* 476:450 */     User.sendToAll(localJSONObject1);
/* 477:    */   }
/* 478:    */   
/* 479:    */   public void deactivate()
/* 480:    */   {
/* 481:455 */     if (this.state == State.CONNECTED) {
/* 482:456 */       setState(State.DISCONNECTED);
/* 483:    */     }
/* 484:458 */     setState(State.NON_CONNECTED);
/* 485:    */     
/* 486:460 */     JSONObject localJSONObject1 = new JSONObject();
/* 487:461 */     localJSONObject1.put("response", "processNewData");
/* 488:    */     
/* 489:463 */     JSONArray localJSONArray1 = new JSONArray();
/* 490:464 */     JSONObject localJSONObject2 = new JSONObject();
/* 491:465 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 492:466 */     localJSONArray1.add(localJSONObject2);
/* 493:467 */     localJSONObject1.put("removedActivePeers", localJSONArray1);
/* 494:469 */     if (this.announcedAddress.length() > 0)
/* 495:    */     {
/* 496:470 */       JSONArray localJSONArray2 = new JSONArray();
/* 497:471 */       JSONObject localJSONObject3 = new JSONObject();
/* 498:472 */       localJSONObject3.put("index", Integer.valueOf(this.index));
/* 499:473 */       localJSONObject3.put("announcedAddress", truncate(this.announcedAddress, 25, true));
/* 500:474 */       if (Nxt.wellKnownPeers.contains(this.announcedAddress)) {
/* 501:475 */         localJSONObject3.put("wellKnown", Boolean.valueOf(true));
/* 502:    */       }
/* 503:477 */       localJSONArray2.add(localJSONObject3);
/* 504:478 */       localJSONObject1.put("addedKnownPeers", localJSONArray2);
/* 505:    */     }
/* 506:481 */     User.sendToAll(localJSONObject1);
/* 507:    */   }
/* 508:    */   
/* 509:    */   public int getWeight()
/* 510:    */   {
/* 511:486 */     if (this.accountId == null) {
/* 512:487 */       return 0;
/* 513:    */     }
/* 514:489 */     Account localAccount = Account.getAccount(this.accountId);
/* 515:490 */     if (localAccount == null) {
/* 516:491 */       return 0;
/* 517:    */     }
/* 518:493 */     return (int)(this.adjustedWeight * (localAccount.getBalance() / 100L) / 1000000000L);
/* 519:    */   }
/* 520:    */   
/* 521:    */   public String getSoftware()
/* 522:    */   {
/* 523:497 */     StringBuilder localStringBuilder = new StringBuilder();
/* 524:498 */     localStringBuilder.append(truncate(this.application, 10, false));
/* 525:499 */     localStringBuilder.append(" (");
/* 526:500 */     localStringBuilder.append(truncate(this.version, 10, false));
/* 527:501 */     localStringBuilder.append(")").append(" @ ");
/* 528:502 */     localStringBuilder.append(truncate(this.platform, 10, false));
/* 529:503 */     return localStringBuilder.toString();
/* 530:    */   }
/* 531:    */   
/* 532:    */   public void removeBlacklistedStatus()
/* 533:    */   {
/* 534:508 */     setState(State.NON_CONNECTED);
/* 535:509 */     this.blacklistingTime = 0L;
/* 536:    */     
/* 537:511 */     JSONObject localJSONObject1 = new JSONObject();
/* 538:512 */     localJSONObject1.put("response", "processNewData");
/* 539:    */     
/* 540:514 */     JSONArray localJSONArray1 = new JSONArray();
/* 541:515 */     JSONObject localJSONObject2 = new JSONObject();
/* 542:516 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 543:517 */     localJSONArray1.add(localJSONObject2);
/* 544:518 */     localJSONObject1.put("removedBlacklistedPeers", localJSONArray1);
/* 545:    */     
/* 546:520 */     JSONArray localJSONArray2 = new JSONArray();
/* 547:521 */     JSONObject localJSONObject3 = new JSONObject();
/* 548:522 */     localJSONObject3.put("index", Integer.valueOf(this.index));
/* 549:523 */     localJSONObject3.put("announcedAddress", truncate(this.announcedAddress, 25, true));
/* 550:524 */     if (Nxt.wellKnownPeers.contains(this.announcedAddress)) {
/* 551:525 */       localJSONObject3.put("wellKnown", Boolean.valueOf(true));
/* 552:    */     }
/* 553:527 */     localJSONArray2.add(localJSONObject3);
/* 554:528 */     localJSONObject1.put("addedKnownPeers", localJSONArray2);
/* 555:    */     
/* 556:530 */     User.sendToAll(localJSONObject1);
/* 557:    */   }
/* 558:    */   
/* 559:    */   public void removePeer()
/* 560:    */   {
/* 561:536 */     peers.values().remove(this);
/* 562:    */     
/* 563:538 */     JSONObject localJSONObject1 = new JSONObject();
/* 564:539 */     localJSONObject1.put("response", "processNewData");
/* 565:    */     
/* 566:541 */     JSONArray localJSONArray = new JSONArray();
/* 567:542 */     JSONObject localJSONObject2 = new JSONObject();
/* 568:543 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 569:544 */     localJSONArray.add(localJSONObject2);
/* 570:545 */     localJSONObject1.put("removedKnownPeers", localJSONArray);
/* 571:    */     
/* 572:547 */     User.sendToAll(localJSONObject1);
/* 573:    */   }
/* 574:    */   
/* 575:    */   public JSONObject send(JSONStreamAware paramJSONStreamAware)
/* 576:    */   {
/* 577:555 */     String str = null;
/* 578:556 */     int i = 0;
/* 579:    */     
/* 580:558 */     HttpURLConnection localHttpURLConnection = null;
/* 581:    */     JSONObject localJSONObject;
/* 582:    */     try
/* 583:    */     {
/* 584:562 */       if (Nxt.communicationLoggingMask != 0)
/* 585:    */       {
/* 586:564 */         localObject1 = new StringWriter();
/* 587:565 */         paramJSONStreamAware.writeJSONString((Writer)localObject1);
/* 588:566 */         str = "\"" + this.announcedAddress + "\": " + ((StringWriter)localObject1).toString();
/* 589:    */       }
/* 590:570 */       Object localObject1 = new URL("http://" + this.announcedAddress + (new URL("http://" + this.announcedAddress).getPort() < 0 ? ":7874" : "") + "/nxt");
/* 591:    */       
/* 592:572 */       localHttpURLConnection = (HttpURLConnection)((URL)localObject1).openConnection();
/* 593:573 */       localHttpURLConnection.setRequestMethod("POST");
/* 594:574 */       localHttpURLConnection.setDoOutput(true);
/* 595:575 */       localHttpURLConnection.setConnectTimeout(Nxt.connectTimeout);
/* 596:576 */       localHttpURLConnection.setReadTimeout(Nxt.readTimeout);
/* 597:    */       
/* 598:578 */       CountingOutputStream localCountingOutputStream = new CountingOutputStream(localHttpURLConnection.getOutputStream());
/* 599:579 */       Object localObject2 = new BufferedWriter(new OutputStreamWriter(localCountingOutputStream, "UTF-8"));Object localObject3 = null;
/* 600:    */       try
/* 601:    */       {
/* 602:580 */         paramJSONStreamAware.writeJSONString((Writer)localObject2);
/* 603:    */       }
/* 604:    */       catch (Throwable localThrowable2)
/* 605:    */       {
/* 606:579 */         localObject3 = localThrowable2;throw localThrowable2;
/* 607:    */       }
/* 608:    */       finally
/* 609:    */       {
/* 610:581 */         if (localObject2 != null) {
/* 611:581 */           if (localObject3 != null) {
/* 612:    */             try
/* 613:    */             {
/* 614:581 */               ((Writer)localObject2).close();
/* 615:    */             }
/* 616:    */             catch (Throwable localThrowable5)
/* 617:    */             {
/* 618:581 */               ((Throwable)localObject3).addSuppressed(localThrowable5);
/* 619:    */             }
/* 620:    */           } else {
/* 621:581 */             ((Writer)localObject2).close();
/* 622:    */           }
/* 623:    */         }
/* 624:    */       }
/* 625:582 */       updateUploadedVolume(localCountingOutputStream.getCount());
/* 626:584 */       if (localHttpURLConnection.getResponseCode() == 200)
/* 627:    */       {
/* 628:586 */         if ((Nxt.communicationLoggingMask & 0x4) != 0)
/* 629:    */         {
/* 630:589 */           localObject2 = new ByteArrayOutputStream();
/* 631:590 */           localObject3 = new byte[65536];
/* 632:    */           
/* 633:592 */           Object localObject6 = localHttpURLConnection.getInputStream();Object localObject7 = null;
/* 634:    */           try
/* 635:    */           {
/* 636:    */             int j;
/* 637:593 */             while ((j = ((InputStream)localObject6).read((byte[])localObject3)) > 0) {
/* 638:594 */               ((ByteArrayOutputStream)localObject2).write((byte[])localObject3, 0, j);
/* 639:    */             }
/* 640:    */           }
/* 641:    */           catch (Throwable localThrowable7)
/* 642:    */           {
/* 643:592 */             localObject7 = localThrowable7;throw localThrowable7;
/* 644:    */           }
/* 645:    */           finally
/* 646:    */           {
/* 647:596 */             if (localObject6 != null) {
/* 648:596 */               if (localObject7 != null) {
/* 649:    */                 try
/* 650:    */                 {
/* 651:596 */                   ((InputStream)localObject6).close();
/* 652:    */                 }
/* 653:    */                 catch (Throwable localThrowable8)
/* 654:    */                 {
/* 655:596 */                   localObject7.addSuppressed(localThrowable8);
/* 656:    */                 }
/* 657:    */               } else {
/* 658:596 */                 ((InputStream)localObject6).close();
/* 659:    */               }
/* 660:    */             }
/* 661:    */           }
/* 662:597 */           localObject6 = ((ByteArrayOutputStream)localObject2).toString("UTF-8");
/* 663:598 */           str = str + " >>> " + (String)localObject6;
/* 664:599 */           i = 1;
/* 665:600 */           updateDownloadedVolume(((String)localObject6).getBytes("UTF-8").length);
/* 666:601 */           localJSONObject = (JSONObject)JSONValue.parse((String)localObject6);
/* 667:    */         }
/* 668:    */         else
/* 669:    */         {
/* 670:605 */           localObject2 = new CountingInputStream(localHttpURLConnection.getInputStream());
/* 671:    */           
/* 672:607 */           localObject3 = new BufferedReader(new InputStreamReader((InputStream)localObject2, "UTF-8"));Object localObject4 = null;
/* 673:    */           try
/* 674:    */           {
/* 675:608 */             localJSONObject = (JSONObject)JSONValue.parse((Reader)localObject3);
/* 676:    */           }
/* 677:    */           catch (Throwable localThrowable4)
/* 678:    */           {
/* 679:607 */             localObject4 = localThrowable4;throw localThrowable4;
/* 680:    */           }
/* 681:    */           finally
/* 682:    */           {
/* 683:609 */             if (localObject3 != null) {
/* 684:609 */               if (localObject4 != null) {
/* 685:    */                 try
/* 686:    */                 {
/* 687:609 */                   ((Reader)localObject3).close();
/* 688:    */                 }
/* 689:    */                 catch (Throwable localThrowable9)
/* 690:    */                 {
/* 691:609 */                   localObject4.addSuppressed(localThrowable9);
/* 692:    */                 }
/* 693:    */               } else {
/* 694:609 */                 ((Reader)localObject3).close();
/* 695:    */               }
/* 696:    */             }
/* 697:    */           }
/* 698:611 */           updateDownloadedVolume(((CountingInputStream)localObject2).getCount());
/* 699:    */         }
/* 700:    */       }
/* 701:    */       else
/* 702:    */       {
/* 703:617 */         if ((Nxt.communicationLoggingMask & 0x2) != 0)
/* 704:    */         {
/* 705:619 */           str = str + " >>> Peer responded with HTTP " + localHttpURLConnection.getResponseCode() + " code!";
/* 706:620 */           i = 1;
/* 707:    */         }
/* 708:624 */         setState(State.DISCONNECTED);
/* 709:    */         
/* 710:626 */         localJSONObject = null;
/* 711:    */       }
/* 712:    */     }
/* 713:    */     catch (RuntimeException|IOException localRuntimeException)
/* 714:    */     {
/* 715:632 */       if ((!(localRuntimeException instanceof UnknownHostException)) && (!(localRuntimeException instanceof SocketTimeoutException)) && (!(localRuntimeException instanceof SocketException))) {
/* 716:633 */         Logger.logDebugMessage("Error sending JSON request", localRuntimeException);
/* 717:    */       }
/* 718:636 */       if ((Nxt.communicationLoggingMask & 0x1) != 0)
/* 719:    */       {
/* 720:638 */         str = str + " >>> " + localRuntimeException.toString();
/* 721:639 */         i = 1;
/* 722:    */       }
/* 723:643 */       if (this.state == State.NON_CONNECTED) {
/* 724:645 */         blacklist();
/* 725:    */       } else {
/* 726:649 */         setState(State.DISCONNECTED);
/* 727:    */       }
/* 728:653 */       localJSONObject = null;
/* 729:    */     }
/* 730:657 */     if (i != 0) {
/* 731:659 */       Logger.logMessage(str + "\n");
/* 732:    */     }
/* 733:663 */     if (localHttpURLConnection != null) {
/* 734:665 */       localHttpURLConnection.disconnect();
/* 735:    */     }
/* 736:669 */     return localJSONObject;
/* 737:    */   }
/* 738:    */   
/* 739:    */   boolean analyzeHallmark(String paramString1, String paramString2)
/* 740:    */   {
/* 741:675 */     if ((paramString2 == null) || (paramString2.equals(this.hallmark))) {
/* 742:676 */       return true;
/* 743:    */     }
/* 744:    */     try
/* 745:    */     {
/* 746:    */       byte[] arrayOfByte1;
/* 747:    */       try
/* 748:    */       {
/* 749:682 */         arrayOfByte1 = Convert.convert(paramString2);
/* 750:    */       }
/* 751:    */       catch (NumberFormatException localNumberFormatException)
/* 752:    */       {
/* 753:684 */         return false;
/* 754:    */       }
/* 755:686 */       ByteBuffer localByteBuffer = ByteBuffer.wrap(arrayOfByte1);
/* 756:687 */       localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/* 757:688 */       byte[] arrayOfByte2 = new byte[32];
/* 758:689 */       localByteBuffer.get(arrayOfByte2);
/* 759:690 */       int i = localByteBuffer.getShort();
/* 760:691 */       if (i > 300) {
/* 761:692 */         return false;
/* 762:    */       }
/* 763:694 */       byte[] arrayOfByte3 = new byte[i];
/* 764:695 */       localByteBuffer.get(arrayOfByte3);
/* 765:696 */       String str = new String(arrayOfByte3, "UTF-8");
/* 766:697 */       if ((str.length() > 100) || (!str.equals(paramString1))) {
/* 767:698 */         return false;
/* 768:    */       }
/* 769:700 */       int j = localByteBuffer.getInt();
/* 770:701 */       if ((j <= 0) || (j > 1000000000L)) {
/* 771:702 */         return false;
/* 772:    */       }
/* 773:704 */       int k = localByteBuffer.getInt();
/* 774:705 */       localByteBuffer.get();
/* 775:706 */       byte[] arrayOfByte4 = new byte[64];
/* 776:707 */       localByteBuffer.get(arrayOfByte4);
/* 777:    */       
/* 778:709 */       byte[] arrayOfByte5 = new byte[arrayOfByte1.length - 64];
/* 779:710 */       System.arraycopy(arrayOfByte1, 0, arrayOfByte5, 0, arrayOfByte5.length);
/* 780:712 */       if (Crypto.verify(arrayOfByte4, arrayOfByte5, arrayOfByte2))
/* 781:    */       {
/* 782:713 */         this.hallmark = paramString2;
/* 783:714 */         Long localLong = Account.getId(arrayOfByte2);
/* 784:715 */         LinkedList localLinkedList = new LinkedList();
/* 785:716 */         int m = 0;
/* 786:717 */         this.accountId = localLong;
/* 787:718 */         this.weight = j;
/* 788:719 */         this.date = k;
/* 789:720 */         for (Peer localPeer1 : peers.values()) {
/* 790:721 */           if (localLong.equals(localPeer1.accountId))
/* 791:    */           {
/* 792:722 */             localLinkedList.add(localPeer1);
/* 793:723 */             if (localPeer1.date > m) {
/* 794:724 */               m = localPeer1.date;
/* 795:    */             }
/* 796:    */           }
/* 797:    */         }
/* 798:729 */         long l = 0L;
/* 799:730 */         for (Iterator localIterator2 = localLinkedList.iterator(); localIterator2.hasNext();)
/* 800:    */         {
/* 801:730 */           localPeer2 = (Peer)localIterator2.next();
/* 802:731 */           if (localPeer2.date == m) {
/* 803:732 */             l += localPeer2.weight;
/* 804:    */           } else {
/* 805:734 */             localPeer2.weight = 0;
/* 806:    */           }
/* 807:    */         }
/* 808:    */         Peer localPeer2;
/* 809:738 */         for (localIterator2 = localLinkedList.iterator(); localIterator2.hasNext();)
/* 810:    */         {
/* 811:738 */           localPeer2 = (Peer)localIterator2.next();
/* 812:739 */           localPeer2.adjustedWeight = (1000000000L * localPeer2.weight / l);
/* 813:740 */           localPeer2.updateWeight();
/* 814:    */         }
/* 815:743 */         return true;
/* 816:    */       }
/* 817:    */     }
/* 818:    */     catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
/* 819:    */     {
/* 820:746 */       Logger.logDebugMessage("Failed to analyze hallmark for peer " + paramString1, localRuntimeException);
/* 821:    */     }
/* 822:748 */     return false;
/* 823:    */   }
/* 824:    */   
/* 825:    */   void setState(State paramState)
/* 826:    */   {
/* 827:    */     JSONObject localJSONObject1;
/* 828:    */     JSONArray localJSONArray;
/* 829:    */     JSONObject localJSONObject2;
/* 830:754 */     if ((this.state == State.NON_CONNECTED) && (paramState != State.NON_CONNECTED))
/* 831:    */     {
/* 832:756 */       localJSONObject1 = new JSONObject();
/* 833:757 */       localJSONObject1.put("response", "processNewData");
/* 834:759 */       if (this.announcedAddress.length() > 0)
/* 835:    */       {
/* 836:761 */         localJSONArray = new JSONArray();
/* 837:762 */         localJSONObject2 = new JSONObject();
/* 838:763 */         localJSONObject2.put("index", Integer.valueOf(this.index));
/* 839:764 */         localJSONArray.add(localJSONObject2);
/* 840:765 */         localJSONObject1.put("removedKnownPeers", localJSONArray);
/* 841:    */       }
/* 842:769 */       localJSONArray = new JSONArray();
/* 843:770 */       localJSONObject2 = new JSONObject();
/* 844:771 */       localJSONObject2.put("index", Integer.valueOf(this.index));
/* 845:772 */       if (paramState == State.DISCONNECTED) {
/* 846:773 */         localJSONObject2.put("disconnected", Boolean.valueOf(true));
/* 847:    */       }
/* 848:777 */       localJSONObject2.put("address", truncate(this.peerAddress, 25, true));
/* 849:778 */       localJSONObject2.put("announcedAddress", truncate(this.announcedAddress, 25, true));
/* 850:779 */       localJSONObject2.put("weight", Integer.valueOf(getWeight()));
/* 851:780 */       localJSONObject2.put("downloaded", Long.valueOf(this.downloadedVolume));
/* 852:781 */       localJSONObject2.put("uploaded", Long.valueOf(this.uploadedVolume));
/* 853:782 */       localJSONObject2.put("software", getSoftware());
/* 854:783 */       if (Nxt.wellKnownPeers.contains(this.announcedAddress)) {
/* 855:784 */         localJSONObject2.put("wellKnown", Boolean.valueOf(true));
/* 856:    */       }
/* 857:786 */       localJSONArray.add(localJSONObject2);
/* 858:787 */       localJSONObject1.put("addedActivePeers", localJSONArray);
/* 859:    */       
/* 860:789 */       User.sendToAll(localJSONObject1);
/* 861:    */     }
/* 862:791 */     else if ((this.state != State.NON_CONNECTED) && (paramState != State.NON_CONNECTED))
/* 863:    */     {
/* 864:793 */       localJSONObject1 = new JSONObject();
/* 865:794 */       localJSONObject1.put("response", "processNewData");
/* 866:    */       
/* 867:796 */       localJSONArray = new JSONArray();
/* 868:797 */       localJSONObject2 = new JSONObject();
/* 869:798 */       localJSONObject2.put("index", Integer.valueOf(this.index));
/* 870:799 */       localJSONObject2.put(paramState == State.CONNECTED ? "connected" : "disconnected", Boolean.valueOf(true));
/* 871:800 */       localJSONArray.add(localJSONObject2);
/* 872:801 */       localJSONObject1.put("changedActivePeers", localJSONArray);
/* 873:    */       
/* 874:803 */       User.sendToAll(localJSONObject1);
/* 875:    */     }
/* 876:807 */     this.state = paramState;
/* 877:    */   }
/* 878:    */   
/* 879:    */   void updateDownloadedVolume(long paramLong)
/* 880:    */   {
/* 881:813 */     this.downloadedVolume += paramLong;
/* 882:    */     
/* 883:815 */     JSONObject localJSONObject1 = new JSONObject();
/* 884:816 */     localJSONObject1.put("response", "processNewData");
/* 885:    */     
/* 886:818 */     JSONArray localJSONArray = new JSONArray();
/* 887:819 */     JSONObject localJSONObject2 = new JSONObject();
/* 888:820 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 889:821 */     localJSONObject2.put("downloaded", Long.valueOf(this.downloadedVolume));
/* 890:822 */     localJSONArray.add(localJSONObject2);
/* 891:823 */     localJSONObject1.put("changedActivePeers", localJSONArray);
/* 892:    */     
/* 893:825 */     User.sendToAll(localJSONObject1);
/* 894:    */   }
/* 895:    */   
/* 896:    */   void updateUploadedVolume(long paramLong)
/* 897:    */   {
/* 898:831 */     this.uploadedVolume += paramLong;
/* 899:    */     
/* 900:833 */     JSONObject localJSONObject1 = new JSONObject();
/* 901:834 */     localJSONObject1.put("response", "processNewData");
/* 902:    */     
/* 903:836 */     JSONArray localJSONArray = new JSONArray();
/* 904:837 */     JSONObject localJSONObject2 = new JSONObject();
/* 905:838 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 906:839 */     localJSONObject2.put("uploaded", Long.valueOf(this.uploadedVolume));
/* 907:840 */     localJSONArray.add(localJSONObject2);
/* 908:841 */     localJSONObject1.put("changedActivePeers", localJSONArray);
/* 909:    */     
/* 910:843 */     User.sendToAll(localJSONObject1);
/* 911:    */   }
/* 912:    */   
/* 913:    */   private void connect()
/* 914:    */   {
/* 915:848 */     JSONObject localJSONObject1 = new JSONObject();
/* 916:849 */     localJSONObject1.put("requestType", "getInfo");
/* 917:850 */     if ((Nxt.myAddress != null) && (Nxt.myAddress.length() > 0)) {
/* 918:851 */       localJSONObject1.put("announcedAddress", Nxt.myAddress);
/* 919:    */     }
/* 920:853 */     if ((Nxt.myHallmark != null) && (Nxt.myHallmark.length() > 0)) {
/* 921:854 */       localJSONObject1.put("hallmark", Nxt.myHallmark);
/* 922:    */     }
/* 923:856 */     localJSONObject1.put("application", "NRS");
/* 924:857 */     localJSONObject1.put("version", "0.7.0e");
/* 925:858 */     localJSONObject1.put("platform", Nxt.myPlatform);
/* 926:859 */     localJSONObject1.put("scheme", Nxt.myScheme);
/* 927:860 */     localJSONObject1.put("port", Integer.valueOf(Nxt.myPort));
/* 928:861 */     localJSONObject1.put("shareAddress", Boolean.valueOf(Nxt.shareMyAddress));
/* 929:862 */     JSONObject localJSONObject2 = send(JSON.prepareRequest(localJSONObject1));
/* 930:864 */     if (localJSONObject2 != null)
/* 931:    */     {
/* 932:865 */       this.application = ((String)localJSONObject2.get("application"));
/* 933:866 */       this.version = ((String)localJSONObject2.get("version"));
/* 934:867 */       this.platform = ((String)localJSONObject2.get("platform"));
/* 935:868 */       this.shareAddress = Boolean.TRUE.equals(localJSONObject2.get("shareAddress"));
/* 936:870 */       if (analyzeHallmark(this.announcedAddress, (String)localJSONObject2.get("hallmark"))) {
/* 937:871 */         setState(State.CONNECTED);
/* 938:    */       }
/* 939:    */     }
/* 940:    */   }
/* 941:    */   
/* 942:    */   private void updateWeight()
/* 943:    */   {
/* 944:878 */     JSONObject localJSONObject1 = new JSONObject();
/* 945:879 */     localJSONObject1.put("response", "processNewData");
/* 946:    */     
/* 947:881 */     JSONArray localJSONArray = new JSONArray();
/* 948:882 */     JSONObject localJSONObject2 = new JSONObject();
/* 949:883 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 950:884 */     localJSONObject2.put("weight", Integer.valueOf(getWeight()));
/* 951:885 */     localJSONArray.add(localJSONObject2);
/* 952:886 */     localJSONObject1.put("changedActivePeers", localJSONArray);
/* 953:    */     
/* 954:888 */     User.sendToAll(localJSONObject1);
/* 955:    */   }
/* 956:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.Peer
 * JD-Core Version:    0.7.0.1
 */