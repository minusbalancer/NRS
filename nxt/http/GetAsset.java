/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Asset;
/*  5:   */ import nxt.util.Convert;
/*  6:   */ import org.json.simple.JSONObject;
/*  7:   */ import org.json.simple.JSONStreamAware;
/*  8:   */ 
/*  9:   */ public final class GetAsset
/* 10:   */   extends HttpRequestHandler
/* 11:   */ {
/* 12:16 */   static final GetAsset instance = new GetAsset();
/* 13:   */   
/* 14:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 15:   */   {
/* 16:23 */     String str = paramHttpServletRequest.getParameter("asset");
/* 17:24 */     if (str == null) {
/* 18:25 */       return JSONResponses.MISSING_ASSET;
/* 19:   */     }
/* 20:   */     Asset localAsset;
/* 21:   */     try
/* 22:   */     {
/* 23:30 */       localAsset = Asset.getAsset(Convert.parseUnsignedLong(str));
/* 24:31 */       if (localAsset == null) {
/* 25:32 */         return JSONResponses.UNKNOWN_ASSET;
/* 26:   */       }
/* 27:   */     }
/* 28:   */     catch (RuntimeException localRuntimeException)
/* 29:   */     {
/* 30:35 */       return JSONResponses.INCORRECT_ASSET;
/* 31:   */     }
/* 32:38 */     JSONObject localJSONObject = new JSONObject();
/* 33:39 */     localJSONObject.put("account", Convert.convert(localAsset.getAccountId()));
/* 34:40 */     localJSONObject.put("name", localAsset.getName());
/* 35:41 */     if (localAsset.getDescription().length() > 0) {
/* 36:42 */       localJSONObject.put("description", localAsset.getDescription());
/* 37:   */     }
/* 38:44 */     localJSONObject.put("quantity", Integer.valueOf(localAsset.getQuantity()));
/* 39:   */     
/* 40:46 */     return localJSONObject;
/* 41:   */   }
/* 42:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetAsset
 * JD-Core Version:    0.7.0.1
 */