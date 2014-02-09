/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Account;
/*  5:   */ import nxt.Alias;
/*  6:   */ import nxt.util.Convert;
/*  7:   */ import org.json.simple.JSONObject;
/*  8:   */ import org.json.simple.JSONStreamAware;
/*  9:   */ 
/* 10:   */ public final class GetAlias
/* 11:   */   extends HttpRequestHandler
/* 12:   */ {
/* 13:16 */   static final GetAlias instance = new GetAlias();
/* 14:   */   
/* 15:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 16:   */   {
/* 17:23 */     String str = paramHttpServletRequest.getParameter("alias");
/* 18:24 */     if (str == null) {
/* 19:25 */       return JSONResponses.MISSING_ALIAS;
/* 20:   */     }
/* 21:   */     Alias localAlias;
/* 22:   */     try
/* 23:   */     {
/* 24:30 */       localAlias = Alias.getAlias(Convert.parseUnsignedLong(str));
/* 25:31 */       if (localAlias == null) {
/* 26:32 */         return JSONResponses.UNKNOWN_ALIAS;
/* 27:   */       }
/* 28:   */     }
/* 29:   */     catch (RuntimeException localRuntimeException)
/* 30:   */     {
/* 31:35 */       return JSONResponses.INCORRECT_ALIAS;
/* 32:   */     }
/* 33:38 */     JSONObject localJSONObject = new JSONObject();
/* 34:39 */     localJSONObject.put("account", Convert.convert(localAlias.getAccount().getId()));
/* 35:40 */     localJSONObject.put("alias", localAlias.getAliasName());
/* 36:41 */     if (localAlias.getURI().length() > 0) {
/* 37:42 */       localJSONObject.put("uri", localAlias.getURI());
/* 38:   */     }
/* 39:44 */     localJSONObject.put("timestamp", Integer.valueOf(localAlias.getTimestamp()));
/* 40:45 */     return localJSONObject;
/* 41:   */   }
/* 42:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetAlias
 * JD-Core Version:    0.7.0.1
 */