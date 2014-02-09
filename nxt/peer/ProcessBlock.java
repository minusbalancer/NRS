/*  1:   */ package nxt.peer;
/*  2:   */ 
/*  3:   */ import nxt.Blockchain;
/*  4:   */ import nxt.NxtException;
/*  5:   */ import nxt.util.JSON;
/*  6:   */ import org.json.simple.JSONObject;
/*  7:   */ import org.json.simple.JSONStreamAware;
/*  8:   */ 
/*  9:   */ final class ProcessBlock
/* 10:   */   extends HttpJSONRequestHandler
/* 11:   */ {
/* 12:11 */   static final ProcessBlock instance = new ProcessBlock();
/* 13:   */   private static final JSONStreamAware ACCEPTED;
/* 14:   */   private static final JSONStreamAware NOT_ACCEPTED;
/* 15:   */   
/* 16:   */   static
/* 17:   */   {
/* 18:17 */     JSONObject localJSONObject = new JSONObject();
/* 19:18 */     localJSONObject.put("accepted", Boolean.valueOf(true));
/* 20:19 */     ACCEPTED = JSON.prepare(localJSONObject);
/* 21:   */     
/* 22:   */ 
/* 23:   */ 
/* 24:   */ 
/* 25:24 */     localJSONObject = new JSONObject();
/* 26:25 */     localJSONObject.put("accepted", Boolean.valueOf(false));
/* 27:26 */     NOT_ACCEPTED = JSON.prepare(localJSONObject);
/* 28:   */   }
/* 29:   */   
/* 30:   */   JSONStreamAware processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 31:   */   {
/* 32:   */     try
/* 33:   */     {
/* 34:34 */       boolean bool = Blockchain.pushBlock(paramJSONObject);
/* 35:35 */       return bool ? ACCEPTED : NOT_ACCEPTED;
/* 36:   */     }
/* 37:   */     catch (NxtException localNxtException)
/* 38:   */     {
/* 39:38 */       if (paramPeer != null) {
/* 40:39 */         paramPeer.blacklist(localNxtException);
/* 41:   */       }
/* 42:   */     }
/* 43:41 */     return NOT_ACCEPTED;
/* 44:   */   }
/* 45:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.ProcessBlock
 * JD-Core Version:    0.7.0.1
 */