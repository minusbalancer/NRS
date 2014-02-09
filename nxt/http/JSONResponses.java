/*   1:    */ package nxt.http;
/*   2:    */ 
/*   3:    */ import nxt.util.JSON;
/*   4:    */ import org.json.simple.JSONObject;
/*   5:    */ import org.json.simple.JSONStreamAware;
/*   6:    */ 
/*   7:    */ final class JSONResponses
/*   8:    */ {
/*   9: 10 */   static final JSONStreamAware INCORRECT_ALIAS_LENGTH = incorrect("alias", "(length must be in [1..100] range)");
/*  10: 11 */   static final JSONStreamAware INCORRECT_ALIAS = incorrect("alias", "(must contain only digits and latin letters)");
/*  11: 12 */   static final JSONStreamAware INCORRECT_URI_LENGTH = incorrect("uri", "(length must be not longer than 1000 characters)");
/*  12: 13 */   static final JSONStreamAware MISSING_SECRET_PHRASE = missing("secretPhrase");
/*  13: 14 */   static final JSONStreamAware MISSING_ALIAS = missing("alias");
/*  14: 15 */   static final JSONStreamAware MISSING_URI = missing("uri");
/*  15: 16 */   static final JSONStreamAware MISSING_FEE = missing("fee");
/*  16: 17 */   static final JSONStreamAware MISSING_DEADLINE = missing("deadline");
/*  17: 18 */   static final JSONStreamAware INCORRECT_DEADLINE = incorrect("deadline");
/*  18: 19 */   static final JSONStreamAware INCORRECT_FEE = incorrect("fee");
/*  19: 20 */   static final JSONStreamAware MISSING_TRANSACTION_BYTES = missing("transactionBytes");
/*  20: 21 */   static final JSONStreamAware INCORRECT_TRANSACTION_BYTES = incorrect("transactionBytes");
/*  21: 22 */   static final JSONStreamAware MISSING_ORDER = missing("order");
/*  22: 23 */   static final JSONStreamAware INCORRECT_ORDER = incorrect("order");
/*  23: 24 */   static final JSONStreamAware UNKNOWN_ORDER = unknown("order");
/*  24: 25 */   static final JSONStreamAware MISSING_HALLMARK = missing("hallmark");
/*  25: 26 */   static final JSONStreamAware INCORRECT_HALLMARK = incorrect("hallmark");
/*  26: 27 */   static final JSONStreamAware MISSING_WEBSITE = missing("website");
/*  27: 28 */   static final JSONStreamAware INCORRECT_WEBSITE = incorrect("website");
/*  28: 29 */   static final JSONStreamAware MISSING_TOKEN = missing("token");
/*  29: 30 */   static final JSONStreamAware INCORRECT_TOKEN = incorrect("token");
/*  30: 31 */   static final JSONStreamAware MISSING_ACCOUNT = missing("account");
/*  31: 32 */   static final JSONStreamAware INCORRECT_ACCOUNT = incorrect("account");
/*  32: 33 */   static final JSONStreamAware MISSING_TIMESTAMP = missing("timestamp");
/*  33: 34 */   static final JSONStreamAware INCORRECT_TIMESTAMP = incorrect("timestamp");
/*  34: 35 */   static final JSONStreamAware UNKNOWN_ACCOUNT = unknown("account");
/*  35: 36 */   static final JSONStreamAware UNKNOWN_ALIAS = unknown("alias");
/*  36: 37 */   static final JSONStreamAware MISSING_ASSET = missing("asset");
/*  37: 38 */   static final JSONStreamAware UNKNOWN_ASSET = unknown("asset");
/*  38: 39 */   static final JSONStreamAware INCORRECT_ASSET = incorrect("asset");
/*  39: 40 */   static final JSONStreamAware MISSING_BLOCK = missing("block");
/*  40: 41 */   static final JSONStreamAware UNKNOWN_BLOCK = unknown("block");
/*  41: 42 */   static final JSONStreamAware INCORRECT_BLOCK = incorrect("block");
/*  42: 43 */   static final JSONStreamAware MISSING_NUMBER_OF_CONFIRMATIONS = missing("numberOfConfirmations");
/*  43: 44 */   static final JSONStreamAware INCORRECT_NUMBER_OF_CONFIRMATIONS = incorrect("numberOfConfirmations");
/*  44: 45 */   static final JSONStreamAware MISSING_PEER = missing("peer");
/*  45: 46 */   static final JSONStreamAware UNKNOWN_PEER = unknown("peer");
/*  46: 47 */   static final JSONStreamAware MISSING_TRANSACTION = missing("transaction");
/*  47: 48 */   static final JSONStreamAware UNKNOWN_TRANSACTION = unknown("transaction");
/*  48: 49 */   static final JSONStreamAware INCORRECT_TRANSACTION = incorrect("transaction");
/*  49: 50 */   static final JSONStreamAware INCORRECT_ASSET_ISSUANCE_FEE = incorrect("fee", "(must be not less than 1'000)");
/*  50: 51 */   static final JSONStreamAware INCORRECT_ASSET_DESCRIPTION = incorrect("description", "(length must be not longer than 1000 characters)");
/*  51: 52 */   static final JSONStreamAware INCORRECT_ASSET_NAME = incorrect("name", "(must contain only digits and latin letters)");
/*  52: 53 */   static final JSONStreamAware INCORRECT_ASSET_NAME_LENGTH = incorrect("name", "(length must be in [3..10] range)");
/*  53: 54 */   static final JSONStreamAware MISSING_NAME = missing("name");
/*  54: 55 */   static final JSONStreamAware MISSING_QUANTITY = missing("quantity");
/*  55: 56 */   static final JSONStreamAware INCORRECT_QUANTITY = incorrect("quantity");
/*  56: 57 */   static final JSONStreamAware INCORRECT_ASSET_QUANTITY = incorrect("quantity", "(must be in [1..1'000'000'000] range)");
/*  57: 58 */   static final JSONStreamAware MISSING_HOST = missing("host");
/*  58: 59 */   static final JSONStreamAware MISSING_DATE = missing("date");
/*  59: 60 */   static final JSONStreamAware MISSING_WEIGHT = missing("weight");
/*  60: 61 */   static final JSONStreamAware INCORRECT_HOST = incorrect("host", "(the length exceeds 100 chars limit)");
/*  61: 62 */   static final JSONStreamAware INCORRECT_WEIGHT = incorrect("weight");
/*  62: 63 */   static final JSONStreamAware INCORRECT_DATE = incorrect("date");
/*  63: 64 */   static final JSONStreamAware MISSING_PRICE = missing("price");
/*  64: 65 */   static final JSONStreamAware INCORRECT_PRICE = incorrect("price");
/*  65: 66 */   static final JSONStreamAware INCORRECT_REFERENCED_TRANSACTION = incorrect("referencedTransaction");
/*  66: 67 */   static final JSONStreamAware MISSING_MESSAGE = missing("message");
/*  67: 68 */   static final JSONStreamAware MISSING_RECIPIENT = missing("recipient");
/*  68: 69 */   static final JSONStreamAware INCORRECT_RECIPIENT = incorrect("recipient");
/*  69: 70 */   static final JSONStreamAware INCORRECT_ARBITRARY_MESSAGE = incorrect("message", "(length must be not longer than \"1000\" bytes)");
/*  70: 71 */   static final JSONStreamAware MISSING_AMOUNT = missing("amount");
/*  71: 72 */   static final JSONStreamAware INCORRECT_AMOUNT = incorrect("amount");
/*  72:    */   static final JSONStreamAware NOT_ENOUGH_FUNDS;
/*  73:    */   static final JSONStreamAware ASSET_NAME_ALREADY_USED;
/*  74:    */   static final JSONStreamAware ERROR_NOT_ALLOWED;
/*  75:    */   static final JSONStreamAware ERROR_INCORRECT_REQUEST;
/*  76:    */   
/*  77:    */   static
/*  78:    */   {
/*  79: 76 */     JSONObject localJSONObject = new JSONObject();
/*  80: 77 */     localJSONObject.put("errorCode", Integer.valueOf(6));
/*  81: 78 */     localJSONObject.put("errorDescription", "Not enough funds");
/*  82: 79 */     NOT_ENOUGH_FUNDS = JSON.prepare(localJSONObject);
/*  83:    */     
/*  84:    */ 
/*  85:    */ 
/*  86:    */ 
/*  87: 84 */     localJSONObject = new JSONObject();
/*  88: 85 */     localJSONObject.put("errorCode", Integer.valueOf(8));
/*  89: 86 */     localJSONObject.put("errorDescription", "Asset name is already used");
/*  90: 87 */     ASSET_NAME_ALREADY_USED = JSON.prepare(localJSONObject);
/*  91:    */     
/*  92:    */ 
/*  93:    */ 
/*  94:    */ 
/*  95: 92 */     localJSONObject = new JSONObject();
/*  96: 93 */     localJSONObject.put("errorCode", Integer.valueOf(7));
/*  97: 94 */     localJSONObject.put("errorDescription", "Not allowed");
/*  98: 95 */     ERROR_NOT_ALLOWED = JSON.prepare(localJSONObject);
/*  99:    */     
/* 100:    */ 
/* 101:    */ 
/* 102:    */ 
/* 103:100 */     localJSONObject = new JSONObject();
/* 104:101 */     localJSONObject.put("errorCode", Integer.valueOf(1));
/* 105:102 */     localJSONObject.put("errorDescription", "Incorrect request");
/* 106:103 */     ERROR_INCORRECT_REQUEST = JSON.prepare(localJSONObject);
/* 107:    */   }
/* 108:    */   
/* 109:    */   private static JSONStreamAware missing(String paramString)
/* 110:    */   {
/* 111:107 */     JSONObject localJSONObject = new JSONObject();
/* 112:108 */     localJSONObject.put("errorCode", Integer.valueOf(3));
/* 113:109 */     localJSONObject.put("errorDescription", "\"" + paramString + "\"" + " not specified");
/* 114:110 */     return JSON.prepare(localJSONObject);
/* 115:    */   }
/* 116:    */   
/* 117:    */   private static JSONStreamAware incorrect(String paramString)
/* 118:    */   {
/* 119:114 */     return incorrect(paramString, "");
/* 120:    */   }
/* 121:    */   
/* 122:    */   private static JSONStreamAware incorrect(String paramString1, String paramString2)
/* 123:    */   {
/* 124:118 */     JSONObject localJSONObject = new JSONObject();
/* 125:119 */     localJSONObject.put("errorCode", Integer.valueOf(4));
/* 126:120 */     localJSONObject.put("errorDescription", "Incorrect \"" + paramString1 + "\"" + paramString2);
/* 127:121 */     return JSON.prepare(localJSONObject);
/* 128:    */   }
/* 129:    */   
/* 130:    */   private static JSONStreamAware unknown(String paramString)
/* 131:    */   {
/* 132:125 */     JSONObject localJSONObject = new JSONObject();
/* 133:126 */     localJSONObject.put("errorCode", Integer.valueOf(5));
/* 134:127 */     localJSONObject.put("errorDescription", "Unknown " + paramString);
/* 135:128 */     return JSON.prepare(localJSONObject);
/* 136:    */   }
/* 137:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.JSONResponses
 * JD-Core Version:    0.7.0.1
 */