/*   1:    */ package nxt;
/*   2:    */ 
/*   3:    */ import java.math.BigInteger;
/*   4:    */ import java.security.MessageDigest;
/*   5:    */ import java.util.ArrayList;
/*   6:    */ import java.util.Arrays;
/*   7:    */ import java.util.Collection;
/*   8:    */ import java.util.Collections;
/*   9:    */ import java.util.HashMap;
/*  10:    */ import java.util.Iterator;
/*  11:    */ import java.util.List;
/*  12:    */ import java.util.Map;
/*  13:    */ import java.util.concurrent.ConcurrentHashMap;
/*  14:    */ import java.util.concurrent.ConcurrentMap;
/*  15:    */ import java.util.concurrent.atomic.AtomicReference;
/*  16:    */ import nxt.crypto.Crypto;
/*  17:    */ import nxt.peer.Peer;
/*  18:    */ import nxt.user.User;
/*  19:    */ 
/*  20:    */ public final class Account
/*  21:    */ {
/*  22:    */   private static final int maxTrackedBalanceConfirmations = 2881;
/*  23: 23 */   private static final ConcurrentMap<Long, Account> accounts = new ConcurrentHashMap();
/*  24: 25 */   private static final Collection<Account> allAccounts = Collections.unmodifiableCollection(accounts.values());
/*  25:    */   private final Long id;
/*  26:    */   private final int height;
/*  27:    */   
/*  28:    */   public static Collection<Account> getAllAccounts()
/*  29:    */   {
/*  30: 28 */     return allAccounts;
/*  31:    */   }
/*  32:    */   
/*  33:    */   public static Account getAccount(Long paramLong)
/*  34:    */   {
/*  35: 32 */     return (Account)accounts.get(paramLong);
/*  36:    */   }
/*  37:    */   
/*  38:    */   public static Account getAccount(byte[] paramArrayOfByte)
/*  39:    */   {
/*  40: 36 */     return (Account)accounts.get(getId(paramArrayOfByte));
/*  41:    */   }
/*  42:    */   
/*  43:    */   public static Long getId(byte[] paramArrayOfByte)
/*  44:    */   {
/*  45: 40 */     byte[] arrayOfByte = Crypto.sha256().digest(paramArrayOfByte);
/*  46: 41 */     BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
/*  47:    */     
/*  48: 43 */     return Long.valueOf(localBigInteger.longValue());
/*  49:    */   }
/*  50:    */   
/*  51:    */   static Account addOrGetAccount(Long paramLong)
/*  52:    */   {
/*  53: 47 */     Account localAccount1 = new Account(paramLong);
/*  54: 48 */     Account localAccount2 = (Account)accounts.putIfAbsent(paramLong, localAccount1);
/*  55: 49 */     return localAccount2 != null ? localAccount2 : localAccount1;
/*  56:    */   }
/*  57:    */   
/*  58:    */   static void clear()
/*  59:    */   {
/*  60: 53 */     accounts.clear();
/*  61:    */   }
/*  62:    */   
/*  63: 58 */   private final AtomicReference<byte[]> publicKey = new AtomicReference();
/*  64:    */   private long balance;
/*  65:    */   private long unconfirmedBalance;
/*  66: 61 */   private final List<GuaranteedBalance> guaranteedBalances = new ArrayList();
/*  67: 63 */   private final Map<Long, Integer> assetBalances = new HashMap();
/*  68: 64 */   private final Map<Long, Integer> unconfirmedAssetBalances = new HashMap();
/*  69:    */   
/*  70:    */   private Account(Long paramLong)
/*  71:    */   {
/*  72: 67 */     this.id = paramLong;
/*  73: 68 */     this.height = Blockchain.getLastBlock().getHeight();
/*  74:    */   }
/*  75:    */   
/*  76:    */   public Long getId()
/*  77:    */   {
/*  78: 72 */     return this.id;
/*  79:    */   }
/*  80:    */   
/*  81:    */   public byte[] getPublicKey()
/*  82:    */   {
/*  83: 76 */     return (byte[])this.publicKey.get();
/*  84:    */   }
/*  85:    */   
/*  86:    */   public synchronized long getBalance()
/*  87:    */   {
/*  88: 80 */     return this.balance;
/*  89:    */   }
/*  90:    */   
/*  91:    */   public synchronized long getUnconfirmedBalance()
/*  92:    */   {
/*  93: 84 */     return this.unconfirmedBalance;
/*  94:    */   }
/*  95:    */   
/*  96:    */   public int getEffectiveBalance()
/*  97:    */   {
/*  98: 89 */     Block localBlock = Blockchain.getLastBlock();
/*  99: 90 */     if ((localBlock.getHeight() < 51000) && (this.height < 47000))
/* 100:    */     {
/* 101: 92 */       if (this.height == 0) {
/* 102: 93 */         return (int)(getBalance() / 100L);
/* 103:    */       }
/* 104: 95 */       if (localBlock.getHeight() - this.height < 1440) {
/* 105: 96 */         return 0;
/* 106:    */       }
/* 107: 98 */       int i = 0;
/* 108: 99 */       for (Transaction localTransaction : localBlock.blockTransactions) {
/* 109:100 */         if (localTransaction.getRecipientId().equals(this.id)) {
/* 110:101 */           i += localTransaction.getAmount();
/* 111:    */         }
/* 112:    */       }
/* 113:104 */       return (int)(getBalance() / 100L) - i;
/* 114:    */     }
/* 115:107 */     return (int)(getGuaranteedBalance(1440) / 100L);
/* 116:    */   }
/* 117:    */   
/* 118:    */   public synchronized long getGuaranteedBalance(int paramInt)
/* 119:    */   {
/* 120:113 */     if ((paramInt > 2881) || (paramInt >= Blockchain.getLastBlock().getHeight()) || (paramInt < 0)) {
/* 121:114 */       throw new IllegalArgumentException("Number of required confirmations must be between 0 and 2881");
/* 122:    */     }
/* 123:116 */     if (this.guaranteedBalances.isEmpty()) {
/* 124:117 */       return 0L;
/* 125:    */     }
/* 126:119 */     int i = Collections.binarySearch(this.guaranteedBalances, new GuaranteedBalance(Blockchain.getLastBlock().getHeight() - paramInt, 0L, null));
/* 127:120 */     if (i == -1) {
/* 128:121 */       return 0L;
/* 129:    */     }
/* 130:123 */     if (i < -1) {
/* 131:124 */       i = -i - 2;
/* 132:    */     }
/* 133:126 */     if (i > this.guaranteedBalances.size() - 1) {
/* 134:127 */       i = this.guaranteedBalances.size() - 1;
/* 135:    */     }
/* 136:    */     GuaranteedBalance localGuaranteedBalance;
/* 137:130 */     while (((localGuaranteedBalance = (GuaranteedBalance)this.guaranteedBalances.get(i)).ignore) && (i > 0)) {
/* 138:131 */       i--;
/* 139:    */     }
/* 140:133 */     return localGuaranteedBalance.ignore ? 0L : localGuaranteedBalance.balance;
/* 141:    */   }
/* 142:    */   
/* 143:    */   public synchronized Integer getUnconfirmedAssetBalance(Long paramLong)
/* 144:    */   {
/* 145:138 */     return (Integer)this.unconfirmedAssetBalances.get(paramLong);
/* 146:    */   }
/* 147:    */   
/* 148:    */   public Map<Long, Integer> getAssetBalances()
/* 149:    */   {
/* 150:142 */     return Collections.unmodifiableMap(this.assetBalances);
/* 151:    */   }
/* 152:    */   
/* 153:    */   public boolean equals(Object paramObject)
/* 154:    */   {
/* 155:147 */     return ((paramObject instanceof Account)) && (getId().equals(((Account)paramObject).getId()));
/* 156:    */   }
/* 157:    */   
/* 158:    */   public int hashCode()
/* 159:    */   {
/* 160:152 */     return getId().hashCode();
/* 161:    */   }
/* 162:    */   
/* 163:    */   boolean setOrVerify(byte[] paramArrayOfByte)
/* 164:    */   {
/* 165:160 */     return (this.publicKey.compareAndSet(null, paramArrayOfByte)) || (Arrays.equals(paramArrayOfByte, (byte[])this.publicKey.get()));
/* 166:    */   }
/* 167:    */   
/* 168:    */   synchronized Integer getAssetBalance(Long paramLong)
/* 169:    */   {
/* 170:164 */     return (Integer)this.assetBalances.get(paramLong);
/* 171:    */   }
/* 172:    */   
/* 173:    */   synchronized void addToAssetBalance(Long paramLong, int paramInt)
/* 174:    */   {
/* 175:168 */     Integer localInteger = (Integer)this.assetBalances.get(paramLong);
/* 176:169 */     if (localInteger == null) {
/* 177:170 */       this.assetBalances.put(paramLong, Integer.valueOf(paramInt));
/* 178:    */     } else {
/* 179:172 */       this.assetBalances.put(paramLong, Integer.valueOf(localInteger.intValue() + paramInt));
/* 180:    */     }
/* 181:    */   }
/* 182:    */   
/* 183:    */   synchronized void addToUnconfirmedAssetBalance(Long paramLong, int paramInt)
/* 184:    */   {
/* 185:177 */     Integer localInteger = (Integer)this.unconfirmedAssetBalances.get(paramLong);
/* 186:178 */     if (localInteger == null) {
/* 187:179 */       this.unconfirmedAssetBalances.put(paramLong, Integer.valueOf(paramInt));
/* 188:    */     } else {
/* 189:181 */       this.unconfirmedAssetBalances.put(paramLong, Integer.valueOf(localInteger.intValue() + paramInt));
/* 190:    */     }
/* 191:    */   }
/* 192:    */   
/* 193:    */   synchronized void addToAssetAndUnconfirmedAssetBalance(Long paramLong, int paramInt)
/* 194:    */   {
/* 195:186 */     Integer localInteger = (Integer)this.assetBalances.get(paramLong);
/* 196:187 */     if (localInteger == null)
/* 197:    */     {
/* 198:188 */       this.assetBalances.put(paramLong, Integer.valueOf(paramInt));
/* 199:189 */       this.unconfirmedAssetBalances.put(paramLong, Integer.valueOf(paramInt));
/* 200:    */     }
/* 201:    */     else
/* 202:    */     {
/* 203:191 */       this.assetBalances.put(paramLong, Integer.valueOf(localInteger.intValue() + paramInt));
/* 204:192 */       this.unconfirmedAssetBalances.put(paramLong, Integer.valueOf(((Integer)this.unconfirmedAssetBalances.get(paramLong)).intValue() + paramInt));
/* 205:    */     }
/* 206:    */   }
/* 207:    */   
/* 208:    */   void addToBalance(long paramLong)
/* 209:    */   {
/* 210:197 */     synchronized (this)
/* 211:    */     {
/* 212:198 */       this.balance += paramLong;
/* 213:199 */       addToGuaranteedBalance(paramLong);
/* 214:    */     }
/* 215:201 */     Peer.updatePeerWeights(this);
/* 216:    */   }
/* 217:    */   
/* 218:    */   void addToUnconfirmedBalance(long paramLong)
/* 219:    */   {
/* 220:205 */     synchronized (this)
/* 221:    */     {
/* 222:206 */       this.unconfirmedBalance += paramLong;
/* 223:    */     }
/* 224:208 */     User.updateUserUnconfirmedBalance(this);
/* 225:    */   }
/* 226:    */   
/* 227:    */   void addToBalanceAndUnconfirmedBalance(long paramLong)
/* 228:    */   {
/* 229:212 */     synchronized (this)
/* 230:    */     {
/* 231:213 */       this.balance += paramLong;
/* 232:214 */       this.unconfirmedBalance += paramLong;
/* 233:215 */       addToGuaranteedBalance(paramLong);
/* 234:    */     }
/* 235:217 */     Peer.updatePeerWeights(this);
/* 236:218 */     User.updateUserUnconfirmedBalance(this);
/* 237:    */   }
/* 238:    */   
/* 239:    */   private synchronized void addToGuaranteedBalance(long paramLong)
/* 240:    */   {
/* 241:222 */     int i = Blockchain.getLastBlock().getHeight();
/* 242:223 */     GuaranteedBalance localGuaranteedBalance1 = null;
/* 243:224 */     if ((this.guaranteedBalances.size() > 0) && ((localGuaranteedBalance1 = (GuaranteedBalance)this.guaranteedBalances.get(this.guaranteedBalances.size() - 1)).height > i))
/* 244:    */     {
/* 245:226 */       if (paramLong > 0L)
/* 246:    */       {
/* 247:228 */         Iterator localIterator1 = this.guaranteedBalances.iterator();
/* 248:229 */         while (localIterator1.hasNext())
/* 249:    */         {
/* 250:230 */           GuaranteedBalance localGuaranteedBalance2 = (GuaranteedBalance)localIterator1.next();
/* 251:231 */           localGuaranteedBalance2.balance += paramLong;
/* 252:    */         }
/* 253:    */       }
/* 254:234 */       localGuaranteedBalance1.ignore = true;
/* 255:235 */       return;
/* 256:    */     }
/* 257:237 */     int j = 0;
/* 258:238 */     for (int k = 0; k < this.guaranteedBalances.size(); k++)
/* 259:    */     {
/* 260:239 */       GuaranteedBalance localGuaranteedBalance3 = (GuaranteedBalance)this.guaranteedBalances.get(k);
/* 261:240 */       if ((localGuaranteedBalance3.height < i - 2881) && (k < this.guaranteedBalances.size() - 1) && (((GuaranteedBalance)this.guaranteedBalances.get(k + 1)).height >= i - 2881))
/* 262:    */       {
/* 263:243 */         j = k;
/* 264:244 */         if (i >= 64000) {
/* 265:245 */           localGuaranteedBalance3.balance += paramLong;
/* 266:    */         }
/* 267:    */       }
/* 268:247 */       else if (paramLong < 0L)
/* 269:    */       {
/* 270:248 */         localGuaranteedBalance3.balance += paramLong;
/* 271:    */       }
/* 272:    */     }
/* 273:252 */     if (j > 0)
/* 274:    */     {
/* 275:253 */       Iterator localIterator2 = this.guaranteedBalances.iterator();
/* 276:254 */       while ((localIterator2.hasNext()) && (j > 0))
/* 277:    */       {
/* 278:255 */         localIterator2.next();
/* 279:256 */         localIterator2.remove();
/* 280:257 */         j--;
/* 281:    */       }
/* 282:    */     }
/* 283:260 */     if ((this.guaranteedBalances.size() == 0) || (localGuaranteedBalance1.height < i))
/* 284:    */     {
/* 285:262 */       this.guaranteedBalances.add(new GuaranteedBalance(i, this.balance, null));
/* 286:    */     }
/* 287:263 */     else if (localGuaranteedBalance1.height == i)
/* 288:    */     {
/* 289:266 */       localGuaranteedBalance1.balance = this.balance;
/* 290:267 */       localGuaranteedBalance1.ignore = false;
/* 291:    */     }
/* 292:    */     else
/* 293:    */     {
/* 294:270 */       throw new IllegalStateException("last guaranteed balance height exceeds blockchain height");
/* 295:    */     }
/* 296:    */   }
/* 297:    */   
/* 298:    */   private static class GuaranteedBalance
/* 299:    */     implements Comparable<GuaranteedBalance>
/* 300:    */   {
/* 301:    */     final int height;
/* 302:    */     long balance;
/* 303:    */     boolean ignore;
/* 304:    */     
/* 305:    */     private GuaranteedBalance(int paramInt, long paramLong)
/* 306:    */     {
/* 307:281 */       this.height = paramInt;
/* 308:282 */       this.balance = paramLong;
/* 309:283 */       this.ignore = false;
/* 310:    */     }
/* 311:    */     
/* 312:    */     public int compareTo(GuaranteedBalance paramGuaranteedBalance)
/* 313:    */     {
/* 314:288 */       if (this.height < paramGuaranteedBalance.height) {
/* 315:289 */         return -1;
/* 316:    */       }
/* 317:290 */       if (this.height > paramGuaranteedBalance.height) {
/* 318:291 */         return 1;
/* 319:    */       }
/* 320:293 */       return 0;
/* 321:    */     }
/* 322:    */     
/* 323:    */     public String toString()
/* 324:    */     {
/* 325:298 */       return "height: " + this.height + ", guaranteed: " + this.balance;
/* 326:    */     }
/* 327:    */   }
/* 328:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Account
 * JD-Core Version:    0.7.0.1
 */