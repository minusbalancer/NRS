/*  1:   */ package nxt.peer;
/*  2:   */ 
/*  3:   */ import nxt.Block;
/*  4:   */ import nxt.Blockchain;
/*  5:   */ import org.json.simple.JSONArray;
/*  6:   */ import org.json.simple.JSONObject;
/*  7:   */ 
/*  8:   */ final class GetMilestoneBlockIds
/*  9:   */   extends HttpJSONRequestHandler
/* 10:   */ {
/* 11:10 */   static final GetMilestoneBlockIds instance = new GetMilestoneBlockIds();
/* 12:   */   
/* 13:   */   JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 14:   */   {
/* 15:18 */     JSONObject localJSONObject = new JSONObject();
/* 16:   */     
/* 17:20 */     JSONArray localJSONArray = new JSONArray();
/* 18:21 */     Block localBlock = Blockchain.getLastBlock();
/* 19:22 */     int i = localBlock.getHeight() * 4 / 1461 + 1;
/* 20:23 */     for (; (localBlock != null) && (localBlock.getHeight() > 0); goto 64)
/* 21:   */     {
/* 22:25 */       localJSONArray.add(localBlock.getStringId());
/* 23:26 */       int j = 0;
/* 24:26 */       if ((j < i) && (localBlock != null) && (localBlock.getHeight() > 0))
/* 25:   */       {
/* 26:28 */         localBlock = Blockchain.getBlock(localBlock.getPreviousBlockId());j++;
/* 27:   */       }
/* 28:   */     }
/* 29:33 */     localJSONObject.put("milestoneBlockIds", localJSONArray);
/* 30:   */     
/* 31:35 */     return localJSONObject;
/* 32:   */   }
/* 33:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.GetMilestoneBlockIds
 * JD-Core Version:    0.7.0.1
 */