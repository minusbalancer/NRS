/*  1:   */ package nxt.peer;
/*  2:   */ 
/*  3:   */ import java.util.List;
/*  4:   */ import nxt.Blockchain;
/*  5:   */ import nxt.util.Convert;
/*  6:   */ import org.json.simple.JSONArray;
/*  7:   */ import org.json.simple.JSONObject;
/*  8:   */ 
/*  9:   */ final class GetNextBlockIds
/* 10:   */   extends HttpJSONRequestHandler
/* 11:   */ {
/* 12:12 */   static final GetNextBlockIds instance = new GetNextBlockIds();
/* 13:   */   
/* 14:   */   JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 15:   */   {
/* 16:20 */     JSONObject localJSONObject = new JSONObject();
/* 17:   */     
/* 18:22 */     JSONArray localJSONArray = new JSONArray();
/* 19:23 */     Long localLong1 = Convert.parseUnsignedLong((String)paramJSONObject.get("blockId"));
/* 20:24 */     List localList = Blockchain.getBlockIdsAfter(localLong1, 1440);
/* 21:26 */     for (Long localLong2 : localList) {
/* 22:27 */       localJSONArray.add(Convert.convert(localLong2));
/* 23:   */     }
/* 24:30 */     localJSONObject.put("nextBlockIds", localJSONArray);
/* 25:   */     
/* 26:32 */     return localJSONObject;
/* 27:   */   }
/* 28:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.GetNextBlockIds
 * JD-Core Version:    0.7.0.1
 */