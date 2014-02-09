/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.util.Collection;
/*  4:   */ import java.util.Iterator;
/*  5:   */ import javax.servlet.http.HttpServletRequest;
/*  6:   */ import nxt.Alias;
/*  7:   */ import nxt.util.Convert;
/*  8:   */ import org.json.simple.JSONArray;
/*  9:   */ import org.json.simple.JSONObject;
/* 10:   */ import org.json.simple.JSONStreamAware;
/* 11:   */ 
/* 12:   */ public final class GetAliasIds
/* 13:   */   extends HttpRequestHandler
/* 14:   */ {
/* 15:16 */   static final GetAliasIds instance = new GetAliasIds();
/* 16:   */   
/* 17:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 18:   */   {
/* 19:23 */     String str = paramHttpServletRequest.getParameter("timestamp");
/* 20:24 */     if (str == null) {
/* 21:25 */       return JSONResponses.MISSING_TIMESTAMP;
/* 22:   */     }
/* 23:   */     int i;
/* 24:   */     try
/* 25:   */     {
/* 26:30 */       i = Integer.parseInt(str);
/* 27:31 */       if (i < 0) {
/* 28:32 */         return JSONResponses.INCORRECT_TIMESTAMP;
/* 29:   */       }
/* 30:   */     }
/* 31:   */     catch (NumberFormatException localNumberFormatException)
/* 32:   */     {
/* 33:35 */       return JSONResponses.INCORRECT_TIMESTAMP;
/* 34:   */     }
/* 35:38 */     JSONArray localJSONArray = new JSONArray();
/* 36:39 */     for (Object localObject = Alias.getAllAliases().iterator(); ((Iterator)localObject).hasNext();)
/* 37:   */     {
/* 38:39 */       Alias localAlias = (Alias)((Iterator)localObject).next();
/* 39:40 */       if (localAlias.getTimestamp() >= i) {
/* 40:41 */         localJSONArray.add(Convert.convert(localAlias.getId()));
/* 41:   */       }
/* 42:   */     }
/* 43:45 */     localObject = new JSONObject();
/* 44:   */     
/* 45:47 */     ((JSONObject)localObject).put("aliasIds", localJSONArray);
/* 46:48 */     return localObject;
/* 47:   */   }
/* 48:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetAliasIds
 * JD-Core Version:    0.7.0.1
 */