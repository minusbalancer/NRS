/*  1:   */ package nxt.peer;
/*  2:   */ 
/*  3:   */ import org.json.simple.JSONArray;
/*  4:   */ import org.json.simple.JSONObject;
/*  5:   */ 
/*  6:   */ final class GetPeers
/*  7:   */   extends HttpJSONRequestHandler
/*  8:   */ {
/*  9: 8 */   static final GetPeers instance = new GetPeers();
/* 10:   */   
/* 11:   */   JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 12:   */   {
/* 13:16 */     JSONObject localJSONObject = new JSONObject();
/* 14:   */     
/* 15:18 */     JSONArray localJSONArray = new JSONArray();
/* 16:19 */     for (Peer localPeer : Peer.getAllPeers()) {
/* 17:21 */       if ((!localPeer.isBlacklisted()) && (localPeer.getAnnouncedAddress() != null) && (localPeer.getState() == Peer.State.CONNECTED) && (localPeer.shareAddress())) {
/* 18:24 */         localJSONArray.add(localPeer.getAnnouncedAddress());
/* 19:   */       }
/* 20:   */     }
/* 21:29 */     localJSONObject.put("peers", localJSONArray);
/* 22:   */     
/* 23:31 */     return localJSONObject;
/* 24:   */   }
/* 25:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.GetPeers
 * JD-Core Version:    0.7.0.1
 */