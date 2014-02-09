/* 1:  */ package nxt.util;
/* 2:  */ 
/* 3:  */ public final class DbUtils
/* 4:  */ {
/* 5:  */   public static void close(AutoCloseable... paramVarArgs)
/* 6:  */   {
/* 7:6 */     for (AutoCloseable localAutoCloseable : paramVarArgs) {
/* 8:7 */       if (localAutoCloseable != null) {
/* 9:  */         try
/* ::  */         {
/* ;:9 */           localAutoCloseable.close();
/* <:  */         }
/* =:  */         catch (Exception localException) {}
/* >:  */       }
/* ?:  */     }
/* @:  */   }
/* A:  */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.util.DbUtils
 * JD-Core Version:    0.7.0.1
 */