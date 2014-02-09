/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Alias;
/*  5:   */ import nxt.util.JSON;
/*  6:   */ import org.json.simple.JSONObject;
/*  7:   */ import org.json.simple.JSONStreamAware;
/*  8:   */ 
/*  9:   */ final class GetAliasURI
/* 10:   */   extends HttpRequestHandler
/* 11:   */ {
/* 12:15 */   static final GetAliasURI instance = new GetAliasURI();
/* 13:   */   
/* 14:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 15:   */   {
/* 16:22 */     String str = paramHttpServletRequest.getParameter("alias");
/* 17:23 */     if (str == null) {
/* 18:24 */       return JSONResponses.MISSING_ALIAS;
/* 19:   */     }
/* 20:27 */     Alias localAlias = Alias.getAlias(str.toLowerCase());
/* 21:28 */     if (localAlias == null) {
/* 22:29 */       return JSONResponses.UNKNOWN_ALIAS;
/* 23:   */     }
/* 24:32 */     if (localAlias.getURI().length() > 0)
/* 25:   */     {
/* 26:34 */       JSONObject localJSONObject = new JSONObject();
/* 27:35 */       localJSONObject.put("uri", localAlias.getURI());
/* 28:36 */       return localJSONObject;
/* 29:   */     }
/* 30:39 */     return JSON.emptyJSON;
/* 31:   */   }
/* 32:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetAliasURI
 * JD-Core Version:    0.7.0.1
 */