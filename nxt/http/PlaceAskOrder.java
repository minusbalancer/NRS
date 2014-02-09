/*   1:    */ package nxt.http;
/*   2:    */ 
/*   3:    */ import javax.servlet.http.HttpServletRequest;
/*   4:    */ import nxt.Account;
/*   5:    */ import nxt.Attachment.ColoredCoinsAskOrderPlacement;
/*   6:    */ import nxt.Blockchain;
/*   7:    */ import nxt.Genesis;
/*   8:    */ import nxt.NxtException.ValidationException;
/*   9:    */ import nxt.Transaction;
/*  10:    */ import nxt.crypto.Crypto;
/*  11:    */ import nxt.util.Convert;
/*  12:    */ import org.json.simple.JSONObject;
/*  13:    */ import org.json.simple.JSONStreamAware;
/*  14:    */ 
/*  15:    */ final class PlaceAskOrder
/*  16:    */   extends HttpRequestHandler
/*  17:    */ {
/*  18: 33 */   static final PlaceAskOrder instance = new PlaceAskOrder();
/*  19:    */   
/*  20:    */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/*  21:    */     throws NxtException.ValidationException
/*  22:    */   {
/*  23: 40 */     String str1 = paramHttpServletRequest.getParameter("secretPhrase");
/*  24: 41 */     String str2 = paramHttpServletRequest.getParameter("asset");
/*  25: 42 */     String str3 = paramHttpServletRequest.getParameter("quantity");
/*  26: 43 */     String str4 = paramHttpServletRequest.getParameter("price");
/*  27: 44 */     String str5 = paramHttpServletRequest.getParameter("fee");
/*  28: 45 */     String str6 = paramHttpServletRequest.getParameter("deadline");
/*  29: 46 */     String str7 = paramHttpServletRequest.getParameter("referencedTransaction");
/*  30: 47 */     if (str1 == null) {
/*  31: 48 */       return JSONResponses.MISSING_SECRET_PHRASE;
/*  32:    */     }
/*  33: 49 */     if (str2 == null) {
/*  34: 50 */       return JSONResponses.MISSING_ASSET;
/*  35:    */     }
/*  36: 51 */     if (str3 == null) {
/*  37: 52 */       return JSONResponses.MISSING_QUANTITY;
/*  38:    */     }
/*  39: 53 */     if (str4 == null) {
/*  40: 54 */       return JSONResponses.MISSING_PRICE;
/*  41:    */     }
/*  42: 55 */     if (str5 == null) {
/*  43: 56 */       return JSONResponses.MISSING_FEE;
/*  44:    */     }
/*  45: 57 */     if (str6 == null) {
/*  46: 58 */       return JSONResponses.MISSING_DEADLINE;
/*  47:    */     }
/*  48:    */     long l;
/*  49:    */     try
/*  50:    */     {
/*  51: 63 */       l = Long.parseLong(str4);
/*  52: 64 */       if ((l <= 0L) || (l > 100000000000L)) {
/*  53: 65 */         return JSONResponses.INCORRECT_PRICE;
/*  54:    */       }
/*  55:    */     }
/*  56:    */     catch (NumberFormatException localNumberFormatException1)
/*  57:    */     {
/*  58: 68 */       return JSONResponses.INCORRECT_PRICE;
/*  59:    */     }
/*  60:    */     Long localLong1;
/*  61:    */     try
/*  62:    */     {
/*  63: 73 */       localLong1 = Convert.parseUnsignedLong(str2);
/*  64:    */     }
/*  65:    */     catch (RuntimeException localRuntimeException1)
/*  66:    */     {
/*  67: 75 */       return JSONResponses.INCORRECT_ASSET;
/*  68:    */     }
/*  69:    */     int i;
/*  70:    */     try
/*  71:    */     {
/*  72: 80 */       i = Integer.parseInt(str3);
/*  73: 81 */       if ((i <= 0) || (i >= 1000000000L)) {
/*  74: 82 */         return JSONResponses.INCORRECT_QUANTITY;
/*  75:    */       }
/*  76:    */     }
/*  77:    */     catch (NumberFormatException localNumberFormatException2)
/*  78:    */     {
/*  79: 85 */       return JSONResponses.INCORRECT_QUANTITY;
/*  80:    */     }
/*  81:    */     int j;
/*  82:    */     try
/*  83:    */     {
/*  84: 90 */       j = Integer.parseInt(str5);
/*  85: 91 */       if ((j <= 0) || (j >= 1000000000L)) {
/*  86: 92 */         return JSONResponses.INCORRECT_FEE;
/*  87:    */       }
/*  88:    */     }
/*  89:    */     catch (NumberFormatException localNumberFormatException3)
/*  90:    */     {
/*  91: 95 */       return JSONResponses.INCORRECT_FEE;
/*  92:    */     }
/*  93:    */     short s;
/*  94:    */     try
/*  95:    */     {
/*  96:100 */       s = Short.parseShort(str6);
/*  97:101 */       if ((s < 1) || (s > 1440)) {
/*  98:102 */         return JSONResponses.INCORRECT_DEADLINE;
/*  99:    */       }
/* 100:    */     }
/* 101:    */     catch (NumberFormatException localNumberFormatException4)
/* 102:    */     {
/* 103:105 */       return JSONResponses.INCORRECT_DEADLINE;
/* 104:    */     }
/* 105:    */     Long localLong2;
/* 106:    */     try
/* 107:    */     {
/* 108:111 */       localLong2 = str7 == null ? null : Convert.parseUnsignedLong(str7);
/* 109:    */     }
/* 110:    */     catch (RuntimeException localRuntimeException2)
/* 111:    */     {
/* 112:113 */       return JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
/* 113:    */     }
/* 114:116 */     byte[] arrayOfByte = Crypto.getPublicKey(str1);
/* 115:    */     
/* 116:118 */     Account localAccount = Account.getAccount(arrayOfByte);
/* 117:119 */     if ((localAccount == null) || (j * 100L > localAccount.getUnconfirmedBalance())) {
/* 118:120 */       return JSONResponses.NOT_ENOUGH_FUNDS;
/* 119:    */     }
/* 120:123 */     Integer localInteger = localAccount.getUnconfirmedAssetBalance(localLong1);
/* 121:124 */     if ((localInteger == null) || (i > localInteger.intValue())) {
/* 122:125 */       return JSONResponses.NOT_ENOUGH_FUNDS;
/* 123:    */     }
/* 124:128 */     int k = Convert.getEpochTime();
/* 125:    */     
/* 126:130 */     Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = new Attachment.ColoredCoinsAskOrderPlacement(localLong1, i, l);
/* 127:131 */     Transaction localTransaction = Transaction.newTransaction(k, s, arrayOfByte, Genesis.CREATOR_ID, 0, j, localLong2, localColoredCoinsAskOrderPlacement);
/* 128:    */     
/* 129:133 */     localTransaction.sign(str1);
/* 130:    */     
/* 131:135 */     Blockchain.broadcast(localTransaction);
/* 132:    */     
/* 133:137 */     JSONObject localJSONObject = new JSONObject();
/* 134:138 */     localJSONObject.put("transaction", localTransaction.getStringId());
/* 135:139 */     return localJSONObject;
/* 136:    */   }
/* 137:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.PlaceAskOrder
 * JD-Core Version:    0.7.0.1
 */