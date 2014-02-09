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
/* 11:   */ final class GetTransaction
/* 12:   */   extends HttpRequestHandler
/* 13:   */ {
/* 14:18 */   static final GetTransaction instance = new GetTransaction();
/* 15:   */   
/* 16:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 17:   */   {
/* 18:25 */     String str = paramHttpServletRequest.getParameter("transaction");
/* 19:26 */     if (str == null) {
/* 20:27 */       return JSONResponses.MISSING_TRANSACTION;
/* 21:   */     }
/* 22:   */     Long localLong;
/* 23:   */     Transaction localTransaction;
/* 24:   */     try
/* 25:   */     {
/* 26:34 */       localLong = Convert.parseUnsignedLong(str);
/* 27:35 */       localTransaction = Blockchain.getTransaction(localLong);
/* 28:   */     }
/* 29:   */     catch (RuntimeException localRuntimeException)
/* 30:   */     {
/* 31:37 */       return JSONResponses.INCORRECT_TRANSACTION;
/* 32:   */     }
/* 33:   */     JSONObject localJSONObject;
/* 34:41 */     if (localTransaction == null)
/* 35:   */     {
/* 36:42 */       localTransaction = Blockchain.getUnconfirmedTransaction(localLong);
/* 37:43 */       if (localTransaction == null) {
/* 38:44 */         return JSONResponses.UNKNOWN_TRANSACTION;
/* 39:   */       }
/* 40:46 */       localJSONObject = localTransaction.getJSONObject();
/* 41:47 */       localJSONObject.put("sender", Convert.convert(localTransaction.getSenderAccountId()));
/* 42:   */     }
/* 43:   */     else
/* 44:   */     {
/* 45:50 */       localJSONObject = localTransaction.getJSONObject();
/* 46:51 */       localJSONObject.put("sender", Convert.convert(localTransaction.getSenderAccountId()));
/* 47:52 */       Block localBlock = localTransaction.getBlock();
/* 48:53 */       localJSONObject.put("block", localBlock.getStringId());
/* 49:54 */       localJSONObject.put("confirmations", Integer.valueOf(Blockchain.getLastBlock().getHeight() - localBlock.getHeight() + 1));
/* 50:   */     }
/* 51:57 */     return localJSONObject;
/* 52:   */   }
/* 53:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetTransaction
 * JD-Core Version:    0.7.0.1
 */