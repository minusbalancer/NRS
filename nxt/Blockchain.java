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
/*  265: 287 */                               if (!Blockchain.pushBlock(localBlock, arrayOfTransaction, false))
/*  266:     */                               {
/*  267: 288 */                                 Logger.logDebugMessage("Failed to accept block received from " + localPeer.getPeerAddress() + ", blacklisting");
/*  268: 289 */                                 localPeer.blacklist();
/*  269: 290 */                                 return;
/*  270:     */                               }
/*  271:     */                             }
/*  272:     */                             catch (NxtException.ValidationException localValidationException2)
/*  273:     */                             {
/*  274: 293 */                               localPeer.blacklist(localValidationException2);
/*  275: 294 */                               return;
/*  276:     */                             }
/*  277:     */                           }
/*  278: 297 */                           else if ((Blockchain.blocks.get(localBlock.getId()) == null) && (localBlock.transactionIds.length <= 255))
/*  279:     */                           {
/*  280: 299 */                             localLinkedList.add(localBlock);
/*  281:     */                             
/*  282: 301 */                             localJSONArray2 = (JSONArray)localJSONObject2.get("transactions");
/*  283:     */                             try
/*  284:     */                             {
/*  285: 303 */                               for (int m = 0; m < localBlock.transactionIds.length; m++)
/*  286:     */                               {
/*  287: 305 */                                 Transaction localTransaction = Transaction.getTransaction((JSONObject)localJSONArray2.get(m));
/*  288: 306 */                                 localBlock.transactionIds[m] = localTransaction.getId();
/*  289: 307 */                                 localBlock.blockTransactions[m] = localTransaction;
/*  290: 308 */                                 ((HashMap)localObject4).put(localBlock.transactionIds[m], localTransaction);
/*  291:     */                               }
/*  292:     */                             }
/*  293:     */                             catch (NxtException.ValidationException localValidationException3)
/*  294:     */                             {
/*  295: 312 */                               localPeer.blacklist(localValidationException3);
/*  296: 313 */                               return;
/*  297:     */                             }
/*  298:     */                           }
/*  299:     */                         }
/*  300:     */                       }
/*  301:     */                     }
/*  302: 323 */                     if ((!localLinkedList.isEmpty()) && (((Block)Blockchain.lastBlock.get()).getHeight() - ((Block)Blockchain.blocks.get(localObject1)).getHeight() < 720)) {
/*  303: 325 */                       synchronized (Blockchain.class)
/*  304:     */                       {
/*  305: 327 */                         Blockchain.saveTransactions("transactions.nxt.bak");
/*  306: 328 */                         Blockchain.saveBlocks("blocks.nxt.bak");
/*  307:     */                         
/*  308: 330 */                         localBigInteger1 = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty();
/*  309:     */                         for (;;)
/*  310:     */                         {
/*  311:     */                           int k;
/*  312:     */                           try
/*  313:     */                           {
/*  314: 334 */                             while ((!((Block)Blockchain.lastBlock.get()).getId().equals(localObject1)) && (Blockchain.access$800())) {}
/*  315: 336 */                             if (((Block)Blockchain.lastBlock.get()).getId().equals(localObject1)) {
/*  316: 337 */                               for (??? = localLinkedList.iterator(); ((Iterator)???).hasNext();)
/*  317:     */                               {
/*  318: 337 */                                 localObject7 = (Block)((Iterator)???).next();
/*  319: 338 */                                 if ((((Block)Blockchain.lastBlock.get()).getId().equals(((Block)localObject7).getPreviousBlockId())) && 
/*  320: 339 */                                   (!Blockchain.pushBlock((Block)localObject7, ((Block)localObject7).blockTransactions, false))) {
/*  321:     */                                   break;
/*  322:     */                                 }
/*  323:     */                               }
/*  324:     */                             }
/*  325: 346 */                             k = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty().compareTo(localBigInteger1) < 0 ? 1 : 0;
/*  326: 347 */                             if (k != 0)
/*  327:     */                             {
/*  328: 348 */                               Logger.logDebugMessage("Rescan caused by peer " + localPeer.getPeerAddress() + ", blacklisting");
/*  329: 349 */                               localPeer.blacklist();
/*  330:     */                             }
/*  331:     */                           }
/*  332:     */                           catch (Transaction.UndoNotSupportedException localUndoNotSupportedException)
/*  333:     */                           {
/*  334: 352 */                             Logger.logDebugMessage(localUndoNotSupportedException.getMessage());
/*  335: 353 */                             Logger.logDebugMessage("Popping off last block not possible, will do a rescan");
/*  336: 354 */                             k = 1;
/*  337:     */                           }
/*  338:     */                         }
/*  339: 357 */                         if (k != 0)
/*  340:     */                         {
/*  341: 358 */                           Blockchain.loadTransactions("transactions.nxt.bak");
/*  342: 359 */                           Blockchain.loadBlocks("blocks.nxt.bak");
/*  343: 360 */                           Account.clear();
/*  344: 361 */                           Alias.clear();
/*  345: 362 */                           Asset.clear();
/*  346: 363 */                           Order.clear();
/*  347: 364 */                           Blockchain.unconfirmedTransactions.clear();
/*  348: 365 */                           Blockchain.doubleSpendingTransactions.clear();
/*  349: 366 */                           Blockchain.nonBroadcastedTransactions.clear();
/*  350: 367 */                           Blockchain.transactionHashes.clear();
/*  351: 368 */                           Logger.logMessage("Re-scanning blockchain...");
/*  352: 369 */                           Blockchain.access$1300();
/*  353: 370 */                           Logger.logMessage("...Done");
/*  354:     */                         }
/*  355:     */                       }
/*  356:     */                     }
/*  357: 375 */                     synchronized (Blockchain.class)
/*  358:     */                     {
/*  359: 376 */                       Blockchain.saveTransactions("transactions.nxt");
/*  360: 377 */                       Blockchain.saveBlocks("blocks.nxt");
/*  361:     */                     }
/*  362:     */                   }
/*  363:     */                 }
/*  364:     */               }
/*  365:     */             }
/*  366:     */           }
/*  367:     */         }
/*  368:     */         catch (Exception localException)
/*  369:     */         {
/*  370: 386 */           Logger.logDebugMessage("Error in milestone blocks processing thread", localException);
/*  371:     */         }
/*  372:     */       }
/*  373:     */       catch (Throwable localThrowable)
/*  374:     */       {
/*  375: 389 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  376: 390 */         localThrowable.printStackTrace();
/*  377: 391 */         System.exit(1);
/*  378:     */       }
/*  379:     */     }
/*  380:     */   };
/*  381: 398 */   static final Runnable generateBlockThread = new Runnable()
/*  382:     */   {
/*  383: 400 */     private final ConcurrentMap<Account, Block> lastBlocks = new ConcurrentHashMap();
/*  384: 401 */     private final ConcurrentMap<Account, BigInteger> hits = new ConcurrentHashMap();
/*  385:     */     
/*  386:     */     public void run()
/*  387:     */     {
/*  388:     */       try
/*  389:     */       {
/*  390:     */         try
/*  391:     */         {
/*  392: 409 */           HashMap localHashMap = new HashMap();
/*  393: 410 */           for (localIterator = User.getAllUsers().iterator(); localIterator.hasNext();)
/*  394:     */           {
/*  395: 410 */             localObject1 = (User)localIterator.next();
/*  396: 411 */             if (((User)localObject1).getSecretPhrase() != null)
/*  397:     */             {
/*  398: 412 */               localAccount = Account.getAccount(((User)localObject1).getPublicKey());
/*  399: 413 */               if ((localAccount != null) && (localAccount.getEffectiveBalance() > 0)) {
/*  400: 414 */                 localHashMap.put(localAccount, localObject1);
/*  401:     */               }
/*  402:     */             }
/*  403:     */           }
/*  404: 419 */           for (localIterator = localHashMap.entrySet().iterator(); localIterator.hasNext();)
/*  405:     */           {
/*  406: 419 */             localObject1 = (Map.Entry)localIterator.next();
/*  407:     */             
/*  408: 421 */             localAccount = (Account)((Map.Entry)localObject1).getKey();
/*  409: 422 */             User localUser = (User)((Map.Entry)localObject1).getValue();
/*  410: 423 */             Block localBlock = (Block)Blockchain.lastBlock.get();
/*  411: 424 */             if (this.lastBlocks.get(localAccount) != localBlock)
/*  412:     */             {
/*  413: 426 */               long l = localAccount.getEffectiveBalance();
/*  414: 427 */               if (l > 0L)
/*  415:     */               {
/*  416: 430 */                 MessageDigest localMessageDigest = Crypto.sha256();
/*  417:     */                 byte[] arrayOfByte;
/*  418: 432 */                 if (localBlock.getHeight() < 30000)
/*  419:     */                 {
/*  420: 434 */                   localObject2 = Crypto.sign(localBlock.getGenerationSignature(), localUser.getSecretPhrase());
/*  421: 435 */                   arrayOfByte = localMessageDigest.digest((byte[])localObject2);
/*  422:     */                 }
/*  423:     */                 else
/*  424:     */                 {
/*  425: 439 */                   localMessageDigest.update(localBlock.getGenerationSignature());
/*  426: 440 */                   arrayOfByte = localMessageDigest.digest(localUser.getPublicKey());
/*  427:     */                 }
/*  428: 443 */                 Object localObject2 = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
/*  429:     */                 
/*  430: 445 */                 this.lastBlocks.put(localAccount, localBlock);
/*  431: 446 */                 this.hits.put(localAccount, localObject2);
/*  432:     */                 
/*  433: 448 */                 JSONObject localJSONObject = new JSONObject();
/*  434: 449 */                 localJSONObject.put("response", "setBlockGenerationDeadline");
/*  435: 450 */                 localJSONObject.put("deadline", Long.valueOf(((BigInteger)localObject2).divide(BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(l))).longValue() - (Convert.getEpochTime() - localBlock.getTimestamp())));
/*  436:     */                 
/*  437: 452 */                 localUser.send(localJSONObject);
/*  438:     */               }
/*  439:     */             }
/*  440:     */             else
/*  441:     */             {
/*  442: 456 */               int i = Convert.getEpochTime() - localBlock.getTimestamp();
/*  443: 457 */               if (i > 0)
/*  444:     */               {
/*  445: 459 */                 BigInteger localBigInteger = BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance())).multiply(BigInteger.valueOf(i));
/*  446: 460 */                 if (((BigInteger)this.hits.get(localAccount)).compareTo(localBigInteger) < 0) {
/*  447: 462 */                   Blockchain.generateBlock(localUser.getSecretPhrase());
/*  448:     */                 }
/*  449:     */               }
/*  450:     */             }
/*  451:     */           }
/*  452:     */         }
/*  453:     */         catch (Exception localException)
/*  454:     */         {
/*  455:     */           Iterator localIterator;
/*  456:     */           Object localObject1;
/*  457:     */           Account localAccount;
/*  458: 471 */           Logger.logDebugMessage("Error in block generation thread", localException);
/*  459:     */         }
/*  460:     */       }
/*  461:     */       catch (Throwable localThrowable)
/*  462:     */       {
/*  463: 474 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  464: 475 */         localThrowable.printStackTrace();
/*  465: 476 */         System.exit(1);
/*  466:     */       }
/*  467:     */     }
/*  468:     */   };
/*  469: 483 */   static final Runnable rebroadcastTransactionsThread = new Runnable()
/*  470:     */   {
/*  471:     */     public void run()
/*  472:     */     {
/*  473:     */       try
/*  474:     */       {
/*  475:     */         try
/*  476:     */         {
/*  477: 490 */           JSONArray localJSONArray = new JSONArray();
/*  478: 492 */           for (Object localObject = Blockchain.nonBroadcastedTransactions.values().iterator(); ((Iterator)localObject).hasNext();)
/*  479:     */           {
/*  480: 492 */             Transaction localTransaction = (Transaction)((Iterator)localObject).next();
/*  481: 494 */             if ((Blockchain.unconfirmedTransactions.get(localTransaction.getId()) == null) && (Blockchain.transactions.get(localTransaction.getId()) == null)) {
/*  482: 496 */               localJSONArray.add(localTransaction.getJSONObject());
/*  483:     */             } else {
/*  484: 500 */               Blockchain.nonBroadcastedTransactions.remove(localTransaction.getId());
/*  485:     */             }
/*  486:     */           }
/*  487: 506 */           if (localJSONArray.size() > 0)
/*  488:     */           {
/*  489: 508 */             localObject = new JSONObject();
/*  490: 509 */             ((JSONObject)localObject).put("requestType", "processTransactions");
/*  491: 510 */             ((JSONObject)localObject).put("transactions", localJSONArray);
/*  492:     */             
/*  493: 512 */             Peer.sendToSomePeers((JSONObject)localObject);
/*  494:     */           }
/*  495:     */         }
/*  496:     */         catch (Exception localException)
/*  497:     */         {
/*  498: 517 */           Logger.logDebugMessage("Error in transaction re-broadcasting thread", localException);
/*  499:     */         }
/*  500:     */       }
/*  501:     */       catch (Throwable localThrowable)
/*  502:     */       {
/*  503: 520 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  504: 521 */         localThrowable.printStackTrace();
/*  505: 522 */         System.exit(1);
/*  506:     */       }
/*  507:     */     }
/*  508:     */   };
/*  509:     */   
/*  510:     */   public static Collection<Block> getAllBlocks()
/*  511:     */   {
/*  512: 530 */     return allBlocks;
/*  513:     */   }
/*  514:     */   
/*  515:     */   public static Collection<Transaction> getAllTransactions()
/*  516:     */   {
/*  517: 534 */     return allTransactions;
/*  518:     */   }
/*  519:     */   
/*  520:     */   public static Collection<Transaction> getAllUnconfirmedTransactions()
/*  521:     */   {
/*  522: 538 */     return allUnconfirmedTransactions;
/*  523:     */   }
/*  524:     */   
/*  525:     */   public static Block getLastBlock()
/*  526:     */   {
/*  527: 542 */     return (Block)lastBlock.get();
/*  528:     */   }
/*  529:     */   
/*  530:     */   public static Block getBlock(Long paramLong)
/*  531:     */   {
/*  532: 546 */     return (Block)blocks.get(paramLong);
/*  533:     */   }
/*  534:     */   
/*  535:     */   public static Transaction getTransaction(Long paramLong)
/*  536:     */   {
/*  537: 550 */     return (Transaction)transactions.get(paramLong);
/*  538:     */   }
/*  539:     */   
/*  540:     */   public static Transaction getUnconfirmedTransaction(Long paramLong)
/*  541:     */   {
/*  542: 554 */     return (Transaction)unconfirmedTransactions.get(paramLong);
/*  543:     */   }
/*  544:     */   
/*  545:     */   public static void broadcast(Transaction paramTransaction)
/*  546:     */   {
/*  547: 559 */     JSONObject localJSONObject = new JSONObject();
/*  548: 560 */     localJSONObject.put("requestType", "processTransactions");
/*  549: 561 */     JSONArray localJSONArray = new JSONArray();
/*  550: 562 */     localJSONArray.add(paramTransaction.getJSONObject());
/*  551: 563 */     localJSONObject.put("transactions", localJSONArray);
/*  552:     */     
/*  553: 565 */     Peer.sendToSomePeers(localJSONObject);
/*  554:     */     
/*  555: 567 */     nonBroadcastedTransactions.put(paramTransaction.getId(), paramTransaction);
/*  556:     */   }
/*  557:     */   
/*  558:     */   public static Peer getLastBlockchainFeeder()
/*  559:     */   {
/*  560: 571 */     return lastBlockchainFeeder;
/*  561:     */   }
/*  562:     */   
/*  563:     */   public static void processTransactions(JSONObject paramJSONObject)
/*  564:     */     throws NxtException.ValidationException
/*  565:     */   {
/*  566: 575 */     JSONArray localJSONArray = (JSONArray)paramJSONObject.get("transactions");
/*  567: 576 */     processTransactions(localJSONArray, false);
/*  568:     */   }
/*  569:     */   
/*  570:     */   public static boolean pushBlock(JSONObject paramJSONObject)
/*  571:     */     throws NxtException.ValidationException
/*  572:     */   {
/*  573: 581 */     Block localBlock = Block.getBlock(paramJSONObject);
/*  574: 582 */     if (!((Block)lastBlock.get()).getId().equals(localBlock.getPreviousBlockId())) {
/*  575: 585 */       return false;
/*  576:     */     }
/*  577: 587 */     JSONArray localJSONArray = (JSONArray)paramJSONObject.get("transactions");
/*  578: 588 */     Transaction[] arrayOfTransaction = new Transaction[localJSONArray.size()];
/*  579: 589 */     for (int i = 0; i < arrayOfTransaction.length; i++) {
/*  580: 590 */       arrayOfTransaction[i] = Transaction.getTransaction((JSONObject)localJSONArray.get(i));
/*  581:     */     }
/*  582: 592 */     return pushBlock(localBlock, arrayOfTransaction, true);
/*  583:     */   }
/*  584:     */   
/*  585:     */   static void addBlock(Block paramBlock)
/*  586:     */   {
/*  587: 597 */     if (paramBlock.getPreviousBlockId() == null)
/*  588:     */     {
/*  589: 598 */       blocks.put(paramBlock.getId(), paramBlock);
/*  590: 599 */       lastBlock.set(paramBlock);
/*  591:     */     }
/*  592:     */     else
/*  593:     */     {
/*  594: 601 */       if (!lastBlock.compareAndSet(blocks.get(paramBlock.getPreviousBlockId()), paramBlock)) {
/*  595: 602 */         throw new IllegalStateException("Last block not equal to this.previousBlock");
/*  596:     */       }
/*  597: 604 */       if (blocks.putIfAbsent(paramBlock.getId(), paramBlock) != null) {
/*  598: 605 */         throw new IllegalStateException("duplicate block id: " + paramBlock.getId());
/*  599:     */       }
/*  600:     */     }
/*  601:     */   }
/*  602:     */   
/*  603:     */   static void init()
/*  604:     */   {
/*  605:     */     Object localObject1;
/*  606:     */     try
/*  607:     */     {
/*  608: 614 */       Logger.logMessage("Loading transactions...");
/*  609: 615 */       loadTransactions("transactions.nxt");
/*  610: 616 */       Logger.logMessage("...Done");
/*  611:     */     }
/*  612:     */     catch (FileNotFoundException localFileNotFoundException1)
/*  613:     */     {
/*  614: 619 */       Logger.logMessage("transactions.nxt not found, starting from scratch");
/*  615: 620 */       transactions.clear();
/*  616:     */       Transaction localTransaction;
/*  617:     */       try
/*  618:     */       {
/*  619: 624 */         for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++)
/*  620:     */         {
/*  621: 626 */           localTransaction = Transaction.newTransaction(0, (short)0, Genesis.CREATOR_PUBLIC_KEY, Genesis.GENESIS_RECIPIENTS[i], Genesis.GENESIS_AMOUNTS[i], 0, null, Genesis.GENESIS_SIGNATURES[i]);
/*  622:     */           
/*  623:     */ 
/*  624: 629 */           transactions.put(localTransaction.getId(), localTransaction);
/*  625:     */         }
/*  626:     */       }
/*  627:     */       catch (NxtException.ValidationException localValidationException1)
/*  628:     */       {
/*  629: 634 */         Logger.logMessage(localValidationException1.getMessage());
/*  630: 635 */         System.exit(1);
/*  631:     */       }
/*  632: 638 */       for (localObject1 = transactions.values().iterator(); ((Iterator)localObject1).hasNext();)
/*  633:     */       {
/*  634: 638 */         localTransaction = (Transaction)((Iterator)localObject1).next();
/*  635: 639 */         localTransaction.setIndex(transactionCounter.incrementAndGet());
/*  636: 640 */         localTransaction.setBlockId(Genesis.GENESIS_BLOCK_ID);
/*  637:     */       }
/*  638: 644 */       saveTransactions("transactions.nxt");
/*  639:     */     }
/*  640:     */     try
/*  641:     */     {
/*  642: 650 */       Logger.logMessage("Loading blocks...");
/*  643: 651 */       loadBlocks("blocks.nxt");
/*  644: 652 */       Logger.logMessage("...Done");
/*  645:     */     }
/*  646:     */     catch (FileNotFoundException localFileNotFoundException2)
/*  647:     */     {
/*  648: 655 */       Logger.logMessage("blocks.nxt not found, starting from scratch");
/*  649: 656 */       blocks.clear();
/*  650:     */       try
/*  651:     */       {
/*  652: 660 */         localObject1 = new Block(-1, 0, null, transactions.size(), 1000000000, 0, transactions.size() * 128, null, Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE);
/*  653:     */         
/*  654: 662 */         ((Block)localObject1).setIndex(blockCounter.incrementAndGet());
/*  655:     */         
/*  656: 664 */         int j = 0;
/*  657: 665 */         for (Object localObject2 = transactions.keySet().iterator(); ((Iterator)localObject2).hasNext();)
/*  658:     */         {
/*  659: 665 */           localObject3 = (Long)((Iterator)localObject2).next();
/*  660:     */           
/*  661: 667 */           ((Block)localObject1).transactionIds[(j++)] = localObject3;
/*  662:     */         }
/*  663:     */         Object localObject3;
/*  664: 670 */         Arrays.sort(((Block)localObject1).transactionIds);
/*  665: 671 */         localObject2 = Crypto.sha256();
/*  666: 672 */         for (j = 0; j < ((Block)localObject1).transactionIds.length; j++)
/*  667:     */         {
/*  668: 673 */           localObject3 = (Transaction)transactions.get(localObject1.transactionIds[j]);
/*  669: 674 */           ((MessageDigest)localObject2).update(((Transaction)localObject3).getBytes());
/*  670: 675 */           ((Block)localObject1).blockTransactions[j] = localObject3;
/*  671:     */         }
/*  672: 677 */         ((Block)localObject1).setPayloadHash(((MessageDigest)localObject2).digest());
/*  673:     */         
/*  674: 679 */         blocks.put(Genesis.GENESIS_BLOCK_ID, localObject1);
/*  675: 680 */         lastBlock.set(localObject1);
/*  676:     */       }
/*  677:     */       catch (NxtException.ValidationException localValidationException2)
/*  678:     */       {
/*  679: 683 */         Logger.logMessage(localValidationException2.getMessage());
/*  680: 684 */         System.exit(1);
/*  681:     */       }
/*  682: 687 */       saveBlocks("blocks.nxt");
/*  683:     */     }
/*  684: 691 */     Logger.logMessage("Scanning blockchain...");
/*  685: 692 */     scan();
/*  686: 693 */     Logger.logMessage("...Done");
/*  687:     */   }
/*  688:     */   
/*  689:     */   static void shutdown()
/*  690:     */   {
/*  691:     */     try
/*  692:     */     {
/*  693: 698 */       saveBlocks("blocks.nxt");
/*  694: 699 */       Logger.logMessage("Saved blocks.nxt");
/*  695:     */     }
/*  696:     */     catch (RuntimeException localRuntimeException1)
/*  697:     */     {
/*  698: 701 */       Logger.logMessage("Error saving blocks", localRuntimeException1);
/*  699:     */     }
/*  700:     */     try
/*  701:     */     {
/*  702: 705 */       saveTransactions("transactions.nxt");
/*  703: 706 */       Logger.logMessage("Saved transactions.nxt");
/*  704:     */     }
/*  705:     */     catch (RuntimeException localRuntimeException2)
/*  706:     */     {
/*  707: 708 */       Logger.logMessage("Error saving transactions", localRuntimeException2);
/*  708:     */     }
/*  709:     */   }
/*  710:     */   
/*  711:     */   private static void processUnconfirmedTransactions(JSONObject paramJSONObject)
/*  712:     */     throws NxtException.ValidationException
/*  713:     */   {
/*  714: 713 */     JSONArray localJSONArray = (JSONArray)paramJSONObject.get("unconfirmedTransactions");
/*  715: 714 */     processTransactions(localJSONArray, true);
/*  716:     */   }
/*  717:     */   
/*  718:     */   private static void processTransactions(JSONArray paramJSONArray, boolean paramBoolean)
/*  719:     */     throws NxtException.ValidationException
/*  720:     */   {
/*  721: 719 */     JSONArray localJSONArray = new JSONArray();
/*  722: 721 */     for (Object localObject1 = paramJSONArray.iterator(); ((Iterator)localObject1).hasNext();)
/*  723:     */     {
/*  724: 721 */       Object localObject2 = ((Iterator)localObject1).next();
/*  725:     */       try
/*  726:     */       {
/*  727: 725 */         Transaction localTransaction = Transaction.getTransaction((JSONObject)localObject2);
/*  728:     */         
/*  729: 727 */         int i = Convert.getEpochTime();
/*  730: 728 */         if ((localTransaction.getTimestamp() > i + 15) || (localTransaction.getExpiration() < i) || (localTransaction.getDeadline() <= 1440))
/*  731:     */         {
/*  732:     */           boolean bool;
/*  733: 735 */           synchronized (Blockchain.class)
/*  734:     */           {
/*  735: 737 */             localObject3 = localTransaction.getId();
/*  736: 738 */             if (((!transactions.containsKey(localObject3)) && (!unconfirmedTransactions.containsKey(localObject3)) && (!doubleSpendingTransactions.containsKey(localObject3)) && (!localTransaction.verify())) || 
/*  737:     */             
/*  738:     */ 
/*  739:     */ 
/*  740:     */ 
/*  741: 743 */               (transactionHashes.containsKey(localTransaction.getHash()))) {
/*  742:     */               continue;
/*  743:     */             }
/*  744: 747 */             bool = localTransaction.isDoubleSpending();
/*  745:     */             
/*  746: 749 */             localTransaction.setIndex(transactionCounter.incrementAndGet());
/*  747: 751 */             if (bool)
/*  748:     */             {
/*  749: 753 */               doubleSpendingTransactions.put(localObject3, localTransaction);
/*  750:     */             }
/*  751:     */             else
/*  752:     */             {
/*  753: 757 */               unconfirmedTransactions.put(localObject3, localTransaction);
/*  754: 759 */               if (!paramBoolean) {
/*  755: 761 */                 localJSONArray.add(localObject2);
/*  756:     */               }
/*  757:     */             }
/*  758:     */           }
/*  759: 769 */           ??? = new JSONObject();
/*  760: 770 */           ((JSONObject)???).put("response", "processNewData");
/*  761:     */           
/*  762: 772 */           Object localObject3 = new JSONArray();
/*  763: 773 */           JSONObject localJSONObject = new JSONObject();
/*  764: 774 */           localJSONObject.put("index", Integer.valueOf(localTransaction.getIndex()));
/*  765: 775 */           localJSONObject.put("timestamp", Integer.valueOf(localTransaction.getTimestamp()));
/*  766: 776 */           localJSONObject.put("deadline", Short.valueOf(localTransaction.getDeadline()));
/*  767: 777 */           localJSONObject.put("recipient", Convert.convert(localTransaction.getRecipientId()));
/*  768: 778 */           localJSONObject.put("amount", Integer.valueOf(localTransaction.getAmount()));
/*  769: 779 */           localJSONObject.put("fee", Integer.valueOf(localTransaction.getFee()));
/*  770: 780 */           localJSONObject.put("sender", Convert.convert(localTransaction.getSenderAccountId()));
/*  771: 781 */           localJSONObject.put("id", localTransaction.getStringId());
/*  772: 782 */           ((JSONArray)localObject3).add(localJSONObject);
/*  773: 784 */           if (bool) {
/*  774: 786 */             ((JSONObject)???).put("addedDoubleSpendingTransactions", localObject3);
/*  775:     */           } else {
/*  776: 790 */             ((JSONObject)???).put("addedUnconfirmedTransactions", localObject3);
/*  777:     */           }
/*  778: 794 */           User.sendToAll((JSONStreamAware)???);
/*  779:     */         }
/*  780:     */       }
/*  781:     */       catch (RuntimeException localRuntimeException)
/*  782:     */       {
/*  783: 798 */         Logger.logMessage("Error processing transaction", localRuntimeException);
/*  784:     */       }
/*  785:     */     }
/*  786: 804 */     if (localJSONArray.size() > 0)
/*  787:     */     {
/*  788: 806 */       localObject1 = new JSONObject();
/*  789: 807 */       ((JSONObject)localObject1).put("requestType", "processTransactions");
/*  790: 808 */       ((JSONObject)localObject1).put("transactions", localJSONArray);
/*  791:     */       
/*  792: 810 */       Peer.sendToSomePeers((JSONObject)localObject1);
/*  793:     */     }
/*  794:     */   }
/*  795:     */   
/*  796:     */   private static synchronized byte[] calculateTransactionsChecksum()
/*  797:     */   {
/*  798: 817 */     PriorityQueue localPriorityQueue = new PriorityQueue(transactions.size(), new Comparator()
/*  799:     */     {
/*  800:     */       public int compare(Transaction paramAnonymousTransaction1, Transaction paramAnonymousTransaction2)
/*  801:     */       {
/*  802: 820 */         long l1 = paramAnonymousTransaction1.getId().longValue();
/*  803: 821 */         long l2 = paramAnonymousTransaction2.getId().longValue();
/*  804: 822 */         return paramAnonymousTransaction1.getTimestamp() > paramAnonymousTransaction2.getTimestamp() ? 1 : paramAnonymousTransaction1.getTimestamp() < paramAnonymousTransaction2.getTimestamp() ? -1 : l1 > l2 ? 1 : l1 < l2 ? -1 : 0;
/*  805:     */       }
/*  806: 824 */     });
/*  807: 825 */     localPriorityQueue.addAll(transactions.values());
/*  808: 826 */     MessageDigest localMessageDigest = Crypto.sha256();
/*  809: 827 */     while (!localPriorityQueue.isEmpty()) {
/*  810: 828 */       localMessageDigest.update(((Transaction)localPriorityQueue.poll()).getBytes());
/*  811:     */     }
/*  812: 830 */     return localMessageDigest.digest();
/*  813:     */   }
/*  814:     */   
/*  815:     */   private static boolean pushBlock(Block paramBlock, Transaction[] paramArrayOfTransaction, boolean paramBoolean)
/*  816:     */   {
/*  817: 837 */     int i = Convert.getEpochTime();
/*  818:     */     JSONArray localJSONArray1;
/*  819:     */     JSONArray localJSONArray2;
/*  820: 839 */     synchronized (Blockchain.class)
/*  821:     */     {
/*  822:     */       try
/*  823:     */       {
/*  824: 842 */         Block localBlock = (Block)lastBlock.get();
/*  825: 844 */         if (paramBlock.getVersion() != (localBlock.getHeight() < 30000 ? 1 : 2)) {
/*  826: 846 */           return false;
/*  827:     */         }
/*  828: 849 */         if (localBlock.getHeight() == 30000)
/*  829:     */         {
/*  830: 851 */           localObject1 = calculateTransactionsChecksum();
/*  831: 852 */           if (CHECKSUM_TRANSPARENT_FORGING == null)
/*  832:     */           {
/*  833: 853 */             Logger.logMessage("Checksum calculated:\n" + Arrays.toString((byte[])localObject1));
/*  834:     */           }
/*  835:     */           else
/*  836:     */           {
/*  837: 854 */             if (!Arrays.equals((byte[])localObject1, CHECKSUM_TRANSPARENT_FORGING))
/*  838:     */             {
/*  839: 855 */               Logger.logMessage("Checksum failed at block 30000");
/*  840: 856 */               return false;
/*  841:     */             }
/*  842: 858 */             Logger.logMessage("Checksum passed at block 30000");
/*  843:     */           }
/*  844:     */         }
/*  845: 863 */         if ((paramBlock.getVersion() != 1) && (!Arrays.equals(Crypto.sha256().digest(localBlock.getBytes()), paramBlock.getPreviousBlockHash()))) {
/*  846: 865 */           return false;
/*  847:     */         }
/*  848: 868 */         if ((paramBlock.getTimestamp() > i + 15) || (paramBlock.getTimestamp() <= localBlock.getTimestamp())) {
/*  849: 870 */           return false;
/*  850:     */         }
/*  851: 873 */         if ((!localBlock.getId().equals(paramBlock.getPreviousBlockId())) || (paramBlock.getId().equals(Long.valueOf(0L))) || (blocks.containsKey(paramBlock.getId())) || (!paramBlock.verifyGenerationSignature()) || (!paramBlock.verifyBlockSignature())) {
/*  852: 877 */           return false;
/*  853:     */         }
/*  854: 880 */         paramBlock.setIndex(blockCounter.incrementAndGet());
/*  855:     */         
/*  856: 882 */         localObject1 = new HashMap();
/*  857: 883 */         HashMap localHashMap1 = new HashMap();
/*  858: 884 */         for (int j = 0; j < paramBlock.transactionIds.length; j++)
/*  859:     */         {
/*  860: 886 */           localObject2 = paramArrayOfTransaction[j];
/*  861: 887 */           ((Transaction)localObject2).setIndex(transactionCounter.incrementAndGet());
/*  862: 889 */           if (((Map)localObject1).put(paramBlock.transactionIds[j] =  = ((Transaction)localObject2).getId(), localObject2) != null) {
/*  863: 891 */             return false;
/*  864:     */           }
/*  865: 894 */           if (((Transaction)localObject2).isDuplicate(localHashMap1)) {
/*  866: 896 */             return false;
/*  867:     */           }
/*  868:     */         }
/*  869: 899 */         Arrays.sort(paramBlock.transactionIds);
/*  870:     */         
/*  871: 901 */         HashMap localHashMap2 = new HashMap();
/*  872: 902 */         Object localObject2 = new HashMap();
/*  873: 903 */         int k = 0;int m = 0;
/*  874: 904 */         MessageDigest localMessageDigest = Crypto.sha256();
/*  875:     */         Object localObject6;
/*  876:     */         Object localObject7;
/*  877: 905 */         for (localObject6 : paramBlock.transactionIds)
/*  878:     */         {
/*  879: 907 */           localObject7 = (Transaction)((Map)localObject1).get(localObject6);
/*  880: 909 */           if ((((Transaction)localObject7).getTimestamp() > i + 15) || ((((Transaction)localObject7).getExpiration() < paramBlock.getTimestamp()) && (localBlock.getHeight() > 303)) || (transactions.get(localObject6) != null) || ((((Transaction)localObject7).getReferencedTransactionId() != null) && (transactions.get(((Transaction)localObject7).getReferencedTransactionId()) == null) && (((Map)localObject1).get(((Transaction)localObject7).getReferencedTransactionId()) == null)) || ((unconfirmedTransactions.get(localObject6) == null) && (!((Transaction)localObject7).verify())) || (((Transaction)localObject7).getId().equals(Long.valueOf(0L)))) {
/*  881: 916 */             return false;
/*  882:     */           }
/*  883: 919 */           k += ((Transaction)localObject7).getAmount();
/*  884:     */           
/*  885: 921 */           ((Transaction)localObject7).updateTotals(localHashMap2, (Map)localObject2);
/*  886:     */           
/*  887: 923 */           m += ((Transaction)localObject7).getFee();
/*  888:     */           
/*  889: 925 */           localMessageDigest.update(((Transaction)localObject7).getBytes());
/*  890:     */         }
/*  891: 929 */         if ((k != paramBlock.getTotalAmount()) || (m != paramBlock.getTotalFee())) {
/*  892: 931 */           return false;
/*  893:     */         }
/*  894: 934 */         if (!Arrays.equals(localMessageDigest.digest(), paramBlock.getPayloadHash())) {
/*  895: 936 */           return false;
/*  896:     */         }
/*  897: 939 */         for (??? = localHashMap2.entrySet().iterator(); ((Iterator)???).hasNext();)
/*  898:     */         {
/*  899: 939 */           localObject4 = (Map.Entry)((Iterator)???).next();
/*  900: 940 */           localObject5 = Account.getAccount((Long)((Map.Entry)localObject4).getKey());
/*  901: 941 */           if (((Account)localObject5).getBalance() < ((Long)((Map.Entry)localObject4).getValue()).longValue()) {
/*  902: 943 */             return false;
/*  903:     */           }
/*  904:     */         }
/*  905:     */         Object localObject5;
/*  906: 947 */         for (??? = ((Map)localObject2).entrySet().iterator(); ((Iterator)???).hasNext();)
/*  907:     */         {
/*  908: 947 */           localObject4 = (Map.Entry)((Iterator)???).next();
/*  909: 948 */           localObject5 = Account.getAccount((Long)((Map.Entry)localObject4).getKey());
/*  910: 949 */           for (localObject6 = ((Map)((Map.Entry)localObject4).getValue()).entrySet().iterator(); ((Iterator)localObject6).hasNext();)
/*  911:     */           {
/*  912: 949 */             localObject7 = (Map.Entry)((Iterator)localObject6).next();
/*  913: 950 */             long l1 = ((Long)((Map.Entry)localObject7).getKey()).longValue();
/*  914: 951 */             long l2 = ((Long)((Map.Entry)localObject7).getValue()).longValue();
/*  915: 952 */             if (((Account)localObject5).getAssetBalance(Long.valueOf(l1)).intValue() < l2) {
/*  916: 954 */               return false;
/*  917:     */             }
/*  918:     */           }
/*  919:     */         }
/*  920: 959 */         paramBlock.setHeight(localBlock.getHeight() + 1);
/*  921:     */         
/*  922: 961 */         ??? = null;
/*  923: 962 */         for (Object localObject4 = ((Map)localObject1).entrySet().iterator(); ((Iterator)localObject4).hasNext();)
/*  924:     */         {
/*  925: 962 */           localObject5 = (Map.Entry)((Iterator)localObject4).next();
/*  926: 963 */           localObject6 = (Transaction)((Map.Entry)localObject5).getValue();
/*  927: 964 */           ((Transaction)localObject6).setHeight(paramBlock.getHeight());
/*  928: 965 */           ((Transaction)localObject6).setBlockId(paramBlock.getId());
/*  929: 967 */           if ((transactionHashes.putIfAbsent(((Transaction)localObject6).getHash(), localObject6) != null) && (paramBlock.getHeight() != 58294))
/*  930:     */           {
/*  931: 969 */             ??? = localObject6;
/*  932: 970 */             break;
/*  933:     */           }
/*  934: 973 */           if (transactions.putIfAbsent(((Map.Entry)localObject5).getKey(), localObject6) != null)
/*  935:     */           {
/*  936: 974 */             Logger.logMessage("duplicate transaction id " + ((Map.Entry)localObject5).getKey());
/*  937: 975 */             ??? = localObject6;
/*  938: 976 */             break;
/*  939:     */           }
/*  940:     */         }
/*  941: 980 */         if (??? != null)
/*  942:     */         {
/*  943: 981 */           for (localObject4 = ((Map)localObject1).keySet().iterator(); ((Iterator)localObject4).hasNext();)
/*  944:     */           {
/*  945: 981 */             localObject5 = (Long)((Iterator)localObject4).next();
/*  946: 982 */             if (!((Long)localObject5).equals(((Transaction)???).getId()))
/*  947:     */             {
/*  948: 985 */               localObject6 = (Transaction)transactions.remove(localObject5);
/*  949: 986 */               if (localObject6 != null)
/*  950:     */               {
/*  951: 987 */                 localObject7 = (Transaction)transactionHashes.get(((Transaction)localObject6).getHash());
/*  952: 988 */                 if ((localObject7 != null) && (((Transaction)localObject7).getId().equals(localObject5))) {
/*  953: 989 */                   transactionHashes.remove(((Transaction)localObject6).getHash());
/*  954:     */                 }
/*  955:     */               }
/*  956:     */             }
/*  957:     */           }
/*  958: 993 */           return false;
/*  959:     */         }
/*  960: 996 */         paramBlock.apply();
/*  961:     */         
/*  962: 998 */         localJSONArray1 = new JSONArray();
/*  963: 999 */         localJSONArray2 = new JSONArray();
/*  964:1001 */         for (localObject4 = ((Map)localObject1).entrySet().iterator(); ((Iterator)localObject4).hasNext();)
/*  965:     */         {
/*  966:1001 */           localObject5 = (Map.Entry)((Iterator)localObject4).next();
/*  967:     */           
/*  968:1003 */           localObject6 = (Transaction)((Map.Entry)localObject5).getValue();
/*  969:     */           
/*  970:1005 */           localObject7 = new JSONObject();
/*  971:1006 */           ((JSONObject)localObject7).put("index", Integer.valueOf(((Transaction)localObject6).getIndex()));
/*  972:1007 */           ((JSONObject)localObject7).put("blockTimestamp", Integer.valueOf(paramBlock.getTimestamp()));
/*  973:1008 */           ((JSONObject)localObject7).put("transactionTimestamp", Integer.valueOf(((Transaction)localObject6).getTimestamp()));
/*  974:1009 */           ((JSONObject)localObject7).put("sender", Convert.convert(((Transaction)localObject6).getSenderAccountId()));
/*  975:1010 */           ((JSONObject)localObject7).put("recipient", Convert.convert(((Transaction)localObject6).getRecipientId()));
/*  976:1011 */           ((JSONObject)localObject7).put("amount", Integer.valueOf(((Transaction)localObject6).getAmount()));
/*  977:1012 */           ((JSONObject)localObject7).put("fee", Integer.valueOf(((Transaction)localObject6).getFee()));
/*  978:1013 */           ((JSONObject)localObject7).put("id", ((Transaction)localObject6).getStringId());
/*  979:1014 */           localJSONArray1.add(localObject7);
/*  980:     */           
/*  981:1016 */           Transaction localTransaction = (Transaction)unconfirmedTransactions.remove(((Map.Entry)localObject5).getKey());
/*  982:1017 */           if (localTransaction != null)
/*  983:     */           {
/*  984:1018 */             JSONObject localJSONObject2 = new JSONObject();
/*  985:1019 */             localJSONObject2.put("index", Integer.valueOf(localTransaction.getIndex()));
/*  986:1020 */             localJSONArray2.add(localJSONObject2);
/*  987:     */             
/*  988:1022 */             Account localAccount = Account.getAccount(localTransaction.getSenderAccountId());
/*  989:1023 */             localAccount.addToUnconfirmedBalance((localTransaction.getAmount() + localTransaction.getFee()) * 100L);
/*  990:     */           }
/*  991:     */         }
/*  992:1030 */         if (paramBoolean)
/*  993:     */         {
/*  994:1031 */           saveTransactions("transactions.nxt");
/*  995:1032 */           saveBlocks("blocks.nxt");
/*  996:     */         }
/*  997:     */       }
/*  998:     */       catch (RuntimeException localRuntimeException)
/*  999:     */       {
/* 1000:1036 */         Logger.logMessage("Error pushing block", localRuntimeException);
/* 1001:1037 */         return false;
/* 1002:     */       }
/* 1003:     */     }
/* 1004:1041 */     if (paramBlock.getTimestamp() >= i - 15)
/* 1005:     */     {
/* 1006:1043 */       ??? = paramBlock.getJSONObject();
/* 1007:1044 */       ((JSONObject)???).put("requestType", "processBlock");
/* 1008:     */       
/* 1009:1046 */       Peer.sendToSomePeers((JSONObject)???);
/* 1010:     */     }
/* 1011:1050 */     ??? = new JSONArray();
/* 1012:1051 */     JSONObject localJSONObject1 = new JSONObject();
/* 1013:1052 */     localJSONObject1.put("index", Integer.valueOf(paramBlock.getIndex()));
/* 1014:1053 */     localJSONObject1.put("timestamp", Integer.valueOf(paramBlock.getTimestamp()));
/* 1015:1054 */     localJSONObject1.put("numberOfTransactions", Integer.valueOf(paramBlock.transactionIds.length));
/* 1016:1055 */     localJSONObject1.put("totalAmount", Integer.valueOf(paramBlock.getTotalAmount()));
/* 1017:1056 */     localJSONObject1.put("totalFee", Integer.valueOf(paramBlock.getTotalFee()));
/* 1018:1057 */     localJSONObject1.put("payloadLength", Integer.valueOf(paramBlock.getPayloadLength()));
/* 1019:1058 */     localJSONObject1.put("generator", Convert.convert(paramBlock.getGeneratorAccountId()));
/* 1020:1059 */     localJSONObject1.put("height", Integer.valueOf(paramBlock.getHeight()));
/* 1021:1060 */     localJSONObject1.put("version", Integer.valueOf(paramBlock.getVersion()));
/* 1022:1061 */     localJSONObject1.put("block", paramBlock.getStringId());
/* 1023:1062 */     localJSONObject1.put("baseTarget", BigInteger.valueOf(paramBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
/* 1024:1063 */     ((JSONArray)???).add(localJSONObject1);
/* 1025:     */     
/* 1026:1065 */     Object localObject1 = new JSONObject();
/* 1027:1066 */     ((JSONObject)localObject1).put("response", "processNewData");
/* 1028:1067 */     ((JSONObject)localObject1).put("addedConfirmedTransactions", localJSONArray1);
/* 1029:1068 */     if (localJSONArray2.size() > 0) {
/* 1030:1069 */       ((JSONObject)localObject1).put("removedUnconfirmedTransactions", localJSONArray2);
/* 1031:     */     }
/* 1032:1071 */     ((JSONObject)localObject1).put("addedRecentBlocks", ???);
/* 1033:     */     
/* 1034:1073 */     User.sendToAll((JSONStreamAware)localObject1);
/* 1035:     */     
/* 1036:1075 */     return true;
/* 1037:     */   }
/* 1038:     */   
/* 1039:     */   private static boolean popLastBlock()
/* 1040:     */     throws Transaction.UndoNotSupportedException
/* 1041:     */   {
/* 1042:     */     try
/* 1043:     */     {
/* 1044:1083 */       JSONObject localJSONObject1 = new JSONObject();
/* 1045:1084 */       localJSONObject1.put("response", "processNewData");
/* 1046:     */       
/* 1047:1086 */       JSONArray localJSONArray = new JSONArray();
/* 1048:     */       Block localBlock;
/* 1049:1090 */       synchronized (Blockchain.class)
/* 1050:     */       {
/* 1051:1092 */         localBlock = (Block)lastBlock.get();
/* 1052:1094 */         if (localBlock.getId().equals(Genesis.GENESIS_BLOCK_ID)) {
/* 1053:1095 */           return false;
/* 1054:     */         }
/* 1055:1098 */         localObject1 = (Block)blocks.get(localBlock.getPreviousBlockId());
/* 1056:1099 */         if (localObject1 == null)
/* 1057:     */         {
/* 1058:1100 */           Logger.logMessage("Previous block is null");
/* 1059:1101 */           throw new IllegalStateException();
/* 1060:     */         }
/* 1061:1103 */         if (!lastBlock.compareAndSet(localBlock, localObject1))
/* 1062:     */         {
/* 1063:1104 */           Logger.logMessage("This block is no longer last block");
/* 1064:1105 */           throw new IllegalStateException();
/* 1065:     */         }
/* 1066:1108 */         Account localAccount = Account.getAccount(localBlock.getGeneratorAccountId());
/* 1067:1109 */         localAccount.addToBalanceAndUnconfirmedBalance(-localBlock.getTotalFee() * 100L);
/* 1068:1111 */         for (Long localLong : localBlock.transactionIds)
/* 1069:     */         {
/* 1070:1113 */           Transaction localTransaction1 = (Transaction)transactions.remove(localLong);
/* 1071:1114 */           Transaction localTransaction2 = (Transaction)transactionHashes.get(localTransaction1.getHash());
/* 1072:1115 */           if ((localTransaction2 != null) && (localTransaction2.getId().equals(localLong))) {
/* 1073:1116 */             transactionHashes.remove(localTransaction1.getHash());
/* 1074:     */           }
/* 1075:1118 */           unconfirmedTransactions.put(localLong, localTransaction1);
/* 1076:     */           
/* 1077:1120 */           localTransaction1.undo();
/* 1078:     */           
/* 1079:1122 */           JSONObject localJSONObject2 = new JSONObject();
/* 1080:1123 */           localJSONObject2.put("index", Integer.valueOf(localTransaction1.getIndex()));
/* 1081:1124 */           localJSONObject2.put("timestamp", Integer.valueOf(localTransaction1.getTimestamp()));
/* 1082:1125 */           localJSONObject2.put("deadline", Short.valueOf(localTransaction1.getDeadline()));
/* 1083:1126 */           localJSONObject2.put("recipient", Convert.convert(localTransaction1.getRecipientId()));
/* 1084:1127 */           localJSONObject2.put("amount", Integer.valueOf(localTransaction1.getAmount()));
/* 1085:1128 */           localJSONObject2.put("fee", Integer.valueOf(localTransaction1.getFee()));
/* 1086:1129 */           localJSONObject2.put("sender", Convert.convert(localTransaction1.getSenderAccountId()));
/* 1087:1130 */           localJSONObject2.put("id", localTransaction1.getStringId());
/* 1088:1131 */           localJSONArray.add(localJSONObject2);
/* 1089:     */         }
/* 1090:     */       }
/* 1091:1137 */       ??? = new JSONArray();
/* 1092:1138 */       Object localObject1 = new JSONObject();
/* 1093:1139 */       ((JSONObject)localObject1).put("index", Integer.valueOf(localBlock.getIndex()));
/* 1094:1140 */       ((JSONObject)localObject1).put("timestamp", Integer.valueOf(localBlock.getTimestamp()));
/* 1095:1141 */       ((JSONObject)localObject1).put("numberOfTransactions", Integer.valueOf(localBlock.transactionIds.length));
/* 1096:1142 */       ((JSONObject)localObject1).put("totalAmount", Integer.valueOf(localBlock.getTotalAmount()));
/* 1097:1143 */       ((JSONObject)localObject1).put("totalFee", Integer.valueOf(localBlock.getTotalFee()));
/* 1098:1144 */       ((JSONObject)localObject1).put("payloadLength", Integer.valueOf(localBlock.getPayloadLength()));
/* 1099:1145 */       ((JSONObject)localObject1).put("generator", Convert.convert(localBlock.getGeneratorAccountId()));
/* 1100:1146 */       ((JSONObject)localObject1).put("height", Integer.valueOf(localBlock.getHeight()));
/* 1101:1147 */       ((JSONObject)localObject1).put("version", Integer.valueOf(localBlock.getVersion()));
/* 1102:1148 */       ((JSONObject)localObject1).put("block", localBlock.getStringId());
/* 1103:1149 */       ((JSONObject)localObject1).put("baseTarget", BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
/* 1104:1150 */       ((JSONArray)???).add(localObject1);
/* 1105:1151 */       localJSONObject1.put("addedOrphanedBlocks", ???);
/* 1106:1153 */       if (localJSONArray.size() > 0) {
/* 1107:1154 */         localJSONObject1.put("addedUnconfirmedTransactions", localJSONArray);
/* 1108:     */       }
/* 1109:1157 */       User.sendToAll(localJSONObject1);
/* 1110:     */     }
/* 1111:     */     catch (RuntimeException localRuntimeException)
/* 1112:     */     {
/* 1113:1160 */       Logger.logMessage("Error popping last block", localRuntimeException);
/* 1114:1161 */       return false;
/* 1115:     */     }
/* 1116:1163 */     return true;
/* 1117:     */   }
/* 1118:     */   
/* 1119:     */   private static synchronized void scan()
/* 1120:     */   {
/* 1121:1167 */     HashMap localHashMap = new HashMap(blocks);
/* 1122:1168 */     blocks.clear();
/* 1123:1169 */     Long localLong = Genesis.GENESIS_BLOCK_ID;
/* 1124:     */     Block localBlock;
/* 1125:1171 */     while ((localBlock = (Block)localHashMap.get(localLong)) != null)
/* 1126:     */     {
/* 1127:1172 */       localBlock.apply();
/* 1128:1173 */       localLong = localBlock.getNextBlockId();
/* 1129:     */     }
/* 1130:     */   }
/* 1131:     */   
/* 1132:     */   private static void generateBlock(String paramString)
/* 1133:     */   {
/* 1134:1179 */     TreeSet localTreeSet = new TreeSet();
/* 1135:1181 */     for (Object localObject1 = unconfirmedTransactions.values().iterator(); ((Iterator)localObject1).hasNext();)
/* 1136:     */     {
/* 1137:1181 */       localObject2 = (Transaction)((Iterator)localObject1).next();
/* 1138:1183 */       if ((((Transaction)localObject2).getReferencedTransactionId() == null) || (transactions.get(((Transaction)localObject2).getReferencedTransactionId()) != null)) {
/* 1139:1185 */         localTreeSet.add(localObject2);
/* 1140:     */       }
/* 1141:     */     }
/* 1142:1191 */     localObject1 = new HashMap();
/* 1143:1192 */     Object localObject2 = new HashMap();
/* 1144:1193 */     HashMap localHashMap = new HashMap();
/* 1145:     */     
/* 1146:1195 */     int i = 0;
/* 1147:1196 */     int j = 0;
/* 1148:1197 */     int k = 0;
/* 1149:     */     Object localObject3;
/* 1150:1199 */     while (k <= 32640)
/* 1151:     */     {
/* 1152:1201 */       int m = ((Map)localObject1).size();
/* 1153:1203 */       for (localObject3 = localTreeSet.iterator(); ((Iterator)localObject3).hasNext();)
/* 1154:     */       {
/* 1155:1203 */         localObject4 = (Transaction)((Iterator)localObject3).next();
/* 1156:     */         
/* 1157:1205 */         int n = ((Transaction)localObject4).getSize();
/* 1158:1206 */         if ((((Map)localObject1).get(((Transaction)localObject4).getId()) == null) && (k + n <= 32640))
/* 1159:     */         {
/* 1160:1208 */           localObject5 = ((Transaction)localObject4).getSenderAccountId();
/* 1161:1209 */           localObject6 = (Long)localHashMap.get(localObject5);
/* 1162:1210 */           if (localObject6 == null) {
/* 1163:1212 */             localObject6 = Long.valueOf(0L);
/* 1164:     */           }
/* 1165:1216 */           long l = (((Transaction)localObject4).getAmount() + ((Transaction)localObject4).getFee()) * 100L;
/* 1166:1217 */           if (((Long)localObject6).longValue() + l <= Account.getAccount((Long)localObject5).getBalance()) {
/* 1167:1219 */             if (!((Transaction)localObject4).isDuplicate((Map)localObject2))
/* 1168:     */             {
/* 1169:1223 */               localHashMap.put(localObject5, Long.valueOf(((Long)localObject6).longValue() + l));
/* 1170:     */               
/* 1171:1225 */               ((Map)localObject1).put(((Transaction)localObject4).getId(), localObject4);
/* 1172:1226 */               k += n;
/* 1173:1227 */               i += ((Transaction)localObject4).getAmount();
/* 1174:1228 */               j += ((Transaction)localObject4).getFee();
/* 1175:     */             }
/* 1176:     */           }
/* 1177:     */         }
/* 1178:     */       }
/* 1179:1236 */       if (((Map)localObject1).size() == m) {
/* 1180:     */         break;
/* 1181:     */       }
/* 1182:     */     }
/* 1183:1244 */     byte[] arrayOfByte1 = Crypto.getPublicKey(paramString);
/* 1184:     */     
/* 1185:     */ 
/* 1186:1247 */     Object localObject4 = (Block)lastBlock.get();
/* 1187:     */     try
/* 1188:     */     {
/* 1189:1250 */       if (((Block)localObject4).getHeight() < 30000)
/* 1190:     */       {
/* 1191:1252 */         localObject3 = new Block(1, Convert.getEpochTime(), ((Block)localObject4).getId(), ((Map)localObject1).size(), i, j, k, null, arrayOfByte1, null, new byte[64]);
/* 1192:     */       }
/* 1193:     */       else
/* 1194:     */       {
/* 1195:1257 */         byte[] arrayOfByte2 = Crypto.sha256().digest(((Block)localObject4).getBytes());
/* 1196:1258 */         localObject3 = new Block(2, Convert.getEpochTime(), ((Block)localObject4).getId(), ((Map)localObject1).size(), i, j, k, null, arrayOfByte1, null, new byte[64], arrayOfByte2);
/* 1197:     */       }
/* 1198:     */     }
/* 1199:     */     catch (NxtException.ValidationException localValidationException)
/* 1200:     */     {
/* 1201:1264 */       Logger.logMessage("Error generating block", localValidationException);
/* 1202:1265 */       return;
/* 1203:     */     }
/* 1204:1268 */     int i1 = 0;
/* 1205:1269 */     for (Object localObject5 = ((Map)localObject1).keySet().iterator(); ((Iterator)localObject5).hasNext();)
/* 1206:     */     {
/* 1207:1269 */       localObject6 = (Long)((Iterator)localObject5).next();
/* 1208:1270 */       ((Block)localObject3).transactionIds[(i1++)] = localObject6;
/* 1209:     */     }
/* 1210:1273 */     Arrays.sort(((Block)localObject3).transactionIds);
/* 1211:1274 */     localObject5 = Crypto.sha256();
/* 1212:1275 */     for (i1 = 0; i1 < ((Block)localObject3).transactionIds.length; i1++)
/* 1213:     */     {
/* 1214:1276 */       localObject6 = (Transaction)((Map)localObject1).get(localObject3.transactionIds[i1]);
/* 1215:1277 */       ((MessageDigest)localObject5).update(((Transaction)localObject6).getBytes());
/* 1216:1278 */       ((Block)localObject3).blockTransactions[i1] = localObject6;
/* 1217:     */     }
/* 1218:1280 */     ((Block)localObject3).setPayloadHash(((MessageDigest)localObject5).digest());
/* 1219:1282 */     if (((Block)localObject4).getHeight() < 30000)
/* 1220:     */     {
/* 1221:1284 */       ((Block)localObject3).setGenerationSignature(Crypto.sign(((Block)localObject4).getGenerationSignature(), paramString));
/* 1222:     */     }
/* 1223:     */     else
/* 1224:     */     {
/* 1225:1288 */       ((MessageDigest)localObject5).update(((Block)localObject4).getGenerationSignature());
/* 1226:1289 */       ((Block)localObject3).setGenerationSignature(((MessageDigest)localObject5).digest(arrayOfByte1));
/* 1227:     */     }
/* 1228:1293 */     Object localObject6 = ((Block)localObject3).getBytes();
/* 1229:1294 */     byte[] arrayOfByte3 = new byte[localObject6.length - 64];
/* 1230:1295 */     System.arraycopy(localObject6, 0, arrayOfByte3, 0, arrayOfByte3.length);
/* 1231:1296 */     ((Block)localObject3).setBlockSignature(Crypto.sign(arrayOfByte3, paramString));
/* 1232:1298 */     if ((((Block)localObject3).verifyBlockSignature()) && (((Block)localObject3).verifyGenerationSignature()))
/* 1233:     */     {
/* 1234:1300 */       JSONObject localJSONObject = ((Block)localObject3).getJSONObject();
/* 1235:1301 */       localJSONObject.put("requestType", "processBlock");
/* 1236:1302 */       Peer.sendToSomePeers(localJSONObject);
/* 1237:     */     }
/* 1238:     */     else
/* 1239:     */     {
/* 1240:1306 */       Logger.logMessage("Generated an incorrect block. Waiting for the next one...");
/* 1241:     */     }
/* 1242:     */   }
/* 1243:     */   
/* 1244:     */   static void purgeExpiredHashes(int paramInt)
/* 1245:     */   {
/* 1246:1313 */     Iterator localIterator = transactionHashes.entrySet().iterator();
/* 1247:1314 */     while (localIterator.hasNext()) {
/* 1248:1315 */       if (((Transaction)((Map.Entry)localIterator.next()).getValue()).getExpiration() < paramInt) {
/* 1249:1316 */         localIterator.remove();
/* 1250:     */       }
/* 1251:     */     }
/* 1252:     */   }
/* 1253:     */   
/* 1254:     */   private static void loadTransactions(String paramString)
/* 1255:     */     throws FileNotFoundException
/* 1256:     */   {
/* 1257:     */     try
/* 1258:     */     {
/* 1259:1323 */       FileInputStream localFileInputStream = new FileInputStream(paramString);Object localObject1 = null;
/* 1260:     */       try
/* 1261:     */       {
/* 1262:1324 */         ObjectInputStream localObjectInputStream = new ObjectInputStream(localFileInputStream);Object localObject2 = null;
/* 1263:     */         try
/* 1264:     */         {
/* 1265:1325 */           transactionCounter.set(localObjectInputStream.readInt());
/* 1266:1326 */           transactions.clear();
/* 1267:1327 */           transactions.putAll((HashMap)localObjectInputStream.readObject());
/* 1268:     */         }
/* 1269:     */         catch (Throwable localThrowable4)
/* 1270:     */         {
/* 1271:1323 */           localObject2 = localThrowable4;throw localThrowable4;
/* 1272:     */         }
/* 1273:     */         finally {}
/* 1274:     */       }
/* 1275:     */       catch (Throwable localThrowable2)
/* 1276:     */       {
/* 1277:1323 */         localObject1 = localThrowable2;throw localThrowable2;
/* 1278:     */       }
/* 1279:     */       finally
/* 1280:     */       {
/* 1281:1328 */         if (localFileInputStream != null) {
/* 1282:1328 */           if (localObject1 != null) {
/* 1283:     */             try
/* 1284:     */             {
/* 1285:1328 */               localFileInputStream.close();
/* 1286:     */             }
/* 1287:     */             catch (Throwable localThrowable6)
/* 1288:     */             {
/* 1289:1328 */               localObject1.addSuppressed(localThrowable6);
/* 1290:     */             }
/* 1291:     */           } else {
/* 1292:1328 */             localFileInputStream.close();
/* 1293:     */           }
/* 1294:     */         }
/* 1295:     */       }
/* 1296:     */     }
/* 1297:     */     catch (FileNotFoundException localFileNotFoundException)
/* 1298:     */     {
/* 1299:1329 */       throw localFileNotFoundException;
/* 1300:     */     }
/* 1301:     */     catch (IOException|ClassNotFoundException localIOException)
/* 1302:     */     {
/* 1303:1331 */       Logger.logMessage("Error loading transactions from " + paramString, localIOException);
/* 1304:1332 */       System.exit(1);
/* 1305:     */     }
/* 1306:     */   }
/* 1307:     */   
/* 1308:     */   private static void saveTransactions(String paramString)
/* 1309:     */   {
/* 1310:     */     try
/* 1311:     */     {
/* 1312:1339 */       FileOutputStream localFileOutputStream = new FileOutputStream(paramString);Object localObject1 = null;
/* 1313:     */       try
/* 1314:     */       {
/* 1315:1340 */         ObjectOutputStream localObjectOutputStream = new ObjectOutputStream(localFileOutputStream);Object localObject2 = null;
/* 1316:     */         try
/* 1317:     */         {
/* 1318:1342 */           localObjectOutputStream.writeInt(transactionCounter.get());
/* 1319:1343 */           localObjectOutputStream.writeObject(new HashMap(transactions));
/* 1320:1344 */           localObjectOutputStream.close();
/* 1321:     */         }
/* 1322:     */         catch (Throwable localThrowable4)
/* 1323:     */         {
/* 1324:1339 */           localObject2 = localThrowable4;throw localThrowable4;
/* 1325:     */         }
/* 1326:     */         finally {}
/* 1327:     */       }
/* 1328:     */       catch (Throwable localThrowable2)
/* 1329:     */       {
/* 1330:1339 */         localObject1 = localThrowable2;throw localThrowable2;
/* 1331:     */       }
/* 1332:     */       finally
/* 1333:     */       {
/* 1334:1345 */         if (localFileOutputStream != null) {
/* 1335:1345 */           if (localObject1 != null) {
/* 1336:     */             try
/* 1337:     */             {
/* 1338:1345 */               localFileOutputStream.close();
/* 1339:     */             }
/* 1340:     */             catch (Throwable localThrowable6)
/* 1341:     */             {
/* 1342:1345 */               localObject1.addSuppressed(localThrowable6);
/* 1343:     */             }
/* 1344:     */           } else {
/* 1345:1345 */             localFileOutputStream.close();
/* 1346:     */           }
/* 1347:     */         }
/* 1348:     */       }
/* 1349:     */     }
/* 1350:     */     catch (IOException localIOException)
/* 1351:     */     {
/* 1352:1346 */       Logger.logMessage("Error saving transactions to " + paramString, localIOException);
/* 1353:1347 */       throw new RuntimeException(localIOException);
/* 1354:     */     }
/* 1355:     */   }
/* 1356:     */   
/* 1357:     */   private static void loadBlocks(String paramString)
/* 1358:     */     throws FileNotFoundException
/* 1359:     */   {
/* 1360:     */     try
/* 1361:     */     {
/* 1362:1354 */       FileInputStream localFileInputStream = new FileInputStream(paramString);Object localObject1 = null;
/* 1363:     */       try
/* 1364:     */       {
/* 1365:1355 */         ObjectInputStream localObjectInputStream = new ObjectInputStream(localFileInputStream);Object localObject2 = null;
/* 1366:     */         try
/* 1367:     */         {
/* 1368:1357 */           blockCounter.set(localObjectInputStream.readInt());
/* 1369:1358 */           blocks.clear();
/* 1370:1359 */           blocks.putAll((HashMap)localObjectInputStream.readObject());
/* 1371:     */         }
/* 1372:     */         catch (Throwable localThrowable4)
/* 1373:     */         {
/* 1374:1354 */           localObject2 = localThrowable4;throw localThrowable4;
/* 1375:     */         }
/* 1376:     */         finally {}
/* 1377:     */       }
/* 1378:     */       catch (Throwable localThrowable2)
/* 1379:     */       {
/* 1380:1354 */         localObject1 = localThrowable2;throw localThrowable2;
/* 1381:     */       }
/* 1382:     */       finally
/* 1383:     */       {
/* 1384:1360 */         if (localFileInputStream != null) {
/* 1385:1360 */           if (localObject1 != null) {
/* 1386:     */             try
/* 1387:     */             {
/* 1388:1360 */               localFileInputStream.close();
/* 1389:     */             }
/* 1390:     */             catch (Throwable localThrowable6)
/* 1391:     */             {
/* 1392:1360 */               localObject1.addSuppressed(localThrowable6);
/* 1393:     */             }
/* 1394:     */           } else {
/* 1395:1360 */             localFileInputStream.close();
/* 1396:     */           }
/* 1397:     */         }
/* 1398:     */       }
/* 1399:     */     }
/* 1400:     */     catch (FileNotFoundException localFileNotFoundException)
/* 1401:     */     {
/* 1402:1361 */       throw localFileNotFoundException;
/* 1403:     */     }
/* 1404:     */     catch (IOException|ClassNotFoundException localIOException)
/* 1405:     */     {
/* 1406:1363 */       Logger.logMessage("Error loading blocks from " + paramString, localIOException);
/* 1407:1364 */       System.exit(1);
/* 1408:     */     }
/* 1409:     */   }
/* 1410:     */   
/* 1411:     */   private static void saveBlocks(String paramString)
/* 1412:     */   {
/* 1413:     */     try
/* 1414:     */     {
/* 1415:1371 */       FileOutputStream localFileOutputStream = new FileOutputStream(paramString);Object localObject1 = null;
/* 1416:     */       try
/* 1417:     */       {
/* 1418:1372 */         ObjectOutputStream localObjectOutputStream = new ObjectOutputStream(localFileOutputStream);Object localObject2 = null;
/* 1419:     */         try
/* 1420:     */         {
/* 1421:1374 */           localObjectOutputStream.writeInt(blockCounter.get());
/* 1422:1375 */           localObjectOutputStream.writeObject(new HashMap(blocks));
/* 1423:     */         }
/* 1424:     */         catch (Throwable localThrowable4)
/* 1425:     */         {
/* 1426:1371 */           localObject2 = localThrowable4;throw localThrowable4;
/* 1427:     */         }
/* 1428:     */         finally {}
/* 1429:     */       }
/* 1430:     */       catch (Throwable localThrowable2)
/* 1431:     */       {
/* 1432:1371 */         localObject1 = localThrowable2;throw localThrowable2;
/* 1433:     */       }
/* 1434:     */       finally
/* 1435:     */       {
/* 1436:1376 */         if (localFileOutputStream != null) {
/* 1437:1376 */           if (localObject1 != null) {
/* 1438:     */             try
/* 1439:     */             {
/* 1440:1376 */               localFileOutputStream.close();
/* 1441:     */             }
/* 1442:     */             catch (Throwable localThrowable6)
/* 1443:     */             {
/* 1444:1376 */               localObject1.addSuppressed(localThrowable6);
/* 1445:     */             }
/* 1446:     */           } else {
/* 1447:1376 */             localFileOutputStream.close();
/* 1448:     */           }
/* 1449:     */         }
/* 1450:     */       }
/* 1451:     */     }
/* 1452:     */     catch (IOException localIOException)
/* 1453:     */     {
/* 1454:1377 */       Logger.logMessage("Error saving blocks to " + paramString, localIOException);
/* 1455:1378 */       throw new RuntimeException(localIOException);
/* 1456:     */     }
/* 1457:     */   }
/* 1458:     */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Blockchain
 * JD-Core Version:    0.7.0.1
 */