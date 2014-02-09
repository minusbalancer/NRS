/*  1:   */ package nxt;
/*  2:   */ 
/*  3:   */ public abstract class NxtException
/*  4:   */   extends Exception
/*  5:   */ {
/*  6:   */   protected NxtException() {}
/*  7:   */   
/*  8:   */   protected NxtException(String paramString)
/*  9:   */   {
/* 10:10 */     super(paramString);
/* 11:   */   }
/* 12:   */   
/* 13:   */   protected NxtException(String paramString, Throwable paramThrowable)
/* 14:   */   {
/* 15:14 */     super(paramString, paramThrowable);
/* 16:   */   }
/* 17:   */   
/* 18:   */   protected NxtException(Throwable paramThrowable)
/* 19:   */   {
/* 20:18 */     super(paramThrowable);
/* 21:   */   }
/* 22:   */   
/* 23:   */   public static class ValidationException
/* 24:   */     extends NxtException
/* 25:   */   {
/* 26:   */     public ValidationException(String paramString)
/* 27:   */     {
/* 28:24 */       super();
/* 29:   */     }
/* 30:   */     
/* 31:   */     public ValidationException(String paramString, Throwable paramThrowable)
/* 32:   */     {
/* 33:28 */       super(paramThrowable);
/* 34:   */     }
/* 35:   */   }
/* 36:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.NxtException
 * JD-Core Version:    0.7.0.1
 */