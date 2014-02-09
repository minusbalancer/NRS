/*   1:    */ package nxt.http;
/*   2:    */ 
/*   3:    */ import javax.servlet.http.HttpServletRequest;
/*   4:    */ import nxt.Account;
/*   5:    */ import nxt.Attachment.ColoredCoinsBidOrderCancellation;
/*   6:    */ import nxt.Blockchain;
/*   7:    */ import nxt.Genesis;
/*   8:    */ import nxt.NxtException.ValidationException;
/*   9:    */ import nxt.Order.Bid;
/*  10:    */ import nxt.Transaction;
/*  11:    */ import nxt.crypto.Crypto;
/*  12:    */ import nxt.util.Convert;
/*  13:    */ import org.json.simple.JSONObject;
/*  14:    */ import org.json.simple.JSONStreamAware;
/*  15:    */ 
/*  16:    */ public final class CancelBidOrder
/*  17:    */   extends HttpRequestHandler
/*  18:    */ {
/*  19: 30 */   static final CancelBidOrder instance = new CancelBidOrder();
/*  20:    */   
/*  21:    */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/*  22:    */     throws NxtException.ValidationException
/*  23:    */   {
/*  24: 37 */     String str1 = paramHttpServletRequest.getParameter("secretPhrase");
/*  25: 38 */     String str2 = paramHttpServletRequest.getParameter("order");
/*  26: 39 */     String str3 = paramHttpServletRequest.getParameter("fee");
/*  27: 40 */     String str4 = paramHttpServletRequest.getParameter("deadline");
/*  28: 41 */     String str5 = paramHttpServletRequest.getParameter("referencedTransaction");
/*  29: 43 */     if (str1 == null) {
/*  30: 44 */       return JSONResponses.MISSING_SECRET_PHRASE;
/*  31:    */     }
/*  32: 45 */     if (str2 == null) {
/*  33: 46 */       return JSONResponses.MISSING_ORDER;
/*  34:    */     }
/*  35: 47 */     if (str3 == null) {
/*  36: 48 */       return JSONResponses.MISSING_FEE;
/*  37:    */     }
/*  38: 49 */     if (str4 == null) {
/*  39: 50 */       return JSONResponses.MISSING_DEADLINE;
/*  40:    */     }
/*  41:    */     Long localLong1;
/*  42:    */     try
/*  43:    */     {
/*  44: 55 */       localLong1 = Convert.parseUnsignedLong(str2);
/*  45:    */     }
/*  46:    */     catch (RuntimeException localRuntimeException)
/*  47:    */     {
/*  48: 57 */       return JSONResponses.INCORRECT_ORDER;
/*  49:    */     }
/*  50:    */     int i;
/*  51:    */     try
/*  52:    */     {
/*  53: 62 */       i = Integer.parseInt(str3);
/*  54: 63 */       if ((i <= 0) || (i >= 1000000000L)) {
/*  55: 64 */         return JSONResponses.INCORRECT_FEE;
/*  56:    */       }
/*  57:    */     }
/*  58:    */     catch (NumberFormatException localNumberFormatException1)
/*  59:    */     {
/*  60: 67 */       return JSONResponses.INCORRECT_FEE;
/*  61:    */     }
/*  62:    */     short s;
/*  63:    */     try
/*  64:    */     {
/*  65: 72 */       s = Short.parseShort(str4);
/*  66: 73 */       if ((s < 1) || (s > 1440)) {
/*  67: 74 */         return JSONResponses.INCORRECT_DEADLINE;
/*  68:    */       }
/*  69:    */     }
/*  70:    */     catch (NumberFormatException localNumberFormatException2)
/*  71:    */     {
/*  72: 77 */       return JSONResponses.INCORRECT_DEADLINE;
/*  73:    */     }
/*  74: 80 */     Long localLong2 = str5 == null ? null : Convert.parseUnsignedLong(str5);
/*  75:    */     
/*  76: 82 */     byte[] arrayOfByte = Crypto.getPublicKey(str1);
/*  77: 83 */     Long localLong3 = Account.getId(arrayOfByte);
/*  78:    */     
/*  79: 85 */     Order.Bid localBid = Order.Bid.getBidOrder(localLong1);
/*  80: 86 */     if ((localBid == null) || (!localBid.getAccount().getId().equals(localLong3))) {
/*  81: 87 */       return JSONResponses.UNKNOWN_ORDER;
/*  82:    */     }
/*  83: 90 */     Account localAccount = Account.getAccount(localLong3);
/*  84: 91 */     if ((localAccount == null) || (i * 100L > localAccount.getUnconfirmedBalance())) {
/*  85: 92 */       return JSONResponses.NOT_ENOUGH_FUNDS;
/*  86:    */     }
/*  87: 95 */     int j = Convert.getEpochTime();
/*  88: 96 */     Attachment.ColoredCoinsBidOrderCancellation localColoredCoinsBidOrderCancellation = new Attachment.ColoredCoinsBidOrderCancellation(localLong1);
/*  89: 97 */     Transaction localTransaction = Transaction.newTransaction(j, s, arrayOfByte, Genesis.CREATOR_ID, 0, i, localLong2, localColoredCoinsBidOrderCancellation);
/*  90:    */     
/*  91: 99 */     localTransaction.sign(str1);
/*  92:    */     
/*  93:101 */     Blockchain.broadcast(localTransaction);
/*  94:    */     
/*  95:103 */     JSONObject localJSONObject = new JSONObject();
/*  96:104 */     localJSONObject.put("transaction", localTransaction.getStringId());
/*  97:105 */     return localJSONObject;
/*  98:    */   }
/*  99:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.CancelBidOrder
 * JD-Core Version:    0.7.0.1
 */