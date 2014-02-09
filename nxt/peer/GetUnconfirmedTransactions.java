/*  1:   */ package nxt.peer;
/*  2:   */ 
/*  3:   */ import nxt.Blockchain;
/*  4:   */ import nxt.Transaction;
/*  5:   */ import org.json.simple.JSONArray;
/*  6:   */ import org.json.simple.JSONObject;
/*  7:   */ 
/*  8:   */ final class GetUnconfirmedTransactions
/*  9:   */   extends HttpJSONRequestHandler
/* 10:   */ {
/* 11:10 */   static final GetUnconfirmedTransactions instance = new GetUnconfirmedTransactions();
/* 12:   */   
/* 13:   */   public JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 14:   */   {
/* 15:18 */     JSONObject localJSONObject = new JSONObject();
/* 16:   */     
/* 17:20 */     JSONArray localJSONArray = new JSONArray();
/* 18:21 */     for (Transaction localTransaction : Blockchain.getAllUnconfirmedTransactions()) {
/* 19:23 */       localJSONArray.add(localTransaction.getJSONObject());
/* 20:   */     }
/* 21:26 */     localJSONObject.put("unconfirmedTransactions", localJSONArray);
/* 22:   */     
/* 23:   */ 
/* 24:29 */     return localJSONObject;
/* 25:   */   }
/* 26:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.GetUnconfirmedTransactions
 * JD-Core Version:    0.7.0.1
 */