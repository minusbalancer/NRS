/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import org.json.simple.JSONObject;
/*  5:   */ import org.json.simple.JSONStreamAware;
/*  6:   */ 
/*  7:   */ public final class GetMyInfo
/*  8:   */   extends HttpRequestHandler
/*  9:   */ {
/* 10:10 */   static final GetMyInfo instance = new GetMyInfo();
/* 11:   */   
/* 12:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 13:   */   {
/* 14:17 */     JSONObject localJSONObject = new JSONObject();
/* 15:18 */     localJSONObject.put("host", paramHttpServletRequest.getRemoteHost());
/* 16:19 */     localJSONObject.put("address", paramHttpServletRequest.getRemoteAddr());
/* 17:20 */     return localJSONObject;
/* 18:   */   }
/* 19:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetMyInfo
 * JD-Core Version:    0.7.0.1
 */