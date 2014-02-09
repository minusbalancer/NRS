/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Account;
/*  5:   */ import nxt.Order.Bid;
/*  6:   */ import nxt.util.Convert;
/*  7:   */ import org.json.simple.JSONObject;
/*  8:   */ import org.json.simple.JSONStreamAware;
/*  9:   */ 
/* 10:   */ public final class GetBidOrder
/* 11:   */   extends HttpRequestHandler
/* 12:   */ {
/* 13:16 */   static final GetBidOrder instance = new GetBidOrder();
/* 14:   */   
/* 15:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 16:   */   {
/* 17:23 */     String str = paramHttpServletRequest.getParameter("order");
/* 18:24 */     if (str == null) {
/* 19:25 */       return JSONResponses.MISSING_ORDER;
/* 20:   */     }
/* 21:   */     Order.Bid localBid;
/* 22:   */     try
/* 23:   */     {
/* 24:30 */       localBid = Order.Bid.getBidOrder(Convert.parseUnsignedLong(str));
/* 25:31 */       if (localBid == null) {
/* 26:32 */         return JSONResponses.UNKNOWN_ORDER;
/* 27:   */       }
/* 28:   */     }
/* 29:   */     catch (RuntimeException localRuntimeException)
/* 30:   */     {
/* 31:35 */       return JSONResponses.INCORRECT_ORDER;
/* 32:   */     }
/* 33:38 */     JSONObject localJSONObject = new JSONObject();
/* 34:39 */     localJSONObject.put("account", Convert.convert(localBid.getAccount().getId()));
/* 35:40 */     localJSONObject.put("asset", Convert.convert(localBid.getAssetId()));
/* 36:41 */     localJSONObject.put("quantity", Integer.valueOf(localBid.getQuantity()));
/* 37:42 */     localJSONObject.put("price", Long.valueOf(localBid.getPrice()));
/* 38:   */     
/* 39:44 */     return localJSONObject;
/* 40:   */   }
/* 41:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetBidOrder
 * JD-Core Version:    0.7.0.1
 */