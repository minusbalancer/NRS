/*  1:   */ package nxt;
/*  2:   */ 
/*  3:   */ import java.util.Collection;
/*  4:   */ import java.util.Collections;
/*  5:   */ import java.util.concurrent.ConcurrentHashMap;
/*  6:   */ import java.util.concurrent.ConcurrentMap;
/*  7:   */ 
/*  8:   */ public final class Alias
/*  9:   */ {
/* 10:10 */   private static final ConcurrentMap<String, Alias> aliases = new ConcurrentHashMap();
/* 11:11 */   private static final ConcurrentMap<Long, Alias> aliasIdToAliasMappings = new ConcurrentHashMap();
/* 12:12 */   private static final Collection<Alias> allAliases = Collections.unmodifiableCollection(aliases.values());
/* 13:   */   private final Account account;
/* 14:   */   private final Long id;
/* 15:   */   private final String aliasName;
/* 16:   */   private volatile String aliasURI;
/* 17:   */   private volatile int timestamp;
/* 18:   */   
/* 19:   */   public static Collection<Alias> getAllAliases()
/* 20:   */   {
/* 21:15 */     return allAliases;
/* 22:   */   }
/* 23:   */   
/* 24:   */   public static Alias getAlias(String paramString)
/* 25:   */   {
/* 26:19 */     return (Alias)aliases.get(paramString);
/* 27:   */   }
/* 28:   */   
/* 29:   */   public static Alias getAlias(Long paramLong)
/* 30:   */   {
/* 31:23 */     return (Alias)aliasIdToAliasMappings.get(paramLong);
/* 32:   */   }
/* 33:   */   
/* 34:   */   static void addOrUpdateAlias(Account paramAccount, Long paramLong, String paramString1, String paramString2, int paramInt)
/* 35:   */   {
/* 36:27 */     String str = paramString1.toLowerCase();
/* 37:28 */     Alias localAlias1 = new Alias(paramAccount, paramLong, paramString1, paramString2, paramInt);
/* 38:29 */     Alias localAlias2 = (Alias)aliases.putIfAbsent(str, localAlias1);
/* 39:30 */     if (localAlias2 == null)
/* 40:   */     {
/* 41:31 */       aliasIdToAliasMappings.putIfAbsent(paramLong, localAlias1);
/* 42:   */     }
/* 43:   */     else
/* 44:   */     {
/* 45:33 */       localAlias2.aliasURI = paramString2.intern();
/* 46:34 */       localAlias2.timestamp = paramInt;
/* 47:   */     }
/* 48:   */   }
/* 49:   */   
/* 50:   */   static void clear()
/* 51:   */   {
/* 52:39 */     aliases.clear();
/* 53:40 */     aliasIdToAliasMappings.clear();
/* 54:   */   }
/* 55:   */   
/* 56:   */   private Alias(Account paramAccount, Long paramLong, String paramString1, String paramString2, int paramInt)
/* 57:   */   {
/* 58:51 */     this.account = paramAccount;
/* 59:52 */     this.id = paramLong;
/* 60:53 */     this.aliasName = paramString1.intern();
/* 61:54 */     this.aliasURI = paramString2.intern();
/* 62:55 */     this.timestamp = paramInt;
/* 63:   */   }
/* 64:   */   
/* 65:   */   public Long getId()
/* 66:   */   {
/* 67:60 */     return this.id;
/* 68:   */   }
/* 69:   */   
/* 70:   */   public String getAliasName()
/* 71:   */   {
/* 72:64 */     return this.aliasName;
/* 73:   */   }
/* 74:   */   
/* 75:   */   public String getURI()
/* 76:   */   {
/* 77:68 */     return this.aliasURI;
/* 78:   */   }
/* 79:   */   
/* 80:   */   public int getTimestamp()
/* 81:   */   {
/* 82:72 */     return this.timestamp;
/* 83:   */   }
/* 84:   */   
/* 85:   */   public Account getAccount()
/* 86:   */   {
/* 87:76 */     return this.account;
/* 88:   */   }
/* 89:   */   
/* 90:   */   public boolean equals(Object paramObject)
/* 91:   */   {
/* 92:81 */     return ((paramObject instanceof Alias)) && (getId().equals(((Alias)paramObject).getId()));
/* 93:   */   }
/* 94:   */   
/* 95:   */   public int hashCode()
/* 96:   */   {
/* 97:86 */     return getId().hashCode();
/* 98:   */   }
/* 99:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Alias
 * JD-Core Version:    0.7.0.1
 */