/*  1:   */ package nxt.user;
/*  2:   */ 
/*  3:   */ import java.io.IOException;
/*  4:   */ import java.util.Collections;
/*  5:   */ import java.util.HashMap;
/*  6:   */ import java.util.Map;
/*  7:   */ import javax.servlet.ServletException;
/*  8:   */ import javax.servlet.http.HttpServletRequest;
/*  9:   */ import nxt.NxtException;
/* 10:   */ import nxt.util.Logger;
/* 11:   */ import org.json.simple.JSONObject;
/* 12:   */ import org.json.simple.JSONStreamAware;
/* 13:   */ 
/* 14:   */ public abstract class UserRequestHandler
/* 15:   */ {
/* 16:   */   private static final Map<String, UserRequestHandler> userRequestHandlers;
/* 17:   */   
/* 18:   */   static
/* 19:   */   {
/* 20:21 */     HashMap localHashMap = new HashMap();
/* 21:   */     
/* 22:23 */     localHashMap.put("generateAuthorizationToken", GenerateAuthorizationToken.instance);
/* 23:24 */     localHashMap.put("getInitialData", GetInitialData.instance);
/* 24:25 */     localHashMap.put("getNewData", GetNewData.instance);
/* 25:26 */     localHashMap.put("lockAccount", LockAccount.instance);
/* 26:27 */     localHashMap.put("removeActivePeer", RemoveActivePeer.instance);
/* 27:28 */     localHashMap.put("removeBlacklistedPeer", RemoveBlacklistedPeer.instance);
/* 28:29 */     localHashMap.put("removeKnownPeer", RemoveKnownPeer.instance);
/* 29:30 */     localHashMap.put("sendMoney", SendMoney.instance);
/* 30:31 */     localHashMap.put("unlockAccount", UnlockAccount.instance);
/* 31:   */     
/* 32:33 */     userRequestHandlers = Collections.unmodifiableMap(localHashMap);
/* 33:   */   }
/* 34:   */   
/* 35:   */   abstract JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
/* 36:   */     throws NxtException, IOException;
/* 37:   */   
/* 38:   */   public static void process(HttpServletRequest paramHttpServletRequest, User paramUser)
/* 39:   */     throws ServletException, IOException
/* 40:   */   {
/* 41:   */     try
/* 42:   */     {
/* 43:39 */       String str = paramHttpServletRequest.getParameter("requestType");
/* 44:41 */       if (str != null)
/* 45:   */       {
/* 46:42 */         localObject = (UserRequestHandler)userRequestHandlers.get(str);
/* 47:43 */         if (localObject != null)
/* 48:   */         {
/* 49:44 */           JSONStreamAware localJSONStreamAware = ((UserRequestHandler)localObject).processRequest(paramHttpServletRequest, paramUser);
/* 50:45 */           if (localJSONStreamAware != null) {
/* 51:46 */             paramUser.enqueue(localJSONStreamAware);
/* 52:   */           }
/* 53:48 */           return;
/* 54:   */         }
/* 55:   */       }
/* 56:51 */       localObject = new JSONObject();
/* 57:52 */       ((JSONObject)localObject).put("response", "showMessage");
/* 58:53 */       ((JSONObject)localObject).put("message", "Incorrect request!");
/* 59:54 */       paramUser.enqueue((JSONStreamAware)localObject);
/* 60:   */     }
/* 61:   */     catch (Exception localException)
/* 62:   */     {
/* 63:57 */       Logger.logMessage("Error processing GET request", localException);
/* 64:58 */       Object localObject = new JSONObject();
/* 65:59 */       ((JSONObject)localObject).put("response", "showMessage");
/* 66:60 */       ((JSONObject)localObject).put("message", localException.toString());
/* 67:61 */       paramUser.enqueue((JSONStreamAware)localObject);
/* 68:   */     }
/* 69:   */   }
/* 70:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.UserRequestHandler
 * JD-Core Version:    0.7.0.1
 */