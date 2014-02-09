/*  1:   */ package nxt;
/*  2:   */ 
/*  3:   */ import java.sql.Connection;
/*  4:   */ import java.sql.SQLException;
/*  5:   */ import nxt.util.Logger;
/*  6:   */ import org.h2.jdbcx.JdbcConnectionPool;
/*  7:   */ 
/*  8:   */ final class Db
/*  9:   */ {
/* 10:   */   private static JdbcConnectionPool cp;
/* 11:   */   
/* 12:   */   static void init()
/* 13:   */   {
/* 14:14 */     long l = Runtime.getRuntime().maxMemory() / 2048L;
/* 15:15 */     Logger.logDebugMessage("Database cache size set to " + l + " kB");
/* 16:16 */     cp = JdbcConnectionPool.create("jdbc:h2:nxt_db/nxt;DB_CLOSE_DELAY=10;DB_CLOSE_ON_EXIT=FALSE;CACHE_SIZE=" + l, "sa", "sa");
/* 17:17 */     cp.setMaxConnections(200);
/* 18:18 */     cp.setLoginTimeout(70);
/* 19:19 */     DbVersion.init();
/* 20:   */   }
/* 21:   */   
/* 22:   */   static void shutdown()
/* 23:   */   {
/* 24:23 */     if (cp != null) {
/* 25:24 */       cp.dispose();
/* 26:   */     }
/* 27:   */   }
/* 28:   */   
/* 29:   */   static Connection getConnection()
/* 30:   */     throws SQLException
/* 31:   */   {
/* 32:29 */     Connection localConnection = cp.getConnection();
/* 33:30 */     localConnection.setAutoCommit(false);
/* 34:31 */     return localConnection;
/* 35:   */   }
/* 36:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Db
 * JD-Core Version:    0.7.0.1
 */