/*  1:   */ package nxt.peer;
/*  2:   */ 
/*  3:   */ import nxt.Block;
/*  4:   */ import nxt.Blockchain;
/*  5:   */ import nxt.util.Convert;
/*  6:   */ import org.json.simple.JSONArray;
/*  7:   */ import org.json.simple.JSONObject;
/*  8:   */ 
/*  9:   */ final class GetNextBlockIds
/* 10:   */   extends HttpJSONRequestHandler
/* 11:   */ {
/* 12:11 */   static final GetNextBlockIds instance = new GetNextBlockIds();
/* 13:   */   
/* 14:   */   public JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 15:   */   {
/* 16:19 */     JSONObject localJSONObject = new JSONObject();
/* 17:   */     
/* 18:21 */     JSONArray localJSONArray = new JSONArray();
/* 19:22 */     Block localBlock = Blockchain.getBlock(Convert.parseUnsignedLong((String)paramJSONObject.get("blockId")));
/* 20:23 */     while ((localBlock != null) && (localBlock.getNextBlockId() != null) && (localJSONArray.size() < 1440))
/* 21:   */     {
/* 22:25 */       localBlock = Blockchain.getBlock(localBlock.getNextBlockId());
/* 23:26 */       if (localBlock != null) {
/* 24:28 */         localJSONArray.add(localBlock.getStringId());
/* 25:   */       }
/* 26:   */     }
/* 27:33 */     localJSONObject.put("nextBlockIds", localJSONArray);
/* 28:   */     
/* 29:35 */     return localJSONObject;
/* 30:   */   }
/* 31:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.GetNextBlockIds
 * JD-Core Version:    0.7.0.1
 */