/*   1:    */ package nxt;
/*   2:    */ 
/*   3:    */ import java.lang.ref.SoftReference;
/*   4:    */ import java.math.BigInteger;
/*   5:    */ import java.nio.ByteBuffer;
/*   6:    */ import java.nio.ByteOrder;
/*   7:    */ import java.security.MessageDigest;
/*   8:    */ import java.sql.Connection;
/*   9:    */ import java.sql.PreparedStatement;
/*  10:    */ import java.sql.ResultSet;
/*  11:    */ import java.sql.SQLException;
/*  12:    */ import java.util.Arrays;
/*  13:    */ import java.util.Comparator;
/*  14:    */ import java.util.List;
/*  15:    */ import nxt.crypto.Crypto;
/*  16:    */ import nxt.util.Convert;
/*  17:    */ import nxt.util.JSON;
/*  18:    */ import nxt.util.Logger;
/*  19:    */ import org.json.simple.JSONArray;
/*  20:    */ import org.json.simple.JSONObject;
/*  21:    */ import org.json.simple.JSONStreamAware;
/*  22:    */ 
/*  23:    */ public final class Block
/*  24:    */ {
/*  25: 27 */   static final Long[] emptyLong = new Long[0];
/*  26: 28 */   static final Transaction[] emptyTransactions = new Transaction[0];
/*  27: 30 */   public static final Comparator<Block> heightComparator = new Comparator()
/*  28:    */   {
/*  29:    */     public int compare(Block paramAnonymousBlock1, Block paramAnonymousBlock2)
/*  30:    */     {
/*  31: 33 */       return paramAnonymousBlock1.height > paramAnonymousBlock2.height ? 1 : paramAnonymousBlock1.height < paramAnonymousBlock2.height ? -1 : 0;
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
/*  43:    */   final Transaction[] blockTransactions;
/*  44:    */   
/*  45:    */   /* Error */
/*  46:    */   static Block findBlock(Long paramLong)
/*  47:    */   {
/*  48:    */     // Byte code:
/*  49:    */     //   0: invokestatic 2	nxt/Db:getConnection	()Ljava/sql/Connection;
/*  50:    */     //   3: astore_1
/*  51:    */     //   4: aconst_null
/*  52:    */     //   5: astore_2
/*  53:    */     //   6: aload_1
/*  54:    */     //   7: ldc 3
/*  55:    */     //   9: invokeinterface 4 2 0
/*  56:    */     //   14: astore_3
/*  57:    */     //   15: aconst_null
/*  58:    */     //   16: astore 4
/*  59:    */     //   18: aload_3
/*  60:    */     //   19: iconst_1
/*  61:    */     //   20: aload_0
/*  62:    */     //   21: invokevirtual 5	java/lang/Long:longValue	()J
/*  63:    */     //   24: invokeinterface 6 4 0
/*  64:    */     //   29: aload_3
/*  65:    */     //   30: invokeinterface 7 1 0
/*  66:    */     //   35: astore 5
/*  67:    */     //   37: aconst_null
/*  68:    */     //   38: astore 6
/*  69:    */     //   40: aload 5
/*  70:    */     //   42: invokeinterface 8 1 0
/*  71:    */     //   47: ifeq +11 -> 58
/*  72:    */     //   50: aload_1
/*  73:    */     //   51: aload 5
/*  74:    */     //   53: invokestatic 9	nxt/Block:getBlock	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/Block;
/*  75:    */     //   56: astore 6
/*  76:    */     //   58: aload 5
/*  77:    */     //   60: invokeinterface 10 1 0
/*  78:    */     //   65: aload 6
/*  79:    */     //   67: astore 7
/*  80:    */     //   69: aload_3
/*  81:    */     //   70: ifnull +35 -> 105
/*  82:    */     //   73: aload 4
/*  83:    */     //   75: ifnull +24 -> 99
/*  84:    */     //   78: aload_3
/*  85:    */     //   79: invokeinterface 11 1 0
/*  86:    */     //   84: goto +21 -> 105
/*  87:    */     //   87: astore 8
/*  88:    */     //   89: aload 4
/*  89:    */     //   91: aload 8
/*  90:    */     //   93: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  91:    */     //   96: goto +9 -> 105
/*  92:    */     //   99: aload_3
/*  93:    */     //   100: invokeinterface 11 1 0
/*  94:    */     //   105: aload_1
/*  95:    */     //   106: ifnull +33 -> 139
/*  96:    */     //   109: aload_2
/*  97:    */     //   110: ifnull +23 -> 133
/*  98:    */     //   113: aload_1
/*  99:    */     //   114: invokeinterface 14 1 0
/* 100:    */     //   119: goto +20 -> 139
/* 101:    */     //   122: astore 8
/* 102:    */     //   124: aload_2
/* 103:    */     //   125: aload 8
/* 104:    */     //   127: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 105:    */     //   130: goto +9 -> 139
/* 106:    */     //   133: aload_1
/* 107:    */     //   134: invokeinterface 14 1 0
/* 108:    */     //   139: aload 7
/* 109:    */     //   141: areturn
/* 110:    */     //   142: astore 5
/* 111:    */     //   144: aload 5
/* 112:    */     //   146: astore 4
/* 113:    */     //   148: aload 5
/* 114:    */     //   150: athrow
/* 115:    */     //   151: astore 9
/* 116:    */     //   153: aload_3
/* 117:    */     //   154: ifnull +35 -> 189
/* 118:    */     //   157: aload 4
/* 119:    */     //   159: ifnull +24 -> 183
/* 120:    */     //   162: aload_3
/* 121:    */     //   163: invokeinterface 11 1 0
/* 122:    */     //   168: goto +21 -> 189
/* 123:    */     //   171: astore 10
/* 124:    */     //   173: aload 4
/* 125:    */     //   175: aload 10
/* 126:    */     //   177: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 127:    */     //   180: goto +9 -> 189
/* 128:    */     //   183: aload_3
/* 129:    */     //   184: invokeinterface 11 1 0
/* 130:    */     //   189: aload 9
/* 131:    */     //   191: athrow
/* 132:    */     //   192: astore_3
/* 133:    */     //   193: aload_3
/* 134:    */     //   194: astore_2
/* 135:    */     //   195: aload_3
/* 136:    */     //   196: athrow
/* 137:    */     //   197: astore 11
/* 138:    */     //   199: aload_1
/* 139:    */     //   200: ifnull +33 -> 233
/* 140:    */     //   203: aload_2
/* 141:    */     //   204: ifnull +23 -> 227
/* 142:    */     //   207: aload_1
/* 143:    */     //   208: invokeinterface 14 1 0
/* 144:    */     //   213: goto +20 -> 233
/* 145:    */     //   216: astore 12
/* 146:    */     //   218: aload_2
/* 147:    */     //   219: aload 12
/* 148:    */     //   221: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 149:    */     //   224: goto +9 -> 233
/* 150:    */     //   227: aload_1
/* 151:    */     //   228: invokeinterface 14 1 0
/* 152:    */     //   233: aload 11
/* 153:    */     //   235: athrow
/* 154:    */     //   236: astore_1
/* 155:    */     //   237: new 16	java/lang/RuntimeException
/* 156:    */     //   240: dup
/* 157:    */     //   241: aload_1
/* 158:    */     //   242: invokevirtual 17	java/sql/SQLException:getMessage	()Ljava/lang/String;
/* 159:    */     //   245: aload_1
/* 160:    */     //   246: invokespecial 18	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/* 161:    */     //   249: athrow
/* 162:    */     //   250: astore_1
/* 163:    */     //   251: new 16	java/lang/RuntimeException
/* 164:    */     //   254: dup
/* 165:    */     //   255: new 20	java/lang/StringBuilder
/* 166:    */     //   258: dup
/* 167:    */     //   259: invokespecial 21	java/lang/StringBuilder:<init>	()V
/* 168:    */     //   262: ldc 22
/* 169:    */     //   264: invokevirtual 23	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
/* 170:    */     //   267: aload_0
/* 171:    */     //   268: invokevirtual 24	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
/* 172:    */     //   271: ldc 25
/* 173:    */     //   273: invokevirtual 23	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
/* 174:    */     //   276: invokevirtual 26	java/lang/StringBuilder:toString	()Ljava/lang/String;
/* 175:    */     //   279: invokespecial 27	java/lang/RuntimeException:<init>	(Ljava/lang/String;)V
/* 176:    */     //   282: athrow
/* 177:    */     // Line number table:
/* 178:    */     //   Java source line #38	-> byte code offset #0
/* 179:    */     //   Java source line #39	-> byte code offset #6
/* 180:    */     //   Java source line #38	-> byte code offset #15
/* 181:    */     //   Java source line #40	-> byte code offset #18
/* 182:    */     //   Java source line #41	-> byte code offset #29
/* 183:    */     //   Java source line #42	-> byte code offset #37
/* 184:    */     //   Java source line #43	-> byte code offset #40
/* 185:    */     //   Java source line #44	-> byte code offset #50
/* 186:    */     //   Java source line #46	-> byte code offset #58
/* 187:    */     //   Java source line #47	-> byte code offset #65
/* 188:    */     //   Java source line #48	-> byte code offset #69
/* 189:    */     //   Java source line #38	-> byte code offset #142
/* 190:    */     //   Java source line #48	-> byte code offset #151
/* 191:    */     //   Java source line #38	-> byte code offset #192
/* 192:    */     //   Java source line #48	-> byte code offset #197
/* 193:    */     //   Java source line #49	-> byte code offset #237
/* 194:    */     //   Java source line #50	-> byte code offset #250
/* 195:    */     //   Java source line #51	-> byte code offset #251
/* 196:    */     // Local variable table:
/* 197:    */     //   start	length	slot	name	signature
/* 198:    */     //   0	283	0	paramLong	Long
/* 199:    */     //   3	225	1	localConnection	Connection
/* 200:    */     //   236	10	1	localSQLException	SQLException
/* 201:    */     //   250	1	1	localValidationException	NxtException.ValidationException
/* 202:    */     //   5	214	2	localObject1	Object
/* 203:    */     //   14	170	3	localPreparedStatement	PreparedStatement
/* 204:    */     //   192	4	3	localThrowable1	Throwable
/* 205:    */     //   16	158	4	localObject2	Object
/* 206:    */     //   35	24	5	localResultSet	ResultSet
/* 207:    */     //   142	7	5	localThrowable2	Throwable
/* 208:    */     //   38	28	6	localBlock1	Block
/* 209:    */     //   87	5	8	localThrowable3	Throwable
/* 210:    */     //   122	4	8	localThrowable4	Throwable
/* 211:    */     //   151	39	9	localObject3	Object
/* 212:    */     //   171	5	10	localThrowable5	Throwable
/* 213:    */     //   197	37	11	localObject4	Object
/* 214:    */     //   216	4	12	localThrowable6	Throwable
/* 215:    */     // Exception table:
/* 216:    */     //   from	to	target	type
/* 217:    */     //   78	84	87	java/lang/Throwable
/* 218:    */     //   113	119	122	java/lang/Throwable
/* 219:    */     //   18	69	142	java/lang/Throwable
/* 220:    */     //   18	69	151	finally
/* 221:    */     //   142	153	151	finally
/* 222:    */     //   162	168	171	java/lang/Throwable
/* 223:    */     //   6	105	192	java/lang/Throwable
/* 224:    */     //   142	192	192	java/lang/Throwable
/* 225:    */     //   6	105	197	finally
/* 226:    */     //   142	199	197	finally
/* 227:    */     //   207	213	216	java/lang/Throwable
/* 228:    */     //   0	139	236	java/sql/SQLException
/* 229:    */     //   142	236	236	java/sql/SQLException
/* 230:    */     //   0	139	250	nxt/NxtException$ValidationException
/* 231:    */     //   142	236	250	nxt/NxtException$ValidationException
/* 232:    */   }
/* 233:    */   
/* 234:    */   /* Error */
/* 235:    */   static boolean hasBlock(Long paramLong)
/* 236:    */   {
/* 237:    */     // Byte code:
/* 238:    */     //   0: invokestatic 2	nxt/Db:getConnection	()Ljava/sql/Connection;
/* 239:    */     //   3: astore_1
/* 240:    */     //   4: aconst_null
/* 241:    */     //   5: astore_2
/* 242:    */     //   6: aload_1
/* 243:    */     //   7: ldc 28
/* 244:    */     //   9: invokeinterface 4 2 0
/* 245:    */     //   14: astore_3
/* 246:    */     //   15: aconst_null
/* 247:    */     //   16: astore 4
/* 248:    */     //   18: aload_3
/* 249:    */     //   19: iconst_1
/* 250:    */     //   20: aload_0
/* 251:    */     //   21: invokevirtual 5	java/lang/Long:longValue	()J
/* 252:    */     //   24: invokeinterface 6 4 0
/* 253:    */     //   29: aload_3
/* 254:    */     //   30: invokeinterface 7 1 0
/* 255:    */     //   35: astore 5
/* 256:    */     //   37: aload 5
/* 257:    */     //   39: invokeinterface 8 1 0
/* 258:    */     //   44: istore 6
/* 259:    */     //   46: aload_3
/* 260:    */     //   47: ifnull +35 -> 82
/* 261:    */     //   50: aload 4
/* 262:    */     //   52: ifnull +24 -> 76
/* 263:    */     //   55: aload_3
/* 264:    */     //   56: invokeinterface 11 1 0
/* 265:    */     //   61: goto +21 -> 82
/* 266:    */     //   64: astore 7
/* 267:    */     //   66: aload 4
/* 268:    */     //   68: aload 7
/* 269:    */     //   70: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 270:    */     //   73: goto +9 -> 82
/* 271:    */     //   76: aload_3
/* 272:    */     //   77: invokeinterface 11 1 0
/* 273:    */     //   82: aload_1
/* 274:    */     //   83: ifnull +33 -> 116
/* 275:    */     //   86: aload_2
/* 276:    */     //   87: ifnull +23 -> 110
/* 277:    */     //   90: aload_1
/* 278:    */     //   91: invokeinterface 14 1 0
/* 279:    */     //   96: goto +20 -> 116
/* 280:    */     //   99: astore 7
/* 281:    */     //   101: aload_2
/* 282:    */     //   102: aload 7
/* 283:    */     //   104: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 284:    */     //   107: goto +9 -> 116
/* 285:    */     //   110: aload_1
/* 286:    */     //   111: invokeinterface 14 1 0
/* 287:    */     //   116: iload 6
/* 288:    */     //   118: ireturn
/* 289:    */     //   119: astore 5
/* 290:    */     //   121: aload 5
/* 291:    */     //   123: astore 4
/* 292:    */     //   125: aload 5
/* 293:    */     //   127: athrow
/* 294:    */     //   128: astore 8
/* 295:    */     //   130: aload_3
/* 296:    */     //   131: ifnull +35 -> 166
/* 297:    */     //   134: aload 4
/* 298:    */     //   136: ifnull +24 -> 160
/* 299:    */     //   139: aload_3
/* 300:    */     //   140: invokeinterface 11 1 0
/* 301:    */     //   145: goto +21 -> 166
/* 302:    */     //   148: astore 9
/* 303:    */     //   150: aload 4
/* 304:    */     //   152: aload 9
/* 305:    */     //   154: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 306:    */     //   157: goto +9 -> 166
/* 307:    */     //   160: aload_3
/* 308:    */     //   161: invokeinterface 11 1 0
/* 309:    */     //   166: aload 8
/* 310:    */     //   168: athrow
/* 311:    */     //   169: astore_3
/* 312:    */     //   170: aload_3
/* 313:    */     //   171: astore_2
/* 314:    */     //   172: aload_3
/* 315:    */     //   173: athrow
/* 316:    */     //   174: astore 10
/* 317:    */     //   176: aload_1
/* 318:    */     //   177: ifnull +33 -> 210
/* 319:    */     //   180: aload_2
/* 320:    */     //   181: ifnull +23 -> 204
/* 321:    */     //   184: aload_1
/* 322:    */     //   185: invokeinterface 14 1 0
/* 323:    */     //   190: goto +20 -> 210
/* 324:    */     //   193: astore 11
/* 325:    */     //   195: aload_2
/* 326:    */     //   196: aload 11
/* 327:    */     //   198: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 328:    */     //   201: goto +9 -> 210
/* 329:    */     //   204: aload_1
/* 330:    */     //   205: invokeinterface 14 1 0
/* 331:    */     //   210: aload 10
/* 332:    */     //   212: athrow
/* 333:    */     //   213: astore_1
/* 334:    */     //   214: new 16	java/lang/RuntimeException
/* 335:    */     //   217: dup
/* 336:    */     //   218: aload_1
/* 337:    */     //   219: invokevirtual 17	java/sql/SQLException:getMessage	()Ljava/lang/String;
/* 338:    */     //   222: aload_1
/* 339:    */     //   223: invokespecial 18	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/* 340:    */     //   226: athrow
/* 341:    */     // Line number table:
/* 342:    */     //   Java source line #56	-> byte code offset #0
/* 343:    */     //   Java source line #57	-> byte code offset #6
/* 344:    */     //   Java source line #56	-> byte code offset #15
/* 345:    */     //   Java source line #58	-> byte code offset #18
/* 346:    */     //   Java source line #59	-> byte code offset #29
/* 347:    */     //   Java source line #60	-> byte code offset #37
/* 348:    */     //   Java source line #61	-> byte code offset #46
/* 349:    */     //   Java source line #56	-> byte code offset #119
/* 350:    */     //   Java source line #61	-> byte code offset #128
/* 351:    */     //   Java source line #56	-> byte code offset #169
/* 352:    */     //   Java source line #61	-> byte code offset #174
/* 353:    */     //   Java source line #62	-> byte code offset #214
/* 354:    */     // Local variable table:
/* 355:    */     //   start	length	slot	name	signature
/* 356:    */     //   0	227	0	paramLong	Long
/* 357:    */     //   3	202	1	localConnection	Connection
/* 358:    */     //   213	10	1	localSQLException	SQLException
/* 359:    */     //   5	191	2	localObject1	Object
/* 360:    */     //   14	147	3	localPreparedStatement	PreparedStatement
/* 361:    */     //   169	4	3	localThrowable1	Throwable
/* 362:    */     //   16	135	4	localObject2	Object
/* 363:    */     //   35	3	5	localResultSet	ResultSet
/* 364:    */     //   119	7	5	localThrowable2	Throwable
/* 365:    */     //   64	5	7	localThrowable3	Throwable
/* 366:    */     //   99	4	7	localThrowable4	Throwable
/* 367:    */     //   128	39	8	localObject3	Object
/* 368:    */     //   148	5	9	localThrowable5	Throwable
/* 369:    */     //   174	37	10	localObject4	Object
/* 370:    */     //   193	4	11	localThrowable6	Throwable
/* 371:    */     // Exception table:
/* 372:    */     //   from	to	target	type
/* 373:    */     //   55	61	64	java/lang/Throwable
/* 374:    */     //   90	96	99	java/lang/Throwable
/* 375:    */     //   18	46	119	java/lang/Throwable
/* 376:    */     //   18	46	128	finally
/* 377:    */     //   119	130	128	finally
/* 378:    */     //   139	145	148	java/lang/Throwable
/* 379:    */     //   6	82	169	java/lang/Throwable
/* 380:    */     //   119	169	169	java/lang/Throwable
/* 381:    */     //   6	82	174	finally
/* 382:    */     //   119	176	174	finally
/* 383:    */     //   184	190	193	java/lang/Throwable
/* 384:    */     //   0	116	213	java/sql/SQLException
/* 385:    */     //   119	213	213	java/sql/SQLException
/* 386:    */   }
/* 387:    */   
/* 388:    */   /* Error */
/* 389:    */   static long findBlockIdAtHeight(int paramInt)
/* 390:    */   {
/* 391:    */     // Byte code:
/* 392:    */     //   0: invokestatic 2	nxt/Db:getConnection	()Ljava/sql/Connection;
/* 393:    */     //   3: astore_1
/* 394:    */     //   4: aconst_null
/* 395:    */     //   5: astore_2
/* 396:    */     //   6: aload_1
/* 397:    */     //   7: ldc 29
/* 398:    */     //   9: invokeinterface 4 2 0
/* 399:    */     //   14: astore_3
/* 400:    */     //   15: aconst_null
/* 401:    */     //   16: astore 4
/* 402:    */     //   18: aload_3
/* 403:    */     //   19: iconst_1
/* 404:    */     //   20: iload_0
/* 405:    */     //   21: invokeinterface 30 3 0
/* 406:    */     //   26: aload_3
/* 407:    */     //   27: invokeinterface 7 1 0
/* 408:    */     //   32: astore 5
/* 409:    */     //   34: aload 5
/* 410:    */     //   36: invokeinterface 8 1 0
/* 411:    */     //   41: ifne +42 -> 83
/* 412:    */     //   44: aload 5
/* 413:    */     //   46: invokeinterface 10 1 0
/* 414:    */     //   51: new 16	java/lang/RuntimeException
/* 415:    */     //   54: dup
/* 416:    */     //   55: new 20	java/lang/StringBuilder
/* 417:    */     //   58: dup
/* 418:    */     //   59: invokespecial 21	java/lang/StringBuilder:<init>	()V
/* 419:    */     //   62: ldc 31
/* 420:    */     //   64: invokevirtual 23	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
/* 421:    */     //   67: iload_0
/* 422:    */     //   68: invokevirtual 32	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
/* 423:    */     //   71: ldc 33
/* 424:    */     //   73: invokevirtual 23	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
/* 425:    */     //   76: invokevirtual 26	java/lang/StringBuilder:toString	()Ljava/lang/String;
/* 426:    */     //   79: invokespecial 27	java/lang/RuntimeException:<init>	(Ljava/lang/String;)V
/* 427:    */     //   82: athrow
/* 428:    */     //   83: aload 5
/* 429:    */     //   85: ldc 34
/* 430:    */     //   87: invokeinterface 35 2 0
/* 431:    */     //   92: lstore 6
/* 432:    */     //   94: aload 5
/* 433:    */     //   96: invokeinterface 10 1 0
/* 434:    */     //   101: lload 6
/* 435:    */     //   103: lstore 8
/* 436:    */     //   105: aload_3
/* 437:    */     //   106: ifnull +35 -> 141
/* 438:    */     //   109: aload 4
/* 439:    */     //   111: ifnull +24 -> 135
/* 440:    */     //   114: aload_3
/* 441:    */     //   115: invokeinterface 11 1 0
/* 442:    */     //   120: goto +21 -> 141
/* 443:    */     //   123: astore 10
/* 444:    */     //   125: aload 4
/* 445:    */     //   127: aload 10
/* 446:    */     //   129: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 447:    */     //   132: goto +9 -> 141
/* 448:    */     //   135: aload_3
/* 449:    */     //   136: invokeinterface 11 1 0
/* 450:    */     //   141: aload_1
/* 451:    */     //   142: ifnull +33 -> 175
/* 452:    */     //   145: aload_2
/* 453:    */     //   146: ifnull +23 -> 169
/* 454:    */     //   149: aload_1
/* 455:    */     //   150: invokeinterface 14 1 0
/* 456:    */     //   155: goto +20 -> 175
/* 457:    */     //   158: astore 10
/* 458:    */     //   160: aload_2
/* 459:    */     //   161: aload 10
/* 460:    */     //   163: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 461:    */     //   166: goto +9 -> 175
/* 462:    */     //   169: aload_1
/* 463:    */     //   170: invokeinterface 14 1 0
/* 464:    */     //   175: lload 8
/* 465:    */     //   177: lreturn
/* 466:    */     //   178: astore 5
/* 467:    */     //   180: aload 5
/* 468:    */     //   182: astore 4
/* 469:    */     //   184: aload 5
/* 470:    */     //   186: athrow
/* 471:    */     //   187: astore 11
/* 472:    */     //   189: aload_3
/* 473:    */     //   190: ifnull +35 -> 225
/* 474:    */     //   193: aload 4
/* 475:    */     //   195: ifnull +24 -> 219
/* 476:    */     //   198: aload_3
/* 477:    */     //   199: invokeinterface 11 1 0
/* 478:    */     //   204: goto +21 -> 225
/* 479:    */     //   207: astore 12
/* 480:    */     //   209: aload 4
/* 481:    */     //   211: aload 12
/* 482:    */     //   213: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 483:    */     //   216: goto +9 -> 225
/* 484:    */     //   219: aload_3
/* 485:    */     //   220: invokeinterface 11 1 0
/* 486:    */     //   225: aload 11
/* 487:    */     //   227: athrow
/* 488:    */     //   228: astore_3
/* 489:    */     //   229: aload_3
/* 490:    */     //   230: astore_2
/* 491:    */     //   231: aload_3
/* 492:    */     //   232: athrow
/* 493:    */     //   233: astore 13
/* 494:    */     //   235: aload_1
/* 495:    */     //   236: ifnull +33 -> 269
/* 496:    */     //   239: aload_2
/* 497:    */     //   240: ifnull +23 -> 263
/* 498:    */     //   243: aload_1
/* 499:    */     //   244: invokeinterface 14 1 0
/* 500:    */     //   249: goto +20 -> 269
/* 501:    */     //   252: astore 14
/* 502:    */     //   254: aload_2
/* 503:    */     //   255: aload 14
/* 504:    */     //   257: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/* 505:    */     //   260: goto +9 -> 269
/* 506:    */     //   263: aload_1
/* 507:    */     //   264: invokeinterface 14 1 0
/* 508:    */     //   269: aload 13
/* 509:    */     //   271: athrow
/* 510:    */     //   272: astore_1
/* 511:    */     //   273: new 16	java/lang/RuntimeException
/* 512:    */     //   276: dup
/* 513:    */     //   277: aload_1
/* 514:    */     //   278: invokevirtual 17	java/sql/SQLException:getMessage	()Ljava/lang/String;
/* 515:    */     //   281: aload_1
/* 516:    */     //   282: invokespecial 18	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/* 517:    */     //   285: athrow
/* 518:    */     // Line number table:
/* 519:    */     //   Java source line #67	-> byte code offset #0
/* 520:    */     //   Java source line #68	-> byte code offset #6
/* 521:    */     //   Java source line #67	-> byte code offset #15
/* 522:    */     //   Java source line #69	-> byte code offset #18
/* 523:    */     //   Java source line #70	-> byte code offset #26
/* 524:    */     //   Java source line #71	-> byte code offset #34
/* 525:    */     //   Java source line #72	-> byte code offset #44
/* 526:    */     //   Java source line #73	-> byte code offset #51
/* 527:    */     //   Java source line #75	-> byte code offset #83
/* 528:    */     //   Java source line #76	-> byte code offset #94
/* 529:    */     //   Java source line #77	-> byte code offset #101
/* 530:    */     //   Java source line #78	-> byte code offset #105
/* 531:    */     //   Java source line #67	-> byte code offset #178
/* 532:    */     //   Java source line #78	-> byte code offset #187
/* 533:    */     //   Java source line #67	-> byte code offset #228
/* 534:    */     //   Java source line #78	-> byte code offset #233
/* 535:    */     //   Java source line #79	-> byte code offset #273
/* 536:    */     // Local variable table:
/* 537:    */     //   start	length	slot	name	signature
/* 538:    */     //   0	286	0	paramInt	int
/* 539:    */     //   3	261	1	localConnection	Connection
/* 540:    */     //   272	10	1	localSQLException	SQLException
/* 541:    */     //   5	250	2	localObject1	Object
/* 542:    */     //   14	206	3	localPreparedStatement	PreparedStatement
/* 543:    */     //   228	4	3	localThrowable1	Throwable
/* 544:    */     //   16	194	4	localObject2	Object
/* 545:    */     //   32	63	5	localResultSet	ResultSet
/* 546:    */     //   178	7	5	localThrowable2	Throwable
/* 547:    */     //   92	10	6	l1	long
/* 548:    */     //   123	5	10	localThrowable3	Throwable
/* 549:    */     //   158	4	10	localThrowable4	Throwable
/* 550:    */     //   187	39	11	localObject3	Object
/* 551:    */     //   207	5	12	localThrowable5	Throwable
/* 552:    */     //   233	37	13	localObject4	Object
/* 553:    */     //   252	4	14	localThrowable6	Throwable
/* 554:    */     // Exception table:
/* 555:    */     //   from	to	target	type
/* 556:    */     //   114	120	123	java/lang/Throwable
/* 557:    */     //   149	155	158	java/lang/Throwable
/* 558:    */     //   18	105	178	java/lang/Throwable
/* 559:    */     //   18	105	187	finally
/* 560:    */     //   178	189	187	finally
/* 561:    */     //   198	204	207	java/lang/Throwable
/* 562:    */     //   6	141	228	java/lang/Throwable
/* 563:    */     //   178	228	228	java/lang/Throwable
/* 564:    */     //   6	141	233	finally
/* 565:    */     //   178	235	233	finally
/* 566:    */     //   243	249	252	java/lang/Throwable
/* 567:    */     //   0	175	272	java/sql/SQLException
/* 568:    */     //   178	272	272	java/sql/SQLException
/* 569:    */   }
/* 570:    */   
/* 571:    */   static Block getBlock(JSONObject paramJSONObject)
/* 572:    */     throws NxtException.ValidationException
/* 573:    */   {
/* 574:    */     try
/* 575:    */     {
/* 576: 87 */       int i = ((Long)paramJSONObject.get("version")).intValue();
/* 577: 88 */       int j = ((Long)paramJSONObject.get("timestamp")).intValue();
/* 578: 89 */       Long localLong = Convert.parseUnsignedLong((String)paramJSONObject.get("previousBlock"));
/* 579: 90 */       int k = ((Long)paramJSONObject.get("numberOfTransactions")).intValue();
/* 580: 91 */       int m = ((Long)paramJSONObject.get("totalAmount")).intValue();
/* 581: 92 */       int n = ((Long)paramJSONObject.get("totalFee")).intValue();
/* 582: 93 */       int i1 = ((Long)paramJSONObject.get("payloadLength")).intValue();
/* 583: 94 */       byte[] arrayOfByte1 = Convert.convert((String)paramJSONObject.get("payloadHash"));
/* 584: 95 */       byte[] arrayOfByte2 = Convert.convert((String)paramJSONObject.get("generatorPublicKey"));
/* 585: 96 */       byte[] arrayOfByte3 = Convert.convert((String)paramJSONObject.get("generationSignature"));
/* 586: 97 */       byte[] arrayOfByte4 = Convert.convert((String)paramJSONObject.get("blockSignature"));
/* 587: 98 */       byte[] arrayOfByte5 = i == 1 ? null : Convert.convert((String)paramJSONObject.get("previousBlockHash"));
/* 588: 99 */       if ((k > 255) || (i1 > 32640)) {
/* 589:100 */         throw new NxtException.ValidationException("Invalid number of transactions or payload length");
/* 590:    */       }
/* 591:103 */       return new Block(i, j, localLong, k, m, n, i1, arrayOfByte1, arrayOfByte2, arrayOfByte3, arrayOfByte4, arrayOfByte5);
/* 592:    */     }
/* 593:    */     catch (RuntimeException localRuntimeException)
/* 594:    */     {
/* 595:107 */       throw new NxtException.ValidationException(localRuntimeException.toString(), localRuntimeException);
/* 596:    */     }
/* 597:    */   }
/* 598:    */   
/* 599:    */   static Block getBlock(Connection paramConnection, ResultSet paramResultSet)
/* 600:    */     throws NxtException.ValidationException
/* 601:    */   {
/* 602:    */     try
/* 603:    */     {
/* 604:114 */       int i = paramResultSet.getInt("version");
/* 605:115 */       int j = paramResultSet.getInt("timestamp");
/* 606:116 */       Long localLong1 = Long.valueOf(paramResultSet.getLong("previous_block_id"));
/* 607:117 */       if (paramResultSet.wasNull()) {
/* 608:118 */         localLong1 = null;
/* 609:    */       }
/* 610:120 */       int k = paramResultSet.getInt("total_amount");
/* 611:121 */       int m = paramResultSet.getInt("total_fee");
/* 612:122 */       int n = paramResultSet.getInt("payload_length");
/* 613:123 */       byte[] arrayOfByte1 = paramResultSet.getBytes("generator_public_key");
/* 614:124 */       byte[] arrayOfByte2 = paramResultSet.getBytes("previous_block_hash");
/* 615:125 */       BigInteger localBigInteger = new BigInteger(paramResultSet.getBytes("cumulative_difficulty"));
/* 616:126 */       long l = paramResultSet.getLong("base_target");
/* 617:127 */       Long localLong2 = Long.valueOf(paramResultSet.getLong("next_block_id"));
/* 618:128 */       if (paramResultSet.wasNull()) {
/* 619:129 */         localLong2 = null;
/* 620:    */       }
/* 621:131 */       int i1 = paramResultSet.getInt("index");
/* 622:132 */       int i2 = paramResultSet.getInt("height");
/* 623:133 */       byte[] arrayOfByte3 = paramResultSet.getBytes("generation_signature");
/* 624:134 */       byte[] arrayOfByte4 = paramResultSet.getBytes("block_signature");
/* 625:135 */       byte[] arrayOfByte5 = paramResultSet.getBytes("payload_hash");
/* 626:    */       
/* 627:137 */       Long localLong3 = Long.valueOf(paramResultSet.getLong("id"));
/* 628:138 */       List localList = Transaction.findBlockTransactions(paramConnection, localLong3);
/* 629:    */       
/* 630:140 */       Block localBlock = new Block(i, j, localLong1, localList.size(), k, m, n, arrayOfByte5, arrayOfByte1, arrayOfByte3, arrayOfByte4, arrayOfByte2);
/* 631:142 */       for (int i3 = 0; i3 < localList.size(); i3++)
/* 632:    */       {
/* 633:143 */         Transaction localTransaction = (Transaction)localList.get(i3);
/* 634:144 */         localBlock.transactionIds[i3] = localTransaction.getId();
/* 635:145 */         localBlock.blockTransactions[i3] = localTransaction;
/* 636:    */       }
/* 637:148 */       localBlock.cumulativeDifficulty = localBigInteger;
/* 638:149 */       localBlock.baseTarget = l;
/* 639:150 */       localBlock.nextBlockId = localLong2;
/* 640:151 */       localBlock.index = i1;
/* 641:152 */       localBlock.height = i2;
/* 642:153 */       localBlock.id = localLong3;
/* 643:    */       
/* 644:155 */       return localBlock;
/* 645:    */     }
/* 646:    */     catch (SQLException localSQLException)
/* 647:    */     {
/* 648:158 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/* 649:    */     }
/* 650:    */   }
/* 651:    */   
/* 652:    */   static void saveBlock(Connection paramConnection, Block paramBlock)
/* 653:    */   {
/* 654:    */     try
/* 655:    */     {
/* 656:164 */       PreparedStatement localPreparedStatement = paramConnection.prepareStatement("INSERT INTO block (id, version, timestamp, previous_block_id, total_amount, total_fee, payload_length, generator_public_key, previous_block_hash, cumulative_difficulty, base_target, next_block_id, index, height, generation_signature, block_signature, payload_hash, generator_account_id)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");Object localObject1 = null;
/* 657:    */       try
/* 658:    */       {
/* 659:168 */         localPreparedStatement.setLong(1, paramBlock.getId().longValue());
/* 660:169 */         localPreparedStatement.setInt(2, paramBlock.version);
/* 661:170 */         localPreparedStatement.setInt(3, paramBlock.timestamp);
/* 662:171 */         if (paramBlock.previousBlockId != null) {
/* 663:172 */           localPreparedStatement.setLong(4, paramBlock.previousBlockId.longValue());
/* 664:    */         } else {
/* 665:174 */           localPreparedStatement.setNull(4, -5);
/* 666:    */         }
/* 667:176 */         localPreparedStatement.setInt(5, paramBlock.totalAmount);
/* 668:177 */         localPreparedStatement.setInt(6, paramBlock.totalFee);
/* 669:178 */         localPreparedStatement.setInt(7, paramBlock.payloadLength);
/* 670:179 */         localPreparedStatement.setBytes(8, paramBlock.generatorPublicKey);
/* 671:180 */         localPreparedStatement.setBytes(9, paramBlock.previousBlockHash);
/* 672:181 */         localPreparedStatement.setBytes(10, paramBlock.cumulativeDifficulty.toByteArray());
/* 673:182 */         localPreparedStatement.setLong(11, paramBlock.baseTarget);
/* 674:183 */         if (paramBlock.nextBlockId != null) {
/* 675:184 */           localPreparedStatement.setLong(12, paramBlock.nextBlockId.longValue());
/* 676:    */         } else {
/* 677:186 */           localPreparedStatement.setNull(12, -5);
/* 678:    */         }
/* 679:188 */         localPreparedStatement.setInt(13, paramBlock.index);
/* 680:189 */         localPreparedStatement.setInt(14, paramBlock.height);
/* 681:190 */         localPreparedStatement.setBytes(15, paramBlock.generationSignature);
/* 682:191 */         localPreparedStatement.setBytes(16, paramBlock.blockSignature);
/* 683:192 */         localPreparedStatement.setBytes(17, paramBlock.payloadHash);
/* 684:193 */         localPreparedStatement.setLong(18, paramBlock.getGeneratorAccountId().longValue());
/* 685:194 */         localPreparedStatement.executeUpdate();
/* 686:195 */         Transaction.saveTransactions(paramConnection, paramBlock.blockTransactions);
/* 687:    */       }
/* 688:    */       catch (Throwable localThrowable2)
/* 689:    */       {
/* 690:164 */         localObject1 = localThrowable2;throw localThrowable2;
/* 691:    */       }
/* 692:    */       finally
/* 693:    */       {
/* 694:196 */         if (localPreparedStatement != null) {
/* 695:196 */           if (localObject1 != null) {
/* 696:    */             try
/* 697:    */             {
/* 698:196 */               localPreparedStatement.close();
/* 699:    */             }
/* 700:    */             catch (Throwable localThrowable5)
/* 701:    */             {
/* 702:196 */               localObject1.addSuppressed(localThrowable5);
/* 703:    */             }
/* 704:    */           } else {
/* 705:196 */             localPreparedStatement.close();
/* 706:    */           }
/* 707:    */         }
/* 708:    */       }
/* 709:197 */       if (paramBlock.previousBlockId != null)
/* 710:    */       {
/* 711:198 */         localPreparedStatement = paramConnection.prepareStatement("UPDATE block SET next_block_id = ? WHERE id = ?");localObject1 = null;
/* 712:    */         try
/* 713:    */         {
/* 714:199 */           localPreparedStatement.setLong(1, paramBlock.getId().longValue());
/* 715:200 */           localPreparedStatement.setLong(2, paramBlock.previousBlockId.longValue());
/* 716:201 */           localPreparedStatement.executeUpdate();
/* 717:    */         }
/* 718:    */         catch (Throwable localThrowable4)
/* 719:    */         {
/* 720:198 */           localObject1 = localThrowable4;throw localThrowable4;
/* 721:    */         }
/* 722:    */         finally
/* 723:    */         {
/* 724:202 */           if (localPreparedStatement != null) {
/* 725:202 */             if (localObject1 != null) {
/* 726:    */               try
/* 727:    */               {
/* 728:202 */                 localPreparedStatement.close();
/* 729:    */               }
/* 730:    */               catch (Throwable localThrowable6)
/* 731:    */               {
/* 732:202 */                 localObject1.addSuppressed(localThrowable6);
/* 733:    */               }
/* 734:    */             } else {
/* 735:202 */               localPreparedStatement.close();
/* 736:    */             }
/* 737:    */           }
/* 738:    */         }
/* 739:    */       }
/* 740:    */     }
/* 741:    */     catch (SQLException localSQLException)
/* 742:    */     {
/* 743:205 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/* 744:    */     }
/* 745:    */   }
/* 746:    */   
/* 747:    */   static void deleteBlock(Long paramLong)
/* 748:    */   {
/* 749:    */     try
/* 750:    */     {
/* 751:211 */       Connection localConnection = Db.getConnection();Object localObject1 = null;
/* 752:    */       try
/* 753:    */       {
/* 754:212 */         PreparedStatement localPreparedStatement = localConnection.prepareStatement("DELETE FROM block WHERE id = ?");Object localObject2 = null;
/* 755:    */         try
/* 756:    */         {
/* 757:    */           try
/* 758:    */           {
/* 759:214 */             localPreparedStatement.setLong(1, paramLong.longValue());
/* 760:215 */             localPreparedStatement.executeUpdate();
/* 761:216 */             localConnection.commit();
/* 762:    */           }
/* 763:    */           catch (SQLException localSQLException2)
/* 764:    */           {
/* 765:218 */             localConnection.rollback();
/* 766:219 */             throw localSQLException2;
/* 767:    */           }
/* 768:    */         }
/* 769:    */         catch (Throwable localThrowable4)
/* 770:    */         {
/* 771:211 */           localObject2 = localThrowable4;throw localThrowable4;
/* 772:    */         }
/* 773:    */         finally {}
/* 774:    */       }
/* 775:    */       catch (Throwable localThrowable2)
/* 776:    */       {
/* 777:211 */         localObject1 = localThrowable2;throw localThrowable2;
/* 778:    */       }
/* 779:    */       finally
/* 780:    */       {
/* 781:221 */         if (localConnection != null) {
/* 782:221 */           if (localObject1 != null) {
/* 783:    */             try
/* 784:    */             {
/* 785:221 */               localConnection.close();
/* 786:    */             }
/* 787:    */             catch (Throwable localThrowable6)
/* 788:    */             {
/* 789:221 */               localObject1.addSuppressed(localThrowable6);
/* 790:    */             }
/* 791:    */           } else {
/* 792:221 */             localConnection.close();
/* 793:    */           }
/* 794:    */         }
/* 795:    */       }
/* 796:    */     }
/* 797:    */     catch (SQLException localSQLException1)
/* 798:    */     {
/* 799:222 */       throw new RuntimeException(localSQLException1.toString(), localSQLException1);
/* 800:    */     }
/* 801:    */   }
/* 802:    */   
/* 803:237 */   private BigInteger cumulativeDifficulty = BigInteger.ZERO;
/* 804:238 */   private long baseTarget = 153722867L;
/* 805:    */   private volatile Long nextBlockId;
/* 806:    */   private int index;
/* 807:    */   private int height;
/* 808:    */   private byte[] generationSignature;
/* 809:    */   private byte[] blockSignature;
/* 810:    */   private byte[] payloadHash;
/* 811:    */   private volatile Long id;
/* 812:246 */   private volatile String stringId = null;
/* 813:    */   private volatile Long generatorAccountId;
/* 814:    */   private SoftReference<JSONStreamAware> jsonRef;
/* 815:    */   
/* 816:    */   Block(int paramInt1, int paramInt2, Long paramLong, int paramInt3, int paramInt4, int paramInt5, int paramInt6, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
/* 817:    */     throws NxtException.ValidationException
/* 818:    */   {
/* 819:254 */     this(paramInt1, paramInt2, paramLong, paramInt3, paramInt4, paramInt5, paramInt6, paramArrayOfByte1, paramArrayOfByte2, paramArrayOfByte3, paramArrayOfByte4, null);
/* 820:    */   }
/* 821:    */   
/* 822:    */   Block(int paramInt1, int paramInt2, Long paramLong, int paramInt3, int paramInt4, int paramInt5, int paramInt6, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4, byte[] paramArrayOfByte5)
/* 823:    */     throws NxtException.ValidationException
/* 824:    */   {
/* 825:263 */     if ((paramInt3 > 255) || (paramInt3 < 0)) {
/* 826:264 */       throw new NxtException.ValidationException("attempted to create a block with " + paramInt3 + " transactions");
/* 827:    */     }
/* 828:267 */     if ((paramInt6 > 32640) || (paramInt6 < 0)) {
/* 829:268 */       throw new NxtException.ValidationException("attempted to create a block with payloadLength " + paramInt6);
/* 830:    */     }
/* 831:271 */     this.version = paramInt1;
/* 832:272 */     this.timestamp = paramInt2;
/* 833:273 */     this.previousBlockId = paramLong;
/* 834:274 */     this.totalAmount = paramInt4;
/* 835:275 */     this.totalFee = paramInt5;
/* 836:276 */     this.payloadLength = paramInt6;
/* 837:277 */     this.payloadHash = paramArrayOfByte1;
/* 838:278 */     this.generatorPublicKey = paramArrayOfByte2;
/* 839:279 */     this.generationSignature = paramArrayOfByte3;
/* 840:280 */     this.blockSignature = paramArrayOfByte4;
/* 841:    */     
/* 842:282 */     this.previousBlockHash = paramArrayOfByte5;
/* 843:283 */     this.transactionIds = (paramInt3 == 0 ? emptyLong : new Long[paramInt3]);
/* 844:284 */     this.blockTransactions = (paramInt3 == 0 ? emptyTransactions : new Transaction[paramInt3]);
/* 845:    */   }
/* 846:    */   
/* 847:    */   public int getVersion()
/* 848:    */   {
/* 849:289 */     return this.version;
/* 850:    */   }
/* 851:    */   
/* 852:    */   public int getTimestamp()
/* 853:    */   {
/* 854:293 */     return this.timestamp;
/* 855:    */   }
/* 856:    */   
/* 857:    */   public Long getPreviousBlockId()
/* 858:    */   {
/* 859:297 */     return this.previousBlockId;
/* 860:    */   }
/* 861:    */   
/* 862:    */   public byte[] getGeneratorPublicKey()
/* 863:    */   {
/* 864:301 */     return this.generatorPublicKey;
/* 865:    */   }
/* 866:    */   
/* 867:    */   public byte[] getPreviousBlockHash()
/* 868:    */   {
/* 869:305 */     return this.previousBlockHash;
/* 870:    */   }
/* 871:    */   
/* 872:    */   public int getTotalAmount()
/* 873:    */   {
/* 874:309 */     return this.totalAmount;
/* 875:    */   }
/* 876:    */   
/* 877:    */   public int getTotalFee()
/* 878:    */   {
/* 879:313 */     return this.totalFee;
/* 880:    */   }
/* 881:    */   
/* 882:    */   public int getPayloadLength()
/* 883:    */   {
/* 884:317 */     return this.payloadLength;
/* 885:    */   }
/* 886:    */   
/* 887:    */   public Long[] getTransactionIds()
/* 888:    */   {
/* 889:321 */     return this.transactionIds;
/* 890:    */   }
/* 891:    */   
/* 892:    */   public byte[] getPayloadHash()
/* 893:    */   {
/* 894:325 */     return this.payloadHash;
/* 895:    */   }
/* 896:    */   
/* 897:    */   void setPayloadHash(byte[] paramArrayOfByte)
/* 898:    */   {
/* 899:329 */     this.payloadHash = paramArrayOfByte;
/* 900:    */   }
/* 901:    */   
/* 902:    */   public byte[] getGenerationSignature()
/* 903:    */   {
/* 904:333 */     return this.generationSignature;
/* 905:    */   }
/* 906:    */   
/* 907:    */   void setGenerationSignature(byte[] paramArrayOfByte)
/* 908:    */   {
/* 909:337 */     this.generationSignature = paramArrayOfByte;
/* 910:    */   }
/* 911:    */   
/* 912:    */   public byte[] getBlockSignature()
/* 913:    */   {
/* 914:341 */     return this.blockSignature;
/* 915:    */   }
/* 916:    */   
/* 917:    */   void setBlockSignature(byte[] paramArrayOfByte)
/* 918:    */   {
/* 919:345 */     this.blockSignature = paramArrayOfByte;
/* 920:    */   }
/* 921:    */   
/* 922:    */   public Transaction[] getTransactions()
/* 923:    */   {
/* 924:349 */     return this.blockTransactions;
/* 925:    */   }
/* 926:    */   
/* 927:    */   public long getBaseTarget()
/* 928:    */   {
/* 929:353 */     return this.baseTarget;
/* 930:    */   }
/* 931:    */   
/* 932:    */   public BigInteger getCumulativeDifficulty()
/* 933:    */   {
/* 934:357 */     return this.cumulativeDifficulty;
/* 935:    */   }
/* 936:    */   
/* 937:    */   public Long getNextBlockId()
/* 938:    */   {
/* 939:361 */     return this.nextBlockId;
/* 940:    */   }
/* 941:    */   
/* 942:    */   public int getIndex()
/* 943:    */   {
/* 944:365 */     return this.index;
/* 945:    */   }
/* 946:    */   
/* 947:    */   void setIndex(int paramInt)
/* 948:    */   {
/* 949:369 */     this.index = paramInt;
/* 950:    */   }
/* 951:    */   
/* 952:    */   public int getHeight()
/* 953:    */   {
/* 954:373 */     return this.height;
/* 955:    */   }
/* 956:    */   
/* 957:    */   void setHeight(int paramInt)
/* 958:    */   {
/* 959:377 */     this.height = paramInt;
/* 960:    */   }
/* 961:    */   
/* 962:    */   public Long getId()
/* 963:    */   {
/* 964:381 */     if (this.id == null)
/* 965:    */     {
/* 966:382 */       byte[] arrayOfByte = Crypto.sha256().digest(getBytes());
/* 967:383 */       BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
/* 968:384 */       this.id = Long.valueOf(localBigInteger.longValue());
/* 969:385 */       this.stringId = localBigInteger.toString();
/* 970:    */     }
/* 971:387 */     return this.id;
/* 972:    */   }
/* 973:    */   
/* 974:    */   public String getStringId()
/* 975:    */   {
/* 976:391 */     if (this.stringId == null)
/* 977:    */     {
/* 978:392 */       getId();
/* 979:393 */       if (this.stringId == null) {
/* 980:394 */         this.stringId = Convert.convert(this.id);
/* 981:    */       }
/* 982:    */     }
/* 983:397 */     return this.stringId;
/* 984:    */   }
/* 985:    */   
/* 986:    */   public Long getGeneratorAccountId()
/* 987:    */   {
/* 988:401 */     if (this.generatorAccountId == null) {
/* 989:402 */       this.generatorAccountId = Account.getId(this.generatorPublicKey);
/* 990:    */     }
/* 991:404 */     return this.generatorAccountId;
/* 992:    */   }
/* 993:    */   
/* 994:    */   public synchronized JSONStreamAware getJSON()
/* 995:    */   {
/* 996:409 */     if (this.jsonRef != null)
/* 997:    */     {
/* 998:410 */       localJSONStreamAware = (JSONStreamAware)this.jsonRef.get();
/* 999:411 */       if (localJSONStreamAware != null) {
/* :00:412 */         return localJSONStreamAware;
/* :01:    */       }
/* :02:    */     }
/* :03:415 */     JSONStreamAware localJSONStreamAware = JSON.prepare(getJSONObject());
/* :04:416 */     this.jsonRef = new SoftReference(localJSONStreamAware);
/* :05:417 */     return localJSONStreamAware;
/* :06:    */   }
/* :07:    */   
/* :08:    */   public boolean equals(Object paramObject)
/* :09:    */   {
/* :10:422 */     return ((paramObject instanceof Block)) && (getId().equals(((Block)paramObject).getId()));
/* :11:    */   }
/* :12:    */   
/* :13:    */   public int hashCode()
/* :14:    */   {
/* :15:427 */     return getId().hashCode();
/* :16:    */   }
/* :17:    */   
/* :18:    */   byte[] getBytes()
/* :19:    */   {
/* :20:432 */     ByteBuffer localByteBuffer = ByteBuffer.allocate(224);
/* :21:433 */     localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/* :22:434 */     localByteBuffer.putInt(this.version);
/* :23:435 */     localByteBuffer.putInt(this.timestamp);
/* :24:436 */     localByteBuffer.putLong(Convert.nullToZero(this.previousBlockId));
/* :25:437 */     localByteBuffer.putInt(this.transactionIds.length);
/* :26:438 */     localByteBuffer.putInt(this.totalAmount);
/* :27:439 */     localByteBuffer.putInt(this.totalFee);
/* :28:440 */     localByteBuffer.putInt(this.payloadLength);
/* :29:441 */     localByteBuffer.put(this.payloadHash);
/* :30:442 */     localByteBuffer.put(this.generatorPublicKey);
/* :31:443 */     localByteBuffer.put(this.generationSignature);
/* :32:444 */     if (this.version > 1) {
/* :33:445 */       localByteBuffer.put(this.previousBlockHash);
/* :34:    */     }
/* :35:447 */     localByteBuffer.put(this.blockSignature);
/* :36:448 */     return localByteBuffer.array();
/* :37:    */   }
/* :38:    */   
/* :39:    */   JSONObject getJSONObject()
/* :40:    */   {
/* :41:453 */     JSONObject localJSONObject = new JSONObject();
/* :42:    */     
/* :43:455 */     localJSONObject.put("version", Integer.valueOf(this.version));
/* :44:456 */     localJSONObject.put("timestamp", Integer.valueOf(this.timestamp));
/* :45:457 */     localJSONObject.put("previousBlock", Convert.convert(this.previousBlockId));
/* :46:458 */     localJSONObject.put("numberOfTransactions", Integer.valueOf(this.transactionIds.length));
/* :47:459 */     localJSONObject.put("totalAmount", Integer.valueOf(this.totalAmount));
/* :48:460 */     localJSONObject.put("totalFee", Integer.valueOf(this.totalFee));
/* :49:461 */     localJSONObject.put("payloadLength", Integer.valueOf(this.payloadLength));
/* :50:462 */     localJSONObject.put("payloadHash", Convert.convert(this.payloadHash));
/* :51:463 */     localJSONObject.put("generatorPublicKey", Convert.convert(this.generatorPublicKey));
/* :52:464 */     localJSONObject.put("generationSignature", Convert.convert(this.generationSignature));
/* :53:465 */     if (this.version > 1) {
/* :54:467 */       localJSONObject.put("previousBlockHash", Convert.convert(this.previousBlockHash));
/* :55:    */     }
/* :56:470 */     localJSONObject.put("blockSignature", Convert.convert(this.blockSignature));
/* :57:    */     
/* :58:472 */     JSONArray localJSONArray = new JSONArray();
/* :59:473 */     for (Transaction localTransaction : this.blockTransactions) {
/* :60:475 */       localJSONArray.add(localTransaction.getJSONObject());
/* :61:    */     }
/* :62:478 */     localJSONObject.put("transactions", localJSONArray);
/* :63:    */     
/* :64:480 */     return localJSONObject;
/* :65:    */   }
/* :66:    */   
/* :67:    */   boolean verifyBlockSignature()
/* :68:    */   {
/* :69:486 */     Account localAccount = Account.getAccount(getGeneratorAccountId());
/* :70:487 */     if (localAccount == null) {
/* :71:489 */       return false;
/* :72:    */     }
/* :73:493 */     byte[] arrayOfByte1 = getBytes();
/* :74:494 */     byte[] arrayOfByte2 = new byte[arrayOfByte1.length - 64];
/* :75:495 */     System.arraycopy(arrayOfByte1, 0, arrayOfByte2, 0, arrayOfByte2.length);
/* :76:    */     
/* :77:497 */     return (Crypto.verify(this.blockSignature, arrayOfByte2, this.generatorPublicKey)) && (localAccount.setOrVerify(this.generatorPublicKey));
/* :78:    */   }
/* :79:    */   
/* :80:    */   boolean verifyGenerationSignature()
/* :81:    */   {
/* :82:    */     try
/* :83:    */     {
/* :84:505 */       Block localBlock = Blockchain.getBlock(this.previousBlockId);
/* :85:506 */       if (localBlock == null) {
/* :86:508 */         return false;
/* :87:    */       }
/* :88:512 */       if ((this.version == 1) && (!Crypto.verify(this.generationSignature, localBlock.generationSignature, this.generatorPublicKey))) {
/* :89:514 */         return false;
/* :90:    */       }
/* :91:518 */       Account localAccount = Account.getAccount(getGeneratorAccountId());
/* :92:519 */       if ((localAccount == null) || (localAccount.getEffectiveBalance() <= 0)) {
/* :93:521 */         return false;
/* :94:    */       }
/* :95:525 */       int i = this.timestamp - localBlock.timestamp;
/* :96:526 */       BigInteger localBigInteger1 = BigInteger.valueOf(Blockchain.getLastBlock().baseTarget).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance())).multiply(BigInteger.valueOf(i));
/* :97:    */       
/* :98:528 */       MessageDigest localMessageDigest = Crypto.sha256();
/* :99:    */       byte[] arrayOfByte;
/* ;00:530 */       if (this.version == 1)
/* ;01:    */       {
/* ;02:532 */         arrayOfByte = localMessageDigest.digest(this.generationSignature);
/* ;03:    */       }
/* ;04:    */       else
/* ;05:    */       {
/* ;06:536 */         localMessageDigest.update(localBlock.generationSignature);
/* ;07:537 */         arrayOfByte = localMessageDigest.digest(this.generatorPublicKey);
/* ;08:538 */         if (!Arrays.equals(this.generationSignature, arrayOfByte)) {
/* ;09:540 */           return false;
/* ;10:    */         }
/* ;11:    */       }
/* ;12:546 */       BigInteger localBigInteger2 = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
/* ;13:    */       
/* ;14:548 */       return localBigInteger2.compareTo(localBigInteger1) < 0;
/* ;15:    */     }
/* ;16:    */     catch (RuntimeException localRuntimeException)
/* ;17:    */     {
/* ;18:552 */       Logger.logMessage("Error verifying block generation signature", localRuntimeException);
/* ;19:    */     }
/* ;20:553 */     return false;
/* ;21:    */   }
/* ;22:    */   
/* ;23:    */   void apply()
/* ;24:    */   {
/* ;25:561 */     Account localAccount = Account.addOrGetAccount(getGeneratorAccountId());
/* ;26:562 */     if (!localAccount.setOrVerify(this.generatorPublicKey)) {
/* ;27:563 */       throw new IllegalStateException("Generator public key mismatch");
/* ;28:    */     }
/* ;29:565 */     localAccount.addToBalanceAndUnconfirmedBalance(this.totalFee * 100L);
/* ;30:567 */     for (Transaction localTransaction : this.blockTransactions) {
/* ;31:568 */       localTransaction.apply();
/* ;32:    */     }
/* ;33:571 */     Blockchain.purgeExpiredHashes(this.timestamp);
/* ;34:    */   }
/* ;35:    */   
/* ;36:    */   void calculateBaseTarget()
/* ;37:    */   {
/* ;38:577 */     if ((getId().equals(Genesis.GENESIS_BLOCK_ID)) && (this.previousBlockId == null))
/* ;39:    */     {
/* ;40:578 */       this.baseTarget = 153722867L;
/* ;41:579 */       this.cumulativeDifficulty = BigInteger.ZERO;
/* ;42:    */     }
/* ;43:    */     else
/* ;44:    */     {
/* ;45:581 */       Block localBlock = Blockchain.getBlock(this.previousBlockId);
/* ;46:582 */       long l1 = localBlock.baseTarget;
/* ;47:583 */       long l2 = BigInteger.valueOf(l1).multiply(BigInteger.valueOf(this.timestamp - localBlock.timestamp)).divide(BigInteger.valueOf(60L)).longValue();
/* ;48:586 */       if ((l2 < 0L) || (l2 > 153722867000000000L)) {
/* ;49:587 */         l2 = 153722867000000000L;
/* ;50:    */       }
/* ;51:589 */       if (l2 < l1 / 2L) {
/* ;52:590 */         l2 = l1 / 2L;
/* ;53:    */       }
/* ;54:592 */       if (l2 == 0L) {
/* ;55:593 */         l2 = 1L;
/* ;56:    */       }
/* ;57:595 */       long l3 = l1 * 2L;
/* ;58:596 */       if (l3 < 0L) {
/* ;59:597 */         l3 = 153722867000000000L;
/* ;60:    */       }
/* ;61:599 */       if (l2 > l3) {
/* ;62:600 */         l2 = l3;
/* ;63:    */       }
/* ;64:602 */       this.baseTarget = l2;
/* ;65:603 */       this.cumulativeDifficulty = localBlock.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(this.baseTarget)));
/* ;66:    */     }
/* ;67:    */   }
/* ;68:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Block
 * JD-Core Version:    0.7.0.1
 */