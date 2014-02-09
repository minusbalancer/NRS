/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Account;
/*  5:   */ import nxt.util.Convert;
/*  6:   */ import nxt.util.JSON;
/*  7:   */ import org.json.simple.JSONObject;
/*  8:   */ import org.json.simple.JSONStreamAware;
/*  9:   */ 
/* 10:   */ final class GetAccountPublicKey
/* 11:   */   extends HttpRequestHandler
/* 12:   */ {
/* 13:17 */   static final GetAccountPublicKey instance = new GetAccountPublicKey();
/* 14:   */   
/* 15:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 16:   */   {
/* 17:24 */     String str = paramHttpServletRequest.getParameter("account");
/* 18:25 */     if (str == null) {
/* 19:26 */       return JSONResponses.MISSING_ACCOUNT;
/* 20:   */     }
/* 21:   */     Account localAccount;
/* 22:   */     try
/* 23:   */     {
/* 24:31 */       localAccount = Account.getAccount(Convert.parseUnsignedLong(str));
/* 25:   */     }
/* 26:   */     catch (RuntimeException localRuntimeException)
/* 27:   */     {
/* 28:33 */       return JSONResponses.INCORRECT_ACCOUNT;
/* 29:   */     }
/* 30:35 */     if (localAccount == null) {
/* 31:36 */       return JSONResponses.UNKNOWN_ACCOUNT;
/* 32:   */     }
/* 33:39 */     if (localAccount.getPublicKey() != null)
/* 34:   */     {
/* 35:41 */       JSONObject localJSONObject = new JSONObject();
/* 36:42 */       localJSONObject.put("publicKey", Convert.convert(localAccount.getPublicKey()));
/* 37:43 */       return localJSONObject;
/* 38:   */     }
/* 39:46 */     return JSON.emptyJSON;
/* 40:   */   }
/* 41:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetAccountPublicKey
 * JD-Core Version:    0.7.0.1
 */