/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Account;
/*  5:   */ import nxt.peer.Hallmark;
/*  6:   */ import nxt.util.Convert;
/*  7:   */ import org.json.simple.JSONObject;
/*  8:   */ import org.json.simple.JSONStreamAware;
/*  9:   */ 
/* 10:   */ public final class DecodeHallmark
/* 11:   */   extends HttpRequestHandler
/* 12:   */ {
/* 13:16 */   static final DecodeHallmark instance = new DecodeHallmark();
/* 14:   */   
/* 15:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 16:   */   {
/* 17:23 */     String str1 = paramHttpServletRequest.getParameter("hallmark");
/* 18:24 */     if (str1 == null) {
/* 19:25 */       return JSONResponses.MISSING_HALLMARK;
/* 20:   */     }
/* 21:   */     try
/* 22:   */     {
/* 23:30 */       Hallmark localHallmark = Hallmark.parseHallmark(str1);
/* 24:   */       
/* 25:32 */       JSONObject localJSONObject = new JSONObject();
/* 26:33 */       localJSONObject.put("account", Convert.convert(Account.getId(localHallmark.getPublicKey())));
/* 27:34 */       localJSONObject.put("host", localHallmark.getHost());
/* 28:35 */       localJSONObject.put("weight", Integer.valueOf(localHallmark.getWeight()));
/* 29:36 */       String str2 = Hallmark.formatDate(localHallmark.getDate());
/* 30:37 */       localJSONObject.put("date", str2);
/* 31:38 */       localJSONObject.put("valid", Boolean.valueOf(localHallmark.isValid()));
/* 32:   */       
/* 33:40 */       return localJSONObject;
/* 34:   */     }
/* 35:   */     catch (RuntimeException localRuntimeException) {}
/* 36:43 */     return JSONResponses.INCORRECT_HALLMARK;
/* 37:   */   }
/* 38:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.DecodeHallmark
 * JD-Core Version:    0.7.0.1
 */