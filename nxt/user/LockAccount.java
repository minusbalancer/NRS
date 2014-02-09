/*  1:   */ package nxt.user;
/*  2:   */ 
/*  3:   */ import java.io.IOException;
/*  4:   */ import javax.servlet.http.HttpServletRequest;
/*  5:   */ import org.json.simple.JSONStreamAware;
/*  6:   */ 
/*  7:   */ final class LockAccount
/*  8:   */   extends UserRequestHandler
/*  9:   */ {
/* 10:12 */   static final LockAccount instance = new LockAccount();
/* 11:   */   
/* 12:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
/* 13:   */     throws IOException
/* 14:   */   {
/* 15:19 */     paramUser.deinitializeKeyPair();
/* 16:   */     
/* 17:21 */     return JSONResponses.LOCK_ACCOUNT;
/* 18:   */   }
/* 19:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.LockAccount
 * JD-Core Version:    0.7.0.1
 */