/*  1:   */ package nxt;
/*  2:   */ 
/*  3:   */ import java.util.Collection;
/*  4:   */ import java.util.Collections;
/*  5:   */ import java.util.concurrent.ConcurrentHashMap;
/*  6:   */ import java.util.concurrent.ConcurrentMap;
/*  7:   */ 
/*  8:   */ public final class Asset
/*  9:   */ {
/* 10:10 */   private static final ConcurrentMap<Long, Asset> assets = new ConcurrentHashMap();
/* 11:11 */   private static final ConcurrentMap<String, Asset> assetNameToAssetMappings = new ConcurrentHashMap();
/* 12:12 */   private static final Collection<Asset> allAssets = Collections.unmodifiableCollection(assets.values());
/* 13:   */   private final Long assetId;
/* 14:   */   private final Long accountId;
/* 15:   */   private final String name;
/* 16:   */   private final String description;
/* 17:   */   private final int quantity;
/* 18:   */   
/* 19:   */   public static Collection<Asset> getAllAssets()
/* 20:   */   {
/* 21:15 */     return allAssets;
/* 22:   */   }
/* 23:   */   
/* 24:   */   public static Asset getAsset(Long paramLong)
/* 25:   */   {
/* 26:19 */     return (Asset)assets.get(paramLong);
/* 27:   */   }
/* 28:   */   
/* 29:   */   public static Asset getAsset(String paramString)
/* 30:   */   {
/* 31:23 */     return (Asset)assetNameToAssetMappings.get(paramString);
/* 32:   */   }
/* 33:   */   
/* 34:   */   static void addAsset(Long paramLong1, Long paramLong2, String paramString1, String paramString2, int paramInt)
/* 35:   */   {
/* 36:27 */     Asset localAsset = new Asset(paramLong1, paramLong2, paramString1, paramString2, paramInt);
/* 37:28 */     assets.put(paramLong1, localAsset);
/* 38:29 */     assetNameToAssetMappings.put(paramString1.toLowerCase(), localAsset);
/* 39:   */   }
/* 40:   */   
/* 41:   */   static void removeAsset(Long paramLong)
/* 42:   */   {
/* 43:33 */     Asset localAsset = (Asset)assets.remove(paramLong);
/* 44:34 */     assetNameToAssetMappings.remove(localAsset.getName());
/* 45:   */   }
/* 46:   */   
/* 47:   */   static void clear()
/* 48:   */   {
/* 49:38 */     assets.clear();
/* 50:39 */     assetNameToAssetMappings.clear();
/* 51:   */   }
/* 52:   */   
/* 53:   */   private Asset(Long paramLong1, Long paramLong2, String paramString1, String paramString2, int paramInt)
/* 54:   */   {
/* 55:49 */     this.assetId = paramLong1;
/* 56:50 */     this.accountId = paramLong2;
/* 57:51 */     this.name = paramString1;
/* 58:52 */     this.description = paramString2;
/* 59:53 */     this.quantity = paramInt;
/* 60:   */   }
/* 61:   */   
/* 62:   */   public Long getId()
/* 63:   */   {
/* 64:57 */     return this.assetId;
/* 65:   */   }
/* 66:   */   
/* 67:   */   public Long getAccountId()
/* 68:   */   {
/* 69:61 */     return this.accountId;
/* 70:   */   }
/* 71:   */   
/* 72:   */   public String getName()
/* 73:   */   {
/* 74:65 */     return this.name;
/* 75:   */   }
/* 76:   */   
/* 77:   */   public String getDescription()
/* 78:   */   {
/* 79:69 */     return this.description;
/* 80:   */   }
/* 81:   */   
/* 82:   */   public int getQuantity()
/* 83:   */   {
/* 84:73 */     return this.quantity;
/* 85:   */   }
/* 86:   */   
/* 87:   */   public boolean equals(Object paramObject)
/* 88:   */   {
/* 89:78 */     return ((paramObject instanceof Asset)) && (getId().equals(((Asset)paramObject).getId()));
/* 90:   */   }
/* 91:   */   
/* 92:   */   public int hashCode()
/* 93:   */   {
/* 94:83 */     return getId().hashCode();
/* 95:   */   }
/* 96:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Asset
 * JD-Core Version:    0.7.0.1
 */