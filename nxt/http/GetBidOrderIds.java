/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.util.Iterator;
/*  4:   */ import java.util.SortedSet;
/*  5:   */ import javax.servlet.http.HttpServletRequest;
/*  6:   */ import nxt.Asset;
/*  7:   */ import nxt.Order.Bid;
/*  8:   */ import nxt.util.Convert;
/*  9:   */ import org.json.simple.JSONArray;
/* 10:   */ import org.json.simple.JSONObject;
/* 11:   */ import org.json.simple.JSONStreamAware;
/* 12:   */ 
/* 13:   */ public final class GetBidOrderIds
/* 14:   */   extends HttpRequestHandler
/* 15:   */ {
/* 16:19 */   static final GetBidOrderIds instance = new GetBidOrderIds();
/* 17:   */   
/* 18:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 19:   */   {
/* 20:26 */     String str = paramHttpServletRequest.getParameter("asset");
/* 21:27 */     if (str == null) {
/* 22:28 */       return JSONResponses.MISSING_ASSET;
/* 23:   */     }
/* 24:   */     long l;
/* 25:   */     try
/* 26:   */     {
/* 27:33 */       l = Convert.parseUnsignedLong(str).longValue();
/* 28:   */     }
/* 29:   */     catch (RuntimeException localRuntimeException)
/* 30:   */     {
/* 31:35 */       return JSONResponses.INCORRECT_ASSET;
/* 32:   */     }
/* 33:38 */     if (Asset.getAsset(Long.valueOf(l)) == null) {
/* 34:39 */       return JSONResponses.UNKNOWN_ASSET;
/* 35:   */     }
/* 36:42 */     JSONArray localJSONArray = new JSONArray();
/* 37:43 */     Iterator localIterator = Order.Bid.getSortedOrders(Long.valueOf(l)).iterator();
/* 38:44 */     while (localIterator.hasNext()) {
/* 39:45 */       localJSONArray.add(Convert.convert(((Order.Bid)localIterator.next()).getId()));
/* 40:   */     }
/* 41:48 */     JSONObject localJSONObject = new JSONObject();
/* 42:49 */     localJSONObject.put("bidOrderIds", localJSONArray);
/* 43:50 */     return localJSONObject;
/* 44:   */   }
/* 45:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetBidOrderIds
 * JD-Core Version:    0.7.0.1
 */