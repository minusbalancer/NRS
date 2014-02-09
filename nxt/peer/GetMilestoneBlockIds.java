/*  1:   */ package nxt.peer;
/*  2:   */ 
/*  3:   */ import nxt.Block;
/*  4:   */ import nxt.Blockchain;
/*  5:   */ import nxt.util.Convert;
/*  6:   */ import org.json.simple.JSONArray;
/*  7:   */ import org.json.simple.JSONObject;
/*  8:   */ 
/*  9:   */ final class GetMilestoneBlockIds
/* 10:   */   extends HttpJSONRequestHandler
/* 11:   */ {
/* 12:11 */   static final GetMilestoneBlockIds instance = new GetMilestoneBlockIds();
/* 13:   */   
/* 14:   */   JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 15:   */   {
/* 16:19 */     JSONObject localJSONObject = new JSONObject();
/* 17:   */     
/* 18:   */ 
/* 19:22 */     JSONArray localJSONArray = new JSONArray();
/* 20:23 */     Block localBlock = Blockchain.getLastBlock();
/* 21:24 */     long l = localBlock.getId().longValue();
/* 22:25 */     int i = localBlock.getHeight();
/* 23:26 */     int j = i * 4 / 1461 + 1;
/* 24:28 */     while (i > 0)
/* 25:   */     {
/* 26:29 */       localJSONArray.add(Convert.convert(l));
/* 27:30 */       l = Blockchain.getBlockIdAtHeight(i);
/* 28:31 */       i -= j;
/* 29:   */     }
/* 30:34 */     localJSONObject.put("milestoneBlockIds", localJSONArray);
/* 31:   */     
/* 32:36 */     return localJSONObject;
/* 33:   */   }
/* 34:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.GetMilestoneBlockIds
 * JD-Core Version:    0.7.0.1
 */