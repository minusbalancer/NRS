/*   1:    */ package nxt.http;
/*   2:    */ 
/*   3:    */ import javax.servlet.http.HttpServletRequest;
/*   4:    */ import nxt.Account;
/*   5:    */ import nxt.Blockchain;
/*   6:    */ import nxt.NxtException.ValidationException;
/*   7:    */ import nxt.Transaction;
/*   8:    */ import nxt.crypto.Crypto;
/*   9:    */ import nxt.util.Convert;
/*  10:    */ import org.json.simple.JSONObject;
/*  11:    */ import org.json.simple.JSONStreamAware;
/*  12:    */ 
/*  13:    */ final class SendMoney
/*  14:    */   extends HttpRequestHandler
/*  15:    */ {
/*  16: 29 */   static final SendMoney instance = new SendMoney();
/*  17:    */   
/*  18:    */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/*  19:    */     throws NxtException.ValidationException
/*  20:    */   {
/*  21: 36 */     String str1 = paramHttpServletRequest.getParameter("secretPhrase");
/*  22: 37 */     String str2 = paramHttpServletRequest.getParameter("recipient");
/*  23: 38 */     String str3 = paramHttpServletRequest.getParameter("amount");
/*  24: 39 */     String str4 = paramHttpServletRequest.getParameter("fee");
/*  25: 40 */     String str5 = paramHttpServletRequest.getParameter("deadline");
/*  26: 41 */     String str6 = paramHttpServletRequest.getParameter("referencedTransaction");
/*  27: 42 */     if (str1 == null) {
/*  28: 43 */       return JSONResponses.MISSING_SECRET_PHRASE;
/*  29:    */     }
/*  30: 44 */     if ((str2 == null) || ("0".equals(str2))) {
/*  31: 45 */       return JSONResponses.MISSING_RECIPIENT;
/*  32:    */     }
/*  33: 46 */     if (str3 == null) {
/*  34: 47 */       return JSONResponses.MISSING_AMOUNT;
/*  35:    */     }
/*  36: 48 */     if (str4 == null) {
/*  37: 49 */       return JSONResponses.MISSING_FEE;
/*  38:    */     }
/*  39: 50 */     if (str5 == null) {
/*  40: 51 */       return JSONResponses.MISSING_DEADLINE;
/*  41:    */     }
/*  42:    */     Long localLong1;
/*  43:    */     try
/*  44:    */     {
/*  45: 56 */       localLong1 = Convert.parseUnsignedLong(str2);
/*  46:    */     }
/*  47:    */     catch (RuntimeException localRuntimeException1)
/*  48:    */     {
/*  49: 58 */       return JSONResponses.INCORRECT_RECIPIENT;
/*  50:    */     }
/*  51:    */     int i;
/*  52:    */     try
/*  53:    */     {
/*  54: 63 */       i = Integer.parseInt(str3);
/*  55: 64 */       if ((i <= 0) || (i >= 1000000000L)) {
/*  56: 65 */         return JSONResponses.INCORRECT_AMOUNT;
/*  57:    */       }
/*  58:    */     }
/*  59:    */     catch (NumberFormatException localNumberFormatException1)
/*  60:    */     {
/*  61: 68 */       return JSONResponses.INCORRECT_AMOUNT;
/*  62:    */     }
/*  63:    */     int j;
/*  64:    */     try
/*  65:    */     {
/*  66: 73 */       j = Integer.parseInt(str4);
/*  67: 74 */       if ((j <= 0) || (j >= 1000000000L)) {
/*  68: 75 */         return JSONResponses.INCORRECT_FEE;
/*  69:    */       }
/*  70:    */     }
/*  71:    */     catch (NumberFormatException localNumberFormatException2)
/*  72:    */     {
/*  73: 78 */       return JSONResponses.INCORRECT_FEE;
/*  74:    */     }
/*  75:    */     short s;
/*  76:    */     try
/*  77:    */     {
/*  78: 84 */       s = Short.parseShort(str5);
/*  79: 85 */       if ((s < 1) || (s > 1440)) {
/*  80: 86 */         return JSONResponses.INCORRECT_DEADLINE;
/*  81:    */       }
/*  82:    */     }
/*  83:    */     catch (NumberFormatException localNumberFormatException3)
/*  84:    */     {
/*  85: 89 */       return JSONResponses.INCORRECT_DEADLINE;
/*  86:    */     }
/*  87:    */     Long localLong2;
/*  88:    */     try
/*  89:    */     {
/*  90: 94 */       localLong2 = str6 == null ? null : Convert.parseUnsignedLong(str6);
/*  91:    */     }
/*  92:    */     catch (RuntimeException localRuntimeException2)
/*  93:    */     {
/*  94: 96 */       return JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
/*  95:    */     }
/*  96: 98 */     byte[] arrayOfByte = Crypto.getPublicKey(str1);
/*  97:    */     
/*  98:100 */     Account localAccount = Account.getAccount(arrayOfByte);
/*  99:101 */     if ((localAccount == null) || ((i + j) * 100L > localAccount.getUnconfirmedBalance())) {
/* 100:102 */       return JSONResponses.NOT_ENOUGH_FUNDS;
/* 101:    */     }
/* 102:106 */     Transaction localTransaction = Transaction.newTransaction(Convert.getEpochTime(), s, arrayOfByte, localLong1, i, j, localLong2);
/* 103:    */     
/* 104:108 */     localTransaction.sign(str1);
/* 105:    */     
/* 106:110 */     Blockchain.broadcast(localTransaction);
/* 107:    */     
/* 108:112 */     JSONObject localJSONObject = new JSONObject();
/* 109:113 */     localJSONObject.put("transaction", localTransaction.getStringId());
/* 110:114 */     localJSONObject.put("bytes", Convert.convert(localTransaction.getBytes()));
/* 111:    */     
/* 112:116 */     return localJSONObject;
/* 113:    */   }
/* 114:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.SendMoney
 * JD-Core Version:    0.7.0.1
 */