/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Account;
/*  5:   */ import nxt.util.Convert;
/*  6:   */ import org.json.simple.JSONObject;
/*  7:   */ import org.json.simple.JSONStreamAware;
/*  8:   */ 
/*  9:   */ final class GetGuaranteedBalance
/* 10:   */   extends HttpRequestHandler
/* 11:   */ {
/* 12:17 */   static final GetGuaranteedBalance instance = new GetGuaranteedBalance();
/* 13:   */   
/* 14:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 15:   */   {
/* 16:24 */     String str1 = paramHttpServletRequest.getParameter("account");
/* 17:25 */     String str2 = paramHttpServletRequest.getParameter("numberOfConfirmations");
/* 18:26 */     if (str1 == null) {
/* 19:27 */       return JSONResponses.MISSING_ACCOUNT;
/* 20:   */     }
/* 21:28 */     if (str2 == null) {
/* 22:29 */       return JSONResponses.MISSING_NUMBER_OF_CONFIRMATIONS;
/* 23:   */     }
/* 24:   */     Account localAccount;
/* 25:   */     try
/* 26:   */     {
/* 27:34 */       localAccount = Account.getAccount(Convert.parseUnsignedLong(str1));
/* 28:   */     }
/* 29:   */     catch (RuntimeException localRuntimeException)
/* 30:   */     {
/* 31:36 */       return JSONResponses.INCORRECT_ACCOUNT;
/* 32:   */     }
/* 33:39 */     JSONObject localJSONObject = new JSONObject();
/* 34:40 */     if (localAccount == null) {
/* 35:41 */       localJSONObject.put("guaranteedBalance", Integer.valueOf(0));
/* 36:   */     } else {
/* 37:   */       try
/* 38:   */       {
/* 39:44 */         int i = Integer.parseInt(str2);
/* 40:45 */         localJSONObject.put("guaranteedBalance", Long.valueOf(localAccount.getGuaranteedBalance(i)));
/* 41:   */       }
/* 42:   */       catch (NumberFormatException localNumberFormatException)
/* 43:   */       {
/* 44:47 */         return JSONResponses.INCORRECT_NUMBER_OF_CONFIRMATIONS;
/* 45:   */       }
/* 46:   */     }
/* 47:51 */     return localJSONObject;
/* 48:   */   }
/* 49:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetGuaranteedBalance
 * JD-Core Version:    0.7.0.1
 */