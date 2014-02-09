/*   1:    */ package nxt;
/*   2:    */ 
/*   3:    */ import java.io.IOException;
/*   4:    */ import java.io.ObjectInputStream;
/*   5:    */ import java.io.Serializable;
/*   6:    */ import java.lang.ref.SoftReference;
/*   7:    */ import java.math.BigInteger;
/*   8:    */ import java.nio.ByteBuffer;
/*   9:    */ import java.nio.ByteOrder;
/*  10:    */ import java.security.MessageDigest;
/*  11:    */ import java.util.Arrays;
/*  12:    */ import java.util.Comparator;
/*  13:    */ import nxt.crypto.Crypto;
/*  14:    */ import nxt.util.Convert;
/*  15:    */ import nxt.util.JSON;
/*  16:    */ import nxt.util.Logger;
/*  17:    */ import org.json.simple.JSONArray;
/*  18:    */ import org.json.simple.JSONObject;
/*  19:    */ import org.json.simple.JSONStreamAware;
/*  20:    */ 
/*  21:    */ public final class Block
/*  22:    */   implements Serializable
/*  23:    */ {
/*  24:    */   static final long serialVersionUID = 0L;
/*  25: 25 */   static final Long[] emptyLong = new Long[0];
/*  26: 26 */   static final Transaction[] emptyTransactions = new Transaction[0];
/*  27: 28 */   public static final Comparator<Block> heightComparator = new Comparator()
/*  28:    */   {
/*  29:    */     public int compare(Block paramAnonymousBlock1, Block paramAnonymousBlock2)
/*  30:    */     {
/*  31: 31 */       return paramAnonymousBlock1.height > paramAnonymousBlock2.height ? 1 : paramAnonymousBlock1.height < paramAnonymousBlock2.height ? -1 : 0;
/*  32:    */     }
/*  33:    */   };
/*  34:    */   private final int version;
/*  35:    */   private final int timestamp;
/*  36:    */   private final Long previousBlockId;
/*  37:    */   private final byte[] generatorPublicKey;
/*  38:    */   private final byte[] previousBlockHash;
/*  39:    */   private final int totalAmount;
/*  40:    */   private final int totalFee;
/*  41:    */   private final int payloadLength;
/*  42:    */   final Long[] transactionIds;
/*  43:    */   transient Transaction[] blockTransactions;
/*  44:    */   
/*  45:    */   static Block getBlock(JSONObject paramJSONObject)
/*  46:    */     throws NxtException.ValidationException
/*  47:    */   {
/*  48:    */     try
/*  49:    */     {
/*  50: 39 */       int i = ((Long)paramJSONObject.get("version")).intValue();
/*  51: 40 */       int j = ((Long)paramJSONObject.get("timestamp")).intValue();
/*  52: 41 */       Long localLong = Convert.parseUnsignedLong((String)paramJSONObject.get("previousBlock"));
/*  53: 42 */       int k = ((Long)paramJSONObject.get("numberOfTransactions")).intValue();
/*  54: 43 */       int m = ((Long)paramJSONObject.get("totalAmount")).intValue();
/*  55: 44 */       int n = ((Long)paramJSONObject.get("totalFee")).intValue();
/*  56: 45 */       int i1 = ((Long)paramJSONObject.get("payloadLength")).intValue();
/*  57: 46 */       byte[] arrayOfByte1 = Convert.convert((String)paramJSONObject.get("payloadHash"));
/*  58: 47 */       byte[] arrayOfByte2 = Convert.convert((String)paramJSONObject.get("generatorPublicKey"));
/*  59: 48 */       byte[] arrayOfByte3 = Convert.convert((String)paramJSONObject.get("generationSignature"));
/*  60: 49 */       byte[] arrayOfByte4 = Convert.convert((String)paramJSONObject.get("blockSignature"));
/*  61: 50 */       byte[] arrayOfByte5 = i == 1 ? null : Convert.convert((String)paramJSONObject.get("previousBlockHash"));
/*  62: 51 */       if ((k > 255) || (i1 > 32640)) {
/*  63: 52 */         throw new NxtException.ValidationException("Invalid number of transactions or payload length");
/*  64:    */       }
/*  65: 55 */       return new Block(i, j, localLong, k, m, n, i1, arrayOfByte1, arrayOfByte2, arrayOfByte3, arrayOfByte4, arrayOfByte5);
/*  66:    */     }
/*  67:    */     catch (RuntimeException localRuntimeException)
/*  68:    */     {
/*  69: 59 */       throw new NxtException.ValidationException(localRuntimeException.toString(), localRuntimeException);
/*  70:    */     }
/*  71:    */   }
/*  72:    */   
/*  73: 77 */   public BigInteger cumulativeDifficulty = BigInteger.ZERO;
/*  74: 78 */   public long baseTarget = 153722867L;
/*  75:    */   public volatile Long nextBlockId;
/*  76:    */   public int index;
/*  77:    */   public int height;
/*  78:    */   private byte[] generationSignature;
/*  79:    */   private byte[] blockSignature;
/*  80:    */   private byte[] payloadHash;
/*  81:    */   private volatile transient Long id;
/*  82: 87 */   private volatile transient String stringId = null;
/*  83:    */   private volatile transient Long generatorAccountId;
/*  84:    */   private transient SoftReference<JSONStreamAware> jsonRef;
/*  85:    */   
/*  86:    */   Block(int paramInt1, int paramInt2, Long paramLong, int paramInt3, int paramInt4, int paramInt5, int paramInt6, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
/*  87:    */     throws NxtException.ValidationException
/*  88:    */   {
/*  89: 95 */     this(paramInt1, paramInt2, paramLong, paramInt3, paramInt4, paramInt5, paramInt6, paramArrayOfByte1, paramArrayOfByte2, paramArrayOfByte3, paramArrayOfByte4, null);
/*  90:    */   }
/*  91:    */   
/*  92:    */   public Block(int paramInt1, int paramInt2, Long paramLong, int paramInt3, int paramInt4, int paramInt5, int paramInt6, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4, byte[] paramArrayOfByte5)
/*  93:    */     throws NxtException.ValidationException
/*  94:    */   {
/*  95:105 */     if ((paramInt3 > 255) || (paramInt3 < 0)) {
/*  96:106 */       throw new NxtException.ValidationException("attempted to create a block with " + paramInt3 + " transactions");
/*  97:    */     }
/*  98:109 */     if ((paramInt6 > 32640) || (paramInt6 < 0)) {
/*  99:110 */       throw new NxtException.ValidationException("attempted to create a block with payloadLength " + paramInt6);
/* 100:    */     }
/* 101:113 */     this.version = paramInt1;
/* 102:114 */     this.timestamp = paramInt2;
/* 103:115 */     this.previousBlockId = paramLong;
/* 104:116 */     this.totalAmount = paramInt4;
/* 105:117 */     this.totalFee = paramInt5;
/* 106:118 */     this.payloadLength = paramInt6;
/* 107:119 */     this.payloadHash = paramArrayOfByte1;
/* 108:120 */     this.generatorPublicKey = paramArrayOfByte2;
/* 109:121 */     this.generationSignature = paramArrayOfByte3;
/* 110:122 */     this.blockSignature = paramArrayOfByte4;
/* 111:    */     
/* 112:124 */     this.previousBlockHash = paramArrayOfByte5;
/* 113:125 */     this.transactionIds = (paramInt3 == 0 ? emptyLong : new Long[paramInt3]);
/* 114:126 */     this.blockTransactions = (paramInt3 == 0 ? emptyTransactions : new Transaction[paramInt3]);
/* 115:    */   }
/* 116:    */   
/* 117:    */   public int getVersion()
/* 118:    */   {
/* 119:131 */     return this.version;
/* 120:    */   }
/* 121:    */   
/* 122:    */   public int getTimestamp()
/* 123:    */   {
/* 124:135 */     return this.timestamp;
/* 125:    */   }
/* 126:    */   
/* 127:    */   public Long getPreviousBlockId()
/* 128:    */   {
/* 129:139 */     return this.previousBlockId;
/* 130:    */   }
/* 131:    */   
/* 132:    */   public byte[] getGeneratorPublicKey()
/* 133:    */   {
/* 134:143 */     return this.generatorPublicKey;
/* 135:    */   }
/* 136:    */   
/* 137:    */   public byte[] getPreviousBlockHash()
/* 138:    */   {
/* 139:147 */     return this.previousBlockHash;
/* 140:    */   }
/* 141:    */   
/* 142:    */   public int getTotalAmount()
/* 143:    */   {
/* 144:151 */     return this.totalAmount;
/* 145:    */   }
/* 146:    */   
/* 147:    */   public int getTotalFee()
/* 148:    */   {
/* 149:155 */     return this.totalFee;
/* 150:    */   }
/* 151:    */   
/* 152:    */   public int getPayloadLength()
/* 153:    */   {
/* 154:159 */     return this.payloadLength;
/* 155:    */   }
/* 156:    */   
/* 157:    */   public Long[] getTransactionIds()
/* 158:    */   {
/* 159:163 */     return this.transactionIds;
/* 160:    */   }
/* 161:    */   
/* 162:    */   public byte[] getPayloadHash()
/* 163:    */   {
/* 164:167 */     return this.payloadHash;
/* 165:    */   }
/* 166:    */   
/* 167:    */   void setPayloadHash(byte[] paramArrayOfByte)
/* 168:    */   {
/* 169:171 */     this.payloadHash = paramArrayOfByte;
/* 170:    */   }
/* 171:    */   
/* 172:    */   public byte[] getGenerationSignature()
/* 173:    */   {
/* 174:175 */     return this.generationSignature;
/* 175:    */   }
/* 176:    */   
/* 177:    */   void setGenerationSignature(byte[] paramArrayOfByte)
/* 178:    */   {
/* 179:179 */     this.generationSignature = paramArrayOfByte;
/* 180:    */   }
/* 181:    */   
/* 182:    */   public byte[] getBlockSignature()
/* 183:    */   {
/* 184:183 */     return this.blockSignature;
/* 185:    */   }
/* 186:    */   
/* 187:    */   void setBlockSignature(byte[] paramArrayOfByte)
/* 188:    */   {
/* 189:187 */     this.blockSignature = paramArrayOfByte;
/* 190:    */   }
/* 191:    */   
/* 192:    */   public Transaction[] getTransactions()
/* 193:    */   {
/* 194:191 */     return this.blockTransactions;
/* 195:    */   }
/* 196:    */   
/* 197:    */   public long getBaseTarget()
/* 198:    */   {
/* 199:195 */     return this.baseTarget;
/* 200:    */   }
/* 201:    */   
/* 202:    */   public BigInteger getCumulativeDifficulty()
/* 203:    */   {
/* 204:199 */     return this.cumulativeDifficulty;
/* 205:    */   }
/* 206:    */   
/* 207:    */   public Long getNextBlockId()
/* 208:    */   {
/* 209:203 */     return this.nextBlockId;
/* 210:    */   }
/* 211:    */   
/* 212:    */   public int getIndex()
/* 213:    */   {
/* 214:207 */     return this.index;
/* 215:    */   }
/* 216:    */   
/* 217:    */   void setIndex(int paramInt)
/* 218:    */   {
/* 219:211 */     this.index = paramInt;
/* 220:    */   }
/* 221:    */   
/* 222:    */   public int getHeight()
/* 223:    */   {
/* 224:215 */     return this.height;
/* 225:    */   }
/* 226:    */   
/* 227:    */   void setHeight(int paramInt)
/* 228:    */   {
/* 229:219 */     this.height = paramInt;
/* 230:    */   }
/* 231:    */   
/* 232:    */   public Long getId()
/* 233:    */   {
/* 234:223 */     calculateIds();
/* 235:224 */     return this.id;
/* 236:    */   }
/* 237:    */   
/* 238:    */   public String getStringId()
/* 239:    */   {
/* 240:228 */     calculateIds();
/* 241:229 */     return this.stringId;
/* 242:    */   }
/* 243:    */   
/* 244:    */   public Long getGeneratorAccountId()
/* 245:    */   {
/* 246:233 */     calculateIds();
/* 247:234 */     return this.generatorAccountId;
/* 248:    */   }
/* 249:    */   
/* 250:    */   public synchronized JSONStreamAware getJSON()
/* 251:    */   {
/* 252:239 */     if (this.jsonRef != null)
/* 253:    */     {
/* 254:240 */       localJSONStreamAware = (JSONStreamAware)this.jsonRef.get();
/* 255:241 */       if (localJSONStreamAware != null) {
/* 256:242 */         return localJSONStreamAware;
/* 257:    */       }
/* 258:    */     }
/* 259:245 */     JSONStreamAware localJSONStreamAware = JSON.prepare(getJSONObject());
/* 260:246 */     this.jsonRef = new SoftReference(localJSONStreamAware);
/* 261:247 */     return localJSONStreamAware;
/* 262:    */   }
/* 263:    */   
/* 264:    */   public boolean equals(Object paramObject)
/* 265:    */   {
/* 266:252 */     return ((paramObject instanceof Block)) && (getId().equals(((Block)paramObject).getId()));
/* 267:    */   }
/* 268:    */   
/* 269:    */   public int hashCode()
/* 270:    */   {
/* 271:257 */     return getId().hashCode();
/* 272:    */   }
/* 273:    */   
/* 274:    */   byte[] getBytes()
/* 275:    */   {
/* 276:262 */     ByteBuffer localByteBuffer = ByteBuffer.allocate(224);
/* 277:263 */     localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/* 278:264 */     localByteBuffer.putInt(this.version);
/* 279:265 */     localByteBuffer.putInt(this.timestamp);
/* 280:266 */     localByteBuffer.putLong(Convert.nullToZero(this.previousBlockId));
/* 281:267 */     localByteBuffer.putInt(this.transactionIds.length);
/* 282:268 */     localByteBuffer.putInt(this.totalAmount);
/* 283:269 */     localByteBuffer.putInt(this.totalFee);
/* 284:270 */     localByteBuffer.putInt(this.payloadLength);
/* 285:271 */     localByteBuffer.put(this.payloadHash);
/* 286:272 */     localByteBuffer.put(this.generatorPublicKey);
/* 287:273 */     localByteBuffer.put(this.generationSignature);
/* 288:274 */     if (this.version > 1) {
/* 289:275 */       localByteBuffer.put(this.previousBlockHash);
/* 290:    */     }
/* 291:277 */     localByteBuffer.put(this.blockSignature);
/* 292:278 */     return localByteBuffer.array();
/* 293:    */   }
/* 294:    */   
/* 295:    */   JSONObject getJSONObject()
/* 296:    */   {
/* 297:283 */     JSONObject localJSONObject = new JSONObject();
/* 298:    */     
/* 299:285 */     localJSONObject.put("version", Integer.valueOf(this.version));
/* 300:286 */     localJSONObject.put("timestamp", Integer.valueOf(this.timestamp));
/* 301:287 */     localJSONObject.put("previousBlock", Convert.convert(this.previousBlockId));
/* 302:288 */     localJSONObject.put("numberOfTransactions", Integer.valueOf(this.transactionIds.length));
/* 303:289 */     localJSONObject.put("totalAmount", Integer.valueOf(this.totalAmount));
/* 304:290 */     localJSONObject.put("totalFee", Integer.valueOf(this.totalFee));
/* 305:291 */     localJSONObject.put("payloadLength", Integer.valueOf(this.payloadLength));
/* 306:292 */     localJSONObject.put("payloadHash", Convert.convert(this.payloadHash));
/* 307:293 */     localJSONObject.put("generatorPublicKey", Convert.convert(this.generatorPublicKey));
/* 308:294 */     localJSONObject.put("generationSignature", Convert.convert(this.generationSignature));
/* 309:295 */     if (this.version > 1) {
/* 310:297 */       localJSONObject.put("previousBlockHash", Convert.convert(this.previousBlockHash));
/* 311:    */     }
/* 312:300 */     localJSONObject.put("blockSignature", Convert.convert(this.blockSignature));
/* 313:    */     
/* 314:302 */     JSONArray localJSONArray = new JSONArray();
/* 315:303 */     for (Transaction localTransaction : this.blockTransactions) {
/* 316:305 */       localJSONArray.add(localTransaction.getJSONObject());
/* 317:    */     }
/* 318:308 */     localJSONObject.put("transactions", localJSONArray);
/* 319:    */     
/* 320:310 */     return localJSONObject;
/* 321:    */   }
/* 322:    */   
/* 323:    */   boolean verifyBlockSignature()
/* 324:    */   {
/* 325:316 */     Account localAccount = Account.getAccount(getGeneratorAccountId());
/* 326:317 */     if (localAccount == null) {
/* 327:319 */       return false;
/* 328:    */     }
/* 329:323 */     byte[] arrayOfByte1 = getBytes();
/* 330:324 */     byte[] arrayOfByte2 = new byte[arrayOfByte1.length - 64];
/* 331:325 */     System.arraycopy(arrayOfByte1, 0, arrayOfByte2, 0, arrayOfByte2.length);
/* 332:    */     
/* 333:327 */     return (Crypto.verify(this.blockSignature, arrayOfByte2, this.generatorPublicKey)) && (localAccount.setOrVerify(this.generatorPublicKey));
/* 334:    */   }
/* 335:    */   
/* 336:    */   boolean verifyGenerationSignature()
/* 337:    */   {
/* 338:    */     try
/* 339:    */     {
/* 340:335 */       Block localBlock = Blockchain.getBlock(this.previousBlockId);
/* 341:336 */       if (localBlock == null) {
/* 342:338 */         return false;
/* 343:    */       }
/* 344:342 */       if ((this.version == 1) && (!Crypto.verify(this.generationSignature, localBlock.generationSignature, this.generatorPublicKey))) {
/* 345:344 */         return false;
/* 346:    */       }
/* 347:348 */       Account localAccount = Account.getAccount(getGeneratorAccountId());
/* 348:349 */       if ((localAccount == null) || (localAccount.getEffectiveBalance() <= 0)) {
/* 349:351 */         return false;
/* 350:    */       }
/* 351:355 */       int i = this.timestamp - localBlock.timestamp;
/* 352:356 */       BigInteger localBigInteger1 = BigInteger.valueOf(Blockchain.getLastBlock().baseTarget).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance())).multiply(BigInteger.valueOf(i));
/* 353:    */       
/* 354:358 */       MessageDigest localMessageDigest = Crypto.sha256();
/* 355:    */       byte[] arrayOfByte;
/* 356:360 */       if (this.version == 1)
/* 357:    */       {
/* 358:362 */         arrayOfByte = localMessageDigest.digest(this.generationSignature);
/* 359:    */       }
/* 360:    */       else
/* 361:    */       {
/* 362:366 */         localMessageDigest.update(localBlock.generationSignature);
/* 363:367 */         arrayOfByte = localMessageDigest.digest(this.generatorPublicKey);
/* 364:368 */         if (!Arrays.equals(this.generationSignature, arrayOfByte)) {
/* 365:370 */           return false;
/* 366:    */         }
/* 367:    */       }
/* 368:376 */       BigInteger localBigInteger2 = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
/* 369:    */       
/* 370:378 */       return localBigInteger2.compareTo(localBigInteger1) < 0;
/* 371:    */     }
/* 372:    */     catch (RuntimeException localRuntimeException)
/* 373:    */     {
/* 374:382 */       Logger.logMessage("Error verifying block generation signature", localRuntimeException);
/* 375:    */     }
/* 376:383 */     return false;
/* 377:    */   }
/* 378:    */   
/* 379:    */   void apply()
/* 380:    */   {
/* 381:391 */     for (int i = 0; i < this.transactionIds.length; i++)
/* 382:    */     {
/* 383:392 */       this.blockTransactions[i] = Blockchain.getTransaction(this.transactionIds[i]);
/* 384:393 */       if (this.blockTransactions[i] == null) {
/* 385:394 */         throw new IllegalStateException("Missing transaction " + Convert.convert(this.transactionIds[i]));
/* 386:    */       }
/* 387:    */     }
/* 388:398 */     if ((this.previousBlockId == null) && (getId().equals(Genesis.GENESIS_BLOCK_ID)))
/* 389:    */     {
/* 390:400 */       calculateBaseTarget();
/* 391:401 */       Blockchain.addBlock(this);
/* 392:    */     }
/* 393:    */     else
/* 394:    */     {
/* 395:405 */       localObject = Blockchain.getLastBlock();
/* 396:    */       
/* 397:407 */       ((Block)localObject).nextBlockId = getId();
/* 398:408 */       this.height = (((Block)localObject).height + 1);
/* 399:409 */       calculateBaseTarget();
/* 400:410 */       Blockchain.addBlock(this);
/* 401:    */     }
/* 402:414 */     Object localObject = Account.addOrGetAccount(getGeneratorAccountId());
/* 403:415 */     if (!((Account)localObject).setOrVerify(this.generatorPublicKey)) {
/* 404:416 */       throw new IllegalStateException("Generator public key mismatch");
/* 405:    */     }
/* 406:418 */     ((Account)localObject).addToBalanceAndUnconfirmedBalance(this.totalFee * 100L);
/* 407:420 */     for (Transaction localTransaction : this.blockTransactions)
/* 408:    */     {
/* 409:422 */       localTransaction.setHeight(this.height);
/* 410:423 */       localTransaction.setBlockId(getId());
/* 411:424 */       localTransaction.apply();
/* 412:    */     }
/* 413:428 */     Blockchain.purgeExpiredHashes(this.timestamp);
/* 414:    */   }
/* 415:    */   
/* 416:    */   private void calculateBaseTarget()
/* 417:    */   {
/* 418:434 */     if ((getId().equals(Genesis.GENESIS_BLOCK_ID)) && (this.previousBlockId == null))
/* 419:    */     {
/* 420:435 */       this.baseTarget = 153722867L;
/* 421:436 */       this.cumulativeDifficulty = BigInteger.ZERO;
/* 422:    */     }
/* 423:    */     else
/* 424:    */     {
/* 425:438 */       Block localBlock = Blockchain.getBlock(this.previousBlockId);
/* 426:439 */       long l1 = localBlock.baseTarget;
/* 427:440 */       long l2 = BigInteger.valueOf(l1).multiply(BigInteger.valueOf(this.timestamp - localBlock.timestamp)).divide(BigInteger.valueOf(60L)).longValue();
/* 428:443 */       if ((l2 < 0L) || (l2 > 153722867000000000L)) {
/* 429:444 */         l2 = 153722867000000000L;
/* 430:    */       }
/* 431:446 */       if (l2 < l1 / 2L) {
/* 432:447 */         l2 = l1 / 2L;
/* 433:    */       }
/* 434:449 */       if (l2 == 0L) {
/* 435:450 */         l2 = 1L;
/* 436:    */       }
/* 437:452 */       long l3 = l1 * 2L;
/* 438:453 */       if (l3 < 0L) {
/* 439:454 */         l3 = 153722867000000000L;
/* 440:    */       }
/* 441:456 */       if (l2 > l3) {
/* 442:457 */         l2 = l3;
/* 443:    */       }
/* 444:459 */       this.baseTarget = l2;
/* 445:460 */       this.cumulativeDifficulty = localBlock.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(this.baseTarget)));
/* 446:    */     }
/* 447:    */   }
/* 448:    */   
/* 449:    */   private void calculateIds()
/* 450:    */   {
/* 451:465 */     if (this.stringId != null) {
/* 452:466 */       return;
/* 453:    */     }
/* 454:468 */     byte[] arrayOfByte = Crypto.sha256().digest(getBytes());
/* 455:469 */     BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
/* 456:470 */     this.id = Long.valueOf(localBigInteger.longValue());
/* 457:471 */     this.stringId = localBigInteger.toString();
/* 458:472 */     this.generatorAccountId = Account.getId(this.generatorPublicKey);
/* 459:    */   }
/* 460:    */   
/* 461:    */   private void readObject(ObjectInputStream paramObjectInputStream)
/* 462:    */     throws IOException, ClassNotFoundException
/* 463:    */   {
/* 464:476 */     paramObjectInputStream.defaultReadObject();
/* 465:477 */     this.blockTransactions = (this.transactionIds.length == 0 ? emptyTransactions : new Transaction[this.transactionIds.length]);
/* 466:478 */     for (int i = 0; i < this.transactionIds.length; i++) {
/* 467:479 */       this.blockTransactions[i] = Blockchain.getTransaction(this.transactionIds[i]);
/* 468:    */     }
/* 469:    */   }
/* 470:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Block
 * JD-Core Version:    0.7.0.1
 */