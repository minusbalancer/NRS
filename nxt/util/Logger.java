/*  1:   */ package nxt.util;
/*  2:   */ 
/*  3:   */ import java.io.PrintStream;
/*  4:   */ import java.text.SimpleDateFormat;
/*  5:   */ import java.util.Date;
/*  6:   */ 
/*  7:   */ public final class Logger
/*  8:   */ {
/*  9: 8 */   private static final ThreadLocal<SimpleDateFormat> logDateFormat = new ThreadLocal()
/* 10:   */   {
/* 11:   */     protected SimpleDateFormat initialValue()
/* 12:   */     {
/* 13:11 */       return new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ");
/* 14:   */     }
/* 15:   */   };
/* 16:15 */   public static final boolean debug = System.getProperty("nxt.debug") != null;
/* 17:16 */   public static final boolean enableStackTraces = System.getProperty("nxt.enableStackTraces") != null;
/* 18:   */   
/* 19:   */   public static void logMessage(String paramString)
/* 20:   */   {
/* 21:21 */     System.out.println(((SimpleDateFormat)logDateFormat.get()).format(new Date()) + paramString);
/* 22:   */   }
/* 23:   */   
/* 24:   */   public static void logMessage(String paramString, Exception paramException)
/* 25:   */   {
/* 26:25 */     if (enableStackTraces)
/* 27:   */     {
/* 28:26 */       logMessage(paramString);
/* 29:27 */       paramException.printStackTrace();
/* 30:   */     }
/* 31:   */     else
/* 32:   */     {
/* 33:29 */       logMessage(paramString + ":\n" + paramException.toString());
/* 34:   */     }
/* 35:   */   }
/* 36:   */   
/* 37:   */   public static void logDebugMessage(String paramString)
/* 38:   */   {
/* 39:34 */     if (debug) {
/* 40:35 */       logMessage("DEBUG: " + paramString);
/* 41:   */     }
/* 42:   */   }
/* 43:   */   
/* 44:   */   public static void logDebugMessage(String paramString, Exception paramException)
/* 45:   */   {
/* 46:40 */     if (debug) {
/* 47:41 */       if (enableStackTraces)
/* 48:   */       {
/* 49:42 */         logMessage("DEBUG: " + paramString);
/* 50:43 */         paramException.printStackTrace();
/* 51:   */       }
/* 52:   */       else
/* 53:   */       {
/* 54:45 */         logMessage("DEBUG: " + paramString + ":\n" + paramException.toString());
/* 55:   */       }
/* 56:   */     }
/* 57:   */   }
/* 58:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.util.Logger
 * JD-Core Version:    0.7.0.1
 */