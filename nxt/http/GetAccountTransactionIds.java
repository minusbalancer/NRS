/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Account;
/*  5:   */ import nxt.Blockchain;
/*  6:   */ import nxt.Transaction;
/*  7:   */ import nxt.util.Convert;
/*  8:   */ import nxt.util.DbIterator;
/*  9:   */ import org.json.simple.JSONArray;
/* 10:   */ import org.json.simple.JSONObject;
/* 11:   */ import org.json.simple.JSONStreamAware;
/* 12:   */ 
/* 13:   */ public final class GetAccountTransactionIds
/* 14:   */   extends HttpRequestHandler
/* 15:   */ {
/* 16:22 */   static final GetAccountTransactionIds instance = new GetAccountTransactionIds();
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
/* 48:   */     catch (NumberFormatException localNumberFormatException1)
/* 49:   */     {
/* 50:54 */       return JSONResponses.INCORRECT_TIMESTAMP;
/* 51:   */     }
/* 52:   */     byte b1;
/* 53:   */     try
/* 54:   */     {
/* 55:60 */       b1 = Byte.parseByte(paramHttpServletRequest.getParameter("type"));
/* 56:   */     }
/* 57:   */     catch (NumberFormatException localNumberFormatException2)
/* 58:   */     {
/* 59:62 */       b1 = -1;
/* 60:   */     }
/* 61:   */     byte b2;
/* 62:   */     try
/* 63:   */     {
/* 64:65 */       b2 = Byte.parseByte(paramHttpServletRequest.getParameter("subtype"));
/* 65:   */     }
/* 66:   */     catch (NumberFormatException localNumberFormatException3)
/* 67:   */     {
/* 68:67 */       b2 = -1;
/* 69:   */     }
/* 70:70 */     JSONArray localJSONArray = new JSONArray();
/* 71:71 */     Object localObject1 = Blockchain.getAllTransactions(localAccount, b1, b2, i);Object localObject2 = null;
/* 72:   */     try
/* 73:   */     {
/* 74:72 */       while (((DbIterator)localObject1).hasNext())
/* 75:   */       {
/* 76:73 */         Transaction localTransaction = (Transaction)((DbIterator)localObject1).next();
/* 77:74 */         localJSONArray.add(localTransaction.getStringId());
/* 78:   */       }
/* 79:   */     }
/* 80:   */     catch (Throwable localThrowable2)
/* 81:   */     {
/* 82:71 */       localObject2 = localThrowable2;throw localThrowable2;
/* 83:   */     }
/* 84:   */     finally
/* 85:   */     {
/* 86:76 */       if (localObject1 != null) {
/* 87:76 */         if (localObject2 != null) {
/* 88:   */           try
/* 89:   */           {
/* 90:76 */             ((DbIterator)localObject1).close();
/* 91:   */           }
/* 92:   */           catch (Throwable localThrowable3)
/* 93:   */           {
/* 94:76 */             localObject2.addSuppressed(localThrowable3);
/* 95:   */           }
/* 96:   */         } else {
/* 97:76 */           ((DbIterator)localObject1).close();
/* 98:   */         }
/* 99:   */       }
/* :0:   */     }
/* :1:78 */     localObject1 = new JSONObject();
/* :2:79 */     ((JSONObject)localObject1).put("transactionIds", localJSONArray);
/* :3:80 */     return localObject1;
/* :4:   */   }
/* :5:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetAccountTransactionIds
 * JD-Core Version:    0.7.0.1
 */