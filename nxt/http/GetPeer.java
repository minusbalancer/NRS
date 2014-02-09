/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.peer.Peer;
/*  5:   */ import nxt.peer.Peer.State;
/*  6:   */ import org.json.simple.JSONObject;
/*  7:   */ import org.json.simple.JSONStreamAware;
/*  8:   */ 
/*  9:   */ final class GetPeer
/* 10:   */   extends HttpRequestHandler
/* 11:   */ {
/* 12:14 */   static final GetPeer instance = new GetPeer();
/* 13:   */   
/* 14:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 15:   */   {
/* 16:21 */     String str = paramHttpServletRequest.getParameter("peer");
/* 17:22 */     if (str == null) {
/* 18:23 */       return JSONResponses.MISSING_PEER;
/* 19:   */     }
/* 20:26 */     Peer localPeer = Peer.getPeer(str);
/* 21:27 */     if (localPeer == null) {
/* 22:28 */       return JSONResponses.UNKNOWN_PEER;
/* 23:   */     }
/* 24:31 */     JSONObject localJSONObject = new JSONObject();
/* 25:32 */     localJSONObject.put("state", Integer.valueOf(localPeer.getState().ordinal()));
/* 26:33 */     localJSONObject.put("announcedAddress", localPeer.getAnnouncedAddress());
/* 27:34 */     if (localPeer.getHallmark() != null) {
/* 28:35 */       localJSONObject.put("hallmark", localPeer.getHallmark());
/* 29:   */     }
/* 30:37 */     localJSONObject.put("weight", Integer.valueOf(localPeer.getWeight()));
/* 31:38 */     localJSONObject.put("downloadedVolume", Long.valueOf(localPeer.getDownloadedVolume()));
/* 32:39 */     localJSONObject.put("uploadedVolume", Long.valueOf(localPeer.getUploadedVolume()));
/* 33:40 */     localJSONObject.put("application", localPeer.getApplication());
/* 34:41 */     localJSONObject.put("version", localPeer.getVersion());
/* 35:42 */     localJSONObject.put("platform", localPeer.getPlatform());
/* 36:   */     
/* 37:44 */     return localJSONObject;
/* 38:   */   }
/* 39:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetPeer
 * JD-Core Version:    0.7.0.1
 */