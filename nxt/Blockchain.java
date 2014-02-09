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
/*  264: 284 */                               if (!Blockchain.pushBlock(localBlock2, arrayOfTransaction))
/*  265:     */                               {
/*  266: 285 */                                 Logger.logDebugMessage("Failed to accept block received from " + localPeer.getPeerAddress() + ", blacklisting");
/*  267: 286 */                                 localPeer.blacklist();
/*  268: 287 */                                 return;
/*  269:     */                               }
/*  270:     */                             }
/*  271:     */                             catch (NxtException.ValidationException localValidationException2)
/*  272:     */                             {
/*  273: 290 */                               localPeer.blacklist(localValidationException2);
/*  274: 291 */                               return;
/*  275:     */                             }
/*  276:     */                           }
/*  277: 294 */                           else if ((!Block.hasBlock(localBlock2.getId())) && (localBlock2.transactionIds.length <= 255))
/*  278:     */                           {
/*  279: 296 */                             ((LinkedList)localObject4).add(localBlock2);
/*  280:     */                             
/*  281: 298 */                             localJSONArray3 = (JSONArray)localJSONObject2.get("transactions");
/*  282:     */                             try
/*  283:     */                             {
/*  284: 300 */                               for (int m = 0; m < localBlock2.transactionIds.length; m++)
/*  285:     */                               {
/*  286: 302 */                                 Transaction localTransaction = Transaction.getTransaction((JSONObject)localJSONArray3.get(m));
/*  287: 303 */                                 localBlock2.transactionIds[m] = localTransaction.getId();
/*  288: 304 */                                 localBlock2.blockTransactions[m] = localTransaction;
/*  289: 305 */                                 ((HashMap)localObject5).put(localBlock2.transactionIds[m], localTransaction);
/*  290:     */                               }
/*  291:     */                             }
/*  292:     */                             catch (NxtException.ValidationException localValidationException3)
/*  293:     */                             {
/*  294: 309 */                               localPeer.blacklist(localValidationException3);
/*  295: 310 */                               return;
/*  296:     */                             }
/*  297:     */                           }
/*  298:     */                         }
/*  299:     */                       }
/*  300:     */                     }
/*  301: 320 */                     if ((!((LinkedList)localObject4).isEmpty()) && (((Block)Blockchain.lastBlock.get()).getHeight() - localBlock1.getHeight() < 720)) {
/*  302: 322 */                       synchronized (Blockchain.class)
/*  303:     */                       {
/*  304: 323 */                         localBigInteger1 = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty();
/*  305:     */                         for (;;)
/*  306:     */                         {
/*  307:     */                           int k;
/*  308:     */                           try
/*  309:     */                           {
/*  310: 327 */                             while ((!((Block)Blockchain.lastBlock.get()).getId().equals(localObject1)) && (Blockchain.access$500())) {}
/*  311: 329 */                             if (((Block)Blockchain.lastBlock.get()).getId().equals(localObject1)) {
/*  312: 330 */                               for (??? = ((LinkedList)localObject4).iterator(); ((Iterator)???).hasNext();)
/*  313:     */                               {
/*  314: 330 */                                 localObject7 = (Block)((Iterator)???).next();
/*  315: 331 */                                 if ((((Block)Blockchain.lastBlock.get()).getId().equals(((Block)localObject7).getPreviousBlockId())) && 
/*  316: 332 */                                   (!Blockchain.pushBlock((Block)localObject7, ((Block)localObject7).blockTransactions))) {
/*  317:     */                                   break;
/*  318:     */                                 }
/*  319:     */                               }
/*  320:     */                             }
/*  321: 339 */                             k = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty().compareTo(localBigInteger1) < 0 ? 1 : 0;
/*  322: 340 */                             if (k != 0)
/*  323:     */                             {
/*  324: 341 */                               Logger.logDebugMessage("Rescan caused by peer " + localPeer.getPeerAddress() + ", blacklisting");
/*  325: 342 */                               localPeer.blacklist();
/*  326:     */                             }
/*  327:     */                           }
/*  328:     */                           catch (Transaction.UndoNotSupportedException localUndoNotSupportedException)
/*  329:     */                           {
/*  330: 345 */                             Logger.logDebugMessage(localUndoNotSupportedException.getMessage());
/*  331: 346 */                             Logger.logDebugMessage("Popping off last block not possible, will do a rescan");
/*  332: 347 */                             k = 1;
/*  333:     */                           }
/*  334:     */                         }
/*  335: 350 */                         if (k != 0)
/*  336:     */                         {
/*  337: 352 */                           if (localBlock1.getNextBlockId() != null) {
/*  338: 353 */                             Block.deleteBlock(localBlock1.getNextBlockId());
/*  339:     */                           }
/*  340: 355 */                           Logger.logMessage("Re-scanning blockchain...");
/*  341: 356 */                           Blockchain.access$600();
/*  342: 357 */                           Logger.logMessage("...Done");
/*  343:     */                         }
/*  344:     */                       }
/*  345:     */                     }
/*  346:     */                   }
/*  347:     */                 }
/*  348:     */               }
/*  349:     */             }
/*  350:     */           }
/*  351:     */         }
/*  352:     */         catch (Exception localException)
/*  353:     */         {
/*  354: 368 */           Logger.logDebugMessage("Error in milestone blocks processing thread", localException);
/*  355:     */         }
/*  356:     */       }
/*  357:     */       catch (Throwable localThrowable)
/*  358:     */       {
/*  359: 371 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  360: 372 */         localThrowable.printStackTrace();
/*  361: 373 */         System.exit(1);
/*  362:     */       }
/*  363:     */     }
/*  364:     */   };
/*  365: 380 */   static final Runnable generateBlockThread = new Runnable()
/*  366:     */   {
/*  367: 382 */     private final ConcurrentMap<Account, Block> lastBlocks = new ConcurrentHashMap();
/*  368: 383 */     private final ConcurrentMap<Account, BigInteger> hits = new ConcurrentHashMap();
/*  369:     */     
/*  370:     */     public void run()
/*  371:     */     {
/*  372:     */       try
/*  373:     */       {
/*  374:     */         try
/*  375:     */         {
/*  376: 391 */           HashMap localHashMap = new HashMap();
/*  377: 392 */           for (localIterator = User.getAllUsers().iterator(); localIterator.hasNext();)
/*  378:     */           {
/*  379: 392 */             localObject1 = (User)localIterator.next();
/*  380: 393 */             if (((User)localObject1).getSecretPhrase() != null)
/*  381:     */             {
/*  382: 394 */               localAccount = Account.getAccount(((User)localObject1).getPublicKey());
/*  383: 395 */               if ((localAccount != null) && (localAccount.getEffectiveBalance() > 0)) {
/*  384: 396 */                 localHashMap.put(localAccount, localObject1);
/*  385:     */               }
/*  386:     */             }
/*  387:     */           }
/*  388: 401 */           for (localIterator = localHashMap.entrySet().iterator(); localIterator.hasNext();)
/*  389:     */           {
/*  390: 401 */             localObject1 = (Map.Entry)localIterator.next();
/*  391:     */             
/*  392: 403 */             localAccount = (Account)((Map.Entry)localObject1).getKey();
/*  393: 404 */             User localUser = (User)((Map.Entry)localObject1).getValue();
/*  394: 405 */             Block localBlock = (Block)Blockchain.lastBlock.get();
/*  395: 406 */             if (this.lastBlocks.get(localAccount) != localBlock)
/*  396:     */             {
/*  397: 408 */               long l = localAccount.getEffectiveBalance();
/*  398: 409 */               if (l > 0L)
/*  399:     */               {
/*  400: 412 */                 MessageDigest localMessageDigest = Crypto.sha256();
/*  401:     */                 byte[] arrayOfByte;
/*  402: 414 */                 if (localBlock.getHeight() < 30000)
/*  403:     */                 {
/*  404: 416 */                   localObject2 = Crypto.sign(localBlock.getGenerationSignature(), localUser.getSecretPhrase());
/*  405: 417 */                   arrayOfByte = localMessageDigest.digest((byte[])localObject2);
/*  406:     */                 }
/*  407:     */                 else
/*  408:     */                 {
/*  409: 421 */                   localMessageDigest.update(localBlock.getGenerationSignature());
/*  410: 422 */                   arrayOfByte = localMessageDigest.digest(localUser.getPublicKey());
/*  411:     */                 }
/*  412: 425 */                 Object localObject2 = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
/*  413:     */                 
/*  414: 427 */                 this.lastBlocks.put(localAccount, localBlock);
/*  415: 428 */                 this.hits.put(localAccount, localObject2);
/*  416:     */                 
/*  417: 430 */                 JSONObject localJSONObject = new JSONObject();
/*  418: 431 */                 localJSONObject.put("response", "setBlockGenerationDeadline");
/*  419: 432 */                 localJSONObject.put("deadline", Long.valueOf(((BigInteger)localObject2).divide(BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(l))).longValue() - (Convert.getEpochTime() - localBlock.getTimestamp())));
/*  420:     */                 
/*  421: 434 */                 localUser.send(localJSONObject);
/*  422:     */               }
/*  423:     */             }
/*  424:     */             else
/*  425:     */             {
/*  426: 438 */               int i = Convert.getEpochTime() - localBlock.getTimestamp();
/*  427: 439 */               if (i > 0)
/*  428:     */               {
/*  429: 441 */                 BigInteger localBigInteger = BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance())).multiply(BigInteger.valueOf(i));
/*  430: 442 */                 if (((BigInteger)this.hits.get(localAccount)).compareTo(localBigInteger) < 0) {
/*  431: 444 */                   Blockchain.generateBlock(localUser.getSecretPhrase());
/*  432:     */                 }
/*  433:     */               }
/*  434:     */             }
/*  435:     */           }
/*  436:     */         }
/*  437:     */         catch (Exception localException)
/*  438:     */         {
/*  439:     */           Iterator localIterator;
/*  440:     */           Object localObject1;
/*  441:     */           Account localAccount;
/*  442: 453 */           Logger.logDebugMessage("Error in block generation thread", localException);
/*  443:     */         }
/*  444:     */       }
/*  445:     */       catch (Throwable localThrowable)
/*  446:     */       {
/*  447: 456 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  448: 457 */         localThrowable.printStackTrace();
/*  449: 458 */         System.exit(1);
/*  450:     */       }
/*  451:     */     }
/*  452:     */   };
/*  453: 465 */   static final Runnable rebroadcastTransactionsThread = new Runnable()
/*  454:     */   {
/*  455:     */     public void run()
/*  456:     */     {
/*  457:     */       try
/*  458:     */       {
/*  459:     */         try
/*  460:     */         {
/*  461: 472 */           JSONArray localJSONArray = new JSONArray();
/*  462: 474 */           for (Object localObject = Blockchain.nonBroadcastedTransactions.values().iterator(); ((Iterator)localObject).hasNext();)
/*  463:     */           {
/*  464: 474 */             Transaction localTransaction = (Transaction)((Iterator)localObject).next();
/*  465: 476 */             if ((Blockchain.unconfirmedTransactions.get(localTransaction.getId()) == null) && (!Transaction.hasTransaction(localTransaction.getId()))) {
/*  466: 478 */               localJSONArray.add(localTransaction.getJSONObject());
/*  467:     */             } else {
/*  468: 482 */               Blockchain.nonBroadcastedTransactions.remove(localTransaction.getId());
/*  469:     */             }
/*  470:     */           }
/*  471: 488 */           if (localJSONArray.size() > 0)
/*  472:     */           {
/*  473: 490 */             localObject = new JSONObject();
/*  474: 491 */             ((JSONObject)localObject).put("requestType", "processTransactions");
/*  475: 492 */             ((JSONObject)localObject).put("transactions", localJSONArray);
/*  476:     */             
/*  477: 494 */             Peer.sendToSomePeers((JSONObject)localObject);
/*  478:     */           }
/*  479:     */         }
/*  480:     */         catch (Exception localException)
/*  481:     */         {
/*  482: 499 */           Logger.logDebugMessage("Error in transaction re-broadcasting thread", localException);
/*  483:     */         }
/*  484:     */       }
/*  485:     */       catch (Throwable localThrowable)
/*  486:     */       {
/*  487: 502 */         Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
/*  488: 503 */         localThrowable.printStackTrace();
/*  489: 504 */         System.exit(1);
/*  490:     */       }
/*  491:     */     }
/*  492:     */   };
/*  493:     */   
/*  494:     */   public static DbIterator<Block> getAllBlocks()
/*  495:     */   {
/*  496: 512 */     Connection localConnection = null;
/*  497:     */     try
/*  498:     */     {
/*  499: 514 */       localConnection = Db.getConnection();
/*  500: 515 */       PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM block ORDER BY db_id ASC");
/*  501: 516 */       new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
/*  502:     */       {
/*  503:     */         public Block get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
/*  504:     */           throws NxtException.ValidationException
/*  505:     */         {
/*  506: 519 */           return Block.getBlock(paramAnonymousConnection, paramAnonymousResultSet);
/*  507:     */         }
/*  508:     */       });
/*  509:     */     }
/*  510:     */     catch (SQLException localSQLException)
/*  511:     */     {
/*  512: 523 */       DbUtils.close(new AutoCloseable[] { localConnection });
/*  513: 524 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/*  514:     */     }
/*  515:     */   }
/*  516:     */   
/*  517:     */   public static DbIterator<Block> getAllBlocks(Account paramAccount, int paramInt)
/*  518:     */   {
/*  519: 529 */     Connection localConnection = null;
/*  520:     */     try
/*  521:     */     {
/*  522: 531 */       localConnection = Db.getConnection();
/*  523: 532 */       PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM block WHERE timestamp >= ? AND generator_public_key = ? ORDER BY db_id ASC");
/*  524: 533 */       localPreparedStatement.setInt(1, paramInt);
/*  525: 534 */       localPreparedStatement.setBytes(2, paramAccount.getPublicKey());
/*  526: 535 */       new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
/*  527:     */       {
/*  528:     */         public Block get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
/*  529:     */           throws NxtException.ValidationException
/*  530:     */         {
/*  531: 538 */           return Block.getBlock(paramAnonymousConnection, paramAnonymousResultSet);
/*  532:     */         }
/*  533:     */       });
/*  534:     */     }
/*  535:     */     catch (SQLException localSQLException)
/*  536:     */     {
/*  537: 542 */       DbUtils.close(new AutoCloseable[] { localConnection });
/*  538: 543 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/*  539:     */     }
/*  540:     */   }
/*  541:     */   
/*  542:     */   /* Error */
/*  543:     */   public static int getBlockCount()
/*  544:     */   {
/*  545:     */     // Byte code:
/*  546:     */     //   0: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
/*  547:     */     //   3: astore_0
/*  548:     */     //   4: aconst_null
/*  549:     */     //   5: astore_1
/*  550:     */     //   6: aload_0
/*  551:     */     //   7: ldc 29
/*  552:     */     //   9: invokeinterface 12 2 0
/*  553:     */     //   14: astore_2
/*  554:     */     //   15: aconst_null
/*  555:     */     //   16: astore_3
/*  556:     */     //   17: aload_2
/*  557:     */     //   18: invokeinterface 30 1 0
/*  558:     */     //   23: astore 4
/*  559:     */     //   25: aload 4
/*  560:     */     //   27: invokeinterface 31 1 0
/*  561:     */     //   32: pop
/*  562:     */     //   33: aload 4
/*  563:     */     //   35: iconst_1
/*  564:     */     //   36: invokeinterface 32 2 0
/*  565:     */     //   41: istore 5
/*  566:     */     //   43: aload_2
/*  567:     */     //   44: ifnull +33 -> 77
/*  568:     */     //   47: aload_3
/*  569:     */     //   48: ifnull +23 -> 71
/*  570:     */     //   51: aload_2
/*  571:     */     //   52: invokeinterface 33 1 0
/*  572:     */     //   57: goto +20 -> 77
/*  573:     */     //   60: astore 6
/*  574:     */     //   62: aload_3
/*  575:     */     //   63: aload 6
/*  576:     */     //   65: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  577:     */     //   68: goto +9 -> 77
/*  578:     */     //   71: aload_2
/*  579:     */     //   72: invokeinterface 33 1 0
/*  580:     */     //   77: aload_0
/*  581:     */     //   78: ifnull +33 -> 111
/*  582:     */     //   81: aload_1
/*  583:     */     //   82: ifnull +23 -> 105
/*  584:     */     //   85: aload_0
/*  585:     */     //   86: invokeinterface 36 1 0
/*  586:     */     //   91: goto +20 -> 111
/*  587:     */     //   94: astore 6
/*  588:     */     //   96: aload_1
/*  589:     */     //   97: aload 6
/*  590:     */     //   99: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  591:     */     //   102: goto +9 -> 111
/*  592:     */     //   105: aload_0
/*  593:     */     //   106: invokeinterface 36 1 0
/*  594:     */     //   111: iload 5
/*  595:     */     //   113: ireturn
/*  596:     */     //   114: astore 4
/*  597:     */     //   116: aload 4
/*  598:     */     //   118: astore_3
/*  599:     */     //   119: aload 4
/*  600:     */     //   121: athrow
/*  601:     */     //   122: astore 7
/*  602:     */     //   124: aload_2
/*  603:     */     //   125: ifnull +33 -> 158
/*  604:     */     //   128: aload_3
/*  605:     */     //   129: ifnull +23 -> 152
/*  606:     */     //   132: aload_2
/*  607:     */     //   133: invokeinterface 33 1 0
/*  608:     */     //   138: goto +20 -> 158
/*  609:     */     //   141: astore 8
/*  610:     */     //   143: aload_3
/*  611:     */     //   144: aload 8
/*  612:     */     //   146: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  613:     */     //   149: goto +9 -> 158
/*  614:     */     //   152: aload_2
/*  615:     */     //   153: invokeinterface 33 1 0
/*  616:     */     //   158: aload 7
/*  617:     */     //   160: athrow
/*  618:     */     //   161: astore_2
/*  619:     */     //   162: aload_2
/*  620:     */     //   163: astore_1
/*  621:     */     //   164: aload_2
/*  622:     */     //   165: athrow
/*  623:     */     //   166: astore 9
/*  624:     */     //   168: aload_0
/*  625:     */     //   169: ifnull +33 -> 202
/*  626:     */     //   172: aload_1
/*  627:     */     //   173: ifnull +23 -> 196
/*  628:     */     //   176: aload_0
/*  629:     */     //   177: invokeinterface 36 1 0
/*  630:     */     //   182: goto +20 -> 202
/*  631:     */     //   185: astore 10
/*  632:     */     //   187: aload_1
/*  633:     */     //   188: aload 10
/*  634:     */     //   190: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  635:     */     //   193: goto +9 -> 202
/*  636:     */     //   196: aload_0
/*  637:     */     //   197: invokeinterface 36 1 0
/*  638:     */     //   202: aload 9
/*  639:     */     //   204: athrow
/*  640:     */     //   205: astore_0
/*  641:     */     //   206: new 20	java/lang/RuntimeException
/*  642:     */     //   209: dup
/*  643:     */     //   210: aload_0
/*  644:     */     //   211: invokevirtual 21	java/sql/SQLException:toString	()Ljava/lang/String;
/*  645:     */     //   214: aload_0
/*  646:     */     //   215: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/*  647:     */     //   218: athrow
/*  648:     */     // Line number table:
/*  649:     */     //   Java source line #548	-> byte code offset #0
/*  650:     */     //   Java source line #549	-> byte code offset #17
/*  651:     */     //   Java source line #550	-> byte code offset #25
/*  652:     */     //   Java source line #551	-> byte code offset #33
/*  653:     */     //   Java source line #552	-> byte code offset #43
/*  654:     */     //   Java source line #548	-> byte code offset #114
/*  655:     */     //   Java source line #552	-> byte code offset #122
/*  656:     */     //   Java source line #548	-> byte code offset #161
/*  657:     */     //   Java source line #552	-> byte code offset #166
/*  658:     */     //   Java source line #553	-> byte code offset #206
/*  659:     */     // Local variable table:
/*  660:     */     //   start	length	slot	name	signature
/*  661:     */     //   3	194	0	localConnection	Connection
/*  662:     */     //   205	10	0	localSQLException	SQLException
/*  663:     */     //   5	183	1	localObject1	Object
/*  664:     */     //   14	139	2	localPreparedStatement	PreparedStatement
/*  665:     */     //   161	4	2	localThrowable1	Throwable
/*  666:     */     //   16	128	3	localObject2	Object
/*  667:     */     //   23	11	4	localResultSet	ResultSet
/*  668:     */     //   114	6	4	localThrowable2	Throwable
/*  669:     */     //   60	4	6	localThrowable3	Throwable
/*  670:     */     //   94	4	6	localThrowable4	Throwable
/*  671:     */     //   122	37	7	localObject3	Object
/*  672:     */     //   141	4	8	localThrowable5	Throwable
/*  673:     */     //   166	37	9	localObject4	Object
/*  674:     */     //   185	4	10	localThrowable6	Throwable
/*  675:     */     // Exception table:
/*  676:     */     //   from	to	target	type
/*  677:     */     //   51	57	60	java/lang/Throwable
/*  678:     */     //   85	91	94	java/lang/Throwable
/*  679:     */     //   17	43	114	java/lang/Throwable
/*  680:     */     //   17	43	122	finally
/*  681:     */     //   114	124	122	finally
/*  682:     */     //   132	138	141	java/lang/Throwable
/*  683:     */     //   6	77	161	java/lang/Throwable
/*  684:     */     //   114	161	161	java/lang/Throwable
/*  685:     */     //   6	77	166	finally
/*  686:     */     //   114	168	166	finally
/*  687:     */     //   176	182	185	java/lang/Throwable
/*  688:     */     //   0	111	205	java/sql/SQLException
/*  689:     */     //   114	205	205	java/sql/SQLException
/*  690:     */   }
/*  691:     */   
/*  692:     */   public static DbIterator<Transaction> getAllTransactions()
/*  693:     */   {
/*  694: 558 */     Connection localConnection = null;
/*  695:     */     try
/*  696:     */     {
/*  697: 560 */       localConnection = Db.getConnection();
/*  698: 561 */       PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction ORDER BY db_id ASC");
/*  699: 562 */       new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
/*  700:     */       {
/*  701:     */         public Transaction get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
/*  702:     */           throws NxtException.ValidationException
/*  703:     */         {
/*  704: 565 */           return Transaction.getTransaction(paramAnonymousConnection, paramAnonymousResultSet);
/*  705:     */         }
/*  706:     */       });
/*  707:     */     }
/*  708:     */     catch (SQLException localSQLException)
/*  709:     */     {
/*  710: 569 */       DbUtils.close(new AutoCloseable[] { localConnection });
/*  711: 570 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/*  712:     */     }
/*  713:     */   }
/*  714:     */   
/*  715:     */   public static DbIterator<Transaction> getAllTransactions(Account paramAccount, byte paramByte1, byte paramByte2, int paramInt)
/*  716:     */   {
/*  717: 575 */     Connection localConnection = null;
/*  718:     */     try
/*  719:     */     {
/*  720: 577 */       localConnection = Db.getConnection();
/*  721:     */       PreparedStatement localPreparedStatement;
/*  722: 579 */       if (paramByte1 >= 0)
/*  723:     */       {
/*  724: 580 */         if (paramByte2 >= 0)
/*  725:     */         {
/*  726: 581 */           localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_account_id = ?) AND type = ? AND subtype = ? ORDER BY timestamp ASC");
/*  727: 582 */           localPreparedStatement.setInt(1, paramInt);
/*  728: 583 */           localPreparedStatement.setLong(2, paramAccount.getId().longValue());
/*  729: 584 */           localPreparedStatement.setLong(3, paramAccount.getId().longValue());
/*  730: 585 */           localPreparedStatement.setByte(4, paramByte1);
/*  731: 586 */           localPreparedStatement.setByte(5, paramByte2);
/*  732:     */         }
/*  733:     */         else
/*  734:     */         {
/*  735: 588 */           localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_account_id = ?) AND type = ? ORDER BY timestamp ASC");
/*  736: 589 */           localPreparedStatement.setInt(1, paramInt);
/*  737: 590 */           localPreparedStatement.setLong(2, paramAccount.getId().longValue());
/*  738: 591 */           localPreparedStatement.setLong(3, paramAccount.getId().longValue());
/*  739: 592 */           localPreparedStatement.setByte(4, paramByte1);
/*  740:     */         }
/*  741:     */       }
/*  742:     */       else
/*  743:     */       {
/*  744: 595 */         localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_account_id = ?) ORDER BY timestamp ASC");
/*  745: 596 */         localPreparedStatement.setInt(1, paramInt);
/*  746: 597 */         localPreparedStatement.setLong(2, paramAccount.getId().longValue());
/*  747: 598 */         localPreparedStatement.setLong(3, paramAccount.getId().longValue());
/*  748:     */       }
/*  749: 600 */       new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
/*  750:     */       {
/*  751:     */         public Transaction get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
/*  752:     */           throws NxtException.ValidationException
/*  753:     */         {
/*  754: 603 */           return Transaction.getTransaction(paramAnonymousConnection, paramAnonymousResultSet);
/*  755:     */         }
/*  756:     */       });
/*  757:     */     }
/*  758:     */     catch (SQLException localSQLException)
/*  759:     */     {
/*  760: 607 */       DbUtils.close(new AutoCloseable[] { localConnection });
/*  761: 608 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/*  762:     */     }
/*  763:     */   }
/*  764:     */   
/*  765:     */   /* Error */
/*  766:     */   public static int getTransactionCount()
/*  767:     */   {
/*  768:     */     // Byte code:
/*  769:     */     //   0: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
/*  770:     */     //   3: astore_0
/*  771:     */     //   4: aconst_null
/*  772:     */     //   5: astore_1
/*  773:     */     //   6: aload_0
/*  774:     */     //   7: ldc 49
/*  775:     */     //   9: invokeinterface 12 2 0
/*  776:     */     //   14: astore_2
/*  777:     */     //   15: aconst_null
/*  778:     */     //   16: astore_3
/*  779:     */     //   17: aload_2
/*  780:     */     //   18: invokeinterface 30 1 0
/*  781:     */     //   23: astore 4
/*  782:     */     //   25: aload 4
/*  783:     */     //   27: invokeinterface 31 1 0
/*  784:     */     //   32: pop
/*  785:     */     //   33: aload 4
/*  786:     */     //   35: iconst_1
/*  787:     */     //   36: invokeinterface 32 2 0
/*  788:     */     //   41: istore 5
/*  789:     */     //   43: aload_2
/*  790:     */     //   44: ifnull +33 -> 77
/*  791:     */     //   47: aload_3
/*  792:     */     //   48: ifnull +23 -> 71
/*  793:     */     //   51: aload_2
/*  794:     */     //   52: invokeinterface 33 1 0
/*  795:     */     //   57: goto +20 -> 77
/*  796:     */     //   60: astore 6
/*  797:     */     //   62: aload_3
/*  798:     */     //   63: aload 6
/*  799:     */     //   65: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  800:     */     //   68: goto +9 -> 77
/*  801:     */     //   71: aload_2
/*  802:     */     //   72: invokeinterface 33 1 0
/*  803:     */     //   77: aload_0
/*  804:     */     //   78: ifnull +33 -> 111
/*  805:     */     //   81: aload_1
/*  806:     */     //   82: ifnull +23 -> 105
/*  807:     */     //   85: aload_0
/*  808:     */     //   86: invokeinterface 36 1 0
/*  809:     */     //   91: goto +20 -> 111
/*  810:     */     //   94: astore 6
/*  811:     */     //   96: aload_1
/*  812:     */     //   97: aload 6
/*  813:     */     //   99: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  814:     */     //   102: goto +9 -> 111
/*  815:     */     //   105: aload_0
/*  816:     */     //   106: invokeinterface 36 1 0
/*  817:     */     //   111: iload 5
/*  818:     */     //   113: ireturn
/*  819:     */     //   114: astore 4
/*  820:     */     //   116: aload 4
/*  821:     */     //   118: astore_3
/*  822:     */     //   119: aload 4
/*  823:     */     //   121: athrow
/*  824:     */     //   122: astore 7
/*  825:     */     //   124: aload_2
/*  826:     */     //   125: ifnull +33 -> 158
/*  827:     */     //   128: aload_3
/*  828:     */     //   129: ifnull +23 -> 152
/*  829:     */     //   132: aload_2
/*  830:     */     //   133: invokeinterface 33 1 0
/*  831:     */     //   138: goto +20 -> 158
/*  832:     */     //   141: astore 8
/*  833:     */     //   143: aload_3
/*  834:     */     //   144: aload 8
/*  835:     */     //   146: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  836:     */     //   149: goto +9 -> 158
/*  837:     */     //   152: aload_2
/*  838:     */     //   153: invokeinterface 33 1 0
/*  839:     */     //   158: aload 7
/*  840:     */     //   160: athrow
/*  841:     */     //   161: astore_2
/*  842:     */     //   162: aload_2
/*  843:     */     //   163: astore_1
/*  844:     */     //   164: aload_2
/*  845:     */     //   165: athrow
/*  846:     */     //   166: astore 9
/*  847:     */     //   168: aload_0
/*  848:     */     //   169: ifnull +33 -> 202
/*  849:     */     //   172: aload_1
/*  850:     */     //   173: ifnull +23 -> 196
/*  851:     */     //   176: aload_0
/*  852:     */     //   177: invokeinterface 36 1 0
/*  853:     */     //   182: goto +20 -> 202
/*  854:     */     //   185: astore 10
/*  855:     */     //   187: aload_1
/*  856:     */     //   188: aload 10
/*  857:     */     //   190: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  858:     */     //   193: goto +9 -> 202
/*  859:     */     //   196: aload_0
/*  860:     */     //   197: invokeinterface 36 1 0
/*  861:     */     //   202: aload 9
/*  862:     */     //   204: athrow
/*  863:     */     //   205: astore_0
/*  864:     */     //   206: new 20	java/lang/RuntimeException
/*  865:     */     //   209: dup
/*  866:     */     //   210: aload_0
/*  867:     */     //   211: invokevirtual 21	java/sql/SQLException:toString	()Ljava/lang/String;
/*  868:     */     //   214: aload_0
/*  869:     */     //   215: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/*  870:     */     //   218: athrow
/*  871:     */     // Line number table:
/*  872:     */     //   Java source line #613	-> byte code offset #0
/*  873:     */     //   Java source line #614	-> byte code offset #17
/*  874:     */     //   Java source line #615	-> byte code offset #25
/*  875:     */     //   Java source line #616	-> byte code offset #33
/*  876:     */     //   Java source line #617	-> byte code offset #43
/*  877:     */     //   Java source line #613	-> byte code offset #114
/*  878:     */     //   Java source line #617	-> byte code offset #122
/*  879:     */     //   Java source line #613	-> byte code offset #161
/*  880:     */     //   Java source line #617	-> byte code offset #166
/*  881:     */     //   Java source line #618	-> byte code offset #206
/*  882:     */     // Local variable table:
/*  883:     */     //   start	length	slot	name	signature
/*  884:     */     //   3	194	0	localConnection	Connection
/*  885:     */     //   205	10	0	localSQLException	SQLException
/*  886:     */     //   5	183	1	localObject1	Object
/*  887:     */     //   14	139	2	localPreparedStatement	PreparedStatement
/*  888:     */     //   161	4	2	localThrowable1	Throwable
/*  889:     */     //   16	128	3	localObject2	Object
/*  890:     */     //   23	11	4	localResultSet	ResultSet
/*  891:     */     //   114	6	4	localThrowable2	Throwable
/*  892:     */     //   60	4	6	localThrowable3	Throwable
/*  893:     */     //   94	4	6	localThrowable4	Throwable
/*  894:     */     //   122	37	7	localObject3	Object
/*  895:     */     //   141	4	8	localThrowable5	Throwable
/*  896:     */     //   166	37	9	localObject4	Object
/*  897:     */     //   185	4	10	localThrowable6	Throwable
/*  898:     */     // Exception table:
/*  899:     */     //   from	to	target	type
/*  900:     */     //   51	57	60	java/lang/Throwable
/*  901:     */     //   85	91	94	java/lang/Throwable
/*  902:     */     //   17	43	114	java/lang/Throwable
/*  903:     */     //   17	43	122	finally
/*  904:     */     //   114	124	122	finally
/*  905:     */     //   132	138	141	java/lang/Throwable
/*  906:     */     //   6	77	161	java/lang/Throwable
/*  907:     */     //   114	161	161	java/lang/Throwable
/*  908:     */     //   6	77	166	finally
/*  909:     */     //   114	168	166	finally
/*  910:     */     //   176	182	185	java/lang/Throwable
/*  911:     */     //   0	111	205	java/sql/SQLException
/*  912:     */     //   114	205	205	java/sql/SQLException
/*  913:     */   }
/*  914:     */   
/*  915:     */   /* Error */
/*  916:     */   public static java.util.List<Long> getBlockIdsAfter(Long paramLong, int paramInt)
/*  917:     */   {
/*  918:     */     // Byte code:
/*  919:     */     //   0: iload_1
/*  920:     */     //   1: sipush 1440
/*  921:     */     //   4: if_icmple +13 -> 17
/*  922:     */     //   7: new 50	java/lang/IllegalArgumentException
/*  923:     */     //   10: dup
/*  924:     */     //   11: ldc 51
/*  925:     */     //   13: invokespecial 52	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
/*  926:     */     //   16: athrow
/*  927:     */     //   17: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
/*  928:     */     //   20: astore_2
/*  929:     */     //   21: aconst_null
/*  930:     */     //   22: astore_3
/*  931:     */     //   23: aload_2
/*  932:     */     //   24: ldc 53
/*  933:     */     //   26: invokeinterface 12 2 0
/*  934:     */     //   31: astore 4
/*  935:     */     //   33: aconst_null
/*  936:     */     //   34: astore 5
/*  937:     */     //   36: aload_2
/*  938:     */     //   37: ldc 54
/*  939:     */     //   39: invokeinterface 12 2 0
/*  940:     */     //   44: astore 6
/*  941:     */     //   46: aconst_null
/*  942:     */     //   47: astore 7
/*  943:     */     //   49: aload 4
/*  944:     */     //   51: iconst_1
/*  945:     */     //   52: aload_0
/*  946:     */     //   53: invokevirtual 42	java/lang/Long:longValue	()J
/*  947:     */     //   56: invokeinterface 43 4 0
/*  948:     */     //   61: aload 4
/*  949:     */     //   63: invokeinterface 30 1 0
/*  950:     */     //   68: astore 8
/*  951:     */     //   70: aload 8
/*  952:     */     //   72: invokeinterface 31 1 0
/*  953:     */     //   77: ifne +130 -> 207
/*  954:     */     //   80: aload 8
/*  955:     */     //   82: invokeinterface 55 1 0
/*  956:     */     //   87: invokestatic 56	java/util/Collections:emptyList	()Ljava/util/List;
/*  957:     */     //   90: astore 9
/*  958:     */     //   92: aload 6
/*  959:     */     //   94: ifnull +37 -> 131
/*  960:     */     //   97: aload 7
/*  961:     */     //   99: ifnull +25 -> 124
/*  962:     */     //   102: aload 6
/*  963:     */     //   104: invokeinterface 33 1 0
/*  964:     */     //   109: goto +22 -> 131
/*  965:     */     //   112: astore 10
/*  966:     */     //   114: aload 7
/*  967:     */     //   116: aload 10
/*  968:     */     //   118: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  969:     */     //   121: goto +10 -> 131
/*  970:     */     //   124: aload 6
/*  971:     */     //   126: invokeinterface 33 1 0
/*  972:     */     //   131: aload 4
/*  973:     */     //   133: ifnull +37 -> 170
/*  974:     */     //   136: aload 5
/*  975:     */     //   138: ifnull +25 -> 163
/*  976:     */     //   141: aload 4
/*  977:     */     //   143: invokeinterface 33 1 0
/*  978:     */     //   148: goto +22 -> 170
/*  979:     */     //   151: astore 10
/*  980:     */     //   153: aload 5
/*  981:     */     //   155: aload 10
/*  982:     */     //   157: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  983:     */     //   160: goto +10 -> 170
/*  984:     */     //   163: aload 4
/*  985:     */     //   165: invokeinterface 33 1 0
/*  986:     */     //   170: aload_2
/*  987:     */     //   171: ifnull +33 -> 204
/*  988:     */     //   174: aload_3
/*  989:     */     //   175: ifnull +23 -> 198
/*  990:     */     //   178: aload_2
/*  991:     */     //   179: invokeinterface 36 1 0
/*  992:     */     //   184: goto +20 -> 204
/*  993:     */     //   187: astore 10
/*  994:     */     //   189: aload_3
/*  995:     */     //   190: aload 10
/*  996:     */     //   192: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  997:     */     //   195: goto +9 -> 204
/*  998:     */     //   198: aload_2
/*  999:     */     //   199: invokeinterface 36 1 0
/* 1000:     */     //   204: aload 9
/* 1001:     */     //   206: areturn
/* 1002:     */     //   207: new 57	java/util/ArrayList
/* 1003:     */     //   210: dup
/* 1004:     */     //   211: invokespecial 58	java/util/ArrayList:<init>	()V
/* 1005:     */     //   214: astore 9
/* 1006:     */     //   216: aload 8
/* 1007:     */     //   218: ldc 59
/* 1008:     */     //   220: invokeinterface 60 2 0
/* 1009:     */     //   225: istore 10
/* 1010:     */     //   227: aload 6
/* 1011:     */     //   229: iconst_1
/* 1012:     */     //   230: iload 10
/* 1013:     */     //   232: invokeinterface 24 3 0
/* 1014:     */     //   237: aload 6
/* 1015:     */     //   239: iconst_2
/* 1016:     */     //   240: iload_1
/* 1017:     */     //   241: invokeinterface 24 3 0
/* 1018:     */     //   246: aload 6
/* 1019:     */     //   248: invokeinterface 30 1 0
/* 1020:     */     //   253: astore 8
/* 1021:     */     //   255: aload 8
/* 1022:     */     //   257: invokeinterface 31 1 0
/* 1023:     */     //   262: ifeq +26 -> 288
/* 1024:     */     //   265: aload 9
/* 1025:     */     //   267: aload 8
/* 1026:     */     //   269: ldc 61
/* 1027:     */     //   271: invokeinterface 62 2 0
/* 1028:     */     //   276: invokestatic 63	java/lang/Long:valueOf	(J)Ljava/lang/Long;
/* 1029:     */     //   279: invokeinterface 64 2 0
/* 1030:     */     //   284: pop
/* 1031:     */     //   285: goto -30 -> 255
/* 1032:     */     //   288: aload 8
/* 1033:     */     //   290: invokeinterface 55 1 0
/* 1034:     */     //   295: aload 9
/* 1035:     */     //   297: astore 11
/* 1036:     */     //   299: aload 6
/* 1037:     */     //   301: ifnull +37 -> 338
/* 1038:     */     //   304: aload 7
/* 1039:     */     //   306: ifnull +25 -> 331
/* 1040:     */     //   309: aload 6
/* 1041:     */     //   311: invokeinterface 33 1 0
/* 1042:     */     //   316: goto +22 -> 338
/* 1043:     */     //   319: astore 12
/* 1044:     */     //   321: aload 7
/* 1045:     */     //   323: aload 12
/* 1046:     */     //   325: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1047:     */     //   328: goto +10 -> 338
/* 1048:     */     //   331: aload 6
/* 1049:     */     //   333: invokeinterface 33 1 0
/* 1050:     */     //   338: aload 4
/* 1051:     */     //   340: ifnull +37 -> 377
/* 1052:     */     //   343: aload 5
/* 1053:     */     //   345: ifnull +25 -> 370
/* 1054:     */     //   348: aload 4
/* 1055:     */     //   350: invokeinterface 33 1 0
/* 1056:     */     //   355: goto +22 -> 377
/* 1057:     */     //   358: astore 12
/* 1058:     */     //   360: aload 5
/* 1059:     */     //   362: aload 12
/* 1060:     */     //   364: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1061:     */     //   367: goto +10 -> 377
/* 1062:     */     //   370: aload 4
/* 1063:     */     //   372: invokeinterface 33 1 0
/* 1064:     */     //   377: aload_2
/* 1065:     */     //   378: ifnull +33 -> 411
/* 1066:     */     //   381: aload_3
/* 1067:     */     //   382: ifnull +23 -> 405
/* 1068:     */     //   385: aload_2
/* 1069:     */     //   386: invokeinterface 36 1 0
/* 1070:     */     //   391: goto +20 -> 411
/* 1071:     */     //   394: astore 12
/* 1072:     */     //   396: aload_3
/* 1073:     */     //   397: aload 12
/* 1074:     */     //   399: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1075:     */     //   402: goto +9 -> 411
/* 1076:     */     //   405: aload_2
/* 1077:     */     //   406: invokeinterface 36 1 0
/* 1078:     */     //   411: aload 11
/* 1079:     */     //   413: areturn
/* 1080:     */     //   414: astore 8
/* 1081:     */     //   416: aload 8
/* 1082:     */     //   418: astore 7
/* 1083:     */     //   420: aload 8
/* 1084:     */     //   422: athrow
/* 1085:     */     //   423: astore 13
/* 1086:     */     //   425: aload 6
/* 1087:     */     //   427: ifnull +37 -> 464
/* 1088:     */     //   430: aload 7
/* 1089:     */     //   432: ifnull +25 -> 457
/* 1090:     */     //   435: aload 6
/* 1091:     */     //   437: invokeinterface 33 1 0
/* 1092:     */     //   442: goto +22 -> 464
/* 1093:     */     //   445: astore 14
/* 1094:     */     //   447: aload 7
/* 1095:     */     //   449: aload 14
/* 1096:     */     //   451: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1097:     */     //   454: goto +10 -> 464
/* 1098:     */     //   457: aload 6
/* 1099:     */     //   459: invokeinterface 33 1 0
/* 1100:     */     //   464: aload 13
/* 1101:     */     //   466: athrow
/* 1102:     */     //   467: astore 6
/* 1103:     */     //   469: aload 6
/* 1104:     */     //   471: astore 5
/* 1105:     */     //   473: aload 6
/* 1106:     */     //   475: athrow
/* 1107:     */     //   476: astore 15
/* 1108:     */     //   478: aload 4
/* 1109:     */     //   480: ifnull +37 -> 517
/* 1110:     */     //   483: aload 5
/* 1111:     */     //   485: ifnull +25 -> 510
/* 1112:     */     //   488: aload 4
/* 1113:     */     //   490: invokeinterface 33 1 0
/* 1114:     */     //   495: goto +22 -> 517
/* 1115:     */     //   498: astore 16
/* 1116:     */     //   500: aload 5
/* 1117:     */     //   502: aload 16
/* 1118:     */     //   504: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1119:     */     //   507: goto +10 -> 517
/* 1120:     */     //   510: aload 4
/* 1121:     */     //   512: invokeinterface 33 1 0
/* 1122:     */     //   517: aload 15
/* 1123:     */     //   519: athrow
/* 1124:     */     //   520: astore 4
/* 1125:     */     //   522: aload 4
/* 1126:     */     //   524: astore_3
/* 1127:     */     //   525: aload 4
/* 1128:     */     //   527: athrow
/* 1129:     */     //   528: astore 17
/* 1130:     */     //   530: aload_2
/* 1131:     */     //   531: ifnull +33 -> 564
/* 1132:     */     //   534: aload_3
/* 1133:     */     //   535: ifnull +23 -> 558
/* 1134:     */     //   538: aload_2
/* 1135:     */     //   539: invokeinterface 36 1 0
/* 1136:     */     //   544: goto +20 -> 564
/* 1137:     */     //   547: astore 18
/* 1138:     */     //   549: aload_3
/* 1139:     */     //   550: aload 18
/* 1140:     */     //   552: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1141:     */     //   555: goto +9 -> 564
/* 1142:     */     //   558: aload_2
/* 1143:     */     //   559: invokeinterface 36 1 0
/* 1144:     */     //   564: aload 17
/* 1145:     */     //   566: athrow
/* 1146:     */     //   567: astore_2
/* 1147:     */     //   568: new 20	java/lang/RuntimeException
/* 1148:     */     //   571: dup
/* 1149:     */     //   572: aload_2
/* 1150:     */     //   573: invokevirtual 21	java/sql/SQLException:toString	()Ljava/lang/String;
/* 1151:     */     //   576: aload_2
/* 1152:     */     //   577: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/* 1153:     */     //   580: athrow
/* 1154:     */     // Line number table:
/* 1155:     */     //   Java source line #623	-> byte code offset #0
/* 1156:     */     //   Java source line #624	-> byte code offset #7
/* 1157:     */     //   Java source line #626	-> byte code offset #17
/* 1158:     */     //   Java source line #627	-> byte code offset #23
/* 1159:     */     //   Java source line #626	-> byte code offset #33
/* 1160:     */     //   Java source line #628	-> byte code offset #36
/* 1161:     */     //   Java source line #626	-> byte code offset #46
/* 1162:     */     //   Java source line #629	-> byte code offset #49
/* 1163:     */     //   Java source line #630	-> byte code offset #61
/* 1164:     */     //   Java source line #631	-> byte code offset #70
/* 1165:     */     //   Java source line #632	-> byte code offset #80
/* 1166:     */     //   Java source line #633	-> byte code offset #87
/* 1167:     */     //   Java source line #645	-> byte code offset #92
/* 1168:     */     //   Java source line #635	-> byte code offset #207
/* 1169:     */     //   Java source line #636	-> byte code offset #216
/* 1170:     */     //   Java source line #637	-> byte code offset #227
/* 1171:     */     //   Java source line #638	-> byte code offset #237
/* 1172:     */     //   Java source line #639	-> byte code offset #246
/* 1173:     */     //   Java source line #640	-> byte code offset #255
/* 1174:     */     //   Java source line #641	-> byte code offset #265
/* 1175:     */     //   Java source line #643	-> byte code offset #288
/* 1176:     */     //   Java source line #644	-> byte code offset #295
/* 1177:     */     //   Java source line #645	-> byte code offset #299
/* 1178:     */     //   Java source line #626	-> byte code offset #414
/* 1179:     */     //   Java source line #645	-> byte code offset #423
/* 1180:     */     //   Java source line #626	-> byte code offset #467
/* 1181:     */     //   Java source line #645	-> byte code offset #476
/* 1182:     */     //   Java source line #626	-> byte code offset #520
/* 1183:     */     //   Java source line #645	-> byte code offset #528
/* 1184:     */     //   Java source line #646	-> byte code offset #568
/* 1185:     */     // Local variable table:
/* 1186:     */     //   start	length	slot	name	signature
/* 1187:     */     //   0	581	0	paramLong	Long
/* 1188:     */     //   0	581	1	paramInt	int
/* 1189:     */     //   20	539	2	localConnection	Connection
/* 1190:     */     //   567	10	2	localSQLException	SQLException
/* 1191:     */     //   22	528	3	localObject1	Object
/* 1192:     */     //   31	480	4	localPreparedStatement1	PreparedStatement
/* 1193:     */     //   520	6	4	localThrowable1	Throwable
/* 1194:     */     //   34	467	5	localObject2	Object
/* 1195:     */     //   44	414	6	localPreparedStatement2	PreparedStatement
/* 1196:     */     //   467	7	6	localThrowable2	Throwable
/* 1197:     */     //   47	401	7	localObject3	Object
/* 1198:     */     //   68	221	8	localResultSet	ResultSet
/* 1199:     */     //   414	7	8	localThrowable3	Throwable
/* 1200:     */     //   90	206	9	localObject4	Object
/* 1201:     */     //   112	5	10	localThrowable4	Throwable
/* 1202:     */     //   151	5	10	localThrowable5	Throwable
/* 1203:     */     //   187	4	10	localThrowable6	Throwable
/* 1204:     */     //   225	6	10	i	int
/* 1205:     */     //   297	115	11	localObject5	Object
/* 1206:     */     //   319	5	12	localThrowable7	Throwable
/* 1207:     */     //   358	5	12	localThrowable8	Throwable
/* 1208:     */     //   394	4	12	localThrowable9	Throwable
/* 1209:     */     //   423	42	13	localObject6	Object
/* 1210:     */     //   445	5	14	localThrowable10	Throwable
/* 1211:     */     //   476	42	15	localObject7	Object
/* 1212:     */     //   498	5	16	localThrowable11	Throwable
/* 1213:     */     //   528	37	17	localObject8	Object
/* 1214:     */     //   547	4	18	localThrowable12	Throwable
/* 1215:     */     // Exception table:
/* 1216:     */     //   from	to	target	type
/* 1217:     */     //   102	109	112	java/lang/Throwable
/* 1218:     */     //   141	148	151	java/lang/Throwable
/* 1219:     */     //   178	184	187	java/lang/Throwable
/* 1220:     */     //   309	316	319	java/lang/Throwable
/* 1221:     */     //   348	355	358	java/lang/Throwable
/* 1222:     */     //   385	391	394	java/lang/Throwable
/* 1223:     */     //   49	92	414	java/lang/Throwable
/* 1224:     */     //   207	299	414	java/lang/Throwable
/* 1225:     */     //   49	92	423	finally
/* 1226:     */     //   207	299	423	finally
/* 1227:     */     //   414	425	423	finally
/* 1228:     */     //   435	442	445	java/lang/Throwable
/* 1229:     */     //   36	131	467	java/lang/Throwable
/* 1230:     */     //   207	338	467	java/lang/Throwable
/* 1231:     */     //   414	467	467	java/lang/Throwable
/* 1232:     */     //   36	131	476	finally
/* 1233:     */     //   207	338	476	finally
/* 1234:     */     //   414	478	476	finally
/* 1235:     */     //   488	495	498	java/lang/Throwable
/* 1236:     */     //   23	170	520	java/lang/Throwable
/* 1237:     */     //   207	377	520	java/lang/Throwable
/* 1238:     */     //   414	520	520	java/lang/Throwable
/* 1239:     */     //   23	170	528	finally
/* 1240:     */     //   207	377	528	finally
/* 1241:     */     //   414	530	528	finally
/* 1242:     */     //   538	544	547	java/lang/Throwable
/* 1243:     */     //   17	204	567	java/sql/SQLException
/* 1244:     */     //   207	411	567	java/sql/SQLException
/* 1245:     */     //   414	567	567	java/sql/SQLException
/* 1246:     */   }
/* 1247:     */   
/* 1248:     */   /* Error */
/* 1249:     */   public static java.util.List<Block> getBlocksAfter(Long paramLong, int paramInt)
/* 1250:     */   {
/* 1251:     */     // Byte code:
/* 1252:     */     //   0: iload_1
/* 1253:     */     //   1: sipush 1440
/* 1254:     */     //   4: if_icmple +13 -> 17
/* 1255:     */     //   7: new 50	java/lang/IllegalArgumentException
/* 1256:     */     //   10: dup
/* 1257:     */     //   11: ldc 51
/* 1258:     */     //   13: invokespecial 52	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
/* 1259:     */     //   16: athrow
/* 1260:     */     //   17: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
/* 1261:     */     //   20: astore_2
/* 1262:     */     //   21: aconst_null
/* 1263:     */     //   22: astore_3
/* 1264:     */     //   23: aload_2
/* 1265:     */     //   24: ldc 53
/* 1266:     */     //   26: invokeinterface 12 2 0
/* 1267:     */     //   31: astore 4
/* 1268:     */     //   33: aconst_null
/* 1269:     */     //   34: astore 5
/* 1270:     */     //   36: aload_2
/* 1271:     */     //   37: ldc 65
/* 1272:     */     //   39: invokeinterface 12 2 0
/* 1273:     */     //   44: astore 6
/* 1274:     */     //   46: aconst_null
/* 1275:     */     //   47: astore 7
/* 1276:     */     //   49: aload 4
/* 1277:     */     //   51: iconst_1
/* 1278:     */     //   52: aload_0
/* 1279:     */     //   53: invokevirtual 42	java/lang/Long:longValue	()J
/* 1280:     */     //   56: invokeinterface 43 4 0
/* 1281:     */     //   61: aload 4
/* 1282:     */     //   63: invokeinterface 30 1 0
/* 1283:     */     //   68: astore 8
/* 1284:     */     //   70: aload 8
/* 1285:     */     //   72: invokeinterface 31 1 0
/* 1286:     */     //   77: ifne +130 -> 207
/* 1287:     */     //   80: aload 8
/* 1288:     */     //   82: invokeinterface 55 1 0
/* 1289:     */     //   87: invokestatic 56	java/util/Collections:emptyList	()Ljava/util/List;
/* 1290:     */     //   90: astore 9
/* 1291:     */     //   92: aload 6
/* 1292:     */     //   94: ifnull +37 -> 131
/* 1293:     */     //   97: aload 7
/* 1294:     */     //   99: ifnull +25 -> 124
/* 1295:     */     //   102: aload 6
/* 1296:     */     //   104: invokeinterface 33 1 0
/* 1297:     */     //   109: goto +22 -> 131
/* 1298:     */     //   112: astore 10
/* 1299:     */     //   114: aload 7
/* 1300:     */     //   116: aload 10
/* 1301:     */     //   118: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1302:     */     //   121: goto +10 -> 131
/* 1303:     */     //   124: aload 6
/* 1304:     */     //   126: invokeinterface 33 1 0
/* 1305:     */     //   131: aload 4
/* 1306:     */     //   133: ifnull +37 -> 170
/* 1307:     */     //   136: aload 5
/* 1308:     */     //   138: ifnull +25 -> 163
/* 1309:     */     //   141: aload 4
/* 1310:     */     //   143: invokeinterface 33 1 0
/* 1311:     */     //   148: goto +22 -> 170
/* 1312:     */     //   151: astore 10
/* 1313:     */     //   153: aload 5
/* 1314:     */     //   155: aload 10
/* 1315:     */     //   157: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1316:     */     //   160: goto +10 -> 170
/* 1317:     */     //   163: aload 4
/* 1318:     */     //   165: invokeinterface 33 1 0
/* 1319:     */     //   170: aload_2
/* 1320:     */     //   171: ifnull +33 -> 204
/* 1321:     */     //   174: aload_3
/* 1322:     */     //   175: ifnull +23 -> 198
/* 1323:     */     //   178: aload_2
/* 1324:     */     //   179: invokeinterface 36 1 0
/* 1325:     */     //   184: goto +20 -> 204
/* 1326:     */     //   187: astore 10
/* 1327:     */     //   189: aload_3
/* 1328:     */     //   190: aload 10
/* 1329:     */     //   192: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1330:     */     //   195: goto +9 -> 204
/* 1331:     */     //   198: aload_2
/* 1332:     */     //   199: invokeinterface 36 1 0
/* 1333:     */     //   204: aload 9
/* 1334:     */     //   206: areturn
/* 1335:     */     //   207: new 57	java/util/ArrayList
/* 1336:     */     //   210: dup
/* 1337:     */     //   211: invokespecial 58	java/util/ArrayList:<init>	()V
/* 1338:     */     //   214: astore 9
/* 1339:     */     //   216: aload 8
/* 1340:     */     //   218: ldc 59
/* 1341:     */     //   220: invokeinterface 60 2 0
/* 1342:     */     //   225: istore 10
/* 1343:     */     //   227: aload 6
/* 1344:     */     //   229: iconst_1
/* 1345:     */     //   230: iload 10
/* 1346:     */     //   232: invokeinterface 24 3 0
/* 1347:     */     //   237: aload 6
/* 1348:     */     //   239: iconst_2
/* 1349:     */     //   240: iload_1
/* 1350:     */     //   241: invokeinterface 24 3 0
/* 1351:     */     //   246: aload 6
/* 1352:     */     //   248: invokeinterface 30 1 0
/* 1353:     */     //   253: astore 8
/* 1354:     */     //   255: aload 8
/* 1355:     */     //   257: invokeinterface 31 1 0
/* 1356:     */     //   262: ifeq +20 -> 282
/* 1357:     */     //   265: aload 9
/* 1358:     */     //   267: aload_2
/* 1359:     */     //   268: aload 8
/* 1360:     */     //   270: invokestatic 66	nxt/Block:getBlock	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/Block;
/* 1361:     */     //   273: invokeinterface 64 2 0
/* 1362:     */     //   278: pop
/* 1363:     */     //   279: goto -24 -> 255
/* 1364:     */     //   282: aload 8
/* 1365:     */     //   284: invokeinterface 55 1 0
/* 1366:     */     //   289: aload 9
/* 1367:     */     //   291: astore 11
/* 1368:     */     //   293: aload 6
/* 1369:     */     //   295: ifnull +37 -> 332
/* 1370:     */     //   298: aload 7
/* 1371:     */     //   300: ifnull +25 -> 325
/* 1372:     */     //   303: aload 6
/* 1373:     */     //   305: invokeinterface 33 1 0
/* 1374:     */     //   310: goto +22 -> 332
/* 1375:     */     //   313: astore 12
/* 1376:     */     //   315: aload 7
/* 1377:     */     //   317: aload 12
/* 1378:     */     //   319: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1379:     */     //   322: goto +10 -> 332
/* 1380:     */     //   325: aload 6
/* 1381:     */     //   327: invokeinterface 33 1 0
/* 1382:     */     //   332: aload 4
/* 1383:     */     //   334: ifnull +37 -> 371
/* 1384:     */     //   337: aload 5
/* 1385:     */     //   339: ifnull +25 -> 364
/* 1386:     */     //   342: aload 4
/* 1387:     */     //   344: invokeinterface 33 1 0
/* 1388:     */     //   349: goto +22 -> 371
/* 1389:     */     //   352: astore 12
/* 1390:     */     //   354: aload 5
/* 1391:     */     //   356: aload 12
/* 1392:     */     //   358: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1393:     */     //   361: goto +10 -> 371
/* 1394:     */     //   364: aload 4
/* 1395:     */     //   366: invokeinterface 33 1 0
/* 1396:     */     //   371: aload_2
/* 1397:     */     //   372: ifnull +33 -> 405
/* 1398:     */     //   375: aload_3
/* 1399:     */     //   376: ifnull +23 -> 399
/* 1400:     */     //   379: aload_2
/* 1401:     */     //   380: invokeinterface 36 1 0
/* 1402:     */     //   385: goto +20 -> 405
/* 1403:     */     //   388: astore 12
/* 1404:     */     //   390: aload_3
/* 1405:     */     //   391: aload 12
/* 1406:     */     //   393: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1407:     */     //   396: goto +9 -> 405
/* 1408:     */     //   399: aload_2
/* 1409:     */     //   400: invokeinterface 36 1 0
/* 1410:     */     //   405: aload 11
/* 1411:     */     //   407: areturn
/* 1412:     */     //   408: astore 8
/* 1413:     */     //   410: aload 8
/* 1414:     */     //   412: astore 7
/* 1415:     */     //   414: aload 8
/* 1416:     */     //   416: athrow
/* 1417:     */     //   417: astore 13
/* 1418:     */     //   419: aload 6
/* 1419:     */     //   421: ifnull +37 -> 458
/* 1420:     */     //   424: aload 7
/* 1421:     */     //   426: ifnull +25 -> 451
/* 1422:     */     //   429: aload 6
/* 1423:     */     //   431: invokeinterface 33 1 0
/* 1424:     */     //   436: goto +22 -> 458
/* 1425:     */     //   439: astore 14
/* 1426:     */     //   441: aload 7
/* 1427:     */     //   443: aload 14
/* 1428:     */     //   445: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1429:     */     //   448: goto +10 -> 458
/* 1430:     */     //   451: aload 6
/* 1431:     */     //   453: invokeinterface 33 1 0
/* 1432:     */     //   458: aload 13
/* 1433:     */     //   460: athrow
/* 1434:     */     //   461: astore 6
/* 1435:     */     //   463: aload 6
/* 1436:     */     //   465: astore 5
/* 1437:     */     //   467: aload 6
/* 1438:     */     //   469: athrow
/* 1439:     */     //   470: astore 15
/* 1440:     */     //   472: aload 4
/* 1441:     */     //   474: ifnull +37 -> 511
/* 1442:     */     //   477: aload 5
/* 1443:     */     //   479: ifnull +25 -> 504
/* 1444:     */     //   482: aload 4
/* 1445:     */     //   484: invokeinterface 33 1 0
/* 1446:     */     //   489: goto +22 -> 511
/* 1447:     */     //   492: astore 16
/* 1448:     */     //   494: aload 5
/* 1449:     */     //   496: aload 16
/* 1450:     */     //   498: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1451:     */     //   501: goto +10 -> 511
/* 1452:     */     //   504: aload 4
/* 1453:     */     //   506: invokeinterface 33 1 0
/* 1454:     */     //   511: aload 15
/* 1455:     */     //   513: athrow
/* 1456:     */     //   514: astore 4
/* 1457:     */     //   516: aload 4
/* 1458:     */     //   518: astore_3
/* 1459:     */     //   519: aload 4
/* 1460:     */     //   521: athrow
/* 1461:     */     //   522: astore 17
/* 1462:     */     //   524: aload_2
/* 1463:     */     //   525: ifnull +33 -> 558
/* 1464:     */     //   528: aload_3
/* 1465:     */     //   529: ifnull +23 -> 552
/* 1466:     */     //   532: aload_2
/* 1467:     */     //   533: invokeinterface 36 1 0
/* 1468:     */     //   538: goto +20 -> 558
/* 1469:     */     //   541: astore 18
/* 1470:     */     //   543: aload_3
/* 1471:     */     //   544: aload 18
/* 1472:     */     //   546: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1473:     */     //   549: goto +9 -> 558
/* 1474:     */     //   552: aload_2
/* 1475:     */     //   553: invokeinterface 36 1 0
/* 1476:     */     //   558: aload 17
/* 1477:     */     //   560: athrow
/* 1478:     */     //   561: astore_2
/* 1479:     */     //   562: new 20	java/lang/RuntimeException
/* 1480:     */     //   565: dup
/* 1481:     */     //   566: aload_2
/* 1482:     */     //   567: invokevirtual 68	java/lang/Exception:toString	()Ljava/lang/String;
/* 1483:     */     //   570: aload_2
/* 1484:     */     //   571: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/* 1485:     */     //   574: athrow
/* 1486:     */     // Line number table:
/* 1487:     */     //   Java source line #651	-> byte code offset #0
/* 1488:     */     //   Java source line #652	-> byte code offset #7
/* 1489:     */     //   Java source line #654	-> byte code offset #17
/* 1490:     */     //   Java source line #655	-> byte code offset #23
/* 1491:     */     //   Java source line #654	-> byte code offset #33
/* 1492:     */     //   Java source line #656	-> byte code offset #36
/* 1493:     */     //   Java source line #654	-> byte code offset #46
/* 1494:     */     //   Java source line #657	-> byte code offset #49
/* 1495:     */     //   Java source line #658	-> byte code offset #61
/* 1496:     */     //   Java source line #659	-> byte code offset #70
/* 1497:     */     //   Java source line #660	-> byte code offset #80
/* 1498:     */     //   Java source line #661	-> byte code offset #87
/* 1499:     */     //   Java source line #673	-> byte code offset #92
/* 1500:     */     //   Java source line #663	-> byte code offset #207
/* 1501:     */     //   Java source line #664	-> byte code offset #216
/* 1502:     */     //   Java source line #665	-> byte code offset #227
/* 1503:     */     //   Java source line #666	-> byte code offset #237
/* 1504:     */     //   Java source line #667	-> byte code offset #246
/* 1505:     */     //   Java source line #668	-> byte code offset #255
/* 1506:     */     //   Java source line #669	-> byte code offset #265
/* 1507:     */     //   Java source line #671	-> byte code offset #282
/* 1508:     */     //   Java source line #672	-> byte code offset #289
/* 1509:     */     //   Java source line #673	-> byte code offset #293
/* 1510:     */     //   Java source line #654	-> byte code offset #408
/* 1511:     */     //   Java source line #673	-> byte code offset #417
/* 1512:     */     //   Java source line #654	-> byte code offset #461
/* 1513:     */     //   Java source line #673	-> byte code offset #470
/* 1514:     */     //   Java source line #654	-> byte code offset #514
/* 1515:     */     //   Java source line #673	-> byte code offset #522
/* 1516:     */     //   Java source line #674	-> byte code offset #562
/* 1517:     */     // Local variable table:
/* 1518:     */     //   start	length	slot	name	signature
/* 1519:     */     //   0	575	0	paramLong	Long
/* 1520:     */     //   0	575	1	paramInt	int
/* 1521:     */     //   20	533	2	localConnection	Connection
/* 1522:     */     //   561	10	2	localValidationException	NxtException.ValidationException
/* 1523:     */     //   22	522	3	localObject1	Object
/* 1524:     */     //   31	474	4	localPreparedStatement1	PreparedStatement
/* 1525:     */     //   514	6	4	localThrowable1	Throwable
/* 1526:     */     //   34	461	5	localObject2	Object
/* 1527:     */     //   44	408	6	localPreparedStatement2	PreparedStatement
/* 1528:     */     //   461	7	6	localThrowable2	Throwable
/* 1529:     */     //   47	395	7	localObject3	Object
/* 1530:     */     //   68	215	8	localResultSet	ResultSet
/* 1531:     */     //   408	7	8	localThrowable3	Throwable
/* 1532:     */     //   90	200	9	localObject4	Object
/* 1533:     */     //   112	5	10	localThrowable4	Throwable
/* 1534:     */     //   151	5	10	localThrowable5	Throwable
/* 1535:     */     //   187	4	10	localThrowable6	Throwable
/* 1536:     */     //   225	6	10	i	int
/* 1537:     */     //   291	115	11	localObject5	Object
/* 1538:     */     //   313	5	12	localThrowable7	Throwable
/* 1539:     */     //   352	5	12	localThrowable8	Throwable
/* 1540:     */     //   388	4	12	localThrowable9	Throwable
/* 1541:     */     //   417	42	13	localObject6	Object
/* 1542:     */     //   439	5	14	localThrowable10	Throwable
/* 1543:     */     //   470	42	15	localObject7	Object
/* 1544:     */     //   492	5	16	localThrowable11	Throwable
/* 1545:     */     //   522	37	17	localObject8	Object
/* 1546:     */     //   541	4	18	localThrowable12	Throwable
/* 1547:     */     // Exception table:
/* 1548:     */     //   from	to	target	type
/* 1549:     */     //   102	109	112	java/lang/Throwable
/* 1550:     */     //   141	148	151	java/lang/Throwable
/* 1551:     */     //   178	184	187	java/lang/Throwable
/* 1552:     */     //   303	310	313	java/lang/Throwable
/* 1553:     */     //   342	349	352	java/lang/Throwable
/* 1554:     */     //   379	385	388	java/lang/Throwable
/* 1555:     */     //   49	92	408	java/lang/Throwable
/* 1556:     */     //   207	293	408	java/lang/Throwable
/* 1557:     */     //   49	92	417	finally
/* 1558:     */     //   207	293	417	finally
/* 1559:     */     //   408	419	417	finally
/* 1560:     */     //   429	436	439	java/lang/Throwable
/* 1561:     */     //   36	131	461	java/lang/Throwable
/* 1562:     */     //   207	332	461	java/lang/Throwable
/* 1563:     */     //   408	461	461	java/lang/Throwable
/* 1564:     */     //   36	131	470	finally
/* 1565:     */     //   207	332	470	finally
/* 1566:     */     //   408	472	470	finally
/* 1567:     */     //   482	489	492	java/lang/Throwable
/* 1568:     */     //   23	170	514	java/lang/Throwable
/* 1569:     */     //   207	371	514	java/lang/Throwable
/* 1570:     */     //   408	514	514	java/lang/Throwable
/* 1571:     */     //   23	170	522	finally
/* 1572:     */     //   207	371	522	finally
/* 1573:     */     //   408	524	522	finally
/* 1574:     */     //   532	538	541	java/lang/Throwable
/* 1575:     */     //   17	204	561	nxt/NxtException$ValidationException
/* 1576:     */     //   17	204	561	java/sql/SQLException
/* 1577:     */     //   207	405	561	nxt/NxtException$ValidationException
/* 1578:     */     //   207	405	561	java/sql/SQLException
/* 1579:     */     //   408	561	561	nxt/NxtException$ValidationException
/* 1580:     */     //   408	561	561	java/sql/SQLException
/* 1581:     */   }
/* 1582:     */   
/* 1583:     */   public static long getBlockIdAtHeight(int paramInt)
/* 1584:     */   {
/* 1585: 679 */     Block localBlock = (Block)lastBlock.get();
/* 1586: 680 */     if (paramInt > localBlock.getHeight()) {
/* 1587: 681 */       throw new IllegalArgumentException("Invalid height " + paramInt + ", current blockchain is at " + localBlock.getHeight());
/* 1588:     */     }
/* 1589: 683 */     if (paramInt == localBlock.getHeight()) {
/* 1590: 684 */       return localBlock.getId().longValue();
/* 1591:     */     }
/* 1592: 686 */     return Block.findBlockIdAtHeight(paramInt);
/* 1593:     */   }
/* 1594:     */   
/* 1595:     */   /* Error */
/* 1596:     */   public static java.util.List<Block> getBlocksFromHeight(int paramInt)
/* 1597:     */   {
/* 1598:     */     // Byte code:
/* 1599:     */     //   0: iload_0
/* 1600:     */     //   1: iflt +23 -> 24
/* 1601:     */     //   4: getstatic 6	nxt/Blockchain:lastBlock	Ljava/util/concurrent/atomic/AtomicReference;
/* 1602:     */     //   7: invokevirtual 69	java/util/concurrent/atomic/AtomicReference:get	()Ljava/lang/Object;
/* 1603:     */     //   10: checkcast 70	nxt/Block
/* 1604:     */     //   13: invokevirtual 71	nxt/Block:getHeight	()I
/* 1605:     */     //   16: iload_0
/* 1606:     */     //   17: isub
/* 1607:     */     //   18: sipush 1440
/* 1608:     */     //   21: if_icmple +13 -> 34
/* 1609:     */     //   24: new 50	java/lang/IllegalArgumentException
/* 1610:     */     //   27: dup
/* 1611:     */     //   28: ldc 81
/* 1612:     */     //   30: invokespecial 52	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
/* 1613:     */     //   33: athrow
/* 1614:     */     //   34: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
/* 1615:     */     //   37: astore_1
/* 1616:     */     //   38: aconst_null
/* 1617:     */     //   39: astore_2
/* 1618:     */     //   40: aload_1
/* 1619:     */     //   41: ldc 82
/* 1620:     */     //   43: invokeinterface 12 2 0
/* 1621:     */     //   48: astore_3
/* 1622:     */     //   49: aconst_null
/* 1623:     */     //   50: astore 4
/* 1624:     */     //   52: aload_3
/* 1625:     */     //   53: iconst_1
/* 1626:     */     //   54: iload_0
/* 1627:     */     //   55: invokeinterface 24 3 0
/* 1628:     */     //   60: aload_3
/* 1629:     */     //   61: invokeinterface 30 1 0
/* 1630:     */     //   66: astore 5
/* 1631:     */     //   68: new 57	java/util/ArrayList
/* 1632:     */     //   71: dup
/* 1633:     */     //   72: invokespecial 58	java/util/ArrayList:<init>	()V
/* 1634:     */     //   75: astore 6
/* 1635:     */     //   77: aload 5
/* 1636:     */     //   79: invokeinterface 31 1 0
/* 1637:     */     //   84: ifeq +20 -> 104
/* 1638:     */     //   87: aload 6
/* 1639:     */     //   89: aload_1
/* 1640:     */     //   90: aload 5
/* 1641:     */     //   92: invokestatic 66	nxt/Block:getBlock	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/Block;
/* 1642:     */     //   95: invokeinterface 64 2 0
/* 1643:     */     //   100: pop
/* 1644:     */     //   101: goto -24 -> 77
/* 1645:     */     //   104: aload 6
/* 1646:     */     //   106: astore 7
/* 1647:     */     //   108: aload_3
/* 1648:     */     //   109: ifnull +35 -> 144
/* 1649:     */     //   112: aload 4
/* 1650:     */     //   114: ifnull +24 -> 138
/* 1651:     */     //   117: aload_3
/* 1652:     */     //   118: invokeinterface 33 1 0
/* 1653:     */     //   123: goto +21 -> 144
/* 1654:     */     //   126: astore 8
/* 1655:     */     //   128: aload 4
/* 1656:     */     //   130: aload 8
/* 1657:     */     //   132: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1658:     */     //   135: goto +9 -> 144
/* 1659:     */     //   138: aload_3
/* 1660:     */     //   139: invokeinterface 33 1 0
/* 1661:     */     //   144: aload_1
/* 1662:     */     //   145: ifnull +33 -> 178
/* 1663:     */     //   148: aload_2
/* 1664:     */     //   149: ifnull +23 -> 172
/* 1665:     */     //   152: aload_1
/* 1666:     */     //   153: invokeinterface 36 1 0
/* 1667:     */     //   158: goto +20 -> 178
/* 1668:     */     //   161: astore 8
/* 1669:     */     //   163: aload_2
/* 1670:     */     //   164: aload 8
/* 1671:     */     //   166: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1672:     */     //   169: goto +9 -> 178
/* 1673:     */     //   172: aload_1
/* 1674:     */     //   173: invokeinterface 36 1 0
/* 1675:     */     //   178: aload 7
/* 1676:     */     //   180: areturn
/* 1677:     */     //   181: astore 5
/* 1678:     */     //   183: aload 5
/* 1679:     */     //   185: astore 4
/* 1680:     */     //   187: aload 5
/* 1681:     */     //   189: athrow
/* 1682:     */     //   190: astore 9
/* 1683:     */     //   192: aload_3
/* 1684:     */     //   193: ifnull +35 -> 228
/* 1685:     */     //   196: aload 4
/* 1686:     */     //   198: ifnull +24 -> 222
/* 1687:     */     //   201: aload_3
/* 1688:     */     //   202: invokeinterface 33 1 0
/* 1689:     */     //   207: goto +21 -> 228
/* 1690:     */     //   210: astore 10
/* 1691:     */     //   212: aload 4
/* 1692:     */     //   214: aload 10
/* 1693:     */     //   216: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1694:     */     //   219: goto +9 -> 228
/* 1695:     */     //   222: aload_3
/* 1696:     */     //   223: invokeinterface 33 1 0
/* 1697:     */     //   228: aload 9
/* 1698:     */     //   230: athrow
/* 1699:     */     //   231: astore_3
/* 1700:     */     //   232: aload_3
/* 1701:     */     //   233: astore_2
/* 1702:     */     //   234: aload_3
/* 1703:     */     //   235: athrow
/* 1704:     */     //   236: astore 11
/* 1705:     */     //   238: aload_1
/* 1706:     */     //   239: ifnull +33 -> 272
/* 1707:     */     //   242: aload_2
/* 1708:     */     //   243: ifnull +23 -> 266
/* 1709:     */     //   246: aload_1
/* 1710:     */     //   247: invokeinterface 36 1 0
/* 1711:     */     //   252: goto +20 -> 272
/* 1712:     */     //   255: astore 12
/* 1713:     */     //   257: aload_2
/* 1714:     */     //   258: aload 12
/* 1715:     */     //   260: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 1716:     */     //   263: goto +9 -> 272
/* 1717:     */     //   266: aload_1
/* 1718:     */     //   267: invokeinterface 36 1 0
/* 1719:     */     //   272: aload 11
/* 1720:     */     //   274: athrow
/* 1721:     */     //   275: astore_1
/* 1722:     */     //   276: new 20	java/lang/RuntimeException
/* 1723:     */     //   279: dup
/* 1724:     */     //   280: aload_1
/* 1725:     */     //   281: invokevirtual 68	java/lang/Exception:toString	()Ljava/lang/String;
/* 1726:     */     //   284: aload_1
/* 1727:     */     //   285: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/* 1728:     */     //   288: athrow
/* 1729:     */     // Line number table:
/* 1730:     */     //   Java source line #690	-> byte code offset #0
/* 1731:     */     //   Java source line #691	-> byte code offset #24
/* 1732:     */     //   Java source line #693	-> byte code offset #34
/* 1733:     */     //   Java source line #694	-> byte code offset #40
/* 1734:     */     //   Java source line #693	-> byte code offset #49
/* 1735:     */     //   Java source line #695	-> byte code offset #52
/* 1736:     */     //   Java source line #696	-> byte code offset #60
/* 1737:     */     //   Java source line #697	-> byte code offset #68
/* 1738:     */     //   Java source line #698	-> byte code offset #77
/* 1739:     */     //   Java source line #699	-> byte code offset #87
/* 1740:     */     //   Java source line #701	-> byte code offset #104
/* 1741:     */     //   Java source line #702	-> byte code offset #108
/* 1742:     */     //   Java source line #693	-> byte code offset #181
/* 1743:     */     //   Java source line #702	-> byte code offset #190
/* 1744:     */     //   Java source line #693	-> byte code offset #231
/* 1745:     */     //   Java source line #702	-> byte code offset #236
/* 1746:     */     //   Java source line #703	-> byte code offset #276
/* 1747:     */     // Local variable table:
/* 1748:     */     //   start	length	slot	name	signature
/* 1749:     */     //   0	289	0	paramInt	int
/* 1750:     */     //   37	230	1	localConnection	Connection
/* 1751:     */     //   275	10	1	localSQLException	SQLException
/* 1752:     */     //   39	219	2	localObject1	Object
/* 1753:     */     //   48	175	3	localPreparedStatement	PreparedStatement
/* 1754:     */     //   231	4	3	localThrowable1	Throwable
/* 1755:     */     //   50	163	4	localObject2	Object
/* 1756:     */     //   66	25	5	localResultSet	ResultSet
/* 1757:     */     //   181	7	5	localThrowable2	Throwable
/* 1758:     */     //   75	30	6	localArrayList1	java.util.ArrayList
/* 1759:     */     //   126	5	8	localThrowable3	Throwable
/* 1760:     */     //   161	4	8	localThrowable4	Throwable
/* 1761:     */     //   190	39	9	localObject3	Object
/* 1762:     */     //   210	5	10	localThrowable5	Throwable
/* 1763:     */     //   236	37	11	localObject4	Object
/* 1764:     */     //   255	4	12	localThrowable6	Throwable
/* 1765:     */     // Exception table:
/* 1766:     */     //   from	to	target	type
/* 1767:     */     //   117	123	126	java/lang/Throwable
/* 1768:     */     //   152	158	161	java/lang/Throwable
/* 1769:     */     //   52	108	181	java/lang/Throwable
/* 1770:     */     //   52	108	190	finally
/* 1771:     */     //   181	192	190	finally
/* 1772:     */     //   201	207	210	java/lang/Throwable
/* 1773:     */     //   40	144	231	java/lang/Throwable
/* 1774:     */     //   181	231	231	java/lang/Throwable
/* 1775:     */     //   40	144	236	finally
/* 1776:     */     //   181	238	236	finally
/* 1777:     */     //   246	252	255	java/lang/Throwable
/* 1778:     */     //   34	178	275	java/sql/SQLException
/* 1779:     */     //   34	178	275	nxt/NxtException$ValidationException
/* 1780:     */     //   181	275	275	java/sql/SQLException
/* 1781:     */     //   181	275	275	nxt/NxtException$ValidationException
/* 1782:     */   }
/* 1783:     */   
/* 1784:     */   public static Collection<Transaction> getAllUnconfirmedTransactions()
/* 1785:     */   {
/* 1786: 708 */     return allUnconfirmedTransactions;
/* 1787:     */   }
/* 1788:     */   
/* 1789:     */   public static Block getLastBlock()
/* 1790:     */   {
/* 1791: 712 */     return (Block)lastBlock.get();
/* 1792:     */   }
/* 1793:     */   
/* 1794:     */   public static Block getBlock(Long paramLong)
/* 1795:     */   {
/* 1796: 716 */     return Block.findBlock(paramLong);
/* 1797:     */   }
/* 1798:     */   
/* 1799:     */   public static Transaction getTransaction(Long paramLong)
/* 1800:     */   {
/* 1801: 720 */     return Transaction.findTransaction(paramLong);
/* 1802:     */   }
/* 1803:     */   
/* 1804:     */   public static Transaction getUnconfirmedTransaction(Long paramLong)
/* 1805:     */   {
/* 1806: 724 */     return (Transaction)unconfirmedTransactions.get(paramLong);
/* 1807:     */   }
/* 1808:     */   
/* 1809:     */   public static void broadcast(Transaction paramTransaction)
/* 1810:     */   {
/* 1811: 729 */     JSONObject localJSONObject = new JSONObject();
/* 1812: 730 */     localJSONObject.put("requestType", "processTransactions");
/* 1813: 731 */     JSONArray localJSONArray = new JSONArray();
/* 1814: 732 */     localJSONArray.add(paramTransaction.getJSONObject());
/* 1815: 733 */     localJSONObject.put("transactions", localJSONArray);
/* 1816:     */     
/* 1817: 735 */     Peer.sendToSomePeers(localJSONObject);
/* 1818:     */     
/* 1819: 737 */     nonBroadcastedTransactions.put(paramTransaction.getId(), paramTransaction);
/* 1820:     */   }
/* 1821:     */   
/* 1822:     */   public static Peer getLastBlockchainFeeder()
/* 1823:     */   {
/* 1824: 741 */     return lastBlockchainFeeder;
/* 1825:     */   }
/* 1826:     */   
/* 1827:     */   public static void processTransactions(JSONObject paramJSONObject)
/* 1828:     */     throws NxtException.ValidationException
/* 1829:     */   {
/* 1830: 745 */     JSONArray localJSONArray = (JSONArray)paramJSONObject.get("transactions");
/* 1831: 746 */     processTransactions(localJSONArray, false);
/* 1832:     */   }
/* 1833:     */   
/* 1834:     */   public static boolean pushBlock(JSONObject paramJSONObject)
/* 1835:     */     throws NxtException.ValidationException
/* 1836:     */   {
/* 1837: 751 */     Block localBlock = Block.getBlock(paramJSONObject);
/* 1838: 752 */     if (!((Block)lastBlock.get()).getId().equals(localBlock.getPreviousBlockId())) {
/* 1839: 755 */       return false;
/* 1840:     */     }
/* 1841: 757 */     JSONArray localJSONArray = (JSONArray)paramJSONObject.get("transactions");
/* 1842: 758 */     Transaction[] arrayOfTransaction = new Transaction[localJSONArray.size()];
/* 1843: 759 */     for (int i = 0; i < arrayOfTransaction.length; i++) {
/* 1844: 760 */       arrayOfTransaction[i] = Transaction.getTransaction((JSONObject)localJSONArray.get(i));
/* 1845:     */     }
/* 1846: 762 */     return pushBlock(localBlock, arrayOfTransaction);
/* 1847:     */   }
/* 1848:     */   
/* 1849:     */   static void addBlock(Block paramBlock)
/* 1850:     */   {
/* 1851:     */     try
/* 1852:     */     {
/* 1853: 767 */       Connection localConnection = Db.getConnection();Object localObject1 = null;
/* 1854:     */       try
/* 1855:     */       {
/* 1856:     */         try
/* 1857:     */         {
/* 1858: 769 */           Block.saveBlock(localConnection, paramBlock);
/* 1859: 770 */           lastBlock.set(paramBlock);
/* 1860: 771 */           localConnection.commit();
/* 1861:     */         }
/* 1862:     */         catch (SQLException localSQLException2)
/* 1863:     */         {
/* 1864: 773 */           localConnection.rollback();
/* 1865: 774 */           throw localSQLException2;
/* 1866:     */         }
/* 1867:     */       }
/* 1868:     */       catch (Throwable localThrowable2)
/* 1869:     */       {
/* 1870: 767 */         localObject1 = localThrowable2;throw localThrowable2;
/* 1871:     */       }
/* 1872:     */       finally
/* 1873:     */       {
/* 1874: 776 */         if (localConnection != null) {
/* 1875: 776 */           if (localObject1 != null) {
/* 1876:     */             try
/* 1877:     */             {
/* 1878: 776 */               localConnection.close();
/* 1879:     */             }
/* 1880:     */             catch (Throwable localThrowable3)
/* 1881:     */             {
/* 1882: 776 */               localObject1.addSuppressed(localThrowable3);
/* 1883:     */             }
/* 1884:     */           } else {
/* 1885: 776 */             localConnection.close();
/* 1886:     */           }
/* 1887:     */         }
/* 1888:     */       }
/* 1889:     */     }
/* 1890:     */     catch (SQLException localSQLException1)
/* 1891:     */     {
/* 1892: 777 */       throw new RuntimeException(localSQLException1.toString(), localSQLException1);
/* 1893:     */     }
/* 1894:     */   }
/* 1895:     */   
/* 1896:     */   static void init()
/* 1897:     */   {
/* 1898: 783 */     if (!Block.hasBlock(Genesis.GENESIS_BLOCK_ID))
/* 1899:     */     {
/* 1900: 784 */       Logger.logMessage("Genesis block not in database, starting from scratch");
/* 1901:     */       
/* 1902: 786 */       TreeMap localTreeMap = new TreeMap();
/* 1903:     */       try
/* 1904:     */       {
/* 1905: 790 */         for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++)
/* 1906:     */         {
/* 1907: 791 */           localObject1 = Transaction.newTransaction(0, (short)0, Genesis.CREATOR_PUBLIC_KEY, Genesis.GENESIS_RECIPIENTS[i], Genesis.GENESIS_AMOUNTS[i], 0, null, Genesis.GENESIS_SIGNATURES[i]);
/* 1908:     */           
/* 1909: 793 */           ((Transaction)localObject1).setIndex(transactionCounter.incrementAndGet());
/* 1910: 794 */           localTreeMap.put(((Transaction)localObject1).getId(), localObject1);
/* 1911:     */         }
/* 1912: 797 */         Block localBlock = new Block(-1, 0, null, localTreeMap.size(), 1000000000, 0, localTreeMap.size() * 128, null, Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE);
/* 1913:     */         
/* 1914: 799 */         localBlock.setIndex(blockCounter.incrementAndGet());
/* 1915:     */         
/* 1916: 801 */         Object localObject1 = (Transaction[])localTreeMap.values().toArray(new Transaction[localTreeMap.size()]);
/* 1917: 802 */         MessageDigest localMessageDigest = Crypto.sha256();
/* 1918: 803 */         for (int j = 0; j < localObject1.length; j++)
/* 1919:     */         {
/* 1920: 804 */           Object localObject2 = localObject1[j];
/* 1921: 805 */           localBlock.transactionIds[j] = localObject2.getId();
/* 1922: 806 */           localBlock.blockTransactions[j] = localObject2;
/* 1923: 807 */           localMessageDigest.update(localObject2.getBytes());
/* 1924:     */         }
/* 1925: 810 */         localBlock.setPayloadHash(localMessageDigest.digest());
/* 1926: 812 */         for (Transaction localTransaction : localBlock.blockTransactions) {
/* 1927: 813 */           localTransaction.setBlock(localBlock);
/* 1928:     */         }
/* 1929: 816 */         addBlock(localBlock);
/* 1930:     */       }
/* 1931:     */       catch (NxtException.ValidationException localValidationException)
/* 1932:     */       {
/* 1933: 819 */         Logger.logMessage(localValidationException.getMessage());
/* 1934: 820 */         System.exit(1);
/* 1935:     */       }
/* 1936:     */     }
/* 1937: 824 */     Logger.logMessage("Scanning blockchain...");
/* 1938: 825 */     scan();
/* 1939: 826 */     Logger.logMessage("...Done");
/* 1940:     */   }
/* 1941:     */   
/* 1942:     */   private static void processUnconfirmedTransactions(JSONObject paramJSONObject)
/* 1943:     */     throws NxtException.ValidationException
/* 1944:     */   {
/* 1945: 830 */     JSONArray localJSONArray = (JSONArray)paramJSONObject.get("unconfirmedTransactions");
/* 1946: 831 */     processTransactions(localJSONArray, true);
/* 1947:     */   }
/* 1948:     */   
/* 1949:     */   private static void processTransactions(JSONArray paramJSONArray, boolean paramBoolean)
/* 1950:     */     throws NxtException.ValidationException
/* 1951:     */   {
/* 1952: 836 */     JSONArray localJSONArray = new JSONArray();
/* 1953: 838 */     for (Object localObject1 = paramJSONArray.iterator(); ((Iterator)localObject1).hasNext();)
/* 1954:     */     {
/* 1955: 838 */       Object localObject2 = ((Iterator)localObject1).next();
/* 1956:     */       try
/* 1957:     */       {
/* 1958: 842 */         Transaction localTransaction = Transaction.getTransaction((JSONObject)localObject2);
/* 1959:     */         
/* 1960: 844 */         int i = Convert.getEpochTime();
/* 1961: 845 */         if ((localTransaction.getTimestamp() > i + 15) || (localTransaction.getExpiration() < i) || (localTransaction.getDeadline() <= 1440))
/* 1962:     */         {
/* 1963:     */           boolean bool;
/* 1964: 852 */           synchronized (Blockchain.class)
/* 1965:     */           {
/* 1966: 854 */             localObject3 = localTransaction.getId();
/* 1967: 855 */             if (((!Transaction.hasTransaction((Long)localObject3)) && (!unconfirmedTransactions.containsKey(localObject3)) && (!doubleSpendingTransactions.containsKey(localObject3)) && (!localTransaction.verify())) || 
/* 1968:     */             
/* 1969:     */ 
/* 1970:     */ 
/* 1971:     */ 
/* 1972: 860 */               (transactionHashes.containsKey(localTransaction.getHash()))) {
/* 1973:     */               continue;
/* 1974:     */             }
/* 1975: 864 */             bool = localTransaction.isDoubleSpending();
/* 1976:     */             
/* 1977: 866 */             localTransaction.setIndex(transactionCounter.incrementAndGet());
/* 1978: 868 */             if (bool)
/* 1979:     */             {
/* 1980: 870 */               doubleSpendingTransactions.put(localObject3, localTransaction);
/* 1981:     */             }
/* 1982:     */             else
/* 1983:     */             {
/* 1984: 874 */               unconfirmedTransactions.put(localObject3, localTransaction);
/* 1985: 876 */               if (!paramBoolean) {
/* 1986: 878 */                 localJSONArray.add(localObject2);
/* 1987:     */               }
/* 1988:     */             }
/* 1989:     */           }
/* 1990: 886 */           ??? = new JSONObject();
/* 1991: 887 */           ((JSONObject)???).put("response", "processNewData");
/* 1992:     */           
/* 1993: 889 */           Object localObject3 = new JSONArray();
/* 1994: 890 */           JSONObject localJSONObject = new JSONObject();
/* 1995: 891 */           localJSONObject.put("index", Integer.valueOf(localTransaction.getIndex()));
/* 1996: 892 */           localJSONObject.put("timestamp", Integer.valueOf(localTransaction.getTimestamp()));
/* 1997: 893 */           localJSONObject.put("deadline", Short.valueOf(localTransaction.getDeadline()));
/* 1998: 894 */           localJSONObject.put("recipient", Convert.convert(localTransaction.getRecipientId()));
/* 1999: 895 */           localJSONObject.put("amount", Integer.valueOf(localTransaction.getAmount()));
/* 2000: 896 */           localJSONObject.put("fee", Integer.valueOf(localTransaction.getFee()));
/* 2001: 897 */           localJSONObject.put("sender", Convert.convert(localTransaction.getSenderAccountId()));
/* 2002: 898 */           localJSONObject.put("id", localTransaction.getStringId());
/* 2003: 899 */           ((JSONArray)localObject3).add(localJSONObject);
/* 2004: 901 */           if (bool) {
/* 2005: 903 */             ((JSONObject)???).put("addedDoubleSpendingTransactions", localObject3);
/* 2006:     */           } else {
/* 2007: 907 */             ((JSONObject)???).put("addedUnconfirmedTransactions", localObject3);
/* 2008:     */           }
/* 2009: 911 */           User.sendToAll((JSONStreamAware)???);
/* 2010:     */         }
/* 2011:     */       }
/* 2012:     */       catch (RuntimeException localRuntimeException)
/* 2013:     */       {
/* 2014: 915 */         Logger.logMessage("Error processing transaction", localRuntimeException);
/* 2015:     */       }
/* 2016:     */     }
/* 2017: 921 */     if (localJSONArray.size() > 0)
/* 2018:     */     {
/* 2019: 923 */       localObject1 = new JSONObject();
/* 2020: 924 */       ((JSONObject)localObject1).put("requestType", "processTransactions");
/* 2021: 925 */       ((JSONObject)localObject1).put("transactions", localJSONArray);
/* 2022:     */       
/* 2023: 927 */       Peer.sendToSomePeers((JSONObject)localObject1);
/* 2024:     */     }
/* 2025:     */   }
/* 2026:     */   
/* 2027:     */   private static synchronized byte[] calculateTransactionsChecksum()
/* 2028:     */   {
/* 2029: 934 */     PriorityQueue localPriorityQueue = new PriorityQueue(getTransactionCount(), new Comparator()
/* 2030:     */     {
/* 2031:     */       public int compare(Transaction paramAnonymousTransaction1, Transaction paramAnonymousTransaction2)
/* 2032:     */       {
/* 2033: 937 */         long l1 = paramAnonymousTransaction1.getId().longValue();
/* 2034: 938 */         long l2 = paramAnonymousTransaction2.getId().longValue();
/* 2035: 939 */         return paramAnonymousTransaction1.getTimestamp() > paramAnonymousTransaction2.getTimestamp() ? 1 : paramAnonymousTransaction1.getTimestamp() < paramAnonymousTransaction2.getTimestamp() ? -1 : l1 > l2 ? 1 : l1 < l2 ? -1 : 0;
/* 2036:     */       }
/* 2037: 941 */     });
/* 2038: 942 */     Object localObject1 = getAllTransactions();Object localObject2 = null;
/* 2039:     */     try
/* 2040:     */     {
/* 2041: 943 */       while (((DbIterator)localObject1).hasNext()) {
/* 2042: 944 */         localPriorityQueue.add(((DbIterator)localObject1).next());
/* 2043:     */       }
/* 2044:     */     }
/* 2045:     */     catch (Throwable localThrowable2)
/* 2046:     */     {
/* 2047: 942 */       localObject2 = localThrowable2;throw localThrowable2;
/* 2048:     */     }
/* 2049:     */     finally
/* 2050:     */     {
/* 2051: 946 */       if (localObject1 != null) {
/* 2052: 946 */         if (localObject2 != null) {
/* 2053:     */           try
/* 2054:     */           {
/* 2055: 946 */             ((DbIterator)localObject1).close();
/* 2056:     */           }
/* 2057:     */           catch (Throwable localThrowable3)
/* 2058:     */           {
/* 2059: 946 */             localObject2.addSuppressed(localThrowable3);
/* 2060:     */           }
/* 2061:     */         } else {
/* 2062: 946 */           ((DbIterator)localObject1).close();
/* 2063:     */         }
/* 2064:     */       }
/* 2065:     */     }
/* 2066: 947 */     localObject1 = Crypto.sha256();
/* 2067: 948 */     while (!localPriorityQueue.isEmpty()) {
/* 2068: 949 */       ((MessageDigest)localObject1).update(((Transaction)localPriorityQueue.poll()).getBytes());
/* 2069:     */     }
/* 2070: 951 */     return ((MessageDigest)localObject1).digest();
/* 2071:     */   }
/* 2072:     */   
/* 2073:     */   private static boolean pushBlock(Block paramBlock, Transaction[] paramArrayOfTransaction)
/* 2074:     */   {
/* 2075: 958 */     int i = Convert.getEpochTime();
/* 2076:     */     JSONArray localJSONArray1;
/* 2077:     */     JSONArray localJSONArray2;
/* 2078: 960 */     synchronized (Blockchain.class)
/* 2079:     */     {
/* 2080:     */       try
/* 2081:     */       {
/* 2082: 963 */         Block localBlock = (Block)lastBlock.get();
/* 2083: 965 */         if (paramBlock.getVersion() != (localBlock.getHeight() < 30000 ? 1 : 2)) {
/* 2084: 967 */           return false;
/* 2085:     */         }
/* 2086: 970 */         if (localBlock.getHeight() == 30000)
/* 2087:     */         {
/* 2088: 972 */           localObject1 = calculateTransactionsChecksum();
/* 2089: 973 */           if (CHECKSUM_TRANSPARENT_FORGING == null)
/* 2090:     */           {
/* 2091: 974 */             Logger.logMessage("Checksum calculated:\n" + Arrays.toString((byte[])localObject1));
/* 2092:     */           }
/* 2093:     */           else
/* 2094:     */           {
/* 2095: 975 */             if (!Arrays.equals((byte[])localObject1, CHECKSUM_TRANSPARENT_FORGING))
/* 2096:     */             {
/* 2097: 976 */               Logger.logMessage("Checksum failed at block 30000");
/* 2098: 977 */               return false;
/* 2099:     */             }
/* 2100: 979 */             Logger.logMessage("Checksum passed at block 30000");
/* 2101:     */           }
/* 2102:     */         }
/* 2103: 984 */         if ((paramBlock.getVersion() != 1) && (!Arrays.equals(Crypto.sha256().digest(localBlock.getBytes()), paramBlock.getPreviousBlockHash()))) {
/* 2104: 986 */           return false;
/* 2105:     */         }
/* 2106: 989 */         if ((paramBlock.getTimestamp() > i + 15) || (paramBlock.getTimestamp() <= localBlock.getTimestamp())) {
/* 2107: 991 */           return false;
/* 2108:     */         }
/* 2109: 994 */         if ((!localBlock.getId().equals(paramBlock.getPreviousBlockId())) || (paramBlock.getId().equals(Long.valueOf(0L))) || (Block.hasBlock(paramBlock.getId())) || (!paramBlock.verifyGenerationSignature()) || (!paramBlock.verifyBlockSignature())) {
/* 2110: 998 */           return false;
/* 2111:     */         }
/* 2112:1001 */         paramBlock.setIndex(blockCounter.incrementAndGet());
/* 2113:     */         
/* 2114:1003 */         localObject1 = new HashMap();
/* 2115:1004 */         HashMap localHashMap1 = new HashMap();
/* 2116:1005 */         for (int j = 0; j < paramBlock.transactionIds.length; j++)
/* 2117:     */         {
/* 2118:1007 */           localObject2 = paramArrayOfTransaction[j];
/* 2119:1008 */           ((Transaction)localObject2).setIndex(transactionCounter.incrementAndGet());
/* 2120:1010 */           if (((Map)localObject1).put(paramBlock.transactionIds[j] =  = ((Transaction)localObject2).getId(), localObject2) != null) {
/* 2121:1012 */             return false;
/* 2122:     */           }
/* 2123:1015 */           if (((Transaction)localObject2).isDuplicate(localHashMap1)) {
/* 2124:1017 */             return false;
/* 2125:     */           }
/* 2126:     */         }
/* 2127:1021 */         Arrays.sort(paramBlock.transactionIds);
/* 2128:     */         
/* 2129:1023 */         HashMap localHashMap2 = new HashMap();
/* 2130:1024 */         Object localObject2 = new HashMap();
/* 2131:1025 */         int k = 0;int m = 0;
/* 2132:1026 */         MessageDigest localMessageDigest = Crypto.sha256();
/* 2133:     */         Object localObject5;
/* 2134:1027 */         for (int n = 0; n < paramBlock.transactionIds.length; n++)
/* 2135:     */         {
/* 2136:1029 */           localObject4 = paramBlock.transactionIds[n];
/* 2137:1030 */           localObject5 = (Transaction)((Map)localObject1).get(localObject4);
/* 2138:1032 */           if ((((Transaction)localObject5).getTimestamp() > i + 15) || ((((Transaction)localObject5).getExpiration() < paramBlock.getTimestamp()) && (localBlock.getHeight() > 303)) || (Transaction.hasTransaction((Long)localObject4)) || ((((Transaction)localObject5).getReferencedTransactionId() != null) && (!Transaction.hasTransaction(((Transaction)localObject5).getReferencedTransactionId())) && (((Map)localObject1).get(((Transaction)localObject5).getReferencedTransactionId()) == null)) || ((unconfirmedTransactions.get(localObject4) == null) && (!((Transaction)localObject5).verify())) || (((Transaction)localObject5).getId().equals(Long.valueOf(0L)))) {
/* 2139:1041 */             return false;
/* 2140:     */           }
/* 2141:1044 */           paramBlock.blockTransactions[n] = localObject5;
/* 2142:     */           
/* 2143:1046 */           k += ((Transaction)localObject5).getAmount();
/* 2144:     */           
/* 2145:1048 */           ((Transaction)localObject5).updateTotals(localHashMap2, (Map)localObject2);
/* 2146:     */           
/* 2147:1050 */           m += ((Transaction)localObject5).getFee();
/* 2148:     */           
/* 2149:1052 */           localMessageDigest.update(((Transaction)localObject5).getBytes());
/* 2150:     */         }
/* 2151:1056 */         if ((k != paramBlock.getTotalAmount()) || (m != paramBlock.getTotalFee())) {
/* 2152:1058 */           return false;
/* 2153:     */         }
/* 2154:1061 */         if (!Arrays.equals(localMessageDigest.digest(), paramBlock.getPayloadHash())) {
/* 2155:1063 */           return false;
/* 2156:     */         }
/* 2157:1066 */         for (Object localObject3 = localHashMap2.entrySet().iterator(); ((Iterator)localObject3).hasNext();)
/* 2158:     */         {
/* 2159:1066 */           localObject4 = (Map.Entry)((Iterator)localObject3).next();
/* 2160:1067 */           localObject5 = Account.getAccount((Long)((Map.Entry)localObject4).getKey());
/* 2161:1068 */           if (((Account)localObject5).getBalance() < ((Long)((Map.Entry)localObject4).getValue()).longValue()) {
/* 2162:1070 */             return false;
/* 2163:     */           }
/* 2164:     */         }
/* 2165:1074 */         for (localObject3 = ((Map)localObject2).entrySet().iterator(); ((Iterator)localObject3).hasNext();)
/* 2166:     */         {
/* 2167:1074 */           localObject4 = (Map.Entry)((Iterator)localObject3).next();
/* 2168:1075 */           localObject5 = Account.getAccount((Long)((Map.Entry)localObject4).getKey());
/* 2169:1076 */           for (localIterator = ((Map)((Map.Entry)localObject4).getValue()).entrySet().iterator(); localIterator.hasNext();)
/* 2170:     */           {
/* 2171:1076 */             localObject6 = (Map.Entry)localIterator.next();
/* 2172:1077 */             long l1 = ((Long)((Map.Entry)localObject6).getKey()).longValue();
/* 2173:1078 */             long l2 = ((Long)((Map.Entry)localObject6).getValue()).longValue();
/* 2174:1079 */             if (((Account)localObject5).getAssetBalance(Long.valueOf(l1)).intValue() < l2) {
/* 2175:1081 */               return false;
/* 2176:     */             }
/* 2177:     */           }
/* 2178:     */         }
/* 2179:     */         Iterator localIterator;
/* 2180:1086 */         paramBlock.setHeight(localBlock.getHeight() + 1);
/* 2181:     */         
/* 2182:1088 */         localObject3 = null;
/* 2183:1089 */         for (localObject6 : paramBlock.blockTransactions)
/* 2184:     */         {
/* 2185:1090 */           ((Transaction)localObject6).setBlock(paramBlock);
/* 2186:1092 */           if ((transactionHashes.putIfAbsent(((Transaction)localObject6).getHash(), localObject6) != null) && (paramBlock.getHeight() != 58294))
/* 2187:     */           {
/* 2188:1094 */             localObject3 = localObject6;
/* 2189:1095 */             break;
/* 2190:     */           }
/* 2191:     */         }
/* 2192:1099 */         if (localObject3 != null)
/* 2193:     */         {
/* 2194:1100 */           for (localObject6 : paramBlock.blockTransactions) {
/* 2195:1101 */             if (!((Transaction)localObject6).equals(localObject3))
/* 2196:     */             {
/* 2197:1102 */               localTransaction2 = (Transaction)transactionHashes.get(((Transaction)localObject6).getHash());
/* 2198:1103 */               if ((localTransaction2 != null) && (localTransaction2.equals(localObject6))) {
/* 2199:1104 */                 transactionHashes.remove(((Transaction)localObject6).getHash());
/* 2200:     */               }
/* 2201:     */             }
/* 2202:     */           }
/* 2203:1108 */           return false;
/* 2204:     */         }
/* 2205:1111 */         paramBlock.calculateBaseTarget();
/* 2206:     */         
/* 2207:1113 */         addBlock(paramBlock);
/* 2208:     */         
/* 2209:1115 */         paramBlock.apply();
/* 2210:     */         
/* 2211:1117 */         localJSONArray1 = new JSONArray();
/* 2212:1118 */         localJSONArray2 = new JSONArray();
/* 2213:1120 */         for (localObject4 = ((Map)localObject1).entrySet().iterator(); ((Iterator)localObject4).hasNext();)
/* 2214:     */         {
/* 2215:1120 */           Map.Entry localEntry = (Map.Entry)((Iterator)localObject4).next();
/* 2216:     */           
/* 2217:1122 */           Transaction localTransaction1 = (Transaction)localEntry.getValue();
/* 2218:     */           
/* 2219:1124 */           localObject6 = new JSONObject();
/* 2220:1125 */           ((JSONObject)localObject6).put("index", Integer.valueOf(localTransaction1.getIndex()));
/* 2221:1126 */           ((JSONObject)localObject6).put("blockTimestamp", Integer.valueOf(paramBlock.getTimestamp()));
/* 2222:1127 */           ((JSONObject)localObject6).put("transactionTimestamp", Integer.valueOf(localTransaction1.getTimestamp()));
/* 2223:1128 */           ((JSONObject)localObject6).put("sender", Convert.convert(localTransaction1.getSenderAccountId()));
/* 2224:1129 */           ((JSONObject)localObject6).put("recipient", Convert.convert(localTransaction1.getRecipientId()));
/* 2225:1130 */           ((JSONObject)localObject6).put("amount", Integer.valueOf(localTransaction1.getAmount()));
/* 2226:1131 */           ((JSONObject)localObject6).put("fee", Integer.valueOf(localTransaction1.getFee()));
/* 2227:1132 */           ((JSONObject)localObject6).put("id", localTransaction1.getStringId());
/* 2228:1133 */           localJSONArray1.add(localObject6);
/* 2229:     */           
/* 2230:1135 */           localTransaction2 = (Transaction)unconfirmedTransactions.remove(localEntry.getKey());
/* 2231:1136 */           if (localTransaction2 != null)
/* 2232:     */           {
/* 2233:1137 */             JSONObject localJSONObject2 = new JSONObject();
/* 2234:1138 */             localJSONObject2.put("index", Integer.valueOf(localTransaction2.getIndex()));
/* 2235:1139 */             localJSONArray2.add(localJSONObject2);
/* 2236:     */             
/* 2237:1141 */             Account localAccount = Account.getAccount(localTransaction2.getSenderAccountId());
/* 2238:1142 */             localAccount.addToUnconfirmedBalance((localTransaction2.getAmount() + localTransaction2.getFee()) * 100L);
/* 2239:     */           }
/* 2240:     */         }
/* 2241:     */       }
/* 2242:     */       catch (RuntimeException localRuntimeException)
/* 2243:     */       {
/* 2244:     */         Object localObject4;
/* 2245:     */         Object localObject6;
/* 2246:     */         Transaction localTransaction2;
/* 2247:1150 */         Logger.logMessage("Error pushing block", localRuntimeException);
/* 2248:1151 */         return false;
/* 2249:     */       }
/* 2250:     */     }
/* 2251:1155 */     if (paramBlock.getTimestamp() >= i - 15)
/* 2252:     */     {
/* 2253:1157 */       ??? = paramBlock.getJSONObject();
/* 2254:1158 */       ((JSONObject)???).put("requestType", "processBlock");
/* 2255:     */       
/* 2256:1160 */       Peer.sendToSomePeers((JSONObject)???);
/* 2257:     */     }
/* 2258:1164 */     ??? = new JSONArray();
/* 2259:1165 */     JSONObject localJSONObject1 = new JSONObject();
/* 2260:1166 */     localJSONObject1.put("index", Integer.valueOf(paramBlock.getIndex()));
/* 2261:1167 */     localJSONObject1.put("timestamp", Integer.valueOf(paramBlock.getTimestamp()));
/* 2262:1168 */     localJSONObject1.put("numberOfTransactions", Integer.valueOf(paramBlock.transactionIds.length));
/* 2263:1169 */     localJSONObject1.put("totalAmount", Integer.valueOf(paramBlock.getTotalAmount()));
/* 2264:1170 */     localJSONObject1.put("totalFee", Integer.valueOf(paramBlock.getTotalFee()));
/* 2265:1171 */     localJSONObject1.put("payloadLength", Integer.valueOf(paramBlock.getPayloadLength()));
/* 2266:1172 */     localJSONObject1.put("generator", Convert.convert(paramBlock.getGeneratorAccountId()));
/* 2267:1173 */     localJSONObject1.put("height", Integer.valueOf(paramBlock.getHeight()));
/* 2268:1174 */     localJSONObject1.put("version", Integer.valueOf(paramBlock.getVersion()));
/* 2269:1175 */     localJSONObject1.put("block", paramBlock.getStringId());
/* 2270:1176 */     localJSONObject1.put("baseTarget", BigInteger.valueOf(paramBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
/* 2271:1177 */     ((JSONArray)???).add(localJSONObject1);
/* 2272:     */     
/* 2273:1179 */     Object localObject1 = new JSONObject();
/* 2274:1180 */     ((JSONObject)localObject1).put("response", "processNewData");
/* 2275:1181 */     ((JSONObject)localObject1).put("addedConfirmedTransactions", localJSONArray1);
/* 2276:1182 */     if (localJSONArray2.size() > 0) {
/* 2277:1183 */       ((JSONObject)localObject1).put("removedUnconfirmedTransactions", localJSONArray2);
/* 2278:     */     }
/* 2279:1185 */     ((JSONObject)localObject1).put("addedRecentBlocks", ???);
/* 2280:     */     
/* 2281:1187 */     User.sendToAll((JSONStreamAware)localObject1);
/* 2282:     */     
/* 2283:1189 */     return true;
/* 2284:     */   }
/* 2285:     */   
/* 2286:     */   private static boolean popLastBlock()
/* 2287:     */     throws Transaction.UndoNotSupportedException
/* 2288:     */   {
/* 2289:     */     try
/* 2290:     */     {
/* 2291:1197 */       JSONObject localJSONObject1 = new JSONObject();
/* 2292:1198 */       localJSONObject1.put("response", "processNewData");
/* 2293:     */       
/* 2294:1200 */       JSONArray localJSONArray = new JSONArray();
/* 2295:     */       Block localBlock;
/* 2296:1204 */       synchronized (Blockchain.class)
/* 2297:     */       {
/* 2298:1206 */         localBlock = (Block)lastBlock.get();
/* 2299:1208 */         if (localBlock.getId().equals(Genesis.GENESIS_BLOCK_ID)) {
/* 2300:1209 */           return false;
/* 2301:     */         }
/* 2302:1212 */         localObject1 = Block.findBlock(localBlock.getPreviousBlockId());
/* 2303:1213 */         if (localObject1 == null)
/* 2304:     */         {
/* 2305:1214 */           Logger.logMessage("Previous block is null");
/* 2306:1215 */           throw new IllegalStateException();
/* 2307:     */         }
/* 2308:1217 */         if (!lastBlock.compareAndSet(localBlock, localObject1))
/* 2309:     */         {
/* 2310:1218 */           Logger.logMessage("This block is no longer last block");
/* 2311:1219 */           throw new IllegalStateException();
/* 2312:     */         }
/* 2313:1222 */         Account localAccount = Account.getAccount(localBlock.getGeneratorAccountId());
/* 2314:1223 */         localAccount.addToBalanceAndUnconfirmedBalance(-localBlock.getTotalFee() * 100L);
/* 2315:1225 */         for (Transaction localTransaction1 : localBlock.blockTransactions)
/* 2316:     */         {
/* 2317:1227 */           Transaction localTransaction2 = (Transaction)transactionHashes.get(localTransaction1.getHash());
/* 2318:1228 */           if ((localTransaction2 != null) && (localTransaction2.equals(localTransaction1))) {
/* 2319:1229 */             transactionHashes.remove(localTransaction1.getHash());
/* 2320:     */           }
/* 2321:1232 */           unconfirmedTransactions.put(localTransaction1.getId(), localTransaction1);
/* 2322:     */           
/* 2323:1234 */           localTransaction1.undo();
/* 2324:     */           
/* 2325:1236 */           JSONObject localJSONObject2 = new JSONObject();
/* 2326:1237 */           localJSONObject2.put("index", Integer.valueOf(localTransaction1.getIndex()));
/* 2327:1238 */           localJSONObject2.put("timestamp", Integer.valueOf(localTransaction1.getTimestamp()));
/* 2328:1239 */           localJSONObject2.put("deadline", Short.valueOf(localTransaction1.getDeadline()));
/* 2329:1240 */           localJSONObject2.put("recipient", Convert.convert(localTransaction1.getRecipientId()));
/* 2330:1241 */           localJSONObject2.put("amount", Integer.valueOf(localTransaction1.getAmount()));
/* 2331:1242 */           localJSONObject2.put("fee", Integer.valueOf(localTransaction1.getFee()));
/* 2332:1243 */           localJSONObject2.put("sender", Convert.convert(localTransaction1.getSenderAccountId()));
/* 2333:1244 */           localJSONObject2.put("id", localTransaction1.getStringId());
/* 2334:1245 */           localJSONArray.add(localJSONObject2);
/* 2335:     */         }
/* 2336:1249 */         Block.deleteBlock(localBlock.getId());
/* 2337:     */       }
/* 2338:1253 */       ??? = new JSONArray();
/* 2339:1254 */       Object localObject1 = new JSONObject();
/* 2340:1255 */       ((JSONObject)localObject1).put("index", Integer.valueOf(localBlock.getIndex()));
/* 2341:1256 */       ((JSONObject)localObject1).put("timestamp", Integer.valueOf(localBlock.getTimestamp()));
/* 2342:1257 */       ((JSONObject)localObject1).put("numberOfTransactions", Integer.valueOf(localBlock.transactionIds.length));
/* 2343:1258 */       ((JSONObject)localObject1).put("totalAmount", Integer.valueOf(localBlock.getTotalAmount()));
/* 2344:1259 */       ((JSONObject)localObject1).put("totalFee", Integer.valueOf(localBlock.getTotalFee()));
/* 2345:1260 */       ((JSONObject)localObject1).put("payloadLength", Integer.valueOf(localBlock.getPayloadLength()));
/* 2346:1261 */       ((JSONObject)localObject1).put("generator", Convert.convert(localBlock.getGeneratorAccountId()));
/* 2347:1262 */       ((JSONObject)localObject1).put("height", Integer.valueOf(localBlock.getHeight()));
/* 2348:1263 */       ((JSONObject)localObject1).put("version", Integer.valueOf(localBlock.getVersion()));
/* 2349:1264 */       ((JSONObject)localObject1).put("block", localBlock.getStringId());
/* 2350:1265 */       ((JSONObject)localObject1).put("baseTarget", BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
/* 2351:1266 */       ((JSONArray)???).add(localObject1);
/* 2352:1267 */       localJSONObject1.put("addedOrphanedBlocks", ???);
/* 2353:1269 */       if (localJSONArray.size() > 0) {
/* 2354:1270 */         localJSONObject1.put("addedUnconfirmedTransactions", localJSONArray);
/* 2355:     */       }
/* 2356:1273 */       User.sendToAll(localJSONObject1);
/* 2357:     */     }
/* 2358:     */     catch (RuntimeException localRuntimeException)
/* 2359:     */     {
/* 2360:1276 */       Logger.logMessage("Error popping last block", localRuntimeException);
/* 2361:1277 */       return false;
/* 2362:     */     }
/* 2363:1279 */     return true;
/* 2364:     */   }
/* 2365:     */   
/* 2366:     */   private static synchronized void scan()
/* 2367:     */   {
/* 2368:1283 */     Account.clear();
/* 2369:1284 */     Alias.clear();
/* 2370:1285 */     Asset.clear();
/* 2371:1286 */     Order.clear();
/* 2372:1287 */     unconfirmedTransactions.clear();
/* 2373:1288 */     doubleSpendingTransactions.clear();
/* 2374:1289 */     nonBroadcastedTransactions.clear();
/* 2375:1290 */     transactionHashes.clear();
/* 2376:     */     try
/* 2377:     */     {
/* 2378:1291 */       Connection localConnection = Db.getConnection();Object localObject1 = null;
/* 2379:     */       try
/* 2380:     */       {
/* 2381:1291 */         PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM block ORDER BY db_id ASC");Object localObject2 = null;
/* 2382:     */         try
/* 2383:     */         {
/* 2384:1292 */           Long localLong = Genesis.GENESIS_BLOCK_ID;
/* 2385:     */           
/* 2386:1294 */           ResultSet localResultSet = localPreparedStatement.executeQuery();
/* 2387:1295 */           while (localResultSet.next())
/* 2388:     */           {
/* 2389:1296 */             Block localBlock = Block.getBlock(localConnection, localResultSet);
/* 2390:1297 */             if (!localBlock.getId().equals(localLong)) {
/* 2391:1298 */               throw new NxtException.ValidationException("Database blocks in the wrong order!");
/* 2392:     */             }
/* 2393:1300 */             lastBlock.set(localBlock);
/* 2394:1301 */             localBlock.apply();
/* 2395:1302 */             localLong = localBlock.getNextBlockId();
/* 2396:     */           }
/* 2397:     */         }
/* 2398:     */         catch (Throwable localThrowable4)
/* 2399:     */         {
/* 2400:1291 */           localObject2 = localThrowable4;throw localThrowable4;
/* 2401:     */         }
/* 2402:     */         finally {}
/* 2403:     */       }
/* 2404:     */       catch (Throwable localThrowable2)
/* 2405:     */       {
/* 2406:1291 */         localObject1 = localThrowable2;throw localThrowable2;
/* 2407:     */       }
/* 2408:     */       finally
/* 2409:     */       {
/* 2410:1304 */         if (localConnection != null) {
/* 2411:1304 */           if (localObject1 != null) {
/* 2412:     */             try
/* 2413:     */             {
/* 2414:1304 */               localConnection.close();
/* 2415:     */             }
/* 2416:     */             catch (Throwable localThrowable6)
/* 2417:     */             {
/* 2418:1304 */               localObject1.addSuppressed(localThrowable6);
/* 2419:     */             }
/* 2420:     */           } else {
/* 2421:1304 */             localConnection.close();
/* 2422:     */           }
/* 2423:     */         }
/* 2424:     */       }
/* 2425:     */     }
/* 2426:     */     catch (NxtException.ValidationException|SQLException localValidationException)
/* 2427:     */     {
/* 2428:1305 */       throw new RuntimeException(localValidationException.toString(), localValidationException);
/* 2429:     */     }
/* 2430:     */   }
/* 2431:     */   
/* 2432:     */   private static void generateBlock(String paramString)
/* 2433:     */   {
/* 2434:1311 */     TreeSet localTreeSet = new TreeSet();
/* 2435:1313 */     for (Object localObject1 = unconfirmedTransactions.values().iterator(); ((Iterator)localObject1).hasNext();)
/* 2436:     */     {
/* 2437:1313 */       localObject2 = (Transaction)((Iterator)localObject1).next();
/* 2438:1315 */       if ((((Transaction)localObject2).getReferencedTransactionId() == null) || (Transaction.hasTransaction(((Transaction)localObject2).getReferencedTransactionId()))) {
/* 2439:1317 */         localTreeSet.add(localObject2);
/* 2440:     */       }
/* 2441:     */     }
/* 2442:1323 */     localObject1 = new HashMap();
/* 2443:1324 */     Object localObject2 = new HashMap();
/* 2444:1325 */     HashMap localHashMap = new HashMap();
/* 2445:     */     
/* 2446:1327 */     int i = 0;
/* 2447:1328 */     int j = 0;
/* 2448:1329 */     int k = 0;
/* 2449:     */     Object localObject3;
/* 2450:1331 */     while (k <= 32640)
/* 2451:     */     {
/* 2452:1333 */       int m = ((Map)localObject1).size();
/* 2453:1335 */       for (localObject3 = localTreeSet.iterator(); ((Iterator)localObject3).hasNext();)
/* 2454:     */       {
/* 2455:1335 */         localObject4 = (Transaction)((Iterator)localObject3).next();
/* 2456:     */         
/* 2457:1337 */         int n = ((Transaction)localObject4).getSize();
/* 2458:1338 */         if ((((Map)localObject1).get(((Transaction)localObject4).getId()) == null) && (k + n <= 32640))
/* 2459:     */         {
/* 2460:1340 */           localObject5 = ((Transaction)localObject4).getSenderAccountId();
/* 2461:1341 */           localObject6 = (Long)localHashMap.get(localObject5);
/* 2462:1342 */           if (localObject6 == null) {
/* 2463:1344 */             localObject6 = Long.valueOf(0L);
/* 2464:     */           }
/* 2465:1348 */           long l = (((Transaction)localObject4).getAmount() + ((Transaction)localObject4).getFee()) * 100L;
/* 2466:1349 */           if (((Long)localObject6).longValue() + l <= Account.getAccount((Long)localObject5).getBalance()) {
/* 2467:1351 */             if (!((Transaction)localObject4).isDuplicate((Map)localObject2))
/* 2468:     */             {
/* 2469:1355 */               localHashMap.put(localObject5, Long.valueOf(((Long)localObject6).longValue() + l));
/* 2470:     */               
/* 2471:1357 */               ((Map)localObject1).put(((Transaction)localObject4).getId(), localObject4);
/* 2472:1358 */               k += n;
/* 2473:1359 */               i += ((Transaction)localObject4).getAmount();
/* 2474:1360 */               j += ((Transaction)localObject4).getFee();
/* 2475:     */             }
/* 2476:     */           }
/* 2477:     */         }
/* 2478:     */       }
/* 2479:1368 */       if (((Map)localObject1).size() == m) {
/* 2480:     */         break;
/* 2481:     */       }
/* 2482:     */     }
/* 2483:1376 */     byte[] arrayOfByte1 = Crypto.getPublicKey(paramString);
/* 2484:     */     
/* 2485:     */ 
/* 2486:1379 */     Object localObject4 = (Block)lastBlock.get();
/* 2487:     */     try
/* 2488:     */     {
/* 2489:1382 */       if (((Block)localObject4).getHeight() < 30000)
/* 2490:     */       {
/* 2491:1384 */         localObject3 = new Block(1, Convert.getEpochTime(), ((Block)localObject4).getId(), ((Map)localObject1).size(), i, j, k, null, arrayOfByte1, null, new byte[64]);
/* 2492:     */       }
/* 2493:     */       else
/* 2494:     */       {
/* 2495:1389 */         byte[] arrayOfByte2 = Crypto.sha256().digest(((Block)localObject4).getBytes());
/* 2496:1390 */         localObject3 = new Block(2, Convert.getEpochTime(), ((Block)localObject4).getId(), ((Map)localObject1).size(), i, j, k, null, arrayOfByte1, null, new byte[64], arrayOfByte2);
/* 2497:     */       }
/* 2498:     */     }
/* 2499:     */     catch (NxtException.ValidationException localValidationException)
/* 2500:     */     {
/* 2501:1396 */       Logger.logMessage("Error generating block", localValidationException);
/* 2502:1397 */       return;
/* 2503:     */     }
/* 2504:1400 */     int i1 = 0;
/* 2505:1401 */     for (Object localObject5 = ((Map)localObject1).keySet().iterator(); ((Iterator)localObject5).hasNext();)
/* 2506:     */     {
/* 2507:1401 */       localObject6 = (Long)((Iterator)localObject5).next();
/* 2508:1402 */       ((Block)localObject3).transactionIds[(i1++)] = localObject6;
/* 2509:     */     }
/* 2510:1405 */     Arrays.sort(((Block)localObject3).transactionIds);
/* 2511:1406 */     localObject5 = Crypto.sha256();
/* 2512:1407 */     for (i1 = 0; i1 < ((Block)localObject3).transactionIds.length; i1++)
/* 2513:     */     {
/* 2514:1408 */       localObject6 = (Transaction)((Map)localObject1).get(localObject3.transactionIds[i1]);
/* 2515:1409 */       ((MessageDigest)localObject5).update(((Transaction)localObject6).getBytes());
/* 2516:1410 */       ((Block)localObject3).blockTransactions[i1] = localObject6;
/* 2517:     */     }
/* 2518:1412 */     ((Block)localObject3).setPayloadHash(((MessageDigest)localObject5).digest());
/* 2519:1414 */     if (((Block)localObject4).getHeight() < 30000)
/* 2520:     */     {
/* 2521:1416 */       ((Block)localObject3).setGenerationSignature(Crypto.sign(((Block)localObject4).getGenerationSignature(), paramString));
/* 2522:     */     }
/* 2523:     */     else
/* 2524:     */     {
/* 2525:1420 */       ((MessageDigest)localObject5).update(((Block)localObject4).getGenerationSignature());
/* 2526:1421 */       ((Block)localObject3).setGenerationSignature(((MessageDigest)localObject5).digest(arrayOfByte1));
/* 2527:     */     }
/* 2528:1425 */     Object localObject6 = ((Block)localObject3).getBytes();
/* 2529:1426 */     byte[] arrayOfByte3 = new byte[localObject6.length - 64];
/* 2530:1427 */     System.arraycopy(localObject6, 0, arrayOfByte3, 0, arrayOfByte3.length);
/* 2531:1428 */     ((Block)localObject3).setBlockSignature(Crypto.sign(arrayOfByte3, paramString));
/* 2532:1430 */     if ((((Block)localObject3).verifyBlockSignature()) && (((Block)localObject3).verifyGenerationSignature()))
/* 2533:     */     {
/* 2534:1432 */       JSONObject localJSONObject = ((Block)localObject3).getJSONObject();
/* 2535:1433 */       localJSONObject.put("requestType", "processBlock");
/* 2536:1434 */       Peer.sendToSomePeers(localJSONObject);
/* 2537:     */     }
/* 2538:     */     else
/* 2539:     */     {
/* 2540:1438 */       Logger.logMessage("Generated an incorrect block. Waiting for the next one...");
/* 2541:     */     }
/* 2542:     */   }
/* 2543:     */   
/* 2544:     */   static void purgeExpiredHashes(int paramInt)
/* 2545:     */   {
/* 2546:1445 */     Iterator localIterator = transactionHashes.entrySet().iterator();
/* 2547:1446 */     while (localIterator.hasNext()) {
/* 2548:1447 */       if (((Transaction)((Map.Entry)localIterator.next()).getValue()).getExpiration() < paramInt) {
/* 2549:1448 */         localIterator.remove();
/* 2550:     */       }
/* 2551:     */     }
/* 2552:     */   }
/* 2553:     */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Blockchain
 * JD-Core Version:    0.7.0.1
 */