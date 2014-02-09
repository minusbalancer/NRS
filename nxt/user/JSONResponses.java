/*  1:   */ package nxt.user;
/*  2:   */ 
/*  3:   */ import nxt.util.JSON;
/*  4:   */ import org.json.simple.JSONObject;
/*  5:   */ import org.json.simple.JSONStreamAware;
/*  6:   */ 
/*  7:   */ final class JSONResponses
/*  8:   */ {
/*  9:   */   static final JSONStreamAware INVALID_SECRET_PHRASE;
/* 10:   */   static final JSONStreamAware LOCK_ACCOUNT;
/* 11:   */   static final JSONStreamAware LOCAL_USERS_ONLY;
/* 12:   */   static final JSONStreamAware NOTIFY_OF_ACCEPTED_TRANSACTION;
/* 13:   */   
/* 14:   */   static
/* 15:   */   {
/* 16:11 */     JSONObject localJSONObject = new JSONObject();
/* 17:12 */     localJSONObject.put("response", "showMessage");
/* 18:13 */     localJSONObject.put("message", "Invalid secret phrase!");
/* 19:14 */     INVALID_SECRET_PHRASE = JSON.prepare(localJSONObject);
/* 20:   */     
/* 21:   */ 
/* 22:   */ 
/* 23:   */ 
/* 24:19 */     localJSONObject = new JSONObject();
/* 25:20 */     localJSONObject.put("response", "lockAccount");
/* 26:21 */     LOCK_ACCOUNT = JSON.prepare(localJSONObject);
/* 27:   */     
/* 28:   */ 
/* 29:   */ 
/* 30:   */ 
/* 31:26 */     localJSONObject = new JSONObject();
/* 32:27 */     localJSONObject.put("response", "showMessage");
/* 33:28 */     localJSONObject.put("message", "This operation is allowed to local host users only!");
/* 34:29 */     LOCAL_USERS_ONLY = JSON.prepare(localJSONObject);
/* 35:   */     
/* 36:   */ 
/* 37:   */ 
/* 38:   */ 
/* 39:34 */     localJSONObject = new JSONObject();
/* 40:35 */     localJSONObject.put("response", "notifyOfAcceptedTransaction");
/* 41:36 */     NOTIFY_OF_ACCEPTED_TRANSACTION = JSON.prepare(localJSONObject);
/* 42:   */   }
/* 43:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.JSONResponses
 * JD-Core Version:    0.7.0.1
 */