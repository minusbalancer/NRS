/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Account;
/*  5:   */ import nxt.Block;
/*  6:   */ import nxt.Blockchain;
/*  7:   */ import nxt.util.Convert;
/*  8:   */ import nxt.util.DbIterator;
/*  9:   */ import org.json.simple.JSONArray;
/* 10:   */ import org.json.simple.JSONObject;
/* 11:   */ import org.json.simple.JSONStreamAware;
/* 12:   */ 
/* 13:   */ public final class GetAccountBlockIds
/* 14:   */   extends HttpRequestHandler
/* 15:   */ {
/* 16:22 */   static final GetAccountBlockIds instance = new GetAccountBlockIds();
/* 17:   */   
/* 18:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 19:   */   {
/* 20:29 */     String str1 = paramHttpServletRequest.getParameter("account");
/* 21:30 */     String str2 = paramHttpServletRequest.getParameter("timestamp");
/* 22:31 */     if (str1 == null) {
/* 23:32 */       return JSONResponses.MISSING_ACCOUNT;
/* 24:   */     }
/* 25:33 */     if (str2 == null) {
/* 26:34 */       return JSONResponses.MISSING_TIMESTAMP;
/* 27:   */     }
/* 28:   */     Account localAccount;
/* 29:   */     try
/* 30:   */     {
/* 31:39 */       localAccount = Account.getAccount(Convert.parseUnsignedLong(str1));
/* 32:40 */       if (localAccount == null) {
/* 33:41 */         return JSONResponses.UNKNOWN_ACCOUNT;
/* 34:   */       }
/* 35:   */     }
/* 36:   */     catch (RuntimeException localRuntimeException)
/* 37:   */     {
/* 38:44 */       return JSONResponses.INCORRECT_ACCOUNT;
/* 39:   */     }
/* 40:   */     int i;
/* 41:   */     try
/* 42:   */     {
/* 43:49 */       i = Integer.parseInt(str2);
/* 44:50 */       if (i < 0) {
/* 45:51 */         return JSONResponses.INCORRECT_TIMESTAMP;
/* 46:   */       }
/* 47:   */     }
/* 48:   */     catch (NumberFormatException localNumberFormatException)
/* 49:   */     {
/* 50:54 */       return JSONResponses.INCORRECT_TIMESTAMP;
/* 51:   */     }
/* 52:57 */     JSONArray localJSONArray = new JSONArray();
/* 53:58 */     Object localObject1 = Blockchain.getAllBlocks(localAccount, i);Object localObject2 = null;
/* 54:   */     try
/* 55:   */     {
/* 56:59 */       while (((DbIterator)localObject1).hasNext())
/* 57:   */       {
/* 58:60 */         Block localBlock = (Block)((DbIterator)localObject1).next();
/* 59:61 */         localJSONArray.add(localBlock.getStringId());
/* 60:   */       }
/* 61:   */     }
/* 62:   */     catch (Throwable localThrowable2)
/* 63:   */     {
/* 64:58 */       localObject2 = localThrowable2;throw localThrowable2;
/* 65:   */     }
/* 66:   */     finally
/* 67:   */     {
/* 68:63 */       if (localObject1 != null) {
/* 69:63 */         if (localObject2 != null) {
/* 70:   */           try
/* 71:   */           {
/* 72:63 */             ((DbIterator)localObject1).close();
/* 73:   */           }
/* 74:   */           catch (Throwable localThrowable3)
/* 75:   */           {
/* 76:63 */             localObject2.addSuppressed(localThrowable3);
/* 77:   */           }
/* 78:   */         } else {
/* 79:63 */           ((DbIterator)localObject1).close();
/* 80:   */         }
/* 81:   */       }
/* 82:   */     }
/* 83:65 */     localObject1 = new JSONObject();
/* 84:66 */     ((JSONObject)localObject1).put("blockIds", localJSONArray);
/* 85:   */     
/* 86:68 */     return localObject1;
/* 87:   */   }
/* 88:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetAccountBlockIds
 * JD-Core Version:    0.7.0.1
 */