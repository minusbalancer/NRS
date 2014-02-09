/*  1:   */ package nxt.user;
/*  2:   */ 
/*  3:   */ import java.io.IOException;
/*  4:   */ import java.net.InetAddress;
/*  5:   */ import javax.servlet.http.HttpServletRequest;
/*  6:   */ import nxt.Nxt;
/*  7:   */ import nxt.peer.Peer;
/*  8:   */ import nxt.peer.Peer.State;
/*  9:   */ import org.json.simple.JSONStreamAware;
/* 10:   */ 
/* 11:   */ final class RemoveActivePeer
/* 12:   */   extends UserRequestHandler
/* 13:   */ {
/* 14:15 */   static final RemoveActivePeer instance = new RemoveActivePeer();
/* 15:   */   
/* 16:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
/* 17:   */     throws IOException
/* 18:   */   {
/* 19:21 */     if ((Nxt.allowedUserHosts == null) && (!InetAddress.getByName(paramHttpServletRequest.getRemoteAddr()).isLoopbackAddress())) {
/* 20:22 */       return JSONResponses.LOCAL_USERS_ONLY;
/* 21:   */     }
/* 22:24 */     int i = Integer.parseInt(paramHttpServletRequest.getParameter("peer"));
/* 23:25 */     for (Peer localPeer : Peer.getAllPeers()) {
/* 24:26 */       if (localPeer.getIndex() == i)
/* 25:   */       {
/* 26:27 */         if ((localPeer.getBlacklistingTime() != 0L) || (localPeer.getState() == Peer.State.NON_CONNECTED)) {
/* 27:   */           break;
/* 28:   */         }
/* 29:28 */         localPeer.deactivate(); break;
/* 30:   */       }
/* 31:   */     }
/* 32:34 */     return null;
/* 33:   */   }
/* 34:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.RemoveActivePeer
 * JD-Core Version:    0.7.0.1
 */