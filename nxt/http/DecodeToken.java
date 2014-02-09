/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Account;
/*  5:   */ import nxt.Token;
/*  6:   */ import nxt.util.Convert;
/*  7:   */ import org.json.simple.JSONObject;
/*  8:   */ import org.json.simple.JSONStreamAware;
/*  9:   */ 
/* 10:   */ public final class DecodeToken
/* 11:   */   extends HttpRequestHandler
/* 12:   */ {
/* 13:17 */   static final DecodeToken instance = new DecodeToken();
/* 14:   */   
/* 15:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 16:   */   {
/* 17:24 */     String str1 = paramHttpServletRequest.getParameter("website");
/* 18:25 */     String str2 = paramHttpServletRequest.getParameter("token");
/* 19:26 */     if (str1 == null) {
/* 20:27 */       return JSONResponses.MISSING_WEBSITE;
/* 21:   */     }
/* 22:28 */     if (str2 == null) {
/* 23:29 */       return JSONResponses.MISSING_TOKEN;
/* 24:   */     }
/* 25:   */     try
/* 26:   */     {
/* 27:34 */       Token localToken = Token.parseToken(str2, str1.trim());
/* 28:   */       
/* 29:36 */       JSONObject localJSONObject = new JSONObject();
/* 30:37 */       localJSONObject.put("account", Convert.convert(Account.getId(localToken.getPublicKey())));
/* 31:38 */       localJSONObject.put("timestamp", Integer.valueOf(localToken.getTimestamp()));
/* 32:39 */       localJSONObject.put("valid", Boolean.valueOf(localToken.isValid()));
/* 33:   */       
/* 34:41 */       return localJSONObject;
/* 35:   */     }
/* 36:   */     catch (RuntimeException localRuntimeException) {}
/* 37:44 */     return JSONResponses.INCORRECT_WEBSITE;
/* 38:   */   }
/* 39:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.DecodeToken
 * JD-Core Version:    0.7.0.1
 */