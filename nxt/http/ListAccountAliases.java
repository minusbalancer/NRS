/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.util.Collection;
/*  4:   */ import java.util.Iterator;
/*  5:   */ import javax.servlet.http.HttpServletRequest;
/*  6:   */ import nxt.Account;
/*  7:   */ import nxt.Alias;
/*  8:   */ import nxt.util.Convert;
/*  9:   */ import org.json.simple.JSONArray;
/* 10:   */ import org.json.simple.JSONObject;
/* 11:   */ import org.json.simple.JSONStreamAware;
/* 12:   */ 
/* 13:   */ public final class ListAccountAliases
/* 14:   */   extends HttpRequestHandler
/* 15:   */ {
/* 16:18 */   static final ListAccountAliases instance = new ListAccountAliases();
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
/* 27:33 */       Long localLong = Convert.parseUnsignedLong(str);
/* 28:34 */       localAccount = Account.getAccount(localLong);
/* 29:35 */       if (localAccount == null) {
/* 30:36 */         return JSONResponses.UNKNOWN_ACCOUNT;
/* 31:   */       }
/* 32:   */     }
/* 33:   */     catch (RuntimeException localRuntimeException)
/* 34:   */     {
/* 35:39 */       return JSONResponses.INCORRECT_ACCOUNT;
/* 36:   */     }
/* 37:42 */     JSONArray localJSONArray = new JSONArray();
/* 38:43 */     for (Object localObject = Alias.getAllAliases().iterator(); ((Iterator)localObject).hasNext();)
/* 39:   */     {
/* 40:43 */       Alias localAlias = (Alias)((Iterator)localObject).next();
/* 41:44 */       if (localAlias.getAccount().equals(localAccount))
/* 42:   */       {
/* 43:45 */         JSONObject localJSONObject = new JSONObject();
/* 44:46 */         localJSONObject.put("alias", localAlias.getAliasName());
/* 45:47 */         localJSONObject.put("uri", localAlias.getURI());
/* 46:48 */         localJSONArray.add(localJSONObject);
/* 47:   */       }
/* 48:   */     }
/* 49:52 */     localObject = new JSONObject();
/* 50:53 */     ((JSONObject)localObject).put("aliases", localJSONArray);
/* 51:   */     
/* 52:55 */     return localObject;
/* 53:   */   }
/* 54:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.ListAccountAliases
 * JD-Core Version:    0.7.0.1
 */