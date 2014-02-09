/*    1:     */ package nxt;
/*    2:     */ 
/*    3:     */ import java.io.IOException;
/*    4:     */ import java.io.ObjectInputStream;
/*    5:     */ import java.io.ObjectOutputStream;
/*    6:     */ import java.io.Serializable;
/*    7:     */ import java.io.UnsupportedEncodingException;
/*    8:     */ import java.math.BigInteger;
/*    9:     */ import java.nio.ByteBuffer;
/*   10:     */ import java.nio.ByteOrder;
/*   11:     */ import java.security.MessageDigest;
/*   12:     */ import java.util.Arrays;
/*   13:     */ import java.util.Comparator;
/*   14:     */ import java.util.HashMap;
/*   15:     */ import java.util.HashSet;
/*   16:     */ import java.util.Map;
/*   17:     */ import java.util.Set;
/*   18:     */ import java.util.concurrent.ConcurrentMap;
/*   19:     */ import nxt.crypto.Crypto;
/*   20:     */ import nxt.util.Convert;
/*   21:     */ import nxt.util.Logger;
/*   22:     */ import org.json.simple.JSONObject;
/*   23:     */ 
/*   24:     */ public final class Transaction
/*   25:     */   implements Comparable<Transaction>, Serializable
/*   26:     */ {
/*   27:     */   static final long serialVersionUID = 0L;
/*   28:     */   private static final byte TYPE_PAYMENT = 0;
/*   29:     */   private static final byte TYPE_MESSAGING = 1;
/*   30:     */   private static final byte TYPE_COLORED_COINS = 2;
/*   31:     */   private static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;
/*   32:     */   private static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
/*   33:     */   private static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
/*   34:     */   private static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
/*   35:     */   private static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
/*   36:     */   private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
/*   37:     */   private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
/*   38:     */   private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
/*   39:     */   private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;
/*   40:  43 */   public static final Comparator<Transaction> timestampComparator = new Comparator()
/*   41:     */   {
/*   42:     */     public int compare(Transaction paramAnonymousTransaction1, Transaction paramAnonymousTransaction2)
/*   43:     */     {
/*   44:  46 */       return paramAnonymousTransaction1.timestamp > paramAnonymousTransaction2.timestamp ? 1 : paramAnonymousTransaction1.timestamp < paramAnonymousTransaction2.timestamp ? -1 : 0;
/*   45:     */     }
/*   46:     */   };
/*   47:     */   private final short deadline;
/*   48:     */   private final byte[] senderPublicKey;
/*   49:     */   private final Long recipientId;
/*   50:     */   private final int amount;
/*   51:     */   private final int fee;
/*   52:     */   private final Long referencedTransactionId;
/*   53:     */   public int index;
/*   54:     */   public int height;
/*   55:     */   public Long blockId;
/*   56:     */   public byte[] signature;
/*   57:     */   private int timestamp;
/*   58:     */   private transient Type type;
/*   59:     */   private Attachment attachment;
/*   60:     */   private volatile transient Long id;
/*   61:     */   
/*   62:     */   public static Transaction getTransaction(byte[] paramArrayOfByte)
/*   63:     */     throws NxtException.ValidationException
/*   64:     */   {
/*   65:     */     try
/*   66:     */     {
/*   67:  53 */       ByteBuffer localByteBuffer = ByteBuffer.wrap(paramArrayOfByte);
/*   68:  54 */       localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/*   69:     */       
/*   70:  56 */       byte b1 = localByteBuffer.get();
/*   71:  57 */       byte b2 = localByteBuffer.get();
/*   72:  58 */       int i = localByteBuffer.getInt();
/*   73:  59 */       short s = localByteBuffer.getShort();
/*   74:  60 */       byte[] arrayOfByte1 = new byte[32];
/*   75:  61 */       localByteBuffer.get(arrayOfByte1);
/*   76:  62 */       Long localLong1 = Long.valueOf(localByteBuffer.getLong());
/*   77:  63 */       int j = localByteBuffer.getInt();
/*   78:  64 */       int k = localByteBuffer.getInt();
/*   79:  65 */       Long localLong2 = Convert.zeroToNull(localByteBuffer.getLong());
/*   80:  66 */       byte[] arrayOfByte2 = new byte[64];
/*   81:  67 */       localByteBuffer.get(arrayOfByte2);
/*   82:     */       
/*   83:  69 */       Type localType = findTransactionType(b1, b2);
/*   84:  70 */       Transaction localTransaction = new Transaction(localType, i, s, arrayOfByte1, localLong1, j, k, localLong2, arrayOfByte2);
/*   85:  73 */       if (!localType.loadAttachment(localTransaction, localByteBuffer)) {
/*   86:  74 */         throw new NxtException.ValidationException("Invalid transaction attachment:\n" + localTransaction.attachment.getJSON());
/*   87:     */       }
/*   88:  77 */       return localTransaction;
/*   89:     */     }
/*   90:     */     catch (RuntimeException localRuntimeException)
/*   91:     */     {
/*   92:  80 */       throw new NxtException.ValidationException(localRuntimeException.toString());
/*   93:     */     }
/*   94:     */   }
/*   95:     */   
/*   96:     */   public static Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2)
/*   97:     */     throws NxtException.ValidationException
/*   98:     */   {
/*   99:  86 */     return new Transaction(Transaction.Type.Payment.ORDINARY, paramInt1, paramShort, paramArrayOfByte, paramLong1, paramInt2, paramInt3, paramLong2, null);
/*  100:     */   }
/*  101:     */   
/*  102:     */   public static Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, Attachment paramAttachment)
/*  103:     */     throws NxtException.ValidationException
/*  104:     */   {
/*  105:  92 */     Transaction localTransaction = new Transaction(paramAttachment.getTransactionType(), paramInt1, paramShort, paramArrayOfByte, paramLong1, paramInt2, paramInt3, paramLong2, null);
/*  106:     */     
/*  107:  94 */     localTransaction.attachment = paramAttachment;
/*  108:  95 */     return localTransaction;
/*  109:     */   }
/*  110:     */   
/*  111:     */   static Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte1, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, byte[] paramArrayOfByte2)
/*  112:     */     throws NxtException.ValidationException
/*  113:     */   {
/*  114: 100 */     return new Transaction(Transaction.Type.Payment.ORDINARY, paramInt1, paramShort, paramArrayOfByte1, paramLong1, paramInt2, paramInt3, paramLong2, paramArrayOfByte2);
/*  115:     */   }
/*  116:     */   
/*  117:     */   static Transaction getTransaction(JSONObject paramJSONObject)
/*  118:     */     throws NxtException.ValidationException
/*  119:     */   {
/*  120:     */     try
/*  121:     */     {
/*  122: 107 */       byte b1 = ((Long)paramJSONObject.get("type")).byteValue();
/*  123: 108 */       byte b2 = ((Long)paramJSONObject.get("subtype")).byteValue();
/*  124: 109 */       int i = ((Long)paramJSONObject.get("timestamp")).intValue();
/*  125: 110 */       short s = ((Long)paramJSONObject.get("deadline")).shortValue();
/*  126: 111 */       byte[] arrayOfByte1 = Convert.convert((String)paramJSONObject.get("senderPublicKey"));
/*  127: 112 */       Long localLong1 = Convert.parseUnsignedLong((String)paramJSONObject.get("recipient"));
/*  128: 113 */       if (localLong1 == null) {
/*  129: 113 */         localLong1 = Long.valueOf(0L);
/*  130:     */       }
/*  131: 114 */       int j = ((Long)paramJSONObject.get("amount")).intValue();
/*  132: 115 */       int k = ((Long)paramJSONObject.get("fee")).intValue();
/*  133: 116 */       Long localLong2 = Convert.parseUnsignedLong((String)paramJSONObject.get("referencedTransaction"));
/*  134: 117 */       byte[] arrayOfByte2 = Convert.convert((String)paramJSONObject.get("signature"));
/*  135:     */       
/*  136: 119 */       Type localType = findTransactionType(b1, b2);
/*  137: 120 */       Transaction localTransaction = new Transaction(localType, i, s, arrayOfByte1, localLong1, j, k, localLong2, arrayOfByte2);
/*  138:     */       
/*  139:     */ 
/*  140: 123 */       JSONObject localJSONObject = (JSONObject)paramJSONObject.get("attachment");
/*  141: 125 */       if (!localType.loadAttachment(localTransaction, localJSONObject)) {
/*  142: 126 */         throw new NxtException.ValidationException("Invalid transaction attachment:\n" + localJSONObject.toJSONString());
/*  143:     */       }
/*  144: 129 */       return localTransaction;
/*  145:     */     }
/*  146:     */     catch (RuntimeException localRuntimeException)
/*  147:     */     {
/*  148: 132 */       throw new NxtException.ValidationException(localRuntimeException.toString());
/*  149:     */     }
/*  150:     */   }
/*  151:     */   
/*  152: 153 */   private volatile transient String stringId = null;
/*  153:     */   private volatile transient Long senderAccountId;
/*  154:     */   private volatile transient String hash;
/*  155:     */   private static final int TRANSACTION_BYTES_LENGTH = 128;
/*  156:     */   
/*  157:     */   private Transaction(Type paramType, int paramInt1, short paramShort, byte[] paramArrayOfByte1, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, byte[] paramArrayOfByte2)
/*  158:     */     throws NxtException.ValidationException
/*  159:     */   {
/*  160: 160 */     if ((paramInt1 == 0) && (Arrays.equals(paramArrayOfByte1, Genesis.CREATOR_PUBLIC_KEY)) ? (paramShort == 0) || (paramInt3 == 0) : (paramShort < 1) || (paramInt3 <= 0) || (paramInt3 > 1000000000L) || (paramInt2 < 0) || (paramInt2 > 1000000000L) || (paramType == null)) {
/*  161: 162 */       throw new NxtException.ValidationException("Invalid transaction parameters:\n type: " + paramType + ", timestamp: " + paramInt1 + ", deadline: " + paramShort + ", fee: " + paramInt3 + ", amount: " + paramInt2);
/*  162:     */     }
/*  163: 166 */     this.timestamp = paramInt1;
/*  164: 167 */     this.deadline = paramShort;
/*  165: 168 */     this.senderPublicKey = paramArrayOfByte1;
/*  166: 169 */     this.recipientId = paramLong1;
/*  167: 170 */     this.amount = paramInt2;
/*  168: 171 */     this.fee = paramInt3;
/*  169: 172 */     this.referencedTransactionId = paramLong2;
/*  170: 173 */     this.signature = paramArrayOfByte2;
/*  171: 174 */     this.type = paramType;
/*  172: 175 */     this.height = 2147483647;
/*  173:     */   }
/*  174:     */   
/*  175:     */   public short getDeadline()
/*  176:     */   {
/*  177: 180 */     return this.deadline;
/*  178:     */   }
/*  179:     */   
/*  180:     */   public byte[] getSenderPublicKey()
/*  181:     */   {
/*  182: 184 */     return this.senderPublicKey;
/*  183:     */   }
/*  184:     */   
/*  185:     */   public Long getRecipientId()
/*  186:     */   {
/*  187: 188 */     return this.recipientId;
/*  188:     */   }
/*  189:     */   
/*  190:     */   public int getAmount()
/*  191:     */   {
/*  192: 192 */     return this.amount;
/*  193:     */   }
/*  194:     */   
/*  195:     */   public int getFee()
/*  196:     */   {
/*  197: 196 */     return this.fee;
/*  198:     */   }
/*  199:     */   
/*  200:     */   public Long getReferencedTransactionId()
/*  201:     */   {
/*  202: 200 */     return this.referencedTransactionId;
/*  203:     */   }
/*  204:     */   
/*  205:     */   public int getHeight()
/*  206:     */   {
/*  207: 204 */     return this.height;
/*  208:     */   }
/*  209:     */   
/*  210:     */   public byte[] getSignature()
/*  211:     */   {
/*  212: 208 */     return this.signature;
/*  213:     */   }
/*  214:     */   
/*  215:     */   public Type getType()
/*  216:     */   {
/*  217: 212 */     return this.type;
/*  218:     */   }
/*  219:     */   
/*  220:     */   public Block getBlock()
/*  221:     */   {
/*  222: 216 */     return Blockchain.getBlock(this.blockId);
/*  223:     */   }
/*  224:     */   
/*  225:     */   void setBlockId(Long paramLong)
/*  226:     */   {
/*  227: 220 */     this.blockId = paramLong;
/*  228:     */   }
/*  229:     */   
/*  230:     */   void setHeight(int paramInt)
/*  231:     */   {
/*  232: 224 */     this.height = paramInt;
/*  233:     */   }
/*  234:     */   
/*  235:     */   public int getIndex()
/*  236:     */   {
/*  237: 228 */     return this.index;
/*  238:     */   }
/*  239:     */   
/*  240:     */   void setIndex(int paramInt)
/*  241:     */   {
/*  242: 232 */     this.index = paramInt;
/*  243:     */   }
/*  244:     */   
/*  245:     */   public int getTimestamp()
/*  246:     */   {
/*  247: 236 */     return this.timestamp;
/*  248:     */   }
/*  249:     */   
/*  250:     */   public int getExpiration()
/*  251:     */   {
/*  252: 240 */     return this.timestamp + this.deadline * 60;
/*  253:     */   }
/*  254:     */   
/*  255:     */   public Attachment getAttachment()
/*  256:     */   {
/*  257: 244 */     return this.attachment;
/*  258:     */   }
/*  259:     */   
/*  260:     */   public Long getId()
/*  261:     */   {
/*  262: 248 */     calculateIds();
/*  263: 249 */     return this.id;
/*  264:     */   }
/*  265:     */   
/*  266:     */   public String getStringId()
/*  267:     */   {
/*  268: 253 */     calculateIds();
/*  269: 254 */     return this.stringId;
/*  270:     */   }
/*  271:     */   
/*  272:     */   public Long getSenderAccountId()
/*  273:     */   {
/*  274: 258 */     calculateIds();
/*  275: 259 */     return this.senderAccountId;
/*  276:     */   }
/*  277:     */   
/*  278:     */   public int compareTo(Transaction paramTransaction)
/*  279:     */   {
/*  280: 265 */     if (this.height < paramTransaction.height) {
/*  281: 267 */       return -1;
/*  282:     */     }
/*  283: 269 */     if (this.height > paramTransaction.height) {
/*  284: 271 */       return 1;
/*  285:     */     }
/*  286: 276 */     if (this.fee * paramTransaction.getSize() > paramTransaction.fee * getSize()) {
/*  287: 278 */       return -1;
/*  288:     */     }
/*  289: 280 */     if (this.fee * paramTransaction.getSize() < paramTransaction.fee * getSize()) {
/*  290: 282 */       return 1;
/*  291:     */     }
/*  292: 286 */     if (this.timestamp < paramTransaction.timestamp) {
/*  293: 288 */       return -1;
/*  294:     */     }
/*  295: 290 */     if (this.timestamp > paramTransaction.timestamp) {
/*  296: 292 */       return 1;
/*  297:     */     }
/*  298: 296 */     if (this.index < paramTransaction.index) {
/*  299: 298 */       return -1;
/*  300:     */     }
/*  301: 300 */     if (this.index > paramTransaction.index) {
/*  302: 302 */       return 1;
/*  303:     */     }
/*  304: 306 */     return 0;
/*  305:     */   }
/*  306:     */   
/*  307:     */   public byte[] getBytes()
/*  308:     */   {
/*  309: 320 */     ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
/*  310: 321 */     localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/*  311: 322 */     localByteBuffer.put(this.type.getType());
/*  312: 323 */     localByteBuffer.put(this.type.getSubtype());
/*  313: 324 */     localByteBuffer.putInt(this.timestamp);
/*  314: 325 */     localByteBuffer.putShort(this.deadline);
/*  315: 326 */     localByteBuffer.put(this.senderPublicKey);
/*  316: 327 */     localByteBuffer.putLong(Convert.nullToZero(this.recipientId));
/*  317: 328 */     localByteBuffer.putInt(this.amount);
/*  318: 329 */     localByteBuffer.putInt(this.fee);
/*  319: 330 */     localByteBuffer.putLong(Convert.nullToZero(this.referencedTransactionId));
/*  320: 331 */     localByteBuffer.put(this.signature);
/*  321: 332 */     if (this.attachment != null) {
/*  322: 333 */       localByteBuffer.put(this.attachment.getBytes());
/*  323:     */     }
/*  324: 335 */     return localByteBuffer.array();
/*  325:     */   }
/*  326:     */   
/*  327:     */   public JSONObject getJSONObject()
/*  328:     */   {
/*  329: 341 */     JSONObject localJSONObject = new JSONObject();
/*  330:     */     
/*  331: 343 */     localJSONObject.put("type", Byte.valueOf(this.type.getType()));
/*  332: 344 */     localJSONObject.put("subtype", Byte.valueOf(this.type.getSubtype()));
/*  333: 345 */     localJSONObject.put("timestamp", Integer.valueOf(this.timestamp));
/*  334: 346 */     localJSONObject.put("deadline", Short.valueOf(this.deadline));
/*  335: 347 */     localJSONObject.put("senderPublicKey", Convert.convert(this.senderPublicKey));
/*  336: 348 */     localJSONObject.put("recipient", Convert.convert(this.recipientId));
/*  337: 349 */     localJSONObject.put("amount", Integer.valueOf(this.amount));
/*  338: 350 */     localJSONObject.put("fee", Integer.valueOf(this.fee));
/*  339: 351 */     localJSONObject.put("referencedTransaction", Convert.convert(this.referencedTransactionId));
/*  340: 352 */     localJSONObject.put("signature", Convert.convert(this.signature));
/*  341: 353 */     if (this.attachment != null) {
/*  342: 354 */       localJSONObject.put("attachment", this.attachment.getJSON());
/*  343:     */     }
/*  344: 357 */     return localJSONObject;
/*  345:     */   }
/*  346:     */   
/*  347:     */   public void sign(String paramString)
/*  348:     */   {
/*  349: 362 */     if (this.signature != null) {
/*  350: 363 */       throw new IllegalStateException("Transaction already signed");
/*  351:     */     }
/*  352: 366 */     this.signature = new byte[64];
/*  353: 367 */     this.signature = Crypto.sign(getBytes(), paramString);
/*  354:     */     try
/*  355:     */     {
/*  356: 371 */       while (!verify())
/*  357:     */       {
/*  358: 373 */         this.timestamp += 1;
/*  359:     */         
/*  360: 375 */         this.signature = new byte[64];
/*  361: 376 */         this.signature = Crypto.sign(getBytes(), paramString);
/*  362:     */       }
/*  363:     */     }
/*  364:     */     catch (RuntimeException localRuntimeException)
/*  365:     */     {
/*  366: 382 */       Logger.logMessage("Error signing transaction", localRuntimeException);
/*  367:     */     }
/*  368:     */   }
/*  369:     */   
/*  370:     */   public boolean equals(Object paramObject)
/*  371:     */   {
/*  372: 390 */     return ((paramObject instanceof Transaction)) && (getId().equals(((Transaction)paramObject).getId()));
/*  373:     */   }
/*  374:     */   
/*  375:     */   public int hashCode()
/*  376:     */   {
/*  377: 395 */     return getId().hashCode();
/*  378:     */   }
/*  379:     */   
/*  380:     */   boolean verify()
/*  381:     */   {
/*  382: 399 */     Account localAccount = Account.getAccount(getSenderAccountId());
/*  383: 400 */     if (localAccount == null) {
/*  384: 401 */       return false;
/*  385:     */     }
/*  386: 403 */     byte[] arrayOfByte = getBytes();
/*  387: 404 */     for (int i = 64; i < 128; i++) {
/*  388: 405 */       arrayOfByte[i] = 0;
/*  389:     */     }
/*  390: 407 */     return (Crypto.verify(this.signature, arrayOfByte, this.senderPublicKey)) && (localAccount.setOrVerify(this.senderPublicKey));
/*  391:     */   }
/*  392:     */   
/*  393:     */   boolean isDoubleSpending()
/*  394:     */   {
/*  395: 412 */     Account localAccount = Account.getAccount(getSenderAccountId());
/*  396: 413 */     if (localAccount == null) {
/*  397: 414 */       return true;
/*  398:     */     }
/*  399: 416 */     synchronized (localAccount)
/*  400:     */     {
/*  401: 417 */       return this.type.isDoubleSpending(this, localAccount, this.amount + this.fee);
/*  402:     */     }
/*  403:     */   }
/*  404:     */   
/*  405:     */   void apply()
/*  406:     */   {
/*  407: 422 */     Account localAccount1 = Account.getAccount(getSenderAccountId());
/*  408: 423 */     if (!localAccount1.setOrVerify(this.senderPublicKey)) {
/*  409: 424 */       throw new RuntimeException("sender public key mismatch");
/*  410:     */     }
/*  411: 427 */     Blockchain.transactionHashes.put(getHash(), this);
/*  412: 428 */     Account localAccount2 = Account.getAccount(this.recipientId);
/*  413: 429 */     if (localAccount2 == null) {
/*  414: 430 */       localAccount2 = Account.addOrGetAccount(this.recipientId);
/*  415:     */     }
/*  416: 432 */     localAccount1.addToBalanceAndUnconfirmedBalance(-(this.amount + this.fee) * 100L);
/*  417: 433 */     this.type.apply(this, localAccount1, localAccount2);
/*  418:     */   }
/*  419:     */   
/*  420:     */   void undo()
/*  421:     */     throws Transaction.UndoNotSupportedException
/*  422:     */   {
/*  423: 438 */     Account localAccount1 = Account.getAccount(this.senderAccountId);
/*  424: 439 */     localAccount1.addToBalance((this.amount + this.fee) * 100L);
/*  425: 440 */     Account localAccount2 = Account.getAccount(this.recipientId);
/*  426: 441 */     this.type.undo(this, localAccount1, localAccount2);
/*  427:     */   }
/*  428:     */   
/*  429:     */   void updateTotals(Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1)
/*  430:     */   {
/*  431: 445 */     Long localLong1 = getSenderAccountId();
/*  432: 446 */     Long localLong2 = (Long)paramMap.get(localLong1);
/*  433: 447 */     if (localLong2 == null) {
/*  434: 448 */       localLong2 = Long.valueOf(0L);
/*  435:     */     }
/*  436: 450 */     paramMap.put(localLong1, Long.valueOf(localLong2.longValue() + (this.amount + this.fee) * 100L));
/*  437: 451 */     this.type.updateTotals(this, paramMap, paramMap1, localLong2);
/*  438:     */   }
/*  439:     */   
/*  440:     */   boolean isDuplicate(Map<Type, Set<String>> paramMap)
/*  441:     */   {
/*  442: 455 */     return this.type.isDuplicate(this, paramMap);
/*  443:     */   }
/*  444:     */   
/*  445:     */   int getSize()
/*  446:     */   {
/*  447: 461 */     return 128 + (this.attachment == null ? 0 : this.attachment.getSize());
/*  448:     */   }
/*  449:     */   
/*  450:     */   String getHash()
/*  451:     */   {
/*  452: 465 */     if (this.hash == null)
/*  453:     */     {
/*  454: 466 */       byte[] arrayOfByte = getBytes();
/*  455: 467 */       for (int i = 64; i < 128; i++) {
/*  456: 468 */         arrayOfByte[i] = 0;
/*  457:     */       }
/*  458: 470 */       this.hash = Convert.convert(Crypto.sha256().digest(arrayOfByte));
/*  459:     */     }
/*  460: 472 */     return this.hash;
/*  461:     */   }
/*  462:     */   
/*  463:     */   private void calculateIds()
/*  464:     */   {
/*  465: 476 */     if (this.stringId != null) {
/*  466: 477 */       return;
/*  467:     */     }
/*  468: 479 */     byte[] arrayOfByte = Crypto.sha256().digest(getBytes());
/*  469: 480 */     BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
/*  470: 481 */     this.id = Long.valueOf(localBigInteger.longValue());
/*  471: 482 */     this.senderAccountId = Account.getId(this.senderPublicKey);
/*  472: 483 */     this.stringId = localBigInteger.toString();
/*  473:     */   }
/*  474:     */   
/*  475:     */   private void writeObject(ObjectOutputStream paramObjectOutputStream)
/*  476:     */     throws IOException
/*  477:     */   {
/*  478: 487 */     paramObjectOutputStream.defaultWriteObject();
/*  479: 488 */     paramObjectOutputStream.write(this.type.getType());
/*  480: 489 */     paramObjectOutputStream.write(this.type.getSubtype());
/*  481:     */   }
/*  482:     */   
/*  483:     */   private void readObject(ObjectInputStream paramObjectInputStream)
/*  484:     */     throws IOException, ClassNotFoundException
/*  485:     */   {
/*  486: 493 */     paramObjectInputStream.defaultReadObject();
/*  487: 494 */     this.type = findTransactionType(paramObjectInputStream.readByte(), paramObjectInputStream.readByte());
/*  488:     */   }
/*  489:     */   
/*  490:     */   public static Type findTransactionType(byte paramByte1, byte paramByte2)
/*  491:     */   {
/*  492: 498 */     switch (paramByte1)
/*  493:     */     {
/*  494:     */     case 0: 
/*  495: 500 */       switch (paramByte2)
/*  496:     */       {
/*  497:     */       case 0: 
/*  498: 502 */         return Transaction.Type.Payment.ORDINARY;
/*  499:     */       }
/*  500: 504 */       return null;
/*  501:     */     case 1: 
/*  502: 507 */       switch (paramByte2)
/*  503:     */       {
/*  504:     */       case 0: 
/*  505: 509 */         return Transaction.Type.Messaging.ARBITRARY_MESSAGE;
/*  506:     */       case 1: 
/*  507: 511 */         return Transaction.Type.Messaging.ALIAS_ASSIGNMENT;
/*  508:     */       }
/*  509: 513 */       return null;
/*  510:     */     case 2: 
/*  511: 516 */       switch (paramByte2)
/*  512:     */       {
/*  513:     */       case 0: 
/*  514: 518 */         return Transaction.Type.ColoredCoins.ASSET_ISSUANCE;
/*  515:     */       case 1: 
/*  516: 520 */         return Transaction.Type.ColoredCoins.ASSET_TRANSFER;
/*  517:     */       case 2: 
/*  518: 522 */         return Transaction.Type.ColoredCoins.ASK_ORDER_PLACEMENT;
/*  519:     */       case 3: 
/*  520: 524 */         return Transaction.Type.ColoredCoins.BID_ORDER_PLACEMENT;
/*  521:     */       case 4: 
/*  522: 526 */         return Transaction.Type.ColoredCoins.ASK_ORDER_CANCELLATION;
/*  523:     */       case 5: 
/*  524: 528 */         return Transaction.Type.ColoredCoins.BID_ORDER_CANCELLATION;
/*  525:     */       }
/*  526: 530 */       return null;
/*  527:     */     }
/*  528: 533 */     return null;
/*  529:     */   }
/*  530:     */   
/*  531:     */   public static abstract class Type
/*  532:     */   {
/*  533:     */     public abstract byte getType();
/*  534:     */     
/*  535:     */     public abstract byte getSubtype();
/*  536:     */     
/*  537:     */     abstract boolean loadAttachment(Transaction paramTransaction, ByteBuffer paramByteBuffer)
/*  538:     */       throws NxtException.ValidationException;
/*  539:     */     
/*  540:     */     abstract boolean loadAttachment(Transaction paramTransaction, JSONObject paramJSONObject)
/*  541:     */       throws NxtException.ValidationException;
/*  542:     */     
/*  543:     */     final boolean isDoubleSpending(Transaction paramTransaction, Account paramAccount, int paramInt)
/*  544:     */     {
/*  545: 549 */       if (paramAccount.getUnconfirmedBalance() < paramInt * 100L) {
/*  546: 550 */         return true;
/*  547:     */       }
/*  548: 552 */       paramAccount.addToUnconfirmedBalance(-paramInt * 100L);
/*  549: 553 */       return checkDoubleSpending(paramTransaction, paramAccount, paramInt);
/*  550:     */     }
/*  551:     */     
/*  552:     */     abstract boolean checkDoubleSpending(Transaction paramTransaction, Account paramAccount, int paramInt);
/*  553:     */     
/*  554:     */     abstract void apply(Transaction paramTransaction, Account paramAccount1, Account paramAccount2);
/*  555:     */     
/*  556:     */     abstract void undo(Transaction paramTransaction, Account paramAccount1, Account paramAccount2)
/*  557:     */       throws Transaction.UndoNotSupportedException;
/*  558:     */     
/*  559:     */     abstract void updateTotals(Transaction paramTransaction, Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1, Long paramLong);
/*  560:     */     
/*  561:     */     boolean isDuplicate(Transaction paramTransaction, Map<Type, Set<String>> paramMap)
/*  562:     */     {
/*  563: 566 */       return false;
/*  564:     */     }
/*  565:     */     
/*  566:     */     public static abstract class Payment
/*  567:     */       extends Transaction.Type
/*  568:     */     {
/*  569:     */       public final byte getType()
/*  570:     */       {
/*  571: 573 */         return 0;
/*  572:     */       }
/*  573:     */       
/*  574: 576 */       public static final Transaction.Type ORDINARY = new Payment()
/*  575:     */       {
/*  576:     */         public final byte getSubtype()
/*  577:     */         {
/*  578: 580 */           return 0;
/*  579:     */         }
/*  580:     */         
/*  581:     */         final boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/*  582:     */         {
/*  583: 585 */           return validateAttachment(paramAnonymousTransaction);
/*  584:     */         }
/*  585:     */         
/*  586:     */         final boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/*  587:     */         {
/*  588: 590 */           return validateAttachment(paramAnonymousTransaction);
/*  589:     */         }
/*  590:     */         
/*  591:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/*  592:     */         {
/*  593: 595 */           paramAnonymousAccount2.addToBalanceAndUnconfirmedBalance(paramAnonymousTransaction.amount * 100L);
/*  594:     */         }
/*  595:     */         
/*  596:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/*  597:     */         {
/*  598: 600 */           paramAnonymousAccount2.addToBalanceAndUnconfirmedBalance(-paramAnonymousTransaction.amount * 100L);
/*  599:     */         }
/*  600:     */         
/*  601:     */         void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong) {}
/*  602:     */         
/*  603:     */         boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
/*  604:     */         {
/*  605: 609 */           return false;
/*  606:     */         }
/*  607:     */         
/*  608:     */         private boolean validateAttachment(Transaction paramAnonymousTransaction)
/*  609:     */         {
/*  610: 613 */           return (paramAnonymousTransaction.amount > 0) && (paramAnonymousTransaction.amount < 1000000000L);
/*  611:     */         }
/*  612:     */       };
/*  613:     */     }
/*  614:     */     
/*  615:     */     public static abstract class Messaging
/*  616:     */       extends Transaction.Type
/*  617:     */     {
/*  618:     */       public final byte getType()
/*  619:     */       {
/*  620: 623 */         return 1;
/*  621:     */       }
/*  622:     */       
/*  623:     */       boolean checkDoubleSpending(Transaction paramTransaction, Account paramAccount, int paramInt)
/*  624:     */       {
/*  625: 628 */         return false;
/*  626:     */       }
/*  627:     */       
/*  628: 635 */       public static final Transaction.Type ARBITRARY_MESSAGE = new Messaging()
/*  629:     */       {
/*  630:     */         public final byte getSubtype()
/*  631:     */         {
/*  632: 639 */           return 0;
/*  633:     */         }
/*  634:     */         
/*  635:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/*  636:     */           throws NxtException.ValidationException
/*  637:     */         {
/*  638: 644 */           int i = paramAnonymousByteBuffer.getInt();
/*  639: 645 */           if (i <= 1000)
/*  640:     */           {
/*  641: 646 */             byte[] arrayOfByte = new byte[i];
/*  642: 647 */             paramAnonymousByteBuffer.get(arrayOfByte);
/*  643: 648 */             paramAnonymousTransaction.attachment = new Attachment.MessagingArbitraryMessage(arrayOfByte);
/*  644: 649 */             return validateAttachment(paramAnonymousTransaction);
/*  645:     */           }
/*  646: 651 */           return false;
/*  647:     */         }
/*  648:     */         
/*  649:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/*  650:     */           throws NxtException.ValidationException
/*  651:     */         {
/*  652: 656 */           String str = (String)paramAnonymousJSONObject.get("message");
/*  653: 657 */           paramAnonymousTransaction.attachment = new Attachment.MessagingArbitraryMessage(Convert.convert(str));
/*  654: 658 */           return validateAttachment(paramAnonymousTransaction);
/*  655:     */         }
/*  656:     */         
/*  657:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2) {}
/*  658:     */         
/*  659:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2) {}
/*  660:     */         
/*  661:     */         private boolean validateAttachment(Transaction paramAnonymousTransaction)
/*  662:     */           throws NxtException.ValidationException
/*  663:     */         {
/*  664: 668 */           if (Blockchain.getLastBlock().getHeight() < 40000) {
/*  665: 669 */             throw new Transaction.NotYetEnabledException("Arbitrary messages not yet enabled at height " + Blockchain.getLastBlock().getHeight());
/*  666:     */           }
/*  667:     */           try
/*  668:     */           {
/*  669: 672 */             Attachment.MessagingArbitraryMessage localMessagingArbitraryMessage = (Attachment.MessagingArbitraryMessage)paramAnonymousTransaction.attachment;
/*  670: 673 */             return (paramAnonymousTransaction.amount == 0) && (localMessagingArbitraryMessage.getMessage().length <= 1000);
/*  671:     */           }
/*  672:     */           catch (RuntimeException localRuntimeException)
/*  673:     */           {
/*  674: 675 */             Logger.logDebugMessage("Error validating arbitrary message", localRuntimeException);
/*  675:     */           }
/*  676: 676 */           return false;
/*  677:     */         }
/*  678:     */       };
/*  679: 682 */       public static final Transaction.Type ALIAS_ASSIGNMENT = new Messaging()
/*  680:     */       {
/*  681:     */         public final byte getSubtype()
/*  682:     */         {
/*  683: 686 */           return 1;
/*  684:     */         }
/*  685:     */         
/*  686:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/*  687:     */           throws NxtException.ValidationException
/*  688:     */         {
/*  689: 691 */           int i = paramAnonymousByteBuffer.get();
/*  690: 692 */           if (i > 300) {
/*  691: 693 */             throw new NxtException.ValidationException("Max alias length exceeded");
/*  692:     */           }
/*  693: 695 */           byte[] arrayOfByte1 = new byte[i];
/*  694: 696 */           paramAnonymousByteBuffer.get(arrayOfByte1);
/*  695: 697 */           int j = paramAnonymousByteBuffer.getShort();
/*  696: 698 */           if (j > 3000) {
/*  697: 699 */             throw new NxtException.ValidationException("Max alias URI length exceeded");
/*  698:     */           }
/*  699: 701 */           byte[] arrayOfByte2 = new byte[j];
/*  700: 702 */           paramAnonymousByteBuffer.get(arrayOfByte2);
/*  701:     */           try
/*  702:     */           {
/*  703: 704 */             paramAnonymousTransaction.attachment = new Attachment.MessagingAliasAssignment(new String(arrayOfByte1, "UTF-8"), new String(arrayOfByte2, "UTF-8"));
/*  704:     */             
/*  705: 706 */             return validateAttachment(paramAnonymousTransaction);
/*  706:     */           }
/*  707:     */           catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
/*  708:     */           {
/*  709: 708 */             Logger.logDebugMessage("Error parsing alias assignment", localRuntimeException);
/*  710:     */           }
/*  711: 710 */           return false;
/*  712:     */         }
/*  713:     */         
/*  714:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/*  715:     */           throws NxtException.ValidationException
/*  716:     */         {
/*  717: 715 */           String str1 = (String)paramAnonymousJSONObject.get("alias");
/*  718: 716 */           String str2 = (String)paramAnonymousJSONObject.get("uri");
/*  719: 717 */           paramAnonymousTransaction.attachment = new Attachment.MessagingAliasAssignment(str1, str2);
/*  720: 718 */           return validateAttachment(paramAnonymousTransaction);
/*  721:     */         }
/*  722:     */         
/*  723:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/*  724:     */         {
/*  725: 723 */           Attachment.MessagingAliasAssignment localMessagingAliasAssignment = (Attachment.MessagingAliasAssignment)paramAnonymousTransaction.attachment;
/*  726: 724 */           Block localBlock = paramAnonymousTransaction.getBlock();
/*  727: 725 */           Alias.addOrUpdateAlias(paramAnonymousAccount1, paramAnonymousTransaction.getId(), localMessagingAliasAssignment.getAliasName(), localMessagingAliasAssignment.getAliasURI(), localBlock.getTimestamp());
/*  728:     */         }
/*  729:     */         
/*  730:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/*  731:     */           throws Transaction.UndoNotSupportedException
/*  732:     */         {
/*  733: 731 */           throw new Transaction.UndoNotSupportedException(paramAnonymousTransaction, "Reversal of alias assignment not supported");
/*  734:     */         }
/*  735:     */         
/*  736:     */         boolean isDuplicate(Transaction paramAnonymousTransaction, Map<Transaction.Type, Set<String>> paramAnonymousMap)
/*  737:     */         {
/*  738: 736 */           Object localObject = (Set)paramAnonymousMap.get(this);
/*  739: 737 */           if (localObject == null)
/*  740:     */           {
/*  741: 738 */             localObject = new HashSet();
/*  742: 739 */             paramAnonymousMap.put(this, localObject);
/*  743:     */           }
/*  744: 741 */           Attachment.MessagingAliasAssignment localMessagingAliasAssignment = (Attachment.MessagingAliasAssignment)paramAnonymousTransaction.attachment;
/*  745: 742 */           return !((Set)localObject).add(localMessagingAliasAssignment.getAliasName().toLowerCase());
/*  746:     */         }
/*  747:     */         
/*  748:     */         private boolean validateAttachment(Transaction paramAnonymousTransaction)
/*  749:     */           throws NxtException.ValidationException
/*  750:     */         {
/*  751: 746 */           if (Blockchain.getLastBlock().getHeight() < 22000) {
/*  752: 747 */             throw new Transaction.NotYetEnabledException("Aliases not yet enabled at height " + Blockchain.getLastBlock().getHeight());
/*  753:     */           }
/*  754:     */           try
/*  755:     */           {
/*  756: 750 */             Attachment.MessagingAliasAssignment localMessagingAliasAssignment = (Attachment.MessagingAliasAssignment)paramAnonymousTransaction.attachment;
/*  757: 751 */             if ((!Genesis.CREATOR_ID.equals(paramAnonymousTransaction.recipientId)) || (paramAnonymousTransaction.amount != 0) || (localMessagingAliasAssignment.getAliasName().length() == 0) || (localMessagingAliasAssignment.getAliasName().length() > 100) || (localMessagingAliasAssignment.getAliasURI().length() > 1000)) {
/*  758: 753 */               return false;
/*  759:     */             }
/*  760: 755 */             String str = localMessagingAliasAssignment.getAliasName().toLowerCase();
/*  761: 756 */             for (int i = 0; i < str.length(); i++) {
/*  762: 757 */               if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(str.charAt(i)) < 0) {
/*  763: 758 */                 return false;
/*  764:     */               }
/*  765:     */             }
/*  766: 761 */             Alias localAlias = Alias.getAlias(str);
/*  767: 762 */             return (localAlias == null) || (Arrays.equals(localAlias.getAccount().getPublicKey(), paramAnonymousTransaction.senderPublicKey));
/*  768:     */           }
/*  769:     */           catch (RuntimeException localRuntimeException)
/*  770:     */           {
/*  771: 765 */             Logger.logDebugMessage("Error in alias assignment validation", localRuntimeException);
/*  772:     */           }
/*  773: 766 */           return false;
/*  774:     */         }
/*  775:     */       };
/*  776:     */       
/*  777:     */       void updateTotals(Transaction paramTransaction, Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1, Long paramLong) {}
/*  778:     */     }
/*  779:     */     
/*  780:     */     public static abstract class ColoredCoins
/*  781:     */       extends Transaction.Type
/*  782:     */     {
/*  783:     */       public final byte getType()
/*  784:     */       {
/*  785: 777 */         return 2;
/*  786:     */       }
/*  787:     */       
/*  788: 780 */       public static final Transaction.Type ASSET_ISSUANCE = new ColoredCoins()
/*  789:     */       {
/*  790:     */         public final byte getSubtype()
/*  791:     */         {
/*  792: 784 */           return 0;
/*  793:     */         }
/*  794:     */         
/*  795:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/*  796:     */           throws NxtException.ValidationException
/*  797:     */         {
/*  798: 789 */           int i = paramAnonymousByteBuffer.get();
/*  799: 790 */           if (i > 30) {
/*  800: 791 */             throw new NxtException.ValidationException("Max asset name length exceeded");
/*  801:     */           }
/*  802: 793 */           byte[] arrayOfByte1 = new byte[i];
/*  803: 794 */           paramAnonymousByteBuffer.get(arrayOfByte1);
/*  804: 795 */           int j = paramAnonymousByteBuffer.getShort();
/*  805: 796 */           if (j > 300) {
/*  806: 797 */             throw new NxtException.ValidationException("Max asset description length exceeded");
/*  807:     */           }
/*  808: 799 */           byte[] arrayOfByte2 = new byte[j];
/*  809: 800 */           paramAnonymousByteBuffer.get(arrayOfByte2);
/*  810: 801 */           int k = paramAnonymousByteBuffer.getInt();
/*  811:     */           try
/*  812:     */           {
/*  813: 803 */             paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAssetIssuance(new String(arrayOfByte1, "UTF-8").intern(), new String(arrayOfByte2, "UTF-8").intern(), k);
/*  814:     */             
/*  815: 805 */             return validateAttachment(paramAnonymousTransaction);
/*  816:     */           }
/*  817:     */           catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
/*  818:     */           {
/*  819: 807 */             Logger.logDebugMessage("Error in asset issuance", localRuntimeException);
/*  820:     */           }
/*  821: 809 */           return false;
/*  822:     */         }
/*  823:     */         
/*  824:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/*  825:     */         {
/*  826: 814 */           String str1 = (String)paramAnonymousJSONObject.get("name");
/*  827: 815 */           String str2 = (String)paramAnonymousJSONObject.get("description");
/*  828: 816 */           int i = ((Long)paramAnonymousJSONObject.get("quantity")).intValue();
/*  829: 817 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAssetIssuance(str1.trim(), str2.trim(), i);
/*  830: 818 */           return validateAttachment(paramAnonymousTransaction);
/*  831:     */         }
/*  832:     */         
/*  833:     */         boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
/*  834:     */         {
/*  835: 823 */           return false;
/*  836:     */         }
/*  837:     */         
/*  838:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/*  839:     */         {
/*  840: 828 */           Attachment.ColoredCoinsAssetIssuance localColoredCoinsAssetIssuance = (Attachment.ColoredCoinsAssetIssuance)paramAnonymousTransaction.attachment;
/*  841: 829 */           Long localLong = paramAnonymousTransaction.getId();
/*  842: 830 */           Asset.addAsset(localLong, paramAnonymousTransaction.getSenderAccountId(), localColoredCoinsAssetIssuance.getName(), localColoredCoinsAssetIssuance.getDescription(), localColoredCoinsAssetIssuance.getQuantity());
/*  843: 831 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localLong, localColoredCoinsAssetIssuance.getQuantity());
/*  844:     */         }
/*  845:     */         
/*  846:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/*  847:     */         {
/*  848: 836 */           Attachment.ColoredCoinsAssetIssuance localColoredCoinsAssetIssuance = (Attachment.ColoredCoinsAssetIssuance)paramAnonymousTransaction.attachment;
/*  849: 837 */           Long localLong = paramAnonymousTransaction.getId();
/*  850: 838 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localLong, -localColoredCoinsAssetIssuance.getQuantity());
/*  851: 839 */           Asset.removeAsset(localLong);
/*  852:     */         }
/*  853:     */         
/*  854:     */         void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong) {}
/*  855:     */         
/*  856:     */         private boolean validateAttachment(Transaction paramAnonymousTransaction)
/*  857:     */         {
/*  858:     */           try
/*  859:     */           {
/*  860: 848 */             Attachment.ColoredCoinsAssetIssuance localColoredCoinsAssetIssuance = (Attachment.ColoredCoinsAssetIssuance)paramAnonymousTransaction.attachment;
/*  861: 849 */             if ((!Genesis.CREATOR_ID.equals(paramAnonymousTransaction.recipientId)) || (paramAnonymousTransaction.amount != 0) || (paramAnonymousTransaction.fee < 1000) || (localColoredCoinsAssetIssuance.getName().length() < 3) || (localColoredCoinsAssetIssuance.getName().length() > 10) || (localColoredCoinsAssetIssuance.getDescription().length() > 1000) || (localColoredCoinsAssetIssuance.getQuantity() <= 0) || (localColoredCoinsAssetIssuance.getQuantity() > 1000000000L)) {
/*  862: 852 */               return false;
/*  863:     */             }
/*  864: 854 */             String str = localColoredCoinsAssetIssuance.getName().toLowerCase();
/*  865: 855 */             for (int i = 0; i < str.length(); i++) {
/*  866: 856 */               if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(str.charAt(i)) < 0) {
/*  867: 857 */                 return false;
/*  868:     */               }
/*  869:     */             }
/*  870: 860 */             return Asset.getAsset(str) == null;
/*  871:     */           }
/*  872:     */           catch (RuntimeException localRuntimeException)
/*  873:     */           {
/*  874: 863 */             Logger.logDebugMessage("Error validating colored coins asset issuance", localRuntimeException);
/*  875:     */           }
/*  876: 864 */           return false;
/*  877:     */         }
/*  878:     */       };
/*  879: 870 */       public static final Transaction.Type ASSET_TRANSFER = new ColoredCoins()
/*  880:     */       {
/*  881:     */         public final byte getSubtype()
/*  882:     */         {
/*  883: 874 */           return 1;
/*  884:     */         }
/*  885:     */         
/*  886:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/*  887:     */         {
/*  888: 879 */           Long localLong = Convert.zeroToNull(paramAnonymousByteBuffer.getLong());
/*  889: 880 */           int i = paramAnonymousByteBuffer.getInt();
/*  890: 881 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAssetTransfer(localLong, i);
/*  891: 882 */           return validateAttachment(paramAnonymousTransaction);
/*  892:     */         }
/*  893:     */         
/*  894:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/*  895:     */         {
/*  896: 887 */           Long localLong = Convert.parseUnsignedLong((String)paramAnonymousJSONObject.get("asset"));
/*  897: 888 */           int i = ((Long)paramAnonymousJSONObject.get("quantity")).intValue();
/*  898: 889 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAssetTransfer(localLong, i);
/*  899: 890 */           return validateAttachment(paramAnonymousTransaction);
/*  900:     */         }
/*  901:     */         
/*  902:     */         boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
/*  903:     */         {
/*  904: 895 */           Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
/*  905: 896 */           Integer localInteger = paramAnonymousAccount.getUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId());
/*  906: 897 */           if ((localInteger == null) || (localInteger.intValue() < localColoredCoinsAssetTransfer.getQuantity()))
/*  907:     */           {
/*  908: 898 */             paramAnonymousAccount.addToUnconfirmedBalance(paramAnonymousInt * 100L);
/*  909: 899 */             return true;
/*  910:     */           }
/*  911: 901 */           paramAnonymousAccount.addToUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), -localColoredCoinsAssetTransfer.getQuantity());
/*  912: 902 */           return false;
/*  913:     */         }
/*  914:     */         
/*  915:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/*  916:     */         {
/*  917: 908 */           Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
/*  918: 909 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), -localColoredCoinsAssetTransfer.getQuantity());
/*  919: 910 */           paramAnonymousAccount2.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), localColoredCoinsAssetTransfer.getQuantity());
/*  920:     */         }
/*  921:     */         
/*  922:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/*  923:     */         {
/*  924: 915 */           Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
/*  925: 916 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), localColoredCoinsAssetTransfer.getQuantity());
/*  926: 917 */           paramAnonymousAccount2.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), -localColoredCoinsAssetTransfer.getQuantity());
/*  927:     */         }
/*  928:     */         
/*  929:     */         void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong)
/*  930:     */         {
/*  931: 923 */           Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
/*  932: 924 */           Object localObject = (Map)paramAnonymousMap1.get(paramAnonymousTransaction.getSenderAccountId());
/*  933: 925 */           if (localObject == null)
/*  934:     */           {
/*  935: 926 */             localObject = new HashMap();
/*  936: 927 */             paramAnonymousMap1.put(paramAnonymousTransaction.getSenderAccountId(), localObject);
/*  937:     */           }
/*  938: 929 */           Long localLong = (Long)((Map)localObject).get(localColoredCoinsAssetTransfer.getAssetId());
/*  939: 930 */           if (localLong == null) {
/*  940: 931 */             localLong = Long.valueOf(0L);
/*  941:     */           }
/*  942: 933 */           ((Map)localObject).put(localColoredCoinsAssetTransfer.getAssetId(), Long.valueOf(localLong.longValue() + localColoredCoinsAssetTransfer.getQuantity()));
/*  943:     */         }
/*  944:     */         
/*  945:     */         private boolean validateAttachment(Transaction paramAnonymousTransaction)
/*  946:     */         {
/*  947: 937 */           Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
/*  948: 938 */           return (paramAnonymousTransaction.amount == 0) && (localColoredCoinsAssetTransfer.getQuantity() > 0) && (localColoredCoinsAssetTransfer.getQuantity() <= 1000000000L);
/*  949:     */         }
/*  950:     */       };
/*  951:     */       
/*  952:     */       static abstract class ColoredCoinsOrderPlacement
/*  953:     */         extends Transaction.Type.ColoredCoins
/*  954:     */       {
/*  955:     */         abstract Attachment.ColoredCoinsOrderPlacement makeAttachment(Long paramLong, int paramInt, long paramLong1);
/*  956:     */         
/*  957:     */         final boolean loadAttachment(Transaction paramTransaction, ByteBuffer paramByteBuffer)
/*  958:     */         {
/*  959: 949 */           Long localLong = Convert.zeroToNull(paramByteBuffer.getLong());
/*  960: 950 */           int i = paramByteBuffer.getInt();
/*  961: 951 */           long l = paramByteBuffer.getLong();
/*  962: 952 */           paramTransaction.attachment = makeAttachment(localLong, i, l);
/*  963: 953 */           return validateAttachment(paramTransaction);
/*  964:     */         }
/*  965:     */         
/*  966:     */         final boolean loadAttachment(Transaction paramTransaction, JSONObject paramJSONObject)
/*  967:     */         {
/*  968: 958 */           Long localLong = Convert.parseUnsignedLong((String)paramJSONObject.get("asset"));
/*  969: 959 */           int i = ((Long)paramJSONObject.get("quantity")).intValue();
/*  970: 960 */           long l = ((Long)paramJSONObject.get("price")).longValue();
/*  971: 961 */           paramTransaction.attachment = makeAttachment(localLong, i, l);
/*  972: 962 */           return validateAttachment(paramTransaction);
/*  973:     */         }
/*  974:     */         
/*  975:     */         private boolean validateAttachment(Transaction paramTransaction)
/*  976:     */         {
/*  977: 966 */           Attachment.ColoredCoinsOrderPlacement localColoredCoinsOrderPlacement = (Attachment.ColoredCoinsOrderPlacement)paramTransaction.attachment;
/*  978: 967 */           return (Genesis.CREATOR_ID.equals(paramTransaction.recipientId)) && (paramTransaction.amount == 0) && (localColoredCoinsOrderPlacement.getQuantity() > 0) && (localColoredCoinsOrderPlacement.getQuantity() <= 1000000000L) && (localColoredCoinsOrderPlacement.getPrice() > 0L) && (localColoredCoinsOrderPlacement.getPrice() <= 100000000000L);
/*  979:     */         }
/*  980:     */       }
/*  981:     */       
/*  982: 974 */       public static final Transaction.Type ASK_ORDER_PLACEMENT = new ColoredCoinsOrderPlacement()
/*  983:     */       {
/*  984:     */         public final byte getSubtype()
/*  985:     */         {
/*  986: 978 */           return 2;
/*  987:     */         }
/*  988:     */         
/*  989:     */         final Attachment.ColoredCoinsOrderPlacement makeAttachment(Long paramAnonymousLong, int paramAnonymousInt, long paramAnonymousLong1)
/*  990:     */         {
/*  991: 982 */           return new Attachment.ColoredCoinsAskOrderPlacement(paramAnonymousLong, paramAnonymousInt, paramAnonymousLong1);
/*  992:     */         }
/*  993:     */         
/*  994:     */         boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
/*  995:     */         {
/*  996: 987 */           Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = (Attachment.ColoredCoinsAskOrderPlacement)paramAnonymousTransaction.attachment;
/*  997: 988 */           Integer localInteger = paramAnonymousAccount.getUnconfirmedAssetBalance(localColoredCoinsAskOrderPlacement.getAssetId());
/*  998: 989 */           if ((localInteger == null) || (localInteger.intValue() < localColoredCoinsAskOrderPlacement.getQuantity()))
/*  999:     */           {
/* 1000: 990 */             paramAnonymousAccount.addToUnconfirmedBalance(paramAnonymousInt * 100L);
/* 1001: 991 */             return true;
/* 1002:     */           }
/* 1003: 993 */           paramAnonymousAccount.addToUnconfirmedAssetBalance(localColoredCoinsAskOrderPlacement.getAssetId(), -localColoredCoinsAskOrderPlacement.getQuantity());
/* 1004: 994 */           return false;
/* 1005:     */         }
/* 1006:     */         
/* 1007:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1008:     */         {
/* 1009:1000 */           Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = (Attachment.ColoredCoinsAskOrderPlacement)paramAnonymousTransaction.attachment;
/* 1010:1001 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAskOrderPlacement.getAssetId(), -localColoredCoinsAskOrderPlacement.getQuantity());
/* 1011:1002 */           Order.Ask.addOrder(paramAnonymousTransaction.getId(), paramAnonymousAccount1, localColoredCoinsAskOrderPlacement.getAssetId(), localColoredCoinsAskOrderPlacement.getQuantity(), localColoredCoinsAskOrderPlacement.getPrice());
/* 1012:     */         }
/* 1013:     */         
/* 1014:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1015:     */           throws Transaction.UndoNotSupportedException
/* 1016:     */         {
/* 1017:1007 */           Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = (Attachment.ColoredCoinsAskOrderPlacement)paramAnonymousTransaction.attachment;
/* 1018:1008 */           Order.Ask localAsk = Order.Ask.removeOrder(paramAnonymousTransaction.getId());
/* 1019:1009 */           if ((localAsk == null) || (localAsk.getQuantity() != localColoredCoinsAskOrderPlacement.getQuantity()) || (!localAsk.getAssetId().equals(localColoredCoinsAskOrderPlacement.getAssetId()))) {
/* 1020:1011 */             throw new Transaction.UndoNotSupportedException(paramAnonymousTransaction, "Ask order already filled");
/* 1021:     */           }
/* 1022:1013 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAskOrderPlacement.getAssetId(), localColoredCoinsAskOrderPlacement.getQuantity());
/* 1023:     */         }
/* 1024:     */         
/* 1025:     */         void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong)
/* 1026:     */         {
/* 1027:1019 */           Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = (Attachment.ColoredCoinsAskOrderPlacement)paramAnonymousTransaction.attachment;
/* 1028:1020 */           Object localObject = (Map)paramAnonymousMap1.get(paramAnonymousTransaction.getSenderAccountId());
/* 1029:1021 */           if (localObject == null)
/* 1030:     */           {
/* 1031:1022 */             localObject = new HashMap();
/* 1032:1023 */             paramAnonymousMap1.put(paramAnonymousTransaction.getSenderAccountId(), localObject);
/* 1033:     */           }
/* 1034:1025 */           Long localLong = (Long)((Map)localObject).get(localColoredCoinsAskOrderPlacement.getAssetId());
/* 1035:1026 */           if (localLong == null) {
/* 1036:1027 */             localLong = Long.valueOf(0L);
/* 1037:     */           }
/* 1038:1029 */           ((Map)localObject).put(localColoredCoinsAskOrderPlacement.getAssetId(), Long.valueOf(localLong.longValue() + localColoredCoinsAskOrderPlacement.getQuantity()));
/* 1039:     */         }
/* 1040:     */       };
/* 1041:1034 */       public static final Transaction.Type BID_ORDER_PLACEMENT = new ColoredCoinsOrderPlacement()
/* 1042:     */       {
/* 1043:     */         public final byte getSubtype()
/* 1044:     */         {
/* 1045:1038 */           return 3;
/* 1046:     */         }
/* 1047:     */         
/* 1048:     */         final Attachment.ColoredCoinsOrderPlacement makeAttachment(Long paramAnonymousLong, int paramAnonymousInt, long paramAnonymousLong1)
/* 1049:     */         {
/* 1050:1042 */           return new Attachment.ColoredCoinsBidOrderPlacement(paramAnonymousLong, paramAnonymousInt, paramAnonymousLong1);
/* 1051:     */         }
/* 1052:     */         
/* 1053:     */         boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
/* 1054:     */         {
/* 1055:1047 */           Attachment.ColoredCoinsBidOrderPlacement localColoredCoinsBidOrderPlacement = (Attachment.ColoredCoinsBidOrderPlacement)paramAnonymousTransaction.attachment;
/* 1056:1048 */           if (paramAnonymousAccount.getUnconfirmedBalance() < localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice())
/* 1057:     */           {
/* 1058:1049 */             paramAnonymousAccount.addToUnconfirmedBalance(paramAnonymousInt * 100L);
/* 1059:1050 */             return true;
/* 1060:     */           }
/* 1061:1052 */           paramAnonymousAccount.addToUnconfirmedBalance(-localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice());
/* 1062:1053 */           return false;
/* 1063:     */         }
/* 1064:     */         
/* 1065:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1066:     */         {
/* 1067:1059 */           Attachment.ColoredCoinsBidOrderPlacement localColoredCoinsBidOrderPlacement = (Attachment.ColoredCoinsBidOrderPlacement)paramAnonymousTransaction.attachment;
/* 1068:1060 */           paramAnonymousAccount1.addToBalanceAndUnconfirmedBalance(-localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice());
/* 1069:1061 */           Order.Bid.addOrder(paramAnonymousTransaction.getId(), paramAnonymousAccount1, localColoredCoinsBidOrderPlacement.getAssetId(), localColoredCoinsBidOrderPlacement.getQuantity(), localColoredCoinsBidOrderPlacement.getPrice());
/* 1070:     */         }
/* 1071:     */         
/* 1072:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1073:     */           throws Transaction.UndoNotSupportedException
/* 1074:     */         {
/* 1075:1066 */           Attachment.ColoredCoinsBidOrderPlacement localColoredCoinsBidOrderPlacement = (Attachment.ColoredCoinsBidOrderPlacement)paramAnonymousTransaction.attachment;
/* 1076:1067 */           Order.Bid localBid = Order.Bid.removeOrder(paramAnonymousTransaction.getId());
/* 1077:1068 */           if ((localBid == null) || (localBid.getQuantity() != localColoredCoinsBidOrderPlacement.getQuantity()) || (!localBid.getAssetId().equals(localColoredCoinsBidOrderPlacement.getAssetId()))) {
/* 1078:1070 */             throw new Transaction.UndoNotSupportedException(paramAnonymousTransaction, "Bid order already filled");
/* 1079:     */           }
/* 1080:1072 */           paramAnonymousAccount1.addToBalanceAndUnconfirmedBalance(localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice());
/* 1081:     */         }
/* 1082:     */         
/* 1083:     */         void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong)
/* 1084:     */         {
/* 1085:1078 */           Attachment.ColoredCoinsBidOrderPlacement localColoredCoinsBidOrderPlacement = (Attachment.ColoredCoinsBidOrderPlacement)paramAnonymousTransaction.attachment;
/* 1086:1079 */           paramAnonymousMap.put(paramAnonymousTransaction.getSenderAccountId(), Long.valueOf(paramAnonymousLong.longValue() + localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice()));
/* 1087:     */         }
/* 1088:     */       };
/* 1089:     */       
/* 1090:     */       static abstract class ColoredCoinsOrderCancellation
/* 1091:     */         extends Transaction.Type.ColoredCoins
/* 1092:     */       {
/* 1093:     */         final boolean validateAttachment(Transaction paramTransaction)
/* 1094:     */         {
/* 1095:1100 */           return (Genesis.CREATOR_ID.equals(paramTransaction.recipientId)) && (paramTransaction.amount == 0);
/* 1096:     */         }
/* 1097:     */         
/* 1098:     */         final boolean checkDoubleSpending(Transaction paramTransaction, Account paramAccount, int paramInt)
/* 1099:     */         {
/* 1100:1105 */           return false;
/* 1101:     */         }
/* 1102:     */         
/* 1103:     */         final void updateTotals(Transaction paramTransaction, Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1, Long paramLong) {}
/* 1104:     */         
/* 1105:     */         final void undo(Transaction paramTransaction, Account paramAccount1, Account paramAccount2)
/* 1106:     */           throws Transaction.UndoNotSupportedException
/* 1107:     */         {
/* 1108:1114 */           throw new Transaction.UndoNotSupportedException(paramTransaction, "Reversal of order cancellation not supported");
/* 1109:     */         }
/* 1110:     */       }
/* 1111:     */       
/* 1112:1119 */       public static final Transaction.Type ASK_ORDER_CANCELLATION = new ColoredCoinsOrderCancellation()
/* 1113:     */       {
/* 1114:     */         public final byte getSubtype()
/* 1115:     */         {
/* 1116:1123 */           return 4;
/* 1117:     */         }
/* 1118:     */         
/* 1119:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/* 1120:     */         {
/* 1121:1128 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(Convert.zeroToNull(paramAnonymousByteBuffer.getLong()));
/* 1122:1129 */           return validateAttachment(paramAnonymousTransaction);
/* 1123:     */         }
/* 1124:     */         
/* 1125:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/* 1126:     */         {
/* 1127:1134 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(Convert.parseUnsignedLong((String)paramAnonymousJSONObject.get("order")));
/* 1128:1135 */           return validateAttachment(paramAnonymousTransaction);
/* 1129:     */         }
/* 1130:     */         
/* 1131:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1132:     */         {
/* 1133:1140 */           Attachment.ColoredCoinsAskOrderCancellation localColoredCoinsAskOrderCancellation = (Attachment.ColoredCoinsAskOrderCancellation)paramAnonymousTransaction.attachment;
/* 1134:1141 */           Order.Ask localAsk = Order.Ask.removeOrder(localColoredCoinsAskOrderCancellation.getOrderId());
/* 1135:1142 */           if (localAsk != null) {
/* 1136:1143 */             paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localAsk.getAssetId(), localAsk.getQuantity());
/* 1137:     */           }
/* 1138:     */         }
/* 1139:     */       };
/* 1140:1149 */       public static final Transaction.Type BID_ORDER_CANCELLATION = new ColoredCoinsOrderCancellation()
/* 1141:     */       {
/* 1142:     */         public final byte getSubtype()
/* 1143:     */         {
/* 1144:1153 */           return 5;
/* 1145:     */         }
/* 1146:     */         
/* 1147:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/* 1148:     */         {
/* 1149:1158 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(Convert.zeroToNull(paramAnonymousByteBuffer.getLong()));
/* 1150:1159 */           return validateAttachment(paramAnonymousTransaction);
/* 1151:     */         }
/* 1152:     */         
/* 1153:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/* 1154:     */         {
/* 1155:1164 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(Convert.parseUnsignedLong((String)paramAnonymousJSONObject.get("order")));
/* 1156:1165 */           return validateAttachment(paramAnonymousTransaction);
/* 1157:     */         }
/* 1158:     */         
/* 1159:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1160:     */         {
/* 1161:1170 */           Attachment.ColoredCoinsBidOrderCancellation localColoredCoinsBidOrderCancellation = (Attachment.ColoredCoinsBidOrderCancellation)paramAnonymousTransaction.attachment;
/* 1162:1171 */           Order.Bid localBid = Order.Bid.removeOrder(localColoredCoinsBidOrderCancellation.getOrderId());
/* 1163:1172 */           if (localBid != null) {
/* 1164:1173 */             paramAnonymousAccount1.addToBalanceAndUnconfirmedBalance(localBid.getQuantity() * localBid.getPrice());
/* 1165:     */           }
/* 1166:     */         }
/* 1167:     */       };
/* 1168:     */     }
/* 1169:     */   }
/* 1170:     */   
/* 1171:     */   public static final class UndoNotSupportedException
/* 1172:     */     extends NxtException
/* 1173:     */   {
/* 1174:     */     private final Transaction transaction;
/* 1175:     */     
/* 1176:     */     public UndoNotSupportedException(Transaction paramTransaction, String paramString)
/* 1177:     */     {
/* 1178:1202 */       super();
/* 1179:1203 */       this.transaction = paramTransaction;
/* 1180:     */     }
/* 1181:     */     
/* 1182:     */     public Transaction getTransaction()
/* 1183:     */     {
/* 1184:1207 */       return this.transaction;
/* 1185:     */     }
/* 1186:     */   }
/* 1187:     */   
/* 1188:     */   public static final class NotYetEnabledException
/* 1189:     */     extends NxtException.ValidationException
/* 1190:     */   {
/* 1191:     */     public NotYetEnabledException(String paramString)
/* 1192:     */     {
/* 1193:1215 */       super();
/* 1194:     */     }
/* 1195:     */     
/* 1196:     */     public NotYetEnabledException(String paramString, Throwable paramThrowable)
/* 1197:     */     {
/* 1198:1219 */       super(paramThrowable);
/* 1199:     */     }
/* 1200:     */   }
/* 1201:     */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Transaction
 * JD-Core Version:    0.7.0.1
 */