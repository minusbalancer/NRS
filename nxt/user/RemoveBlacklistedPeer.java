/*  1:   */ package nxt.user;
/*  2:   */ 
/*  3:   */ import java.io.IOException;
/*  4:   */ import java.net.InetAddress;
/*  5:   */ import javax.servlet.http.HttpServletRequest;
/*  6:   */ import nxt.Nxt;
/*  7:   */ import nxt.peer.Peer;
/*  8:   */ import org.json.simple.JSONStreamAware;
/*  9:   */ 
/* 10:   */ final class RemoveBlacklistedPeer
/* 11:   */   extends UserRequestHandler
/* 12:   */ {
/* 13:15 */   static final RemoveBlacklistedPeer instance = new RemoveBlacklistedPeer();
/* 14:   */   
/* 15:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
/* 16:   */     throws IOException
/* 17:   */   {
/* 18:21 */     if ((Nxt.allowedUserHosts == null) && (!InetAddress.getByName(paramHttpServletRequest.getRemoteAddr()).isLoopbackAddress())) {
/* 19:22 */       return JSONResponses.LOCAL_USERS_ONLY;
/* 20:   */     }
/* 21:24 */     int i = Integer.parseInt(paramHttpServletRequest.getParameter("peer"));
/* 22:25 */     for (Peer localPeer : Peer.getAllPeers()) {
/* 23:26 */       if (localPeer.getIndex() == i)
/* 24:   */       {
/* 25:27 */         if (localPeer.getBlacklistingTime() <= 0L) {
/* 26:   */           break;
/* 27:   */         }
/* 28:28 */         localPeer.removeBlacklistedStatus(); break;
/* 29:   */       }
/* 30:   */     }
/* 31:34 */     return null;
/* 32:   */   }
/* 33:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.RemoveBlacklistedPeer
 * JD-Core Version:    0.7.0.1
 */