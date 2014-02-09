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
/*  12:    */ import java.io.Writer;
/*  13:    */ import java.net.HttpURLConnection;
/*  14:    */ import java.net.InetAddress;
/*  15:    */ import java.net.MalformedURLException;
/*  16:    */ import java.net.SocketException;
/*  17:    */ import java.net.SocketTimeoutException;
/*  18:    */ import java.net.URI;
/*  19:    */ import java.net.URISyntaxException;
/*  20:    */ import java.net.URL;
/*  21:    */ import java.net.UnknownHostException;
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
/*  36:    */ import nxt.Blockchain.BlockOutOfOrderException;
/*  37:    */ import nxt.Nxt;
/*  38:    */ import nxt.NxtException;
/*  39:    */ import nxt.ThreadPools;
/*  40:    */ import nxt.Transaction.NotYetEnabledException;
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
/* 166:    */   private int port;
/* 167:    */   private boolean shareAddress;
/* 168:    */   private String hallmark;
/* 169:    */   private String platform;
/* 170:    */   private String application;
/* 171:    */   private String version;
/* 172:    */   private int weight;
/* 173:    */   private int date;
/* 174:    */   private Long accountId;
/* 175:    */   private long adjustedWeight;
/* 176:    */   private volatile long blacklistingTime;
/* 177:    */   private volatile State state;
/* 178:    */   private volatile long downloadedVolume;
/* 179:    */   private volatile long uploadedVolume;
/* 180:    */   
/* 181:    */   public static Collection<Peer> getAllPeers()
/* 182:    */   {
/* 183:168 */     return allPeers;
/* 184:    */   }
/* 185:    */   
/* 186:    */   public static Peer getPeer(String paramString)
/* 187:    */   {
/* 188:172 */     return (Peer)peers.get(paramString);
/* 189:    */   }
/* 190:    */   
/* 191:    */   public static Peer addPeer(String paramString1, String paramString2)
/* 192:    */   {
/* 193:177 */     Object localObject = parseHostAndPort(paramString1);
/* 194:178 */     if (localObject == null) {
/* 195:179 */       return null;
/* 196:    */     }
/* 197:182 */     String str = parseHostAndPort(paramString2);
/* 198:184 */     if ((Nxt.myAddress != null) && (Nxt.myAddress.length() > 0) && (Nxt.myAddress.equalsIgnoreCase(str))) {
/* 199:185 */       return null;
/* 200:    */     }
/* 201:188 */     if (str != null) {
/* 202:189 */       localObject = str;
/* 203:    */     }
/* 204:192 */     Peer localPeer = (Peer)peers.get(localObject);
/* 205:193 */     if (localPeer == null)
/* 206:    */     {
/* 207:194 */       localPeer = new Peer((String)localObject, str);
/* 208:195 */       peers.put(localObject, localPeer);
/* 209:    */     }
/* 210:198 */     return localPeer;
/* 211:    */   }
/* 212:    */   
/* 213:    */   public static void updatePeerWeights(Account paramAccount)
/* 214:    */   {
/* 215:202 */     for (Peer localPeer : peers.values()) {
/* 216:203 */       if ((paramAccount.getId().equals(localPeer.accountId)) && (localPeer.adjustedWeight > 0L)) {
/* 217:204 */         localPeer.updateWeight();
/* 218:    */       }
/* 219:    */     }
/* 220:    */   }
/* 221:    */   
/* 222:    */   public static void sendToSomePeers(JSONObject paramJSONObject)
/* 223:    */   {
/* 224:211 */     JSONStreamAware localJSONStreamAware = JSON.prepareRequest(paramJSONObject);
/* 225:    */     
/* 226:213 */     int i = 0;
/* 227:214 */     ArrayList localArrayList = new ArrayList();
/* 228:215 */     for (Peer localPeer : peers.values()) {
/* 229:217 */       if ((!Nxt.enableHallmarkProtection) || (localPeer.getWeight() >= Nxt.pushThreshold))
/* 230:    */       {
/* 231:    */         Object localObject;
/* 232:221 */         if ((!localPeer.isBlacklisted()) && (localPeer.state == State.CONNECTED) && (localPeer.announcedAddress != null))
/* 233:    */         {
/* 234:222 */           localObject = ThreadPools.sendInParallel(localPeer, localJSONStreamAware);
/* 235:223 */           localArrayList.add(localObject);
/* 236:    */         }
/* 237:225 */         if (localArrayList.size() >= Nxt.sendToPeersLimit - i)
/* 238:    */         {
/* 239:226 */           for (localObject = localArrayList.iterator(); ((Iterator)localObject).hasNext();)
/* 240:    */           {
/* 241:226 */             Future localFuture = (Future)((Iterator)localObject).next();
/* 242:    */             try
/* 243:    */             {
/* 244:228 */               JSONObject localJSONObject = (JSONObject)localFuture.get();
/* 245:229 */               if ((localJSONObject != null) && (localJSONObject.get("error") == null)) {
/* 246:230 */                 i++;
/* 247:    */               }
/* 248:    */             }
/* 249:    */             catch (InterruptedException localInterruptedException)
/* 250:    */             {
/* 251:233 */               Thread.currentThread().interrupt();
/* 252:    */             }
/* 253:    */             catch (ExecutionException localExecutionException)
/* 254:    */             {
/* 255:235 */               Logger.logDebugMessage("Error in sendToSomePeers", localExecutionException);
/* 256:    */             }
/* 257:    */           }
/* 258:239 */           localArrayList.clear();
/* 259:    */         }
/* 260:241 */         if (i >= Nxt.sendToPeersLimit) {
/* 261:242 */           return;
/* 262:    */         }
/* 263:    */       }
/* 264:    */     }
/* 265:    */   }
/* 266:    */   
/* 267:    */   public static Peer getAnyPeer(State paramState, boolean paramBoolean)
/* 268:    */   {
/* 269:251 */     ArrayList localArrayList = new ArrayList();
/* 270:252 */     for (Peer localPeer1 : peers.values()) {
/* 271:253 */       if ((!localPeer1.isBlacklisted()) && (localPeer1.state == paramState) && (localPeer1.announcedAddress != null) && ((!paramBoolean) || (!Nxt.enableHallmarkProtection) || (localPeer1.getWeight() >= Nxt.pullThreshold))) {
/* 272:255 */         localArrayList.add(localPeer1);
/* 273:    */       }
/* 274:    */     }
/* 275:    */     long l2;
/* 276:259 */     if (localArrayList.size() > 0)
/* 277:    */     {
/* 278:260 */       long l1 = 0L;
/* 279:261 */       for (Peer localPeer2 : localArrayList)
/* 280:    */       {
/* 281:262 */         long l3 = localPeer2.getWeight();
/* 282:263 */         if (l3 == 0L) {
/* 283:264 */           l3 = 1L;
/* 284:    */         }
/* 285:266 */         l1 += l3;
/* 286:    */       }
/* 287:269 */       l2 = ThreadLocalRandom.current().nextLong(l1);
/* 288:270 */       for (Peer localPeer3 : localArrayList)
/* 289:    */       {
/* 290:271 */         long l4 = localPeer3.getWeight();
/* 291:272 */         if (l4 == 0L) {
/* 292:273 */           l4 = 1L;
/* 293:    */         }
/* 294:275 */         if (l2 -= l4 < 0L) {
/* 295:276 */           return localPeer3;
/* 296:    */         }
/* 297:    */       }
/* 298:    */     }
/* 299:280 */     return null;
/* 300:    */   }
/* 301:    */   
/* 302:    */   private static String parseHostAndPort(String paramString)
/* 303:    */   {
/* 304:    */     try
/* 305:    */     {
/* 306:285 */       URI localURI = new URI("http://" + paramString.trim());
/* 307:286 */       String str = localURI.getHost();
/* 308:287 */       if ((str == null) || (str.equals("")) || (str.equals("localhost")) || (str.equals("127.0.0.1")) || (str.equals("0:0:0:0:0:0:0:1"))) {
/* 309:288 */         return null;
/* 310:    */       }
/* 311:290 */       InetAddress localInetAddress = InetAddress.getByName(str);
/* 312:291 */       if ((localInetAddress.isAnyLocalAddress()) || (localInetAddress.isLoopbackAddress()) || (localInetAddress.isLinkLocalAddress())) {
/* 313:292 */         return null;
/* 314:    */       }
/* 315:294 */       int i = localURI.getPort();
/* 316:295 */       return str + ':' + i;
/* 317:    */     }
/* 318:    */     catch (URISyntaxException|UnknownHostException localURISyntaxException) {}
/* 319:297 */     return null;
/* 320:    */   }
/* 321:    */   
/* 322:    */   private static int getNumberOfConnectedPublicPeers()
/* 323:    */   {
/* 324:302 */     int i = 0;
/* 325:303 */     for (Peer localPeer : peers.values()) {
/* 326:304 */       if ((localPeer.state == State.CONNECTED) && (localPeer.announcedAddress != null)) {
/* 327:305 */         i++;
/* 328:    */       }
/* 329:    */     }
/* 330:308 */     return i;
/* 331:    */   }
/* 332:    */   
/* 333:    */   private Peer(String paramString1, String paramString2)
/* 334:    */   {
/* 335:332 */     this.peerAddress = paramString1;
/* 336:333 */     this.announcedAddress = paramString2;
/* 337:334 */     this.index = peerCounter.incrementAndGet();
/* 338:335 */     this.state = State.NON_CONNECTED;
/* 339:    */   }
/* 340:    */   
/* 341:    */   public int getIndex()
/* 342:    */   {
/* 343:339 */     return this.index;
/* 344:    */   }
/* 345:    */   
/* 346:    */   public String getPeerAddress()
/* 347:    */   {
/* 348:343 */     return this.peerAddress;
/* 349:    */   }
/* 350:    */   
/* 351:    */   public State getState()
/* 352:    */   {
/* 353:347 */     return this.state;
/* 354:    */   }
/* 355:    */   
/* 356:    */   public long getDownloadedVolume()
/* 357:    */   {
/* 358:351 */     return this.downloadedVolume;
/* 359:    */   }
/* 360:    */   
/* 361:    */   public long getUploadedVolume()
/* 362:    */   {
/* 363:355 */     return this.uploadedVolume;
/* 364:    */   }
/* 365:    */   
/* 366:    */   public String getVersion()
/* 367:    */   {
/* 368:359 */     return this.version;
/* 369:    */   }
/* 370:    */   
/* 371:    */   void setVersion(String paramString)
/* 372:    */   {
/* 373:363 */     this.version = paramString;
/* 374:    */   }
/* 375:    */   
/* 376:    */   public String getApplication()
/* 377:    */   {
/* 378:367 */     return this.application;
/* 379:    */   }
/* 380:    */   
/* 381:    */   void setApplication(String paramString)
/* 382:    */   {
/* 383:371 */     this.application = paramString;
/* 384:    */   }
/* 385:    */   
/* 386:    */   public String getPlatform()
/* 387:    */   {
/* 388:375 */     return this.platform;
/* 389:    */   }
/* 390:    */   
/* 391:    */   void setPlatform(String paramString)
/* 392:    */   {
/* 393:379 */     this.platform = paramString;
/* 394:    */   }
/* 395:    */   
/* 396:    */   public String getHallmark()
/* 397:    */   {
/* 398:383 */     return this.hallmark;
/* 399:    */   }
/* 400:    */   
/* 401:    */   public boolean shareAddress()
/* 402:    */   {
/* 403:387 */     return this.shareAddress;
/* 404:    */   }
/* 405:    */   
/* 406:    */   void setShareAddress(boolean paramBoolean)
/* 407:    */   {
/* 408:391 */     this.shareAddress = paramBoolean;
/* 409:    */   }
/* 410:    */   
/* 411:    */   public String getAnnouncedAddress()
/* 412:    */   {
/* 413:395 */     return this.announcedAddress;
/* 414:    */   }
/* 415:    */   
/* 416:    */   void setAnnouncedAddress(String paramString)
/* 417:    */   {
/* 418:399 */     String str = parseHostAndPort(paramString);
/* 419:400 */     if (str != null)
/* 420:    */     {
/* 421:401 */       this.announcedAddress = str;
/* 422:    */       try
/* 423:    */       {
/* 424:403 */         this.port = new URL(str).getPort();
/* 425:    */       }
/* 426:    */       catch (MalformedURLException localMalformedURLException) {}
/* 427:    */     }
/* 428:    */   }
/* 429:    */   
/* 430:    */   public boolean isWellKnown()
/* 431:    */   {
/* 432:409 */     return (this.announcedAddress != null) && (Nxt.wellKnownPeers.contains(this.announcedAddress));
/* 433:    */   }
/* 434:    */   
/* 435:    */   public boolean isBlacklisted()
/* 436:    */   {
/* 437:413 */     return this.blacklistingTime > 0L;
/* 438:    */   }
/* 439:    */   
/* 440:    */   public int compareTo(Peer paramPeer)
/* 441:    */   {
/* 442:418 */     long l1 = getWeight();long l2 = paramPeer.getWeight();
/* 443:419 */     if (l1 > l2) {
/* 444:420 */       return -1;
/* 445:    */     }
/* 446:421 */     if (l1 < l2) {
/* 447:422 */       return 1;
/* 448:    */     }
/* 449:424 */     return this.index - paramPeer.index;
/* 450:    */   }
/* 451:    */   
/* 452:    */   public void blacklist(NxtException paramNxtException)
/* 453:    */   {
/* 454:429 */     if (((paramNxtException instanceof Transaction.NotYetEnabledException)) || ((paramNxtException instanceof Blockchain.BlockOutOfOrderException))) {
/* 455:432 */       return;
/* 456:    */     }
/* 457:434 */     if (!isBlacklisted()) {
/* 458:435 */       Logger.logDebugMessage("Blacklisting " + this.peerAddress + " because of: " + paramNxtException.getMessage());
/* 459:    */     }
/* 460:437 */     blacklist();
/* 461:    */   }
/* 462:    */   
/* 463:    */   public void blacklist()
/* 464:    */   {
/* 465:442 */     this.blacklistingTime = System.currentTimeMillis();
/* 466:    */     
/* 467:444 */     JSONObject localJSONObject1 = new JSONObject();
/* 468:445 */     localJSONObject1.put("response", "processNewData");
/* 469:    */     
/* 470:447 */     JSONArray localJSONArray1 = new JSONArray();
/* 471:448 */     JSONObject localJSONObject2 = new JSONObject();
/* 472:449 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 473:450 */     localJSONArray1.add(localJSONObject2);
/* 474:451 */     localJSONObject1.put("removedKnownPeers", localJSONArray1);
/* 475:    */     
/* 476:453 */     JSONArray localJSONArray2 = new JSONArray();
/* 477:454 */     JSONObject localJSONObject3 = new JSONObject();
/* 478:455 */     localJSONObject3.put("index", Integer.valueOf(this.index));
/* 479:456 */     localJSONObject3.put("announcedAddress", Convert.truncate(this.announcedAddress, "", 25, true));
/* 480:457 */     if (isWellKnown()) {
/* 481:458 */       localJSONObject3.put("wellKnown", Boolean.valueOf(true));
/* 482:    */     }
/* 483:460 */     localJSONArray2.add(localJSONObject3);
/* 484:461 */     localJSONObject1.put("addedBlacklistedPeers", localJSONArray2);
/* 485:    */     
/* 486:463 */     User.sendToAll(localJSONObject1);
/* 487:    */   }
/* 488:    */   
/* 489:    */   public void deactivate()
/* 490:    */   {
/* 491:468 */     if (this.state == State.CONNECTED) {
/* 492:469 */       setState(State.DISCONNECTED);
/* 493:    */     }
/* 494:471 */     setState(State.NON_CONNECTED);
/* 495:    */     
/* 496:473 */     JSONObject localJSONObject1 = new JSONObject();
/* 497:474 */     localJSONObject1.put("response", "processNewData");
/* 498:    */     
/* 499:476 */     JSONArray localJSONArray1 = new JSONArray();
/* 500:477 */     JSONObject localJSONObject2 = new JSONObject();
/* 501:478 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 502:479 */     localJSONArray1.add(localJSONObject2);
/* 503:480 */     localJSONObject1.put("removedActivePeers", localJSONArray1);
/* 504:482 */     if (this.announcedAddress != null)
/* 505:    */     {
/* 506:483 */       JSONArray localJSONArray2 = new JSONArray();
/* 507:484 */       JSONObject localJSONObject3 = new JSONObject();
/* 508:485 */       localJSONObject3.put("index", Integer.valueOf(this.index));
/* 509:486 */       localJSONObject3.put("announcedAddress", Convert.truncate(this.announcedAddress, "", 25, true));
/* 510:487 */       if (isWellKnown()) {
/* 511:488 */         localJSONObject3.put("wellKnown", Boolean.valueOf(true));
/* 512:    */       }
/* 513:490 */       localJSONArray2.add(localJSONObject3);
/* 514:491 */       localJSONObject1.put("addedKnownPeers", localJSONArray2);
/* 515:    */     }
/* 516:494 */     User.sendToAll(localJSONObject1);
/* 517:    */   }
/* 518:    */   
/* 519:    */   public int getWeight()
/* 520:    */   {
/* 521:499 */     if (this.accountId == null) {
/* 522:500 */       return 0;
/* 523:    */     }
/* 524:502 */     Account localAccount = Account.getAccount(this.accountId);
/* 525:503 */     if (localAccount == null) {
/* 526:504 */       return 0;
/* 527:    */     }
/* 528:506 */     return (int)(this.adjustedWeight * (localAccount.getBalance() / 100L) / 1000000000L);
/* 529:    */   }
/* 530:    */   
/* 531:    */   public String getSoftware()
/* 532:    */   {
/* 533:510 */     StringBuilder localStringBuilder = new StringBuilder();
/* 534:511 */     localStringBuilder.append(Convert.truncate(this.application, "?", 10, false));
/* 535:512 */     localStringBuilder.append(" (");
/* 536:513 */     localStringBuilder.append(Convert.truncate(this.version, "?", 10, false));
/* 537:514 */     localStringBuilder.append(")").append(" @ ");
/* 538:515 */     localStringBuilder.append(Convert.truncate(this.platform, "?", 10, false));
/* 539:516 */     return localStringBuilder.toString();
/* 540:    */   }
/* 541:    */   
/* 542:    */   public void removeBlacklistedStatus()
/* 543:    */   {
/* 544:521 */     setState(State.NON_CONNECTED);
/* 545:522 */     this.blacklistingTime = 0L;
/* 546:    */     
/* 547:524 */     JSONObject localJSONObject1 = new JSONObject();
/* 548:525 */     localJSONObject1.put("response", "processNewData");
/* 549:    */     
/* 550:527 */     JSONArray localJSONArray1 = new JSONArray();
/* 551:528 */     JSONObject localJSONObject2 = new JSONObject();
/* 552:529 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 553:530 */     localJSONArray1.add(localJSONObject2);
/* 554:531 */     localJSONObject1.put("removedBlacklistedPeers", localJSONArray1);
/* 555:    */     
/* 556:533 */     JSONArray localJSONArray2 = new JSONArray();
/* 557:534 */     JSONObject localJSONObject3 = new JSONObject();
/* 558:535 */     localJSONObject3.put("index", Integer.valueOf(this.index));
/* 559:536 */     localJSONObject3.put("announcedAddress", Convert.truncate(this.announcedAddress, "", 25, true));
/* 560:537 */     if (isWellKnown()) {
/* 561:538 */       localJSONObject3.put("wellKnown", Boolean.valueOf(true));
/* 562:    */     }
/* 563:540 */     localJSONArray2.add(localJSONObject3);
/* 564:541 */     localJSONObject1.put("addedKnownPeers", localJSONArray2);
/* 565:    */     
/* 566:543 */     User.sendToAll(localJSONObject1);
/* 567:    */   }
/* 568:    */   
/* 569:    */   public void removePeer()
/* 570:    */   {
/* 571:549 */     peers.values().remove(this);
/* 572:    */     
/* 573:551 */     JSONObject localJSONObject1 = new JSONObject();
/* 574:552 */     localJSONObject1.put("response", "processNewData");
/* 575:    */     
/* 576:554 */     JSONArray localJSONArray = new JSONArray();
/* 577:555 */     JSONObject localJSONObject2 = new JSONObject();
/* 578:556 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 579:557 */     localJSONArray.add(localJSONObject2);
/* 580:558 */     localJSONObject1.put("removedKnownPeers", localJSONArray);
/* 581:    */     
/* 582:560 */     User.sendToAll(localJSONObject1);
/* 583:    */   }
/* 584:    */   
/* 585:    */   public JSONObject send(JSONStreamAware paramJSONStreamAware)
/* 586:    */   {
/* 587:568 */     String str = null;
/* 588:569 */     int i = 0;
/* 589:    */     
/* 590:571 */     HttpURLConnection localHttpURLConnection = null;
/* 591:    */     JSONObject localJSONObject;
/* 592:    */     try
/* 593:    */     {
/* 594:575 */       if (Nxt.communicationLoggingMask != 0)
/* 595:    */       {
/* 596:577 */         localObject1 = new StringWriter();
/* 597:578 */         paramJSONStreamAware.writeJSONString((Writer)localObject1);
/* 598:579 */         str = "\"" + this.announcedAddress + "\": " + ((StringWriter)localObject1).toString();
/* 599:    */       }
/* 600:583 */       Object localObject1 = new URL("http://" + this.announcedAddress + (this.port <= 0 ? ":7874" : "") + "/nxt");
/* 601:    */       
/* 602:585 */       localHttpURLConnection = (HttpURLConnection)((URL)localObject1).openConnection();
/* 603:586 */       localHttpURLConnection.setRequestMethod("POST");
/* 604:587 */       localHttpURLConnection.setDoOutput(true);
/* 605:588 */       localHttpURLConnection.setConnectTimeout(Nxt.connectTimeout);
/* 606:589 */       localHttpURLConnection.setReadTimeout(Nxt.readTimeout);
/* 607:    */       
/* 608:591 */       CountingOutputStream localCountingOutputStream = new CountingOutputStream(localHttpURLConnection.getOutputStream());
/* 609:592 */       Object localObject2 = new BufferedWriter(new OutputStreamWriter(localCountingOutputStream, "UTF-8"));Object localObject3 = null;
/* 610:    */       try
/* 611:    */       {
/* 612:593 */         paramJSONStreamAware.writeJSONString((Writer)localObject2);
/* 613:    */       }
/* 614:    */       catch (Throwable localThrowable2)
/* 615:    */       {
/* 616:592 */         localObject3 = localThrowable2;throw localThrowable2;
/* 617:    */       }
/* 618:    */       finally
/* 619:    */       {
/* 620:594 */         if (localObject2 != null) {
/* 621:594 */           if (localObject3 != null) {
/* 622:    */             try
/* 623:    */             {
/* 624:594 */               ((Writer)localObject2).close();
/* 625:    */             }
/* 626:    */             catch (Throwable localThrowable5)
/* 627:    */             {
/* 628:594 */               ((Throwable)localObject3).addSuppressed(localThrowable5);
/* 629:    */             }
/* 630:    */           } else {
/* 631:594 */             ((Writer)localObject2).close();
/* 632:    */           }
/* 633:    */         }
/* 634:    */       }
/* 635:595 */       updateUploadedVolume(localCountingOutputStream.getCount());
/* 636:597 */       if (localHttpURLConnection.getResponseCode() == 200)
/* 637:    */       {
/* 638:599 */         if ((Nxt.communicationLoggingMask & 0x4) != 0)
/* 639:    */         {
/* 640:602 */           localObject2 = new ByteArrayOutputStream();
/* 641:603 */           localObject3 = new byte[65536];
/* 642:    */           
/* 643:605 */           Object localObject6 = localHttpURLConnection.getInputStream();Object localObject7 = null;
/* 644:    */           try
/* 645:    */           {
/* 646:    */             int j;
/* 647:606 */             while ((j = ((InputStream)localObject6).read((byte[])localObject3)) > 0) {
/* 648:607 */               ((ByteArrayOutputStream)localObject2).write((byte[])localObject3, 0, j);
/* 649:    */             }
/* 650:    */           }
/* 651:    */           catch (Throwable localThrowable7)
/* 652:    */           {
/* 653:605 */             localObject7 = localThrowable7;throw localThrowable7;
/* 654:    */           }
/* 655:    */           finally
/* 656:    */           {
/* 657:609 */             if (localObject6 != null) {
/* 658:609 */               if (localObject7 != null) {
/* 659:    */                 try
/* 660:    */                 {
/* 661:609 */                   ((InputStream)localObject6).close();
/* 662:    */                 }
/* 663:    */                 catch (Throwable localThrowable8)
/* 664:    */                 {
/* 665:609 */                   localObject7.addSuppressed(localThrowable8);
/* 666:    */                 }
/* 667:    */               } else {
/* 668:609 */                 ((InputStream)localObject6).close();
/* 669:    */               }
/* 670:    */             }
/* 671:    */           }
/* 672:610 */           localObject6 = ((ByteArrayOutputStream)localObject2).toString("UTF-8");
/* 673:611 */           str = str + " >>> " + (String)localObject6;
/* 674:612 */           i = 1;
/* 675:613 */           updateDownloadedVolume(((String)localObject6).getBytes("UTF-8").length);
/* 676:614 */           localJSONObject = (JSONObject)JSONValue.parse((String)localObject6);
/* 677:    */         }
/* 678:    */         else
/* 679:    */         {
/* 680:618 */           localObject2 = new CountingInputStream(localHttpURLConnection.getInputStream());
/* 681:    */           
/* 682:620 */           localObject3 = new BufferedReader(new InputStreamReader((InputStream)localObject2, "UTF-8"));Object localObject4 = null;
/* 683:    */           try
/* 684:    */           {
/* 685:621 */             localJSONObject = (JSONObject)JSONValue.parse((Reader)localObject3);
/* 686:    */           }
/* 687:    */           catch (Throwable localThrowable4)
/* 688:    */           {
/* 689:620 */             localObject4 = localThrowable4;throw localThrowable4;
/* 690:    */           }
/* 691:    */           finally
/* 692:    */           {
/* 693:622 */             if (localObject3 != null) {
/* 694:622 */               if (localObject4 != null) {
/* 695:    */                 try
/* 696:    */                 {
/* 697:622 */                   ((Reader)localObject3).close();
/* 698:    */                 }
/* 699:    */                 catch (Throwable localThrowable9)
/* 700:    */                 {
/* 701:622 */                   localObject4.addSuppressed(localThrowable9);
/* 702:    */                 }
/* 703:    */               } else {
/* 704:622 */                 ((Reader)localObject3).close();
/* 705:    */               }
/* 706:    */             }
/* 707:    */           }
/* 708:624 */           updateDownloadedVolume(((CountingInputStream)localObject2).getCount());
/* 709:    */         }
/* 710:    */       }
/* 711:    */       else
/* 712:    */       {
/* 713:630 */         if ((Nxt.communicationLoggingMask & 0x2) != 0)
/* 714:    */         {
/* 715:632 */           str = str + " >>> Peer responded with HTTP " + localHttpURLConnection.getResponseCode() + " code!";
/* 716:633 */           i = 1;
/* 717:    */         }
/* 718:637 */         setState(State.DISCONNECTED);
/* 719:    */         
/* 720:639 */         localJSONObject = null;
/* 721:    */       }
/* 722:    */     }
/* 723:    */     catch (RuntimeException|IOException localRuntimeException)
/* 724:    */     {
/* 725:645 */       if ((!(localRuntimeException instanceof UnknownHostException)) && (!(localRuntimeException instanceof SocketTimeoutException)) && (!(localRuntimeException instanceof SocketException))) {
/* 726:646 */         Logger.logDebugMessage("Error sending JSON request", localRuntimeException);
/* 727:    */       }
/* 728:649 */       if ((Nxt.communicationLoggingMask & 0x1) != 0)
/* 729:    */       {
/* 730:651 */         str = str + " >>> " + localRuntimeException.toString();
/* 731:652 */         i = 1;
/* 732:    */       }
/* 733:656 */       if (this.state == State.NON_CONNECTED) {
/* 734:658 */         blacklist();
/* 735:    */       } else {
/* 736:662 */         setState(State.DISCONNECTED);
/* 737:    */       }
/* 738:666 */       localJSONObject = null;
/* 739:    */     }
/* 740:670 */     if (i != 0) {
/* 741:672 */       Logger.logMessage(str + "\n");
/* 742:    */     }
/* 743:676 */     if (localHttpURLConnection != null) {
/* 744:678 */       localHttpURLConnection.disconnect();
/* 745:    */     }
/* 746:682 */     return localJSONObject;
/* 747:    */   }
/* 748:    */   
/* 749:    */   boolean analyzeHallmark(String paramString1, String paramString2)
/* 750:    */   {
/* 751:688 */     if ((paramString2 == null) || (paramString2.equals(this.hallmark))) {
/* 752:689 */       return true;
/* 753:    */     }
/* 754:    */     try
/* 755:    */     {
/* 756:693 */       Hallmark localHallmark = Hallmark.parseHallmark(paramString2);
/* 757:694 */       if ((!localHallmark.isValid()) || (!localHallmark.getHost().equals(paramString1))) {
/* 758:695 */         return false;
/* 759:    */       }
/* 760:697 */       this.hallmark = paramString2;
/* 761:698 */       Long localLong = Account.getId(localHallmark.getPublicKey());
/* 762:699 */       LinkedList localLinkedList = new LinkedList();
/* 763:700 */       int i = 0;
/* 764:701 */       this.accountId = localLong;
/* 765:702 */       this.weight = localHallmark.getWeight();
/* 766:703 */       this.date = localHallmark.getDate();
/* 767:704 */       for (Peer localPeer1 : peers.values()) {
/* 768:705 */         if (localLong.equals(localPeer1.accountId))
/* 769:    */         {
/* 770:706 */           localLinkedList.add(localPeer1);
/* 771:707 */           if (localPeer1.date > i) {
/* 772:708 */             i = localPeer1.date;
/* 773:    */           }
/* 774:    */         }
/* 775:    */       }
/* 776:713 */       long l = 0L;
/* 777:714 */       for (Iterator localIterator2 = localLinkedList.iterator(); localIterator2.hasNext();)
/* 778:    */       {
/* 779:714 */         localPeer2 = (Peer)localIterator2.next();
/* 780:715 */         if (localPeer2.date == i) {
/* 781:716 */           l += localPeer2.weight;
/* 782:    */         } else {
/* 783:718 */           localPeer2.weight = 0;
/* 784:    */         }
/* 785:    */       }
/* 786:    */       Peer localPeer2;
/* 787:722 */       for (localIterator2 = localLinkedList.iterator(); localIterator2.hasNext();)
/* 788:    */       {
/* 789:722 */         localPeer2 = (Peer)localIterator2.next();
/* 790:723 */         localPeer2.adjustedWeight = (1000000000L * localPeer2.weight / l);
/* 791:724 */         localPeer2.updateWeight();
/* 792:    */       }
/* 793:727 */       return true;
/* 794:    */     }
/* 795:    */     catch (RuntimeException localRuntimeException)
/* 796:    */     {
/* 797:730 */       Logger.logDebugMessage("Failed to analyze hallmark for peer " + paramString1 + ", " + localRuntimeException.toString());
/* 798:    */     }
/* 799:732 */     return false;
/* 800:    */   }
/* 801:    */   
/* 802:    */   void setState(State paramState)
/* 803:    */   {
/* 804:    */     JSONObject localJSONObject1;
/* 805:    */     JSONArray localJSONArray;
/* 806:    */     JSONObject localJSONObject2;
/* 807:738 */     if ((this.state == State.NON_CONNECTED) && (paramState != State.NON_CONNECTED))
/* 808:    */     {
/* 809:740 */       localJSONObject1 = new JSONObject();
/* 810:741 */       localJSONObject1.put("response", "processNewData");
/* 811:743 */       if (this.announcedAddress != null)
/* 812:    */       {
/* 813:745 */         localJSONArray = new JSONArray();
/* 814:746 */         localJSONObject2 = new JSONObject();
/* 815:747 */         localJSONObject2.put("index", Integer.valueOf(this.index));
/* 816:748 */         localJSONArray.add(localJSONObject2);
/* 817:749 */         localJSONObject1.put("removedKnownPeers", localJSONArray);
/* 818:    */       }
/* 819:753 */       localJSONArray = new JSONArray();
/* 820:754 */       localJSONObject2 = new JSONObject();
/* 821:755 */       localJSONObject2.put("index", Integer.valueOf(this.index));
/* 822:756 */       if (paramState == State.DISCONNECTED) {
/* 823:757 */         localJSONObject2.put("disconnected", Boolean.valueOf(true));
/* 824:    */       }
/* 825:761 */       localJSONObject2.put("address", Convert.truncate(this.peerAddress, "", 25, true));
/* 826:762 */       localJSONObject2.put("announcedAddress", Convert.truncate(this.announcedAddress, "", 25, true));
/* 827:763 */       if (isWellKnown()) {
/* 828:764 */         localJSONObject2.put("wellKnown", Boolean.valueOf(true));
/* 829:    */       }
/* 830:766 */       localJSONObject2.put("weight", Integer.valueOf(getWeight()));
/* 831:767 */       localJSONObject2.put("downloaded", Long.valueOf(this.downloadedVolume));
/* 832:768 */       localJSONObject2.put("uploaded", Long.valueOf(this.uploadedVolume));
/* 833:769 */       localJSONObject2.put("software", getSoftware());
/* 834:770 */       localJSONArray.add(localJSONObject2);
/* 835:771 */       localJSONObject1.put("addedActivePeers", localJSONArray);
/* 836:    */       
/* 837:773 */       User.sendToAll(localJSONObject1);
/* 838:    */     }
/* 839:775 */     else if ((this.state != State.NON_CONNECTED) && (paramState != State.NON_CONNECTED))
/* 840:    */     {
/* 841:777 */       localJSONObject1 = new JSONObject();
/* 842:778 */       localJSONObject1.put("response", "processNewData");
/* 843:    */       
/* 844:780 */       localJSONArray = new JSONArray();
/* 845:781 */       localJSONObject2 = new JSONObject();
/* 846:782 */       localJSONObject2.put("index", Integer.valueOf(this.index));
/* 847:783 */       localJSONObject2.put(paramState == State.CONNECTED ? "connected" : "disconnected", Boolean.valueOf(true));
/* 848:784 */       localJSONArray.add(localJSONObject2);
/* 849:785 */       localJSONObject1.put("changedActivePeers", localJSONArray);
/* 850:    */       
/* 851:787 */       User.sendToAll(localJSONObject1);
/* 852:    */     }
/* 853:791 */     this.state = paramState;
/* 854:    */   }
/* 855:    */   
/* 856:    */   void updateDownloadedVolume(long paramLong)
/* 857:    */   {
/* 858:797 */     this.downloadedVolume += paramLong;
/* 859:    */     
/* 860:799 */     JSONObject localJSONObject1 = new JSONObject();
/* 861:800 */     localJSONObject1.put("response", "processNewData");
/* 862:    */     
/* 863:802 */     JSONArray localJSONArray = new JSONArray();
/* 864:803 */     JSONObject localJSONObject2 = new JSONObject();
/* 865:804 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 866:805 */     localJSONObject2.put("downloaded", Long.valueOf(this.downloadedVolume));
/* 867:806 */     localJSONArray.add(localJSONObject2);
/* 868:807 */     localJSONObject1.put("changedActivePeers", localJSONArray);
/* 869:    */     
/* 870:809 */     User.sendToAll(localJSONObject1);
/* 871:    */   }
/* 872:    */   
/* 873:    */   void updateUploadedVolume(long paramLong)
/* 874:    */   {
/* 875:815 */     this.uploadedVolume += paramLong;
/* 876:    */     
/* 877:817 */     JSONObject localJSONObject1 = new JSONObject();
/* 878:818 */     localJSONObject1.put("response", "processNewData");
/* 879:    */     
/* 880:820 */     JSONArray localJSONArray = new JSONArray();
/* 881:821 */     JSONObject localJSONObject2 = new JSONObject();
/* 882:822 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 883:823 */     localJSONObject2.put("uploaded", Long.valueOf(this.uploadedVolume));
/* 884:824 */     localJSONArray.add(localJSONObject2);
/* 885:825 */     localJSONObject1.put("changedActivePeers", localJSONArray);
/* 886:    */     
/* 887:827 */     User.sendToAll(localJSONObject1);
/* 888:    */   }
/* 889:    */   
/* 890:    */   private void connect()
/* 891:    */   {
/* 892:832 */     JSONObject localJSONObject1 = new JSONObject();
/* 893:833 */     localJSONObject1.put("requestType", "getInfo");
/* 894:834 */     if ((Nxt.myAddress != null) && (Nxt.myAddress.length() > 0)) {
/* 895:835 */       localJSONObject1.put("announcedAddress", Nxt.myAddress);
/* 896:    */     }
/* 897:837 */     if ((Nxt.myHallmark != null) && (Nxt.myHallmark.length() > 0)) {
/* 898:838 */       localJSONObject1.put("hallmark", Nxt.myHallmark);
/* 899:    */     }
/* 900:840 */     localJSONObject1.put("application", "NRS");
/* 901:841 */     localJSONObject1.put("version", "0.7.1");
/* 902:842 */     localJSONObject1.put("platform", Nxt.myPlatform);
/* 903:843 */     localJSONObject1.put("scheme", Nxt.myScheme);
/* 904:844 */     localJSONObject1.put("port", Integer.valueOf(Nxt.myPort));
/* 905:845 */     localJSONObject1.put("shareAddress", Boolean.valueOf(Nxt.shareMyAddress));
/* 906:846 */     JSONObject localJSONObject2 = send(JSON.prepareRequest(localJSONObject1));
/* 907:848 */     if (localJSONObject2 != null)
/* 908:    */     {
/* 909:849 */       this.application = ((String)localJSONObject2.get("application"));
/* 910:850 */       this.version = ((String)localJSONObject2.get("version"));
/* 911:851 */       this.platform = ((String)localJSONObject2.get("platform"));
/* 912:852 */       this.shareAddress = Boolean.TRUE.equals(localJSONObject2.get("shareAddress"));
/* 913:854 */       if (analyzeHallmark(this.announcedAddress, (String)localJSONObject2.get("hallmark"))) {
/* 914:855 */         setState(State.CONNECTED);
/* 915:    */       }
/* 916:    */     }
/* 917:    */   }
/* 918:    */   
/* 919:    */   private void updateWeight()
/* 920:    */   {
/* 921:862 */     JSONObject localJSONObject1 = new JSONObject();
/* 922:863 */     localJSONObject1.put("response", "processNewData");
/* 923:    */     
/* 924:865 */     JSONArray localJSONArray = new JSONArray();
/* 925:866 */     JSONObject localJSONObject2 = new JSONObject();
/* 926:867 */     localJSONObject2.put("index", Integer.valueOf(this.index));
/* 927:868 */     localJSONObject2.put("weight", Integer.valueOf(getWeight()));
/* 928:869 */     localJSONArray.add(localJSONObject2);
/* 929:870 */     localJSONObject1.put("changedActivePeers", localJSONArray);
/* 930:    */     
/* 931:872 */     User.sendToAll(localJSONObject1);
/* 932:    */   }
/* 933:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.Peer
 * JD-Core Version:    0.7.0.1
 */