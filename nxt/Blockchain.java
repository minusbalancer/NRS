/*    1:     */ package nxt;
/*    2:     */ 
/*    3:     */ import java.io.FileInputStream;
/*    4:     */ import java.io.FileNotFoundException;
/*    5:     */ import java.io.FileOutputStream;
/*    6:     */ import java.io.IOException;
/*    7:     */ import java.io.ObjectInputStream;
/*    8:     */ import java.io.ObjectOutputStream;
/*    9:     */ import java.math.BigInteger;
/*   10:     */ import java.security.MessageDigest;
/*   11:     */ import java.util.Arrays;
/*   12:     */ import java.util.Collection;
/*   13:     */ import java.util.Collections;
/*   14:     */ import java.util.Comparator;
/*   15:     */ import java.util.HashMap;
/*   16:     */ import java.util.Iterator;
/*   17:     */ import java.util.LinkedList;
/*   18:     */ import java.util.Map;
/*   19:     */ import java.util.Map.Entry;
/*   20:     */ import java.util.PriorityQueue;
/*   21:     */ import java.util.Set;
/*   22:     */ import java.util.TreeSet;
/*   23:     */ import java.util.concurrent.ConcurrentHashMap;
/*   24:     */ import java.util.concurrent.ConcurrentMap;
/*   25:     */ import java.util.concurrent.atomic.AtomicInteger;
/*   26:     */ import java.util.concurrent.atomic.AtomicReference;
/*   27:     */ import nxt.crypto.Crypto;
/*   28:     */ import nxt.peer.Peer;
/*   29:     */ import nxt.peer.Peer.State;
/*   30:     */ import nxt.user.User;
/*   31:     */ import nxt.util.Convert;
/*   32:     */ import nxt.util.JSON;
/*   33:     */ import nxt.util.Logger;
/*   34:     */ import org.json.simple.JSONArray;
/*   35:     */ import org.json.simple.JSONObject;
/*   36:     */ import org.json.simple.JSONStreamAware;
/*   37:     */ 
/*   38:     */ public final class Blockchain
/*   39:     */ {
/*   40:  39 */   private static final byte[] CHECKSUM_TRANSPARENT_FORGING = { 27, -54, -59, -98, 49, -42, 48, -68, -112, 49, 41, 94, -41, 78, -84, 27, -87, -22, -28, 36, -34, -90, 112, -50, -9, 5, 89, -35, 80, -121, -128, 112 };
/*   41:     */   private static volatile Peer lastBlockchainFeeder;
/*   42:  43 */   private static final AtomicInteger blockCounter = new AtomicInteger();
/*   43:  44 */   private static final AtomicReference<Block> lastBlock = new AtomicReference();
/*   44:  45 */   private static final ConcurrentMap<Long, Block> blocks = new ConcurrentHashMap();
/*   45:  47 */   private static final AtomicInteger transactionCounter = new AtomicInteger();
/*   46:  48 */   private static final ConcurrentMap<Long, Transaction> doubleSpendingTransactions = new ConcurrentHashMap();
/*   47:  49 */   private static final ConcurrentMap<Long, Transaction> unconfirmedTransactions = new ConcurrentHashMap();
/*   48:  50 */   private static final ConcurrentMap<Long, Transaction> nonBroadcastedTransactions = new ConcurrentHashMap();
/*   49:  51 */   private static final ConcurrentMap<Long, Transaction> transactions = new ConcurrentHashMap();
/*   50:  53 */   private static final Collection<Block> allBlocks = Collections.unmodifiableCollection(blocks.values());
/*   51:  54 */   private static final Collection<Transaction> allTransactions = Collections.unmodifiableCollection(transactions.values());
/*   52:  55 */   private static final Collection<Transaction> allUnconfirmedTransactions = Collections.unmodifiableCollection(unconfirmedTransactions.values());
/*   53:  57 */   static final ConcurrentMap<String, Transaction> transactionHashes = new ConcurrentHashMap();
/*   54:  59 */   static final Runnable processTransactionsThread = new Runnable()
/*   55:     */   {
/*   56:     */     private final JSONStreamAware getUnconfirmedTransactionsRequest;
/*   57:     */     
/*   58:     */     public void run()
/*   59:     */     {
/*   60:     */       try
/*   61:     */       {
/*   62:     */         try
/*   63:     */         {
/*   64:  73 */           Peer localPeer = Peer.getAnyPeer(Peer.State.CONNECTED, true);
/*   65:  74 */           if (localPeer != null)
/*   66:     */           {
/*   67:  76 */             JSONObject localJSONObject = localPeer.send(this.getUnconfirmedTransactionsRequest);
/*   68:  77 */             if (localJSONObject != null) {
/*   69:     */               try
/*   70:     */               {
/*   71:  79 */                 Blockchain.processUnconfirmedTransactions(localJSONObject);
/*   72:     */               }
/*   73:     */               catch (NxtException.ValidationException localValidationException)
/*   74:     */               {
/*   75:  81 */                 localPeer.blacklist(localValidationException);
/*   76:     */               }
/*   77:     */             }
/*   78:     */           }
/*   79:     */         }
/*   80:     */         catch (Exception localException)
/*   81:     */         {
/*   82:  88 */           Logger.logDebugMessage("Error processing unconfirmed transactions from peer", localException);
/*   83:     */         }
/*   84:     */       }
/*   85:     */       catch (Throwable localThrowable)
/*   86:     */       {
/*   87:  91 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*   88:  92 */         localThrowable.printStackTrace();
/*   89:  93 */         System.exit(1);
/*   90:     */       }
/*   91:     */     }
/*   92:     */   };
/*   93: 100 */   static final Runnable removeUnconfirmedTransactionsThread = new Runnable()
/*   94:     */   {
/*   95:     */     public void run()
/*   96:     */     {
/*   97:     */       try
/*   98:     */       {
/*   99:     */         try
/*  100:     */         {
/*  101: 108 */           int i = Convert.getEpochTime();
/*  102: 109 */           JSONArray localJSONArray = new JSONArray();
/*  103:     */           
/*  104: 111 */           Iterator localIterator = Blockchain.unconfirmedTransactions.values().iterator();
/*  105:     */           Object localObject;
/*  106: 112 */           while (localIterator.hasNext())
/*  107:     */           {
/*  108: 114 */             localObject = (Transaction)localIterator.next();
/*  109: 115 */             if (((Transaction)localObject).getExpiration() < i)
/*  110:     */             {
/*  111: 117 */               localIterator.remove();
/*  112:     */               
/*  113: 119 */               Account localAccount = Account.getAccount(((Transaction)localObject).getSenderAccountId());
/*  114: 120 */               localAccount.addToUnconfirmedBalance((((Transaction)localObject).getAmount() + ((Transaction)localObject).getFee()) * 100L);
/*  115:     */               
/*  116: 122 */               JSONObject localJSONObject = new JSONObject();
/*  117: 123 */               localJSONObject.put("index", Integer.valueOf(((Transaction)localObject).getIndex()));
/*  118: 124 */               localJSONArray.add(localJSONObject);
/*  119:     */             }
/*  120:     */           }
/*  121: 130 */           if (localJSONArray.size() > 0)
/*  122:     */           {
/*  123: 132 */             localObject = new JSONObject();
/*  124: 133 */             ((JSONObject)localObject).put("response", "processNewData");
/*  125:     */             
/*  126: 135 */             ((JSONObject)localObject).put("removedUnconfirmedTransactions", localJSONArray);
/*  127:     */             
/*  128:     */ 
/*  129: 138 */             User.sendToAll((JSONStreamAware)localObject);
/*  130:     */           }
/*  131:     */         }
/*  132:     */         catch (Exception localException)
/*  133:     */         {
/*  134: 143 */           Logger.logDebugMessage("Error removing unconfirmed transactions", localException);
/*  135:     */         }
/*  136:     */       }
/*  137:     */       catch (Throwable localThrowable)
/*  138:     */       {
/*  139: 147 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  140: 148 */         localThrowable.printStackTrace();
/*  141: 149 */         System.exit(1);
/*  142:     */       }
/*  143:     */     }
/*  144:     */   };
/*  145: 156 */   static final Runnable getMoreBlocksThread = new Runnable()
/*  146:     */   {
/*  147:     */     private final JSONStreamAware getCumulativeDifficultyRequest;
/*  148:     */     private final JSONStreamAware getMilestoneBlockIdsRequest;
/*  149:     */     
/*  150:     */     public void run()
/*  151:     */     {
/*  152:     */       try
/*  153:     */       {
/*  154:     */         try
/*  155:     */         {
/*  156: 177 */           Peer localPeer = Peer.getAnyPeer(Peer.State.CONNECTED, true);
/*  157: 178 */           if (localPeer != null)
/*  158:     */           {
/*  159: 180 */             Blockchain.access$202(localPeer);
/*  160:     */             
/*  161: 182 */             JSONObject localJSONObject1 = localPeer.send(this.getCumulativeDifficultyRequest);
/*  162: 183 */             if (localJSONObject1 != null)
/*  163:     */             {
/*  164: 185 */               BigInteger localBigInteger1 = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty();
/*  165: 186 */               String str = (String)localJSONObject1.get("cumulativeDifficulty");
/*  166: 187 */               if (str == null) {
/*  167: 188 */                 return;
/*  168:     */               }
/*  169: 190 */               BigInteger localBigInteger2 = new BigInteger(str);
/*  170: 191 */               if (localBigInteger2.compareTo(localBigInteger1) > 0)
/*  171:     */               {
/*  172: 193 */                 localJSONObject1 = localPeer.send(this.getMilestoneBlockIdsRequest);
/*  173: 194 */                 if (localJSONObject1 != null)
/*  174:     */                 {
/*  175: 196 */                   Object localObject1 = Genesis.GENESIS_BLOCK_ID;
/*  176:     */                   
/*  177: 198 */                   JSONArray localJSONArray1 = (JSONArray)localJSONObject1.get("milestoneBlockIds");
/*  178: 199 */                   if (localJSONArray1 == null) {
/*  179: 200 */                     return;
/*  180:     */                   }
/*  181: 202 */                   for (Object localObject3 : localJSONArray1)
/*  182:     */                   {
/*  183: 204 */                     localObject4 = Convert.parseUnsignedLong((String)localObject3);
/*  184: 205 */                     localObject5 = (Block)Blockchain.blocks.get(localObject4);
/*  185: 206 */                     if (localObject5 != null)
/*  186:     */                     {
/*  187: 208 */                       localObject1 = localObject4;
/*  188:     */                       
/*  189: 210 */                       break;
/*  190:     */                     }
/*  191:     */                   }
/*  192:     */                   Object localObject4;
/*  193:     */                   Object localObject5;
/*  194:     */                   int j;
/*  195:     */                   int i;
/*  196:     */                   Object localObject6;
/*  197:     */                   do
/*  198:     */                   {
/*  199: 220 */                     localObject4 = new JSONObject();
/*  200: 221 */                     ((JSONObject)localObject4).put("requestType", "getNextBlockIds");
/*  201: 222 */                     ((JSONObject)localObject4).put("blockId", Convert.convert((Long)localObject1));
/*  202: 223 */                     localJSONObject1 = localPeer.send(JSON.prepareRequest((JSONObject)localObject4));
/*  203: 224 */                     if (localJSONObject1 == null) {
/*  204: 225 */                       return;
/*  205:     */                     }
/*  206: 228 */                     localObject5 = (JSONArray)localJSONObject1.get("nextBlockIds");
/*  207: 229 */                     if ((localObject5 == null) || ((j = ((JSONArray)localObject5).size()) == 0)) {
/*  208: 230 */                       return;
/*  209:     */                     }
/*  210: 234 */                     for (i = 0; i < j; i++)
/*  211:     */                     {
/*  212: 235 */                       localObject6 = Convert.parseUnsignedLong((String)((JSONArray)localObject5).get(i));
/*  213: 236 */                       if (Blockchain.blocks.get(localObject6) == null) {
/*  214:     */                         break;
/*  215:     */                       }
/*  216: 239 */                       localObject1 = localObject6;
/*  217:     */                     }
/*  218: 242 */                   } while (i == j);
/*  219: 245 */                   if (((Block)Blockchain.lastBlock.get()).getHeight() - ((Block)Blockchain.blocks.get(localObject1)).getHeight() < 720)
/*  220:     */                   {
/*  221: 247 */                     Object localObject2 = localObject1;
/*  222: 248 */                     LinkedList localLinkedList = new LinkedList();
/*  223: 249 */                     localObject4 = new HashMap();
/*  224:     */                     Object localObject7;
/*  225:     */                     for (;;)
/*  226:     */                     {
/*  227: 253 */                       localObject5 = new JSONObject();
/*  228: 254 */                       ((JSONObject)localObject5).put("requestType", "getNextBlocks");
/*  229: 255 */                       ((JSONObject)localObject5).put("blockId", Convert.convert((Long)localObject2));
/*  230: 256 */                       localJSONObject1 = localPeer.send(JSON.prepareRequest((JSONObject)localObject5));
/*  231: 257 */                       if (localJSONObject1 == null) {
/*  232:     */                         break;
/*  233:     */                       }
/*  234: 261 */                       localObject6 = (JSONArray)localJSONObject1.get("nextBlocks");
/*  235: 262 */                       if ((localObject6 == null) || (((JSONArray)localObject6).size() == 0)) {
/*  236:     */                         break;
/*  237:     */                       }
/*  238: 266 */                       synchronized (Blockchain.class)
/*  239:     */                       {
/*  240: 268 */                         for (localObject7 = ((JSONArray)localObject6).iterator(); ((Iterator)localObject7).hasNext();)
/*  241:     */                         {
/*  242: 268 */                           Object localObject8 = ((Iterator)localObject7).next();
/*  243: 269 */                           JSONObject localJSONObject2 = (JSONObject)localObject8;
/*  244:     */                           Block localBlock;
/*  245:     */                           try
/*  246:     */                           {
/*  247: 272 */                             localBlock = Block.getBlock(localJSONObject2);
/*  248:     */                           }
/*  249:     */                           catch (NxtException.ValidationException localValidationException1)
/*  250:     */                           {
/*  251: 274 */                             localPeer.blacklist(localValidationException1);
/*  252: 275 */                             return;
/*  253:     */                           }
/*  254: 277 */                           localObject2 = localBlock.getId();
/*  255:     */                           JSONArray localJSONArray2;
/*  256: 279 */                           if (((Block)Blockchain.lastBlock.get()).getId().equals(localBlock.getPreviousBlockId()))
/*  257:     */                           {
/*  258: 281 */                             localJSONArray2 = (JSONArray)localJSONObject2.get("transactions");
/*  259:     */                             try
/*  260:     */                             {
/*  261: 283 */                               Transaction[] arrayOfTransaction = new Transaction[localJSONArray2.size()];
/*  262: 284 */                               for (int n = 0; n < arrayOfTransaction.length; n++) {
/*  263: 285 */                                 arrayOfTransaction[n] = Transaction.getTransaction((JSONObject)localJSONArray2.get(n));
/*  264:     */                               }
/*  265:     */                               try
/*  266:     */                               {
/*  267: 288 */                                 Blockchain.pushBlock(localBlock, arrayOfTransaction, false);
/*  268:     */                               }
/*  269:     */                               catch (Blockchain.BlockNotAcceptedException localBlockNotAcceptedException2)
/*  270:     */                               {
/*  271: 290 */                                 Logger.logDebugMessage("Failed to accept block " + localBlock.getStringId() + " at height " + ((Block)Blockchain.lastBlock.get()).getHeight() + " received from " + localPeer.getPeerAddress() + ", blacklisting");
/*  272:     */                                 
/*  273:     */ 
/*  274: 293 */                                 localPeer.blacklist(localBlockNotAcceptedException2);
/*  275: 294 */                                 return;
/*  276:     */                               }
/*  277:     */                             }
/*  278:     */                             catch (NxtException.ValidationException localValidationException2)
/*  279:     */                             {
/*  280: 297 */                               localPeer.blacklist(localValidationException2);
/*  281: 298 */                               return;
/*  282:     */                             }
/*  283:     */                           }
/*  284: 301 */                           else if ((Blockchain.blocks.get(localBlock.getId()) == null) && (localBlock.transactionIds.length <= 255))
/*  285:     */                           {
/*  286: 303 */                             localLinkedList.add(localBlock);
/*  287:     */                             
/*  288: 305 */                             localJSONArray2 = (JSONArray)localJSONObject2.get("transactions");
/*  289:     */                             try
/*  290:     */                             {
/*  291: 307 */                               for (int m = 0; m < localBlock.transactionIds.length; m++)
/*  292:     */                               {
/*  293: 309 */                                 Transaction localTransaction = Transaction.getTransaction((JSONObject)localJSONArray2.get(m));
/*  294: 310 */                                 localBlock.transactionIds[m] = localTransaction.getId();
/*  295: 311 */                                 localBlock.blockTransactions[m] = localTransaction;
/*  296: 312 */                                 ((HashMap)localObject4).put(localBlock.transactionIds[m], localTransaction);
/*  297:     */                               }
/*  298:     */                             }
/*  299:     */                             catch (NxtException.ValidationException localValidationException3)
/*  300:     */                             {
/*  301: 316 */                               localPeer.blacklist(localValidationException3);
/*  302: 317 */                               return;
/*  303:     */                             }
/*  304:     */                           }
/*  305:     */                         }
/*  306:     */                       }
/*  307:     */                     }
/*  308: 327 */                     if ((!localLinkedList.isEmpty()) && (((Block)Blockchain.lastBlock.get()).getHeight() - ((Block)Blockchain.blocks.get(localObject1)).getHeight() < 720)) {
/*  309: 329 */                       synchronized (Blockchain.class)
/*  310:     */                       {
/*  311: 331 */                         Blockchain.saveTransactions("transactions.nxt.bak");
/*  312: 332 */                         Blockchain.saveBlocks("blocks.nxt.bak");
/*  313:     */                         
/*  314: 334 */                         localBigInteger1 = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty();
/*  315:     */                         for (;;)
/*  316:     */                         {
/*  317:     */                           int k;
/*  318:     */                           try
/*  319:     */                           {
/*  320: 338 */                             while ((!((Block)Blockchain.lastBlock.get()).getId().equals(localObject1)) && (Blockchain.access$800())) {}
/*  321: 340 */                             if (((Block)Blockchain.lastBlock.get()).getId().equals(localObject1)) {
/*  322: 341 */                               for (??? = localLinkedList.iterator(); ((Iterator)???).hasNext();)
/*  323:     */                               {
/*  324: 341 */                                 localObject7 = (Block)((Iterator)???).next();
/*  325: 342 */                                 if (((Block)Blockchain.lastBlock.get()).getId().equals(((Block)localObject7).getPreviousBlockId())) {
/*  326:     */                                   try
/*  327:     */                                   {
/*  328: 344 */                                     Blockchain.pushBlock((Block)localObject7, ((Block)localObject7).blockTransactions, false);
/*  329:     */                                   }
/*  330:     */                                   catch (Blockchain.BlockNotAcceptedException localBlockNotAcceptedException1)
/*  331:     */                                   {
/*  332: 346 */                                     Logger.logDebugMessage("Failed to push future block " + ((Block)localObject7).getStringId() + " received from " + localPeer.getPeerAddress() + ", blacklisting");
/*  333:     */                                     
/*  334: 348 */                                     localPeer.blacklist(localBlockNotAcceptedException1);
/*  335: 349 */                                     break;
/*  336:     */                                   }
/*  337:     */                                 }
/*  338:     */                               }
/*  339:     */                             }
/*  340: 355 */                             k = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty().compareTo(localBigInteger1) < 0 ? 1 : 0;
/*  341: 356 */                             if (k != 0)
/*  342:     */                             {
/*  343: 357 */                               Logger.logDebugMessage("Rescan caused by peer " + localPeer.getPeerAddress() + ", blacklisting");
/*  344: 358 */                               localPeer.blacklist();
/*  345:     */                             }
/*  346:     */                           }
/*  347:     */                           catch (Transaction.UndoNotSupportedException localUndoNotSupportedException)
/*  348:     */                           {
/*  349: 361 */                             Logger.logDebugMessage(localUndoNotSupportedException.getMessage());
/*  350: 362 */                             Logger.logDebugMessage("Popping off last block not possible, will do a rescan");
/*  351: 363 */                             k = 1;
/*  352:     */                           }
/*  353:     */                         }
/*  354: 366 */                         if (k != 0)
/*  355:     */                         {
/*  356: 367 */                           Blockchain.loadTransactions("transactions.nxt.bak");
/*  357: 368 */                           Blockchain.loadBlocks("blocks.nxt.bak");
/*  358: 369 */                           Account.clear();
/*  359: 370 */                           Alias.clear();
/*  360: 371 */                           Asset.clear();
/*  361: 372 */                           Order.clear();
/*  362: 373 */                           Blockchain.unconfirmedTransactions.clear();
/*  363: 374 */                           Blockchain.doubleSpendingTransactions.clear();
/*  364: 375 */                           Blockchain.nonBroadcastedTransactions.clear();
/*  365: 376 */                           Blockchain.transactionHashes.clear();
/*  366: 377 */                           Logger.logMessage("Re-scanning blockchain...");
/*  367: 378 */                           Blockchain.access$1300();
/*  368: 379 */                           Logger.logMessage("...Done");
/*  369:     */                         }
/*  370:     */                       }
/*  371:     */                     }
/*  372: 384 */                     synchronized (Blockchain.class)
/*  373:     */                     {
/*  374: 385 */                       Blockchain.saveTransactions("transactions.nxt");
/*  375: 386 */                       Blockchain.saveBlocks("blocks.nxt");
/*  376:     */                     }
/*  377:     */                   }
/*  378:     */                 }
/*  379:     */               }
/*  380:     */             }
/*  381:     */           }
/*  382:     */         }
/*  383:     */         catch (Exception localException)
/*  384:     */         {
/*  385: 395 */           Logger.logDebugMessage("Error in milestone blocks processing thread", localException);
/*  386:     */         }
/*  387:     */       }
/*  388:     */       catch (Throwable localThrowable)
/*  389:     */       {
/*  390: 398 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  391: 399 */         localThrowable.printStackTrace();
/*  392: 400 */         System.exit(1);
/*  393:     */       }
/*  394:     */     }
/*  395:     */   };
/*  396: 407 */   static final Runnable generateBlockThread = new Runnable()
/*  397:     */   {
/*  398: 409 */     private final ConcurrentMap<Account, Block> lastBlocks = new ConcurrentHashMap();
/*  399: 410 */     private final ConcurrentMap<Account, BigInteger> hits = new ConcurrentHashMap();
/*  400:     */     
/*  401:     */     public void run()
/*  402:     */     {
/*  403:     */       try
/*  404:     */       {
/*  405:     */         try
/*  406:     */         {
/*  407: 418 */           HashMap localHashMap = new HashMap();
/*  408: 419 */           for (localIterator = User.getAllUsers().iterator(); localIterator.hasNext();)
/*  409:     */           {
/*  410: 419 */             localObject1 = (User)localIterator.next();
/*  411: 420 */             if (((User)localObject1).getSecretPhrase() != null)
/*  412:     */             {
/*  413: 421 */               localAccount = Account.getAccount(((User)localObject1).getPublicKey());
/*  414: 422 */               if ((localAccount != null) && (localAccount.getEffectiveBalance() > 0)) {
/*  415: 423 */                 localHashMap.put(localAccount, localObject1);
/*  416:     */               }
/*  417:     */             }
/*  418:     */           }
/*  419: 428 */           for (localIterator = localHashMap.entrySet().iterator(); localIterator.hasNext();)
/*  420:     */           {
/*  421: 428 */             localObject1 = (Map.Entry)localIterator.next();
/*  422:     */             
/*  423: 430 */             localAccount = (Account)((Map.Entry)localObject1).getKey();
/*  424: 431 */             User localUser = (User)((Map.Entry)localObject1).getValue();
/*  425: 432 */             Block localBlock = (Block)Blockchain.lastBlock.get();
/*  426: 433 */             if (this.lastBlocks.get(localAccount) != localBlock)
/*  427:     */             {
/*  428: 435 */               long l = localAccount.getEffectiveBalance();
/*  429: 436 */               if (l > 0L)
/*  430:     */               {
/*  431: 439 */                 MessageDigest localMessageDigest = Crypto.sha256();
/*  432:     */                 byte[] arrayOfByte;
/*  433: 441 */                 if (localBlock.getHeight() < 30000)
/*  434:     */                 {
/*  435: 443 */                   localObject2 = Crypto.sign(localBlock.getGenerationSignature(), localUser.getSecretPhrase());
/*  436: 444 */                   arrayOfByte = localMessageDigest.digest((byte[])localObject2);
/*  437:     */                 }
/*  438:     */                 else
/*  439:     */                 {
/*  440: 448 */                   localMessageDigest.update(localBlock.getGenerationSignature());
/*  441: 449 */                   arrayOfByte = localMessageDigest.digest(localUser.getPublicKey());
/*  442:     */                 }
/*  443: 452 */                 Object localObject2 = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
/*  444:     */                 
/*  445: 454 */                 this.lastBlocks.put(localAccount, localBlock);
/*  446: 455 */                 this.hits.put(localAccount, localObject2);
/*  447:     */                 
/*  448: 457 */                 JSONObject localJSONObject = new JSONObject();
/*  449: 458 */                 localJSONObject.put("response", "setBlockGenerationDeadline");
/*  450: 459 */                 localJSONObject.put("deadline", Long.valueOf(((BigInteger)localObject2).divide(BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(l))).longValue() - (Convert.getEpochTime() - localBlock.getTimestamp())));
/*  451:     */                 
/*  452: 461 */                 localUser.send(localJSONObject);
/*  453:     */               }
/*  454:     */             }
/*  455:     */             else
/*  456:     */             {
/*  457: 465 */               int i = Convert.getEpochTime() - localBlock.getTimestamp();
/*  458: 466 */               if (i > 0)
/*  459:     */               {
/*  460: 468 */                 BigInteger localBigInteger = BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance())).multiply(BigInteger.valueOf(i));
/*  461: 469 */                 if (((BigInteger)this.hits.get(localAccount)).compareTo(localBigInteger) < 0) {
/*  462: 471 */                   Blockchain.generateBlock(localUser.getSecretPhrase());
/*  463:     */                 }
/*  464:     */               }
/*  465:     */             }
/*  466:     */           }
/*  467:     */         }
/*  468:     */         catch (Exception localException)
/*  469:     */         {
/*  470:     */           Iterator localIterator;
/*  471:     */           Object localObject1;
/*  472:     */           Account localAccount;
/*  473: 480 */           Logger.logDebugMessage("Error in block generation thread", localException);
/*  474:     */         }
/*  475:     */       }
/*  476:     */       catch (Throwable localThrowable)
/*  477:     */       {
/*  478: 483 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  479: 484 */         localThrowable.printStackTrace();
/*  480: 485 */         System.exit(1);
/*  481:     */       }
/*  482:     */     }
/*  483:     */   };
/*  484: 492 */   static final Runnable rebroadcastTransactionsThread = new Runnable()
/*  485:     */   {
/*  486:     */     public void run()
/*  487:     */     {
/*  488:     */       try
/*  489:     */       {
/*  490:     */         try
/*  491:     */         {
/*  492: 499 */           JSONArray localJSONArray = new JSONArray();
/*  493: 501 */           for (Object localObject = Blockchain.nonBroadcastedTransactions.values().iterator(); ((Iterator)localObject).hasNext();)
/*  494:     */           {
/*  495: 501 */             Transaction localTransaction = (Transaction)((Iterator)localObject).next();
/*  496: 503 */             if ((Blockchain.unconfirmedTransactions.get(localTransaction.getId()) == null) && (Blockchain.transactions.get(localTransaction.getId()) == null)) {
/*  497: 505 */               localJSONArray.add(localTransaction.getJSONObject());
/*  498:     */             } else {
/*  499: 509 */               Blockchain.nonBroadcastedTransactions.remove(localTransaction.getId());
/*  500:     */             }
/*  501:     */           }
/*  502: 515 */           if (localJSONArray.size() > 0)
/*  503:     */           {
/*  504: 517 */             localObject = new JSONObject();
/*  505: 518 */             ((JSONObject)localObject).put("requestType", "processTransactions");
/*  506: 519 */             ((JSONObject)localObject).put("transactions", localJSONArray);
/*  507:     */             
/*  508: 521 */             Peer.sendToSomePeers((JSONObject)localObject);
/*  509:     */           }
/*  510:     */         }
/*  511:     */         catch (Exception localException)
/*  512:     */         {
/*  513: 526 */           Logger.logDebugMessage("Error in transaction re-broadcasting thread", localException);
/*  514:     */         }
/*  515:     */       }
/*  516:     */       catch (Throwable localThrowable)
/*  517:     */       {
/*  518: 529 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  519: 530 */         localThrowable.printStackTrace();
/*  520: 531 */         System.exit(1);
/*  521:     */       }
/*  522:     */     }
/*  523:     */   };
/*  524:     */   
/*  525:     */   public static Collection<Block> getAllBlocks()
/*  526:     */   {
/*  527: 539 */     return allBlocks;
/*  528:     */   }
/*  529:     */   
/*  530:     */   public static Collection<Transaction> getAllTransactions()
/*  531:     */   {
/*  532: 543 */     return allTransactions;
/*  533:     */   }
/*  534:     */   
/*  535:     */   public static Collection<Transaction> getAllUnconfirmedTransactions()
/*  536:     */   {
/*  537: 547 */     return allUnconfirmedTransactions;
/*  538:     */   }
/*  539:     */   
/*  540:     */   public static Block getLastBlock()
/*  541:     */   {
/*  542: 551 */     return (Block)lastBlock.get();
/*  543:     */   }
/*  544:     */   
/*  545:     */   public static Block getBlock(Long paramLong)
/*  546:     */   {
/*  547: 555 */     return (Block)blocks.get(paramLong);
/*  548:     */   }
/*  549:     */   
/*  550:     */   public static Transaction getTransaction(Long paramLong)
/*  551:     */   {
/*  552: 559 */     return (Transaction)transactions.get(paramLong);
/*  553:     */   }
/*  554:     */   
/*  555:     */   public static Transaction getUnconfirmedTransaction(Long paramLong)
/*  556:     */   {
/*  557: 563 */     return (Transaction)unconfirmedTransactions.get(paramLong);
/*  558:     */   }
/*  559:     */   
/*  560:     */   public static void broadcast(Transaction paramTransaction)
/*  561:     */   {
/*  562: 568 */     JSONObject localJSONObject = new JSONObject();
/*  563: 569 */     localJSONObject.put("requestType", "processTransactions");
/*  564: 570 */     JSONArray localJSONArray = new JSONArray();
/*  565: 571 */     localJSONArray.add(paramTransaction.getJSONObject());
/*  566: 572 */     localJSONObject.put("transactions", localJSONArray);
/*  567:     */     
/*  568: 574 */     Peer.sendToSomePeers(localJSONObject);
/*  569:     */     
/*  570: 576 */     nonBroadcastedTransactions.put(paramTransaction.getId(), paramTransaction);
/*  571:     */   }
/*  572:     */   
/*  573:     */   public static Peer getLastBlockchainFeeder()
/*  574:     */   {
/*  575: 580 */     return lastBlockchainFeeder;
/*  576:     */   }
/*  577:     */   
/*  578:     */   public static void processTransactions(JSONObject paramJSONObject)
/*  579:     */     throws NxtException.ValidationException
/*  580:     */   {
/*  581: 584 */     JSONArray localJSONArray = (JSONArray)paramJSONObject.get("transactions");
/*  582: 585 */     processTransactions(localJSONArray, false);
/*  583:     */   }
/*  584:     */   
/*  585:     */   public static boolean pushBlock(JSONObject paramJSONObject)
/*  586:     */     throws NxtException
/*  587:     */   {
/*  588: 590 */     Block localBlock = Block.getBlock(paramJSONObject);
/*  589: 591 */     if (!((Block)lastBlock.get()).getId().equals(localBlock.getPreviousBlockId())) {
/*  590: 594 */       return false;
/*  591:     */     }
/*  592: 596 */     JSONArray localJSONArray = (JSONArray)paramJSONObject.get("transactions");
/*  593: 597 */     Transaction[] arrayOfTransaction = new Transaction[localJSONArray.size()];
/*  594: 598 */     for (int i = 0; i < arrayOfTransaction.length; i++) {
/*  595: 599 */       arrayOfTransaction[i] = Transaction.getTransaction((JSONObject)localJSONArray.get(i));
/*  596:     */     }
/*  597:     */     try
/*  598:     */     {
/*  599: 602 */       pushBlock(localBlock, arrayOfTransaction, true);
/*  600: 603 */       return true;
/*  601:     */     }
/*  602:     */     catch (BlockNotAcceptedException localBlockNotAcceptedException)
/*  603:     */     {
/*  604: 605 */       Logger.logDebugMessage("Block " + localBlock.getStringId() + " not accepted: " + localBlockNotAcceptedException.getMessage());
/*  605: 606 */       throw localBlockNotAcceptedException;
/*  606:     */     }
/*  607:     */   }
/*  608:     */   
/*  609:     */   static void addBlock(Block paramBlock)
/*  610:     */   {
/*  611: 611 */     if (paramBlock.getPreviousBlockId() == null)
/*  612:     */     {
/*  613: 612 */       blocks.put(paramBlock.getId(), paramBlock);
/*  614: 613 */       lastBlock.set(paramBlock);
/*  615:     */     }
/*  616:     */     else
/*  617:     */     {
/*  618: 615 */       if (!lastBlock.compareAndSet(blocks.get(paramBlock.getPreviousBlockId()), paramBlock)) {
/*  619: 616 */         throw new IllegalStateException("Last block not equal to this.previousBlock");
/*  620:     */       }
/*  621: 618 */       if (blocks.putIfAbsent(paramBlock.getId(), paramBlock) != null) {
/*  622: 619 */         throw new IllegalStateException("duplicate block id: " + paramBlock.getId());
/*  623:     */       }
/*  624:     */     }
/*  625:     */   }
/*  626:     */   
/*  627:     */   static void init()
/*  628:     */   {
/*  629:     */     Object localObject1;
/*  630:     */     try
/*  631:     */     {
/*  632: 628 */       Logger.logMessage("Loading transactions...");
/*  633: 629 */       loadTransactions("transactions.nxt");
/*  634: 630 */       Logger.logMessage("...Done");
/*  635:     */     }
/*  636:     */     catch (FileNotFoundException localFileNotFoundException1)
/*  637:     */     {
/*  638: 633 */       Logger.logMessage("transactions.nxt not found, starting from scratch");
/*  639: 634 */       transactions.clear();
/*  640:     */       Transaction localTransaction;
/*  641:     */       try
/*  642:     */       {
/*  643: 638 */         for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++)
/*  644:     */         {
/*  645: 640 */           localTransaction = Transaction.newTransaction(0, (short)0, Genesis.CREATOR_PUBLIC_KEY, Genesis.GENESIS_RECIPIENTS[i], Genesis.GENESIS_AMOUNTS[i], 0, null, Genesis.GENESIS_SIGNATURES[i]);
/*  646:     */           
/*  647:     */ 
/*  648: 643 */           transactions.put(localTransaction.getId(), localTransaction);
/*  649:     */         }
/*  650:     */       }
/*  651:     */       catch (NxtException.ValidationException localValidationException1)
/*  652:     */       {
/*  653: 648 */         Logger.logMessage(localValidationException1.getMessage());
/*  654: 649 */         System.exit(1);
/*  655:     */       }
/*  656: 652 */       for (localObject1 = transactions.values().iterator(); ((Iterator)localObject1).hasNext();)
/*  657:     */       {
/*  658: 652 */         localTransaction = (Transaction)((Iterator)localObject1).next();
/*  659: 653 */         localTransaction.setIndex(transactionCounter.incrementAndGet());
/*  660: 654 */         localTransaction.setBlockId(Genesis.GENESIS_BLOCK_ID);
/*  661:     */       }
/*  662: 658 */       saveTransactions("transactions.nxt");
/*  663:     */     }
/*  664:     */     try
/*  665:     */     {
/*  666: 664 */       Logger.logMessage("Loading blocks...");
/*  667: 665 */       loadBlocks("blocks.nxt");
/*  668: 666 */       Logger.logMessage("...Done");
/*  669:     */     }
/*  670:     */     catch (FileNotFoundException localFileNotFoundException2)
/*  671:     */     {
/*  672: 669 */       Logger.logMessage("blocks.nxt not found, starting from scratch");
/*  673: 670 */       blocks.clear();
/*  674:     */       try
/*  675:     */       {
/*  676: 674 */         localObject1 = new Block(-1, 0, null, transactions.size(), 1000000000, 0, transactions.size() * 128, null, Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE);
/*  677:     */         
/*  678: 676 */         ((Block)localObject1).setIndex(blockCounter.incrementAndGet());
/*  679:     */         
/*  680: 678 */         int j = 0;
/*  681: 679 */         for (Object localObject2 = transactions.keySet().iterator(); ((Iterator)localObject2).hasNext();)
/*  682:     */         {
/*  683: 679 */           localObject3 = (Long)((Iterator)localObject2).next();
/*  684:     */           
/*  685: 681 */           ((Block)localObject1).transactionIds[(j++)] = localObject3;
/*  686:     */         }
/*  687:     */         Object localObject3;
/*  688: 684 */         Arrays.sort(((Block)localObject1).transactionIds);
/*  689: 685 */         localObject2 = Crypto.sha256();
/*  690: 686 */         for (j = 0; j < ((Block)localObject1).transactionIds.length; j++)
/*  691:     */         {
/*  692: 687 */           localObject3 = (Transaction)transactions.get(localObject1.transactionIds[j]);
/*  693: 688 */           ((MessageDigest)localObject2).update(((Transaction)localObject3).getBytes());
/*  694: 689 */           ((Block)localObject1).blockTransactions[j] = localObject3;
/*  695:     */         }
/*  696: 691 */         ((Block)localObject1).setPayloadHash(((MessageDigest)localObject2).digest());
/*  697:     */         
/*  698: 693 */         blocks.put(Genesis.GENESIS_BLOCK_ID, localObject1);
/*  699: 694 */         lastBlock.set(localObject1);
/*  700:     */       }
/*  701:     */       catch (NxtException.ValidationException localValidationException2)
/*  702:     */       {
/*  703: 697 */         Logger.logMessage(localValidationException2.getMessage());
/*  704: 698 */         System.exit(1);
/*  705:     */       }
/*  706: 701 */       saveBlocks("blocks.nxt");
/*  707:     */     }
/*  708: 705 */     Logger.logMessage("Scanning blockchain...");
/*  709: 706 */     scan();
/*  710: 707 */     Logger.logMessage("...Done");
/*  711:     */   }
/*  712:     */   
/*  713:     */   static void shutdown()
/*  714:     */   {
/*  715:     */     try
/*  716:     */     {
/*  717: 712 */       saveBlocks("blocks.nxt");
/*  718: 713 */       Logger.logMessage("Saved blocks.nxt");
/*  719:     */     }
/*  720:     */     catch (RuntimeException localRuntimeException1)
/*  721:     */     {
/*  722: 715 */       Logger.logMessage("Error saving blocks", localRuntimeException1);
/*  723:     */     }
/*  724:     */     try
/*  725:     */     {
/*  726: 719 */       saveTransactions("transactions.nxt");
/*  727: 720 */       Logger.logMessage("Saved transactions.nxt");
/*  728:     */     }
/*  729:     */     catch (RuntimeException localRuntimeException2)
/*  730:     */     {
/*  731: 722 */       Logger.logMessage("Error saving transactions", localRuntimeException2);
/*  732:     */     }
/*  733:     */   }
/*  734:     */   
/*  735:     */   private static void processUnconfirmedTransactions(JSONObject paramJSONObject)
/*  736:     */     throws NxtException.ValidationException
/*  737:     */   {
/*  738: 727 */     JSONArray localJSONArray = (JSONArray)paramJSONObject.get("unconfirmedTransactions");
/*  739: 728 */     processTransactions(localJSONArray, true);
/*  740:     */   }
/*  741:     */   
/*  742:     */   private static void processTransactions(JSONArray paramJSONArray, boolean paramBoolean)
/*  743:     */     throws NxtException.ValidationException
/*  744:     */   {
/*  745: 733 */     JSONArray localJSONArray = new JSONArray();
/*  746: 735 */     for (Object localObject1 = paramJSONArray.iterator(); ((Iterator)localObject1).hasNext();)
/*  747:     */     {
/*  748: 735 */       Object localObject2 = ((Iterator)localObject1).next();
/*  749:     */       try
/*  750:     */       {
/*  751: 739 */         Transaction localTransaction = Transaction.getTransaction((JSONObject)localObject2);
/*  752:     */         
/*  753: 741 */         int i = Convert.getEpochTime();
/*  754: 742 */         if ((localTransaction.getTimestamp() > i + 15) || (localTransaction.getExpiration() < i) || (localTransaction.getDeadline() <= 1440))
/*  755:     */         {
/*  756:     */           boolean bool;
/*  757: 749 */           synchronized (Blockchain.class)
/*  758:     */           {
/*  759: 751 */             localObject3 = localTransaction.getId();
/*  760: 752 */             if (((!transactions.containsKey(localObject3)) && (!unconfirmedTransactions.containsKey(localObject3)) && (!doubleSpendingTransactions.containsKey(localObject3)) && (!localTransaction.verify())) || 
/*  761:     */             
/*  762:     */ 
/*  763:     */ 
/*  764:     */ 
/*  765: 757 */               (transactionHashes.containsKey(localTransaction.getHash()))) {
/*  766:     */               continue;
/*  767:     */             }
/*  768: 761 */             bool = localTransaction.isDoubleSpending();
/*  769:     */             
/*  770: 763 */             localTransaction.setIndex(transactionCounter.incrementAndGet());
/*  771: 765 */             if (bool)
/*  772:     */             {
/*  773: 767 */               doubleSpendingTransactions.put(localObject3, localTransaction);
/*  774:     */             }
/*  775:     */             else
/*  776:     */             {
/*  777: 771 */               unconfirmedTransactions.put(localObject3, localTransaction);
/*  778: 773 */               if (!paramBoolean) {
/*  779: 775 */                 localJSONArray.add(localObject2);
/*  780:     */               }
/*  781:     */             }
/*  782:     */           }
/*  783: 783 */           ??? = new JSONObject();
/*  784: 784 */           ((JSONObject)???).put("response", "processNewData");
/*  785:     */           
/*  786: 786 */           Object localObject3 = new JSONArray();
/*  787: 787 */           JSONObject localJSONObject = new JSONObject();
/*  788: 788 */           localJSONObject.put("index", Integer.valueOf(localTransaction.getIndex()));
/*  789: 789 */           localJSONObject.put("timestamp", Integer.valueOf(localTransaction.getTimestamp()));
/*  790: 790 */           localJSONObject.put("deadline", Short.valueOf(localTransaction.getDeadline()));
/*  791: 791 */           localJSONObject.put("recipient", Convert.convert(localTransaction.getRecipientId()));
/*  792: 792 */           localJSONObject.put("amount", Integer.valueOf(localTransaction.getAmount()));
/*  793: 793 */           localJSONObject.put("fee", Integer.valueOf(localTransaction.getFee()));
/*  794: 794 */           localJSONObject.put("sender", Convert.convert(localTransaction.getSenderAccountId()));
/*  795: 795 */           localJSONObject.put("id", localTransaction.getStringId());
/*  796: 796 */           ((JSONArray)localObject3).add(localJSONObject);
/*  797: 798 */           if (bool) {
/*  798: 800 */             ((JSONObject)???).put("addedDoubleSpendingTransactions", localObject3);
/*  799:     */           } else {
/*  800: 804 */             ((JSONObject)???).put("addedUnconfirmedTransactions", localObject3);
/*  801:     */           }
/*  802: 808 */           User.sendToAll((JSONStreamAware)???);
/*  803:     */         }
/*  804:     */       }
/*  805:     */       catch (RuntimeException localRuntimeException)
/*  806:     */       {
/*  807: 812 */         Logger.logMessage("Error processing transaction", localRuntimeException);
/*  808:     */       }
/*  809:     */     }
/*  810: 818 */     if (localJSONArray.size() > 0)
/*  811:     */     {
/*  812: 820 */       localObject1 = new JSONObject();
/*  813: 821 */       ((JSONObject)localObject1).put("requestType", "processTransactions");
/*  814: 822 */       ((JSONObject)localObject1).put("transactions", localJSONArray);
/*  815:     */       
/*  816: 824 */       Peer.sendToSomePeers((JSONObject)localObject1);
/*  817:     */     }
/*  818:     */   }
/*  819:     */   
/*  820:     */   private static synchronized byte[] calculateTransactionsChecksum()
/*  821:     */   {
/*  822: 831 */     PriorityQueue localPriorityQueue = new PriorityQueue(transactions.size(), new Comparator()
/*  823:     */     {
/*  824:     */       public int compare(Transaction paramAnonymousTransaction1, Transaction paramAnonymousTransaction2)
/*  825:     */       {
/*  826: 834 */         long l1 = paramAnonymousTransaction1.getId().longValue();
/*  827: 835 */         long l2 = paramAnonymousTransaction2.getId().longValue();
/*  828: 836 */         return paramAnonymousTransaction1.getTimestamp() > paramAnonymousTransaction2.getTimestamp() ? 1 : paramAnonymousTransaction1.getTimestamp() < paramAnonymousTransaction2.getTimestamp() ? -1 : l1 > l2 ? 1 : l1 < l2 ? -1 : 0;
/*  829:     */       }
/*  830: 838 */     });
/*  831: 839 */     localPriorityQueue.addAll(transactions.values());
/*  832: 840 */     MessageDigest localMessageDigest = Crypto.sha256();
/*  833: 841 */     while (!localPriorityQueue.isEmpty()) {
/*  834: 842 */       localMessageDigest.update(((Transaction)localPriorityQueue.poll()).getBytes());
/*  835:     */     }
/*  836: 844 */     return localMessageDigest.digest();
/*  837:     */   }
/*  838:     */   
/*  839:     */   private static void pushBlock(Block paramBlock, Transaction[] paramArrayOfTransaction, boolean paramBoolean)
/*  840:     */     throws Blockchain.BlockNotAcceptedException
/*  841:     */   {
/*  842: 851 */     int i = Convert.getEpochTime();
/*  843:     */     JSONArray localJSONArray1;
/*  844:     */     JSONArray localJSONArray2;
/*  845: 853 */     synchronized (Blockchain.class)
/*  846:     */     {
/*  847:     */       try
/*  848:     */       {
/*  849: 856 */         Block localBlock = (Block)lastBlock.get();
/*  850: 858 */         if (!localBlock.getId().equals(paramBlock.getPreviousBlockId())) {
/*  851: 859 */           throw new BlockOutOfOrderException("Previous block id doesn't match", null);
/*  852:     */         }
/*  853: 862 */         if (paramBlock.getVersion() != (localBlock.getHeight() < 30000 ? 1 : 2)) {
/*  854: 863 */           throw new BlockNotAcceptedException("Invalid version " + paramBlock.getVersion(), null);
/*  855:     */         }
/*  856: 866 */         if (localBlock.getHeight() == 30000)
/*  857:     */         {
/*  858: 867 */           localObject1 = calculateTransactionsChecksum();
/*  859: 868 */           if (CHECKSUM_TRANSPARENT_FORGING == null)
/*  860:     */           {
/*  861: 869 */             Logger.logMessage("Checksum calculated:\n" + Arrays.toString((byte[])localObject1));
/*  862:     */           }
/*  863:     */           else
/*  864:     */           {
/*  865: 870 */             if (!Arrays.equals((byte[])localObject1, CHECKSUM_TRANSPARENT_FORGING))
/*  866:     */             {
/*  867: 871 */               Logger.logMessage("Checksum failed at block 30000");
/*  868: 872 */               throw new BlockNotAcceptedException("Checksum failed", null);
/*  869:     */             }
/*  870: 874 */             Logger.logMessage("Checksum passed at block 30000");
/*  871:     */           }
/*  872:     */         }
/*  873: 878 */         if ((paramBlock.getVersion() != 1) && (!Arrays.equals(Crypto.sha256().digest(localBlock.getBytes()), paramBlock.getPreviousBlockHash()))) {
/*  874: 879 */           throw new BlockNotAcceptedException("Previous block hash doesn't match", null);
/*  875:     */         }
/*  876: 881 */         if ((paramBlock.getTimestamp() > i + 15) || (paramBlock.getTimestamp() <= localBlock.getTimestamp())) {
/*  877: 882 */           throw new BlockNotAcceptedException("Invalid timestamp: " + paramBlock.getTimestamp() + " current time is " + i + ", previous block timestamp is " + localBlock.getTimestamp(), null);
/*  878:     */         }
/*  879: 886 */         if ((paramBlock.getId().equals(Long.valueOf(0L))) || (blocks.containsKey(paramBlock.getId()))) {
/*  880: 887 */           throw new BlockNotAcceptedException("Duplicate block or invalid id", null);
/*  881:     */         }
/*  882: 889 */         if ((!paramBlock.verifyGenerationSignature()) || (!paramBlock.verifyBlockSignature())) {
/*  883: 890 */           throw new BlockNotAcceptedException("Signature verification failed", null);
/*  884:     */         }
/*  885: 893 */         paramBlock.setIndex(blockCounter.incrementAndGet());
/*  886:     */         
/*  887: 895 */         localObject1 = new HashMap();
/*  888: 896 */         for (int j = 0; j < paramBlock.transactionIds.length; j++)
/*  889:     */         {
/*  890: 897 */           localObject2 = paramArrayOfTransaction[j];
/*  891: 898 */           ((Transaction)localObject2).setIndex(transactionCounter.incrementAndGet());
/*  892: 899 */           if (((Map)localObject1).put(paramBlock.transactionIds[j] =  = ((Transaction)localObject2).getId(), localObject2) != null) {
/*  893: 900 */             throw new BlockNotAcceptedException("Block contains duplicate transactions: " + ((Transaction)localObject2).getStringId(), null);
/*  894:     */           }
/*  895:     */         }
/*  896: 903 */         Arrays.sort(paramBlock.transactionIds);
/*  897:     */         
/*  898: 905 */         HashMap localHashMap1 = new HashMap();
/*  899: 906 */         Object localObject2 = new HashMap();
/*  900: 907 */         HashMap localHashMap2 = new HashMap();
/*  901: 908 */         int k = 0;int m = 0;
/*  902: 909 */         MessageDigest localMessageDigest = Crypto.sha256();
/*  903:     */         Object localObject6;
/*  904:     */         Object localObject7;
/*  905:     */         Object localObject8;
/*  906: 910 */         for (localObject6 : paramBlock.transactionIds)
/*  907:     */         {
/*  908: 912 */           localObject7 = (Transaction)((Map)localObject1).get(localObject6);
/*  909: 914 */           if ((((Transaction)localObject7).getTimestamp() > i + 15) || (((Transaction)localObject7).getTimestamp() > paramBlock.getTimestamp() + 15) || ((((Transaction)localObject7).getExpiration() < paramBlock.getTimestamp()) && (localBlock.getHeight() != 303))) {
/*  910: 916 */             throw new BlockNotAcceptedException("Invalid transaction timestamp " + ((Transaction)localObject7).getTimestamp() + " for transaction " + ((Transaction)localObject7).getStringId() + ", current time is " + i + ", block timestamp is " + paramBlock.getTimestamp(), null);
/*  911:     */           }
/*  912: 920 */           if (transactions.get(localObject6) != null) {
/*  913: 921 */             throw new BlockNotAcceptedException("Transaction " + ((Transaction)localObject7).getStringId() + " is already in the blockchain", null);
/*  914:     */           }
/*  915: 923 */           localObject8 = ((Transaction)localObject7).getReferencedTransactionId();
/*  916: 924 */           if ((localObject8 != null) && (transactions.get(localObject8) == null) && (((Map)localObject1).get(localObject8) == null)) {
/*  917: 927 */             throw new BlockNotAcceptedException("Missing referenced transaction " + Convert.convert((Long)localObject8) + " for transaction " + ((Transaction)localObject7).getStringId(), null);
/*  918:     */           }
/*  919: 930 */           if ((unconfirmedTransactions.get(localObject6) == null) && (!((Transaction)localObject7).verify())) {
/*  920: 931 */             throw new BlockNotAcceptedException("Signature verification failed for transaction " + ((Transaction)localObject7).getStringId(), null);
/*  921:     */           }
/*  922: 933 */           if (((Transaction)localObject7).getId().equals(Long.valueOf(0L))) {
/*  923: 934 */             throw new BlockNotAcceptedException("Invalid transaction id", null);
/*  924:     */           }
/*  925: 936 */           if (((Transaction)localObject7).isDuplicate(localHashMap1)) {
/*  926: 937 */             throw new BlockNotAcceptedException("Transaction is a duplicate: " + ((Transaction)localObject7).getStringId(), null);
/*  927:     */           }
/*  928: 940 */           k += ((Transaction)localObject7).getAmount();
/*  929:     */           
/*  930: 942 */           ((Transaction)localObject7).updateTotals((Map)localObject2, localHashMap2);
/*  931:     */           
/*  932: 944 */           m += ((Transaction)localObject7).getFee();
/*  933:     */           
/*  934: 946 */           localMessageDigest.update(((Transaction)localObject7).getBytes());
/*  935:     */         }
/*  936: 950 */         if ((k != paramBlock.getTotalAmount()) || (m != paramBlock.getTotalFee())) {
/*  937: 951 */           throw new BlockNotAcceptedException("Total amount or fee don't match transaction totals", null);
/*  938:     */         }
/*  939: 953 */         if (!Arrays.equals(localMessageDigest.digest(), paramBlock.getPayloadHash())) {
/*  940: 954 */           throw new BlockNotAcceptedException("Payload hash doesn't match", null);
/*  941:     */         }
/*  942: 956 */         for (??? = ((Map)localObject2).entrySet().iterator(); ((Iterator)???).hasNext();)
/*  943:     */         {
/*  944: 956 */           localObject4 = (Map.Entry)((Iterator)???).next();
/*  945: 957 */           localObject5 = Account.getAccount((Long)((Map.Entry)localObject4).getKey());
/*  946: 958 */           if (((Account)localObject5).getBalance() < ((Long)((Map.Entry)localObject4).getValue()).longValue()) {
/*  947: 959 */             throw new BlockNotAcceptedException("Not enough funds in sender account: " + Convert.convert(((Account)localObject5).getId()), null);
/*  948:     */           }
/*  949:     */         }
/*  950:     */         Object localObject5;
/*  951: 963 */         for (??? = localHashMap2.entrySet().iterator(); ((Iterator)???).hasNext();)
/*  952:     */         {
/*  953: 963 */           localObject4 = (Map.Entry)((Iterator)???).next();
/*  954: 964 */           localObject5 = Account.getAccount((Long)((Map.Entry)localObject4).getKey());
/*  955: 965 */           for (localObject6 = ((Map)((Map.Entry)localObject4).getValue()).entrySet().iterator(); ((Iterator)localObject6).hasNext();)
/*  956:     */           {
/*  957: 965 */             localObject7 = (Map.Entry)((Iterator)localObject6).next();
/*  958: 966 */             localObject8 = (Long)((Map.Entry)localObject7).getKey();
/*  959: 967 */             localObject9 = (Long)((Map.Entry)localObject7).getValue();
/*  960: 968 */             if (((Account)localObject5).getAssetBalance((Long)localObject8).intValue() < ((Long)localObject9).longValue()) {
/*  961: 969 */               throw new BlockNotAcceptedException("Asset balance not sufficient in sender account " + Convert.convert(((Account)localObject5).getId()), null);
/*  962:     */             }
/*  963:     */           }
/*  964:     */         }
/*  965:     */         Object localObject9;
/*  966: 974 */         paramBlock.setHeight(localBlock.getHeight() + 1);
/*  967:     */         
/*  968: 976 */         ??? = null;
/*  969: 977 */         for (Object localObject4 = ((Map)localObject1).entrySet().iterator(); ((Iterator)localObject4).hasNext();)
/*  970:     */         {
/*  971: 977 */           localObject5 = (Map.Entry)((Iterator)localObject4).next();
/*  972: 978 */           localObject6 = (Transaction)((Map.Entry)localObject5).getValue();
/*  973: 979 */           ((Transaction)localObject6).setHeight(paramBlock.getHeight());
/*  974: 980 */           ((Transaction)localObject6).setBlockId(paramBlock.getId());
/*  975: 982 */           if ((transactionHashes.putIfAbsent(((Transaction)localObject6).getHash(), localObject6) != null) && (paramBlock.getHeight() != 58294))
/*  976:     */           {
/*  977: 983 */             ??? = localObject6;
/*  978: 984 */             break;
/*  979:     */           }
/*  980: 987 */           if (transactions.putIfAbsent(((Map.Entry)localObject5).getKey(), localObject6) != null)
/*  981:     */           {
/*  982: 988 */             Logger.logMessage("duplicate transaction id " + ((Map.Entry)localObject5).getKey());
/*  983: 989 */             ??? = localObject6;
/*  984: 990 */             break;
/*  985:     */           }
/*  986:     */         }
/*  987: 994 */         if (??? != null)
/*  988:     */         {
/*  989: 995 */           for (localObject4 = ((Map)localObject1).keySet().iterator(); ((Iterator)localObject4).hasNext();)
/*  990:     */           {
/*  991: 995 */             localObject5 = (Long)((Iterator)localObject4).next();
/*  992: 996 */             if (!((Long)localObject5).equals(((Transaction)???).getId()))
/*  993:     */             {
/*  994: 999 */               localObject6 = (Transaction)transactions.remove(localObject5);
/*  995:1000 */               if (localObject6 != null)
/*  996:     */               {
/*  997:1001 */                 localObject7 = (Transaction)transactionHashes.get(((Transaction)localObject6).getHash());
/*  998:1002 */                 if ((localObject7 != null) && (((Transaction)localObject7).getId().equals(localObject5))) {
/*  999:1003 */                   transactionHashes.remove(((Transaction)localObject6).getHash());
/* 1000:     */                 }
/* 1001:     */               }
/* 1002:     */             }
/* 1003:     */           }
/* 1004:1007 */           throw new BlockNotAcceptedException("Duplicate hash of transaction " + ((Transaction)???).getStringId(), null);
/* 1005:     */         }
/* 1006:1010 */         paramBlock.apply();
/* 1007:     */         
/* 1008:1012 */         localJSONArray1 = new JSONArray();
/* 1009:1013 */         localJSONArray2 = new JSONArray();
/* 1010:1015 */         for (localObject4 = ((Map)localObject1).entrySet().iterator(); ((Iterator)localObject4).hasNext();)
/* 1011:     */         {
/* 1012:1015 */           localObject5 = (Map.Entry)((Iterator)localObject4).next();
/* 1013:     */           
/* 1014:1017 */           localObject6 = (Transaction)((Map.Entry)localObject5).getValue();
/* 1015:     */           
/* 1016:1019 */           localObject7 = new JSONObject();
/* 1017:1020 */           ((JSONObject)localObject7).put("index", Integer.valueOf(((Transaction)localObject6).getIndex()));
/* 1018:1021 */           ((JSONObject)localObject7).put("blockTimestamp", Integer.valueOf(paramBlock.getTimestamp()));
/* 1019:1022 */           ((JSONObject)localObject7).put("transactionTimestamp", Integer.valueOf(((Transaction)localObject6).getTimestamp()));
/* 1020:1023 */           ((JSONObject)localObject7).put("sender", Convert.convert(((Transaction)localObject6).getSenderAccountId()));
/* 1021:1024 */           ((JSONObject)localObject7).put("recipient", Convert.convert(((Transaction)localObject6).getRecipientId()));
/* 1022:1025 */           ((JSONObject)localObject7).put("amount", Integer.valueOf(((Transaction)localObject6).getAmount()));
/* 1023:1026 */           ((JSONObject)localObject7).put("fee", Integer.valueOf(((Transaction)localObject6).getFee()));
/* 1024:1027 */           ((JSONObject)localObject7).put("id", ((Transaction)localObject6).getStringId());
/* 1025:1028 */           localJSONArray1.add(localObject7);
/* 1026:     */           
/* 1027:1030 */           localObject8 = (Transaction)unconfirmedTransactions.remove(((Map.Entry)localObject5).getKey());
/* 1028:1031 */           if (localObject8 != null)
/* 1029:     */           {
/* 1030:1032 */             localObject9 = new JSONObject();
/* 1031:1033 */             ((JSONObject)localObject9).put("index", Integer.valueOf(((Transaction)localObject8).getIndex()));
/* 1032:1034 */             localJSONArray2.add(localObject9);
/* 1033:     */             
/* 1034:1036 */             Account localAccount = Account.getAccount(((Transaction)localObject8).getSenderAccountId());
/* 1035:1037 */             localAccount.addToUnconfirmedBalance((((Transaction)localObject8).getAmount() + ((Transaction)localObject8).getFee()) * 100L);
/* 1036:     */           }
/* 1037:     */         }
/* 1038:1044 */         if (paramBoolean)
/* 1039:     */         {
/* 1040:1045 */           saveTransactions("transactions.nxt");
/* 1041:1046 */           saveBlocks("blocks.nxt");
/* 1042:     */         }
/* 1043:     */       }
/* 1044:     */       catch (RuntimeException localRuntimeException)
/* 1045:     */       {
/* 1046:1050 */         Logger.logMessage("Error pushing block", localRuntimeException);
/* 1047:1051 */         throw new BlockNotAcceptedException(localRuntimeException.toString(), null);
/* 1048:     */       }
/* 1049:     */     }
/* 1050:1055 */     if (paramBlock.getTimestamp() >= i - 15)
/* 1051:     */     {
/* 1052:1057 */       ??? = paramBlock.getJSONObject();
/* 1053:1058 */       ((JSONObject)???).put("requestType", "processBlock");
/* 1054:     */       
/* 1055:1060 */       Peer.sendToSomePeers((JSONObject)???);
/* 1056:     */     }
/* 1057:1064 */     ??? = new JSONArray();
/* 1058:1065 */     JSONObject localJSONObject = new JSONObject();
/* 1059:1066 */     localJSONObject.put("index", Integer.valueOf(paramBlock.getIndex()));
/* 1060:1067 */     localJSONObject.put("timestamp", Integer.valueOf(paramBlock.getTimestamp()));
/* 1061:1068 */     localJSONObject.put("numberOfTransactions", Integer.valueOf(paramBlock.transactionIds.length));
/* 1062:1069 */     localJSONObject.put("totalAmount", Integer.valueOf(paramBlock.getTotalAmount()));
/* 1063:1070 */     localJSONObject.put("totalFee", Integer.valueOf(paramBlock.getTotalFee()));
/* 1064:1071 */     localJSONObject.put("payloadLength", Integer.valueOf(paramBlock.getPayloadLength()));
/* 1065:1072 */     localJSONObject.put("generator", Convert.convert(paramBlock.getGeneratorAccountId()));
/* 1066:1073 */     localJSONObject.put("height", Integer.valueOf(paramBlock.getHeight()));
/* 1067:1074 */     localJSONObject.put("version", Integer.valueOf(paramBlock.getVersion()));
/* 1068:1075 */     localJSONObject.put("block", paramBlock.getStringId());
/* 1069:1076 */     localJSONObject.put("baseTarget", BigInteger.valueOf(paramBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
/* 1070:1077 */     ((JSONArray)???).add(localJSONObject);
/* 1071:     */     
/* 1072:1079 */     Object localObject1 = new JSONObject();
/* 1073:1080 */     ((JSONObject)localObject1).put("response", "processNewData");
/* 1074:1081 */     ((JSONObject)localObject1).put("addedConfirmedTransactions", localJSONArray1);
/* 1075:1082 */     if (localJSONArray2.size() > 0) {
/* 1076:1083 */       ((JSONObject)localObject1).put("removedUnconfirmedTransactions", localJSONArray2);
/* 1077:     */     }
/* 1078:1085 */     ((JSONObject)localObject1).put("addedRecentBlocks", ???);
/* 1079:     */     
/* 1080:1087 */     User.sendToAll((JSONStreamAware)localObject1);
/* 1081:     */   }
/* 1082:     */   
/* 1083:     */   private static boolean popLastBlock()
/* 1084:     */     throws Transaction.UndoNotSupportedException
/* 1085:     */   {
/* 1086:     */     try
/* 1087:     */     {
/* 1088:1095 */       JSONObject localJSONObject1 = new JSONObject();
/* 1089:1096 */       localJSONObject1.put("response", "processNewData");
/* 1090:     */       
/* 1091:1098 */       JSONArray localJSONArray = new JSONArray();
/* 1092:     */       Block localBlock;
/* 1093:1102 */       synchronized (Blockchain.class)
/* 1094:     */       {
/* 1095:1104 */         localBlock = (Block)lastBlock.get();
/* 1096:     */         
/* 1097:1106 */         Logger.logDebugMessage("Will pop block " + localBlock.getStringId() + " at height " + localBlock.getHeight());
/* 1098:1107 */         if (localBlock.getId().equals(Genesis.GENESIS_BLOCK_ID)) {
/* 1099:1108 */           return false;
/* 1100:     */         }
/* 1101:1111 */         localObject1 = (Block)blocks.get(localBlock.getPreviousBlockId());
/* 1102:1112 */         if (localObject1 == null)
/* 1103:     */         {
/* 1104:1113 */           Logger.logMessage("Previous block is null");
/* 1105:1114 */           throw new IllegalStateException();
/* 1106:     */         }
/* 1107:1116 */         if (!lastBlock.compareAndSet(localBlock, localObject1))
/* 1108:     */         {
/* 1109:1117 */           Logger.logMessage("This block is no longer last block");
/* 1110:1118 */           throw new IllegalStateException();
/* 1111:     */         }
/* 1112:1121 */         Account localAccount = Account.getAccount(localBlock.getGeneratorAccountId());
/* 1113:1122 */         localAccount.addToBalanceAndUnconfirmedBalance(-localBlock.getTotalFee() * 100L);
/* 1114:1124 */         for (Long localLong : localBlock.transactionIds)
/* 1115:     */         {
/* 1116:1126 */           Transaction localTransaction1 = (Transaction)transactions.remove(localLong);
/* 1117:1127 */           Transaction localTransaction2 = (Transaction)transactionHashes.get(localTransaction1.getHash());
/* 1118:1128 */           if ((localTransaction2 != null) && (localTransaction2.getId().equals(localLong))) {
/* 1119:1129 */             transactionHashes.remove(localTransaction1.getHash());
/* 1120:     */           }
/* 1121:1131 */           unconfirmedTransactions.put(localLong, localTransaction1);
/* 1122:     */           
/* 1123:1133 */           localTransaction1.undo();
/* 1124:     */           
/* 1125:1135 */           JSONObject localJSONObject2 = new JSONObject();
/* 1126:1136 */           localJSONObject2.put("index", Integer.valueOf(localTransaction1.getIndex()));
/* 1127:1137 */           localJSONObject2.put("timestamp", Integer.valueOf(localTransaction1.getTimestamp()));
/* 1128:1138 */           localJSONObject2.put("deadline", Short.valueOf(localTransaction1.getDeadline()));
/* 1129:1139 */           localJSONObject2.put("recipient", Convert.convert(localTransaction1.getRecipientId()));
/* 1130:1140 */           localJSONObject2.put("amount", Integer.valueOf(localTransaction1.getAmount()));
/* 1131:1141 */           localJSONObject2.put("fee", Integer.valueOf(localTransaction1.getFee()));
/* 1132:1142 */           localJSONObject2.put("sender", Convert.convert(localTransaction1.getSenderAccountId()));
/* 1133:1143 */           localJSONObject2.put("id", localTransaction1.getStringId());
/* 1134:1144 */           localJSONArray.add(localJSONObject2);
/* 1135:     */         }
/* 1136:     */       }
/* 1137:1150 */       ??? = new JSONArray();
/* 1138:1151 */       Object localObject1 = new JSONObject();
/* 1139:1152 */       ((JSONObject)localObject1).put("index", Integer.valueOf(localBlock.getIndex()));
/* 1140:1153 */       ((JSONObject)localObject1).put("timestamp", Integer.valueOf(localBlock.getTimestamp()));
/* 1141:1154 */       ((JSONObject)localObject1).put("numberOfTransactions", Integer.valueOf(localBlock.transactionIds.length));
/* 1142:1155 */       ((JSONObject)localObject1).put("totalAmount", Integer.valueOf(localBlock.getTotalAmount()));
/* 1143:1156 */       ((JSONObject)localObject1).put("totalFee", Integer.valueOf(localBlock.getTotalFee()));
/* 1144:1157 */       ((JSONObject)localObject1).put("payloadLength", Integer.valueOf(localBlock.getPayloadLength()));
/* 1145:1158 */       ((JSONObject)localObject1).put("generator", Convert.convert(localBlock.getGeneratorAccountId()));
/* 1146:1159 */       ((JSONObject)localObject1).put("height", Integer.valueOf(localBlock.getHeight()));
/* 1147:1160 */       ((JSONObject)localObject1).put("version", Integer.valueOf(localBlock.getVersion()));
/* 1148:1161 */       ((JSONObject)localObject1).put("block", localBlock.getStringId());
/* 1149:1162 */       ((JSONObject)localObject1).put("baseTarget", BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
/* 1150:1163 */       ((JSONArray)???).add(localObject1);
/* 1151:1164 */       localJSONObject1.put("addedOrphanedBlocks", ???);
/* 1152:1166 */       if (localJSONArray.size() > 0) {
/* 1153:1167 */         localJSONObject1.put("addedUnconfirmedTransactions", localJSONArray);
/* 1154:     */       }
/* 1155:1170 */       User.sendToAll(localJSONObject1);
/* 1156:     */     }
/* 1157:     */     catch (RuntimeException localRuntimeException)
/* 1158:     */     {
/* 1159:1173 */       Logger.logMessage("Error popping last block", localRuntimeException);
/* 1160:1174 */       return false;
/* 1161:     */     }
/* 1162:1176 */     return true;
/* 1163:     */   }
/* 1164:     */   
/* 1165:     */   private static synchronized void scan()
/* 1166:     */   {
/* 1167:1180 */     HashMap localHashMap = new HashMap(blocks);
/* 1168:1181 */     blocks.clear();
/* 1169:1182 */     Long localLong = Genesis.GENESIS_BLOCK_ID;
/* 1170:     */     Block localBlock;
/* 1171:1184 */     while ((localBlock = (Block)localHashMap.get(localLong)) != null)
/* 1172:     */     {
/* 1173:1185 */       localBlock.apply();
/* 1174:1186 */       localLong = localBlock.getNextBlockId();
/* 1175:     */     }
/* 1176:     */   }
/* 1177:     */   
/* 1178:     */   private static void generateBlock(String paramString)
/* 1179:     */   {
/* 1180:1192 */     TreeSet localTreeSet = new TreeSet();
/* 1181:1194 */     for (Object localObject1 = unconfirmedTransactions.values().iterator(); ((Iterator)localObject1).hasNext();)
/* 1182:     */     {
/* 1183:1194 */       localObject2 = (Transaction)((Iterator)localObject1).next();
/* 1184:1195 */       if ((((Transaction)localObject2).getReferencedTransactionId() == null) || (transactions.get(((Transaction)localObject2).getReferencedTransactionId()) != null)) {
/* 1185:1196 */         localTreeSet.add(localObject2);
/* 1186:     */       }
/* 1187:     */     }
/* 1188:1200 */     localObject1 = new HashMap();
/* 1189:1201 */     Object localObject2 = new HashMap();
/* 1190:1202 */     HashMap localHashMap = new HashMap();
/* 1191:     */     
/* 1192:1204 */     int i = 0;
/* 1193:1205 */     int j = 0;
/* 1194:1206 */     int k = 0;
/* 1195:     */     
/* 1196:1208 */     int m = Convert.getEpochTime();
/* 1197:     */     Object localObject3;
/* 1198:1210 */     while (k <= 32640)
/* 1199:     */     {
/* 1200:1212 */       int n = ((Map)localObject1).size();
/* 1201:1214 */       for (localObject3 = localTreeSet.iterator(); ((Iterator)localObject3).hasNext();)
/* 1202:     */       {
/* 1203:1214 */         localObject4 = (Transaction)((Iterator)localObject3).next();
/* 1204:     */         
/* 1205:1216 */         int i1 = ((Transaction)localObject4).getSize();
/* 1206:1217 */         if ((((Map)localObject1).get(((Transaction)localObject4).getId()) == null) && (k + i1 <= 32640))
/* 1207:     */         {
/* 1208:1219 */           localObject5 = ((Transaction)localObject4).getSenderAccountId();
/* 1209:1220 */           localObject6 = (Long)localHashMap.get(localObject5);
/* 1210:1221 */           if (localObject6 == null) {
/* 1211:1222 */             localObject6 = Long.valueOf(0L);
/* 1212:     */           }
/* 1213:1225 */           long l = (((Transaction)localObject4).getAmount() + ((Transaction)localObject4).getFee()) * 100L;
/* 1214:1226 */           if (((Long)localObject6).longValue() + l <= Account.getAccount((Long)localObject5).getBalance()) {
/* 1215:1228 */             if ((((Transaction)localObject4).getTimestamp() <= m + 15) && (((Transaction)localObject4).getExpiration() >= m) && 
/* 1216:     */             
/* 1217:     */ 
/* 1218:     */ 
/* 1219:1232 */               (!((Transaction)localObject4).isDuplicate((Map)localObject2)))
/* 1220:     */             {
/* 1221:1236 */               localHashMap.put(localObject5, Long.valueOf(((Long)localObject6).longValue() + l));
/* 1222:     */               
/* 1223:1238 */               ((Map)localObject1).put(((Transaction)localObject4).getId(), localObject4);
/* 1224:1239 */               k += i1;
/* 1225:1240 */               i += ((Transaction)localObject4).getAmount();
/* 1226:1241 */               j += ((Transaction)localObject4).getFee();
/* 1227:     */             }
/* 1228:     */           }
/* 1229:     */         }
/* 1230:     */       }
/* 1231:1249 */       if (((Map)localObject1).size() == n) {
/* 1232:     */         break;
/* 1233:     */       }
/* 1234:     */     }
/* 1235:1257 */     byte[] arrayOfByte1 = Crypto.getPublicKey(paramString);
/* 1236:     */     
/* 1237:     */ 
/* 1238:1260 */     Object localObject4 = (Block)lastBlock.get();
/* 1239:     */     try
/* 1240:     */     {
/* 1241:1263 */       if (((Block)localObject4).getHeight() < 30000)
/* 1242:     */       {
/* 1243:1265 */         localObject3 = new Block(1, m, ((Block)localObject4).getId(), ((Map)localObject1).size(), i, j, k, null, arrayOfByte1, null, new byte[64]);
/* 1244:     */       }
/* 1245:     */       else
/* 1246:     */       {
/* 1247:1270 */         byte[] arrayOfByte2 = Crypto.sha256().digest(((Block)localObject4).getBytes());
/* 1248:1271 */         localObject3 = new Block(2, m, ((Block)localObject4).getId(), ((Map)localObject1).size(), i, j, k, null, arrayOfByte1, null, new byte[64], arrayOfByte2);
/* 1249:     */       }
/* 1250:     */     }
/* 1251:     */     catch (NxtException.ValidationException localValidationException)
/* 1252:     */     {
/* 1253:1277 */       Logger.logMessage("Error generating block", localValidationException);
/* 1254:1278 */       return;
/* 1255:     */     }
/* 1256:1281 */     int i2 = 0;
/* 1257:1282 */     for (Object localObject5 = ((Map)localObject1).keySet().iterator(); ((Iterator)localObject5).hasNext();)
/* 1258:     */     {
/* 1259:1282 */       localObject6 = (Long)((Iterator)localObject5).next();
/* 1260:1283 */       ((Block)localObject3).transactionIds[(i2++)] = localObject6;
/* 1261:     */     }
/* 1262:1286 */     Arrays.sort(((Block)localObject3).transactionIds);
/* 1263:1287 */     localObject5 = Crypto.sha256();
/* 1264:1288 */     for (i2 = 0; i2 < ((Block)localObject3).transactionIds.length; i2++)
/* 1265:     */     {
/* 1266:1289 */       localObject6 = (Transaction)((Map)localObject1).get(localObject3.transactionIds[i2]);
/* 1267:1290 */       ((MessageDigest)localObject5).update(((Transaction)localObject6).getBytes());
/* 1268:1291 */       ((Block)localObject3).blockTransactions[i2] = localObject6;
/* 1269:     */     }
/* 1270:1293 */     ((Block)localObject3).setPayloadHash(((MessageDigest)localObject5).digest());
/* 1271:1295 */     if (((Block)localObject4).getHeight() < 30000)
/* 1272:     */     {
/* 1273:1297 */       ((Block)localObject3).setGenerationSignature(Crypto.sign(((Block)localObject4).getGenerationSignature(), paramString));
/* 1274:     */     }
/* 1275:     */     else
/* 1276:     */     {
/* 1277:1301 */       ((MessageDigest)localObject5).update(((Block)localObject4).getGenerationSignature());
/* 1278:1302 */       ((Block)localObject3).setGenerationSignature(((MessageDigest)localObject5).digest(arrayOfByte1));
/* 1279:     */     }
/* 1280:1306 */     Object localObject6 = ((Block)localObject3).getBytes();
/* 1281:1307 */     byte[] arrayOfByte3 = new byte[localObject6.length - 64];
/* 1282:1308 */     System.arraycopy(localObject6, 0, arrayOfByte3, 0, arrayOfByte3.length);
/* 1283:1309 */     ((Block)localObject3).setBlockSignature(Crypto.sign(arrayOfByte3, paramString));
/* 1284:1311 */     if ((((Block)localObject3).verifyBlockSignature()) && (((Block)localObject3).verifyGenerationSignature()))
/* 1285:     */     {
/* 1286:1313 */       JSONObject localJSONObject = ((Block)localObject3).getJSONObject();
/* 1287:1314 */       localJSONObject.put("requestType", "processBlock");
/* 1288:1315 */       Peer.sendToSomePeers(localJSONObject);
/* 1289:     */     }
/* 1290:     */     else
/* 1291:     */     {
/* 1292:1319 */       Logger.logMessage("Generated an incorrect block. Waiting for the next one...");
/* 1293:     */     }
/* 1294:     */   }
/* 1295:     */   
/* 1296:     */   static void purgeExpiredHashes(int paramInt)
/* 1297:     */   {
/* 1298:1326 */     Iterator localIterator = transactionHashes.entrySet().iterator();
/* 1299:1327 */     while (localIterator.hasNext()) {
/* 1300:1328 */       if (((Transaction)((Map.Entry)localIterator.next()).getValue()).getExpiration() < paramInt) {
/* 1301:1329 */         localIterator.remove();
/* 1302:     */       }
/* 1303:     */     }
/* 1304:     */   }
/* 1305:     */   
/* 1306:     */   private static void loadTransactions(String paramString)
/* 1307:     */     throws FileNotFoundException
/* 1308:     */   {
/* 1309:     */     try
/* 1310:     */     {
/* 1311:1336 */       FileInputStream localFileInputStream = new FileInputStream(paramString);Object localObject1 = null;
/* 1312:     */       try
/* 1313:     */       {
/* 1314:1337 */         ObjectInputStream localObjectInputStream = new ObjectInputStream(localFileInputStream);Object localObject2 = null;
/* 1315:     */         try
/* 1316:     */         {
/* 1317:1338 */           transactionCounter.set(localObjectInputStream.readInt());
/* 1318:1339 */           transactions.clear();
/* 1319:1340 */           transactions.putAll((HashMap)localObjectInputStream.readObject());
/* 1320:     */         }
/* 1321:     */         catch (Throwable localThrowable4)
/* 1322:     */         {
/* 1323:1336 */           localObject2 = localThrowable4;throw localThrowable4;
/* 1324:     */         }
/* 1325:     */         finally {}
/* 1326:     */       }
/* 1327:     */       catch (Throwable localThrowable2)
/* 1328:     */       {
/* 1329:1336 */         localObject1 = localThrowable2;throw localThrowable2;
/* 1330:     */       }
/* 1331:     */       finally
/* 1332:     */       {
/* 1333:1341 */         if (localFileInputStream != null) {
/* 1334:1341 */           if (localObject1 != null) {
/* 1335:     */             try
/* 1336:     */             {
/* 1337:1341 */               localFileInputStream.close();
/* 1338:     */             }
/* 1339:     */             catch (Throwable localThrowable6)
/* 1340:     */             {
/* 1341:1341 */               localObject1.addSuppressed(localThrowable6);
/* 1342:     */             }
/* 1343:     */           } else {
/* 1344:1341 */             localFileInputStream.close();
/* 1345:     */           }
/* 1346:     */         }
/* 1347:     */       }
/* 1348:     */     }
/* 1349:     */     catch (FileNotFoundException localFileNotFoundException)
/* 1350:     */     {
/* 1351:1342 */       throw localFileNotFoundException;
/* 1352:     */     }
/* 1353:     */     catch (IOException|ClassNotFoundException localIOException)
/* 1354:     */     {
/* 1355:1344 */       Logger.logMessage("Error loading transactions from " + paramString, localIOException);
/* 1356:1345 */       System.exit(1);
/* 1357:     */     }
/* 1358:     */   }
/* 1359:     */   
/* 1360:     */   private static void saveTransactions(String paramString)
/* 1361:     */   {
/* 1362:     */     try
/* 1363:     */     {
/* 1364:1352 */       FileOutputStream localFileOutputStream = new FileOutputStream(paramString);Object localObject1 = null;
/* 1365:     */       try
/* 1366:     */       {
/* 1367:1353 */         ObjectOutputStream localObjectOutputStream = new ObjectOutputStream(localFileOutputStream);Object localObject2 = null;
/* 1368:     */         try
/* 1369:     */         {
/* 1370:1355 */           localObjectOutputStream.writeInt(transactionCounter.get());
/* 1371:1356 */           localObjectOutputStream.writeObject(new HashMap(transactions));
/* 1372:1357 */           localObjectOutputStream.close();
/* 1373:     */         }
/* 1374:     */         catch (Throwable localThrowable4)
/* 1375:     */         {
/* 1376:1352 */           localObject2 = localThrowable4;throw localThrowable4;
/* 1377:     */         }
/* 1378:     */         finally {}
/* 1379:     */       }
/* 1380:     */       catch (Throwable localThrowable2)
/* 1381:     */       {
/* 1382:1352 */         localObject1 = localThrowable2;throw localThrowable2;
/* 1383:     */       }
/* 1384:     */       finally
/* 1385:     */       {
/* 1386:1358 */         if (localFileOutputStream != null) {
/* 1387:1358 */           if (localObject1 != null) {
/* 1388:     */             try
/* 1389:     */             {
/* 1390:1358 */               localFileOutputStream.close();
/* 1391:     */             }
/* 1392:     */             catch (Throwable localThrowable6)
/* 1393:     */             {
/* 1394:1358 */               localObject1.addSuppressed(localThrowable6);
/* 1395:     */             }
/* 1396:     */           } else {
/* 1397:1358 */             localFileOutputStream.close();
/* 1398:     */           }
/* 1399:     */         }
/* 1400:     */       }
/* 1401:     */     }
/* 1402:     */     catch (IOException localIOException)
/* 1403:     */     {
/* 1404:1359 */       Logger.logMessage("Error saving transactions to " + paramString, localIOException);
/* 1405:1360 */       throw new RuntimeException(localIOException);
/* 1406:     */     }
/* 1407:     */   }
/* 1408:     */   
/* 1409:     */   private static void loadBlocks(String paramString)
/* 1410:     */     throws FileNotFoundException
/* 1411:     */   {
/* 1412:     */     try
/* 1413:     */     {
/* 1414:1367 */       FileInputStream localFileInputStream = new FileInputStream(paramString);Object localObject1 = null;
/* 1415:     */       try
/* 1416:     */       {
/* 1417:1368 */         ObjectInputStream localObjectInputStream = new ObjectInputStream(localFileInputStream);Object localObject2 = null;
/* 1418:     */         try
/* 1419:     */         {
/* 1420:1370 */           blockCounter.set(localObjectInputStream.readInt());
/* 1421:1371 */           blocks.clear();
/* 1422:1372 */           blocks.putAll((HashMap)localObjectInputStream.readObject());
/* 1423:     */         }
/* 1424:     */         catch (Throwable localThrowable4)
/* 1425:     */         {
/* 1426:1367 */           localObject2 = localThrowable4;throw localThrowable4;
/* 1427:     */         }
/* 1428:     */         finally {}
/* 1429:     */       }
/* 1430:     */       catch (Throwable localThrowable2)
/* 1431:     */       {
/* 1432:1367 */         localObject1 = localThrowable2;throw localThrowable2;
/* 1433:     */       }
/* 1434:     */       finally
/* 1435:     */       {
/* 1436:1373 */         if (localFileInputStream != null) {
/* 1437:1373 */           if (localObject1 != null) {
/* 1438:     */             try
/* 1439:     */             {
/* 1440:1373 */               localFileInputStream.close();
/* 1441:     */             }
/* 1442:     */             catch (Throwable localThrowable6)
/* 1443:     */             {
/* 1444:1373 */               localObject1.addSuppressed(localThrowable6);
/* 1445:     */             }
/* 1446:     */           } else {
/* 1447:1373 */             localFileInputStream.close();
/* 1448:     */           }
/* 1449:     */         }
/* 1450:     */       }
/* 1451:     */     }
/* 1452:     */     catch (FileNotFoundException localFileNotFoundException)
/* 1453:     */     {
/* 1454:1374 */       throw localFileNotFoundException;
/* 1455:     */     }
/* 1456:     */     catch (IOException|ClassNotFoundException localIOException)
/* 1457:     */     {
/* 1458:1376 */       Logger.logMessage("Error loading blocks from " + paramString, localIOException);
/* 1459:1377 */       System.exit(1);
/* 1460:     */     }
/* 1461:     */   }
/* 1462:     */   
/* 1463:     */   private static void saveBlocks(String paramString)
/* 1464:     */   {
/* 1465:     */     try
/* 1466:     */     {
/* 1467:1384 */       FileOutputStream localFileOutputStream = new FileOutputStream(paramString);Object localObject1 = null;
/* 1468:     */       try
/* 1469:     */       {
/* 1470:1385 */         ObjectOutputStream localObjectOutputStream = new ObjectOutputStream(localFileOutputStream);Object localObject2 = null;
/* 1471:     */         try
/* 1472:     */         {
/* 1473:1387 */           localObjectOutputStream.writeInt(blockCounter.get());
/* 1474:1388 */           localObjectOutputStream.writeObject(new HashMap(blocks));
/* 1475:     */         }
/* 1476:     */         catch (Throwable localThrowable4)
/* 1477:     */         {
/* 1478:1384 */           localObject2 = localThrowable4;throw localThrowable4;
/* 1479:     */         }
/* 1480:     */         finally {}
/* 1481:     */       }
/* 1482:     */       catch (Throwable localThrowable2)
/* 1483:     */       {
/* 1484:1384 */         localObject1 = localThrowable2;throw localThrowable2;
/* 1485:     */       }
/* 1486:     */       finally
/* 1487:     */       {
/* 1488:1389 */         if (localFileOutputStream != null) {
/* 1489:1389 */           if (localObject1 != null) {
/* 1490:     */             try
/* 1491:     */             {
/* 1492:1389 */               localFileOutputStream.close();
/* 1493:     */             }
/* 1494:     */             catch (Throwable localThrowable6)
/* 1495:     */             {
/* 1496:1389 */               localObject1.addSuppressed(localThrowable6);
/* 1497:     */             }
/* 1498:     */           } else {
/* 1499:1389 */             localFileOutputStream.close();
/* 1500:     */           }
/* 1501:     */         }
/* 1502:     */       }
/* 1503:     */     }
/* 1504:     */     catch (IOException localIOException)
/* 1505:     */     {
/* 1506:1390 */       Logger.logMessage("Error saving blocks to " + paramString, localIOException);
/* 1507:1391 */       throw new RuntimeException(localIOException);
/* 1508:     */     }
/* 1509:     */   }
/* 1510:     */   
/* 1511:     */   public static class BlockNotAcceptedException
/* 1512:     */     extends NxtException
/* 1513:     */   {
/* 1514:     */     private BlockNotAcceptedException(String paramString)
/* 1515:     */     {
/* 1516:1398 */       super();
/* 1517:     */     }
/* 1518:     */   }
/* 1519:     */   
/* 1520:     */   public static class BlockOutOfOrderException
/* 1521:     */     extends Blockchain.BlockNotAcceptedException
/* 1522:     */   {
/* 1523:     */     private BlockOutOfOrderException(String paramString)
/* 1524:     */     {
/* 1525:1406 */       super(null);
/* 1526:     */     }
/* 1527:     */   }
/* 1528:     */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Blockchain
 * JD-Core Version:    0.7.0.1
 */