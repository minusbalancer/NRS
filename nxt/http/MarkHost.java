/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.peer.Hallmark;
/*  5:   */ import org.json.simple.JSONObject;
/*  6:   */ import org.json.simple.JSONStreamAware;
/*  7:   */ 
/*  8:   */ public final class MarkHost
/*  9:   */   extends HttpRequestHandler
/* 10:   */ {
/* 11:21 */   static final MarkHost instance = new MarkHost();
/* 12:   */   
/* 13:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 14:   */   {
/* 15:28 */     String str1 = paramHttpServletRequest.getParameter("secretPhrase");
/* 16:29 */     String str2 = paramHttpServletRequest.getParameter("host");
/* 17:30 */     String str3 = paramHttpServletRequest.getParameter("weight");
/* 18:31 */     String str4 = paramHttpServletRequest.getParameter("date");
/* 19:32 */     if (str1 == null) {
/* 20:33 */       return JSONResponses.MISSING_SECRET_PHRASE;
/* 21:   */     }
/* 22:34 */     if (str2 == null) {
/* 23:35 */       return JSONResponses.MISSING_HOST;
/* 24:   */     }
/* 25:36 */     if (str3 == null) {
/* 26:37 */       return JSONResponses.MISSING_WEIGHT;
/* 27:   */     }
/* 28:38 */     if (str4 == null) {
/* 29:39 */       return JSONResponses.MISSING_DATE;
/* 30:   */     }
/* 31:42 */     if (str2.length() > 100) {
/* 32:43 */       return JSONResponses.INCORRECT_HOST;
/* 33:   */     }
/* 34:   */     int i;
/* 35:   */     try
/* 36:   */     {
/* 37:48 */       i = Integer.parseInt(str3);
/* 38:49 */       if ((i <= 0) || (i > 1000000000L)) {
/* 39:50 */         return JSONResponses.INCORRECT_WEIGHT;
/* 40:   */       }
/* 41:   */     }
/* 42:   */     catch (NumberFormatException localNumberFormatException)
/* 43:   */     {
/* 44:53 */       return JSONResponses.INCORRECT_WEIGHT;
/* 45:   */     }
/* 46:   */     try
/* 47:   */     {
/* 48:58 */       String str5 = Hallmark.generateHallmark(str1, str2, i, Hallmark.parseDate(str4));
/* 49:   */       
/* 50:60 */       JSONObject localJSONObject = new JSONObject();
/* 51:61 */       localJSONObject.put("hallmark", str5);
/* 52:62 */       return localJSONObject;
/* 53:   */     }
/* 54:   */     catch (RuntimeException localRuntimeException) {}
/* 55:65 */     return JSONResponses.INCORRECT_DATE;
/* 56:   */   }
/* 57:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.MarkHost
 * JD-Core Version:    0.7.0.1
 */