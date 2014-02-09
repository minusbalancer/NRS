/*  1:   */ package nxt.peer;
/*  2:   */ 
/*  3:   */ import java.util.ArrayList;
/*  4:   */ import java.util.List;
/*  5:   */ import nxt.Block;
/*  6:   */ import nxt.Blockchain;
/*  7:   */ import nxt.util.Convert;
/*  8:   */ import org.json.simple.JSONArray;
/*  9:   */ import org.json.simple.JSONObject;
/* 10:   */ 
/* 11:   */ final class GetNextBlocks
/* 12:   */   extends HttpJSONRequestHandler
/* 13:   */ {
/* 14:15 */   static final GetNextBlocks instance = new GetNextBlocks();
/* 15:   */   
/* 16:   */   public JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 17:   */   {
/* 18:23 */     JSONObject localJSONObject = new JSONObject();
/* 19:   */     
/* 20:25 */     ArrayList localArrayList = new ArrayList();
/* 21:26 */     int i = 0;
/* 22:27 */     Block localBlock1 = Blockchain.getBlock(Convert.parseUnsignedLong((String)paramJSONObject.get("blockId")));
/* 23:28 */     while ((localBlock1 != null) && (localBlock1.getNextBlockId() != null))
/* 24:   */     {
/* 25:30 */       localBlock1 = Blockchain.getBlock(localBlock1.getNextBlockId());
/* 26:31 */       if (localBlock1 != null)
/* 27:   */       {
/* 28:33 */         int j = 224 + localBlock1.getPayloadLength();
/* 29:34 */         if (i + j > 1048576) {
/* 30:   */           break;
/* 31:   */         }
/* 32:40 */         localArrayList.add(localBlock1);
/* 33:41 */         i += j;
/* 34:   */       }
/* 35:   */     }
/* 36:47 */     JSONArray localJSONArray = new JSONArray();
/* 37:48 */     for (Block localBlock2 : localArrayList) {
/* 38:50 */       localJSONArray.add(localBlock2.getJSON());
/* 39:   */     }
/* 40:53 */     localJSONObject.put("nextBlocks", localJSONArray);
/* 41:   */     
/* 42:55 */     return localJSONObject;
/* 43:   */   }
/* 44:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.GetNextBlocks
 * JD-Core Version:    0.7.0.1
 */