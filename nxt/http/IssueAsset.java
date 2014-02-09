/*   1:    */ package nxt.http;
/*   2:    */ 
/*   3:    */ import javax.servlet.http.HttpServletRequest;
/*   4:    */ import nxt.Account;
/*   5:    */ import nxt.Asset;
/*   6:    */ import nxt.Attachment.ColoredCoinsAssetIssuance;
/*   7:    */ import nxt.Blockchain;
/*   8:    */ import nxt.Genesis;
/*   9:    */ import nxt.NxtException.ValidationException;
/*  10:    */ import nxt.Transaction;
/*  11:    */ import nxt.crypto.Crypto;
/*  12:    */ import nxt.util.Convert;
/*  13:    */ import org.json.simple.JSONObject;
/*  14:    */ import org.json.simple.JSONStreamAware;
/*  15:    */ 
/*  16:    */ final class IssueAsset
/*  17:    */   extends HttpRequestHandler
/*  18:    */ {
/*  19: 34 */   static final IssueAsset instance = new IssueAsset();
/*  20:    */   
/*  21:    */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/*  22:    */     throws NxtException.ValidationException
/*  23:    */   {
/*  24: 41 */     String str1 = paramHttpServletRequest.getParameter("secretPhrase");
/*  25: 42 */     String str2 = paramHttpServletRequest.getParameter("name");
/*  26: 43 */     String str3 = paramHttpServletRequest.getParameter("description");
/*  27: 44 */     String str4 = paramHttpServletRequest.getParameter("quantity");
/*  28: 45 */     String str5 = paramHttpServletRequest.getParameter("fee");
/*  29: 46 */     if (str1 == null) {
/*  30: 47 */       return JSONResponses.MISSING_SECRET_PHRASE;
/*  31:    */     }
/*  32: 48 */     if (str2 == null) {
/*  33: 49 */       return JSONResponses.MISSING_NAME;
/*  34:    */     }
/*  35: 50 */     if (str4 == null) {
/*  36: 51 */       return JSONResponses.MISSING_QUANTITY;
/*  37:    */     }
/*  38: 52 */     if (str5 == null) {
/*  39: 53 */       return JSONResponses.MISSING_FEE;
/*  40:    */     }
/*  41: 56 */     str2 = str2.trim();
/*  42: 57 */     if ((str2.length() < 3) || (str2.length() > 10)) {
/*  43: 58 */       return JSONResponses.INCORRECT_ASSET_NAME_LENGTH;
/*  44:    */     }
/*  45: 61 */     String str6 = str2.toLowerCase();
/*  46: 62 */     for (int i = 0; i < str6.length(); i++) {
/*  47: 63 */       if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(str6.charAt(i)) < 0) {
/*  48: 64 */         return JSONResponses.INCORRECT_ASSET_NAME;
/*  49:    */       }
/*  50:    */     }
/*  51: 68 */     if (Asset.getAsset(str6) != null) {
/*  52: 69 */       return JSONResponses.ASSET_NAME_ALREADY_USED;
/*  53:    */     }
/*  54: 71 */     if ((str3 != null) && (str3.length() > 1000)) {
/*  55: 72 */       return JSONResponses.INCORRECT_ASSET_DESCRIPTION;
/*  56:    */     }
/*  57:    */     try
/*  58:    */     {
/*  59: 77 */       i = Integer.parseInt(str4);
/*  60: 78 */       if ((i <= 0) || (i > 1000000000L)) {
/*  61: 79 */         return JSONResponses.INCORRECT_ASSET_QUANTITY;
/*  62:    */       }
/*  63:    */     }
/*  64:    */     catch (NumberFormatException localNumberFormatException1)
/*  65:    */     {
/*  66: 82 */       return JSONResponses.INCORRECT_QUANTITY;
/*  67:    */     }
/*  68:    */     int j;
/*  69:    */     try
/*  70:    */     {
/*  71: 87 */       j = Integer.parseInt(str5);
/*  72: 88 */       if (j < 1000) {
/*  73: 89 */         return JSONResponses.INCORRECT_ASSET_ISSUANCE_FEE;
/*  74:    */       }
/*  75:    */     }
/*  76:    */     catch (NumberFormatException localNumberFormatException2)
/*  77:    */     {
/*  78: 92 */       return JSONResponses.INCORRECT_FEE;
/*  79:    */     }
/*  80: 95 */     byte[] arrayOfByte = Crypto.getPublicKey(str1);
/*  81: 96 */     Account localAccount = Account.getAccount(arrayOfByte);
/*  82: 97 */     if ((localAccount == null) || (j * 100L > localAccount.getUnconfirmedBalance())) {
/*  83: 98 */       return JSONResponses.NOT_ENOUGH_FUNDS;
/*  84:    */     }
/*  85:101 */     int k = Convert.getEpochTime();
/*  86:102 */     Attachment.ColoredCoinsAssetIssuance localColoredCoinsAssetIssuance = new Attachment.ColoredCoinsAssetIssuance(str2, str3, i);
/*  87:103 */     Transaction localTransaction = Transaction.newTransaction(k, (short)1440, arrayOfByte, Genesis.CREATOR_ID, 0, j, null, localColoredCoinsAssetIssuance);
/*  88:    */     
/*  89:105 */     localTransaction.sign(str1);
/*  90:    */     
/*  91:107 */     Blockchain.broadcast(localTransaction);
/*  92:    */     
/*  93:109 */     JSONObject localJSONObject = new JSONObject();
/*  94:110 */     localJSONObject.put("transaction", localTransaction.getStringId());
/*  95:111 */     return localJSONObject;
/*  96:    */   }
/*  97:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.IssueAsset
 * JD-Core Version:    0.7.0.1
 */