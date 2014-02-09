/*   1:    */ package nxt.http;
/*   2:    */ 
/*   3:    */ import javax.servlet.http.HttpServletRequest;
/*   4:    */ import nxt.Account;
/*   5:    */ import nxt.Alias;
/*   6:    */ import nxt.Attachment.MessagingAliasAssignment;
/*   7:    */ import nxt.Blockchain;
/*   8:    */ import nxt.Genesis;
/*   9:    */ import nxt.NxtException.ValidationException;
/*  10:    */ import nxt.Transaction;
/*  11:    */ import nxt.crypto.Crypto;
/*  12:    */ import nxt.util.Convert;
/*  13:    */ import org.json.simple.JSONObject;
/*  14:    */ import org.json.simple.JSONStreamAware;
/*  15:    */ 
/*  16:    */ final class AssignAlias
/*  17:    */   extends HttpRequestHandler
/*  18:    */ {
/*  19: 33 */   static final AssignAlias instance = new AssignAlias();
/*  20:    */   
/*  21:    */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/*  22:    */     throws NxtException.ValidationException
/*  23:    */   {
/*  24: 39 */     String str1 = paramHttpServletRequest.getParameter("secretPhrase");
/*  25: 40 */     String str2 = paramHttpServletRequest.getParameter("alias");
/*  26: 41 */     String str3 = paramHttpServletRequest.getParameter("uri");
/*  27: 42 */     String str4 = paramHttpServletRequest.getParameter("fee");
/*  28: 43 */     String str5 = paramHttpServletRequest.getParameter("deadline");
/*  29: 44 */     String str6 = paramHttpServletRequest.getParameter("referencedTransaction");
/*  30: 46 */     if (str1 == null) {
/*  31: 47 */       return JSONResponses.MISSING_SECRET_PHRASE;
/*  32:    */     }
/*  33: 48 */     if (str2 == null) {
/*  34: 49 */       return JSONResponses.MISSING_ALIAS;
/*  35:    */     }
/*  36: 50 */     if (str3 == null) {
/*  37: 51 */       return JSONResponses.MISSING_URI;
/*  38:    */     }
/*  39: 52 */     if (str4 == null) {
/*  40: 53 */       return JSONResponses.MISSING_FEE;
/*  41:    */     }
/*  42: 54 */     if (str5 == null) {
/*  43: 55 */       return JSONResponses.MISSING_DEADLINE;
/*  44:    */     }
/*  45: 58 */     str2 = str2.trim();
/*  46: 59 */     if ((str2.length() == 0) || (str2.length() > 100)) {
/*  47: 60 */       return JSONResponses.INCORRECT_ALIAS_LENGTH;
/*  48:    */     }
/*  49: 63 */     String str7 = str2.toLowerCase();
/*  50: 64 */     for (int i = 0; i < str7.length(); i++) {
/*  51: 65 */       if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(str7.charAt(i)) < 0) {
/*  52: 66 */         return JSONResponses.INCORRECT_ALIAS;
/*  53:    */       }
/*  54:    */     }
/*  55: 70 */     str3 = str3.trim();
/*  56: 71 */     if (str3.length() > 1000) {
/*  57: 72 */       return JSONResponses.INCORRECT_URI_LENGTH;
/*  58:    */     }
/*  59:    */     try
/*  60:    */     {
/*  61: 77 */       i = Integer.parseInt(str4);
/*  62: 78 */       if ((i <= 0) || (i >= 1000000000L)) {
/*  63: 79 */         return JSONResponses.INCORRECT_FEE;
/*  64:    */       }
/*  65:    */     }
/*  66:    */     catch (NumberFormatException localNumberFormatException1)
/*  67:    */     {
/*  68: 82 */       return JSONResponses.INCORRECT_FEE;
/*  69:    */     }
/*  70:    */     short s;
/*  71:    */     try
/*  72:    */     {
/*  73: 87 */       s = Short.parseShort(str5);
/*  74: 88 */       if ((s < 1) || (s > 1440)) {
/*  75: 89 */         return JSONResponses.INCORRECT_DEADLINE;
/*  76:    */       }
/*  77:    */     }
/*  78:    */     catch (NumberFormatException localNumberFormatException2)
/*  79:    */     {
/*  80: 92 */       return JSONResponses.INCORRECT_DEADLINE;
/*  81:    */     }
/*  82: 95 */     Long localLong = str6 == null ? null : Convert.parseUnsignedLong(str6);
/*  83: 96 */     byte[] arrayOfByte = Crypto.getPublicKey(str1);
/*  84: 97 */     Account localAccount = Account.getAccount(arrayOfByte);
/*  85: 98 */     if ((localAccount == null) || (i * 100L > localAccount.getUnconfirmedBalance())) {
/*  86: 99 */       return JSONResponses.NOT_ENOUGH_FUNDS;
/*  87:    */     }
/*  88:102 */     Alias localAlias = Alias.getAlias(str7);
/*  89:103 */     JSONObject localJSONObject = new JSONObject();
/*  90:104 */     if ((localAlias != null) && (localAlias.getAccount() != localAccount))
/*  91:    */     {
/*  92:106 */       localJSONObject.put("errorCode", Integer.valueOf(8));
/*  93:107 */       localJSONObject.put("errorDescription", "\"" + str2 + "\" is already used");
/*  94:    */     }
/*  95:    */     else
/*  96:    */     {
/*  97:111 */       int j = Convert.getEpochTime();
/*  98:112 */       Attachment.MessagingAliasAssignment localMessagingAliasAssignment = new Attachment.MessagingAliasAssignment(str2, str3);
/*  99:113 */       Transaction localTransaction = Transaction.newTransaction(j, s, arrayOfByte, Genesis.CREATOR_ID, 0, i, localLong, localMessagingAliasAssignment);
/* 100:    */       
/* 101:115 */       localTransaction.sign(str1);
/* 102:    */       
/* 103:117 */       Blockchain.broadcast(localTransaction);
/* 104:    */       
/* 105:119 */       localJSONObject.put("transaction", localTransaction.getStringId());
/* 106:    */     }
/* 107:123 */     return localJSONObject;
/* 108:    */   }
/* 109:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.AssignAlias
 * JD-Core Version:    0.7.0.1
 */