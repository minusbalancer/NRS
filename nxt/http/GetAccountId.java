/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Account;
/*  5:   */ import nxt.crypto.Crypto;
/*  6:   */ import nxt.util.Convert;
/*  7:   */ import org.json.simple.JSONObject;
/*  8:   */ import org.json.simple.JSONStreamAware;
/*  9:   */ 
/* 10:   */ final class GetAccountId
/* 11:   */   extends HttpRequestHandler
/* 12:   */ {
/* 13:15 */   static final GetAccountId instance = new GetAccountId();
/* 14:   */   
/* 15:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 16:   */   {
/* 17:22 */     String str = paramHttpServletRequest.getParameter("secretPhrase");
/* 18:23 */     if (str == null) {
/* 19:24 */       return JSONResponses.MISSING_SECRET_PHRASE;
/* 20:   */     }
/* 21:27 */     byte[] arrayOfByte = Crypto.getPublicKey(str);
/* 22:   */     
/* 23:29 */     JSONObject localJSONObject = new JSONObject();
/* 24:30 */     localJSONObject.put("accountId", Convert.convert(Account.getId(arrayOfByte)));
/* 25:   */     
/* 26:32 */     return localJSONObject;
/* 27:   */   }
/* 28:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetAccountId
 * JD-Core Version:    0.7.0.1
 */