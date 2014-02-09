/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.util.Convert;
/*  5:   */ import org.json.simple.JSONObject;
/*  6:   */ import org.json.simple.JSONStreamAware;
/*  7:   */ 
/*  8:   */ public final class GetTime
/*  9:   */   extends HttpRequestHandler
/* 10:   */ {
/* 11:11 */   static final GetTime instance = new GetTime();
/* 12:   */   
/* 13:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 14:   */   {
/* 15:18 */     JSONObject localJSONObject = new JSONObject();
/* 16:19 */     localJSONObject.put("time", Integer.valueOf(Convert.getEpochTime()));
/* 17:   */     
/* 18:21 */     return localJSONObject;
/* 19:   */   }
/* 20:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetTime
 * JD-Core Version:    0.7.0.1
 */