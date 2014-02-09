/*  1:   */ package nxt.user;
/*  2:   */ 
/*  3:   */ import java.io.IOException;
/*  4:   */ import java.net.InetAddress;
/*  5:   */ import javax.servlet.http.HttpServletRequest;
/*  6:   */ import nxt.Nxt;
/*  7:   */ import nxt.peer.Peer;
/*  8:   */ import org.json.simple.JSONStreamAware;
/*  9:   */ 
/* 10:   */ final class RemoveKnownPeer
/* 11:   */   extends UserRequestHandler
/* 12:   */ {
/* 13:15 */   static final RemoveKnownPeer instance = new RemoveKnownPeer();
/* 14:   */   
/* 15:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
/* 16:   */     throws IOException
/* 17:   */   {
/* 18:21 */     if ((Nxt.allowedUserHosts == null) && (!InetAddress.getByName(paramHttpServletRequest.getRemoteAddr()).isLoopbackAddress())) {
/* 19:22 */       return JSONResponses.LOCAL_USERS_ONLY;
/* 20:   */     }
/* 21:24 */     int i = Integer.parseInt(paramHttpServletRequest.getParameter("peer"));
/* 22:25 */     for (Peer localPeer : Peer.getAllPeers()) {
/* 23:26 */       if (localPeer.getIndex() == i)
/* 24:   */       {
/* 25:27 */         localPeer.removePeer();
/* 26:28 */         break;
/* 27:   */       }
/* 28:   */     }
/* 29:32 */     return null;
/* 30:   */   }
/* 31:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.RemoveKnownPeer
 * JD-Core Version:    0.7.0.1
 */