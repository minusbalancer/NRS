/*    1:     */ package nxt;
/*    2:     */ 
/*    3:     */ import java.math.BigInteger;
/*    4:     */ import java.security.MessageDigest;
/*    5:     */ import java.sql.Connection;
/*    6:     */ import java.sql.PreparedStatement;
/*    7:     */ import java.sql.ResultSet;
/*    8:     */ import java.sql.SQLException;
/*    9:     */ import java.util.Arrays;
/*   10:     */ import java.util.Collection;
/*   11:     */ import java.util.Collections;
/*   12:     */ import java.util.Comparator;
/*   13:     */ import java.util.HashMap;
/*   14:     */ import java.util.Iterator;
/*   15:     */ import java.util.LinkedList;
/*   16:     */ import java.util.Map;
/*   17:     */ import java.util.Map.Entry;
/*   18:     */ import java.util.PriorityQueue;
/*   19:     */ import java.util.Set;
/*   20:     */ import java.util.SortedMap;
/*   21:     */ import java.util.TreeMap;
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
/*   32:     */ import nxt.util.DbIterator;
/*   33:     */ import nxt.util.DbIterator.ResultSetReader;
/*   34:     */ import nxt.util.DbUtils;
/*   35:     */ import nxt.util.JSON;
/*   36:     */ import nxt.util.Logger;
/*   37:     */ import org.json.simple.JSONArray;
/*   38:     */ import org.json.simple.JSONObject;
/*   39:     */ import org.json.simple.JSONStreamAware;
/*   40:     */ 
/*   41:     */ public final class Blockchain
/*   42:     */ {
/*   43:  43 */   private static final byte[] CHECKSUM_TRANSPARENT_FORGING = { 27, -54, -59, -98, 49, -42, 48, -68, -112, 49, 41, 94, -41, 78, -84, 27, -87, -22, -28, 36, -34, -90, 112, -50, -9, 5, 89, -35, 80, -121, -128, 112 };
/*   44:     */   private static volatile Peer lastBlockchainFeeder;
/*   45:  47 */   private static final AtomicInteger blockCounter = new AtomicInteger();
/*   46:  48 */   private static final AtomicReference<Block> lastBlock = new AtomicReference();
/*   47:  50 */   private static final AtomicInteger transactionCounter = new AtomicInteger();
/*   48:  51 */   private static final ConcurrentMap<Long, Transaction> doubleSpendingTransactions = new ConcurrentHashMap();
/*   49:  52 */   private static final ConcurrentMap<Long, Transaction> unconfirmedTransactions = new ConcurrentHashMap();
/*   50:  53 */   private static final ConcurrentMap<Long, Transaction> nonBroadcastedTransactions = new ConcurrentHashMap();
/*   51:  55 */   private static final Collection<Transaction> allUnconfirmedTransactions = Collections.unmodifiableCollection(unconfirmedTransactions.values());
/*   52:  57 */   static final ConcurrentMap<String, Transaction> transactionHashes = new ConcurrentHashMap();
/*   53:  59 */   static final Runnable processTransactionsThread = new Runnable()
/*   54:     */   {
/*   55:     */     private final JSONStreamAware getUnconfirmedTransactionsRequest;
/*   56:     */     
/*   57:     */     public void run()
/*   58:     */     {
/*   59:     */       try
/*   60:     */       {
/*   61:     */         try
/*   62:     */         {
/*   63:  73 */           Peer localPeer = Peer.getAnyPeer(Peer.State.CONNECTED, true);
/*   64:  74 */           if (localPeer != null)
/*   65:     */           {
/*   66:  76 */             JSONObject localJSONObject = localPeer.send(this.getUnconfirmedTransactionsRequest);
/*   67:  77 */             if (localJSONObject != null) {
/*   68:     */               try
/*   69:     */               {
/*   70:  79 */                 Blockchain.processUnconfirmedTransactions(localJSONObject);
/*   71:     */               }
/*   72:     */               catch (NxtException.ValidationException localValidationException)
/*   73:     */               {
/*   74:  81 */                 localPeer.blacklist(localValidationException);
/*   75:     */               }
/*   76:     */             }
/*   77:     */           }
/*   78:     */         }
/*   79:     */         catch (Exception localException)
/*   80:     */         {
/*   81:  88 */           Logger.logDebugMessage("Error processing unconfirmed transactions from peer", localException);
/*   82:     */         }
/*   83:     */       }
/*   84:     */       catch (Throwable localThrowable)
/*   85:     */       {
/*   86:  91 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*   87:  92 */         localThrowable.printStackTrace();
/*   88:  93 */         System.exit(1);
/*   89:     */       }
/*   90:     */     }
/*   91:     */   };
/*   92: 100 */   static final Runnable removeUnconfirmedTransactionsThread = new Runnable()
/*   93:     */   {
/*   94:     */     public void run()
/*   95:     */     {
/*   96:     */       try
/*   97:     */       {
/*   98:     */         try
/*   99:     */         {
/*  100: 108 */           int i = Convert.getEpochTime();
/*  101: 109 */           JSONArray localJSONArray = new JSONArray();
/*  102:     */           
/*  103: 111 */           Iterator localIterator = Blockchain.unconfirmedTransactions.values().iterator();
/*  104:     */           Object localObject;
/*  105: 112 */           while (localIterator.hasNext())
/*  106:     */           {
/*  107: 114 */             localObject = (Transaction)localIterator.next();
/*  108: 115 */             if (((Transaction)localObject).getExpiration() < i)
/*  109:     */             {
/*  110: 117 */               localIterator.remove();
/*  111:     */               
/*  112: 119 */               Account localAccount = Account.getAccount(((Transaction)localObject).getSenderAccountId());
/*  113: 120 */               localAccount.addToUnconfirmedBalance((((Transaction)localObject).getAmount() + ((Transaction)localObject).getFee()) * 100L);
/*  114:     */               
/*  115: 122 */               JSONObject localJSONObject = new JSONObject();
/*  116: 123 */               localJSONObject.put("index", Integer.valueOf(((Transaction)localObject).getIndex()));
/*  117: 124 */               localJSONArray.add(localJSONObject);
/*  118:     */             }
/*  119:     */           }
/*  120: 130 */           if (localJSONArray.size() > 0)
/*  121:     */           {
/*  122: 132 */             localObject = new JSONObject();
/*  123: 133 */             ((JSONObject)localObject).put("response", "processNewData");
/*  124:     */             
/*  125: 135 */             ((JSONObject)localObject).put("removedUnconfirmedTransactions", localJSONArray);
/*  126:     */             
/*  127:     */ 
/*  128: 138 */             User.sendToAll((JSONStreamAware)localObject);
/*  129:     */           }
/*  130:     */         }
/*  131:     */         catch (Exception localException)
/*  132:     */         {
/*  133: 143 */           Logger.logDebugMessage("Error removing unconfirmed transactions", localException);
/*  134:     */         }
/*  135:     */       }
/*  136:     */       catch (Throwable localThrowable)
/*  137:     */       {
/*  138: 147 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  139: 148 */         localThrowable.printStackTrace();
/*  140: 149 */         System.exit(1);
/*  141:     */       }
/*  142:     */     }
/*  143:     */   };
/*  144: 156 */   static final Runnable getMoreBlocksThread = new Runnable()
/*  145:     */   {
/*  146:     */     private final JSONStreamAware getCumulativeDifficultyRequest;
/*  147:     */     private final JSONStreamAware getMilestoneBlockIdsRequest;
/*  148:     */     
/*  149:     */     public void run()
/*  150:     */     {
/*  151:     */       try
/*  152:     */       {
/*  153:     */         try
/*  154:     */         {
/*  155: 177 */           Peer localPeer = Peer.getAnyPeer(Peer.State.CONNECTED, true);
/*  156: 178 */           if (localPeer != null)
/*  157:     */           {
/*  158: 180 */             Blockchain.access$202(localPeer);
/*  159:     */             
/*  160: 182 */             JSONObject localJSONObject1 = localPeer.send(this.getCumulativeDifficultyRequest);
/*  161: 183 */             if (localJSONObject1 != null)
/*  162:     */             {
/*  163: 185 */               BigInteger localBigInteger1 = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty();
/*  164: 186 */               String str = (String)localJSONObject1.get("cumulativeDifficulty");
/*  165: 187 */               if (str == null) {
/*  166: 188 */                 return;
/*  167:     */               }
/*  168: 190 */               BigInteger localBigInteger2 = new BigInteger(str);
/*  169: 191 */               if (localBigInteger2.compareTo(localBigInteger1) > 0)
/*  170:     */               {
/*  171: 193 */                 localJSONObject1 = localPeer.send(this.getMilestoneBlockIdsRequest);
/*  172: 194 */                 if (localJSONObject1 != null)
/*  173:     */                 {
/*  174: 196 */                   Object localObject1 = Genesis.GENESIS_BLOCK_ID;
/*  175:     */                   
/*  176:     */ 
/*  177: 199 */                   JSONArray localJSONArray1 = (JSONArray)localJSONObject1.get("milestoneBlockIds");
/*  178: 200 */                   if (localJSONArray1 == null) {
/*  179: 201 */                     return;
/*  180:     */                   }
/*  181: 204 */                   for (Object localObject2 : localJSONArray1)
/*  182:     */                   {
/*  183: 205 */                     localObject4 = Convert.parseUnsignedLong((String)localObject2);
/*  184: 206 */                     if (Block.hasBlock((Long)localObject4))
/*  185:     */                     {
/*  186: 207 */                       localObject1 = localObject4;
/*  187: 208 */                       break;
/*  188:     */                     }
/*  189:     */                   }
/*  190:     */                   Object localObject4;
/*  191:     */                   Object localObject5;
/*  192:     */                   int j;
/*  193:     */                   int i;
/*  194:     */                   Object localObject6;
/*  195:     */                   do
/*  196:     */                   {
/*  197: 216 */                     localObject4 = new JSONObject();
/*  198: 217 */                     ((JSONObject)localObject4).put("requestType", "getNextBlockIds");
/*  199: 218 */                     ((JSONObject)localObject4).put("blockId", Convert.convert((Long)localObject1));
/*  200: 219 */                     localJSONObject1 = localPeer.send(JSON.prepareRequest((JSONObject)localObject4));
/*  201: 220 */                     if (localJSONObject1 == null) {
/*  202: 221 */                       return;
/*  203:     */                     }
/*  204: 224 */                     localObject5 = (JSONArray)localJSONObject1.get("nextBlockIds");
/*  205: 225 */                     if ((localObject5 == null) || ((j = ((JSONArray)localObject5).size()) == 0)) {
/*  206: 226 */                       return;
/*  207:     */                     }
/*  208: 230 */                     for (i = 0; i < j; i++)
/*  209:     */                     {
/*  210: 231 */                       localObject6 = Convert.parseUnsignedLong((String)((JSONArray)localObject5).get(i));
/*  211: 232 */                       if (!Block.hasBlock((Long)localObject6)) {
/*  212:     */                         break;
/*  213:     */                       }
/*  214: 235 */                       localObject1 = localObject6;
/*  215:     */                     }
/*  216: 238 */                   } while (i == j);
/*  217: 241 */                   Block localBlock1 = Block.findBlock((Long)localObject1);
/*  218: 242 */                   if (((Block)Blockchain.lastBlock.get()).getHeight() - localBlock1.getHeight() < 720)
/*  219:     */                   {
/*  220: 244 */                     Object localObject3 = localObject1;
/*  221: 245 */                     localObject4 = new LinkedList();
/*  222: 246 */                     localObject5 = new HashMap();
/*  223:     */                     Object localObject7;
/*  224:     */                     for (;;)
/*  225:     */                     {
/*  226: 250 */                       localObject6 = new JSONObject();
/*  227: 251 */                       ((JSONObject)localObject6).put("requestType", "getNextBlocks");
/*  228: 252 */                       ((JSONObject)localObject6).put("blockId", Convert.convert((Long)localObject3));
/*  229: 253 */                       localJSONObject1 = localPeer.send(JSON.prepareRequest((JSONObject)localObject6));
/*  230: 254 */                       if (localJSONObject1 == null) {
/*  231:     */                         break;
/*  232:     */                       }
/*  233: 258 */                       JSONArray localJSONArray2 = (JSONArray)localJSONObject1.get("nextBlocks");
/*  234: 259 */                       if ((localJSONArray2 == null) || (localJSONArray2.size() == 0)) {
/*  235:     */                         break;
/*  236:     */                       }
/*  237: 263 */                       synchronized (Blockchain.class)
/*  238:     */                       {
/*  239: 265 */                         for (localObject7 = localJSONArray2.iterator(); ((Iterator)localObject7).hasNext();)
/*  240:     */                         {
/*  241: 265 */                           Object localObject8 = ((Iterator)localObject7).next();
/*  242: 266 */                           JSONObject localJSONObject2 = (JSONObject)localObject8;
/*  243:     */                           Block localBlock2;
/*  244:     */                           try
/*  245:     */                           {
/*  246: 269 */                             localBlock2 = Block.getBlock(localJSONObject2);
/*  247:     */                           }
/*  248:     */                           catch (NxtException.ValidationException localValidationException1)
/*  249:     */                           {
/*  250: 271 */                             localPeer.blacklist(localValidationException1);
/*  251: 272 */                             return;
/*  252:     */                           }
/*  253: 274 */                           localObject3 = localBlock2.getId();
/*  254:     */                           JSONArray localJSONArray3;
/*  255: 276 */                           if (((Block)Blockchain.lastBlock.get()).getId().equals(localBlock2.getPreviousBlockId()))
/*  256:     */                           {
/*  257: 278 */                             localJSONArray3 = (JSONArray)localJSONObject2.get("transactions");
/*  258:     */                             try
/*  259:     */                             {
/*  260: 280 */                               Transaction[] arrayOfTransaction = new Transaction[localJSONArray3.size()];
/*  261: 281 */                               for (int n = 0; n < arrayOfTransaction.length; n++) {
/*  262: 282 */                                 arrayOfTransaction[n] = Transaction.getTransaction((JSONObject)localJSONArray3.get(n));
/*  263:     */                               }
/*  264:     */                               try
/*  265:     */                               {
/*  266: 285 */                                 Blockchain.pushBlock(localBlock2, arrayOfTransaction);
/*  267:     */                               }
/*  268:     */                               catch (Blockchain.BlockNotAcceptedException localBlockNotAcceptedException2)
/*  269:     */                               {
/*  270: 287 */                                 Logger.logDebugMessage("Failed to accept block " + localBlock2.getStringId() + " at height " + ((Block)Blockchain.lastBlock.get()).getHeight() + " received from " + localPeer.getPeerAddress() + ", blacklisting");
/*  271:     */                                 
/*  272:     */ 
/*  273: 290 */                                 localPeer.blacklist(localBlockNotAcceptedException2);
/*  274: 291 */                                 return;
/*  275:     */                               }
/*  276:     */                             }
/*  277:     */                             catch (NxtException.ValidationException localValidationException2)
/*  278:     */                             {
/*  279: 294 */                               localPeer.blacklist(localValidationException2);
/*  280: 295 */                               return;
/*  281:     */                             }
/*  282:     */                           }
/*  283: 298 */                           else if ((!Block.hasBlock(localBlock2.getId())) && (localBlock2.transactionIds.length <= 255))
/*  284:     */                           {
/*  285: 300 */                             ((LinkedList)localObject4).add(localBlock2);
/*  286:     */                             
/*  287: 302 */                             localJSONArray3 = (JSONArray)localJSONObject2.get("transactions");
/*  288:     */                             try
/*  289:     */                             {
/*  290: 304 */                               for (int m = 0; m < localBlock2.transactionIds.length; m++)
/*  291:     */                               {
/*  292: 306 */                                 Transaction localTransaction = Transaction.getTransaction((JSONObject)localJSONArray3.get(m));
/*  293: 307 */                                 localBlock2.transactionIds[m] = localTransaction.getId();
/*  294: 308 */                                 localBlock2.blockTransactions[m] = localTransaction;
/*  295: 309 */                                 ((HashMap)localObject5).put(localBlock2.transactionIds[m], localTransaction);
/*  296:     */                               }
/*  297:     */                             }
/*  298:     */                             catch (NxtException.ValidationException localValidationException3)
/*  299:     */                             {
/*  300: 313 */                               localPeer.blacklist(localValidationException3);
/*  301: 314 */                               return;
/*  302:     */                             }
/*  303:     */                           }
/*  304:     */                         }
/*  305:     */                       }
/*  306:     */                     }
/*  307: 324 */                     if ((!((LinkedList)localObject4).isEmpty()) && (((Block)Blockchain.lastBlock.get()).getHeight() - localBlock1.getHeight() < 720)) {
/*  308: 326 */                       synchronized (Blockchain.class)
/*  309:     */                       {
/*  310: 327 */                         localBigInteger1 = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty();
/*  311:     */                         for (;;)
/*  312:     */                         {
/*  313:     */                           int k;
/*  314:     */                           try
/*  315:     */                           {
/*  316: 331 */                             while ((!((Block)Blockchain.lastBlock.get()).getId().equals(localObject1)) && (Blockchain.access$500())) {}
/*  317: 333 */                             if (((Block)Blockchain.lastBlock.get()).getId().equals(localObject1)) {
/*  318: 334 */                               for (??? = ((LinkedList)localObject4).iterator(); ((Iterator)???).hasNext();)
/*  319:     */                               {
/*  320: 334 */                                 localObject7 = (Block)((Iterator)???).next();
/*  321: 335 */                                 if (((Block)Blockchain.lastBlock.get()).getId().equals(((Block)localObject7).getPreviousBlockId())) {
/*  322:     */                                   try
/*  323:     */                                   {
/*  324: 337 */                                     Blockchain.pushBlock((Block)localObject7, ((Block)localObject7).blockTransactions);
/*  325:     */                                   }
/*  326:     */                                   catch (Blockchain.BlockNotAcceptedException localBlockNotAcceptedException1)
/*  327:     */                                   {
/*  328: 339 */                                     Logger.logDebugMessage("Failed to push future block " + ((Block)localObject7).getStringId() + " received from " + localPeer.getPeerAddress() + ", blacklisting");
/*  329:     */                                     
/*  330: 341 */                                     localPeer.blacklist(localBlockNotAcceptedException1);
/*  331: 342 */                                     break;
/*  332:     */                                   }
/*  333:     */                                 }
/*  334:     */                               }
/*  335:     */                             }
/*  336: 348 */                             k = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty().compareTo(localBigInteger1) < 0 ? 1 : 0;
/*  337: 349 */                             if (k != 0)
/*  338:     */                             {
/*  339: 350 */                               Logger.logDebugMessage("Rescan caused by peer " + localPeer.getPeerAddress() + ", blacklisting");
/*  340: 351 */                               localPeer.blacklist();
/*  341:     */                             }
/*  342:     */                           }
/*  343:     */                           catch (Transaction.UndoNotSupportedException localUndoNotSupportedException)
/*  344:     */                           {
/*  345: 354 */                             Logger.logDebugMessage(localUndoNotSupportedException.getMessage());
/*  346: 355 */                             Logger.logDebugMessage("Popping off last block not possible, will do a rescan");
/*  347: 356 */                             k = 1;
/*  348:     */                           }
/*  349:     */                         }
/*  350: 359 */                         if (k != 0)
/*  351:     */                         {
/*  352: 361 */                           if (localBlock1.getNextBlockId() != null) {
/*  353: 362 */                             Block.deleteBlock(localBlock1.getNextBlockId());
/*  354:     */                           }
/*  355: 364 */                           Logger.logMessage("Re-scanning blockchain...");
/*  356: 365 */                           Blockchain.access$600();
/*  357: 366 */                           Logger.logMessage("...Done");
/*  358:     */                         }
/*  359:     */                       }
/*  360:     */                     }
/*  361:     */                   }
/*  362:     */                 }
/*  363:     */               }
/*  364:     */             }
/*  365:     */           }
/*  366:     */         }
/*  367:     */         catch (Exception localException)
/*  368:     */         {
/*  369: 377 */           Logger.logDebugMessage("Error in milestone blocks processing thread", localException);
/*  370:     */         }
/*  371:     */       }
/*  372:     */       catch (Throwable localThrowable)
/*  373:     */       {
/*  374: 380 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  375: 381 */         localThrowable.printStackTrace();
/*  376: 382 */         System.exit(1);
/*  377:     */       }
/*  378:     */     }
/*  379:     */   };
/*  380: 389 */   static final Runnable generateBlockThread = new Runnable()
/*  381:     */   {
/*  382: 391 */     private final ConcurrentMap<Account, Block> lastBlocks = new ConcurrentHashMap();
/*  383: 392 */     private final ConcurrentMap<Account, BigInteger> hits = new ConcurrentHashMap();
/*  384:     */     
/*  385:     */     public void run()
/*  386:     */     {
/*  387:     */       try
/*  388:     */       {
/*  389:     */         try
/*  390:     */         {
/*  391: 400 */           HashMap localHashMap = new HashMap();
/*  392: 401 */           for (localIterator = User.getAllUsers().iterator(); localIterator.hasNext();)
/*  393:     */           {
/*  394: 401 */             localObject1 = (User)localIterator.next();
/*  395: 402 */             if (((User)localObject1).getSecretPhrase() != null)
/*  396:     */             {
/*  397: 403 */               localAccount = Account.getAccount(((User)localObject1).getPublicKey());
/*  398: 404 */               if ((localAccount != null) && (localAccount.getEffectiveBalance() > 0)) {
/*  399: 405 */                 localHashMap.put(localAccount, localObject1);
/*  400:     */               }
/*  401:     */             }
/*  402:     */           }
/*  403: 410 */           for (localIterator = localHashMap.entrySet().iterator(); localIterator.hasNext();)
/*  404:     */           {
/*  405: 410 */             localObject1 = (Map.Entry)localIterator.next();
/*  406:     */             
/*  407: 412 */             localAccount = (Account)((Map.Entry)localObject1).getKey();
/*  408: 413 */             User localUser = (User)((Map.Entry)localObject1).getValue();
/*  409: 414 */             Block localBlock = (Block)Blockchain.lastBlock.get();
/*  410: 415 */             if (this.lastBlocks.get(localAccount) != localBlock)
/*  411:     */             {
/*  412: 417 */               long l = localAccount.getEffectiveBalance();
/*  413: 418 */               if (l > 0L)
/*  414:     */               {
/*  415: 421 */                 MessageDigest localMessageDigest = Crypto.sha256();
/*  416:     */                 byte[] arrayOfByte;
/*  417: 423 */                 if (localBlock.getHeight() < 30000)
/*  418:     */                 {
/*  419: 425 */                   localObject2 = Crypto.sign(localBlock.getGenerationSignature(), localUser.getSecretPhrase());
/*  420: 426 */                   arrayOfByte = localMessageDigest.digest((byte[])localObject2);
/*  421:     */                 }
/*  422:     */                 else
/*  423:     */                 {
/*  424: 430 */                   localMessageDigest.update(localBlock.getGenerationSignature());
/*  425: 431 */                   arrayOfByte = localMessageDigest.digest(localUser.getPublicKey());
/*  426:     */                 }
/*  427: 434 */                 Object localObject2 = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
/*  428:     */                 
/*  429: 436 */                 this.lastBlocks.put(localAccount, localBlock);
/*  430: 437 */                 this.hits.put(localAccount, localObject2);
/*  431:     */                 
/*  432: 439 */                 JSONObject localJSONObject = new JSONObject();
/*  433: 440 */                 localJSONObject.put("response", "setBlockGenerationDeadline");
/*  434: 441 */                 localJSONObject.put("deadline", Long.valueOf(((BigInteger)localObject2).divide(BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(l))).longValue() - (Convert.getEpochTime() - localBlock.getTimestamp())));
/*  435:     */                 
/*  436: 443 */                 localUser.send(localJSONObject);
/*  437:     */               }
/*  438:     */             }
/*  439:     */             else
/*  440:     */             {
/*  441: 447 */               int i = Convert.getEpochTime() - localBlock.getTimestamp();
/*  442: 448 */               if (i > 0)
/*  443:     */               {
/*  444: 450 */                 BigInteger localBigInteger = BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance())).multiply(BigInteger.valueOf(i));
/*  445: 451 */                 if (((BigInteger)this.hits.get(localAccount)).compareTo(localBigInteger) < 0) {
/*  446: 453 */                   Blockchain.generateBlock(localUser.getSecretPhrase());
/*  447:     */                 }
/*  448:     */               }
/*  449:     */             }
/*  450:     */           }
/*  451:     */         }
/*  452:     */         catch (Exception localException)
/*  453:     */         {
/*  454:     */           Iterator localIterator;
/*  455:     */           Object localObject1;
/*  456:     */           Account localAccount;
/*  457: 462 */           Logger.logDebugMessage("Error in block generation thread", localException);
/*  458:     */         }
/*  459:     */       }
/*  460:     */       catch (Throwable localThrowable)
/*  461:     */       {
/*  462: 465 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  463: 466 */         localThrowable.printStackTrace();
/*  464: 467 */         System.exit(1);
/*  465:     */       }
/*  466:     */     }
/*  467:     */   };
/*  468: 474 */   static final Runnable rebroadcastTransactionsThread = new Runnable()
/*  469:     */   {
/*  470:     */     public void run()
/*  471:     */     {
/*  472:     */       try
/*  473:     */       {
/*  474:     */         try
/*  475:     */         {
/*  476: 481 */           JSONArray localJSONArray = new JSONArray();
/*  477: 483 */           for (Object localObject = Blockchain.nonBroadcastedTransactions.values().iterator(); ((Iterator)localObject).hasNext();)
/*  478:     */           {
/*  479: 483 */             Transaction localTransaction = (Transaction)((Iterator)localObject).next();
/*  480: 485 */             if ((Blockchain.unconfirmedTransactions.get(localTransaction.getId()) == null) && (!Transaction.hasTransaction(localTransaction.getId()))) {
/*  481: 487 */               localJSONArray.add(localTransaction.getJSONObject());
/*  482:     */             } else {
/*  483: 491 */               Blockchain.nonBroadcastedTransactions.remove(localTransaction.getId());
/*  484:     */             }
/*  485:     */           }
/*  486: 497 */           if (localJSONArray.size() > 0)
/*  487:     */           {
/*  488: 499 */             localObject = new JSONObject();
/*  489: 500 */             ((JSONObject)localObject).put("requestType", "processTransactions");
/*  490: 501 */             ((JSONObject)localObject).put("transactions", localJSONArray);
/*  491:     */             
/*  492: 503 */             Peer.sendToSomePeers((JSONObject)localObject);
/*  493:     */           }
/*  494:     */         }
/*  495:     */         catch (Exception localException)
/*  496:     */         {
/*  497: 508 */           Logger.logDebugMessage("Error in transaction re-broadcasting thread", localException);
/*  498:     */         }
/*  499:     */       }
/*  500:     */       catch (Throwable localThrowable)
/*  501:     */       {
/*  502: 511 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  503: 512 */         localThrowable.printStackTrace();
/*  504: 513 */         System.exit(1);
/*  505:     */       }
/*  506:     */     }
/*  507:     */   };
/*  508:     */   
/*  509:     */   public static DbIterator<Block> getAllBlocks()
/*  510:     */   {
/*  511: 521 */     Connection localConnection = null;
/*  512:     */     try
/*  513:     */     {
/*  514: 523 */       localConnection = Db.getConnection();
/*  515: 524 */       PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM block ORDER BY db_id ASC");
/*  516: 525 */       new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
/*  517:     */       {
/*  518:     */         public Block get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
/*  519:     */           throws NxtException.ValidationException
/*  520:     */         {
/*  521: 528 */           return Block.getBlock(paramAnonymousConnection, paramAnonymousResultSet);
/*  522:     */         }
/*  523:     */       });
/*  524:     */     }
/*  525:     */     catch (SQLException localSQLException)
/*  526:     */     {
/*  527: 532 */       DbUtils.close(new AutoCloseable[] { localConnection });
/*  528: 533 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/*  529:     */     }
/*  530:     */   }
/*  531:     */   
/*  532:     */   public static DbIterator<Block> getAllBlocks(Account paramAccount, int paramInt)
/*  533:     */   {
/*  534: 538 */     Connection localConnection = null;
/*  535:     */     try
/*  536:     */     {
/*  537: 540 */       localConnection = Db.getConnection();
/*  538: 541 */       PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM block WHERE timestamp >= ? AND generator_public_key = ? ORDER BY db_id ASC");
/*  539: 542 */       localPreparedStatement.setInt(1, paramInt);
/*  540: 543 */       localPreparedStatement.setBytes(2, paramAccount.getPublicKey());
/*  541: 544 */       new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
/*  542:     */       {
/*  543:     */         public Block get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
/*  544:     */           throws NxtException.ValidationException
/*  545:     */         {
/*  546: 547 */           return Block.getBlock(paramAnonymousConnection, paramAnonymousResultSet);
/*  547:     */         }
/*  548:     */       });
/*  549:     */     }
/*  550:     */     catch (SQLException localSQLException)
/*  551:     */     {
/*  552: 551 */       DbUtils.close(new AutoCloseable[] { localConnection });
/*  553: 552 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/*  554:     */     }
/*  555:     */   }
/*  556:     */   
/*  557:     */   /* Error */
/*  558:     */   public static int getBlockCount()
/*  559:     */   {
/*  560:     */     // Byte code:
/*  561:     */     //   0: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
/*  562:     */     //   3: astore_0
/*  563:     */     //   4: aconst_null
/*  564:     */     //   5: astore_1
/*  565:     */     //   6: aload_0
/*  566:     */     //   7: ldc 29
/*  567:     */     //   9: invokeinterface 12 2 0
/*  568:     */     //   14: astore_2
/*  569:     */     //   15: aconst_null
/*  570:     */     //   16: astore_3
/*  571:     */     //   17: aload_2
/*  572:     */     //   18: invokeinterface 30 1 0
/*  573:     */     //   23: astore 4
/*  574:     */     //   25: aload 4
/*  575:     */     //   27: invokeinterface 31 1 0
/*  576:     */     //   32: pop
/*  577:     */     //   33: aload 4
/*  578:     */     //   35: iconst_1
/*  579:     */     //   36: invokeinterface 32 2 0
/*  580:     */     //   41: istore 5
/*  581:     */     //   43: aload_2
/*  582:     */     //   44: ifnull +33 -> 77
/*  583:     */     //   47: aload_3
/*  584:     */     //   48: ifnull +23 -> 71
/*  585:     */     //   51: aload_2
/*  586:     */     //   52: invokeinterface 33 1 0
/*  587:     */     //   57: goto +20 -> 77
/*  588:     */     //   60: astore 6
/*  589:     */     //   62: aload_3
/*  590:     */     //   63: aload 6
/*  591:     */     //   65: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  592:     */     //   68: goto +9 -> 77
/*  593:     */     //   71: aload_2
/*  594:     */     //   72: invokeinterface 33 1 0
/*  595:     */     //   77: aload_0
/*  596:     */     //   78: ifnull +33 -> 111
/*  597:     */     //   81: aload_1
/*  598:     */     //   82: ifnull +23 -> 105
/*  599:     */     //   85: aload_0
/*  600:     */     //   86: invokeinterface 36 1 0
/*  601:     */     //   91: goto +20 -> 111
/*  602:     */     //   94: astore 6
/*  603:     */     //   96: aload_1
/*  604:     */     //   97: aload 6
/*  605:     */     //   99: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  606:     */     //   102: goto +9 -> 111
/*  607:     */     //   105: aload_0
/*  608:     */     //   106: invokeinterface 36 1 0
/*  609:     */     //   111: iload 5
/*  610:     */     //   113: ireturn
/*  611:     */     //   114: astore 4
/*  612:     */     //   116: aload 4
/*  613:     */     //   118: astore_3
/*  614:     */     //   119: aload 4
/*  615:     */     //   121: athrow
/*  616:     */     //   122: astore 7
/*  617:     */     //   124: aload_2
/*  618:     */     //   125: ifnull +33 -> 158
/*  619:     */     //   128: aload_3
/*  620:     */     //   129: ifnull +23 -> 152
/*  621:     */     //   132: aload_2
/*  622:     */     //   133: invokeinterface 33 1 0
/*  623:     */     //   138: goto +20 -> 158
/*  624:     */     //   141: astore 8
/*  625:     */     //   143: aload_3
/*  626:     */     //   144: aload 8
/*  627:     */     //   146: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  628:     */     //   149: goto +9 -> 158
/*  629:     */     //   152: aload_2
/*  630:     */     //   153: invokeinterface 33 1 0
/*  631:     */     //   158: aload 7
/*  632:     */     //   160: athrow
/*  633:     */     //   161: astore_2
/*  634:     */     //   162: aload_2
/*  635:     */     //   163: astore_1
/*  636:     */     //   164: aload_2
/*  637:     */     //   165: athrow
/*  638:     */     //   166: astore 9
/*  639:     */     //   168: aload_0
/*  640:     */     //   169: ifnull +33 -> 202
/*  641:     */     //   172: aload_1
/*  642:     */     //   173: ifnull +23 -> 196
/*  643:     */     //   176: aload_0
/*  644:     */     //   177: invokeinterface 36 1 0
/*  645:     */     //   182: goto +20 -> 202
/*  646:     */     //   185: astore 10
/*  647:     */     //   187: aload_1
/*  648:     */     //   188: aload 10
/*  649:     */     //   190: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  650:     */     //   193: goto +9 -> 202
/*  651:     */     //   196: aload_0
/*  652:     */     //   197: invokeinterface 36 1 0
/*  653:     */     //   202: aload 9
/*  654:     */     //   204: athrow
/*  655:     */     //   205: astore_0
/*  656:     */     //   206: new 20	java/lang/RuntimeException
/*  657:     */     //   209: dup
/*  658:     */     //   210: aload_0
/*  659:     */     //   211: invokevirtual 21	java/sql/SQLException:toString	()Ljava/lang/String;
/*  660:     */     //   214: aload_0
/*  661:     */     //   215: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/*  662:     */     //   218: athrow
/*  663:     */     // Line number table:
/*  664:     */     //   Java source line #557	-> byte code offset #0
/*  665:     */     //   Java source line #558	-> byte code offset #17
/*  666:     */     //   Java source line #559	-> byte code offset #25
/*  667:     */     //   Java source line #560	-> byte code offset #33
/*  668:     */     //   Java source line #561	-> byte code offset #43
/*  669:     */     //   Java source line #557	-> byte code offset #114
/*  670:     */     //   Java source line #561	-> byte code offset #122
/*  671:     */     //   Java source line #557	-> byte code offset #161
/*  672:     */     //   Java source line #561	-> byte code offset #166
/*  673:     */     //   Java source line #562	-> byte code offset #206
/*  674:     */     // Local variable table:
/*  675:     */     //   start	length	slot	name	signature
/*  676:     */     //   3	194	0	localConnection	Connection
/*  677:     */     //   205	10	0	localSQLException	SQLException
/*  678:     */     //   5	183	1	localObject1	Object
/*  679:     */     //   14	139	2	localPreparedStatement	PreparedStatement
/*  680:     */     //   161	4	2	localThrowable1	Throwable
/*  681:     */     //   16	128	3	localObject2	Object
/*  682:     */     //   23	11	4	localResultSet	ResultSet
/*  683:     */     //   114	6	4	localThrowable2	Throwable
/*  684:     */     //   60	4	6	localThrowable3	Throwable
/*  685:     */     //   94	4	6	localThrowable4	Throwable
/*  686:     */     //   122	37	7	localObject3	Object
/*  687:     */     //   141	4	8	localThrowable5	Throwable
/*  688:     */     //   166	37	9	localObject4	Object
/*  689:     */     //   185	4	10	localThrowable6	Throwable
/*  690:     */     // Exception table:
/*  691:     */     //   from	to	target	type
/*  692:     */     //   51	57	60	java/lang/Throwable
/*  693:     */     //   85	91	94	java/lang/Throwable
/*  694:     */     //   17	43	114	java/lang/Throwable
/*  695:     */     //   17	43	122	finally
/*  696:     */     //   114	124	122	finally
/*  697:     */     //   132	138	141	java/lang/Throwable
/*  698:     */     //   6	77	161	java/lang/Throwable
/*  699:     */     //   114	161	161	java/lang/Throwable
/*  700:     */     //   6	77	166	finally
/*  701:     */     //   114	168	166	finally
/*  702:     */     //   176	182	185	java/lang/Throwable
/*  703:     */     //   0	111	205	java/sql/SQLException
/*  704:     */     //   114	205	205	java/sql/SQLException
/*  705:     */   }
/*  706:     */   
/*  707:     */   public static DbIterator<Transaction> getAllTransactions()
/*  708:     */   {
/*  709: 567 */     Connection localConnection = null;
/*  710:     */     try
/*  711:     */     {
/*  712: 569 */       localConnection = Db.getConnection();
/*  713: 570 */       PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction ORDER BY db_id ASC");
/*  714: 571 */       new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
/*  715:     */       {
/*  716:     */         public Transaction get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
/*  717:     */           throws NxtException.ValidationException
/*  718:     */         {
/*  719: 574 */           return Transaction.getTransaction(paramAnonymousConnection, paramAnonymousResultSet);
/*  720:     */         }
/*  721:     */       });
/*  722:     */     }
/*  723:     */     catch (SQLException localSQLException)
/*  724:     */     {
/*  725: 578 */       DbUtils.close(new AutoCloseable[] { localConnection });
/*  726: 579 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/*  727:     */     }
/*  728:     */   }
/*  729:     */   
/*  730:     */   public static DbIterator<Transaction> getAllTransactions(Account paramAccount, byte paramByte1, byte paramByte2, int paramInt)
/*  731:     */   {
/*  732: 584 */     Connection localConnection = null;
/*  733:     */     try
/*  734:     */     {
/*  735: 586 */       localConnection = Db.getConnection();
/*  736:     */       PreparedStatement localPreparedStatement;
/*  737: 588 */       if (paramByte1 >= 0)
/*  738:     */       {
/*  739: 589 */         if (paramByte2 >= 0)
/*  740:     */         {
/*  741: 590 */           localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_account_id = ?) AND type = ? AND subtype = ? ORDER BY timestamp ASC");
/*  742: 591 */           localPreparedStatement.setInt(1, paramInt);
/*  743: 592 */           localPreparedStatement.setLong(2, paramAccount.getId().longValue());
/*  744: 593 */           localPreparedStatement.setLong(3, paramAccount.getId().longValue());
/*  745: 594 */           localPreparedStatement.setByte(4, paramByte1);
/*  746: 595 */           localPreparedStatement.setByte(5, paramByte2);
/*  747:     */         }
/*  748:     */         else
/*  749:     */         {
/*  750: 597 */           localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_account_id = ?) AND type = ? ORDER BY timestamp ASC");
/*  751: 598 */           localPreparedStatement.setInt(1, paramInt);
/*  752: 599 */           localPreparedStatement.setLong(2, paramAccount.getId().longValue());
/*  753: 600 */           localPreparedStatement.setLong(3, paramAccount.getId().longValue());
/*  754: 601 */           localPreparedStatement.setByte(4, paramByte1);
/*  755:     */         }
/*  756:     */       }
/*  757:     */       else
/*  758:     */       {
/*  759: 604 */         localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_account_id = ?) ORDER BY timestamp ASC");
/*  760: 605 */         localPreparedStatement.setInt(1, paramInt);
/*  761: 606 */         localPreparedStatement.setLong(2, paramAccount.getId().longValue());
/*  762: 607 */         localPreparedStatement.setLong(3, paramAccount.getId().longValue());
/*  763:     */       }
/*  764: 609 */       new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
/*  765:     */       {
/*  766:     */         public Transaction get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
/*  767:     */           throws NxtException.ValidationException
/*  768:     */         {
/*  769: 612 */           return Transaction.getTransaction(paramAnonymousConnection, paramAnonymousResultSet);
/*  770:     */         }
/*  771:     */       });
/*  772:     */     }
/*  773:     */     catch (SQLException localSQLException)
/*  774:     */     {
/*  775: 616 */       DbUtils.close(new AutoCloseable[] { localConnection });
/*  776: 617 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/*  777:     */     }
/*  778:     */   }
/*  779:     */   
/*  780:     */   /* Error */
/*  781:     */   public static int getTransactionCount()
/*  782:     */   {
/*  783:     */     // Byte code:
/*  784:     */     //   0: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
/*  785:     */     //   3: astore_0
/*  786:     */     //   4: aconst_null
/*  787:     */     //   5: astore_1
/*  788:     */     //   6: aload_0
/*  789:     */     //   7: ldc 49
/*  790:     */     //   9: invokeinterface 12 2 0
/*  791:     */     //   14: astore_2
/*  792:     */     //   15: aconst_null
/*  793:     */     //   16: astore_3
/*  794:     */     //   17: aload_2
/*  795:     */     //   18: invokeinterface 30 1 0
/*  796:     */     //   23: astore 4
/*  797:     */     //   25: aload 4
/*  798:     */     //   27: invokeinterface 31 1 0
/*  799:     */     //   32: pop
/*  800:     */     //   33: aload 4
/*  801:     */     //   35: iconst_1
/*  802:     */     //   36: invokeinterface 32 2 0
/*  803:     */     //   41: istore 5
/*  804:     */     //   43: aload_2
/*  805:     */     //   44: ifnull +33 -> 77
/*  806:     */     //   47: aload_3
/*  807:     */     //   48: ifnull +23 -> 71
/*  808:     */     //   51: aload_2
/*  809:     */     //   52: invokeinterface 33 1 0
/*  810:     */     //   57: goto +20 -> 77
/*  811:     */     //   60: astore 6
/*  812:     */     //   62: aload_3
/*  813:     */     //   63: aload 6
/*  814:     */     //   65: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  815:     */     //   68: goto +9 -> 77
/*  816:     */     //   71: aload_2
/*  817:     */     //   72: invokeinterface 33 1 0
/*  818:     */     //   77: aload_0
/*  819:     */     //   78: ifnull +33 -> 111
/*  820:     */     //   81: aload_1
/*  821:     */     //   82: ifnull +23 -> 105
/*  822:     */     //   85: aload_0
/*  823:     */     //   86: invokeinterface 36 1 0
/*  824:     */     //   91: goto +20 -> 111
/*  825:     */     //   94: astore 6
/*  826:     */     //   96: aload_1
/*  827:     */     //   97: aload 6
/*  828:     */     //   99: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  829:     */     //   102: goto +9 -> 111
/*  830:     */     //   105: aload_0
/*  831:     */     //   106: invokeinterface 36 1 0
/*  832:     */     //   111: iload 5
/*  833:     */     //   113: ireturn
/*  834:     */     //   114: astore 4
/*  835:     */     //   116: aload 4
/*  836:     */     //   118: astore_3
/*  837:     */     //   119: aload 4
/*  838:     */     //   121: athrow
/*  839:     */     //   122: astore 7
/*  840:     */     //   124: aload_2
/*  841:     */     //   125: ifnull +33 -> 158
/*  842:     */     //   128: aload_3
/*  843:     */     //   129: ifnull +23 -> 152
/*  844:     */     //   132: aload_2
/*  845:     */     //   133: invokeinterface 33 1 0
/*  846:     */     //   138: goto +20 -> 158
/*  847:     */     //   141: astore 8
/*  848:     */     //   143: aload_3
/*  849:     */     //   144: aload 8
/*  850:     */     //   146: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  851:     */     //   149: goto +9 -> 158
/*  852:     */     //   152: aload_2
/*  853:     */     //   153: invokeinterface 33 1 0
/*  854:     */     //   158: aload 7
/*  855:     */     //   160: athrow
/*  856:     */     //   161: astore_2
/*  857:     */     //   162: aload_2
/*  858:     */     //   163: astore_1
/*  859:     */     //   164: aload_2
/*  860:     */     //   165: athrow
/*  861:     */     //   166: astore 9
/*  862:     */     //   168: aload_0
/*  863:     */     //   169: ifnull +33 -> 202
/*  864:     */     //   172: aload_1
/*  865:     */     //   173: ifnull +23 -> 196
/*  866:     */     //   176: aload_0
/*  867:     */     //   177: invokeinterface 36 1 0
/*  868:     */     //   182: goto +20 -> 202
/*  869:     */     //   185: astore 10
/*  870:     */     //   187: aload_1
/*  871:     */     //   188: aload 10
/*  872:     */     //   190: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  873:     */     //   193: goto +9 -> 202
/*  874:     */     //   196: aload_0
/*  875:     */     //   197: invokeinterface 36 1 0
/*  876:     */     //   202: aload 9
/*  877:     */     //   204: athrow
/*  878:     */     //   205: astore_0
/*  879:     */     //   206: new 20	java/lang/RuntimeException
/*  880:     */     //   209: dup
/*  881:     */     //   210: aload_0
/*  882:     */     //   211: invokevirtual 21	java/sql/SQLException:toString	()Ljava/lang/String;
/*  883:     */     //   214: aload_0
/*  884:     */     //   215: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/*  885:     */     //   218: athrow
/*  886:     */     // Line number table:
/*  887:     */     //   Java source line #622	-> byte code offset #0
/*  888:     */     //   Java source line #623	-> byte code offset #17
/*  889:     */     //   Java source line #624	-> byte code offset #25
/*  890:     */     //   Java source line #625	-> byte code offset #33
/*  891:     */     //   Java source line #626	-> byte code offset #43
/*  892:     */     //   Java source line #622	-> byte code offset #114
/*  893:     */     //   Java source line #626	-> byte code offset #122
/*  894:     */     //   Java source line #622	-> byte code offset #161
/*  895:     */     //   Java source line #626	-> byte code offset #166
/*  896:     */     //   Java source line #627	-> byte code offset #206
/*  897:     */     // Local variable table:
/*  898:     */     //   start	length	slot	name	signature
/*  899:     */     //   3	194	0	localConnection	Connection
/*  900:     */     //   205	10	0	localSQLException	SQLException
/*  901:     */     //   5	183	1	localObject1	Object
/*  902:     */     //   14	139	2	localPreparedStatement	PreparedStatement
/*  903:     */     //   161	4	2	localThrowable1	Throwable
/*  904:     */     //   16	128	3	localObject2	Object
/*  905:     */     //   23	11	4	localResultSet	ResultSet
/*  906:     */     //   114	6	4	localThrowable2	Throwable
/*  907:     */     //   60	4	6	localThrowable3	Throwable
/*  908:     */     //   94	4	6	localThrowable4	Throwable
/*  909:     */     //   122	37	7	localObject3	Object
/*  910:     */     //   141	4	8	localThrowable5	Throwable
/*  911:     */     //   166	37	9	localObject4	Object
/*  912:     */     //   185	4	10	localThrowable6	Throwable
/*  913:     */     // Exception table:
/*  914:     */     //   from	to	target	type
/*  915:     */     //   51	57	60	java/lang/Throwable
/*  916:     */     //   85	91	94	java/lang/Throwable
/*  917:     */     //   17	43	114	java/lang/Throwable
/*  918:     */     //   17	43	122	finally
/*  919:     */     //   114	124	122	finally
/*  920:     */     //   132	138	141	java/lang/Throwable
/*  921:     */     //   6	77	161	java/lang/Throwable
/*  922:     */     //   114	161	161	java/lang/Throwable
/*  923:     */     //   6	77	166	finally
/*  924:     */     //   114	168	166	finally
/*  925:     */     //   176	182	185	java/lang/Throwable
/*  926:     */     //   0	111	205	java/sql/SQLException
/*  927:     */     //   114	205	205	java/sql/SQLException
/*  928:     */   }
/*  929:     */   
/*  930:     */   /* Error */
/*  931:     */   public static java.util.List<Long> getBlockIdsAfter(Long paramLong, int paramInt)
/*  932:     */   {
/*  933:     */     // Byte code:
/*  934:     */     //   0: iload_1
/*  935:     */     //   1: sipush 1440
/*  936:     */     //   4: if_icmple +13 -> 17
/*  937:     */     //   7: new 50	java/lang/IllegalArgumentException
/*  938:     */     //   10: dup
/*  939:     */     //   11: ldc 51
/*  940:     */     //   13: invokespecial 52	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
/*  941:     */     //   16: athrow
/*  942:     */     //   17: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
/*  943:     */     //   20: astore_2
/*  944:     */     //   21: aconst_null
/*  945:     */     //   22: astore_3
/*  946:     */     //   23: aload_2
/*  947:     */     //   24: ldc 53
/*  948:     */     //   26: invokeinterface 12 2 0
/*  949:     */     //   31: astore 4
/*  950:     */     //   33: aconst_null
/*  951:     */     //   34: astore 5
/*  952:     */     //   36: aload_2
/*  953:     */     //   37: ldc 54
/*  954:     */     //   39: invokeinterface 12 2 0
/*  955:     */     //   44: astore 6
/*  956:     */     //   46: aconst_null
/*  957:     */     //   47: astore 7
/*  958:     */     //   49: aload 4
/*  959:     */     //   51: iconst_1
/*  960:     */     //   52: aload_0
/*  961:     */     //   53: invokevirtual 42	java/lang/Long:longValue	()J
/*  962:     */     //   56: invokeinterface 43 4 0
/*  963:     */     //   61: aload 4
/*  964:     */     //   63: invokeinterface 30 1 0
/*  965:     */     //   68: astore 8
/*  966:     */     //   70: aload 8
/*  967:     */     //   72: invokeinterface 31 1 0
/*  968:     */     //   77: ifne +130 -> 207
/*  969:     */     //   80: aload 8
/*  970:     */     //   82: invokeinterface 55 1 0
/*  971:     */     //   87: invokestatic 56	java/util/Collections:emptyList	()Ljava/util/List;
/*  972:     */     //   90: astore 9
/*  973:     */     //   92: aload 6
/*  974:     */     //   94: ifnull +37 -> 131
/*  975:     */     //   97: aload 7
/*  976:     */     //   99: ifnull +25 -> 124
/*  977:     */     //   102: aload 6
/*  978:     */     //   104: invokeinterface 33 1 0
/*  979:     */     //   109: goto +22 -> 131
/*  980:     */     //   112: astore 10
/*  981:     */     //   114: aload 7
/*  982:     */     //   116: aload 10
/*  983:     */     //   118: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  984:     */     //   121: goto +10 -> 131
/*  985:     */     //   124: aload 6
/*  986:     */     //   126: invokeinterface 33 1 0
/*  987:     */     //   131: aload 4
/*  988:     */     //   133: ifnull +37 -> 170
/*  989:     */     //   136: aload 5
/*  990:     */     //   138: ifnull +25 -> 163
/*  991:     */     //   141: aload 4
/*  992:     */     //   143: invokeinterface 33 1 0
/*  993:     */     //   148: goto +22 -> 170
/*  994:     */     //   151: astore 10
/*  995:     */     //   153: aload 5
/*  996:     */     //   155: aload 10
/*  997:     */     //   157: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  998:     */     //   160: goto +10 -> 170
/*  999:     */     //   163: aload 4
/* 1000:     */     //   165: invokeinterface 33 1 0
/* 1001:     */     //   170: aload_2
/* 1002:     */     //   171: ifnull +33 -> 204
/* 1003:     */     //   174: aload_3
/* 1004:     */     //   175: ifnull +23 -> 198
/* 1005:     */     //   178: aload_2
/* 1006:     */     //   179: invokeinterface 36 1 0
/* 1007:     */     //   184: goto +20 -> 204
/* 1008:     */     //   187: astore 10
/* 1009:     */     //   189: aload_3
/* 1010:     */     //   190: aload 10
/* 1011:     */     //   192: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1012:     */     //   195: goto +9 -> 204
/* 1013:     */     //   198: aload_2
/* 1014:     */     //   199: invokeinterface 36 1 0
/* 1015:     */     //   204: aload 9
/* 1016:     */     //   206: areturn
/* 1017:     */     //   207: new 57	java/util/ArrayList
/* 1018:     */     //   210: dup
/* 1019:     */     //   211: invokespecial 58	java/util/ArrayList:<init>	()V
/* 1020:     */     //   214: astore 9
/* 1021:     */     //   216: aload 8
/* 1022:     */     //   218: ldc 59
/* 1023:     */     //   220: invokeinterface 60 2 0
/* 1024:     */     //   225: istore 10
/* 1025:     */     //   227: aload 6
/* 1026:     */     //   229: iconst_1
/* 1027:     */     //   230: iload 10
/* 1028:     */     //   232: invokeinterface 24 3 0
/* 1029:     */     //   237: aload 6
/* 1030:     */     //   239: iconst_2
/* 1031:     */     //   240: iload_1
/* 1032:     */     //   241: invokeinterface 24 3 0
/* 1033:     */     //   246: aload 6
/* 1034:     */     //   248: invokeinterface 30 1 0
/* 1035:     */     //   253: astore 8
/* 1036:     */     //   255: aload 8
/* 1037:     */     //   257: invokeinterface 31 1 0
/* 1038:     */     //   262: ifeq +26 -> 288
/* 1039:     */     //   265: aload 9
/* 1040:     */     //   267: aload 8
/* 1041:     */     //   269: ldc 61
/* 1042:     */     //   271: invokeinterface 62 2 0
/* 1043:     */     //   276: invokestatic 63	java/lang/Long:valueOf	(J)Ljava/lang/Long;
/* 1044:     */     //   279: invokeinterface 64 2 0
/* 1045:     */     //   284: pop
/* 1046:     */     //   285: goto -30 -> 255
/* 1047:     */     //   288: aload 8
/* 1048:     */     //   290: invokeinterface 55 1 0
/* 1049:     */     //   295: aload 9
/* 1050:     */     //   297: astore 11
/* 1051:     */     //   299: aload 6
/* 1052:     */     //   301: ifnull +37 -> 338
/* 1053:     */     //   304: aload 7
/* 1054:     */     //   306: ifnull +25 -> 331
/* 1055:     */     //   309: aload 6
/* 1056:     */     //   311: invokeinterface 33 1 0
/* 1057:     */     //   316: goto +22 -> 338
/* 1058:     */     //   319: astore 12
/* 1059:     */     //   321: aload 7
/* 1060:     */     //   323: aload 12
/* 1061:     */     //   325: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1062:     */     //   328: goto +10 -> 338
/* 1063:     */     //   331: aload 6
/* 1064:     */     //   333: invokeinterface 33 1 0
/* 1065:     */     //   338: aload 4
/* 1066:     */     //   340: ifnull +37 -> 377
/* 1067:     */     //   343: aload 5
/* 1068:     */     //   345: ifnull +25 -> 370
/* 1069:     */     //   348: aload 4
/* 1070:     */     //   350: invokeinterface 33 1 0
/* 1071:     */     //   355: goto +22 -> 377
/* 1072:     */     //   358: astore 12
/* 1073:     */     //   360: aload 5
/* 1074:     */     //   362: aload 12
/* 1075:     */     //   364: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1076:     */     //   367: goto +10 -> 377
/* 1077:     */     //   370: aload 4
/* 1078:     */     //   372: invokeinterface 33 1 0
/* 1079:     */     //   377: aload_2
/* 1080:     */     //   378: ifnull +33 -> 411
/* 1081:     */     //   381: aload_3
/* 1082:     */     //   382: ifnull +23 -> 405
/* 1083:     */     //   385: aload_2
/* 1084:     */     //   386: invokeinterface 36 1 0
/* 1085:     */     //   391: goto +20 -> 411
/* 1086:     */     //   394: astore 12
/* 1087:     */     //   396: aload_3
/* 1088:     */     //   397: aload 12
/* 1089:     */     //   399: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1090:     */     //   402: goto +9 -> 411
/* 1091:     */     //   405: aload_2
/* 1092:     */     //   406: invokeinterface 36 1 0
/* 1093:     */     //   411: aload 11
/* 1094:     */     //   413: areturn
/* 1095:     */     //   414: astore 8
/* 1096:     */     //   416: aload 8
/* 1097:     */     //   418: astore 7
/* 1098:     */     //   420: aload 8
/* 1099:     */     //   422: athrow
/* 1100:     */     //   423: astore 13
/* 1101:     */     //   425: aload 6
/* 1102:     */     //   427: ifnull +37 -> 464
/* 1103:     */     //   430: aload 7
/* 1104:     */     //   432: ifnull +25 -> 457
/* 1105:     */     //   435: aload 6
/* 1106:     */     //   437: invokeinterface 33 1 0
/* 1107:     */     //   442: goto +22 -> 464
/* 1108:     */     //   445: astore 14
/* 1109:     */     //   447: aload 7
/* 1110:     */     //   449: aload 14
/* 1111:     */     //   451: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1112:     */     //   454: goto +10 -> 464
/* 1113:     */     //   457: aload 6
/* 1114:     */     //   459: invokeinterface 33 1 0
/* 1115:     */     //   464: aload 13
/* 1116:     */     //   466: athrow
/* 1117:     */     //   467: astore 6
/* 1118:     */     //   469: aload 6
/* 1119:     */     //   471: astore 5
/* 1120:     */     //   473: aload 6
/* 1121:     */     //   475: athrow
/* 1122:     */     //   476: astore 15
/* 1123:     */     //   478: aload 4
/* 1124:     */     //   480: ifnull +37 -> 517
/* 1125:     */     //   483: aload 5
/* 1126:     */     //   485: ifnull +25 -> 510
/* 1127:     */     //   488: aload 4
/* 1128:     */     //   490: invokeinterface 33 1 0
/* 1129:     */     //   495: goto +22 -> 517
/* 1130:     */     //   498: astore 16
/* 1131:     */     //   500: aload 5
/* 1132:     */     //   502: aload 16
/* 1133:     */     //   504: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1134:     */     //   507: goto +10 -> 517
/* 1135:     */     //   510: aload 4
/* 1136:     */     //   512: invokeinterface 33 1 0
/* 1137:     */     //   517: aload 15
/* 1138:     */     //   519: athrow
/* 1139:     */     //   520: astore 4
/* 1140:     */     //   522: aload 4
/* 1141:     */     //   524: astore_3
/* 1142:     */     //   525: aload 4
/* 1143:     */     //   527: athrow
/* 1144:     */     //   528: astore 17
/* 1145:     */     //   530: aload_2
/* 1146:     */     //   531: ifnull +33 -> 564
/* 1147:     */     //   534: aload_3
/* 1148:     */     //   535: ifnull +23 -> 558
/* 1149:     */     //   538: aload_2
/* 1150:     */     //   539: invokeinterface 36 1 0
/* 1151:     */     //   544: goto +20 -> 564
/* 1152:     */     //   547: astore 18
/* 1153:     */     //   549: aload_3
/* 1154:     */     //   550: aload 18
/* 1155:     */     //   552: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1156:     */     //   555: goto +9 -> 564
/* 1157:     */     //   558: aload_2
/* 1158:     */     //   559: invokeinterface 36 1 0
/* 1159:     */     //   564: aload 17
/* 1160:     */     //   566: athrow
/* 1161:     */     //   567: astore_2
/* 1162:     */     //   568: new 20	java/lang/RuntimeException
/* 1163:     */     //   571: dup
/* 1164:     */     //   572: aload_2
/* 1165:     */     //   573: invokevirtual 21	java/sql/SQLException:toString	()Ljava/lang/String;
/* 1166:     */     //   576: aload_2
/* 1167:     */     //   577: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/* 1168:     */     //   580: athrow
/* 1169:     */     // Line number table:
/* 1170:     */     //   Java source line #632	-> byte code offset #0
/* 1171:     */     //   Java source line #633	-> byte code offset #7
/* 1172:     */     //   Java source line #635	-> byte code offset #17
/* 1173:     */     //   Java source line #636	-> byte code offset #23
/* 1174:     */     //   Java source line #635	-> byte code offset #33
/* 1175:     */     //   Java source line #637	-> byte code offset #36
/* 1176:     */     //   Java source line #635	-> byte code offset #46
/* 1177:     */     //   Java source line #638	-> byte code offset #49
/* 1178:     */     //   Java source line #639	-> byte code offset #61
/* 1179:     */     //   Java source line #640	-> byte code offset #70
/* 1180:     */     //   Java source line #641	-> byte code offset #80
/* 1181:     */     //   Java source line #642	-> byte code offset #87
/* 1182:     */     //   Java source line #654	-> byte code offset #92
/* 1183:     */     //   Java source line #644	-> byte code offset #207
/* 1184:     */     //   Java source line #645	-> byte code offset #216
/* 1185:     */     //   Java source line #646	-> byte code offset #227
/* 1186:     */     //   Java source line #647	-> byte code offset #237
/* 1187:     */     //   Java source line #648	-> byte code offset #246
/* 1188:     */     //   Java source line #649	-> byte code offset #255
/* 1189:     */     //   Java source line #650	-> byte code offset #265
/* 1190:     */     //   Java source line #652	-> byte code offset #288
/* 1191:     */     //   Java source line #653	-> byte code offset #295
/* 1192:     */     //   Java source line #654	-> byte code offset #299
/* 1193:     */     //   Java source line #635	-> byte code offset #414
/* 1194:     */     //   Java source line #654	-> byte code offset #423
/* 1195:     */     //   Java source line #635	-> byte code offset #467
/* 1196:     */     //   Java source line #654	-> byte code offset #476
/* 1197:     */     //   Java source line #635	-> byte code offset #520
/* 1198:     */     //   Java source line #654	-> byte code offset #528
/* 1199:     */     //   Java source line #655	-> byte code offset #568
/* 1200:     */     // Local variable table:
/* 1201:     */     //   start	length	slot	name	signature
/* 1202:     */     //   0	581	0	paramLong	Long
/* 1203:     */     //   0	581	1	paramInt	int
/* 1204:     */     //   20	539	2	localConnection	Connection
/* 1205:     */     //   567	10	2	localSQLException	SQLException
/* 1206:     */     //   22	528	3	localObject1	Object
/* 1207:     */     //   31	480	4	localPreparedStatement1	PreparedStatement
/* 1208:     */     //   520	6	4	localThrowable1	Throwable
/* 1209:     */     //   34	467	5	localObject2	Object
/* 1210:     */     //   44	414	6	localPreparedStatement2	PreparedStatement
/* 1211:     */     //   467	7	6	localThrowable2	Throwable
/* 1212:     */     //   47	401	7	localObject3	Object
/* 1213:     */     //   68	221	8	localResultSet	ResultSet
/* 1214:     */     //   414	7	8	localThrowable3	Throwable
/* 1215:     */     //   90	206	9	localObject4	Object
/* 1216:     */     //   112	5	10	localThrowable4	Throwable
/* 1217:     */     //   151	5	10	localThrowable5	Throwable
/* 1218:     */     //   187	4	10	localThrowable6	Throwable
/* 1219:     */     //   225	6	10	i	int
/* 1220:     */     //   297	115	11	localObject5	Object
/* 1221:     */     //   319	5	12	localThrowable7	Throwable
/* 1222:     */     //   358	5	12	localThrowable8	Throwable
/* 1223:     */     //   394	4	12	localThrowable9	Throwable
/* 1224:     */     //   423	42	13	localObject6	Object
/* 1225:     */     //   445	5	14	localThrowable10	Throwable
/* 1226:     */     //   476	42	15	localObject7	Object
/* 1227:     */     //   498	5	16	localThrowable11	Throwable
/* 1228:     */     //   528	37	17	localObject8	Object
/* 1229:     */     //   547	4	18	localThrowable12	Throwable
/* 1230:     */     // Exception table:
/* 1231:     */     //   from	to	target	type
/* 1232:     */     //   102	109	112	java/lang/Throwable
/* 1233:     */     //   141	148	151	java/lang/Throwable
/* 1234:     */     //   178	184	187	java/lang/Throwable
/* 1235:     */     //   309	316	319	java/lang/Throwable
/* 1236:     */     //   348	355	358	java/lang/Throwable
/* 1237:     */     //   385	391	394	java/lang/Throwable
/* 1238:     */     //   49	92	414	java/lang/Throwable
/* 1239:     */     //   207	299	414	java/lang/Throwable
/* 1240:     */     //   49	92	423	finally
/* 1241:     */     //   207	299	423	finally
/* 1242:     */     //   414	425	423	finally
/* 1243:     */     //   435	442	445	java/lang/Throwable
/* 1244:     */     //   36	131	467	java/lang/Throwable
/* 1245:     */     //   207	338	467	java/lang/Throwable
/* 1246:     */     //   414	467	467	java/lang/Throwable
/* 1247:     */     //   36	131	476	finally
/* 1248:     */     //   207	338	476	finally
/* 1249:     */     //   414	478	476	finally
/* 1250:     */     //   488	495	498	java/lang/Throwable
/* 1251:     */     //   23	170	520	java/lang/Throwable
/* 1252:     */     //   207	377	520	java/lang/Throwable
/* 1253:     */     //   414	520	520	java/lang/Throwable
/* 1254:     */     //   23	170	528	finally
/* 1255:     */     //   207	377	528	finally
/* 1256:     */     //   414	530	528	finally
/* 1257:     */     //   538	544	547	java/lang/Throwable
/* 1258:     */     //   17	204	567	java/sql/SQLException
/* 1259:     */     //   207	411	567	java/sql/SQLException
/* 1260:     */     //   414	567	567	java/sql/SQLException
/* 1261:     */   }
/* 1262:     */   
/* 1263:     */   /* Error */
/* 1264:     */   public static java.util.List<Block> getBlocksAfter(Long paramLong, int paramInt)
/* 1265:     */   {
/* 1266:     */     // Byte code:
/* 1267:     */     //   0: iload_1
/* 1268:     */     //   1: sipush 1440
/* 1269:     */     //   4: if_icmple +13 -> 17
/* 1270:     */     //   7: new 50	java/lang/IllegalArgumentException
/* 1271:     */     //   10: dup
/* 1272:     */     //   11: ldc 51
/* 1273:     */     //   13: invokespecial 52	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
/* 1274:     */     //   16: athrow
/* 1275:     */     //   17: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
/* 1276:     */     //   20: astore_2
/* 1277:     */     //   21: aconst_null
/* 1278:     */     //   22: astore_3
/* 1279:     */     //   23: aload_2
/* 1280:     */     //   24: ldc 53
/* 1281:     */     //   26: invokeinterface 12 2 0
/* 1282:     */     //   31: astore 4
/* 1283:     */     //   33: aconst_null
/* 1284:     */     //   34: astore 5
/* 1285:     */     //   36: aload_2
/* 1286:     */     //   37: ldc 65
/* 1287:     */     //   39: invokeinterface 12 2 0
/* 1288:     */     //   44: astore 6
/* 1289:     */     //   46: aconst_null
/* 1290:     */     //   47: astore 7
/* 1291:     */     //   49: aload 4
/* 1292:     */     //   51: iconst_1
/* 1293:     */     //   52: aload_0
/* 1294:     */     //   53: invokevirtual 42	java/lang/Long:longValue	()J
/* 1295:     */     //   56: invokeinterface 43 4 0
/* 1296:     */     //   61: aload 4
/* 1297:     */     //   63: invokeinterface 30 1 0
/* 1298:     */     //   68: astore 8
/* 1299:     */     //   70: aload 8
/* 1300:     */     //   72: invokeinterface 31 1 0
/* 1301:     */     //   77: ifne +130 -> 207
/* 1302:     */     //   80: aload 8
/* 1303:     */     //   82: invokeinterface 55 1 0
/* 1304:     */     //   87: invokestatic 56	java/util/Collections:emptyList	()Ljava/util/List;
/* 1305:     */     //   90: astore 9
/* 1306:     */     //   92: aload 6
/* 1307:     */     //   94: ifnull +37 -> 131
/* 1308:     */     //   97: aload 7
/* 1309:     */     //   99: ifnull +25 -> 124
/* 1310:     */     //   102: aload 6
/* 1311:     */     //   104: invokeinterface 33 1 0
/* 1312:     */     //   109: goto +22 -> 131
/* 1313:     */     //   112: astore 10
/* 1314:     */     //   114: aload 7
/* 1315:     */     //   116: aload 10
/* 1316:     */     //   118: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1317:     */     //   121: goto +10 -> 131
/* 1318:     */     //   124: aload 6
/* 1319:     */     //   126: invokeinterface 33 1 0
/* 1320:     */     //   131: aload 4
/* 1321:     */     //   133: ifnull +37 -> 170
/* 1322:     */     //   136: aload 5
/* 1323:     */     //   138: ifnull +25 -> 163
/* 1324:     */     //   141: aload 4
/* 1325:     */     //   143: invokeinterface 33 1 0
/* 1326:     */     //   148: goto +22 -> 170
/* 1327:     */     //   151: astore 10
/* 1328:     */     //   153: aload 5
/* 1329:     */     //   155: aload 10
/* 1330:     */     //   157: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1331:     */     //   160: goto +10 -> 170
/* 1332:     */     //   163: aload 4
/* 1333:     */     //   165: invokeinterface 33 1 0
/* 1334:     */     //   170: aload_2
/* 1335:     */     //   171: ifnull +33 -> 204
/* 1336:     */     //   174: aload_3
/* 1337:     */     //   175: ifnull +23 -> 198
/* 1338:     */     //   178: aload_2
/* 1339:     */     //   179: invokeinterface 36 1 0
/* 1340:     */     //   184: goto +20 -> 204
/* 1341:     */     //   187: astore 10
/* 1342:     */     //   189: aload_3
/* 1343:     */     //   190: aload 10
/* 1344:     */     //   192: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1345:     */     //   195: goto +9 -> 204
/* 1346:     */     //   198: aload_2
/* 1347:     */     //   199: invokeinterface 36 1 0
/* 1348:     */     //   204: aload 9
/* 1349:     */     //   206: areturn
/* 1350:     */     //   207: new 57	java/util/ArrayList
/* 1351:     */     //   210: dup
/* 1352:     */     //   211: invokespecial 58	java/util/ArrayList:<init>	()V
/* 1353:     */     //   214: astore 9
/* 1354:     */     //   216: aload 8
/* 1355:     */     //   218: ldc 59
/* 1356:     */     //   220: invokeinterface 60 2 0
/* 1357:     */     //   225: istore 10
/* 1358:     */     //   227: aload 6
/* 1359:     */     //   229: iconst_1
/* 1360:     */     //   230: iload 10
/* 1361:     */     //   232: invokeinterface 24 3 0
/* 1362:     */     //   237: aload 6
/* 1363:     */     //   239: iconst_2
/* 1364:     */     //   240: iload_1
/* 1365:     */     //   241: invokeinterface 24 3 0
/* 1366:     */     //   246: aload 6
/* 1367:     */     //   248: invokeinterface 30 1 0
/* 1368:     */     //   253: astore 8
/* 1369:     */     //   255: aload 8
/* 1370:     */     //   257: invokeinterface 31 1 0
/* 1371:     */     //   262: ifeq +20 -> 282
/* 1372:     */     //   265: aload 9
/* 1373:     */     //   267: aload_2
/* 1374:     */     //   268: aload 8
/* 1375:     */     //   270: invokestatic 66	nxt/Block:getBlock	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/Block;
/* 1376:     */     //   273: invokeinterface 64 2 0
/* 1377:     */     //   278: pop
/* 1378:     */     //   279: goto -24 -> 255
/* 1379:     */     //   282: aload 8
/* 1380:     */     //   284: invokeinterface 55 1 0
/* 1381:     */     //   289: aload 9
/* 1382:     */     //   291: astore 11
/* 1383:     */     //   293: aload 6
/* 1384:     */     //   295: ifnull +37 -> 332
/* 1385:     */     //   298: aload 7
/* 1386:     */     //   300: ifnull +25 -> 325
/* 1387:     */     //   303: aload 6
/* 1388:     */     //   305: invokeinterface 33 1 0
/* 1389:     */     //   310: goto +22 -> 332
/* 1390:     */     //   313: astore 12
/* 1391:     */     //   315: aload 7
/* 1392:     */     //   317: aload 12
/* 1393:     */     //   319: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1394:     */     //   322: goto +10 -> 332
/* 1395:     */     //   325: aload 6
/* 1396:     */     //   327: invokeinterface 33 1 0
/* 1397:     */     //   332: aload 4
/* 1398:     */     //   334: ifnull +37 -> 371
/* 1399:     */     //   337: aload 5
/* 1400:     */     //   339: ifnull +25 -> 364
/* 1401:     */     //   342: aload 4
/* 1402:     */     //   344: invokeinterface 33 1 0
/* 1403:     */     //   349: goto +22 -> 371
/* 1404:     */     //   352: astore 12
/* 1405:     */     //   354: aload 5
/* 1406:     */     //   356: aload 12
/* 1407:     */     //   358: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1408:     */     //   361: goto +10 -> 371
/* 1409:     */     //   364: aload 4
/* 1410:     */     //   366: invokeinterface 33 1 0
/* 1411:     */     //   371: aload_2
/* 1412:     */     //   372: ifnull +33 -> 405
/* 1413:     */     //   375: aload_3
/* 1414:     */     //   376: ifnull +23 -> 399
/* 1415:     */     //   379: aload_2
/* 1416:     */     //   380: invokeinterface 36 1 0
/* 1417:     */     //   385: goto +20 -> 405
/* 1418:     */     //   388: astore 12
/* 1419:     */     //   390: aload_3
/* 1420:     */     //   391: aload 12
/* 1421:     */     //   393: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1422:     */     //   396: goto +9 -> 405
/* 1423:     */     //   399: aload_2
/* 1424:     */     //   400: invokeinterface 36 1 0
/* 1425:     */     //   405: aload 11
/* 1426:     */     //   407: areturn
/* 1427:     */     //   408: astore 8
/* 1428:     */     //   410: aload 8
/* 1429:     */     //   412: astore 7
/* 1430:     */     //   414: aload 8
/* 1431:     */     //   416: athrow
/* 1432:     */     //   417: astore 13
/* 1433:     */     //   419: aload 6
/* 1434:     */     //   421: ifnull +37 -> 458
/* 1435:     */     //   424: aload 7
/* 1436:     */     //   426: ifnull +25 -> 451
/* 1437:     */     //   429: aload 6
/* 1438:     */     //   431: invokeinterface 33 1 0
/* 1439:     */     //   436: goto +22 -> 458
/* 1440:     */     //   439: astore 14
/* 1441:     */     //   441: aload 7
/* 1442:     */     //   443: aload 14
/* 1443:     */     //   445: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1444:     */     //   448: goto +10 -> 458
/* 1445:     */     //   451: aload 6
/* 1446:     */     //   453: invokeinterface 33 1 0
/* 1447:     */     //   458: aload 13
/* 1448:     */     //   460: athrow
/* 1449:     */     //   461: astore 6
/* 1450:     */     //   463: aload 6
/* 1451:     */     //   465: astore 5
/* 1452:     */     //   467: aload 6
/* 1453:     */     //   469: athrow
/* 1454:     */     //   470: astore 15
/* 1455:     */     //   472: aload 4
/* 1456:     */     //   474: ifnull +37 -> 511
/* 1457:     */     //   477: aload 5
/* 1458:     */     //   479: ifnull +25 -> 504
/* 1459:     */     //   482: aload 4
/* 1460:     */     //   484: invokeinterface 33 1 0
/* 1461:     */     //   489: goto +22 -> 511
/* 1462:     */     //   492: astore 16
/* 1463:     */     //   494: aload 5
/* 1464:     */     //   496: aload 16
/* 1465:     */     //   498: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1466:     */     //   501: goto +10 -> 511
/* 1467:     */     //   504: aload 4
/* 1468:     */     //   506: invokeinterface 33 1 0
/* 1469:     */     //   511: aload 15
/* 1470:     */     //   513: athrow
/* 1471:     */     //   514: astore 4
/* 1472:     */     //   516: aload 4
/* 1473:     */     //   518: astore_3
/* 1474:     */     //   519: aload 4
/* 1475:     */     //   521: athrow
/* 1476:     */     //   522: astore 17
/* 1477:     */     //   524: aload_2
/* 1478:     */     //   525: ifnull +33 -> 558
/* 1479:     */     //   528: aload_3
/* 1480:     */     //   529: ifnull +23 -> 552
/* 1481:     */     //   532: aload_2
/* 1482:     */     //   533: invokeinterface 36 1 0
/* 1483:     */     //   538: goto +20 -> 558
/* 1484:     */     //   541: astore 18
/* 1485:     */     //   543: aload_3
/* 1486:     */     //   544: aload 18
/* 1487:     */     //   546: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1488:     */     //   549: goto +9 -> 558
/* 1489:     */     //   552: aload_2
/* 1490:     */     //   553: invokeinterface 36 1 0
/* 1491:     */     //   558: aload 17
/* 1492:     */     //   560: athrow
/* 1493:     */     //   561: astore_2
/* 1494:     */     //   562: new 20	java/lang/RuntimeException
/* 1495:     */     //   565: dup
/* 1496:     */     //   566: aload_2
/* 1497:     */     //   567: invokevirtual 68	java/lang/Exception:toString	()Ljava/lang/String;
/* 1498:     */     //   570: aload_2
/* 1499:     */     //   571: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/* 1500:     */     //   574: athrow
/* 1501:     */     // Line number table:
/* 1502:     */     //   Java source line #660	-> byte code offset #0
/* 1503:     */     //   Java source line #661	-> byte code offset #7
/* 1504:     */     //   Java source line #663	-> byte code offset #17
/* 1505:     */     //   Java source line #664	-> byte code offset #23
/* 1506:     */     //   Java source line #663	-> byte code offset #33
/* 1507:     */     //   Java source line #665	-> byte code offset #36
/* 1508:     */     //   Java source line #663	-> byte code offset #46
/* 1509:     */     //   Java source line #666	-> byte code offset #49
/* 1510:     */     //   Java source line #667	-> byte code offset #61
/* 1511:     */     //   Java source line #668	-> byte code offset #70
/* 1512:     */     //   Java source line #669	-> byte code offset #80
/* 1513:     */     //   Java source line #670	-> byte code offset #87
/* 1514:     */     //   Java source line #682	-> byte code offset #92
/* 1515:     */     //   Java source line #672	-> byte code offset #207
/* 1516:     */     //   Java source line #673	-> byte code offset #216
/* 1517:     */     //   Java source line #674	-> byte code offset #227
/* 1518:     */     //   Java source line #675	-> byte code offset #237
/* 1519:     */     //   Java source line #676	-> byte code offset #246
/* 1520:     */     //   Java source line #677	-> byte code offset #255
/* 1521:     */     //   Java source line #678	-> byte code offset #265
/* 1522:     */     //   Java source line #680	-> byte code offset #282
/* 1523:     */     //   Java source line #681	-> byte code offset #289
/* 1524:     */     //   Java source line #682	-> byte code offset #293
/* 1525:     */     //   Java source line #663	-> byte code offset #408
/* 1526:     */     //   Java source line #682	-> byte code offset #417
/* 1527:     */     //   Java source line #663	-> byte code offset #461
/* 1528:     */     //   Java source line #682	-> byte code offset #470
/* 1529:     */     //   Java source line #663	-> byte code offset #514
/* 1530:     */     //   Java source line #682	-> byte code offset #522
/* 1531:     */     //   Java source line #683	-> byte code offset #562
/* 1532:     */     // Local variable table:
/* 1533:     */     //   start	length	slot	name	signature
/* 1534:     */     //   0	575	0	paramLong	Long
/* 1535:     */     //   0	575	1	paramInt	int
/* 1536:     */     //   20	533	2	localConnection	Connection
/* 1537:     */     //   561	10	2	localValidationException	NxtException.ValidationException
/* 1538:     */     //   22	522	3	localObject1	Object
/* 1539:     */     //   31	474	4	localPreparedStatement1	PreparedStatement
/* 1540:     */     //   514	6	4	localThrowable1	Throwable
/* 1541:     */     //   34	461	5	localObject2	Object
/* 1542:     */     //   44	408	6	localPreparedStatement2	PreparedStatement
/* 1543:     */     //   461	7	6	localThrowable2	Throwable
/* 1544:     */     //   47	395	7	localObject3	Object
/* 1545:     */     //   68	215	8	localResultSet	ResultSet
/* 1546:     */     //   408	7	8	localThrowable3	Throwable
/* 1547:     */     //   90	200	9	localObject4	Object
/* 1548:     */     //   112	5	10	localThrowable4	Throwable
/* 1549:     */     //   151	5	10	localThrowable5	Throwable
/* 1550:     */     //   187	4	10	localThrowable6	Throwable
/* 1551:     */     //   225	6	10	i	int
/* 1552:     */     //   291	115	11	localObject5	Object
/* 1553:     */     //   313	5	12	localThrowable7	Throwable
/* 1554:     */     //   352	5	12	localThrowable8	Throwable
/* 1555:     */     //   388	4	12	localThrowable9	Throwable
/* 1556:     */     //   417	42	13	localObject6	Object
/* 1557:     */     //   439	5	14	localThrowable10	Throwable
/* 1558:     */     //   470	42	15	localObject7	Object
/* 1559:     */     //   492	5	16	localThrowable11	Throwable
/* 1560:     */     //   522	37	17	localObject8	Object
/* 1561:     */     //   541	4	18	localThrowable12	Throwable
/* 1562:     */     // Exception table:
/* 1563:     */     //   from	to	target	type
/* 1564:     */     //   102	109	112	java/lang/Throwable
/* 1565:     */     //   141	148	151	java/lang/Throwable
/* 1566:     */     //   178	184	187	java/lang/Throwable
/* 1567:     */     //   303	310	313	java/lang/Throwable
/* 1568:     */     //   342	349	352	java/lang/Throwable
/* 1569:     */     //   379	385	388	java/lang/Throwable
/* 1570:     */     //   49	92	408	java/lang/Throwable
/* 1571:     */     //   207	293	408	java/lang/Throwable
/* 1572:     */     //   49	92	417	finally
/* 1573:     */     //   207	293	417	finally
/* 1574:     */     //   408	419	417	finally
/* 1575:     */     //   429	436	439	java/lang/Throwable
/* 1576:     */     //   36	131	461	java/lang/Throwable
/* 1577:     */     //   207	332	461	java/lang/Throwable
/* 1578:     */     //   408	461	461	java/lang/Throwable
/* 1579:     */     //   36	131	470	finally
/* 1580:     */     //   207	332	470	finally
/* 1581:     */     //   408	472	470	finally
/* 1582:     */     //   482	489	492	java/lang/Throwable
/* 1583:     */     //   23	170	514	java/lang/Throwable
/* 1584:     */     //   207	371	514	java/lang/Throwable
/* 1585:     */     //   408	514	514	java/lang/Throwable
/* 1586:     */     //   23	170	522	finally
/* 1587:     */     //   207	371	522	finally
/* 1588:     */     //   408	524	522	finally
/* 1589:     */     //   532	538	541	java/lang/Throwable
/* 1590:     */     //   17	204	561	nxt/NxtException$ValidationException
/* 1591:     */     //   17	204	561	java/sql/SQLException
/* 1592:     */     //   207	405	561	nxt/NxtException$ValidationException
/* 1593:     */     //   207	405	561	java/sql/SQLException
/* 1594:     */     //   408	561	561	nxt/NxtException$ValidationException
/* 1595:     */     //   408	561	561	java/sql/SQLException
/* 1596:     */   }
/* 1597:     */   
/* 1598:     */   public static long getBlockIdAtHeight(int paramInt)
/* 1599:     */   {
/* 1600: 688 */     Block localBlock = (Block)lastBlock.get();
/* 1601: 689 */     if (paramInt > localBlock.getHeight()) {
/* 1602: 690 */       throw new IllegalArgumentException("Invalid height " + paramInt + ", current blockchain is at " + localBlock.getHeight());
/* 1603:     */     }
/* 1604: 692 */     if (paramInt == localBlock.getHeight()) {
/* 1605: 693 */       return localBlock.getId().longValue();
/* 1606:     */     }
/* 1607: 695 */     return Block.findBlockIdAtHeight(paramInt);
/* 1608:     */   }
/* 1609:     */   
/* 1610:     */   /* Error */
/* 1611:     */   public static java.util.List<Block> getBlocksFromHeight(int paramInt)
/* 1612:     */   {
/* 1613:     */     // Byte code:
/* 1614:     */     //   0: iload_0
/* 1615:     */     //   1: iflt +23 -> 24
/* 1616:     */     //   4: getstatic 6	nxt/Blockchain:lastBlock	Ljava/util/concurrent/atomic/AtomicReference;
/* 1617:     */     //   7: invokevirtual 69	java/util/concurrent/atomic/AtomicReference:get	()Ljava/lang/Object;
/* 1618:     */     //   10: checkcast 70	nxt/Block
/* 1619:     */     //   13: invokevirtual 71	nxt/Block:getHeight	()I
/* 1620:     */     //   16: iload_0
/* 1621:     */     //   17: isub
/* 1622:     */     //   18: sipush 1440
/* 1623:     */     //   21: if_icmple +13 -> 34
/* 1624:     */     //   24: new 50	java/lang/IllegalArgumentException
/* 1625:     */     //   27: dup
/* 1626:     */     //   28: ldc 81
/* 1627:     */     //   30: invokespecial 52	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
/* 1628:     */     //   33: athrow
/* 1629:     */     //   34: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
/* 1630:     */     //   37: astore_1
/* 1631:     */     //   38: aconst_null
/* 1632:     */     //   39: astore_2
/* 1633:     */     //   40: aload_1
/* 1634:     */     //   41: ldc 82
/* 1635:     */     //   43: invokeinterface 12 2 0
/* 1636:     */     //   48: astore_3
/* 1637:     */     //   49: aconst_null
/* 1638:     */     //   50: astore 4
/* 1639:     */     //   52: aload_3
/* 1640:     */     //   53: iconst_1
/* 1641:     */     //   54: iload_0
/* 1642:     */     //   55: invokeinterface 24 3 0
/* 1643:     */     //   60: aload_3
/* 1644:     */     //   61: invokeinterface 30 1 0
/* 1645:     */     //   66: astore 5
/* 1646:     */     //   68: new 57	java/util/ArrayList
/* 1647:     */     //   71: dup
/* 1648:     */     //   72: invokespecial 58	java/util/ArrayList:<init>	()V
/* 1649:     */     //   75: astore 6
/* 1650:     */     //   77: aload 5
/* 1651:     */     //   79: invokeinterface 31 1 0
/* 1652:     */     //   84: ifeq +20 -> 104
/* 1653:     */     //   87: aload 6
/* 1654:     */     //   89: aload_1
/* 1655:     */     //   90: aload 5
/* 1656:     */     //   92: invokestatic 66	nxt/Block:getBlock	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/Block;
/* 1657:     */     //   95: invokeinterface 64 2 0
/* 1658:     */     //   100: pop
/* 1659:     */     //   101: goto -24 -> 77
/* 1660:     */     //   104: aload 6
/* 1661:     */     //   106: astore 7
/* 1662:     */     //   108: aload_3
/* 1663:     */     //   109: ifnull +35 -> 144
/* 1664:     */     //   112: aload 4
/* 1665:     */     //   114: ifnull +24 -> 138
/* 1666:     */     //   117: aload_3
/* 1667:     */     //   118: invokeinterface 33 1 0
/* 1668:     */     //   123: goto +21 -> 144
/* 1669:     */     //   126: astore 8
/* 1670:     */     //   128: aload 4
/* 1671:     */     //   130: aload 8
/* 1672:     */     //   132: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1673:     */     //   135: goto +9 -> 144
/* 1674:     */     //   138: aload_3
/* 1675:     */     //   139: invokeinterface 33 1 0
/* 1676:     */     //   144: aload_1
/* 1677:     */     //   145: ifnull +33 -> 178
/* 1678:     */     //   148: aload_2
/* 1679:     */     //   149: ifnull +23 -> 172
/* 1680:     */     //   152: aload_1
/* 1681:     */     //   153: invokeinterface 36 1 0
/* 1682:     */     //   158: goto +20 -> 178
/* 1683:     */     //   161: astore 8
/* 1684:     */     //   163: aload_2
/* 1685:     */     //   164: aload 8
/* 1686:     */     //   166: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1687:     */     //   169: goto +9 -> 178
/* 1688:     */     //   172: aload_1
/* 1689:     */     //   173: invokeinterface 36 1 0
/* 1690:     */     //   178: aload 7
/* 1691:     */     //   180: areturn
/* 1692:     */     //   181: astore 5
/* 1693:     */     //   183: aload 5
/* 1694:     */     //   185: astore 4
/* 1695:     */     //   187: aload 5
/* 1696:     */     //   189: athrow
/* 1697:     */     //   190: astore 9
/* 1698:     */     //   192: aload_3
/* 1699:     */     //   193: ifnull +35 -> 228
/* 1700:     */     //   196: aload 4
/* 1701:     */     //   198: ifnull +24 -> 222
/* 1702:     */     //   201: aload_3
/* 1703:     */     //   202: invokeinterface 33 1 0
/* 1704:     */     //   207: goto +21 -> 228
/* 1705:     */     //   210: astore 10
/* 1706:     */     //   212: aload 4
/* 1707:     */     //   214: aload 10
/* 1708:     */     //   216: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1709:     */     //   219: goto +9 -> 228
/* 1710:     */     //   222: aload_3
/* 1711:     */     //   223: invokeinterface 33 1 0
/* 1712:     */     //   228: aload 9
/* 1713:     */     //   230: athrow
/* 1714:     */     //   231: astore_3
/* 1715:     */     //   232: aload_3
/* 1716:     */     //   233: astore_2
/* 1717:     */     //   234: aload_3
/* 1718:     */     //   235: athrow
/* 1719:     */     //   236: astore 11
/* 1720:     */     //   238: aload_1
/* 1721:     */     //   239: ifnull +33 -> 272
/* 1722:     */     //   242: aload_2
/* 1723:     */     //   243: ifnull +23 -> 266
/* 1724:     */     //   246: aload_1
/* 1725:     */     //   247: invokeinterface 36 1 0
/* 1726:     */     //   252: goto +20 -> 272
/* 1727:     */     //   255: astore 12
/* 1728:     */     //   257: aload_2
/* 1729:     */     //   258: aload 12
/* 1730:     */     //   260: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1731:     */     //   263: goto +9 -> 272
/* 1732:     */     //   266: aload_1
/* 1733:     */     //   267: invokeinterface 36 1 0
/* 1734:     */     //   272: aload 11
/* 1735:     */     //   274: athrow
/* 1736:     */     //   275: astore_1
/* 1737:     */     //   276: new 20	java/lang/RuntimeException
/* 1738:     */     //   279: dup
/* 1739:     */     //   280: aload_1
/* 1740:     */     //   281: invokevirtual 68	java/lang/Exception:toString	()Ljava/lang/String;
/* 1741:     */     //   284: aload_1
/* 1742:     */     //   285: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/* 1743:     */     //   288: athrow
/* 1744:     */     // Line number table:
/* 1745:     */     //   Java source line #699	-> byte code offset #0
/* 1746:     */     //   Java source line #700	-> byte code offset #24
/* 1747:     */     //   Java source line #702	-> byte code offset #34
/* 1748:     */     //   Java source line #703	-> byte code offset #40
/* 1749:     */     //   Java source line #702	-> byte code offset #49
/* 1750:     */     //   Java source line #704	-> byte code offset #52
/* 1751:     */     //   Java source line #705	-> byte code offset #60
/* 1752:     */     //   Java source line #706	-> byte code offset #68
/* 1753:     */     //   Java source line #707	-> byte code offset #77
/* 1754:     */     //   Java source line #708	-> byte code offset #87
/* 1755:     */     //   Java source line #710	-> byte code offset #104
/* 1756:     */     //   Java source line #711	-> byte code offset #108
/* 1757:     */     //   Java source line #702	-> byte code offset #181
/* 1758:     */     //   Java source line #711	-> byte code offset #190
/* 1759:     */     //   Java source line #702	-> byte code offset #231
/* 1760:     */     //   Java source line #711	-> byte code offset #236
/* 1761:     */     //   Java source line #712	-> byte code offset #276
/* 1762:     */     // Local variable table:
/* 1763:     */     //   start	length	slot	name	signature
/* 1764:     */     //   0	289	0	paramInt	int
/* 1765:     */     //   37	230	1	localConnection	Connection
/* 1766:     */     //   275	10	1	localSQLException	SQLException
/* 1767:     */     //   39	219	2	localObject1	Object
/* 1768:     */     //   48	175	3	localPreparedStatement	PreparedStatement
/* 1769:     */     //   231	4	3	localThrowable1	Throwable
/* 1770:     */     //   50	163	4	localObject2	Object
/* 1771:     */     //   66	25	5	localResultSet	ResultSet
/* 1772:     */     //   181	7	5	localThrowable2	Throwable
/* 1773:     */     //   75	30	6	localArrayList1	java.util.ArrayList
/* 1774:     */     //   126	5	8	localThrowable3	Throwable
/* 1775:     */     //   161	4	8	localThrowable4	Throwable
/* 1776:     */     //   190	39	9	localObject3	Object
/* 1777:     */     //   210	5	10	localThrowable5	Throwable
/* 1778:     */     //   236	37	11	localObject4	Object
/* 1779:     */     //   255	4	12	localThrowable6	Throwable
/* 1780:     */     // Exception table:
/* 1781:     */     //   from	to	target	type
/* 1782:     */     //   117	123	126	java/lang/Throwable
/* 1783:     */     //   152	158	161	java/lang/Throwable
/* 1784:     */     //   52	108	181	java/lang/Throwable
/* 1785:     */     //   52	108	190	finally
/* 1786:     */     //   181	192	190	finally
/* 1787:     */     //   201	207	210	java/lang/Throwable
/* 1788:     */     //   40	144	231	java/lang/Throwable
/* 1789:     */     //   181	231	231	java/lang/Throwable
/* 1790:     */     //   40	144	236	finally
/* 1791:     */     //   181	238	236	finally
/* 1792:     */     //   246	252	255	java/lang/Throwable
/* 1793:     */     //   34	178	275	java/sql/SQLException
/* 1794:     */     //   34	178	275	nxt/NxtException$ValidationException
/* 1795:     */     //   181	275	275	java/sql/SQLException
/* 1796:     */     //   181	275	275	nxt/NxtException$ValidationException
/* 1797:     */   }
/* 1798:     */   
/* 1799:     */   public static Collection<Transaction> getAllUnconfirmedTransactions()
/* 1800:     */   {
/* 1801: 717 */     return allUnconfirmedTransactions;
/* 1802:     */   }
/* 1803:     */   
/* 1804:     */   public static Block getLastBlock()
/* 1805:     */   {
/* 1806: 721 */     return (Block)lastBlock.get();
/* 1807:     */   }
/* 1808:     */   
/* 1809:     */   public static Block getBlock(Long paramLong)
/* 1810:     */   {
/* 1811: 725 */     return Block.findBlock(paramLong);
/* 1812:     */   }
/* 1813:     */   
/* 1814:     */   public static Transaction getTransaction(Long paramLong)
/* 1815:     */   {
/* 1816: 729 */     return Transaction.findTransaction(paramLong);
/* 1817:     */   }
/* 1818:     */   
/* 1819:     */   public static Transaction getUnconfirmedTransaction(Long paramLong)
/* 1820:     */   {
/* 1821: 733 */     return (Transaction)unconfirmedTransactions.get(paramLong);
/* 1822:     */   }
/* 1823:     */   
/* 1824:     */   public static void broadcast(Transaction paramTransaction)
/* 1825:     */   {
/* 1826: 738 */     JSONObject localJSONObject = new JSONObject();
/* 1827: 739 */     localJSONObject.put("requestType", "processTransactions");
/* 1828: 740 */     JSONArray localJSONArray = new JSONArray();
/* 1829: 741 */     localJSONArray.add(paramTransaction.getJSONObject());
/* 1830: 742 */     localJSONObject.put("transactions", localJSONArray);
/* 1831:     */     
/* 1832: 744 */     Peer.sendToSomePeers(localJSONObject);
/* 1833:     */     
/* 1834: 746 */     nonBroadcastedTransactions.put(paramTransaction.getId(), paramTransaction);
/* 1835:     */   }
/* 1836:     */   
/* 1837:     */   public static Peer getLastBlockchainFeeder()
/* 1838:     */   {
/* 1839: 750 */     return lastBlockchainFeeder;
/* 1840:     */   }
/* 1841:     */   
/* 1842:     */   public static void processTransactions(JSONObject paramJSONObject)
/* 1843:     */     throws NxtException.ValidationException
/* 1844:     */   {
/* 1845: 754 */     JSONArray localJSONArray = (JSONArray)paramJSONObject.get("transactions");
/* 1846: 755 */     processTransactions(localJSONArray, false);
/* 1847:     */   }
/* 1848:     */   
/* 1849:     */   public static boolean pushBlock(JSONObject paramJSONObject)
/* 1850:     */     throws NxtException
/* 1851:     */   {
/* 1852: 760 */     Block localBlock = Block.getBlock(paramJSONObject);
/* 1853: 761 */     if (!((Block)lastBlock.get()).getId().equals(localBlock.getPreviousBlockId())) {
/* 1854: 764 */       return false;
/* 1855:     */     }
/* 1856: 766 */     JSONArray localJSONArray = (JSONArray)paramJSONObject.get("transactions");
/* 1857: 767 */     Transaction[] arrayOfTransaction = new Transaction[localJSONArray.size()];
/* 1858: 768 */     for (int i = 0; i < arrayOfTransaction.length; i++) {
/* 1859: 769 */       arrayOfTransaction[i] = Transaction.getTransaction((JSONObject)localJSONArray.get(i));
/* 1860:     */     }
/* 1861:     */     try
/* 1862:     */     {
/* 1863: 772 */       pushBlock(localBlock, arrayOfTransaction);
/* 1864: 773 */       return true;
/* 1865:     */     }
/* 1866:     */     catch (BlockNotAcceptedException localBlockNotAcceptedException)
/* 1867:     */     {
/* 1868: 775 */       Logger.logDebugMessage("Block " + localBlock.getStringId() + " not accepted: " + localBlockNotAcceptedException.getMessage());
/* 1869: 776 */       throw localBlockNotAcceptedException;
/* 1870:     */     }
/* 1871:     */   }
/* 1872:     */   
/* 1873:     */   static void addBlock(Block paramBlock)
/* 1874:     */   {
/* 1875:     */     try
/* 1876:     */     {
/* 1877: 781 */       Connection localConnection = Db.getConnection();Object localObject1 = null;
/* 1878:     */       try
/* 1879:     */       {
/* 1880:     */         try
/* 1881:     */         {
/* 1882: 783 */           Block.saveBlock(localConnection, paramBlock);
/* 1883: 784 */           lastBlock.set(paramBlock);
/* 1884: 785 */           localConnection.commit();
/* 1885:     */         }
/* 1886:     */         catch (SQLException localSQLException2)
/* 1887:     */         {
/* 1888: 787 */           localConnection.rollback();
/* 1889: 788 */           throw localSQLException2;
/* 1890:     */         }
/* 1891:     */       }
/* 1892:     */       catch (Throwable localThrowable2)
/* 1893:     */       {
/* 1894: 781 */         localObject1 = localThrowable2;throw localThrowable2;
/* 1895:     */       }
/* 1896:     */       finally
/* 1897:     */       {
/* 1898: 790 */         if (localConnection != null) {
/* 1899: 790 */           if (localObject1 != null) {
/* 1900:     */             try
/* 1901:     */             {
/* 1902: 790 */               localConnection.close();
/* 1903:     */             }
/* 1904:     */             catch (Throwable localThrowable3)
/* 1905:     */             {
/* 1906: 790 */               localObject1.addSuppressed(localThrowable3);
/* 1907:     */             }
/* 1908:     */           } else {
/* 1909: 790 */             localConnection.close();
/* 1910:     */           }
/* 1911:     */         }
/* 1912:     */       }
/* 1913:     */     }
/* 1914:     */     catch (SQLException localSQLException1)
/* 1915:     */     {
/* 1916: 791 */       throw new RuntimeException(localSQLException1.toString(), localSQLException1);
/* 1917:     */     }
/* 1918:     */   }
/* 1919:     */   
/* 1920:     */   static void init()
/* 1921:     */   {
/* 1922: 797 */     if (!Block.hasBlock(Genesis.GENESIS_BLOCK_ID))
/* 1923:     */     {
/* 1924: 798 */       Logger.logMessage("Genesis block not in database, starting from scratch");
/* 1925:     */       
/* 1926: 800 */       TreeMap localTreeMap = new TreeMap();
/* 1927:     */       try
/* 1928:     */       {
/* 1929: 804 */         for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++)
/* 1930:     */         {
/* 1931: 805 */           localObject1 = Transaction.newTransaction(0, (short)0, Genesis.CREATOR_PUBLIC_KEY, Genesis.GENESIS_RECIPIENTS[i], Genesis.GENESIS_AMOUNTS[i], 0, null, Genesis.GENESIS_SIGNATURES[i]);
/* 1932:     */           
/* 1933: 807 */           ((Transaction)localObject1).setIndex(transactionCounter.incrementAndGet());
/* 1934: 808 */           localTreeMap.put(((Transaction)localObject1).getId(), localObject1);
/* 1935:     */         }
/* 1936: 811 */         Block localBlock = new Block(-1, 0, null, localTreeMap.size(), 1000000000, 0, localTreeMap.size() * 128, null, Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE);
/* 1937:     */         
/* 1938: 813 */         localBlock.setIndex(blockCounter.incrementAndGet());
/* 1939:     */         
/* 1940: 815 */         Object localObject1 = (Transaction[])localTreeMap.values().toArray(new Transaction[localTreeMap.size()]);
/* 1941: 816 */         MessageDigest localMessageDigest = Crypto.sha256();
/* 1942: 817 */         for (int j = 0; j < localObject1.length; j++)
/* 1943:     */         {
/* 1944: 818 */           Object localObject2 = localObject1[j];
/* 1945: 819 */           localBlock.transactionIds[j] = localObject2.getId();
/* 1946: 820 */           localBlock.blockTransactions[j] = localObject2;
/* 1947: 821 */           localMessageDigest.update(localObject2.getBytes());
/* 1948:     */         }
/* 1949: 824 */         localBlock.setPayloadHash(localMessageDigest.digest());
/* 1950: 826 */         for (Transaction localTransaction : localBlock.blockTransactions) {
/* 1951: 827 */           localTransaction.setBlock(localBlock);
/* 1952:     */         }
/* 1953: 830 */         addBlock(localBlock);
/* 1954:     */       }
/* 1955:     */       catch (NxtException.ValidationException localValidationException)
/* 1956:     */       {
/* 1957: 833 */         Logger.logMessage(localValidationException.getMessage());
/* 1958: 834 */         System.exit(1);
/* 1959:     */       }
/* 1960:     */     }
/* 1961: 838 */     Logger.logMessage("Scanning blockchain...");
/* 1962: 839 */     scan();
/* 1963: 840 */     Logger.logMessage("...Done");
/* 1964:     */   }
/* 1965:     */   
/* 1966:     */   private static void processUnconfirmedTransactions(JSONObject paramJSONObject)
/* 1967:     */     throws NxtException.ValidationException
/* 1968:     */   {
/* 1969: 844 */     JSONArray localJSONArray = (JSONArray)paramJSONObject.get("unconfirmedTransactions");
/* 1970: 845 */     processTransactions(localJSONArray, true);
/* 1971:     */   }
/* 1972:     */   
/* 1973:     */   private static void processTransactions(JSONArray paramJSONArray, boolean paramBoolean)
/* 1974:     */     throws NxtException.ValidationException
/* 1975:     */   {
/* 1976: 850 */     JSONArray localJSONArray = new JSONArray();
/* 1977: 852 */     for (Object localObject1 = paramJSONArray.iterator(); ((Iterator)localObject1).hasNext();)
/* 1978:     */     {
/* 1979: 852 */       Object localObject2 = ((Iterator)localObject1).next();
/* 1980:     */       try
/* 1981:     */       {
/* 1982: 856 */         Transaction localTransaction = Transaction.getTransaction((JSONObject)localObject2);
/* 1983:     */         
/* 1984: 858 */         int i = Convert.getEpochTime();
/* 1985: 859 */         if ((localTransaction.getTimestamp() > i + 15) || (localTransaction.getExpiration() < i) || (localTransaction.getDeadline() <= 1440))
/* 1986:     */         {
/* 1987:     */           boolean bool;
/* 1988: 866 */           synchronized (Blockchain.class)
/* 1989:     */           {
/* 1990: 868 */             localObject3 = localTransaction.getId();
/* 1991: 869 */             if (((!Transaction.hasTransaction((Long)localObject3)) && (!unconfirmedTransactions.containsKey(localObject3)) && (!doubleSpendingTransactions.containsKey(localObject3)) && (!localTransaction.verify())) || 
/* 1992:     */             
/* 1993:     */ 
/* 1994:     */ 
/* 1995:     */ 
/* 1996: 874 */               (transactionHashes.containsKey(localTransaction.getHash()))) {
/* 1997:     */               continue;
/* 1998:     */             }
/* 1999: 878 */             bool = localTransaction.isDoubleSpending();
/* 2000:     */             
/* 2001: 880 */             localTransaction.setIndex(transactionCounter.incrementAndGet());
/* 2002: 882 */             if (bool)
/* 2003:     */             {
/* 2004: 884 */               doubleSpendingTransactions.put(localObject3, localTransaction);
/* 2005:     */             }
/* 2006:     */             else
/* 2007:     */             {
/* 2008: 888 */               unconfirmedTransactions.put(localObject3, localTransaction);
/* 2009: 890 */               if (!paramBoolean) {
/* 2010: 892 */                 localJSONArray.add(localObject2);
/* 2011:     */               }
/* 2012:     */             }
/* 2013:     */           }
/* 2014: 900 */           ??? = new JSONObject();
/* 2015: 901 */           ((JSONObject)???).put("response", "processNewData");
/* 2016:     */           
/* 2017: 903 */           Object localObject3 = new JSONArray();
/* 2018: 904 */           JSONObject localJSONObject = new JSONObject();
/* 2019: 905 */           localJSONObject.put("index", Integer.valueOf(localTransaction.getIndex()));
/* 2020: 906 */           localJSONObject.put("timestamp", Integer.valueOf(localTransaction.getTimestamp()));
/* 2021: 907 */           localJSONObject.put("deadline", Short.valueOf(localTransaction.getDeadline()));
/* 2022: 908 */           localJSONObject.put("recipient", Convert.convert(localTransaction.getRecipientId()));
/* 2023: 909 */           localJSONObject.put("amount", Integer.valueOf(localTransaction.getAmount()));
/* 2024: 910 */           localJSONObject.put("fee", Integer.valueOf(localTransaction.getFee()));
/* 2025: 911 */           localJSONObject.put("sender", Convert.convert(localTransaction.getSenderAccountId()));
/* 2026: 912 */           localJSONObject.put("id", localTransaction.getStringId());
/* 2027: 913 */           ((JSONArray)localObject3).add(localJSONObject);
/* 2028: 915 */           if (bool) {
/* 2029: 917 */             ((JSONObject)???).put("addedDoubleSpendingTransactions", localObject3);
/* 2030:     */           } else {
/* 2031: 921 */             ((JSONObject)???).put("addedUnconfirmedTransactions", localObject3);
/* 2032:     */           }
/* 2033: 925 */           User.sendToAll((JSONStreamAware)???);
/* 2034:     */         }
/* 2035:     */       }
/* 2036:     */       catch (RuntimeException localRuntimeException)
/* 2037:     */       {
/* 2038: 929 */         Logger.logMessage("Error processing transaction", localRuntimeException);
/* 2039:     */       }
/* 2040:     */     }
/* 2041: 935 */     if (localJSONArray.size() > 0)
/* 2042:     */     {
/* 2043: 937 */       localObject1 = new JSONObject();
/* 2044: 938 */       ((JSONObject)localObject1).put("requestType", "processTransactions");
/* 2045: 939 */       ((JSONObject)localObject1).put("transactions", localJSONArray);
/* 2046:     */       
/* 2047: 941 */       Peer.sendToSomePeers((JSONObject)localObject1);
/* 2048:     */     }
/* 2049:     */   }
/* 2050:     */   
/* 2051:     */   private static synchronized byte[] calculateTransactionsChecksum()
/* 2052:     */   {
/* 2053: 948 */     PriorityQueue localPriorityQueue = new PriorityQueue(getTransactionCount(), new Comparator()
/* 2054:     */     {
/* 2055:     */       public int compare(Transaction paramAnonymousTransaction1, Transaction paramAnonymousTransaction2)
/* 2056:     */       {
/* 2057: 951 */         long l1 = paramAnonymousTransaction1.getId().longValue();
/* 2058: 952 */         long l2 = paramAnonymousTransaction2.getId().longValue();
/* 2059: 953 */         return paramAnonymousTransaction1.getTimestamp() > paramAnonymousTransaction2.getTimestamp() ? 1 : paramAnonymousTransaction1.getTimestamp() < paramAnonymousTransaction2.getTimestamp() ? -1 : l1 > l2 ? 1 : l1 < l2 ? -1 : 0;
/* 2060:     */       }
/* 2061: 955 */     });
/* 2062: 956 */     Object localObject1 = getAllTransactions();Object localObject2 = null;
/* 2063:     */     try
/* 2064:     */     {
/* 2065: 957 */       while (((DbIterator)localObject1).hasNext()) {
/* 2066: 958 */         localPriorityQueue.add(((DbIterator)localObject1).next());
/* 2067:     */       }
/* 2068:     */     }
/* 2069:     */     catch (Throwable localThrowable2)
/* 2070:     */     {
/* 2071: 956 */       localObject2 = localThrowable2;throw localThrowable2;
/* 2072:     */     }
/* 2073:     */     finally
/* 2074:     */     {
/* 2075: 960 */       if (localObject1 != null) {
/* 2076: 960 */         if (localObject2 != null) {
/* 2077:     */           try
/* 2078:     */           {
/* 2079: 960 */             ((DbIterator)localObject1).close();
/* 2080:     */           }
/* 2081:     */           catch (Throwable localThrowable3)
/* 2082:     */           {
/* 2083: 960 */             localObject2.addSuppressed(localThrowable3);
/* 2084:     */           }
/* 2085:     */         } else {
/* 2086: 960 */           ((DbIterator)localObject1).close();
/* 2087:     */         }
/* 2088:     */       }
/* 2089:     */     }
/* 2090: 961 */     localObject1 = Crypto.sha256();
/* 2091: 962 */     while (!localPriorityQueue.isEmpty()) {
/* 2092: 963 */       ((MessageDigest)localObject1).update(((Transaction)localPriorityQueue.poll()).getBytes());
/* 2093:     */     }
/* 2094: 965 */     return ((MessageDigest)localObject1).digest();
/* 2095:     */   }
/* 2096:     */   
/* 2097:     */   private static void pushBlock(Block paramBlock, Transaction[] paramArrayOfTransaction)
/* 2098:     */     throws Blockchain.BlockNotAcceptedException
/* 2099:     */   {
/* 2100: 972 */     int i = Convert.getEpochTime();
/* 2101:     */     JSONArray localJSONArray1;
/* 2102:     */     JSONArray localJSONArray2;
/* 2103: 974 */     synchronized (Blockchain.class)
/* 2104:     */     {
/* 2105:     */       try
/* 2106:     */       {
/* 2107: 977 */         Block localBlock = (Block)lastBlock.get();
/* 2108: 979 */         if (!localBlock.getId().equals(paramBlock.getPreviousBlockId())) {
/* 2109: 980 */           throw new BlockOutOfOrderException("Previous block id doesn't match", null);
/* 2110:     */         }
/* 2111: 983 */         if (paramBlock.getVersion() != (localBlock.getHeight() < 30000 ? 1 : 2)) {
/* 2112: 984 */           throw new BlockNotAcceptedException("Invalid version " + paramBlock.getVersion(), null);
/* 2113:     */         }
/* 2114: 987 */         if (localBlock.getHeight() == 30000)
/* 2115:     */         {
/* 2116: 988 */           localObject1 = calculateTransactionsChecksum();
/* 2117: 989 */           if (CHECKSUM_TRANSPARENT_FORGING == null)
/* 2118:     */           {
/* 2119: 990 */             Logger.logMessage("Checksum calculated:\n" + Arrays.toString((byte[])localObject1));
/* 2120:     */           }
/* 2121:     */           else
/* 2122:     */           {
/* 2123: 991 */             if (!Arrays.equals((byte[])localObject1, CHECKSUM_TRANSPARENT_FORGING))
/* 2124:     */             {
/* 2125: 992 */               Logger.logMessage("Checksum failed at block 30000");
/* 2126: 993 */               throw new BlockNotAcceptedException("Checksum failed", null);
/* 2127:     */             }
/* 2128: 995 */             Logger.logMessage("Checksum passed at block 30000");
/* 2129:     */           }
/* 2130:     */         }
/* 2131: 999 */         if ((paramBlock.getVersion() != 1) && (!Arrays.equals(Crypto.sha256().digest(localBlock.getBytes()), paramBlock.getPreviousBlockHash()))) {
/* 2132:1000 */           throw new BlockNotAcceptedException("Previos block hash doesn't match", null);
/* 2133:     */         }
/* 2134:1002 */         if ((paramBlock.getTimestamp() > i + 15) || (paramBlock.getTimestamp() <= localBlock.getTimestamp())) {
/* 2135:1003 */           throw new BlockNotAcceptedException("Invalid timestamp: " + paramBlock.getTimestamp() + " current time is " + i + ", previous block timestamp is " + localBlock.getTimestamp(), null);
/* 2136:     */         }
/* 2137:1006 */         if ((paramBlock.getId().equals(Long.valueOf(0L))) || (Block.hasBlock(paramBlock.getId()))) {
/* 2138:1007 */           throw new BlockNotAcceptedException("Duplicate block or invalid id", null);
/* 2139:     */         }
/* 2140:1009 */         if ((!paramBlock.verifyGenerationSignature()) || (!paramBlock.verifyBlockSignature())) {
/* 2141:1010 */           throw new BlockNotAcceptedException("Signature verification failed", null);
/* 2142:     */         }
/* 2143:1013 */         paramBlock.setIndex(blockCounter.incrementAndGet());
/* 2144:     */         
/* 2145:1015 */         localObject1 = new HashMap();
/* 2146:1016 */         for (int j = 0; j < paramBlock.transactionIds.length; j++)
/* 2147:     */         {
/* 2148:1017 */           localObject2 = paramArrayOfTransaction[j];
/* 2149:1018 */           ((Transaction)localObject2).setIndex(transactionCounter.incrementAndGet());
/* 2150:1019 */           if (((Map)localObject1).put(paramBlock.transactionIds[j] =  = ((Transaction)localObject2).getId(), localObject2) != null) {
/* 2151:1020 */             throw new BlockNotAcceptedException("Block contains duplicate transactions: " + ((Transaction)localObject2).getStringId(), null);
/* 2152:     */           }
/* 2153:     */         }
/* 2154:1024 */         Arrays.sort(paramBlock.transactionIds);
/* 2155:     */         
/* 2156:1026 */         HashMap localHashMap1 = new HashMap();
/* 2157:1027 */         Object localObject2 = new HashMap();
/* 2158:1028 */         HashMap localHashMap2 = new HashMap();
/* 2159:1029 */         int k = 0;int m = 0;
/* 2160:1030 */         MessageDigest localMessageDigest = Crypto.sha256();
/* 2161:     */         Object localObject5;
/* 2162:1031 */         for (int n = 0; n < paramBlock.transactionIds.length; n++)
/* 2163:     */         {
/* 2164:1033 */           localObject4 = paramBlock.transactionIds[n];
/* 2165:1034 */           localObject5 = (Transaction)((Map)localObject1).get(localObject4);
/* 2166:1036 */           if ((((Transaction)localObject5).getTimestamp() > i + 15) || (((Transaction)localObject5).getTimestamp() > paramBlock.getTimestamp() + 15) || ((((Transaction)localObject5).getExpiration() < paramBlock.getTimestamp()) && (localBlock.getHeight() != 303))) {
/* 2167:1038 */             throw new BlockNotAcceptedException("Invalid transaction timestamp " + ((Transaction)localObject5).getTimestamp() + " for transaction " + ((Transaction)localObject5).getStringId() + ", current time is " + i + ", block timestamp is " + paramBlock.getTimestamp(), null);
/* 2168:     */           }
/* 2169:1042 */           if (Transaction.hasTransaction((Long)localObject4)) {
/* 2170:1043 */             throw new BlockNotAcceptedException("Transaction " + ((Transaction)localObject5).getStringId() + " is already in the blockchain", null);
/* 2171:     */           }
/* 2172:1045 */           if ((((Transaction)localObject5).getReferencedTransactionId() != null) && (!Transaction.hasTransaction(((Transaction)localObject5).getReferencedTransactionId())) && (((Map)localObject1).get(((Transaction)localObject5).getReferencedTransactionId()) == null)) {
/* 2173:1048 */             throw new BlockNotAcceptedException("Missing referenced transaction " + Convert.convert(((Transaction)localObject5).getReferencedTransactionId()) + " for transaction " + ((Transaction)localObject5).getStringId(), null);
/* 2174:     */           }
/* 2175:1051 */           if ((unconfirmedTransactions.get(localObject4) == null) && (!((Transaction)localObject5).verify())) {
/* 2176:1052 */             throw new BlockNotAcceptedException("Signature verification failed for transaction " + ((Transaction)localObject5).getStringId(), null);
/* 2177:     */           }
/* 2178:1054 */           if (((Transaction)localObject5).getId().equals(Long.valueOf(0L))) {
/* 2179:1055 */             throw new BlockNotAcceptedException("Invalid transaction id", null);
/* 2180:     */           }
/* 2181:1057 */           if (((Transaction)localObject5).isDuplicate(localHashMap1)) {
/* 2182:1058 */             throw new BlockNotAcceptedException("Transaction is a duplicate: " + ((Transaction)localObject5).getStringId(), null);
/* 2183:     */           }
/* 2184:1061 */           paramBlock.blockTransactions[n] = localObject5;
/* 2185:     */           
/* 2186:1063 */           k += ((Transaction)localObject5).getAmount();
/* 2187:     */           
/* 2188:1065 */           ((Transaction)localObject5).updateTotals((Map)localObject2, localHashMap2);
/* 2189:     */           
/* 2190:1067 */           m += ((Transaction)localObject5).getFee();
/* 2191:     */           
/* 2192:1069 */           localMessageDigest.update(((Transaction)localObject5).getBytes());
/* 2193:     */         }
/* 2194:1073 */         if ((k != paramBlock.getTotalAmount()) || (m != paramBlock.getTotalFee())) {
/* 2195:1074 */           throw new BlockNotAcceptedException("Total amount or fee don't match transaction totals", null);
/* 2196:     */         }
/* 2197:1076 */         if (!Arrays.equals(localMessageDigest.digest(), paramBlock.getPayloadHash())) {
/* 2198:1077 */           throw new BlockNotAcceptedException("Payload hash doesn't match", null);
/* 2199:     */         }
/* 2200:1079 */         for (Object localObject3 = ((Map)localObject2).entrySet().iterator(); ((Iterator)localObject3).hasNext();)
/* 2201:     */         {
/* 2202:1079 */           localObject4 = (Map.Entry)((Iterator)localObject3).next();
/* 2203:1080 */           localObject5 = Account.getAccount((Long)((Map.Entry)localObject4).getKey());
/* 2204:1081 */           if (((Account)localObject5).getBalance() < ((Long)((Map.Entry)localObject4).getValue()).longValue()) {
/* 2205:1082 */             throw new BlockNotAcceptedException("Not enough funds in sender account: " + Convert.convert(((Account)localObject5).getId()), null);
/* 2206:     */           }
/* 2207:     */         }
/* 2208:1086 */         for (localObject3 = localHashMap2.entrySet().iterator(); ((Iterator)localObject3).hasNext();)
/* 2209:     */         {
/* 2210:1086 */           localObject4 = (Map.Entry)((Iterator)localObject3).next();
/* 2211:1087 */           localObject5 = Account.getAccount((Long)((Map.Entry)localObject4).getKey());
/* 2212:1088 */           for (localIterator = ((Map)((Map.Entry)localObject4).getValue()).entrySet().iterator(); localIterator.hasNext();)
/* 2213:     */           {
/* 2214:1088 */             localObject6 = (Map.Entry)localIterator.next();
/* 2215:1089 */             localObject7 = (Long)((Map.Entry)localObject6).getKey();
/* 2216:1090 */             localObject8 = (Long)((Map.Entry)localObject6).getValue();
/* 2217:1091 */             if (((Account)localObject5).getAssetBalance((Long)localObject7).intValue() < ((Long)localObject8).longValue()) {
/* 2218:1092 */               throw new BlockNotAcceptedException("Asset balance not sufficient in sender account " + Convert.convert(((Account)localObject5).getId()), null);
/* 2219:     */             }
/* 2220:     */           }
/* 2221:     */         }
/* 2222:     */         Iterator localIterator;
/* 2223:1097 */         paramBlock.setHeight(localBlock.getHeight() + 1);
/* 2224:     */         
/* 2225:1099 */         localObject3 = null;
/* 2226:1100 */         for (localObject6 : paramBlock.blockTransactions)
/* 2227:     */         {
/* 2228:1101 */           ((Transaction)localObject6).setBlock(paramBlock);
/* 2229:1103 */           if ((transactionHashes.putIfAbsent(((Transaction)localObject6).getHash(), localObject6) != null) && (paramBlock.getHeight() != 58294))
/* 2230:     */           {
/* 2231:1104 */             localObject3 = localObject6;
/* 2232:1105 */             break;
/* 2233:     */           }
/* 2234:     */         }
/* 2235:1109 */         if (localObject3 != null)
/* 2236:     */         {
/* 2237:1110 */           for (localObject6 : paramBlock.blockTransactions) {
/* 2238:1111 */             if (!((Transaction)localObject6).equals(localObject3))
/* 2239:     */             {
/* 2240:1112 */               localObject7 = (Transaction)transactionHashes.get(((Transaction)localObject6).getHash());
/* 2241:1113 */               if ((localObject7 != null) && (((Transaction)localObject7).equals(localObject6))) {
/* 2242:1114 */                 transactionHashes.remove(((Transaction)localObject6).getHash());
/* 2243:     */               }
/* 2244:     */             }
/* 2245:     */           }
/* 2246:1118 */           throw new BlockNotAcceptedException("Duplicate hash of transaction " + ((Transaction)localObject3).getStringId(), null);
/* 2247:     */         }
/* 2248:1121 */         paramBlock.calculateBaseTarget();
/* 2249:     */         
/* 2250:1123 */         addBlock(paramBlock);
/* 2251:     */         
/* 2252:1125 */         paramBlock.apply();
/* 2253:     */         
/* 2254:1127 */         localJSONArray1 = new JSONArray();
/* 2255:1128 */         localJSONArray2 = new JSONArray();
/* 2256:1130 */         for (localObject4 = ((Map)localObject1).entrySet().iterator(); ((Iterator)localObject4).hasNext();)
/* 2257:     */         {
/* 2258:1130 */           Map.Entry localEntry = (Map.Entry)((Iterator)localObject4).next();
/* 2259:     */           
/* 2260:1132 */           Transaction localTransaction = (Transaction)localEntry.getValue();
/* 2261:     */           
/* 2262:1134 */           localObject6 = new JSONObject();
/* 2263:1135 */           ((JSONObject)localObject6).put("index", Integer.valueOf(localTransaction.getIndex()));
/* 2264:1136 */           ((JSONObject)localObject6).put("blockTimestamp", Integer.valueOf(paramBlock.getTimestamp()));
/* 2265:1137 */           ((JSONObject)localObject6).put("transactionTimestamp", Integer.valueOf(localTransaction.getTimestamp()));
/* 2266:1138 */           ((JSONObject)localObject6).put("sender", Convert.convert(localTransaction.getSenderAccountId()));
/* 2267:1139 */           ((JSONObject)localObject6).put("recipient", Convert.convert(localTransaction.getRecipientId()));
/* 2268:1140 */           ((JSONObject)localObject6).put("amount", Integer.valueOf(localTransaction.getAmount()));
/* 2269:1141 */           ((JSONObject)localObject6).put("fee", Integer.valueOf(localTransaction.getFee()));
/* 2270:1142 */           ((JSONObject)localObject6).put("id", localTransaction.getStringId());
/* 2271:1143 */           localJSONArray1.add(localObject6);
/* 2272:     */           
/* 2273:1145 */           localObject7 = (Transaction)unconfirmedTransactions.remove(localEntry.getKey());
/* 2274:1146 */           if (localObject7 != null)
/* 2275:     */           {
/* 2276:1147 */             localObject8 = new JSONObject();
/* 2277:1148 */             ((JSONObject)localObject8).put("index", Integer.valueOf(((Transaction)localObject7).getIndex()));
/* 2278:1149 */             localJSONArray2.add(localObject8);
/* 2279:     */             
/* 2280:1151 */             Account localAccount = Account.getAccount(((Transaction)localObject7).getSenderAccountId());
/* 2281:1152 */             localAccount.addToUnconfirmedBalance((((Transaction)localObject7).getAmount() + ((Transaction)localObject7).getFee()) * 100L);
/* 2282:     */           }
/* 2283:     */         }
/* 2284:     */       }
/* 2285:     */       catch (RuntimeException localRuntimeException)
/* 2286:     */       {
/* 2287:     */         Object localObject4;
/* 2288:     */         Object localObject6;
/* 2289:     */         Object localObject7;
/* 2290:     */         Object localObject8;
/* 2291:1160 */         Logger.logMessage("Error pushing block", localRuntimeException);
/* 2292:1161 */         throw new BlockNotAcceptedException(localRuntimeException.toString(), null);
/* 2293:     */       }
/* 2294:     */     }
/* 2295:1165 */     if (paramBlock.getTimestamp() >= i - 15)
/* 2296:     */     {
/* 2297:1167 */       ??? = paramBlock.getJSONObject();
/* 2298:1168 */       ((JSONObject)???).put("requestType", "processBlock");
/* 2299:     */       
/* 2300:1170 */       Peer.sendToSomePeers((JSONObject)???);
/* 2301:     */     }
/* 2302:1174 */     ??? = new JSONArray();
/* 2303:1175 */     JSONObject localJSONObject = new JSONObject();
/* 2304:1176 */     localJSONObject.put("index", Integer.valueOf(paramBlock.getIndex()));
/* 2305:1177 */     localJSONObject.put("timestamp", Integer.valueOf(paramBlock.getTimestamp()));
/* 2306:1178 */     localJSONObject.put("numberOfTransactions", Integer.valueOf(paramBlock.transactionIds.length));
/* 2307:1179 */     localJSONObject.put("totalAmount", Integer.valueOf(paramBlock.getTotalAmount()));
/* 2308:1180 */     localJSONObject.put("totalFee", Integer.valueOf(paramBlock.getTotalFee()));
/* 2309:1181 */     localJSONObject.put("payloadLength", Integer.valueOf(paramBlock.getPayloadLength()));
/* 2310:1182 */     localJSONObject.put("generator", Convert.convert(paramBlock.getGeneratorAccountId()));
/* 2311:1183 */     localJSONObject.put("height", Integer.valueOf(paramBlock.getHeight()));
/* 2312:1184 */     localJSONObject.put("version", Integer.valueOf(paramBlock.getVersion()));
/* 2313:1185 */     localJSONObject.put("block", paramBlock.getStringId());
/* 2314:1186 */     localJSONObject.put("baseTarget", BigInteger.valueOf(paramBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
/* 2315:1187 */     ((JSONArray)???).add(localJSONObject);
/* 2316:     */     
/* 2317:1189 */     Object localObject1 = new JSONObject();
/* 2318:1190 */     ((JSONObject)localObject1).put("response", "processNewData");
/* 2319:1191 */     ((JSONObject)localObject1).put("addedConfirmedTransactions", localJSONArray1);
/* 2320:1192 */     if (localJSONArray2.size() > 0) {
/* 2321:1193 */       ((JSONObject)localObject1).put("removedUnconfirmedTransactions", localJSONArray2);
/* 2322:     */     }
/* 2323:1195 */     ((JSONObject)localObject1).put("addedRecentBlocks", ???);
/* 2324:     */     
/* 2325:1197 */     User.sendToAll((JSONStreamAware)localObject1);
/* 2326:     */   }
/* 2327:     */   
/* 2328:     */   private static boolean popLastBlock()
/* 2329:     */     throws Transaction.UndoNotSupportedException
/* 2330:     */   {
/* 2331:     */     try
/* 2332:     */     {
/* 2333:1205 */       JSONObject localJSONObject1 = new JSONObject();
/* 2334:1206 */       localJSONObject1.put("response", "processNewData");
/* 2335:     */       
/* 2336:1208 */       JSONArray localJSONArray = new JSONArray();
/* 2337:     */       Block localBlock;
/* 2338:1212 */       synchronized (Blockchain.class)
/* 2339:     */       {
/* 2340:1214 */         localBlock = (Block)lastBlock.get();
/* 2341:     */         
/* 2342:1216 */         Logger.logDebugMessage("Will pop block " + localBlock.getStringId() + " at height " + localBlock.getHeight());
/* 2343:1217 */         if (localBlock.getId().equals(Genesis.GENESIS_BLOCK_ID)) {
/* 2344:1218 */           return false;
/* 2345:     */         }
/* 2346:1221 */         localObject1 = Block.findBlock(localBlock.getPreviousBlockId());
/* 2347:1222 */         if (localObject1 == null)
/* 2348:     */         {
/* 2349:1223 */           Logger.logMessage("Previous block is null");
/* 2350:1224 */           throw new IllegalStateException();
/* 2351:     */         }
/* 2352:1226 */         if (!lastBlock.compareAndSet(localBlock, localObject1))
/* 2353:     */         {
/* 2354:1227 */           Logger.logMessage("This block is no longer last block");
/* 2355:1228 */           throw new IllegalStateException();
/* 2356:     */         }
/* 2357:1231 */         Account localAccount = Account.getAccount(localBlock.getGeneratorAccountId());
/* 2358:1232 */         localAccount.addToBalanceAndUnconfirmedBalance(-localBlock.getTotalFee() * 100L);
/* 2359:1234 */         for (Transaction localTransaction1 : localBlock.blockTransactions)
/* 2360:     */         {
/* 2361:1236 */           Transaction localTransaction2 = (Transaction)transactionHashes.get(localTransaction1.getHash());
/* 2362:1237 */           if ((localTransaction2 != null) && (localTransaction2.equals(localTransaction1))) {
/* 2363:1238 */             transactionHashes.remove(localTransaction1.getHash());
/* 2364:     */           }
/* 2365:1241 */           unconfirmedTransactions.put(localTransaction1.getId(), localTransaction1);
/* 2366:     */           
/* 2367:1243 */           localTransaction1.undo();
/* 2368:     */           
/* 2369:1245 */           JSONObject localJSONObject2 = new JSONObject();
/* 2370:1246 */           localJSONObject2.put("index", Integer.valueOf(localTransaction1.getIndex()));
/* 2371:1247 */           localJSONObject2.put("timestamp", Integer.valueOf(localTransaction1.getTimestamp()));
/* 2372:1248 */           localJSONObject2.put("deadline", Short.valueOf(localTransaction1.getDeadline()));
/* 2373:1249 */           localJSONObject2.put("recipient", Convert.convert(localTransaction1.getRecipientId()));
/* 2374:1250 */           localJSONObject2.put("amount", Integer.valueOf(localTransaction1.getAmount()));
/* 2375:1251 */           localJSONObject2.put("fee", Integer.valueOf(localTransaction1.getFee()));
/* 2376:1252 */           localJSONObject2.put("sender", Convert.convert(localTransaction1.getSenderAccountId()));
/* 2377:1253 */           localJSONObject2.put("id", localTransaction1.getStringId());
/* 2378:1254 */           localJSONArray.add(localJSONObject2);
/* 2379:     */         }
/* 2380:1258 */         Block.deleteBlock(localBlock.getId());
/* 2381:     */       }
/* 2382:1262 */       ??? = new JSONArray();
/* 2383:1263 */       Object localObject1 = new JSONObject();
/* 2384:1264 */       ((JSONObject)localObject1).put("index", Integer.valueOf(localBlock.getIndex()));
/* 2385:1265 */       ((JSONObject)localObject1).put("timestamp", Integer.valueOf(localBlock.getTimestamp()));
/* 2386:1266 */       ((JSONObject)localObject1).put("numberOfTransactions", Integer.valueOf(localBlock.transactionIds.length));
/* 2387:1267 */       ((JSONObject)localObject1).put("totalAmount", Integer.valueOf(localBlock.getTotalAmount()));
/* 2388:1268 */       ((JSONObject)localObject1).put("totalFee", Integer.valueOf(localBlock.getTotalFee()));
/* 2389:1269 */       ((JSONObject)localObject1).put("payloadLength", Integer.valueOf(localBlock.getPayloadLength()));
/* 2390:1270 */       ((JSONObject)localObject1).put("generator", Convert.convert(localBlock.getGeneratorAccountId()));
/* 2391:1271 */       ((JSONObject)localObject1).put("height", Integer.valueOf(localBlock.getHeight()));
/* 2392:1272 */       ((JSONObject)localObject1).put("version", Integer.valueOf(localBlock.getVersion()));
/* 2393:1273 */       ((JSONObject)localObject1).put("block", localBlock.getStringId());
/* 2394:1274 */       ((JSONObject)localObject1).put("baseTarget", BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
/* 2395:1275 */       ((JSONArray)???).add(localObject1);
/* 2396:1276 */       localJSONObject1.put("addedOrphanedBlocks", ???);
/* 2397:1278 */       if (localJSONArray.size() > 0) {
/* 2398:1279 */         localJSONObject1.put("addedUnconfirmedTransactions", localJSONArray);
/* 2399:     */       }
/* 2400:1282 */       User.sendToAll(localJSONObject1);
/* 2401:     */     }
/* 2402:     */     catch (RuntimeException localRuntimeException)
/* 2403:     */     {
/* 2404:1285 */       Logger.logMessage("Error popping last block", localRuntimeException);
/* 2405:1286 */       return false;
/* 2406:     */     }
/* 2407:1288 */     return true;
/* 2408:     */   }
/* 2409:     */   
/* 2410:     */   private static synchronized void scan()
/* 2411:     */   {
/* 2412:1292 */     Account.clear();
/* 2413:1293 */     Alias.clear();
/* 2414:1294 */     Asset.clear();
/* 2415:1295 */     Order.clear();
/* 2416:1296 */     unconfirmedTransactions.clear();
/* 2417:1297 */     doubleSpendingTransactions.clear();
/* 2418:1298 */     nonBroadcastedTransactions.clear();
/* 2419:1299 */     transactionHashes.clear();
/* 2420:     */     try
/* 2421:     */     {
/* 2422:1300 */       Connection localConnection = Db.getConnection();Object localObject1 = null;
/* 2423:     */       try
/* 2424:     */       {
/* 2425:1300 */         PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM block ORDER BY db_id ASC");Object localObject2 = null;
/* 2426:     */         try
/* 2427:     */         {
/* 2428:1301 */           Long localLong = Genesis.GENESIS_BLOCK_ID;
/* 2429:     */           
/* 2430:1303 */           ResultSet localResultSet = localPreparedStatement.executeQuery();
/* 2431:1304 */           while (localResultSet.next())
/* 2432:     */           {
/* 2433:1305 */             Block localBlock = Block.getBlock(localConnection, localResultSet);
/* 2434:1306 */             if (!localBlock.getId().equals(localLong)) {
/* 2435:1307 */               throw new NxtException.ValidationException("Database blocks in the wrong order!");
/* 2436:     */             }
/* 2437:1309 */             lastBlock.set(localBlock);
/* 2438:1310 */             localBlock.apply();
/* 2439:1311 */             localLong = localBlock.getNextBlockId();
/* 2440:     */           }
/* 2441:     */         }
/* 2442:     */         catch (Throwable localThrowable4)
/* 2443:     */         {
/* 2444:1300 */           localObject2 = localThrowable4;throw localThrowable4;
/* 2445:     */         }
/* 2446:     */         finally {}
/* 2447:     */       }
/* 2448:     */       catch (Throwable localThrowable2)
/* 2449:     */       {
/* 2450:1300 */         localObject1 = localThrowable2;throw localThrowable2;
/* 2451:     */       }
/* 2452:     */       finally
/* 2453:     */       {
/* 2454:1313 */         if (localConnection != null) {
/* 2455:1313 */           if (localObject1 != null) {
/* 2456:     */             try
/* 2457:     */             {
/* 2458:1313 */               localConnection.close();
/* 2459:     */             }
/* 2460:     */             catch (Throwable localThrowable6)
/* 2461:     */             {
/* 2462:1313 */               localObject1.addSuppressed(localThrowable6);
/* 2463:     */             }
/* 2464:     */           } else {
/* 2465:1313 */             localConnection.close();
/* 2466:     */           }
/* 2467:     */         }
/* 2468:     */       }
/* 2469:     */     }
/* 2470:     */     catch (NxtException.ValidationException|SQLException localValidationException)
/* 2471:     */     {
/* 2472:1314 */       throw new RuntimeException(localValidationException.toString(), localValidationException);
/* 2473:     */     }
/* 2474:     */   }
/* 2475:     */   
/* 2476:     */   private static void generateBlock(String paramString)
/* 2477:     */   {
/* 2478:1320 */     TreeSet localTreeSet = new TreeSet();
/* 2479:1322 */     for (Object localObject1 = unconfirmedTransactions.values().iterator(); ((Iterator)localObject1).hasNext();)
/* 2480:     */     {
/* 2481:1322 */       localObject2 = (Transaction)((Iterator)localObject1).next();
/* 2482:1323 */       if ((((Transaction)localObject2).getReferencedTransactionId() == null) || (Transaction.hasTransaction(((Transaction)localObject2).getReferencedTransactionId()))) {
/* 2483:1324 */         localTreeSet.add(localObject2);
/* 2484:     */       }
/* 2485:     */     }
/* 2486:1328 */     localObject1 = new HashMap();
/* 2487:1329 */     Object localObject2 = new HashMap();
/* 2488:1330 */     HashMap localHashMap = new HashMap();
/* 2489:     */     
/* 2490:1332 */     int i = 0;
/* 2491:1333 */     int j = 0;
/* 2492:1334 */     int k = 0;
/* 2493:     */     
/* 2494:1336 */     int m = Convert.getEpochTime();
/* 2495:     */     Object localObject3;
/* 2496:1338 */     while (k <= 32640)
/* 2497:     */     {
/* 2498:1340 */       int n = ((Map)localObject1).size();
/* 2499:1342 */       for (localObject3 = localTreeSet.iterator(); ((Iterator)localObject3).hasNext();)
/* 2500:     */       {
/* 2501:1342 */         localObject4 = (Transaction)((Iterator)localObject3).next();
/* 2502:     */         
/* 2503:1344 */         int i1 = ((Transaction)localObject4).getSize();
/* 2504:1345 */         if ((((Map)localObject1).get(((Transaction)localObject4).getId()) == null) && (k + i1 <= 32640))
/* 2505:     */         {
/* 2506:1347 */           localObject5 = ((Transaction)localObject4).getSenderAccountId();
/* 2507:1348 */           localObject6 = (Long)localHashMap.get(localObject5);
/* 2508:1349 */           if (localObject6 == null) {
/* 2509:1350 */             localObject6 = Long.valueOf(0L);
/* 2510:     */           }
/* 2511:1353 */           long l = (((Transaction)localObject4).getAmount() + ((Transaction)localObject4).getFee()) * 100L;
/* 2512:1354 */           if (((Long)localObject6).longValue() + l <= Account.getAccount((Long)localObject5).getBalance()) {
/* 2513:1356 */             if ((((Transaction)localObject4).getTimestamp() <= m + 15) && (((Transaction)localObject4).getExpiration() >= m) && 
/* 2514:     */             
/* 2515:     */ 
/* 2516:     */ 
/* 2517:1360 */               (!((Transaction)localObject4).isDuplicate((Map)localObject2)))
/* 2518:     */             {
/* 2519:1364 */               localHashMap.put(localObject5, Long.valueOf(((Long)localObject6).longValue() + l));
/* 2520:     */               
/* 2521:1366 */               ((Map)localObject1).put(((Transaction)localObject4).getId(), localObject4);
/* 2522:1367 */               k += i1;
/* 2523:1368 */               i += ((Transaction)localObject4).getAmount();
/* 2524:1369 */               j += ((Transaction)localObject4).getFee();
/* 2525:     */             }
/* 2526:     */           }
/* 2527:     */         }
/* 2528:     */       }
/* 2529:1377 */       if (((Map)localObject1).size() == n) {
/* 2530:     */         break;
/* 2531:     */       }
/* 2532:     */     }
/* 2533:1385 */     byte[] arrayOfByte1 = Crypto.getPublicKey(paramString);
/* 2534:     */     
/* 2535:     */ 
/* 2536:1388 */     Object localObject4 = (Block)lastBlock.get();
/* 2537:     */     try
/* 2538:     */     {
/* 2539:1391 */       if (((Block)localObject4).getHeight() < 30000)
/* 2540:     */       {
/* 2541:1393 */         localObject3 = new Block(1, m, ((Block)localObject4).getId(), ((Map)localObject1).size(), i, j, k, null, arrayOfByte1, null, new byte[64]);
/* 2542:     */       }
/* 2543:     */       else
/* 2544:     */       {
/* 2545:1398 */         byte[] arrayOfByte2 = Crypto.sha256().digest(((Block)localObject4).getBytes());
/* 2546:1399 */         localObject3 = new Block(2, m, ((Block)localObject4).getId(), ((Map)localObject1).size(), i, j, k, null, arrayOfByte1, null, new byte[64], arrayOfByte2);
/* 2547:     */       }
/* 2548:     */     }
/* 2549:     */     catch (NxtException.ValidationException localValidationException)
/* 2550:     */     {
/* 2551:1405 */       Logger.logMessage("Error generating block", localValidationException);
/* 2552:1406 */       return;
/* 2553:     */     }
/* 2554:1409 */     int i2 = 0;
/* 2555:1410 */     for (Object localObject5 = ((Map)localObject1).keySet().iterator(); ((Iterator)localObject5).hasNext();)
/* 2556:     */     {
/* 2557:1410 */       localObject6 = (Long)((Iterator)localObject5).next();
/* 2558:1411 */       ((Block)localObject3).transactionIds[(i2++)] = localObject6;
/* 2559:     */     }
/* 2560:1414 */     Arrays.sort(((Block)localObject3).transactionIds);
/* 2561:1415 */     localObject5 = Crypto.sha256();
/* 2562:1416 */     for (i2 = 0; i2 < ((Block)localObject3).transactionIds.length; i2++)
/* 2563:     */     {
/* 2564:1417 */       localObject6 = (Transaction)((Map)localObject1).get(localObject3.transactionIds[i2]);
/* 2565:1418 */       ((MessageDigest)localObject5).update(((Transaction)localObject6).getBytes());
/* 2566:1419 */       ((Block)localObject3).blockTransactions[i2] = localObject6;
/* 2567:     */     }
/* 2568:1421 */     ((Block)localObject3).setPayloadHash(((MessageDigest)localObject5).digest());
/* 2569:1423 */     if (((Block)localObject4).getHeight() < 30000)
/* 2570:     */     {
/* 2571:1425 */       ((Block)localObject3).setGenerationSignature(Crypto.sign(((Block)localObject4).getGenerationSignature(), paramString));
/* 2572:     */     }
/* 2573:     */     else
/* 2574:     */     {
/* 2575:1429 */       ((MessageDigest)localObject5).update(((Block)localObject4).getGenerationSignature());
/* 2576:1430 */       ((Block)localObject3).setGenerationSignature(((MessageDigest)localObject5).digest(arrayOfByte1));
/* 2577:     */     }
/* 2578:1434 */     Object localObject6 = ((Block)localObject3).getBytes();
/* 2579:1435 */     byte[] arrayOfByte3 = new byte[localObject6.length - 64];
/* 2580:1436 */     System.arraycopy(localObject6, 0, arrayOfByte3, 0, arrayOfByte3.length);
/* 2581:1437 */     ((Block)localObject3).setBlockSignature(Crypto.sign(arrayOfByte3, paramString));
/* 2582:1439 */     if ((((Block)localObject3).verifyBlockSignature()) && (((Block)localObject3).verifyGenerationSignature()))
/* 2583:     */     {
/* 2584:1441 */       JSONObject localJSONObject = ((Block)localObject3).getJSONObject();
/* 2585:1442 */       localJSONObject.put("requestType", "processBlock");
/* 2586:1443 */       Peer.sendToSomePeers(localJSONObject);
/* 2587:     */     }
/* 2588:     */     else
/* 2589:     */     {
/* 2590:1447 */       Logger.logMessage("Generated an incorrect block. Waiting for the next one...");
/* 2591:     */     }
/* 2592:     */   }
/* 2593:     */   
/* 2594:     */   static void purgeExpiredHashes(int paramInt)
/* 2595:     */   {
/* 2596:1454 */     Iterator localIterator = transactionHashes.entrySet().iterator();
/* 2597:1455 */     while (localIterator.hasNext()) {
/* 2598:1456 */       if (((Transaction)((Map.Entry)localIterator.next()).getValue()).getExpiration() < paramInt) {
/* 2599:1457 */         localIterator.remove();
/* 2600:     */       }
/* 2601:     */     }
/* 2602:     */   }
/* 2603:     */   
/* 2604:     */   public static class BlockNotAcceptedException
/* 2605:     */     extends NxtException
/* 2606:     */   {
/* 2607:     */     private BlockNotAcceptedException(String paramString)
/* 2608:     */     {
/* 2609:1465 */       super();
/* 2610:     */     }
/* 2611:     */   }
/* 2612:     */   
/* 2613:     */   public static class BlockOutOfOrderException
/* 2614:     */     extends Blockchain.BlockNotAcceptedException
/* 2615:     */   {
/* 2616:     */     private BlockOutOfOrderException(String paramString)
/* 2617:     */     {
/* 2618:1473 */       super(null);
/* 2619:     */     }
/* 2620:     */   }
/* 2621:     */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Blockchain
 * JD-Core Version:    0.7.0.1
 */