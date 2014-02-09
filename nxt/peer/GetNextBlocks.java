/*  1:   */ package nxt.peer;
/*  2:   */ 
/*  3:   */ import java.util.ArrayList;
/*  4:   */ import java.util.Iterator;
/*  5:   */ import java.util.List;
/*  6:   */ import nxt.Block;
/*  7:   */ import nxt.Blockchain;
/*  8:   */ import nxt.util.Convert;
/*  9:   */ import org.json.simple.JSONArray;
/* 10:   */ import org.json.simple.JSONObject;
/* 11:   */ 
/* 12:   */ final class GetNextBlocks
/* 13:   */   extends HttpJSONRequestHandler
/* 14:   */ {
/* 15:15 */   static final GetNextBlocks instance = new GetNextBlocks();
/* 16:   */   
/* 17:   */   JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 18:   */   {
/* 19:23 */     JSONObject localJSONObject = new JSONObject();
/* 20:   */     
/* 21:25 */     ArrayList localArrayList = new ArrayList();
/* 22:26 */     int i = 0;
/* 23:27 */     Long localLong = Convert.parseUnsignedLong((String)paramJSONObject.get("blockId"));
/* 24:28 */     List localList = Blockchain.getBlocksAfter(localLong, 1440);
/* 25:30 */     for (Object localObject1 = localList.iterator(); ((Iterator)localObject1).hasNext();)
/* 26:   */     {
/* 27:30 */       localObject2 = (Block)((Iterator)localObject1).next();
/* 28:31 */       int j = 224 + ((Block)localObject2).getPayloadLength();
/* 29:32 */       if (i + j > 1048576) {
/* 30:   */         break;
/* 31:   */       }
/* 32:35 */       localArrayList.add(localObject2);
/* 33:36 */       i += j;
/* 34:   */     }
/* 35:39 */     localObject1 = new JSONArray();
/* 36:40 */     for (Object localObject2 = localArrayList.iterator(); ((Iterator)localObject2).hasNext();)
/* 37:   */     {
/* 38:40 */       Block localBlock = (Block)((Iterator)localObject2).next();
/* 39:41 */       ((JSONArray)localObject1).add(localBlock.getJSON());
/* 40:   */     }
/* 41:43 */     localJSONObject.put("nextBlocks", localObject1);
/* 42:   */     
/* 43:45 */     return localJSONObject;
/* 44:   */   }
/* 45:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.GetNextBlocks
 * JD-Core Version:    0.7.0.1
 */