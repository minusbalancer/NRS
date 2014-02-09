/*   1:    */ package nxt;
/*   2:    */ 
/*   3:    */ import java.util.Collection;
/*   4:    */ import java.util.Collections;
/*   5:    */ import java.util.SortedSet;
/*   6:    */ import java.util.TreeSet;
/*   7:    */ import java.util.concurrent.ConcurrentHashMap;
/*   8:    */ import java.util.concurrent.ConcurrentMap;
/*   9:    */ 
/*  10:    */ public abstract class Order
/*  11:    */ {
/*  12:    */   private final Long id;
/*  13:    */   private final Account account;
/*  14:    */   private final Long assetId;
/*  15:    */   private final long price;
/*  16:    */   private final long height;
/*  17:    */   private volatile int quantity;
/*  18:    */   
/*  19:    */   static void clear()
/*  20:    */   {
/*  21: 13 */     Ask.askOrders.clear();
/*  22: 14 */     Ask.sortedAskOrders.clear();
/*  23: 15 */     Bid.bidOrders.clear();
/*  24: 16 */     Bid.sortedBidOrders.clear();
/*  25:    */   }
/*  26:    */   
/*  27:    */   private static void matchOrders(Long paramLong)
/*  28:    */   {
/*  29: 22 */     SortedSet localSortedSet1 = (SortedSet)Ask.sortedAskOrders.get(paramLong);
/*  30: 23 */     SortedSet localSortedSet2 = (SortedSet)Bid.sortedBidOrders.get(paramLong);
/*  31: 25 */     while ((!localSortedSet1.isEmpty()) && (!localSortedSet2.isEmpty()))
/*  32:    */     {
/*  33: 27 */       Ask localAsk = (Ask)localSortedSet1.first();
/*  34: 28 */       Bid localBid = (Bid)localSortedSet2.first();
/*  35: 30 */       if (localAsk.getPrice() > localBid.getPrice()) {
/*  36:    */         break;
/*  37:    */       }
/*  38: 36 */       int i = localAsk.quantity < localBid.quantity ? localAsk.quantity : localBid.quantity;
/*  39: 37 */       long l = (localAsk.getHeight() < localBid.getHeight()) || ((localAsk.getHeight() == localBid.getHeight()) && (localAsk.getId().longValue() < localBid.getId().longValue())) ? localAsk.getPrice() : localBid.getPrice();
/*  40: 39 */       if (localAsk.quantity -= i == 0) {
/*  41: 41 */         Ask.removeOrder(localAsk.getId());
/*  42:    */       }
/*  43: 45 */       localAsk.getAccount().addToBalanceAndUnconfirmedBalance(i * l);
/*  44: 47 */       if (localBid.quantity -= i == 0) {
/*  45: 49 */         Bid.removeOrder(localBid.getId());
/*  46:    */       }
/*  47: 53 */       localBid.getAccount().addToAssetAndUnconfirmedAssetBalance(paramLong, i);
/*  48:    */     }
/*  49:    */   }
/*  50:    */   
/*  51:    */   private Order(Long paramLong1, Account paramAccount, Long paramLong2, int paramInt, long paramLong)
/*  52:    */   {
/*  53: 68 */     this.id = paramLong1;
/*  54: 69 */     this.account = paramAccount;
/*  55: 70 */     this.assetId = paramLong2;
/*  56: 71 */     this.quantity = paramInt;
/*  57: 72 */     this.price = paramLong;
/*  58: 73 */     this.height = Blockchain.getLastBlock().getHeight();
/*  59:    */   }
/*  60:    */   
/*  61:    */   public Long getId()
/*  62:    */   {
/*  63: 77 */     return this.id;
/*  64:    */   }
/*  65:    */   
/*  66:    */   public Account getAccount()
/*  67:    */   {
/*  68: 81 */     return this.account;
/*  69:    */   }
/*  70:    */   
/*  71:    */   public Long getAssetId()
/*  72:    */   {
/*  73: 85 */     return this.assetId;
/*  74:    */   }
/*  75:    */   
/*  76:    */   public long getPrice()
/*  77:    */   {
/*  78: 89 */     return this.price;
/*  79:    */   }
/*  80:    */   
/*  81:    */   public final int getQuantity()
/*  82:    */   {
/*  83: 93 */     return this.quantity;
/*  84:    */   }
/*  85:    */   
/*  86:    */   public long getHeight()
/*  87:    */   {
/*  88: 97 */     return this.height;
/*  89:    */   }
/*  90:    */   
/*  91:    */   private int compareTo(Order paramOrder)
/*  92:    */   {
/*  93:102 */     if (this.height < paramOrder.height) {
/*  94:104 */       return -1;
/*  95:    */     }
/*  96:106 */     if (this.height > paramOrder.height) {
/*  97:108 */       return 1;
/*  98:    */     }
/*  99:112 */     if (this.id.longValue() < paramOrder.id.longValue()) {
/* 100:114 */       return -1;
/* 101:    */     }
/* 102:116 */     if (this.id.longValue() > paramOrder.id.longValue()) {
/* 103:118 */       return 1;
/* 104:    */     }
/* 105:122 */     return 0;
/* 106:    */   }
/* 107:    */   
/* 108:    */   public static final class Ask
/* 109:    */     extends Order
/* 110:    */     implements Comparable<Ask>
/* 111:    */   {
/* 112:132 */     private static final ConcurrentMap<Long, Ask> askOrders = new ConcurrentHashMap();
/* 113:133 */     private static final ConcurrentMap<Long, SortedSet<Ask>> sortedAskOrders = new ConcurrentHashMap();
/* 114:135 */     private static final Collection<Ask> allAskOrders = Collections.unmodifiableCollection(askOrders.values());
/* 115:    */     
/* 116:    */     public static Collection<Ask> getAllAskOrders()
/* 117:    */     {
/* 118:138 */       return allAskOrders;
/* 119:    */     }
/* 120:    */     
/* 121:    */     public static Ask getAskOrder(Long paramLong)
/* 122:    */     {
/* 123:142 */       return (Ask)askOrders.get(paramLong);
/* 124:    */     }
/* 125:    */     
/* 126:    */     public static SortedSet<Ask> getSortedOrders(Long paramLong)
/* 127:    */     {
/* 128:146 */       return Collections.unmodifiableSortedSet((SortedSet)sortedAskOrders.get(paramLong));
/* 129:    */     }
/* 130:    */     
/* 131:    */     static void addOrder(Long paramLong1, Account paramAccount, Long paramLong2, int paramInt, long paramLong)
/* 132:    */     {
/* 133:150 */       Ask localAsk = new Ask(paramLong1, paramAccount, paramLong2, paramInt, paramLong);
/* 134:151 */       askOrders.put(localAsk.getId(), localAsk);
/* 135:152 */       Object localObject = (SortedSet)sortedAskOrders.get(paramLong2);
/* 136:153 */       if (localObject == null)
/* 137:    */       {
/* 138:154 */         localObject = new TreeSet();
/* 139:155 */         sortedAskOrders.put(paramLong2, localObject);
/* 140:    */       }
/* 141:157 */       ((SortedSet)localObject).add(localAsk);
/* 142:158 */       Order.matchOrders(paramLong2);
/* 143:    */     }
/* 144:    */     
/* 145:    */     static Ask removeOrder(Long paramLong)
/* 146:    */     {
/* 147:162 */       Ask localAsk = (Ask)askOrders.remove(paramLong);
/* 148:163 */       ((SortedSet)sortedAskOrders.get(localAsk.getAssetId())).remove(localAsk);
/* 149:164 */       return localAsk;
/* 150:    */     }
/* 151:    */     
/* 152:    */     private Ask(Long paramLong1, Account paramAccount, Long paramLong2, int paramInt, long paramLong)
/* 153:    */     {
/* 154:168 */       super(paramAccount, paramLong2, paramInt, paramLong, null);
/* 155:    */     }
/* 156:    */     
/* 157:    */     public int compareTo(Ask paramAsk)
/* 158:    */     {
/* 159:174 */       if (getPrice() < paramAsk.getPrice()) {
/* 160:176 */         return -1;
/* 161:    */       }
/* 162:178 */       if (getPrice() > paramAsk.getPrice()) {
/* 163:180 */         return 1;
/* 164:    */       }
/* 165:184 */       return super.compareTo(paramAsk);
/* 166:    */     }
/* 167:    */     
/* 168:    */     public boolean equals(Object paramObject)
/* 169:    */     {
/* 170:192 */       return ((paramObject instanceof Ask)) && (getId().equals(((Ask)paramObject).getId()));
/* 171:    */     }
/* 172:    */     
/* 173:    */     public int hashCode()
/* 174:    */     {
/* 175:197 */       return getId().hashCode();
/* 176:    */     }
/* 177:    */   }
/* 178:    */   
/* 179:    */   public static final class Bid
/* 180:    */     extends Order
/* 181:    */     implements Comparable<Bid>
/* 182:    */   {
/* 183:204 */     private static final ConcurrentMap<Long, Bid> bidOrders = new ConcurrentHashMap();
/* 184:205 */     private static final ConcurrentMap<Long, SortedSet<Bid>> sortedBidOrders = new ConcurrentHashMap();
/* 185:207 */     private static final Collection<Bid> allBidOrders = Collections.unmodifiableCollection(bidOrders.values());
/* 186:    */     
/* 187:    */     public static Collection<Bid> getAllBidOrders()
/* 188:    */     {
/* 189:210 */       return allBidOrders;
/* 190:    */     }
/* 191:    */     
/* 192:    */     public static Bid getBidOrder(Long paramLong)
/* 193:    */     {
/* 194:214 */       return (Bid)bidOrders.get(paramLong);
/* 195:    */     }
/* 196:    */     
/* 197:    */     public static SortedSet<Bid> getSortedOrders(Long paramLong)
/* 198:    */     {
/* 199:218 */       return Collections.unmodifiableSortedSet((SortedSet)sortedBidOrders.get(paramLong));
/* 200:    */     }
/* 201:    */     
/* 202:    */     static void addOrder(Long paramLong1, Account paramAccount, Long paramLong2, int paramInt, long paramLong)
/* 203:    */     {
/* 204:222 */       Bid localBid = new Bid(paramLong1, paramAccount, paramLong2, paramInt, paramLong);
/* 205:223 */       paramAccount.addToBalanceAndUnconfirmedBalance(-paramInt * paramLong);
/* 206:224 */       bidOrders.put(localBid.getId(), localBid);
/* 207:225 */       Object localObject = (SortedSet)sortedBidOrders.get(paramLong2);
/* 208:226 */       if (localObject == null)
/* 209:    */       {
/* 210:227 */         localObject = new TreeSet();
/* 211:228 */         sortedBidOrders.put(paramLong2, localObject);
/* 212:    */       }
/* 213:230 */       Order.matchOrders(paramLong2);
/* 214:    */     }
/* 215:    */     
/* 216:    */     static Bid removeOrder(Long paramLong)
/* 217:    */     {
/* 218:234 */       Bid localBid = (Bid)bidOrders.remove(paramLong);
/* 219:235 */       ((SortedSet)sortedBidOrders.get(localBid.getAssetId())).remove(localBid);
/* 220:236 */       return localBid;
/* 221:    */     }
/* 222:    */     
/* 223:    */     private Bid(Long paramLong1, Account paramAccount, Long paramLong2, int paramInt, long paramLong)
/* 224:    */     {
/* 225:240 */       super(paramAccount, paramLong2, paramInt, paramLong, null);
/* 226:    */     }
/* 227:    */     
/* 228:    */     public int compareTo(Bid paramBid)
/* 229:    */     {
/* 230:246 */       if (getPrice() > paramBid.getPrice()) {
/* 231:248 */         return -1;
/* 232:    */       }
/* 233:250 */       if (getPrice() < paramBid.getPrice()) {
/* 234:252 */         return 1;
/* 235:    */       }
/* 236:256 */       return super.compareTo(paramBid);
/* 237:    */     }
/* 238:    */     
/* 239:    */     public boolean equals(Object paramObject)
/* 240:    */     {
/* 241:264 */       return ((paramObject instanceof Bid)) && (getId().equals(((Bid)paramObject).getId()));
/* 242:    */     }
/* 243:    */     
/* 244:    */     public int hashCode()
/* 245:    */     {
/* 246:269 */       return getId().hashCode();
/* 247:    */     }
/* 248:    */   }
/* 249:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Order
 * JD-Core Version:    0.7.0.1
 */