/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.util.Map;
/*  4:   */ import java.util.Map.Entry;
/*  5:   */ import javax.servlet.http.HttpServletRequest;
/*  6:   */ import nxt.Account;
/*  7:   */ import nxt.util.Convert;
/*  8:   */ import org.json.simple.JSONArray;
/*  9:   */ import org.json.simple.JSONObject;
/* 10:   */ import org.json.simple.JSONStreamAware;
/* 11:   */ 
/* 12:   */ final class GetAccount
/* 13:   */   extends HttpRequestHandler
/* 14:   */ {
/* 15:18 */   static final GetAccount instance = new GetAccount();
/* 16:   */   
/* 17:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 18:   */   {
/* 19:25 */     String str = paramHttpServletRequest.getParameter("account");
/* 20:26 */     if (str == null) {
/* 21:27 */       return JSONResponses.MISSING_ACCOUNT;
/* 22:   */     }
/* 23:   */     Account localAccount;
/* 24:   */     try
/* 25:   */     {
/* 26:32 */       localAccount = Account.getAccount(Convert.parseUnsignedLong(str));
/* 27:33 */       if (localAccount == null) {
/* 28:34 */         return JSONResponses.UNKNOWN_ACCOUNT;
/* 29:   */       }
/* 30:   */     }
/* 31:   */     catch (RuntimeException localRuntimeException)
/* 32:   */     {
/* 33:37 */       return JSONResponses.INCORRECT_ACCOUNT;
/* 34:   */     }
/* 35:40 */     JSONObject localJSONObject1 = new JSONObject();
/* 36:41 */     synchronized (localAccount)
/* 37:   */     {
/* 38:42 */       if (localAccount.getPublicKey() != null) {
/* 39:43 */         localJSONObject1.put("publicKey", Convert.convert(localAccount.getPublicKey()));
/* 40:   */       }
/* 41:46 */       localJSONObject1.put("balance", Long.valueOf(localAccount.getBalance()));
/* 42:47 */       localJSONObject1.put("effectiveBalance", Long.valueOf(localAccount.getEffectiveBalance() * 100L));
/* 43:   */       
/* 44:49 */       JSONArray localJSONArray = new JSONArray();
/* 45:50 */       for (Map.Entry localEntry : localAccount.getAssetBalances().entrySet())
/* 46:   */       {
/* 47:52 */         JSONObject localJSONObject2 = new JSONObject();
/* 48:53 */         localJSONObject2.put("asset", Convert.convert((Long)localEntry.getKey()));
/* 49:54 */         localJSONObject2.put("balance", localEntry.getValue());
/* 50:55 */         localJSONArray.add(localJSONObject2);
/* 51:   */       }
/* 52:58 */       if (localJSONArray.size() > 0) {
/* 53:60 */         localJSONObject1.put("assetBalances", localJSONArray);
/* 54:   */       }
/* 55:   */     }
/* 56:64 */     return localJSONObject1;
/* 57:   */   }
/* 58:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetAccount
 * JD-Core Version:    0.7.0.1
 */