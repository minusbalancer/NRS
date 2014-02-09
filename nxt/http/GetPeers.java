/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.util.Collection;
/*  4:   */ import java.util.Iterator;
/*  5:   */ import javax.servlet.http.HttpServletRequest;
/*  6:   */ import nxt.peer.Peer;
/*  7:   */ import org.json.simple.JSONArray;
/*  8:   */ import org.json.simple.JSONObject;
/*  9:   */ import org.json.simple.JSONStreamAware;
/* 10:   */ 
/* 11:   */ public final class GetPeers
/* 12:   */   extends HttpRequestHandler
/* 13:   */ {
/* 14:12 */   static final GetPeers instance = new GetPeers();
/* 15:   */   
/* 16:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 17:   */   {
/* 18:19 */     JSONArray localJSONArray = new JSONArray();
/* 19:20 */     for (Object localObject = Peer.getAllPeers().iterator(); ((Iterator)localObject).hasNext();)
/* 20:   */     {
/* 21:20 */       Peer localPeer = (Peer)((Iterator)localObject).next();
/* 22:21 */       localJSONArray.add(localPeer.getPeerAddress());
/* 23:   */     }
/* 24:24 */     localObject = new JSONObject();
/* 25:25 */     ((JSONObject)localObject).put("peers", localJSONArray);
/* 26:26 */     return localObject;
/* 27:   */   }
/* 28:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetPeers
 * JD-Core Version:    0.7.0.1
 */