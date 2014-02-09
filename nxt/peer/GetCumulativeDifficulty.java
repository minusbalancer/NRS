/*  1:   */ package nxt.peer;
/*  2:   */ 
/*  3:   */ import java.math.BigInteger;
/*  4:   */ import nxt.Block;
/*  5:   */ import nxt.Blockchain;
/*  6:   */ import org.json.simple.JSONObject;
/*  7:   */ 
/*  8:   */ final class GetCumulativeDifficulty
/*  9:   */   extends HttpJSONRequestHandler
/* 10:   */ {
/* 11: 8 */   static final GetCumulativeDifficulty instance = new GetCumulativeDifficulty();
/* 12:   */   
/* 13:   */   JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 14:   */   {
/* 15:16 */     JSONObject localJSONObject = new JSONObject();
/* 16:   */     
/* 17:18 */     localJSONObject.put("cumulativeDifficulty", Blockchain.getLastBlock().getCumulativeDifficulty().toString());
/* 18:   */     
/* 19:20 */     return localJSONObject;
/* 20:   */   }
/* 21:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.GetCumulativeDifficulty
 * JD-Core Version:    0.7.0.1
 */