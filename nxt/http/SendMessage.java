/*   1:    */ package nxt.http;
/*   2:    */ 
/*   3:    */ import javax.servlet.http.HttpServletRequest;
/*   4:    */ import nxt.Account;
/*   5:    */ import nxt.Attachment.MessagingArbitraryMessage;
/*   6:    */ import nxt.Blockchain;
/*   7:    */ import nxt.NxtException.ValidationException;
/*   8:    */ import nxt.Transaction;
/*   9:    */ import nxt.crypto.Crypto;
/*  10:    */ import nxt.util.Convert;
/*  11:    */ import org.json.simple.JSONObject;
/*  12:    */ import org.json.simple.JSONStreamAware;
/*  13:    */ 
/*  14:    */ final class SendMessage
/*  15:    */   extends HttpRequestHandler
/*  16:    */ {
/*  17: 30 */   static final SendMessage instance = new SendMessage();
/*  18:    */   
/*  19:    */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/*  20:    */     throws NxtException.ValidationException
/*  21:    */   {
/*  22: 37 */     String str1 = paramHttpServletRequest.getParameter("secretPhrase");
/*  23: 38 */     String str2 = paramHttpServletRequest.getParameter("recipient");
/*  24: 39 */     String str3 = paramHttpServletRequest.getParameter("message");
/*  25: 40 */     String str4 = paramHttpServletRequest.getParameter("fee");
/*  26: 41 */     String str5 = paramHttpServletRequest.getParameter("deadline");
/*  27: 42 */     String str6 = paramHttpServletRequest.getParameter("referencedTransaction");
/*  28: 43 */     if (str1 == null) {
/*  29: 44 */       return JSONResponses.MISSING_SECRET_PHRASE;
/*  30:    */     }
/*  31: 45 */     if ((str2 == null) || ("0".equals(str2))) {
/*  32: 46 */       return JSONResponses.MISSING_RECIPIENT;
/*  33:    */     }
/*  34: 47 */     if (str3 == null) {
/*  35: 48 */       return JSONResponses.MISSING_MESSAGE;
/*  36:    */     }
/*  37: 49 */     if (str4 == null) {
/*  38: 50 */       return JSONResponses.MISSING_FEE;
/*  39:    */     }
/*  40: 51 */     if (str5 == null) {
/*  41: 52 */       return JSONResponses.MISSING_DEADLINE;
/*  42:    */     }
/*  43:    */     Long localLong1;
/*  44:    */     try
/*  45:    */     {
/*  46: 57 */       localLong1 = Convert.parseUnsignedLong(str2);
/*  47:    */     }
/*  48:    */     catch (RuntimeException localRuntimeException1)
/*  49:    */     {
/*  50: 59 */       return JSONResponses.INCORRECT_RECIPIENT;
/*  51:    */     }
/*  52:    */     byte[] arrayOfByte1;
/*  53:    */     try
/*  54:    */     {
/*  55: 64 */       arrayOfByte1 = Convert.convert(str3);
/*  56:    */     }
/*  57:    */     catch (RuntimeException localRuntimeException2)
/*  58:    */     {
/*  59: 66 */       return JSONResponses.INCORRECT_ARBITRARY_MESSAGE;
/*  60:    */     }
/*  61: 68 */     if (arrayOfByte1.length > 1000) {
/*  62: 69 */       return JSONResponses.INCORRECT_ARBITRARY_MESSAGE;
/*  63:    */     }
/*  64:    */     int i;
/*  65:    */     try
/*  66:    */     {
/*  67: 74 */       i = Integer.parseInt(str4);
/*  68: 75 */       if ((i <= 0) || (i >= 1000000000L)) {
/*  69: 76 */         return JSONResponses.INCORRECT_FEE;
/*  70:    */       }
/*  71:    */     }
/*  72:    */     catch (NumberFormatException localNumberFormatException1)
/*  73:    */     {
/*  74: 79 */       return JSONResponses.INCORRECT_FEE;
/*  75:    */     }
/*  76:    */     short s;
/*  77:    */     try
/*  78:    */     {
/*  79: 84 */       s = Short.parseShort(str5);
/*  80: 85 */       if ((s < 1) || (s > 1440)) {
/*  81: 86 */         return JSONResponses.INCORRECT_DEADLINE;
/*  82:    */       }
/*  83:    */     }
/*  84:    */     catch (NumberFormatException localNumberFormatException2)
/*  85:    */     {
/*  86: 89 */       return JSONResponses.INCORRECT_DEADLINE;
/*  87:    */     }
/*  88:    */     Long localLong2;
/*  89:    */     try
/*  90:    */     {
/*  91: 94 */       localLong2 = str6 == null ? null : Convert.parseUnsignedLong(str6);
/*  92:    */     }
/*  93:    */     catch (RuntimeException localRuntimeException3)
/*  94:    */     {
/*  95: 96 */       return JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
/*  96:    */     }
/*  97: 99 */     byte[] arrayOfByte2 = Crypto.getPublicKey(str1);
/*  98:    */     
/*  99:101 */     Account localAccount = Account.getAccount(arrayOfByte2);
/* 100:102 */     if ((localAccount == null) || (i * 100L > localAccount.getUnconfirmedBalance())) {
/* 101:103 */       return JSONResponses.NOT_ENOUGH_FUNDS;
/* 102:    */     }
/* 103:105 */     int j = Convert.getEpochTime();
/* 104:    */     
/* 105:107 */     Attachment.MessagingArbitraryMessage localMessagingArbitraryMessage = new Attachment.MessagingArbitraryMessage(arrayOfByte1);
/* 106:108 */     Transaction localTransaction = Transaction.newTransaction(j, s, arrayOfByte2, localLong1, 0, i, localLong2, localMessagingArbitraryMessage);
/* 107:    */     
/* 108:110 */     localTransaction.sign(str1);
/* 109:    */     
/* 110:112 */     Blockchain.broadcast(localTransaction);
/* 111:    */     
/* 112:114 */     JSONObject localJSONObject = new JSONObject();
/* 113:115 */     localJSONObject.put("transaction", localTransaction.getStringId());
/* 114:116 */     localJSONObject.put("bytes", Convert.convert(localTransaction.getBytes()));
/* 115:    */     
/* 116:118 */     return localJSONObject;
/* 117:    */   }
/* 118:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.SendMessage
 * JD-Core Version:    0.7.0.1
 */