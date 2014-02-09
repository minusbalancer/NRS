/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Account;
/*  5:   */ import nxt.util.Convert;
/*  6:   */ import org.json.simple.JSONObject;
/*  7:   */ import org.json.simple.JSONStreamAware;
/*  8:   */ 
/*  9:   */ final class GetBalance
/* 10:   */   extends HttpRequestHandler
/* 11:   */ {
/* 12:15 */   static final GetBalance instance = new GetBalance();
/* 13:   */   
/* 14:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 15:   */   {
/* 16:22 */     String str = paramHttpServletRequest.getParameter("account");
/* 17:23 */     if (str == null) {
/* 18:24 */       return JSONResponses.MISSING_ACCOUNT;
/* 19:   */     }
/* 20:   */     Account localAccount;
/* 21:   */     try
/* 22:   */     {
/* 23:29 */       localAccount = Account.getAccount(Convert.parseUnsignedLong(str));
/* 24:   */     }
/* 25:   */     catch (RuntimeException localRuntimeException)
/* 26:   */     {
/* 27:31 */       return JSONResponses.INCORRECT_ACCOUNT;
/* 28:   */     }
/* 29:34 */     JSONObject localJSONObject = new JSONObject();
/* 30:35 */     if (localAccount == null)
/* 31:   */     {
/* 32:37 */       localJSONObject.put("balance", Integer.valueOf(0));
/* 33:38 */       localJSONObject.put("unconfirmedBalance", Integer.valueOf(0));
/* 34:39 */       localJSONObject.put("effectiveBalance", Integer.valueOf(0));
/* 35:   */     }
/* 36:   */     else
/* 37:   */     {
/* 38:43 */       synchronized (localAccount)
/* 39:   */       {
/* 40:44 */         localJSONObject.put("balance", Long.valueOf(localAccount.getBalance()));
/* 41:45 */         localJSONObject.put("unconfirmedBalance", Long.valueOf(localAccount.getUnconfirmedBalance()));
/* 42:46 */         localJSONObject.put("effectiveBalance", Long.valueOf(localAccount.getEffectiveBalance() * 100L));
/* 43:   */       }
/* 44:   */     }
/* 45:50 */     return localJSONObject;
/* 46:   */   }
/* 47:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetBalance
 * JD-Core Version:    0.7.0.1
 */