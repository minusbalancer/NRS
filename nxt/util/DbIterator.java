/*  1:   */ package nxt.util;
/*  2:   */ 
/*  3:   */ import java.sql.Connection;
/*  4:   */ import java.sql.PreparedStatement;
/*  5:   */ import java.sql.ResultSet;
/*  6:   */ import java.sql.SQLException;
/*  7:   */ import java.util.Iterator;
/*  8:   */ 
/*  9:   */ public final class DbIterator<T>
/* 10:   */   implements Iterator<T>, AutoCloseable
/* 11:   */ {
/* 12:   */   private final Connection con;
/* 13:   */   private final PreparedStatement pstmt;
/* 14:   */   private final ResultSetReader<T> rsReader;
/* 15:   */   private final ResultSet rs;
/* 16:   */   private boolean hasNext;
/* 17:   */   
/* 18:   */   public DbIterator(Connection paramConnection, PreparedStatement paramPreparedStatement, ResultSetReader<T> paramResultSetReader)
/* 19:   */   {
/* 20:23 */     this.con = paramConnection;
/* 21:24 */     this.pstmt = paramPreparedStatement;
/* 22:25 */     this.rsReader = paramResultSetReader;
/* 23:   */     try
/* 24:   */     {
/* 25:27 */       this.rs = paramPreparedStatement.executeQuery();
/* 26:28 */       this.hasNext = this.rs.next();
/* 27:   */     }
/* 28:   */     catch (SQLException localSQLException)
/* 29:   */     {
/* 30:30 */       DbUtils.close(new AutoCloseable[] { paramPreparedStatement, paramConnection });
/* 31:31 */       throw new RuntimeException(localSQLException.toString(), localSQLException);
/* 32:   */     }
/* 33:   */   }
/* 34:   */   
/* 35:   */   public boolean hasNext()
/* 36:   */   {
/* 37:37 */     if (!this.hasNext) {
/* 38:38 */       DbUtils.close(new AutoCloseable[] { this.rs, this.pstmt, this.con });
/* 39:   */     }
/* 40:40 */     return this.hasNext;
/* 41:   */   }
/* 42:   */   
/* 43:   */   public T next()
/* 44:   */   {
/* 45:45 */     if (!this.hasNext)
/* 46:   */     {
/* 47:46 */       DbUtils.close(new AutoCloseable[] { this.rs, this.pstmt, this.con });
/* 48:47 */       return null;
/* 49:   */     }
/* 50:   */     try
/* 51:   */     {
/* 52:50 */       Object localObject = this.rsReader.get(this.con, this.rs);
/* 53:51 */       this.hasNext = this.rs.next();
/* 54:52 */       return localObject;
/* 55:   */     }
/* 56:   */     catch (Exception localException)
/* 57:   */     {
/* 58:54 */       DbUtils.close(new AutoCloseable[] { this.rs, this.pstmt, this.con });
/* 59:55 */       throw new RuntimeException(localException.toString(), localException);
/* 60:   */     }
/* 61:   */   }
/* 62:   */   
/* 63:   */   public void remove()
/* 64:   */   {
/* 65:61 */     throw new UnsupportedOperationException("Removal not suported");
/* 66:   */   }
/* 67:   */   
/* 68:   */   public void close()
/* 69:   */   {
/* 70:66 */     DbUtils.close(new AutoCloseable[] { this.rs, this.pstmt, this.con });
/* 71:   */   }
/* 72:   */   
/* 73:   */   public static abstract interface ResultSetReader<T>
/* 74:   */   {
/* 75:   */     public abstract T get(Connection paramConnection, ResultSet paramResultSet)
/* 76:   */       throws Exception;
/* 77:   */   }
/* 78:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.util.DbIterator
 * JD-Core Version:    0.7.0.1
 */