/*  1:   */ package nxt.util;
/*  2:   */ 
/*  3:   */ import java.io.BufferedWriter;
/*  4:   */ import java.io.FileOutputStream;
/*  5:   */ import java.io.IOException;
/*  6:   */ import java.io.OutputStreamWriter;
/*  7:   */ import java.io.PrintStream;
/*  8:   */ import java.io.PrintWriter;
/*  9:   */ import java.text.SimpleDateFormat;
/* 10:   */ import java.util.Date;
/* 11:   */ 
/* 12:   */ public final class Logger
/* 13:   */ {
/* 14:13 */   private static final ThreadLocal<SimpleDateFormat> logDateFormat = new ThreadLocal()
/* 15:   */   {
/* 16:   */     protected SimpleDateFormat initialValue()
/* 17:   */     {
/* 18:16 */       return new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ");
/* 19:   */     }
/* 20:   */   };
/* 21:20 */   public static final boolean debug = System.getProperty("nxt.debug") != null;
/* 22:21 */   public static final boolean enableStackTraces = System.getProperty("nxt.enableStackTraces") != null;
/* 23:23 */   private static PrintWriter fileLog = null;
/* 24:   */   
/* 25:   */   static
/* 26:   */   {
/* 27:   */     try
/* 28:   */     {
/* 29:26 */       fileLog = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream("nxt.log"))), true);
/* 30:   */     }
/* 31:   */     catch (IOException localIOException)
/* 32:   */     {
/* 33:28 */       System.out.println("Logging to file nxt.log not possible, will log to stdout only");
/* 34:   */     }
/* 35:   */   }
/* 36:   */   
/* 37:   */   public static void logMessage(String paramString)
/* 38:   */   {
/* 39:35 */     String str = ((SimpleDateFormat)logDateFormat.get()).format(new Date()) + paramString;
/* 40:36 */     System.out.println(str);
/* 41:37 */     if (fileLog != null) {
/* 42:38 */       fileLog.println(str);
/* 43:   */     }
/* 44:   */   }
/* 45:   */   
/* 46:   */   public static void logMessage(String paramString, Exception paramException)
/* 47:   */   {
/* 48:43 */     if (enableStackTraces)
/* 49:   */     {
/* 50:44 */       logMessage(paramString);
/* 51:45 */       paramException.printStackTrace();
/* 52:   */     }
/* 53:   */     else
/* 54:   */     {
/* 55:47 */       logMessage(paramString + ":\n" + paramException.toString());
/* 56:   */     }
/* 57:   */   }
/* 58:   */   
/* 59:   */   public static void logDebugMessage(String paramString)
/* 60:   */   {
/* 61:52 */     if (debug) {
/* 62:53 */       logMessage("DEBUG: " + paramString);
/* 63:   */     }
/* 64:   */   }
/* 65:   */   
/* 66:   */   public static void logDebugMessage(String paramString, Exception paramException)
/* 67:   */   {
/* 68:58 */     if (debug) {
/* 69:59 */       if (enableStackTraces)
/* 70:   */       {
/* 71:60 */         logMessage("DEBUG: " + paramString);
/* 72:61 */         paramException.printStackTrace();
/* 73:   */       }
/* 74:   */       else
/* 75:   */       {
/* 76:63 */         logMessage("DEBUG: " + paramString + ":\n" + paramException.toString());
/* 77:   */       }
/* 78:   */     }
/* 79:   */   }
/* 80:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.util.Logger
 * JD-Core Version:    0.7.0.1
 */