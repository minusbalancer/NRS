/*    1:     */ package nxt;
/*    2:     */ 
/*    3:     */ import java.io.UnsupportedEncodingException;
/*    4:     */ import java.math.BigInteger;
/*    5:     */ import java.nio.ByteBuffer;
/*    6:     */ import java.nio.ByteOrder;
/*    7:     */ import java.security.MessageDigest;
/*    8:     */ import java.sql.Connection;
/*    9:     */ import java.sql.PreparedStatement;
/*   10:     */ import java.sql.ResultSet;
/*   11:     */ import java.sql.SQLException;
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
/*   25:     */   implements Comparable<Transaction>
/*   26:     */ {
/*   27:     */   private static final byte TYPE_PAYMENT = 0;
/*   28:     */   private static final byte TYPE_MESSAGING = 1;
/*   29:     */   private static final byte TYPE_COLORED_COINS = 2;
/*   30:     */   private static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;
/*   31:     */   private static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
/*   32:     */   private static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
/*   33:     */   private static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
/*   34:     */   private static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
/*   35:     */   private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
/*   36:     */   private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
/*   37:     */   private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
/*   38:     */   private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;
/*   39:  44 */   public static final Comparator<Transaction> timestampComparator = new Comparator()
/*   40:     */   {
/*   41:     */     public int compare(Transaction paramAnonymousTransaction1, Transaction paramAnonymousTransaction2)
/*   42:     */     {
/*   43:  47 */       return paramAnonymousTransaction1.timestamp > paramAnonymousTransaction2.timestamp ? 1 : paramAnonymousTransaction1.timestamp < paramAnonymousTransaction2.timestamp ? -1 : 0;
/*   44:     */     }
/*   45:     */   };
/*   46:     */   private final short deadline;
/*   47:     */   private final byte[] senderPublicKey;
/*   48:     */   private final Long recipientId;
/*   49:     */   private final int amount;
/*   50:     */   private final int fee;
/*   51:     */   private final Long referencedTransactionId;
/*   52:     */   private int index;
/*   53:     */   private int height;
/*   54:     */   private Long blockId;
/*   55:     */   private volatile Block block;
/*   56:     */   private byte[] signature;
/*   57:     */   private int timestamp;
/*   58:     */   private final Type type;
/*   59:     */   private Attachment attachment;
/*   60:     */   private volatile Long id;
/*   61:     */   
/*   62:     */   public static Transaction getTransaction(byte[] paramArrayOfByte)
/*   63:     */     throws NxtException.ValidationException
/*   64:     */   {
/*   65:     */     try
/*   66:     */     {
/*   67:  54 */       ByteBuffer localByteBuffer = ByteBuffer.wrap(paramArrayOfByte);
/*   68:  55 */       localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/*   69:     */       
/*   70:  57 */       byte b1 = localByteBuffer.get();
/*   71:  58 */       byte b2 = localByteBuffer.get();
/*   72:  59 */       int i = localByteBuffer.getInt();
/*   73:  60 */       short s = localByteBuffer.getShort();
/*   74:  61 */       byte[] arrayOfByte1 = new byte[32];
/*   75:  62 */       localByteBuffer.get(arrayOfByte1);
/*   76:  63 */       Long localLong1 = Long.valueOf(localByteBuffer.getLong());
/*   77:  64 */       int j = localByteBuffer.getInt();
/*   78:  65 */       int k = localByteBuffer.getInt();
/*   79:  66 */       Long localLong2 = Convert.zeroToNull(localByteBuffer.getLong());
/*   80:  67 */       byte[] arrayOfByte2 = new byte[64];
/*   81:  68 */       localByteBuffer.get(arrayOfByte2);
/*   82:     */       
/*   83:  70 */       Type localType = findTransactionType(b1, b2);
/*   84:  71 */       Transaction localTransaction = new Transaction(localType, i, s, arrayOfByte1, localLong1, j, k, localLong2, arrayOfByte2);
/*   85:  74 */       if (!localType.loadAttachment(localTransaction, localByteBuffer)) {
/*   86:  75 */         throw new NxtException.ValidationException("Invalid transaction attachment:\n" + localTransaction.attachment.getJSON());
/*   87:     */       }
/*   88:  78 */       return localTransaction;
/*   89:     */     }
/*   90:     */     catch (RuntimeException localRuntimeException)
/*   91:     */     {
/*   92:  81 */       throw new NxtException.ValidationException(localRuntimeException.toString());
/*   93:     */     }
/*   94:     */   }
/*   95:     */   
/*   96:     */   public static Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2)
/*   97:     */     throws NxtException.ValidationException
/*   98:     */   {
/*   99:  87 */     return new Transaction(Transaction.Type.Payment.ORDINARY, paramInt1, paramShort, paramArrayOfByte, paramLong1, paramInt2, paramInt3, paramLong2, null);
/*  100:     */   }
/*  101:     */   
/*  102:     */   public static Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, Attachment paramAttachment)
/*  103:     */     throws NxtException.ValidationException
/*  104:     */   {
/*  105:  93 */     Transaction localTransaction = new Transaction(paramAttachment.getTransactionType(), paramInt1, paramShort, paramArrayOfByte, paramLong1, paramInt2, paramInt3, paramLong2, null);
/*  106:     */     
/*  107:  95 */     localTransaction.attachment = paramAttachment;
/*  108:  96 */     return localTransaction;
/*  109:     */   }
/*  110:     */   
/*  111:     */   static Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte1, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, byte[] paramArrayOfByte2)
/*  112:     */     throws NxtException.ValidationException
/*  113:     */   {
/*  114: 101 */     return new Transaction(Transaction.Type.Payment.ORDINARY, paramInt1, paramShort, paramArrayOfByte1, paramLong1, paramInt2, paramInt3, paramLong2, paramArrayOfByte2);
/*  115:     */   }
/*  116:     */   
/*  117:     */   /* Error */
/*  118:     */   static Transaction findTransaction(Long paramLong)
/*  119:     */   {
/*  120:     */     // Byte code:
/*  121:     */     //   0: invokestatic 34	nxt/Db:getConnection	()Ljava/sql/Connection;
/*  122:     */     //   3: astore_1
/*  123:     */     //   4: aconst_null
/*  124:     */     //   5: astore_2
/*  125:     */     //   6: aload_1
/*  126:     */     //   7: ldc 35
/*  127:     */     //   9: invokeinterface 36 2 0
/*  128:     */     //   14: astore_3
/*  129:     */     //   15: aconst_null
/*  130:     */     //   16: astore 4
/*  131:     */     //   18: aload_3
/*  132:     */     //   19: iconst_1
/*  133:     */     //   20: aload_0
/*  134:     */     //   21: invokevirtual 37	java/lang/Long:longValue	()J
/*  135:     */     //   24: invokeinterface 38 4 0
/*  136:     */     //   29: aload_3
/*  137:     */     //   30: invokeinterface 39 1 0
/*  138:     */     //   35: astore 5
/*  139:     */     //   37: aconst_null
/*  140:     */     //   38: astore 6
/*  141:     */     //   40: aload 5
/*  142:     */     //   42: invokeinterface 40 1 0
/*  143:     */     //   47: ifeq +11 -> 58
/*  144:     */     //   50: aload_1
/*  145:     */     //   51: aload 5
/*  146:     */     //   53: invokestatic 41	nxt/Transaction:getTransaction	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/Transaction;
/*  147:     */     //   56: astore 6
/*  148:     */     //   58: aload 5
/*  149:     */     //   60: invokeinterface 42 1 0
/*  150:     */     //   65: aload 6
/*  151:     */     //   67: astore 7
/*  152:     */     //   69: aload_3
/*  153:     */     //   70: ifnull +35 -> 105
/*  154:     */     //   73: aload 4
/*  155:     */     //   75: ifnull +24 -> 99
/*  156:     */     //   78: aload_3
/*  157:     */     //   79: invokeinterface 43 1 0
/*  158:     */     //   84: goto +21 -> 105
/*  159:     */     //   87: astore 8
/*  160:     */     //   89: aload 4
/*  161:     */     //   91: aload 8
/*  162:     */     //   93: invokevirtual 45	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  163:     */     //   96: goto +9 -> 105
/*  164:     */     //   99: aload_3
/*  165:     */     //   100: invokeinterface 43 1 0
/*  166:     */     //   105: aload_1
/*  167:     */     //   106: ifnull +33 -> 139
/*  168:     */     //   109: aload_2
/*  169:     */     //   110: ifnull +23 -> 133
/*  170:     */     //   113: aload_1
/*  171:     */     //   114: invokeinterface 46 1 0
/*  172:     */     //   119: goto +20 -> 139
/*  173:     */     //   122: astore 8
/*  174:     */     //   124: aload_2
/*  175:     */     //   125: aload 8
/*  176:     */     //   127: invokevirtual 45	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  177:     */     //   130: goto +9 -> 139
/*  178:     */     //   133: aload_1
/*  179:     */     //   134: invokeinterface 46 1 0
/*  180:     */     //   139: aload 7
/*  181:     */     //   141: areturn
/*  182:     */     //   142: astore 5
/*  183:     */     //   144: aload 5
/*  184:     */     //   146: astore 4
/*  185:     */     //   148: aload 5
/*  186:     */     //   150: athrow
/*  187:     */     //   151: astore 9
/*  188:     */     //   153: aload_3
/*  189:     */     //   154: ifnull +35 -> 189
/*  190:     */     //   157: aload 4
/*  191:     */     //   159: ifnull +24 -> 183
/*  192:     */     //   162: aload_3
/*  193:     */     //   163: invokeinterface 43 1 0
/*  194:     */     //   168: goto +21 -> 189
/*  195:     */     //   171: astore 10
/*  196:     */     //   173: aload 4
/*  197:     */     //   175: aload 10
/*  198:     */     //   177: invokevirtual 45	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  199:     */     //   180: goto +9 -> 189
/*  200:     */     //   183: aload_3
/*  201:     */     //   184: invokeinterface 43 1 0
/*  202:     */     //   189: aload 9
/*  203:     */     //   191: athrow
/*  204:     */     //   192: astore_3
/*  205:     */     //   193: aload_3
/*  206:     */     //   194: astore_2
/*  207:     */     //   195: aload_3
/*  208:     */     //   196: athrow
/*  209:     */     //   197: astore 11
/*  210:     */     //   199: aload_1
/*  211:     */     //   200: ifnull +33 -> 233
/*  212:     */     //   203: aload_2
/*  213:     */     //   204: ifnull +23 -> 227
/*  214:     */     //   207: aload_1
/*  215:     */     //   208: invokeinterface 46 1 0
/*  216:     */     //   213: goto +20 -> 233
/*  217:     */     //   216: astore 12
/*  218:     */     //   218: aload_2
/*  219:     */     //   219: aload 12
/*  220:     */     //   221: invokevirtual 45	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  221:     */     //   224: goto +9 -> 233
/*  222:     */     //   227: aload_1
/*  223:     */     //   228: invokeinterface 46 1 0
/*  224:     */     //   233: aload 11
/*  225:     */     //   235: athrow
/*  226:     */     //   236: astore_1
/*  227:     */     //   237: new 30	java/lang/RuntimeException
/*  228:     */     //   240: dup
/*  229:     */     //   241: aload_1
/*  230:     */     //   242: invokevirtual 48	java/sql/SQLException:getMessage	()Ljava/lang/String;
/*  231:     */     //   245: aload_1
/*  232:     */     //   246: invokespecial 49	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/*  233:     */     //   249: athrow
/*  234:     */     //   250: astore_1
/*  235:     */     //   251: new 30	java/lang/RuntimeException
/*  236:     */     //   254: dup
/*  237:     */     //   255: new 22	java/lang/StringBuilder
/*  238:     */     //   258: dup
/*  239:     */     //   259: invokespecial 23	java/lang/StringBuilder:<init>	()V
/*  240:     */     //   262: ldc 50
/*  241:     */     //   264: invokevirtual 25	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
/*  242:     */     //   267: aload_0
/*  243:     */     //   268: invokevirtual 27	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
/*  244:     */     //   271: ldc 51
/*  245:     */     //   273: invokevirtual 25	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
/*  246:     */     //   276: invokevirtual 28	java/lang/StringBuilder:toString	()Ljava/lang/String;
/*  247:     */     //   279: invokespecial 52	java/lang/RuntimeException:<init>	(Ljava/lang/String;)V
/*  248:     */     //   282: athrow
/*  249:     */     // Line number table:
/*  250:     */     //   Java source line #105	-> byte code offset #0
/*  251:     */     //   Java source line #106	-> byte code offset #6
/*  252:     */     //   Java source line #105	-> byte code offset #15
/*  253:     */     //   Java source line #107	-> byte code offset #18
/*  254:     */     //   Java source line #108	-> byte code offset #29
/*  255:     */     //   Java source line #109	-> byte code offset #37
/*  256:     */     //   Java source line #110	-> byte code offset #40
/*  257:     */     //   Java source line #111	-> byte code offset #50
/*  258:     */     //   Java source line #113	-> byte code offset #58
/*  259:     */     //   Java source line #114	-> byte code offset #65
/*  260:     */     //   Java source line #115	-> byte code offset #69
/*  261:     */     //   Java source line #105	-> byte code offset #142
/*  262:     */     //   Java source line #115	-> byte code offset #151
/*  263:     */     //   Java source line #105	-> byte code offset #192
/*  264:     */     //   Java source line #115	-> byte code offset #197
/*  265:     */     //   Java source line #116	-> byte code offset #237
/*  266:     */     //   Java source line #117	-> byte code offset #250
/*  267:     */     //   Java source line #118	-> byte code offset #251
/*  268:     */     // Local variable table:
/*  269:     */     //   start	length	slot	name	signature
/*  270:     */     //   0	283	0	paramLong	Long
/*  271:     */     //   3	225	1	localConnection	Connection
/*  272:     */     //   236	10	1	localSQLException	SQLException
/*  273:     */     //   250	1	1	localValidationException	NxtException.ValidationException
/*  274:     */     //   5	214	2	localObject1	Object
/*  275:     */     //   14	170	3	localPreparedStatement	PreparedStatement
/*  276:     */     //   192	4	3	localThrowable1	Throwable
/*  277:     */     //   16	158	4	localObject2	Object
/*  278:     */     //   35	24	5	localResultSet	ResultSet
/*  279:     */     //   142	7	5	localThrowable2	Throwable
/*  280:     */     //   38	28	6	localTransaction1	Transaction
/*  281:     */     //   87	5	8	localThrowable3	Throwable
/*  282:     */     //   122	4	8	localThrowable4	Throwable
/*  283:     */     //   151	39	9	localObject3	Object
/*  284:     */     //   171	5	10	localThrowable5	Throwable
/*  285:     */     //   197	37	11	localObject4	Object
/*  286:     */     //   216	4	12	localThrowable6	Throwable
/*  287:     */     // Exception table:
/*  288:     */     //   from	to	target	type
/*  289:     */     //   78	84	87	java/lang/Throwable
/*  290:     */     //   113	119	122	java/lang/Throwable
/*  291:     */     //   18	69	142	java/lang/Throwable
/*  292:     */     //   18	69	151	finally
/*  293:     */     //   142	153	151	finally
/*  294:     */     //   162	168	171	java/lang/Throwable
/*  295:     */     //   6	105	192	java/lang/Throwable
/*  296:     */     //   142	192	192	java/lang/Throwable
/*  297:     */     //   6	105	197	finally
/*  298:     */     //   142	199	197	finally
/*  299:     */     //   207	213	216	java/lang/Throwable
/*  300:     */     //   0	139	236	java/sql/SQLException
/*  301:     */     //   142	236	236	java/sql/SQLException
/*  302:     */     //   0	139	250	nxt/NxtException$ValidationException
/*  303:     */     //   142	236	250	nxt/NxtException$ValidationException
/*  304:     */   }
/*  305:     */   
/*  306:     */   /* Error */
/*  307:     */   static boolean hasTransaction(Long paramLong)
/*  308:     */   {
/*  309:     */     // Byte code:
/*  310:     */     //   0: invokestatic 34	nxt/Db:getConnection	()Ljava/sql/Connection;
/*  311:     */     //   3: astore_1
/*  312:     */     //   4: aconst_null
/*  313:     */     //   5: astore_2
/*  314:     */     //   6: aload_1
/*  315:     */     //   7: ldc 53
/*  316:     */     //   9: invokeinterface 36 2 0
/*  317:     */     //   14: astore_3
/*  318:     */     //   15: aconst_null
/*  319:     */     //   16: astore 4
/*  320:     */     //   18: aload_3
/*  321:     */     //   19: iconst_1
/*  322:     */     //   20: aload_0
/*  323:     */     //   21: invokevirtual 37	java/lang/Long:longValue	()J
/*  324:     */     //   24: invokeinterface 38 4 0
/*  325:     */     //   29: aload_3
/*  326:     */     //   30: invokeinterface 39 1 0
/*  327:     */     //   35: astore 5
/*  328:     */     //   37: aload 5
/*  329:     */     //   39: invokeinterface 40 1 0
/*  330:     */     //   44: istore 6
/*  331:     */     //   46: aload_3
/*  332:     */     //   47: ifnull +35 -> 82
/*  333:     */     //   50: aload 4
/*  334:     */     //   52: ifnull +24 -> 76
/*  335:     */     //   55: aload_3
/*  336:     */     //   56: invokeinterface 43 1 0
/*  337:     */     //   61: goto +21 -> 82
/*  338:     */     //   64: astore 7
/*  339:     */     //   66: aload 4
/*  340:     */     //   68: aload 7
/*  341:     */     //   70: invokevirtual 45	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  342:     */     //   73: goto +9 -> 82
/*  343:     */     //   76: aload_3
/*  344:     */     //   77: invokeinterface 43 1 0
/*  345:     */     //   82: aload_1
/*  346:     */     //   83: ifnull +33 -> 116
/*  347:     */     //   86: aload_2
/*  348:     */     //   87: ifnull +23 -> 110
/*  349:     */     //   90: aload_1
/*  350:     */     //   91: invokeinterface 46 1 0
/*  351:     */     //   96: goto +20 -> 116
/*  352:     */     //   99: astore 7
/*  353:     */     //   101: aload_2
/*  354:     */     //   102: aload 7
/*  355:     */     //   104: invokevirtual 45	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  356:     */     //   107: goto +9 -> 116
/*  357:     */     //   110: aload_1
/*  358:     */     //   111: invokeinterface 46 1 0
/*  359:     */     //   116: iload 6
/*  360:     */     //   118: ireturn
/*  361:     */     //   119: astore 5
/*  362:     */     //   121: aload 5
/*  363:     */     //   123: astore 4
/*  364:     */     //   125: aload 5
/*  365:     */     //   127: athrow
/*  366:     */     //   128: astore 8
/*  367:     */     //   130: aload_3
/*  368:     */     //   131: ifnull +35 -> 166
/*  369:     */     //   134: aload 4
/*  370:     */     //   136: ifnull +24 -> 160
/*  371:     */     //   139: aload_3
/*  372:     */     //   140: invokeinterface 43 1 0
/*  373:     */     //   145: goto +21 -> 166
/*  374:     */     //   148: astore 9
/*  375:     */     //   150: aload 4
/*  376:     */     //   152: aload 9
/*  377:     */     //   154: invokevirtual 45	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  378:     */     //   157: goto +9 -> 166
/*  379:     */     //   160: aload_3
/*  380:     */     //   161: invokeinterface 43 1 0
/*  381:     */     //   166: aload 8
/*  382:     */     //   168: athrow
/*  383:     */     //   169: astore_3
/*  384:     */     //   170: aload_3
/*  385:     */     //   171: astore_2
/*  386:     */     //   172: aload_3
/*  387:     */     //   173: athrow
/*  388:     */     //   174: astore 10
/*  389:     */     //   176: aload_1
/*  390:     */     //   177: ifnull +33 -> 210
/*  391:     */     //   180: aload_2
/*  392:     */     //   181: ifnull +23 -> 204
/*  393:     */     //   184: aload_1
/*  394:     */     //   185: invokeinterface 46 1 0
/*  395:     */     //   190: goto +20 -> 210
/*  396:     */     //   193: astore 11
/*  397:     */     //   195: aload_2
/*  398:     */     //   196: aload 11
/*  399:     */     //   198: invokevirtual 45	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  400:     */     //   201: goto +9 -> 210
/*  401:     */     //   204: aload_1
/*  402:     */     //   205: invokeinterface 46 1 0
/*  403:     */     //   210: aload 10
/*  404:     */     //   212: athrow
/*  405:     */     //   213: astore_1
/*  406:     */     //   214: new 30	java/lang/RuntimeException
/*  407:     */     //   217: dup
/*  408:     */     //   218: aload_1
/*  409:     */     //   219: invokevirtual 48	java/sql/SQLException:getMessage	()Ljava/lang/String;
/*  410:     */     //   222: aload_1
/*  411:     */     //   223: invokespecial 49	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/*  412:     */     //   226: athrow
/*  413:     */     // Line number table:
/*  414:     */     //   Java source line #123	-> byte code offset #0
/*  415:     */     //   Java source line #124	-> byte code offset #6
/*  416:     */     //   Java source line #123	-> byte code offset #15
/*  417:     */     //   Java source line #125	-> byte code offset #18
/*  418:     */     //   Java source line #126	-> byte code offset #29
/*  419:     */     //   Java source line #127	-> byte code offset #37
/*  420:     */     //   Java source line #128	-> byte code offset #46
/*  421:     */     //   Java source line #123	-> byte code offset #119
/*  422:     */     //   Java source line #128	-> byte code offset #128
/*  423:     */     //   Java source line #123	-> byte code offset #169
/*  424:     */     //   Java source line #128	-> byte code offset #174
/*  425:     */     //   Java source line #129	-> byte code offset #214
/*  426:     */     // Local variable table:
/*  427:     */     //   start	length	slot	name	signature
/*  428:     */     //   0	227	0	paramLong	Long
/*  429:     */     //   3	202	1	localConnection	Connection
/*  430:     */     //   213	10	1	localSQLException	SQLException
/*  431:     */     //   5	191	2	localObject1	Object
/*  432:     */     //   14	147	3	localPreparedStatement	PreparedStatement
/*  433:     */     //   169	4	3	localThrowable1	Throwable
/*  434:     */     //   16	135	4	localObject2	Object
/*  435:     */     //   35	3	5	localResultSet	ResultSet
/*  436:     */     //   119	7	5	localThrowable2	Throwable
/*  437:     */     //   64	5	7	localThrowable3	Throwable
/*  438:     */     //   99	4	7	localThrowable4	Throwable
/*  439:     */     //   128	39	8	localObject3	Object
/*  440:     */     //   148	5	9	localThrowable5	Throwable
/*  441:     */     //   174	37	10	localObject4	Object
/*  442:     */     //   193	4	11	localThrowable6	Throwable
/*  443:     */     // Exception table:
/*  444:     */     //   from	to	target	type
/*  445:     */     //   55	61	64	java/lang/Throwable
/*  446:     */     //   90	96	99	java/lang/Throwable
/*  447:     */     //   18	46	119	java/lang/Throwable
/*  448:     */     //   18	46	128	finally
/*  449:     */     //   119	130	128	finally
/*  450:     */     //   139	145	148	java/lang/Throwable
/*  451:     */     //   6	82	169	java/lang/Throwable
/*  452:     */     //   119	169	169	java/lang/Throwable
/*  453:     */     //   6	82	174	finally
/*  454:     */     //   119	176	174	finally
/*  455:     */     //   184	190	193	java/lang/Throwable
/*  456:     */     //   0	116	213	java/sql/SQLException
/*  457:     */     //   119	213	213	java/sql/SQLException
/*  458:     */   }
/*  459:     */   
/*  460:     */   static Transaction getTransaction(JSONObject paramJSONObject)
/*  461:     */     throws NxtException.ValidationException
/*  462:     */   {
/*  463:     */     try
/*  464:     */     {
/*  465: 137 */       byte b1 = ((Long)paramJSONObject.get("type")).byteValue();
/*  466: 138 */       byte b2 = ((Long)paramJSONObject.get("subtype")).byteValue();
/*  467: 139 */       int i = ((Long)paramJSONObject.get("timestamp")).intValue();
/*  468: 140 */       short s = ((Long)paramJSONObject.get("deadline")).shortValue();
/*  469: 141 */       byte[] arrayOfByte1 = Convert.convert((String)paramJSONObject.get("senderPublicKey"));
/*  470: 142 */       Long localLong1 = Convert.parseUnsignedLong((String)paramJSONObject.get("recipient"));
/*  471: 143 */       if (localLong1 == null) {
/*  472: 143 */         localLong1 = Long.valueOf(0L);
/*  473:     */       }
/*  474: 144 */       int j = ((Long)paramJSONObject.get("amount")).intValue();
/*  475: 145 */       int k = ((Long)paramJSONObject.get("fee")).intValue();
/*  476: 146 */       Long localLong2 = Convert.parseUnsignedLong((String)paramJSONObject.get("referencedTransaction"));
/*  477: 147 */       byte[] arrayOfByte2 = Convert.convert((String)paramJSONObject.get("signature"));
/*  478:     */       
/*  479: 149 */       Type localType = findTransactionType(b1, b2);
/*  480: 150 */       Transaction localTransaction = new Transaction(localType, i, s, arrayOfByte1, localLong1, j, k, localLong2, arrayOfByte2);
/*  481:     */       
/*  482:     */ 
/*  483: 153 */       JSONObject localJSONObject = (JSONObject)paramJSONObject.get("attachment");
/*  484: 155 */       if (!localType.loadAttachment(localTransaction, localJSONObject)) {
/*  485: 156 */         throw new NxtException.ValidationException("Invalid transaction attachment:\n" + localJSONObject.toJSONString());
/*  486:     */       }
/*  487: 159 */       return localTransaction;
/*  488:     */     }
/*  489:     */     catch (RuntimeException localRuntimeException)
/*  490:     */     {
/*  491: 162 */       throw new NxtException.ValidationException(localRuntimeException.toString());
/*  492:     */     }
/*  493:     */   }
/*  494:     */   
/*  495:     */   static Transaction getTransaction(Connection paramConnection, ResultSet paramResultSet)
/*  496:     */     throws NxtException.ValidationException
/*  497:     */   {
/*  498:     */     try
/*  499:     */     {
/*  500: 169 */       byte b1 = paramResultSet.getByte("type");
/*  501: 170 */       byte b2 = paramResultSet.getByte("subtype");
/*  502: 171 */       int i = paramResultSet.getInt("timestamp");
/*  503: 172 */       short s = paramResultSet.getShort("deadline");
/*  504: 173 */       byte[] arrayOfByte1 = paramResultSet.getBytes("sender_public_key");
/*  505: 174 */       Long localLong1 = Long.valueOf(paramResultSet.getLong("recipient_id"));
/*  506: 175 */       int j = paramResultSet.getInt("amount");
/*  507: 176 */       int k = paramResultSet.getInt("fee");
/*  508: 177 */       Long localLong2 = Long.valueOf(paramResultSet.getLong("referenced_transaction_id"));
/*  509: 178 */       if (paramResultSet.wasNull()) {
/*  510: 179 */         localLong2 = null;
/*  511:     */       }
/*  512: 181 */       byte[] arrayOfByte2 = paramResultSet.getBytes("signature");
/*  513:     */       
/*  514: 183 */       Type localType = findTransactionType(b1, b2);
/*  515: 184 */       Transaction localTransaction = new Transaction(localType, i, s, arrayOfByte1, localLong1, j, k, localLong2, arrayOfByte2);
/*  516:     */       
/*  517: 186 */       localTransaction.blockId = Long.valueOf(paramResultSet.getLong("block_id"));
/*  518: 187 */       localTransaction.index = paramResultSet.getInt("index");
/*  519: 188 */       localTransaction.height = paramResultSet.getInt("height");
/*  520: 189 */       localTransaction.id = Long.valueOf(paramResultSet.getLong("id"));
/*  521: 190 */       localTransaction.senderAccountId = Long.valueOf(paramResultSet.getLong("sender_account_id"));
/*  522:     */       
/*  523: 192 */       localTransaction.attachment = ((Attachment)paramResultSet.getObject("attachment"));
/*  524:     */       
/*  525: 194 */       return localTransaction;
/*  526:     */     }
/*  527:     */     catch (SQLException localSQLException)
/*  528:     */     {
/*  529: 197 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/*  530:     */     }
/*  531:     */   }
/*  532:     */   
/*  533:     */   /* Error */
/*  534:     */   static java.util.List<Transaction> findBlockTransactions(Connection paramConnection, Long paramLong)
/*  535:     */   {
/*  536:     */     // Byte code:
/*  537:     */     //   0: new 98	java/util/ArrayList
/*  538:     */     //   3: dup
/*  539:     */     //   4: invokespecial 99	java/util/ArrayList:<init>	()V
/*  540:     */     //   7: astore_2
/*  541:     */     //   8: aload_0
/*  542:     */     //   9: ldc 100
/*  543:     */     //   11: invokeinterface 36 2 0
/*  544:     */     //   16: astore_3
/*  545:     */     //   17: aconst_null
/*  546:     */     //   18: astore 4
/*  547:     */     //   20: aload_3
/*  548:     */     //   21: iconst_1
/*  549:     */     //   22: aload_1
/*  550:     */     //   23: invokevirtual 37	java/lang/Long:longValue	()J
/*  551:     */     //   26: invokeinterface 38 4 0
/*  552:     */     //   31: aload_3
/*  553:     */     //   32: invokeinterface 39 1 0
/*  554:     */     //   37: astore 5
/*  555:     */     //   39: aload 5
/*  556:     */     //   41: invokeinterface 40 1 0
/*  557:     */     //   46: ifeq +19 -> 65
/*  558:     */     //   49: aload_2
/*  559:     */     //   50: aload_0
/*  560:     */     //   51: aload 5
/*  561:     */     //   53: invokestatic 41	nxt/Transaction:getTransaction	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/Transaction;
/*  562:     */     //   56: invokeinterface 101 2 0
/*  563:     */     //   61: pop
/*  564:     */     //   62: goto -23 -> 39
/*  565:     */     //   65: aload 5
/*  566:     */     //   67: invokeinterface 42 1 0
/*  567:     */     //   72: aload_2
/*  568:     */     //   73: astore 6
/*  569:     */     //   75: aload_3
/*  570:     */     //   76: ifnull +35 -> 111
/*  571:     */     //   79: aload 4
/*  572:     */     //   81: ifnull +24 -> 105
/*  573:     */     //   84: aload_3
/*  574:     */     //   85: invokeinterface 43 1 0
/*  575:     */     //   90: goto +21 -> 111
/*  576:     */     //   93: astore 7
/*  577:     */     //   95: aload 4
/*  578:     */     //   97: aload 7
/*  579:     */     //   99: invokevirtual 45	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  580:     */     //   102: goto +9 -> 111
/*  581:     */     //   105: aload_3
/*  582:     */     //   106: invokeinterface 43 1 0
/*  583:     */     //   111: aload 6
/*  584:     */     //   113: areturn
/*  585:     */     //   114: astore 5
/*  586:     */     //   116: aload 5
/*  587:     */     //   118: astore 4
/*  588:     */     //   120: aload 5
/*  589:     */     //   122: athrow
/*  590:     */     //   123: astore 8
/*  591:     */     //   125: aload_3
/*  592:     */     //   126: ifnull +35 -> 161
/*  593:     */     //   129: aload 4
/*  594:     */     //   131: ifnull +24 -> 155
/*  595:     */     //   134: aload_3
/*  596:     */     //   135: invokeinterface 43 1 0
/*  597:     */     //   140: goto +21 -> 161
/*  598:     */     //   143: astore 9
/*  599:     */     //   145: aload 4
/*  600:     */     //   147: aload 9
/*  601:     */     //   149: invokevirtual 45	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
/*  602:     */     //   152: goto +9 -> 161
/*  603:     */     //   155: aload_3
/*  604:     */     //   156: invokeinterface 43 1 0
/*  605:     */     //   161: aload 8
/*  606:     */     //   163: athrow
/*  607:     */     //   164: astore_3
/*  608:     */     //   165: new 30	java/lang/RuntimeException
/*  609:     */     //   168: dup
/*  610:     */     //   169: aload_3
/*  611:     */     //   170: invokevirtual 97	java/sql/SQLException:toString	()Ljava/lang/String;
/*  612:     */     //   173: aload_3
/*  613:     */     //   174: invokespecial 49	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
/*  614:     */     //   177: athrow
/*  615:     */     //   178: astore_3
/*  616:     */     //   179: new 30	java/lang/RuntimeException
/*  617:     */     //   182: dup
/*  618:     */     //   183: new 22	java/lang/StringBuilder
/*  619:     */     //   186: dup
/*  620:     */     //   187: invokespecial 23	java/lang/StringBuilder:<init>	()V
/*  621:     */     //   190: ldc 102
/*  622:     */     //   192: invokevirtual 25	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
/*  623:     */     //   195: aload_1
/*  624:     */     //   196: invokevirtual 27	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
/*  625:     */     //   199: ldc 103
/*  626:     */     //   201: invokevirtual 25	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
/*  627:     */     //   204: invokevirtual 28	java/lang/StringBuilder:toString	()Ljava/lang/String;
/*  628:     */     //   207: invokespecial 52	java/lang/RuntimeException:<init>	(Ljava/lang/String;)V
/*  629:     */     //   210: athrow
/*  630:     */     // Line number table:
/*  631:     */     //   Java source line #202	-> byte code offset #0
/*  632:     */     //   Java source line #203	-> byte code offset #8
/*  633:     */     //   Java source line #204	-> byte code offset #20
/*  634:     */     //   Java source line #205	-> byte code offset #31
/*  635:     */     //   Java source line #206	-> byte code offset #39
/*  636:     */     //   Java source line #207	-> byte code offset #49
/*  637:     */     //   Java source line #209	-> byte code offset #65
/*  638:     */     //   Java source line #210	-> byte code offset #72
/*  639:     */     //   Java source line #211	-> byte code offset #75
/*  640:     */     //   Java source line #203	-> byte code offset #114
/*  641:     */     //   Java source line #211	-> byte code offset #123
/*  642:     */     //   Java source line #212	-> byte code offset #165
/*  643:     */     //   Java source line #213	-> byte code offset #178
/*  644:     */     //   Java source line #214	-> byte code offset #179
/*  645:     */     // Local variable table:
/*  646:     */     //   start	length	slot	name	signature
/*  647:     */     //   0	211	0	paramConnection	Connection
/*  648:     */     //   0	211	1	paramLong	Long
/*  649:     */     //   7	66	2	localArrayList1	java.util.ArrayList
/*  650:     */     //   16	140	3	localPreparedStatement	PreparedStatement
/*  651:     */     //   164	10	3	localSQLException	SQLException
/*  652:     */     //   178	1	3	localValidationException	NxtException.ValidationException
/*  653:     */     //   18	128	4	localObject1	Object
/*  654:     */     //   37	29	5	localResultSet	ResultSet
/*  655:     */     //   114	7	5	localThrowable1	Throwable
/*  656:     */     //   93	5	7	localThrowable2	Throwable
/*  657:     */     //   123	39	8	localObject2	Object
/*  658:     */     //   143	5	9	localThrowable3	Throwable
/*  659:     */     // Exception table:
/*  660:     */     //   from	to	target	type
/*  661:     */     //   84	90	93	java/lang/Throwable
/*  662:     */     //   20	75	114	java/lang/Throwable
/*  663:     */     //   20	75	123	finally
/*  664:     */     //   114	125	123	finally
/*  665:     */     //   134	140	143	java/lang/Throwable
/*  666:     */     //   8	111	164	java/sql/SQLException
/*  667:     */     //   114	164	164	java/sql/SQLException
/*  668:     */     //   8	111	178	nxt/NxtException$ValidationException
/*  669:     */     //   114	164	178	nxt/NxtException$ValidationException
/*  670:     */   }
/*  671:     */   
/*  672:     */   static void saveTransactions(Connection paramConnection, Transaction... paramVarArgs)
/*  673:     */   {
/*  674:     */     try
/*  675:     */     {
/*  676: 220 */       for (Transaction localTransaction : paramVarArgs)
/*  677:     */       {
/*  678: 221 */         PreparedStatement localPreparedStatement = paramConnection.prepareStatement("INSERT INTO transaction (id, deadline, sender_public_key, recipient_id, amount, fee, referenced_transaction_id, index, height, block_id, signature, timestamp, type, subtype, sender_account_id, attachment)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");Object localObject1 = null;
/*  679:     */         try
/*  680:     */         {
/*  681: 224 */           localPreparedStatement.setLong(1, localTransaction.getId().longValue());
/*  682: 225 */           localPreparedStatement.setShort(2, localTransaction.deadline);
/*  683: 226 */           localPreparedStatement.setBytes(3, localTransaction.senderPublicKey);
/*  684: 227 */           localPreparedStatement.setLong(4, localTransaction.recipientId.longValue());
/*  685: 228 */           localPreparedStatement.setInt(5, localTransaction.amount);
/*  686: 229 */           localPreparedStatement.setInt(6, localTransaction.fee);
/*  687: 230 */           if (localTransaction.referencedTransactionId != null) {
/*  688: 231 */             localPreparedStatement.setLong(7, localTransaction.referencedTransactionId.longValue());
/*  689:     */           } else {
/*  690: 233 */             localPreparedStatement.setNull(7, -5);
/*  691:     */           }
/*  692: 235 */           localPreparedStatement.setInt(8, localTransaction.index);
/*  693: 236 */           localPreparedStatement.setInt(9, localTransaction.height);
/*  694: 237 */           localPreparedStatement.setLong(10, localTransaction.blockId.longValue());
/*  695: 238 */           localPreparedStatement.setBytes(11, localTransaction.signature);
/*  696: 239 */           localPreparedStatement.setInt(12, localTransaction.timestamp);
/*  697: 240 */           localPreparedStatement.setByte(13, localTransaction.type.getType());
/*  698: 241 */           localPreparedStatement.setByte(14, localTransaction.type.getSubtype());
/*  699: 242 */           localPreparedStatement.setLong(15, localTransaction.getSenderAccountId().longValue());
/*  700: 243 */           if (localTransaction.attachment != null) {
/*  701: 244 */             localPreparedStatement.setObject(16, localTransaction.attachment);
/*  702:     */           } else {
/*  703: 246 */             localPreparedStatement.setNull(16, 2000);
/*  704:     */           }
/*  705: 248 */           localPreparedStatement.executeUpdate();
/*  706:     */         }
/*  707:     */         catch (Throwable localThrowable2)
/*  708:     */         {
/*  709: 221 */           localObject1 = localThrowable2;throw localThrowable2;
/*  710:     */         }
/*  711:     */         finally
/*  712:     */         {
/*  713: 249 */           if (localPreparedStatement != null) {
/*  714: 249 */             if (localObject1 != null) {
/*  715:     */               try
/*  716:     */               {
/*  717: 249 */                 localPreparedStatement.close();
/*  718:     */               }
/*  719:     */               catch (Throwable localThrowable3)
/*  720:     */               {
/*  721: 249 */                 localObject1.addSuppressed(localThrowable3);
/*  722:     */               }
/*  723:     */             } else {
/*  724: 249 */               localPreparedStatement.close();
/*  725:     */             }
/*  726:     */           }
/*  727:     */         }
/*  728:     */       }
/*  729:     */     }
/*  730:     */     catch (SQLException localSQLException)
/*  731:     */     {
/*  732: 252 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/*  733:     */     }
/*  734:     */   }
/*  735:     */   
/*  736: 273 */   private volatile String stringId = null;
/*  737:     */   private volatile Long senderAccountId;
/*  738:     */   private volatile String hash;
/*  739:     */   private static final int TRANSACTION_BYTES_LENGTH = 128;
/*  740:     */   
/*  741:     */   private Transaction(Type paramType, int paramInt1, short paramShort, byte[] paramArrayOfByte1, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, byte[] paramArrayOfByte2)
/*  742:     */     throws NxtException.ValidationException
/*  743:     */   {
/*  744: 280 */     if ((paramInt1 == 0) && (Arrays.equals(paramArrayOfByte1, Genesis.CREATOR_PUBLIC_KEY)) ? (paramShort == 0) || (paramInt3 == 0) : (paramShort < 1) || (paramInt3 <= 0) || (paramInt3 > 1000000000L) || (paramInt2 < 0) || (paramInt2 > 1000000000L) || (paramType == null)) {
/*  745: 282 */       throw new NxtException.ValidationException("Invalid transaction parameters:\n type: " + paramType + ", timestamp: " + paramInt1 + ", deadline: " + paramShort + ", fee: " + paramInt3 + ", amount: " + paramInt2);
/*  746:     */     }
/*  747: 286 */     this.timestamp = paramInt1;
/*  748: 287 */     this.deadline = paramShort;
/*  749: 288 */     this.senderPublicKey = paramArrayOfByte1;
/*  750: 289 */     this.recipientId = paramLong1;
/*  751: 290 */     this.amount = paramInt2;
/*  752: 291 */     this.fee = paramInt3;
/*  753: 292 */     this.referencedTransactionId = paramLong2;
/*  754: 293 */     this.signature = paramArrayOfByte2;
/*  755: 294 */     this.type = paramType;
/*  756: 295 */     this.height = 2147483647;
/*  757:     */   }
/*  758:     */   
/*  759:     */   public short getDeadline()
/*  760:     */   {
/*  761: 300 */     return this.deadline;
/*  762:     */   }
/*  763:     */   
/*  764:     */   public byte[] getSenderPublicKey()
/*  765:     */   {
/*  766: 304 */     return this.senderPublicKey;
/*  767:     */   }
/*  768:     */   
/*  769:     */   public Long getRecipientId()
/*  770:     */   {
/*  771: 308 */     return this.recipientId;
/*  772:     */   }
/*  773:     */   
/*  774:     */   public int getAmount()
/*  775:     */   {
/*  776: 312 */     return this.amount;
/*  777:     */   }
/*  778:     */   
/*  779:     */   public int getFee()
/*  780:     */   {
/*  781: 316 */     return this.fee;
/*  782:     */   }
/*  783:     */   
/*  784:     */   public Long getReferencedTransactionId()
/*  785:     */   {
/*  786: 320 */     return this.referencedTransactionId;
/*  787:     */   }
/*  788:     */   
/*  789:     */   public int getHeight()
/*  790:     */   {
/*  791: 324 */     return this.height;
/*  792:     */   }
/*  793:     */   
/*  794:     */   public byte[] getSignature()
/*  795:     */   {
/*  796: 328 */     return this.signature;
/*  797:     */   }
/*  798:     */   
/*  799:     */   public Type getType()
/*  800:     */   {
/*  801: 332 */     return this.type;
/*  802:     */   }
/*  803:     */   
/*  804:     */   public Block getBlock()
/*  805:     */   {
/*  806: 336 */     if (this.block == null) {
/*  807: 337 */       this.block = Block.findBlock(this.blockId);
/*  808:     */     }
/*  809: 339 */     return this.block;
/*  810:     */   }
/*  811:     */   
/*  812:     */   void setBlock(Block paramBlock)
/*  813:     */   {
/*  814: 343 */     this.block = paramBlock;
/*  815: 344 */     this.blockId = paramBlock.getId();
/*  816: 345 */     this.height = paramBlock.getHeight();
/*  817:     */   }
/*  818:     */   
/*  819:     */   void setHeight(int paramInt)
/*  820:     */   {
/*  821: 349 */     this.height = paramInt;
/*  822:     */   }
/*  823:     */   
/*  824:     */   public int getIndex()
/*  825:     */   {
/*  826: 353 */     return this.index;
/*  827:     */   }
/*  828:     */   
/*  829:     */   void setIndex(int paramInt)
/*  830:     */   {
/*  831: 357 */     this.index = paramInt;
/*  832:     */   }
/*  833:     */   
/*  834:     */   public int getTimestamp()
/*  835:     */   {
/*  836: 361 */     return this.timestamp;
/*  837:     */   }
/*  838:     */   
/*  839:     */   public int getExpiration()
/*  840:     */   {
/*  841: 365 */     return this.timestamp + this.deadline * 60;
/*  842:     */   }
/*  843:     */   
/*  844:     */   public Attachment getAttachment()
/*  845:     */   {
/*  846: 369 */     return this.attachment;
/*  847:     */   }
/*  848:     */   
/*  849:     */   public Long getId()
/*  850:     */   {
/*  851: 373 */     if (this.id == null)
/*  852:     */     {
/*  853: 374 */       byte[] arrayOfByte = Crypto.sha256().digest(getBytes());
/*  854: 375 */       BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
/*  855: 376 */       this.id = Long.valueOf(localBigInteger.longValue());
/*  856: 377 */       this.stringId = localBigInteger.toString();
/*  857:     */     }
/*  858: 379 */     return this.id;
/*  859:     */   }
/*  860:     */   
/*  861:     */   public String getStringId()
/*  862:     */   {
/*  863: 383 */     if (this.stringId == null)
/*  864:     */     {
/*  865: 384 */       getId();
/*  866: 385 */       if (this.stringId == null) {
/*  867: 386 */         this.stringId = Convert.convert(this.id);
/*  868:     */       }
/*  869:     */     }
/*  870: 389 */     return this.stringId;
/*  871:     */   }
/*  872:     */   
/*  873:     */   public Long getSenderAccountId()
/*  874:     */   {
/*  875: 393 */     if (this.senderAccountId == null) {
/*  876: 394 */       this.senderAccountId = Account.getId(this.senderPublicKey);
/*  877:     */     }
/*  878: 396 */     return this.senderAccountId;
/*  879:     */   }
/*  880:     */   
/*  881:     */   public int compareTo(Transaction paramTransaction)
/*  882:     */   {
/*  883: 402 */     if (this.height < paramTransaction.height) {
/*  884: 404 */       return -1;
/*  885:     */     }
/*  886: 406 */     if (this.height > paramTransaction.height) {
/*  887: 408 */       return 1;
/*  888:     */     }
/*  889: 413 */     if (this.fee * paramTransaction.getSize() > paramTransaction.fee * getSize()) {
/*  890: 415 */       return -1;
/*  891:     */     }
/*  892: 417 */     if (this.fee * paramTransaction.getSize() < paramTransaction.fee * getSize()) {
/*  893: 419 */       return 1;
/*  894:     */     }
/*  895: 423 */     if (this.timestamp < paramTransaction.timestamp) {
/*  896: 425 */       return -1;
/*  897:     */     }
/*  898: 427 */     if (this.timestamp > paramTransaction.timestamp) {
/*  899: 429 */       return 1;
/*  900:     */     }
/*  901: 433 */     if (this.index < paramTransaction.index) {
/*  902: 435 */       return -1;
/*  903:     */     }
/*  904: 437 */     if (this.index > paramTransaction.index) {
/*  905: 439 */       return 1;
/*  906:     */     }
/*  907: 443 */     return 0;
/*  908:     */   }
/*  909:     */   
/*  910:     */   public byte[] getBytes()
/*  911:     */   {
/*  912: 457 */     ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
/*  913: 458 */     localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/*  914: 459 */     localByteBuffer.put(this.type.getType());
/*  915: 460 */     localByteBuffer.put(this.type.getSubtype());
/*  916: 461 */     localByteBuffer.putInt(this.timestamp);
/*  917: 462 */     localByteBuffer.putShort(this.deadline);
/*  918: 463 */     localByteBuffer.put(this.senderPublicKey);
/*  919: 464 */     localByteBuffer.putLong(Convert.nullToZero(this.recipientId));
/*  920: 465 */     localByteBuffer.putInt(this.amount);
/*  921: 466 */     localByteBuffer.putInt(this.fee);
/*  922: 467 */     localByteBuffer.putLong(Convert.nullToZero(this.referencedTransactionId));
/*  923: 468 */     localByteBuffer.put(this.signature);
/*  924: 469 */     if (this.attachment != null) {
/*  925: 470 */       localByteBuffer.put(this.attachment.getBytes());
/*  926:     */     }
/*  927: 472 */     return localByteBuffer.array();
/*  928:     */   }
/*  929:     */   
/*  930:     */   public JSONObject getJSONObject()
/*  931:     */   {
/*  932: 478 */     JSONObject localJSONObject = new JSONObject();
/*  933:     */     
/*  934: 480 */     localJSONObject.put("type", Byte.valueOf(this.type.getType()));
/*  935: 481 */     localJSONObject.put("subtype", Byte.valueOf(this.type.getSubtype()));
/*  936: 482 */     localJSONObject.put("timestamp", Integer.valueOf(this.timestamp));
/*  937: 483 */     localJSONObject.put("deadline", Short.valueOf(this.deadline));
/*  938: 484 */     localJSONObject.put("senderPublicKey", Convert.convert(this.senderPublicKey));
/*  939: 485 */     localJSONObject.put("recipient", Convert.convert(this.recipientId));
/*  940: 486 */     localJSONObject.put("amount", Integer.valueOf(this.amount));
/*  941: 487 */     localJSONObject.put("fee", Integer.valueOf(this.fee));
/*  942: 488 */     localJSONObject.put("referencedTransaction", Convert.convert(this.referencedTransactionId));
/*  943: 489 */     localJSONObject.put("signature", Convert.convert(this.signature));
/*  944: 490 */     if (this.attachment != null) {
/*  945: 491 */       localJSONObject.put("attachment", this.attachment.getJSON());
/*  946:     */     }
/*  947: 494 */     return localJSONObject;
/*  948:     */   }
/*  949:     */   
/*  950:     */   public void sign(String paramString)
/*  951:     */   {
/*  952: 499 */     if (this.signature != null) {
/*  953: 500 */       throw new IllegalStateException("Transaction already signed");
/*  954:     */     }
/*  955: 503 */     this.signature = new byte[64];
/*  956: 504 */     this.signature = Crypto.sign(getBytes(), paramString);
/*  957:     */     try
/*  958:     */     {
/*  959: 508 */       while (!verify())
/*  960:     */       {
/*  961: 510 */         this.timestamp += 1;
/*  962:     */         
/*  963: 512 */         this.signature = new byte[64];
/*  964: 513 */         this.signature = Crypto.sign(getBytes(), paramString);
/*  965:     */       }
/*  966:     */     }
/*  967:     */     catch (RuntimeException localRuntimeException)
/*  968:     */     {
/*  969: 519 */       Logger.logMessage("Error signing transaction", localRuntimeException);
/*  970:     */     }
/*  971:     */   }
/*  972:     */   
/*  973:     */   public boolean equals(Object paramObject)
/*  974:     */   {
/*  975: 527 */     return ((paramObject instanceof Transaction)) && (getId().equals(((Transaction)paramObject).getId()));
/*  976:     */   }
/*  977:     */   
/*  978:     */   public int hashCode()
/*  979:     */   {
/*  980: 532 */     return getId().hashCode();
/*  981:     */   }
/*  982:     */   
/*  983:     */   boolean verify()
/*  984:     */   {
/*  985: 536 */     Account localAccount = Account.getAccount(getSenderAccountId());
/*  986: 537 */     if (localAccount == null) {
/*  987: 538 */       return false;
/*  988:     */     }
/*  989: 540 */     byte[] arrayOfByte = getBytes();
/*  990: 541 */     for (int i = 64; i < 128; i++) {
/*  991: 542 */       arrayOfByte[i] = 0;
/*  992:     */     }
/*  993: 544 */     return (Crypto.verify(this.signature, arrayOfByte, this.senderPublicKey)) && (localAccount.setOrVerify(this.senderPublicKey));
/*  994:     */   }
/*  995:     */   
/*  996:     */   boolean isDoubleSpending()
/*  997:     */   {
/*  998: 549 */     Account localAccount = Account.getAccount(getSenderAccountId());
/*  999: 550 */     if (localAccount == null) {
/* 1000: 551 */       return true;
/* 1001:     */     }
/* 1002: 553 */     synchronized (localAccount)
/* 1003:     */     {
/* 1004: 554 */       return this.type.isDoubleSpending(this, localAccount, this.amount + this.fee);
/* 1005:     */     }
/* 1006:     */   }
/* 1007:     */   
/* 1008:     */   void apply()
/* 1009:     */   {
/* 1010: 559 */     Account localAccount1 = Account.getAccount(getSenderAccountId());
/* 1011: 560 */     if (!localAccount1.setOrVerify(this.senderPublicKey)) {
/* 1012: 561 */       throw new RuntimeException("sender public key mismatch");
/* 1013:     */     }
/* 1014: 564 */     Blockchain.transactionHashes.put(getHash(), this);
/* 1015: 565 */     Account localAccount2 = Account.getAccount(this.recipientId);
/* 1016: 566 */     if (localAccount2 == null) {
/* 1017: 567 */       localAccount2 = Account.addOrGetAccount(this.recipientId);
/* 1018:     */     }
/* 1019: 569 */     localAccount1.addToBalanceAndUnconfirmedBalance(-(this.amount + this.fee) * 100L);
/* 1020: 570 */     this.type.apply(this, localAccount1, localAccount2);
/* 1021:     */   }
/* 1022:     */   
/* 1023:     */   void undo()
/* 1024:     */     throws Transaction.UndoNotSupportedException
/* 1025:     */   {
/* 1026: 575 */     Account localAccount1 = Account.getAccount(this.senderAccountId);
/* 1027: 576 */     localAccount1.addToBalance((this.amount + this.fee) * 100L);
/* 1028: 577 */     Account localAccount2 = Account.getAccount(this.recipientId);
/* 1029: 578 */     this.type.undo(this, localAccount1, localAccount2);
/* 1030:     */   }
/* 1031:     */   
/* 1032:     */   void updateTotals(Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1)
/* 1033:     */   {
/* 1034: 582 */     Long localLong1 = getSenderAccountId();
/* 1035: 583 */     Long localLong2 = (Long)paramMap.get(localLong1);
/* 1036: 584 */     if (localLong2 == null) {
/* 1037: 585 */       localLong2 = Long.valueOf(0L);
/* 1038:     */     }
/* 1039: 587 */     paramMap.put(localLong1, Long.valueOf(localLong2.longValue() + (this.amount + this.fee) * 100L));
/* 1040: 588 */     this.type.updateTotals(this, paramMap, paramMap1, localLong2);
/* 1041:     */   }
/* 1042:     */   
/* 1043:     */   boolean isDuplicate(Map<Type, Set<String>> paramMap)
/* 1044:     */   {
/* 1045: 592 */     return this.type.isDuplicate(this, paramMap);
/* 1046:     */   }
/* 1047:     */   
/* 1048:     */   int getSize()
/* 1049:     */   {
/* 1050: 598 */     return 128 + (this.attachment == null ? 0 : this.attachment.getSize());
/* 1051:     */   }
/* 1052:     */   
/* 1053:     */   String getHash()
/* 1054:     */   {
/* 1055: 602 */     if (this.hash == null)
/* 1056:     */     {
/* 1057: 603 */       byte[] arrayOfByte = getBytes();
/* 1058: 604 */       for (int i = 64; i < 128; i++) {
/* 1059: 605 */         arrayOfByte[i] = 0;
/* 1060:     */       }
/* 1061: 607 */       this.hash = Convert.convert(Crypto.sha256().digest(arrayOfByte));
/* 1062:     */     }
/* 1063: 609 */     return this.hash;
/* 1064:     */   }
/* 1065:     */   
/* 1066:     */   public static Type findTransactionType(byte paramByte1, byte paramByte2)
/* 1067:     */   {
/* 1068: 613 */     switch (paramByte1)
/* 1069:     */     {
/* 1070:     */     case 0: 
/* 1071: 615 */       switch (paramByte2)
/* 1072:     */       {
/* 1073:     */       case 0: 
/* 1074: 617 */         return Transaction.Type.Payment.ORDINARY;
/* 1075:     */       }
/* 1076: 619 */       return null;
/* 1077:     */     case 1: 
/* 1078: 622 */       switch (paramByte2)
/* 1079:     */       {
/* 1080:     */       case 0: 
/* 1081: 624 */         return Transaction.Type.Messaging.ARBITRARY_MESSAGE;
/* 1082:     */       case 1: 
/* 1083: 626 */         return Transaction.Type.Messaging.ALIAS_ASSIGNMENT;
/* 1084:     */       }
/* 1085: 628 */       return null;
/* 1086:     */     case 2: 
/* 1087: 631 */       switch (paramByte2)
/* 1088:     */       {
/* 1089:     */       case 0: 
/* 1090: 633 */         return Transaction.Type.ColoredCoins.ASSET_ISSUANCE;
/* 1091:     */       case 1: 
/* 1092: 635 */         return Transaction.Type.ColoredCoins.ASSET_TRANSFER;
/* 1093:     */       case 2: 
/* 1094: 637 */         return Transaction.Type.ColoredCoins.ASK_ORDER_PLACEMENT;
/* 1095:     */       case 3: 
/* 1096: 639 */         return Transaction.Type.ColoredCoins.BID_ORDER_PLACEMENT;
/* 1097:     */       case 4: 
/* 1098: 641 */         return Transaction.Type.ColoredCoins.ASK_ORDER_CANCELLATION;
/* 1099:     */       case 5: 
/* 1100: 643 */         return Transaction.Type.ColoredCoins.BID_ORDER_CANCELLATION;
/* 1101:     */       }
/* 1102: 645 */       return null;
/* 1103:     */     }
/* 1104: 648 */     return null;
/* 1105:     */   }
/* 1106:     */   
/* 1107:     */   public static abstract class Type
/* 1108:     */   {
/* 1109:     */     public abstract byte getType();
/* 1110:     */     
/* 1111:     */     public abstract byte getSubtype();
/* 1112:     */     
/* 1113:     */     abstract boolean loadAttachment(Transaction paramTransaction, ByteBuffer paramByteBuffer)
/* 1114:     */       throws NxtException.ValidationException;
/* 1115:     */     
/* 1116:     */     abstract boolean loadAttachment(Transaction paramTransaction, JSONObject paramJSONObject)
/* 1117:     */       throws NxtException.ValidationException;
/* 1118:     */     
/* 1119:     */     final boolean isDoubleSpending(Transaction paramTransaction, Account paramAccount, int paramInt)
/* 1120:     */     {
/* 1121: 664 */       if (paramAccount.getUnconfirmedBalance() < paramInt * 100L) {
/* 1122: 665 */         return true;
/* 1123:     */       }
/* 1124: 667 */       paramAccount.addToUnconfirmedBalance(-paramInt * 100L);
/* 1125: 668 */       return checkDoubleSpending(paramTransaction, paramAccount, paramInt);
/* 1126:     */     }
/* 1127:     */     
/* 1128:     */     abstract boolean checkDoubleSpending(Transaction paramTransaction, Account paramAccount, int paramInt);
/* 1129:     */     
/* 1130:     */     abstract void apply(Transaction paramTransaction, Account paramAccount1, Account paramAccount2);
/* 1131:     */     
/* 1132:     */     abstract void undo(Transaction paramTransaction, Account paramAccount1, Account paramAccount2)
/* 1133:     */       throws Transaction.UndoNotSupportedException;
/* 1134:     */     
/* 1135:     */     abstract void updateTotals(Transaction paramTransaction, Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1, Long paramLong);
/* 1136:     */     
/* 1137:     */     boolean isDuplicate(Transaction paramTransaction, Map<Type, Set<String>> paramMap)
/* 1138:     */     {
/* 1139: 681 */       return false;
/* 1140:     */     }
/* 1141:     */     
/* 1142:     */     public static abstract class Payment
/* 1143:     */       extends Transaction.Type
/* 1144:     */     {
/* 1145:     */       public final byte getType()
/* 1146:     */       {
/* 1147: 688 */         return 0;
/* 1148:     */       }
/* 1149:     */       
/* 1150: 691 */       public static final Transaction.Type ORDINARY = new Payment()
/* 1151:     */       {
/* 1152:     */         public final byte getSubtype()
/* 1153:     */         {
/* 1154: 695 */           return 0;
/* 1155:     */         }
/* 1156:     */         
/* 1157:     */         final boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/* 1158:     */         {
/* 1159: 700 */           return validateAttachment(paramAnonymousTransaction);
/* 1160:     */         }
/* 1161:     */         
/* 1162:     */         final boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/* 1163:     */         {
/* 1164: 705 */           return validateAttachment(paramAnonymousTransaction);
/* 1165:     */         }
/* 1166:     */         
/* 1167:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1168:     */         {
/* 1169: 710 */           paramAnonymousAccount2.addToBalanceAndUnconfirmedBalance(paramAnonymousTransaction.amount * 100L);
/* 1170:     */         }
/* 1171:     */         
/* 1172:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1173:     */         {
/* 1174: 715 */           paramAnonymousAccount2.addToBalanceAndUnconfirmedBalance(-paramAnonymousTransaction.amount * 100L);
/* 1175:     */         }
/* 1176:     */         
/* 1177:     */         void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong) {}
/* 1178:     */         
/* 1179:     */         boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
/* 1180:     */         {
/* 1181: 724 */           return false;
/* 1182:     */         }
/* 1183:     */         
/* 1184:     */         private boolean validateAttachment(Transaction paramAnonymousTransaction)
/* 1185:     */         {
/* 1186: 728 */           return (paramAnonymousTransaction.amount > 0) && (paramAnonymousTransaction.amount < 1000000000L);
/* 1187:     */         }
/* 1188:     */       };
/* 1189:     */     }
/* 1190:     */     
/* 1191:     */     public static abstract class Messaging
/* 1192:     */       extends Transaction.Type
/* 1193:     */     {
/* 1194:     */       public final byte getType()
/* 1195:     */       {
/* 1196: 738 */         return 1;
/* 1197:     */       }
/* 1198:     */       
/* 1199:     */       boolean checkDoubleSpending(Transaction paramTransaction, Account paramAccount, int paramInt)
/* 1200:     */       {
/* 1201: 743 */         return false;
/* 1202:     */       }
/* 1203:     */       
/* 1204: 750 */       public static final Transaction.Type ARBITRARY_MESSAGE = new Messaging()
/* 1205:     */       {
/* 1206:     */         public final byte getSubtype()
/* 1207:     */         {
/* 1208: 754 */           return 0;
/* 1209:     */         }
/* 1210:     */         
/* 1211:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/* 1212:     */           throws NxtException.ValidationException
/* 1213:     */         {
/* 1214: 759 */           int i = paramAnonymousByteBuffer.getInt();
/* 1215: 760 */           if (i <= 1000)
/* 1216:     */           {
/* 1217: 761 */             byte[] arrayOfByte = new byte[i];
/* 1218: 762 */             paramAnonymousByteBuffer.get(arrayOfByte);
/* 1219: 763 */             paramAnonymousTransaction.attachment = new Attachment.MessagingArbitraryMessage(arrayOfByte);
/* 1220: 764 */             return validateAttachment(paramAnonymousTransaction);
/* 1221:     */           }
/* 1222: 766 */           return false;
/* 1223:     */         }
/* 1224:     */         
/* 1225:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/* 1226:     */           throws NxtException.ValidationException
/* 1227:     */         {
/* 1228: 771 */           String str = (String)paramAnonymousJSONObject.get("message");
/* 1229: 772 */           paramAnonymousTransaction.attachment = new Attachment.MessagingArbitraryMessage(Convert.convert(str));
/* 1230: 773 */           return validateAttachment(paramAnonymousTransaction);
/* 1231:     */         }
/* 1232:     */         
/* 1233:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2) {}
/* 1234:     */         
/* 1235:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2) {}
/* 1236:     */         
/* 1237:     */         private boolean validateAttachment(Transaction paramAnonymousTransaction)
/* 1238:     */           throws NxtException.ValidationException
/* 1239:     */         {
/* 1240: 783 */           if (Blockchain.getLastBlock().getHeight() < 40000) {
/* 1241: 784 */             throw new Transaction.NotYetEnabledException("Arbitrary messages not yet enabled at height " + Blockchain.getLastBlock().getHeight());
/* 1242:     */           }
/* 1243:     */           try
/* 1244:     */           {
/* 1245: 787 */             Attachment.MessagingArbitraryMessage localMessagingArbitraryMessage = (Attachment.MessagingArbitraryMessage)paramAnonymousTransaction.attachment;
/* 1246: 788 */             return (paramAnonymousTransaction.amount == 0) && (localMessagingArbitraryMessage.getMessage().length <= 1000);
/* 1247:     */           }
/* 1248:     */           catch (RuntimeException localRuntimeException)
/* 1249:     */           {
/* 1250: 790 */             Logger.logDebugMessage("Error validating arbitrary message", localRuntimeException);
/* 1251:     */           }
/* 1252: 791 */           return false;
/* 1253:     */         }
/* 1254:     */       };
/* 1255: 797 */       public static final Transaction.Type ALIAS_ASSIGNMENT = new Messaging()
/* 1256:     */       {
/* 1257:     */         public final byte getSubtype()
/* 1258:     */         {
/* 1259: 801 */           return 1;
/* 1260:     */         }
/* 1261:     */         
/* 1262:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/* 1263:     */           throws NxtException.ValidationException
/* 1264:     */         {
/* 1265: 806 */           int i = paramAnonymousByteBuffer.get();
/* 1266: 807 */           if (i > 300) {
/* 1267: 808 */             throw new NxtException.ValidationException("Max alias length exceeded");
/* 1268:     */           }
/* 1269: 810 */           byte[] arrayOfByte1 = new byte[i];
/* 1270: 811 */           paramAnonymousByteBuffer.get(arrayOfByte1);
/* 1271: 812 */           int j = paramAnonymousByteBuffer.getShort();
/* 1272: 813 */           if (j > 3000) {
/* 1273: 814 */             throw new NxtException.ValidationException("Max alias URI length exceeded");
/* 1274:     */           }
/* 1275: 816 */           byte[] arrayOfByte2 = new byte[j];
/* 1276: 817 */           paramAnonymousByteBuffer.get(arrayOfByte2);
/* 1277:     */           try
/* 1278:     */           {
/* 1279: 819 */             paramAnonymousTransaction.attachment = new Attachment.MessagingAliasAssignment(new String(arrayOfByte1, "UTF-8"), new String(arrayOfByte2, "UTF-8"));
/* 1280:     */             
/* 1281: 821 */             return validateAttachment(paramAnonymousTransaction);
/* 1282:     */           }
/* 1283:     */           catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
/* 1284:     */           {
/* 1285: 823 */             Logger.logDebugMessage("Error parsing alias assignment", localRuntimeException);
/* 1286:     */           }
/* 1287: 825 */           return false;
/* 1288:     */         }
/* 1289:     */         
/* 1290:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/* 1291:     */           throws NxtException.ValidationException
/* 1292:     */         {
/* 1293: 830 */           String str1 = (String)paramAnonymousJSONObject.get("alias");
/* 1294: 831 */           String str2 = (String)paramAnonymousJSONObject.get("uri");
/* 1295: 832 */           paramAnonymousTransaction.attachment = new Attachment.MessagingAliasAssignment(str1, str2);
/* 1296: 833 */           return validateAttachment(paramAnonymousTransaction);
/* 1297:     */         }
/* 1298:     */         
/* 1299:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1300:     */         {
/* 1301: 838 */           Attachment.MessagingAliasAssignment localMessagingAliasAssignment = (Attachment.MessagingAliasAssignment)paramAnonymousTransaction.attachment;
/* 1302: 839 */           Block localBlock = paramAnonymousTransaction.getBlock();
/* 1303: 840 */           Alias.addOrUpdateAlias(paramAnonymousAccount1, paramAnonymousTransaction.getId(), localMessagingAliasAssignment.getAliasName(), localMessagingAliasAssignment.getAliasURI(), localBlock.getTimestamp());
/* 1304:     */         }
/* 1305:     */         
/* 1306:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1307:     */           throws Transaction.UndoNotSupportedException
/* 1308:     */         {
/* 1309: 846 */           throw new Transaction.UndoNotSupportedException(paramAnonymousTransaction, "Reversal of alias assignment not supported");
/* 1310:     */         }
/* 1311:     */         
/* 1312:     */         boolean isDuplicate(Transaction paramAnonymousTransaction, Map<Transaction.Type, Set<String>> paramAnonymousMap)
/* 1313:     */         {
/* 1314: 851 */           Object localObject = (Set)paramAnonymousMap.get(this);
/* 1315: 852 */           if (localObject == null)
/* 1316:     */           {
/* 1317: 853 */             localObject = new HashSet();
/* 1318: 854 */             paramAnonymousMap.put(this, localObject);
/* 1319:     */           }
/* 1320: 856 */           Attachment.MessagingAliasAssignment localMessagingAliasAssignment = (Attachment.MessagingAliasAssignment)paramAnonymousTransaction.attachment;
/* 1321: 857 */           return !((Set)localObject).add(localMessagingAliasAssignment.getAliasName().toLowerCase());
/* 1322:     */         }
/* 1323:     */         
/* 1324:     */         private boolean validateAttachment(Transaction paramAnonymousTransaction)
/* 1325:     */           throws NxtException.ValidationException
/* 1326:     */         {
/* 1327: 861 */           if (Blockchain.getLastBlock().getHeight() < 22000) {
/* 1328: 862 */             throw new Transaction.NotYetEnabledException("Aliases not yet enabled at height " + Blockchain.getLastBlock().getHeight());
/* 1329:     */           }
/* 1330:     */           try
/* 1331:     */           {
/* 1332: 865 */             Attachment.MessagingAliasAssignment localMessagingAliasAssignment = (Attachment.MessagingAliasAssignment)paramAnonymousTransaction.attachment;
/* 1333: 866 */             if ((!Genesis.CREATOR_ID.equals(paramAnonymousTransaction.recipientId)) || (paramAnonymousTransaction.amount != 0) || (localMessagingAliasAssignment.getAliasName().length() == 0) || (localMessagingAliasAssignment.getAliasName().length() > 100) || (localMessagingAliasAssignment.getAliasURI().length() > 1000)) {
/* 1334: 868 */               return false;
/* 1335:     */             }
/* 1336: 870 */             String str = localMessagingAliasAssignment.getAliasName().toLowerCase();
/* 1337: 871 */             for (int i = 0; i < str.length(); i++) {
/* 1338: 872 */               if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(str.charAt(i)) < 0) {
/* 1339: 873 */                 return false;
/* 1340:     */               }
/* 1341:     */             }
/* 1342: 876 */             Alias localAlias = Alias.getAlias(str);
/* 1343: 877 */             return (localAlias == null) || (Arrays.equals(localAlias.getAccount().getPublicKey(), paramAnonymousTransaction.senderPublicKey));
/* 1344:     */           }
/* 1345:     */           catch (RuntimeException localRuntimeException)
/* 1346:     */           {
/* 1347: 880 */             Logger.logDebugMessage("Error in alias assignment validation", localRuntimeException);
/* 1348:     */           }
/* 1349: 881 */           return false;
/* 1350:     */         }
/* 1351:     */       };
/* 1352:     */       
/* 1353:     */       void updateTotals(Transaction paramTransaction, Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1, Long paramLong) {}
/* 1354:     */     }
/* 1355:     */     
/* 1356:     */     public static abstract class ColoredCoins
/* 1357:     */       extends Transaction.Type
/* 1358:     */     {
/* 1359:     */       public final byte getType()
/* 1360:     */       {
/* 1361: 892 */         return 2;
/* 1362:     */       }
/* 1363:     */       
/* 1364: 895 */       public static final Transaction.Type ASSET_ISSUANCE = new ColoredCoins()
/* 1365:     */       {
/* 1366:     */         public final byte getSubtype()
/* 1367:     */         {
/* 1368: 899 */           return 0;
/* 1369:     */         }
/* 1370:     */         
/* 1371:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/* 1372:     */           throws NxtException.ValidationException
/* 1373:     */         {
/* 1374: 904 */           int i = paramAnonymousByteBuffer.get();
/* 1375: 905 */           if (i > 30) {
/* 1376: 906 */             throw new NxtException.ValidationException("Max asset name length exceeded");
/* 1377:     */           }
/* 1378: 908 */           byte[] arrayOfByte1 = new byte[i];
/* 1379: 909 */           paramAnonymousByteBuffer.get(arrayOfByte1);
/* 1380: 910 */           int j = paramAnonymousByteBuffer.getShort();
/* 1381: 911 */           if (j > 300) {
/* 1382: 912 */             throw new NxtException.ValidationException("Max asset description length exceeded");
/* 1383:     */           }
/* 1384: 914 */           byte[] arrayOfByte2 = new byte[j];
/* 1385: 915 */           paramAnonymousByteBuffer.get(arrayOfByte2);
/* 1386: 916 */           int k = paramAnonymousByteBuffer.getInt();
/* 1387:     */           try
/* 1388:     */           {
/* 1389: 918 */             paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAssetIssuance(new String(arrayOfByte1, "UTF-8").intern(), new String(arrayOfByte2, "UTF-8").intern(), k);
/* 1390:     */             
/* 1391: 920 */             return validateAttachment(paramAnonymousTransaction);
/* 1392:     */           }
/* 1393:     */           catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
/* 1394:     */           {
/* 1395: 922 */             Logger.logDebugMessage("Error in asset issuance", localRuntimeException);
/* 1396:     */           }
/* 1397: 924 */           return false;
/* 1398:     */         }
/* 1399:     */         
/* 1400:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/* 1401:     */         {
/* 1402: 929 */           String str1 = (String)paramAnonymousJSONObject.get("name");
/* 1403: 930 */           String str2 = (String)paramAnonymousJSONObject.get("description");
/* 1404: 931 */           int i = ((Long)paramAnonymousJSONObject.get("quantity")).intValue();
/* 1405: 932 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAssetIssuance(str1.trim(), str2.trim(), i);
/* 1406: 933 */           return validateAttachment(paramAnonymousTransaction);
/* 1407:     */         }
/* 1408:     */         
/* 1409:     */         boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
/* 1410:     */         {
/* 1411: 938 */           return false;
/* 1412:     */         }
/* 1413:     */         
/* 1414:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1415:     */         {
/* 1416: 943 */           Attachment.ColoredCoinsAssetIssuance localColoredCoinsAssetIssuance = (Attachment.ColoredCoinsAssetIssuance)paramAnonymousTransaction.attachment;
/* 1417: 944 */           Long localLong = paramAnonymousTransaction.getId();
/* 1418: 945 */           Asset.addAsset(localLong, paramAnonymousTransaction.getSenderAccountId(), localColoredCoinsAssetIssuance.getName(), localColoredCoinsAssetIssuance.getDescription(), localColoredCoinsAssetIssuance.getQuantity());
/* 1419: 946 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localLong, localColoredCoinsAssetIssuance.getQuantity());
/* 1420:     */         }
/* 1421:     */         
/* 1422:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1423:     */         {
/* 1424: 951 */           Attachment.ColoredCoinsAssetIssuance localColoredCoinsAssetIssuance = (Attachment.ColoredCoinsAssetIssuance)paramAnonymousTransaction.attachment;
/* 1425: 952 */           Long localLong = paramAnonymousTransaction.getId();
/* 1426: 953 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localLong, -localColoredCoinsAssetIssuance.getQuantity());
/* 1427: 954 */           Asset.removeAsset(localLong);
/* 1428:     */         }
/* 1429:     */         
/* 1430:     */         void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong) {}
/* 1431:     */         
/* 1432:     */         private boolean validateAttachment(Transaction paramAnonymousTransaction)
/* 1433:     */         {
/* 1434:     */           try
/* 1435:     */           {
/* 1436: 963 */             Attachment.ColoredCoinsAssetIssuance localColoredCoinsAssetIssuance = (Attachment.ColoredCoinsAssetIssuance)paramAnonymousTransaction.attachment;
/* 1437: 964 */             if ((!Genesis.CREATOR_ID.equals(paramAnonymousTransaction.recipientId)) || (paramAnonymousTransaction.amount != 0) || (paramAnonymousTransaction.fee < 1000) || (localColoredCoinsAssetIssuance.getName().length() < 3) || (localColoredCoinsAssetIssuance.getName().length() > 10) || (localColoredCoinsAssetIssuance.getDescription().length() > 1000) || (localColoredCoinsAssetIssuance.getQuantity() <= 0) || (localColoredCoinsAssetIssuance.getQuantity() > 1000000000L)) {
/* 1438: 967 */               return false;
/* 1439:     */             }
/* 1440: 969 */             String str = localColoredCoinsAssetIssuance.getName().toLowerCase();
/* 1441: 970 */             for (int i = 0; i < str.length(); i++) {
/* 1442: 971 */               if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(str.charAt(i)) < 0) {
/* 1443: 972 */                 return false;
/* 1444:     */               }
/* 1445:     */             }
/* 1446: 975 */             return Asset.getAsset(str) == null;
/* 1447:     */           }
/* 1448:     */           catch (RuntimeException localRuntimeException)
/* 1449:     */           {
/* 1450: 978 */             Logger.logDebugMessage("Error validating colored coins asset issuance", localRuntimeException);
/* 1451:     */           }
/* 1452: 979 */           return false;
/* 1453:     */         }
/* 1454:     */       };
/* 1455: 985 */       public static final Transaction.Type ASSET_TRANSFER = new ColoredCoins()
/* 1456:     */       {
/* 1457:     */         public final byte getSubtype()
/* 1458:     */         {
/* 1459: 989 */           return 1;
/* 1460:     */         }
/* 1461:     */         
/* 1462:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/* 1463:     */         {
/* 1464: 994 */           Long localLong = Convert.zeroToNull(paramAnonymousByteBuffer.getLong());
/* 1465: 995 */           int i = paramAnonymousByteBuffer.getInt();
/* 1466: 996 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAssetTransfer(localLong, i);
/* 1467: 997 */           return validateAttachment(paramAnonymousTransaction);
/* 1468:     */         }
/* 1469:     */         
/* 1470:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/* 1471:     */         {
/* 1472:1002 */           Long localLong = Convert.parseUnsignedLong((String)paramAnonymousJSONObject.get("asset"));
/* 1473:1003 */           int i = ((Long)paramAnonymousJSONObject.get("quantity")).intValue();
/* 1474:1004 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAssetTransfer(localLong, i);
/* 1475:1005 */           return validateAttachment(paramAnonymousTransaction);
/* 1476:     */         }
/* 1477:     */         
/* 1478:     */         boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
/* 1479:     */         {
/* 1480:1010 */           Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
/* 1481:1011 */           Integer localInteger = paramAnonymousAccount.getUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId());
/* 1482:1012 */           if ((localInteger == null) || (localInteger.intValue() < localColoredCoinsAssetTransfer.getQuantity()))
/* 1483:     */           {
/* 1484:1013 */             paramAnonymousAccount.addToUnconfirmedBalance(paramAnonymousInt * 100L);
/* 1485:1014 */             return true;
/* 1486:     */           }
/* 1487:1016 */           paramAnonymousAccount.addToUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), -localColoredCoinsAssetTransfer.getQuantity());
/* 1488:1017 */           return false;
/* 1489:     */         }
/* 1490:     */         
/* 1491:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1492:     */         {
/* 1493:1023 */           Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
/* 1494:1024 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), -localColoredCoinsAssetTransfer.getQuantity());
/* 1495:1025 */           paramAnonymousAccount2.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), localColoredCoinsAssetTransfer.getQuantity());
/* 1496:     */         }
/* 1497:     */         
/* 1498:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1499:     */         {
/* 1500:1030 */           Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
/* 1501:1031 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), localColoredCoinsAssetTransfer.getQuantity());
/* 1502:1032 */           paramAnonymousAccount2.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), -localColoredCoinsAssetTransfer.getQuantity());
/* 1503:     */         }
/* 1504:     */         
/* 1505:     */         void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong)
/* 1506:     */         {
/* 1507:1038 */           Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
/* 1508:1039 */           Object localObject = (Map)paramAnonymousMap1.get(paramAnonymousTransaction.getSenderAccountId());
/* 1509:1040 */           if (localObject == null)
/* 1510:     */           {
/* 1511:1041 */             localObject = new HashMap();
/* 1512:1042 */             paramAnonymousMap1.put(paramAnonymousTransaction.getSenderAccountId(), localObject);
/* 1513:     */           }
/* 1514:1044 */           Long localLong = (Long)((Map)localObject).get(localColoredCoinsAssetTransfer.getAssetId());
/* 1515:1045 */           if (localLong == null) {
/* 1516:1046 */             localLong = Long.valueOf(0L);
/* 1517:     */           }
/* 1518:1048 */           ((Map)localObject).put(localColoredCoinsAssetTransfer.getAssetId(), Long.valueOf(localLong.longValue() + localColoredCoinsAssetTransfer.getQuantity()));
/* 1519:     */         }
/* 1520:     */         
/* 1521:     */         private boolean validateAttachment(Transaction paramAnonymousTransaction)
/* 1522:     */         {
/* 1523:1052 */           Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
/* 1524:1053 */           return (paramAnonymousTransaction.amount == 0) && (localColoredCoinsAssetTransfer.getQuantity() > 0) && (localColoredCoinsAssetTransfer.getQuantity() <= 1000000000L);
/* 1525:     */         }
/* 1526:     */       };
/* 1527:     */       
/* 1528:     */       static abstract class ColoredCoinsOrderPlacement
/* 1529:     */         extends Transaction.Type.ColoredCoins
/* 1530:     */       {
/* 1531:     */         abstract Attachment.ColoredCoinsOrderPlacement makeAttachment(Long paramLong, int paramInt, long paramLong1);
/* 1532:     */         
/* 1533:     */         final boolean loadAttachment(Transaction paramTransaction, ByteBuffer paramByteBuffer)
/* 1534:     */         {
/* 1535:1064 */           Long localLong = Convert.zeroToNull(paramByteBuffer.getLong());
/* 1536:1065 */           int i = paramByteBuffer.getInt();
/* 1537:1066 */           long l = paramByteBuffer.getLong();
/* 1538:1067 */           paramTransaction.attachment = makeAttachment(localLong, i, l);
/* 1539:1068 */           return validateAttachment(paramTransaction);
/* 1540:     */         }
/* 1541:     */         
/* 1542:     */         final boolean loadAttachment(Transaction paramTransaction, JSONObject paramJSONObject)
/* 1543:     */         {
/* 1544:1073 */           Long localLong = Convert.parseUnsignedLong((String)paramJSONObject.get("asset"));
/* 1545:1074 */           int i = ((Long)paramJSONObject.get("quantity")).intValue();
/* 1546:1075 */           long l = ((Long)paramJSONObject.get("price")).longValue();
/* 1547:1076 */           paramTransaction.attachment = makeAttachment(localLong, i, l);
/* 1548:1077 */           return validateAttachment(paramTransaction);
/* 1549:     */         }
/* 1550:     */         
/* 1551:     */         private boolean validateAttachment(Transaction paramTransaction)
/* 1552:     */         {
/* 1553:1081 */           Attachment.ColoredCoinsOrderPlacement localColoredCoinsOrderPlacement = (Attachment.ColoredCoinsOrderPlacement)paramTransaction.attachment;
/* 1554:1082 */           return (Genesis.CREATOR_ID.equals(paramTransaction.recipientId)) && (paramTransaction.amount == 0) && (localColoredCoinsOrderPlacement.getQuantity() > 0) && (localColoredCoinsOrderPlacement.getQuantity() <= 1000000000L) && (localColoredCoinsOrderPlacement.getPrice() > 0L) && (localColoredCoinsOrderPlacement.getPrice() <= 100000000000L);
/* 1555:     */         }
/* 1556:     */       }
/* 1557:     */       
/* 1558:1089 */       public static final Transaction.Type ASK_ORDER_PLACEMENT = new ColoredCoinsOrderPlacement()
/* 1559:     */       {
/* 1560:     */         public final byte getSubtype()
/* 1561:     */         {
/* 1562:1093 */           return 2;
/* 1563:     */         }
/* 1564:     */         
/* 1565:     */         final Attachment.ColoredCoinsOrderPlacement makeAttachment(Long paramAnonymousLong, int paramAnonymousInt, long paramAnonymousLong1)
/* 1566:     */         {
/* 1567:1097 */           return new Attachment.ColoredCoinsAskOrderPlacement(paramAnonymousLong, paramAnonymousInt, paramAnonymousLong1);
/* 1568:     */         }
/* 1569:     */         
/* 1570:     */         boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
/* 1571:     */         {
/* 1572:1102 */           Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = (Attachment.ColoredCoinsAskOrderPlacement)paramAnonymousTransaction.attachment;
/* 1573:1103 */           Integer localInteger = paramAnonymousAccount.getUnconfirmedAssetBalance(localColoredCoinsAskOrderPlacement.getAssetId());
/* 1574:1104 */           if ((localInteger == null) || (localInteger.intValue() < localColoredCoinsAskOrderPlacement.getQuantity()))
/* 1575:     */           {
/* 1576:1105 */             paramAnonymousAccount.addToUnconfirmedBalance(paramAnonymousInt * 100L);
/* 1577:1106 */             return true;
/* 1578:     */           }
/* 1579:1108 */           paramAnonymousAccount.addToUnconfirmedAssetBalance(localColoredCoinsAskOrderPlacement.getAssetId(), -localColoredCoinsAskOrderPlacement.getQuantity());
/* 1580:1109 */           return false;
/* 1581:     */         }
/* 1582:     */         
/* 1583:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1584:     */         {
/* 1585:1115 */           Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = (Attachment.ColoredCoinsAskOrderPlacement)paramAnonymousTransaction.attachment;
/* 1586:1116 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAskOrderPlacement.getAssetId(), -localColoredCoinsAskOrderPlacement.getQuantity());
/* 1587:1117 */           Order.Ask.addOrder(paramAnonymousTransaction.getId(), paramAnonymousAccount1, localColoredCoinsAskOrderPlacement.getAssetId(), localColoredCoinsAskOrderPlacement.getQuantity(), localColoredCoinsAskOrderPlacement.getPrice());
/* 1588:     */         }
/* 1589:     */         
/* 1590:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1591:     */           throws Transaction.UndoNotSupportedException
/* 1592:     */         {
/* 1593:1122 */           Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = (Attachment.ColoredCoinsAskOrderPlacement)paramAnonymousTransaction.attachment;
/* 1594:1123 */           Order.Ask localAsk = Order.Ask.removeOrder(paramAnonymousTransaction.getId());
/* 1595:1124 */           if ((localAsk == null) || (localAsk.getQuantity() != localColoredCoinsAskOrderPlacement.getQuantity()) || (!localAsk.getAssetId().equals(localColoredCoinsAskOrderPlacement.getAssetId()))) {
/* 1596:1126 */             throw new Transaction.UndoNotSupportedException(paramAnonymousTransaction, "Ask order already filled");
/* 1597:     */           }
/* 1598:1128 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAskOrderPlacement.getAssetId(), localColoredCoinsAskOrderPlacement.getQuantity());
/* 1599:     */         }
/* 1600:     */         
/* 1601:     */         void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong)
/* 1602:     */         {
/* 1603:1134 */           Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = (Attachment.ColoredCoinsAskOrderPlacement)paramAnonymousTransaction.attachment;
/* 1604:1135 */           Object localObject = (Map)paramAnonymousMap1.get(paramAnonymousTransaction.getSenderAccountId());
/* 1605:1136 */           if (localObject == null)
/* 1606:     */           {
/* 1607:1137 */             localObject = new HashMap();
/* 1608:1138 */             paramAnonymousMap1.put(paramAnonymousTransaction.getSenderAccountId(), localObject);
/* 1609:     */           }
/* 1610:1140 */           Long localLong = (Long)((Map)localObject).get(localColoredCoinsAskOrderPlacement.getAssetId());
/* 1611:1141 */           if (localLong == null) {
/* 1612:1142 */             localLong = Long.valueOf(0L);
/* 1613:     */           }
/* 1614:1144 */           ((Map)localObject).put(localColoredCoinsAskOrderPlacement.getAssetId(), Long.valueOf(localLong.longValue() + localColoredCoinsAskOrderPlacement.getQuantity()));
/* 1615:     */         }
/* 1616:     */       };
/* 1617:1149 */       public static final Transaction.Type BID_ORDER_PLACEMENT = new ColoredCoinsOrderPlacement()
/* 1618:     */       {
/* 1619:     */         public final byte getSubtype()
/* 1620:     */         {
/* 1621:1153 */           return 3;
/* 1622:     */         }
/* 1623:     */         
/* 1624:     */         final Attachment.ColoredCoinsOrderPlacement makeAttachment(Long paramAnonymousLong, int paramAnonymousInt, long paramAnonymousLong1)
/* 1625:     */         {
/* 1626:1157 */           return new Attachment.ColoredCoinsBidOrderPlacement(paramAnonymousLong, paramAnonymousInt, paramAnonymousLong1);
/* 1627:     */         }
/* 1628:     */         
/* 1629:     */         boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
/* 1630:     */         {
/* 1631:1162 */           Attachment.ColoredCoinsBidOrderPlacement localColoredCoinsBidOrderPlacement = (Attachment.ColoredCoinsBidOrderPlacement)paramAnonymousTransaction.attachment;
/* 1632:1163 */           if (paramAnonymousAccount.getUnconfirmedBalance() < localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice())
/* 1633:     */           {
/* 1634:1164 */             paramAnonymousAccount.addToUnconfirmedBalance(paramAnonymousInt * 100L);
/* 1635:1165 */             return true;
/* 1636:     */           }
/* 1637:1167 */           paramAnonymousAccount.addToUnconfirmedBalance(-localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice());
/* 1638:1168 */           return false;
/* 1639:     */         }
/* 1640:     */         
/* 1641:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1642:     */         {
/* 1643:1174 */           Attachment.ColoredCoinsBidOrderPlacement localColoredCoinsBidOrderPlacement = (Attachment.ColoredCoinsBidOrderPlacement)paramAnonymousTransaction.attachment;
/* 1644:1175 */           paramAnonymousAccount1.addToBalanceAndUnconfirmedBalance(-localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice());
/* 1645:1176 */           Order.Bid.addOrder(paramAnonymousTransaction.getId(), paramAnonymousAccount1, localColoredCoinsBidOrderPlacement.getAssetId(), localColoredCoinsBidOrderPlacement.getQuantity(), localColoredCoinsBidOrderPlacement.getPrice());
/* 1646:     */         }
/* 1647:     */         
/* 1648:     */         void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1649:     */           throws Transaction.UndoNotSupportedException
/* 1650:     */         {
/* 1651:1181 */           Attachment.ColoredCoinsBidOrderPlacement localColoredCoinsBidOrderPlacement = (Attachment.ColoredCoinsBidOrderPlacement)paramAnonymousTransaction.attachment;
/* 1652:1182 */           Order.Bid localBid = Order.Bid.removeOrder(paramAnonymousTransaction.getId());
/* 1653:1183 */           if ((localBid == null) || (localBid.getQuantity() != localColoredCoinsBidOrderPlacement.getQuantity()) || (!localBid.getAssetId().equals(localColoredCoinsBidOrderPlacement.getAssetId()))) {
/* 1654:1185 */             throw new Transaction.UndoNotSupportedException(paramAnonymousTransaction, "Bid order already filled");
/* 1655:     */           }
/* 1656:1187 */           paramAnonymousAccount1.addToBalanceAndUnconfirmedBalance(localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice());
/* 1657:     */         }
/* 1658:     */         
/* 1659:     */         void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong)
/* 1660:     */         {
/* 1661:1193 */           Attachment.ColoredCoinsBidOrderPlacement localColoredCoinsBidOrderPlacement = (Attachment.ColoredCoinsBidOrderPlacement)paramAnonymousTransaction.attachment;
/* 1662:1194 */           paramAnonymousMap.put(paramAnonymousTransaction.getSenderAccountId(), Long.valueOf(paramAnonymousLong.longValue() + localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice()));
/* 1663:     */         }
/* 1664:     */       };
/* 1665:     */       
/* 1666:     */       static abstract class ColoredCoinsOrderCancellation
/* 1667:     */         extends Transaction.Type.ColoredCoins
/* 1668:     */       {
/* 1669:     */         final boolean validateAttachment(Transaction paramTransaction)
/* 1670:     */         {
/* 1671:1215 */           return (Genesis.CREATOR_ID.equals(paramTransaction.recipientId)) && (paramTransaction.amount == 0);
/* 1672:     */         }
/* 1673:     */         
/* 1674:     */         final boolean checkDoubleSpending(Transaction paramTransaction, Account paramAccount, int paramInt)
/* 1675:     */         {
/* 1676:1220 */           return false;
/* 1677:     */         }
/* 1678:     */         
/* 1679:     */         final void updateTotals(Transaction paramTransaction, Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1, Long paramLong) {}
/* 1680:     */         
/* 1681:     */         final void undo(Transaction paramTransaction, Account paramAccount1, Account paramAccount2)
/* 1682:     */           throws Transaction.UndoNotSupportedException
/* 1683:     */         {
/* 1684:1229 */           throw new Transaction.UndoNotSupportedException(paramTransaction, "Reversal of order cancellation not supported");
/* 1685:     */         }
/* 1686:     */       }
/* 1687:     */       
/* 1688:1234 */       public static final Transaction.Type ASK_ORDER_CANCELLATION = new ColoredCoinsOrderCancellation()
/* 1689:     */       {
/* 1690:     */         public final byte getSubtype()
/* 1691:     */         {
/* 1692:1238 */           return 4;
/* 1693:     */         }
/* 1694:     */         
/* 1695:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/* 1696:     */         {
/* 1697:1243 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(Convert.zeroToNull(paramAnonymousByteBuffer.getLong()));
/* 1698:1244 */           return validateAttachment(paramAnonymousTransaction);
/* 1699:     */         }
/* 1700:     */         
/* 1701:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/* 1702:     */         {
/* 1703:1249 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(Convert.parseUnsignedLong((String)paramAnonymousJSONObject.get("order")));
/* 1704:1250 */           return validateAttachment(paramAnonymousTransaction);
/* 1705:     */         }
/* 1706:     */         
/* 1707:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1708:     */         {
/* 1709:1255 */           Attachment.ColoredCoinsAskOrderCancellation localColoredCoinsAskOrderCancellation = (Attachment.ColoredCoinsAskOrderCancellation)paramAnonymousTransaction.attachment;
/* 1710:1256 */           Order.Ask localAsk = Order.Ask.removeOrder(localColoredCoinsAskOrderCancellation.getOrderId());
/* 1711:1257 */           paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localAsk.getAssetId(), localAsk.getQuantity());
/* 1712:     */         }
/* 1713:     */       };
/* 1714:1262 */       public static final Transaction.Type BID_ORDER_CANCELLATION = new ColoredCoinsOrderCancellation()
/* 1715:     */       {
/* 1716:     */         public final byte getSubtype()
/* 1717:     */         {
/* 1718:1266 */           return 5;
/* 1719:     */         }
/* 1720:     */         
/* 1721:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
/* 1722:     */         {
/* 1723:1271 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(Convert.zeroToNull(paramAnonymousByteBuffer.getLong()));
/* 1724:1272 */           return validateAttachment(paramAnonymousTransaction);
/* 1725:     */         }
/* 1726:     */         
/* 1727:     */         boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
/* 1728:     */         {
/* 1729:1277 */           paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(Convert.parseUnsignedLong((String)paramAnonymousJSONObject.get("order")));
/* 1730:1278 */           return validateAttachment(paramAnonymousTransaction);
/* 1731:     */         }
/* 1732:     */         
/* 1733:     */         void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
/* 1734:     */         {
/* 1735:1283 */           Attachment.ColoredCoinsBidOrderCancellation localColoredCoinsBidOrderCancellation = (Attachment.ColoredCoinsBidOrderCancellation)paramAnonymousTransaction.attachment;
/* 1736:1284 */           Order.Bid localBid = Order.Bid.removeOrder(localColoredCoinsBidOrderCancellation.getOrderId());
/* 1737:1285 */           paramAnonymousAccount1.addToBalanceAndUnconfirmedBalance(localBid.getQuantity() * localBid.getPrice());
/* 1738:     */         }
/* 1739:     */       };
/* 1740:     */     }
/* 1741:     */   }
/* 1742:     */   
/* 1743:     */   public static final class UndoNotSupportedException
/* 1744:     */     extends NxtException
/* 1745:     */   {
/* 1746:     */     private final Transaction transaction;
/* 1747:     */     
/* 1748:     */     public UndoNotSupportedException(Transaction paramTransaction, String paramString)
/* 1749:     */     {
/* 1750:1313 */       super();
/* 1751:1314 */       this.transaction = paramTransaction;
/* 1752:     */     }
/* 1753:     */     
/* 1754:     */     public Transaction getTransaction()
/* 1755:     */     {
/* 1756:1318 */       return this.transaction;
/* 1757:     */     }
/* 1758:     */   }
/* 1759:     */   
/* 1760:     */   public static final class NotYetEnabledException
/* 1761:     */     extends NxtException.ValidationException
/* 1762:     */   {
/* 1763:     */     public NotYetEnabledException(String paramString)
/* 1764:     */     {
/* 1765:1326 */       super();
/* 1766:     */     }
/* 1767:     */     
/* 1768:     */     public NotYetEnabledException(String paramString, Throwable paramThrowable)
/* 1769:     */     {
/* 1770:1330 */       super(paramThrowable);
/* 1771:     */     }
/* 1772:     */   }
/* 1773:     */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Transaction
 * JD-Core Version:    0.7.0.1
 */