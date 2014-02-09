/*   1:    */ package nxt.http;
/*   2:    */ 
/*   3:    */ import javax.servlet.http.HttpServletRequest;
/*   4:    */ import nxt.Account;
/*   5:    */ import nxt.Attachment.ColoredCoinsAssetTransfer;
/*   6:    */ import nxt.Blockchain;
/*   7:    */ import nxt.NxtException.ValidationException;
/*   8:    */ import nxt.Transaction;
/*   9:    */ import nxt.crypto.Crypto;
/*  10:    */ import nxt.util.Convert;
/*  11:    */ import org.json.simple.JSONObject;
/*  12:    */ import org.json.simple.JSONStreamAware;
/*  13:    */ 
/*  14:    */ public final class TransferAsset
/*  15:    */   extends HttpRequestHandler
/*  16:    */ {
/*  17: 32 */   static final TransferAsset instance = new TransferAsset();
/*  18:    */   
/*  19:    */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/*  20:    */     throws NxtException.ValidationException
/*  21:    */   {
/*  22: 39 */     String str1 = paramHttpServletRequest.getParameter("secretPhrase");
/*  23: 40 */     String str2 = paramHttpServletRequest.getParameter("recipient");
/*  24: 41 */     String str3 = paramHttpServletRequest.getParameter("asset");
/*  25: 42 */     String str4 = paramHttpServletRequest.getParameter("quantity");
/*  26: 43 */     String str5 = paramHttpServletRequest.getParameter("fee");
/*  27: 44 */     String str6 = paramHttpServletRequest.getParameter("deadline");
/*  28: 45 */     String str7 = paramHttpServletRequest.getParameter("referencedTransaction");
/*  29: 46 */     if (str1 == null) {
/*  30: 47 */       return JSONResponses.MISSING_SECRET_PHRASE;
/*  31:    */     }
/*  32: 48 */     if ((str2 == null) || ("0".equals(str2))) {
/*  33: 49 */       return JSONResponses.MISSING_RECIPIENT;
/*  34:    */     }
/*  35: 50 */     if (str3 == null) {
/*  36: 51 */       return JSONResponses.MISSING_ASSET;
/*  37:    */     }
/*  38: 52 */     if (str4 == null) {
/*  39: 53 */       return JSONResponses.MISSING_QUANTITY;
/*  40:    */     }
/*  41: 54 */     if (str5 == null) {
/*  42: 55 */       return JSONResponses.MISSING_FEE;
/*  43:    */     }
/*  44: 56 */     if (str6 == null) {
/*  45: 57 */       return JSONResponses.MISSING_DEADLINE;
/*  46:    */     }
/*  47:    */     Long localLong1;
/*  48:    */     try
/*  49:    */     {
/*  50: 62 */       localLong1 = Convert.parseUnsignedLong(str2);
/*  51:    */     }
/*  52:    */     catch (RuntimeException localRuntimeException1)
/*  53:    */     {
/*  54: 64 */       return JSONResponses.INCORRECT_RECIPIENT;
/*  55:    */     }
/*  56:    */     Long localLong2;
/*  57:    */     try
/*  58:    */     {
/*  59: 69 */       localLong2 = Convert.parseUnsignedLong(str3);
/*  60:    */     }
/*  61:    */     catch (RuntimeException localRuntimeException2)
/*  62:    */     {
/*  63: 71 */       return JSONResponses.INCORRECT_ASSET;
/*  64:    */     }
/*  65:    */     int i;
/*  66:    */     try
/*  67:    */     {
/*  68: 76 */       i = Integer.parseInt(str4);
/*  69: 77 */       if ((i <= 0) || (i >= 1000000000L)) {
/*  70: 78 */         return JSONResponses.INCORRECT_QUANTITY;
/*  71:    */       }
/*  72:    */     }
/*  73:    */     catch (NumberFormatException localNumberFormatException1)
/*  74:    */     {
/*  75: 81 */       return JSONResponses.INCORRECT_QUANTITY;
/*  76:    */     }
/*  77:    */     int j;
/*  78:    */     try
/*  79:    */     {
/*  80: 86 */       j = Integer.parseInt(str5);
/*  81: 87 */       if ((j <= 0) || (j >= 1000000000L)) {
/*  82: 88 */         return JSONResponses.INCORRECT_FEE;
/*  83:    */       }
/*  84:    */     }
/*  85:    */     catch (NumberFormatException localNumberFormatException2)
/*  86:    */     {
/*  87: 91 */       return JSONResponses.INCORRECT_FEE;
/*  88:    */     }
/*  89:    */     short s;
/*  90:    */     try
/*  91:    */     {
/*  92: 96 */       s = Short.parseShort(str6);
/*  93: 97 */       if ((s < 1) || (s > 1440)) {
/*  94: 98 */         return JSONResponses.INCORRECT_DEADLINE;
/*  95:    */       }
/*  96:    */     }
/*  97:    */     catch (NumberFormatException localNumberFormatException3)
/*  98:    */     {
/*  99:101 */       return JSONResponses.INCORRECT_DEADLINE;
/* 100:    */     }
/* 101:    */     Long localLong3;
/* 102:    */     try
/* 103:    */     {
/* 104:106 */       localLong3 = str7 == null ? null : Convert.parseUnsignedLong(str7);
/* 105:    */     }
/* 106:    */     catch (RuntimeException localRuntimeException3)
/* 107:    */     {
/* 108:108 */       return JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
/* 109:    */     }
/* 110:111 */     byte[] arrayOfByte = Crypto.getPublicKey(str1);
/* 111:    */     
/* 112:113 */     Account localAccount = Account.getAccount(arrayOfByte);
/* 113:114 */     if ((localAccount == null) || (j * 100L > localAccount.getUnconfirmedBalance())) {
/* 114:115 */       return JSONResponses.NOT_ENOUGH_FUNDS;
/* 115:    */     }
/* 116:118 */     Integer localInteger = localAccount.getUnconfirmedAssetBalance(localLong2);
/* 117:119 */     if ((localInteger == null) || (i > localInteger.intValue())) {
/* 118:120 */       return JSONResponses.NOT_ENOUGH_FUNDS;
/* 119:    */     }
/* 120:123 */     int k = Convert.getEpochTime();
/* 121:    */     
/* 122:125 */     Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = new Attachment.ColoredCoinsAssetTransfer(localLong2, i);
/* 123:126 */     Transaction localTransaction = Transaction.newTransaction(k, s, arrayOfByte, localLong1, 0, j, localLong3, localColoredCoinsAssetTransfer);
/* 124:    */     
/* 125:128 */     localTransaction.sign(str1);
/* 126:    */     
/* 127:130 */     Blockchain.broadcast(localTransaction);
/* 128:    */     
/* 129:132 */     JSONObject localJSONObject = new JSONObject();
/* 130:133 */     localJSONObject.put("transaction", localTransaction.getStringId());
/* 131:134 */     return localJSONObject;
/* 132:    */   }
/* 133:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.TransferAsset
 * JD-Core Version:    0.7.0.1
 */