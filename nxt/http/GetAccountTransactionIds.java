/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.util.Arrays;
/*  4:   */ import java.util.Collection;
/*  5:   */ import java.util.Iterator;
/*  6:   */ import java.util.PriorityQueue;
/*  7:   */ import javax.servlet.http.HttpServletRequest;
/*  8:   */ import nxt.Account;
/*  9:   */ import nxt.Block;
/* 10:   */ import nxt.Blockchain;
/* 11:   */ import nxt.Transaction;
/* 12:   */ import nxt.Transaction.Type;
/* 13:   */ import nxt.util.Convert;
/* 14:   */ import org.json.simple.JSONArray;
/* 15:   */ import org.json.simple.JSONObject;
/* 16:   */ import org.json.simple.JSONStreamAware;
/* 17:   */ 
/* 18:   */ final class GetAccountTransactionIds
/* 19:   */   extends HttpRequestHandler
/* 20:   */ {
/* 21:23 */   static final GetAccountTransactionIds instance = new GetAccountTransactionIds();
/* 22:   */   
/* 23:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 24:   */   {
/* 25:30 */     String str1 = paramHttpServletRequest.getParameter("account");
/* 26:31 */     String str2 = paramHttpServletRequest.getParameter("timestamp");
/* 27:32 */     if (str1 == null) {
/* 28:33 */       return JSONResponses.MISSING_ACCOUNT;
/* 29:   */     }
/* 30:34 */     if (str2 == null) {
/* 31:35 */       return JSONResponses.MISSING_TIMESTAMP;
/* 32:   */     }
/* 33:   */     Account localAccount;
/* 34:   */     try
/* 35:   */     {
/* 36:40 */       localAccount = Account.getAccount(Convert.parseUnsignedLong(str1));
/* 37:41 */       if (localAccount == null) {
/* 38:42 */         return JSONResponses.UNKNOWN_ACCOUNT;
/* 39:   */       }
/* 40:   */     }
/* 41:   */     catch (RuntimeException localRuntimeException)
/* 42:   */     {
/* 43:45 */       return JSONResponses.INCORRECT_ACCOUNT;
/* 44:   */     }
/* 45:   */     int i;
/* 46:   */     try
/* 47:   */     {
/* 48:50 */       i = Integer.parseInt(str2);
/* 49:51 */       if (i < 0) {
/* 50:52 */         return JSONResponses.INCORRECT_TIMESTAMP;
/* 51:   */       }
/* 52:   */     }
/* 53:   */     catch (NumberFormatException localNumberFormatException1)
/* 54:   */     {
/* 55:55 */       return JSONResponses.INCORRECT_TIMESTAMP;
/* 56:   */     }
/* 57:   */     int j;
/* 58:   */     try
/* 59:   */     {
/* 60:61 */       j = Integer.parseInt(paramHttpServletRequest.getParameter("type"));
/* 61:   */     }
/* 62:   */     catch (NumberFormatException localNumberFormatException2)
/* 63:   */     {
/* 64:63 */       j = -1;
/* 65:   */     }
/* 66:   */     int k;
/* 67:   */     try
/* 68:   */     {
/* 69:66 */       k = Integer.parseInt(paramHttpServletRequest.getParameter("subtype"));
/* 70:   */     }
/* 71:   */     catch (NumberFormatException localNumberFormatException3)
/* 72:   */     {
/* 73:68 */       k = -1;
/* 74:   */     }
/* 75:71 */     PriorityQueue localPriorityQueue = new PriorityQueue(11, Transaction.timestampComparator);
/* 76:72 */     byte[] arrayOfByte = localAccount.getPublicKey();
/* 77:73 */     for (Object localObject1 = Blockchain.getAllTransactions().iterator(); ((Iterator)localObject1).hasNext();)
/* 78:   */     {
/* 79:73 */       localObject2 = (Transaction)((Iterator)localObject1).next();
/* 80:74 */       if (((((Transaction)localObject2).getRecipientId().equals(localAccount.getId())) || (Arrays.equals(((Transaction)localObject2).getSenderPublicKey(), arrayOfByte))) && ((j < 0) || (((Transaction)localObject2).getType().getType() == j)) && ((k < 0) || (((Transaction)localObject2).getType().getSubtype() == k)) && (((Transaction)localObject2).getBlock().getTimestamp() >= i)) {
/* 81:77 */         localPriorityQueue.offer(localObject2);
/* 82:   */       }
/* 83:   */     }
/* 84:80 */     localObject1 = new JSONArray();
/* 85:81 */     while (!localPriorityQueue.isEmpty()) {
/* 86:82 */       ((JSONArray)localObject1).add(((Transaction)localPriorityQueue.poll()).getStringId());
/* 87:   */     }
/* 88:85 */     Object localObject2 = new JSONObject();
/* 89:86 */     ((JSONObject)localObject2).put("transactionIds", localObject1);
/* 90:87 */     return localObject2;
/* 91:   */   }
/* 92:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetAccountTransactionIds
 * JD-Core Version:    0.7.0.1
 */