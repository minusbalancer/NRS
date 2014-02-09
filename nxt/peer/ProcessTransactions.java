/*  1:   */ package nxt.peer;
/*  2:   */ 
/*  3:   */ import nxt.Blockchain;
/*  4:   */ import nxt.NxtException.ValidationException;
/*  5:   */ import nxt.util.JSON;
/*  6:   */ import org.json.simple.JSONObject;
/*  7:   */ import org.json.simple.JSONStreamAware;
/*  8:   */ 
/*  9:   */ final class ProcessTransactions
/* 10:   */   extends HttpJSONRequestHandler
/* 11:   */ {
/* 12:11 */   static final ProcessTransactions instance = new ProcessTransactions();
/* 13:   */   
/* 14:   */   public JSONStreamAware processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 15:   */   {
/* 16:   */     try
/* 17:   */     {
/* 18:21 */       Blockchain.processTransactions(paramJSONObject);
/* 19:   */     }
/* 20:   */     catch (NxtException.ValidationException localValidationException)
/* 21:   */     {
/* 22:24 */       paramPeer.blacklist(localValidationException);
/* 23:   */     }
/* 24:27 */     return JSON.emptyJSON;
/* 25:   */   }
/* 26:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.ProcessTransactions
 * JD-Core Version:    0.7.0.1
 */