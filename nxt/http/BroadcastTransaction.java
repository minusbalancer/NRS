/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Blockchain;
/*  5:   */ import nxt.NxtException.ValidationException;
/*  6:   */ import nxt.Transaction;
/*  7:   */ import nxt.util.Convert;
/*  8:   */ import org.json.simple.JSONObject;
/*  9:   */ import org.json.simple.JSONStreamAware;
/* 10:   */ 
/* 11:   */ public final class BroadcastTransaction
/* 12:   */   extends HttpRequestHandler
/* 13:   */ {
/* 14:17 */   static final BroadcastTransaction instance = new BroadcastTransaction();
/* 15:   */   
/* 16:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 17:   */     throws NxtException.ValidationException
/* 18:   */   {
/* 19:24 */     String str = paramHttpServletRequest.getParameter("transactionBytes");
/* 20:25 */     if (str == null) {
/* 21:26 */       return JSONResponses.MISSING_TRANSACTION_BYTES;
/* 22:   */     }
/* 23:   */     try
/* 24:   */     {
/* 25:31 */       byte[] arrayOfByte = Convert.convert(str);
/* 26:32 */       Transaction localTransaction = Transaction.getTransaction(arrayOfByte);
/* 27:   */       
/* 28:34 */       Blockchain.broadcast(localTransaction);
/* 29:   */       
/* 30:36 */       JSONObject localJSONObject = new JSONObject();
/* 31:37 */       localJSONObject.put("transaction", localTransaction.getStringId());
/* 32:38 */       return localJSONObject;
/* 33:   */     }
/* 34:   */     catch (RuntimeException localRuntimeException) {}
/* 35:41 */     return JSONResponses.INCORRECT_TRANSACTION_BYTES;
/* 36:   */   }
/* 37:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.BroadcastTransaction
 * JD-Core Version:    0.7.0.1
 */