/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.util.Collection;
/*  4:   */ import java.util.Iterator;
/*  5:   */ import javax.servlet.http.HttpServletRequest;
/*  6:   */ import nxt.Account;
/*  7:   */ import nxt.Order.Ask;
/*  8:   */ import nxt.util.Convert;
/*  9:   */ import org.json.simple.JSONArray;
/* 10:   */ import org.json.simple.JSONObject;
/* 11:   */ import org.json.simple.JSONStreamAware;
/* 12:   */ 
/* 13:   */ public final class GetAccountCurrentAskOrderIds
/* 14:   */   extends HttpRequestHandler
/* 15:   */ {
/* 16:18 */   static final GetAccountCurrentAskOrderIds instance = new GetAccountCurrentAskOrderIds();
/* 17:   */   
/* 18:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 19:   */   {
/* 20:25 */     String str = paramHttpServletRequest.getParameter("account");
/* 21:26 */     if (str == null) {
/* 22:27 */       return JSONResponses.MISSING_ACCOUNT;
/* 23:   */     }
/* 24:   */     Account localAccount;
/* 25:   */     try
/* 26:   */     {
/* 27:32 */       localAccount = Account.getAccount(Convert.parseUnsignedLong(str));
/* 28:33 */       if (localAccount == null) {
/* 29:34 */         return JSONResponses.UNKNOWN_ACCOUNT;
/* 30:   */       }
/* 31:   */     }
/* 32:   */     catch (RuntimeException localRuntimeException1)
/* 33:   */     {
/* 34:37 */       return JSONResponses.INCORRECT_ACCOUNT;
/* 35:   */     }
/* 36:40 */     Long localLong = null;
/* 37:   */     try
/* 38:   */     {
/* 39:42 */       localLong = Convert.parseUnsignedLong(paramHttpServletRequest.getParameter("asset"));
/* 40:   */     }
/* 41:   */     catch (RuntimeException localRuntimeException2) {}
/* 42:47 */     JSONArray localJSONArray = new JSONArray();
/* 43:48 */     for (Object localObject = Order.Ask.getAllAskOrders().iterator(); ((Iterator)localObject).hasNext();)
/* 44:   */     {
/* 45:48 */       Order.Ask localAsk = (Order.Ask)((Iterator)localObject).next();
/* 46:49 */       if (((localLong == null) || (localAsk.getAssetId().equals(localLong))) && (localAsk.getAccount().equals(localAccount))) {
/* 47:50 */         localJSONArray.add(Convert.convert(localAsk.getId()));
/* 48:   */       }
/* 49:   */     }
/* 50:54 */     localObject = new JSONObject();
/* 51:55 */     ((JSONObject)localObject).put("askOrderIds", localJSONArray);
/* 52:56 */     return localObject;
/* 53:   */   }
/* 54:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetAccountCurrentAskOrderIds
 * JD-Core Version:    0.7.0.1
 */