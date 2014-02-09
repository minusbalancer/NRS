/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Block;
/*  5:   */ import nxt.Blockchain;
/*  6:   */ import nxt.Transaction;
/*  7:   */ import nxt.util.Convert;
/*  8:   */ import org.json.simple.JSONObject;
/*  9:   */ import org.json.simple.JSONStreamAware;
/* 10:   */ 
/* 11:   */ public final class GetTransactionBytes
/* 12:   */   extends HttpRequestHandler
/* 13:   */ {
/* 14:18 */   static final GetTransactionBytes instance = new GetTransactionBytes();
/* 15:   */   
/* 16:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 17:   */   {
/* 18:25 */     String str = paramHttpServletRequest.getParameter("transaction");
/* 19:26 */     if (str == null) {
/* 20:27 */       return JSONResponses.MISSING_TRANSACTION;
/* 21:   */     }
/* 22:   */     Long localLong;
/* 23:   */     Transaction localTransaction;
/* 24:   */     try
/* 25:   */     {
/* 26:33 */       localLong = Convert.parseUnsignedLong(str);
/* 27:34 */       localTransaction = Blockchain.getTransaction(localLong);
/* 28:   */     }
/* 29:   */     catch (RuntimeException localRuntimeException)
/* 30:   */     {
/* 31:36 */       return JSONResponses.INCORRECT_TRANSACTION;
/* 32:   */     }
/* 33:39 */     JSONObject localJSONObject = new JSONObject();
/* 34:40 */     if (localTransaction == null)
/* 35:   */     {
/* 36:41 */       localTransaction = Blockchain.getUnconfirmedTransaction(localLong);
/* 37:42 */       if (localTransaction == null) {
/* 38:43 */         return JSONResponses.UNKNOWN_TRANSACTION;
/* 39:   */       }
/* 40:45 */       localJSONObject.put("bytes", Convert.convert(localTransaction.getBytes()));
/* 41:   */     }
/* 42:   */     else
/* 43:   */     {
/* 44:48 */       localJSONObject.put("bytes", Convert.convert(localTransaction.getBytes()));
/* 45:49 */       Block localBlock = localTransaction.getBlock();
/* 46:50 */       localJSONObject.put("confirmations", Integer.valueOf(Blockchain.getLastBlock().getHeight() - localBlock.getHeight() + 1));
/* 47:   */     }
/* 48:53 */     return localJSONObject;
/* 49:   */   }
/* 50:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetTransactionBytes
 * JD-Core Version:    0.7.0.1
 */