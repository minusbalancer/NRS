/*   1:    */ package nxt;
/*   2:    */ 
/*   3:    */ import java.io.Serializable;
/*   4:    */ import java.io.UnsupportedEncodingException;
/*   5:    */ import java.nio.ByteBuffer;
/*   6:    */ import java.nio.ByteOrder;
/*   7:    */ import nxt.util.Convert;
/*   8:    */ import nxt.util.Logger;
/*   9:    */ import org.json.simple.JSONObject;
/*  10:    */ import org.json.simple.JSONStreamAware;
/*  11:    */ 
/*  12:    */ public abstract interface Attachment
/*  13:    */ {
/*  14:    */   public abstract int getSize();
/*  15:    */   
/*  16:    */   public abstract byte[] getBytes();
/*  17:    */   
/*  18:    */   public abstract JSONStreamAware getJSON();
/*  19:    */   
/*  20:    */   public abstract Transaction.Type getTransactionType();
/*  21:    */   
/*  22:    */   public static class MessagingArbitraryMessage
/*  23:    */     implements Attachment, Serializable
/*  24:    */   {
/*  25:    */     static final long serialVersionUID = 0L;
/*  26:    */     private final byte[] message;
/*  27:    */     
/*  28:    */     public MessagingArbitraryMessage(byte[] paramArrayOfByte)
/*  29:    */     {
/*  30: 30 */       this.message = paramArrayOfByte;
/*  31:    */     }
/*  32:    */     
/*  33:    */     public int getSize()
/*  34:    */     {
/*  35: 36 */       return 4 + this.message.length;
/*  36:    */     }
/*  37:    */     
/*  38:    */     public byte[] getBytes()
/*  39:    */     {
/*  40: 42 */       ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
/*  41: 43 */       localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/*  42: 44 */       localByteBuffer.putInt(this.message.length);
/*  43: 45 */       localByteBuffer.put(this.message);
/*  44:    */       
/*  45: 47 */       return localByteBuffer.array();
/*  46:    */     }
/*  47:    */     
/*  48:    */     public JSONStreamAware getJSON()
/*  49:    */     {
/*  50: 54 */       JSONObject localJSONObject = new JSONObject();
/*  51: 55 */       localJSONObject.put("message", Convert.convert(this.message));
/*  52:    */       
/*  53: 57 */       return localJSONObject;
/*  54:    */     }
/*  55:    */     
/*  56:    */     public Transaction.Type getTransactionType()
/*  57:    */     {
/*  58: 63 */       return Transaction.Type.Messaging.ARBITRARY_MESSAGE;
/*  59:    */     }
/*  60:    */     
/*  61:    */     public byte[] getMessage()
/*  62:    */     {
/*  63: 67 */       return this.message;
/*  64:    */     }
/*  65:    */   }
/*  66:    */   
/*  67:    */   public static class MessagingAliasAssignment
/*  68:    */     implements Attachment, Serializable
/*  69:    */   {
/*  70:    */     static final long serialVersionUID = 0L;
/*  71:    */     private final String aliasName;
/*  72:    */     private final String aliasURI;
/*  73:    */     
/*  74:    */     public MessagingAliasAssignment(String paramString1, String paramString2)
/*  75:    */     {
/*  76: 80 */       this.aliasName = paramString1.trim().intern();
/*  77: 81 */       this.aliasURI = paramString2.trim().intern();
/*  78:    */     }
/*  79:    */     
/*  80:    */     public int getSize()
/*  81:    */     {
/*  82:    */       try
/*  83:    */       {
/*  84: 88 */         return 1 + this.aliasName.getBytes("UTF-8").length + 2 + this.aliasURI.getBytes("UTF-8").length;
/*  85:    */       }
/*  86:    */       catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
/*  87:    */       {
/*  88: 90 */         Logger.logMessage("Error in getBytes", localRuntimeException);
/*  89:    */       }
/*  90: 91 */       return 0;
/*  91:    */     }
/*  92:    */     
/*  93:    */     public byte[] getBytes()
/*  94:    */     {
/*  95:    */       try
/*  96:    */       {
/*  97:100 */         byte[] arrayOfByte1 = this.aliasName.getBytes("UTF-8");
/*  98:101 */         byte[] arrayOfByte2 = this.aliasURI.getBytes("UTF-8");
/*  99:    */         
/* 100:103 */         ByteBuffer localByteBuffer = ByteBuffer.allocate(1 + arrayOfByte1.length + 2 + arrayOfByte2.length);
/* 101:104 */         localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/* 102:105 */         localByteBuffer.put((byte)arrayOfByte1.length);
/* 103:106 */         localByteBuffer.put(arrayOfByte1);
/* 104:107 */         localByteBuffer.putShort((short)arrayOfByte2.length);
/* 105:108 */         localByteBuffer.put(arrayOfByte2);
/* 106:    */         
/* 107:110 */         return localByteBuffer.array();
/* 108:    */       }
/* 109:    */       catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
/* 110:    */       {
/* 111:113 */         Logger.logMessage("Error in getBytes", localRuntimeException);
/* 112:    */       }
/* 113:114 */       return null;
/* 114:    */     }
/* 115:    */     
/* 116:    */     public JSONStreamAware getJSON()
/* 117:    */     {
/* 118:123 */       JSONObject localJSONObject = new JSONObject();
/* 119:124 */       localJSONObject.put("alias", this.aliasName);
/* 120:125 */       localJSONObject.put("uri", this.aliasURI);
/* 121:    */       
/* 122:127 */       return localJSONObject;
/* 123:    */     }
/* 124:    */     
/* 125:    */     public Transaction.Type getTransactionType()
/* 126:    */     {
/* 127:133 */       return Transaction.Type.Messaging.ALIAS_ASSIGNMENT;
/* 128:    */     }
/* 129:    */     
/* 130:    */     public String getAliasName()
/* 131:    */     {
/* 132:137 */       return this.aliasName;
/* 133:    */     }
/* 134:    */     
/* 135:    */     public String getAliasURI()
/* 136:    */     {
/* 137:141 */       return this.aliasURI;
/* 138:    */     }
/* 139:    */   }
/* 140:    */   
/* 141:    */   public static class ColoredCoinsAssetIssuance
/* 142:    */     implements Attachment, Serializable
/* 143:    */   {
/* 144:    */     static final long serialVersionUID = 0L;
/* 145:    */     private final String name;
/* 146:    */     private final String description;
/* 147:    */     private final int quantity;
/* 148:    */     
/* 149:    */     public ColoredCoinsAssetIssuance(String paramString1, String paramString2, int paramInt)
/* 150:    */     {
/* 151:155 */       this.name = paramString1;
/* 152:156 */       this.description = (paramString2 == null ? "" : paramString2);
/* 153:157 */       this.quantity = paramInt;
/* 154:    */     }
/* 155:    */     
/* 156:    */     public int getSize()
/* 157:    */     {
/* 158:    */       try
/* 159:    */       {
/* 160:164 */         return 1 + this.name.getBytes("UTF-8").length + 2 + this.description.getBytes("UTF-8").length + 4;
/* 161:    */       }
/* 162:    */       catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
/* 163:    */       {
/* 164:166 */         Logger.logMessage("Error in getBytes", localRuntimeException);
/* 165:    */       }
/* 166:167 */       return 0;
/* 167:    */     }
/* 168:    */     
/* 169:    */     public byte[] getBytes()
/* 170:    */     {
/* 171:    */       try
/* 172:    */       {
/* 173:175 */         byte[] arrayOfByte1 = this.name.getBytes("UTF-8");
/* 174:176 */         byte[] arrayOfByte2 = this.description.getBytes("UTF-8");
/* 175:    */         
/* 176:178 */         ByteBuffer localByteBuffer = ByteBuffer.allocate(1 + arrayOfByte1.length + 2 + arrayOfByte2.length + 4);
/* 177:179 */         localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/* 178:180 */         localByteBuffer.put((byte)arrayOfByte1.length);
/* 179:181 */         localByteBuffer.put(arrayOfByte1);
/* 180:182 */         localByteBuffer.putShort((short)arrayOfByte2.length);
/* 181:183 */         localByteBuffer.put(arrayOfByte2);
/* 182:184 */         localByteBuffer.putInt(this.quantity);
/* 183:    */         
/* 184:186 */         return localByteBuffer.array();
/* 185:    */       }
/* 186:    */       catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
/* 187:    */       {
/* 188:188 */         Logger.logMessage("Error in getBytes", localRuntimeException);
/* 189:    */       }
/* 190:189 */       return null;
/* 191:    */     }
/* 192:    */     
/* 193:    */     public JSONStreamAware getJSON()
/* 194:    */     {
/* 195:197 */       JSONObject localJSONObject = new JSONObject();
/* 196:198 */       localJSONObject.put("name", this.name);
/* 197:199 */       localJSONObject.put("description", this.description);
/* 198:200 */       localJSONObject.put("quantity", Integer.valueOf(this.quantity));
/* 199:    */       
/* 200:202 */       return localJSONObject;
/* 201:    */     }
/* 202:    */     
/* 203:    */     public Transaction.Type getTransactionType()
/* 204:    */     {
/* 205:208 */       return Transaction.Type.ColoredCoins.ASSET_ISSUANCE;
/* 206:    */     }
/* 207:    */     
/* 208:    */     public String getName()
/* 209:    */     {
/* 210:212 */       return this.name;
/* 211:    */     }
/* 212:    */     
/* 213:    */     public String getDescription()
/* 214:    */     {
/* 215:216 */       return this.description;
/* 216:    */     }
/* 217:    */     
/* 218:    */     public int getQuantity()
/* 219:    */     {
/* 220:220 */       return this.quantity;
/* 221:    */     }
/* 222:    */   }
/* 223:    */   
/* 224:    */   public static class ColoredCoinsAssetTransfer
/* 225:    */     implements Attachment, Serializable
/* 226:    */   {
/* 227:    */     static final long serialVersionUID = 0L;
/* 228:    */     private final Long assetId;
/* 229:    */     private final int quantity;
/* 230:    */     
/* 231:    */     public ColoredCoinsAssetTransfer(Long paramLong, int paramInt)
/* 232:    */     {
/* 233:233 */       this.assetId = paramLong;
/* 234:234 */       this.quantity = paramInt;
/* 235:    */     }
/* 236:    */     
/* 237:    */     public int getSize()
/* 238:    */     {
/* 239:240 */       return 12;
/* 240:    */     }
/* 241:    */     
/* 242:    */     public byte[] getBytes()
/* 243:    */     {
/* 244:246 */       ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
/* 245:247 */       localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/* 246:248 */       localByteBuffer.putLong(Convert.nullToZero(this.assetId));
/* 247:249 */       localByteBuffer.putInt(this.quantity);
/* 248:    */       
/* 249:251 */       return localByteBuffer.array();
/* 250:    */     }
/* 251:    */     
/* 252:    */     public JSONStreamAware getJSON()
/* 253:    */     {
/* 254:258 */       JSONObject localJSONObject = new JSONObject();
/* 255:259 */       localJSONObject.put("asset", Convert.convert(this.assetId));
/* 256:260 */       localJSONObject.put("quantity", Integer.valueOf(this.quantity));
/* 257:    */       
/* 258:262 */       return localJSONObject;
/* 259:    */     }
/* 260:    */     
/* 261:    */     public Transaction.Type getTransactionType()
/* 262:    */     {
/* 263:268 */       return Transaction.Type.ColoredCoins.ASSET_TRANSFER;
/* 264:    */     }
/* 265:    */     
/* 266:    */     public Long getAssetId()
/* 267:    */     {
/* 268:272 */       return this.assetId;
/* 269:    */     }
/* 270:    */     
/* 271:    */     public int getQuantity()
/* 272:    */     {
/* 273:276 */       return this.quantity;
/* 274:    */     }
/* 275:    */   }
/* 276:    */   
/* 277:    */   public static abstract class ColoredCoinsOrderPlacement
/* 278:    */     implements Attachment, Serializable
/* 279:    */   {
/* 280:    */     static final long serialVersionUID = 0L;
/* 281:    */     private final Long assetId;
/* 282:    */     private final int quantity;
/* 283:    */     private final long price;
/* 284:    */     
/* 285:    */     private ColoredCoinsOrderPlacement(Long paramLong, int paramInt, long paramLong1)
/* 286:    */     {
/* 287:290 */       this.assetId = paramLong;
/* 288:291 */       this.quantity = paramInt;
/* 289:292 */       this.price = paramLong1;
/* 290:    */     }
/* 291:    */     
/* 292:    */     public int getSize()
/* 293:    */     {
/* 294:298 */       return 20;
/* 295:    */     }
/* 296:    */     
/* 297:    */     public byte[] getBytes()
/* 298:    */     {
/* 299:304 */       ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
/* 300:305 */       localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/* 301:306 */       localByteBuffer.putLong(Convert.nullToZero(this.assetId));
/* 302:307 */       localByteBuffer.putInt(this.quantity);
/* 303:308 */       localByteBuffer.putLong(this.price);
/* 304:    */       
/* 305:310 */       return localByteBuffer.array();
/* 306:    */     }
/* 307:    */     
/* 308:    */     public JSONStreamAware getJSON()
/* 309:    */     {
/* 310:317 */       JSONObject localJSONObject = new JSONObject();
/* 311:318 */       localJSONObject.put("asset", Convert.convert(this.assetId));
/* 312:319 */       localJSONObject.put("quantity", Integer.valueOf(this.quantity));
/* 313:320 */       localJSONObject.put("price", Long.valueOf(this.price));
/* 314:    */       
/* 315:322 */       return localJSONObject;
/* 316:    */     }
/* 317:    */     
/* 318:    */     public Long getAssetId()
/* 319:    */     {
/* 320:327 */       return this.assetId;
/* 321:    */     }
/* 322:    */     
/* 323:    */     public int getQuantity()
/* 324:    */     {
/* 325:331 */       return this.quantity;
/* 326:    */     }
/* 327:    */     
/* 328:    */     public long getPrice()
/* 329:    */     {
/* 330:335 */       return this.price;
/* 331:    */     }
/* 332:    */   }
/* 333:    */   
/* 334:    */   public static class ColoredCoinsAskOrderPlacement
/* 335:    */     extends Attachment.ColoredCoinsOrderPlacement
/* 336:    */   {
/* 337:    */     static final long serialVersionUID = 0L;
/* 338:    */     
/* 339:    */     public ColoredCoinsAskOrderPlacement(Long paramLong, int paramInt, long paramLong1)
/* 340:    */     {
/* 341:344 */       super(paramInt, paramLong1, null);
/* 342:    */     }
/* 343:    */     
/* 344:    */     public Transaction.Type getTransactionType()
/* 345:    */     {
/* 346:349 */       return Transaction.Type.ColoredCoins.ASK_ORDER_PLACEMENT;
/* 347:    */     }
/* 348:    */   }
/* 349:    */   
/* 350:    */   public static class ColoredCoinsBidOrderPlacement
/* 351:    */     extends Attachment.ColoredCoinsOrderPlacement
/* 352:    */   {
/* 353:    */     static final long serialVersionUID = 0L;
/* 354:    */     
/* 355:    */     public ColoredCoinsBidOrderPlacement(Long paramLong, int paramInt, long paramLong1)
/* 356:    */     {
/* 357:359 */       super(paramInt, paramLong1, null);
/* 358:    */     }
/* 359:    */     
/* 360:    */     public Transaction.Type getTransactionType()
/* 361:    */     {
/* 362:364 */       return Transaction.Type.ColoredCoins.BID_ORDER_PLACEMENT;
/* 363:    */     }
/* 364:    */   }
/* 365:    */   
/* 366:    */   public static abstract class ColoredCoinsOrderCancellation
/* 367:    */     implements Attachment, Serializable
/* 368:    */   {
/* 369:    */     static final long serialVersionUID = 0L;
/* 370:    */     private final Long orderId;
/* 371:    */     
/* 372:    */     private ColoredCoinsOrderCancellation(Long paramLong)
/* 373:    */     {
/* 374:376 */       this.orderId = paramLong;
/* 375:    */     }
/* 376:    */     
/* 377:    */     public int getSize()
/* 378:    */     {
/* 379:381 */       return 8;
/* 380:    */     }
/* 381:    */     
/* 382:    */     public byte[] getBytes()
/* 383:    */     {
/* 384:387 */       ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
/* 385:388 */       localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/* 386:389 */       localByteBuffer.putLong(Convert.nullToZero(this.orderId));
/* 387:    */       
/* 388:391 */       return localByteBuffer.array();
/* 389:    */     }
/* 390:    */     
/* 391:    */     public JSONStreamAware getJSON()
/* 392:    */     {
/* 393:398 */       JSONObject localJSONObject = new JSONObject();
/* 394:399 */       localJSONObject.put("order", Convert.convert(this.orderId));
/* 395:    */       
/* 396:401 */       return localJSONObject;
/* 397:    */     }
/* 398:    */     
/* 399:    */     public Long getOrderId()
/* 400:    */     {
/* 401:406 */       return this.orderId;
/* 402:    */     }
/* 403:    */   }
/* 404:    */   
/* 405:    */   public static class ColoredCoinsAskOrderCancellation
/* 406:    */     extends Attachment.ColoredCoinsOrderCancellation
/* 407:    */   {
/* 408:    */     static final long serialVersionUID = 0L;
/* 409:    */     
/* 410:    */     public ColoredCoinsAskOrderCancellation(Long paramLong)
/* 411:    */     {
/* 412:415 */       super(null);
/* 413:    */     }
/* 414:    */     
/* 415:    */     public Transaction.Type getTransactionType()
/* 416:    */     {
/* 417:420 */       return Transaction.Type.ColoredCoins.ASK_ORDER_CANCELLATION;
/* 418:    */     }
/* 419:    */   }
/* 420:    */   
/* 421:    */   public static class ColoredCoinsBidOrderCancellation
/* 422:    */     extends Attachment.ColoredCoinsOrderCancellation
/* 423:    */   {
/* 424:    */     static final long serialVersionUID = 0L;
/* 425:    */     
/* 426:    */     public ColoredCoinsBidOrderCancellation(Long paramLong)
/* 427:    */     {
/* 428:430 */       super(null);
/* 429:    */     }
/* 430:    */     
/* 431:    */     public Transaction.Type getTransactionType()
/* 432:    */     {
/* 433:435 */       return Transaction.Type.ColoredCoins.BID_ORDER_CANCELLATION;
/* 434:    */     }
/* 435:    */   }
/* 436:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Attachment
 * JD-Core Version:    0.7.0.1
 */