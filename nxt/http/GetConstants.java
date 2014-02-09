/*   1:    */ package nxt.http;
/*   2:    */ 
/*   3:    */ import javax.servlet.http.HttpServletRequest;
/*   4:    */ import nxt.Genesis;
/*   5:    */ import nxt.Transaction.Type;
/*   6:    */ import nxt.Transaction.Type.ColoredCoins;
/*   7:    */ import nxt.Transaction.Type.Messaging;
/*   8:    */ import nxt.Transaction.Type.Payment;
/*   9:    */ import nxt.util.Convert;
/*  10:    */ import nxt.util.JSON;
/*  11:    */ import org.json.simple.JSONArray;
/*  12:    */ import org.json.simple.JSONObject;
/*  13:    */ import org.json.simple.JSONStreamAware;
/*  14:    */ 
/*  15:    */ final class GetConstants
/*  16:    */   extends HttpRequestHandler
/*  17:    */ {
/*  18: 16 */   static final GetConstants instance = new GetConstants();
/*  19:    */   private static final JSONStreamAware CONSTANTS;
/*  20:    */   
/*  21:    */   static
/*  22:    */   {
/*  23: 22 */     JSONObject localJSONObject1 = new JSONObject();
/*  24: 23 */     localJSONObject1.put("genesisBlockId", Convert.convert(Genesis.GENESIS_BLOCK_ID));
/*  25: 24 */     localJSONObject1.put("genesisAccountId", Convert.convert(Genesis.CREATOR_ID));
/*  26: 25 */     localJSONObject1.put("maxBlockPayloadLength", Integer.valueOf(32640));
/*  27: 26 */     localJSONObject1.put("maxArbitraryMessageLength", Integer.valueOf(1000));
/*  28:    */     
/*  29: 28 */     JSONArray localJSONArray1 = new JSONArray();
/*  30: 29 */     JSONObject localJSONObject2 = new JSONObject();
/*  31: 30 */     localJSONObject2.put("value", Byte.valueOf(Transaction.Type.Payment.ORDINARY.getType()));
/*  32: 31 */     localJSONObject2.put("description", "Payment");
/*  33: 32 */     JSONArray localJSONArray2 = new JSONArray();
/*  34: 33 */     JSONObject localJSONObject3 = new JSONObject();
/*  35: 34 */     localJSONObject3.put("value", Byte.valueOf(Transaction.Type.Payment.ORDINARY.getSubtype()));
/*  36: 35 */     localJSONObject3.put("description", "Ordinary payment");
/*  37: 36 */     localJSONArray2.add(localJSONObject3);
/*  38: 37 */     localJSONObject2.put("subtypes", localJSONArray2);
/*  39: 38 */     localJSONArray1.add(localJSONObject2);
/*  40: 39 */     localJSONObject2 = new JSONObject();
/*  41: 40 */     localJSONObject2.put("value", Byte.valueOf(Transaction.Type.Messaging.ARBITRARY_MESSAGE.getType()));
/*  42: 41 */     localJSONObject2.put("description", "Messaging");
/*  43: 42 */     localJSONArray2 = new JSONArray();
/*  44: 43 */     localJSONObject3 = new JSONObject();
/*  45: 44 */     localJSONObject3.put("value", Byte.valueOf(Transaction.Type.Messaging.ARBITRARY_MESSAGE.getSubtype()));
/*  46: 45 */     localJSONObject3.put("description", "Arbitrary message");
/*  47: 46 */     localJSONArray2.add(localJSONObject3);
/*  48: 47 */     localJSONObject3 = new JSONObject();
/*  49: 48 */     localJSONObject3.put("value", Byte.valueOf(Transaction.Type.Messaging.ALIAS_ASSIGNMENT.getSubtype()));
/*  50: 49 */     localJSONObject3.put("description", "Alias assignment");
/*  51: 50 */     localJSONArray2.add(localJSONObject3);
/*  52: 51 */     localJSONObject2.put("subtypes", localJSONArray2);
/*  53: 52 */     localJSONArray1.add(localJSONObject2);
/*  54: 53 */     localJSONObject2 = new JSONObject();
/*  55: 54 */     localJSONObject2.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.ASSET_ISSUANCE.getType()));
/*  56: 55 */     localJSONObject2.put("description", "Colored coins");
/*  57: 56 */     localJSONArray2 = new JSONArray();
/*  58: 57 */     localJSONObject3 = new JSONObject();
/*  59: 58 */     localJSONObject3.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.ASSET_ISSUANCE.getSubtype()));
/*  60: 59 */     localJSONObject3.put("description", "Asset issuance");
/*  61: 60 */     localJSONArray2.add(localJSONObject3);
/*  62: 61 */     localJSONObject3 = new JSONObject();
/*  63: 62 */     localJSONObject3.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.ASSET_TRANSFER.getSubtype()));
/*  64: 63 */     localJSONObject3.put("description", "Asset transfer");
/*  65: 64 */     localJSONArray2.add(localJSONObject3);
/*  66: 65 */     localJSONObject3 = new JSONObject();
/*  67: 66 */     localJSONObject3.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.ASK_ORDER_PLACEMENT.getSubtype()));
/*  68: 67 */     localJSONObject3.put("description", "Ask order placement");
/*  69: 68 */     localJSONArray2.add(localJSONObject3);
/*  70: 69 */     localJSONObject3 = new JSONObject();
/*  71: 70 */     localJSONObject3.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.BID_ORDER_PLACEMENT.getSubtype()));
/*  72: 71 */     localJSONObject3.put("description", "Bid order placement");
/*  73: 72 */     localJSONArray2.add(localJSONObject3);
/*  74: 73 */     localJSONObject3 = new JSONObject();
/*  75: 74 */     localJSONObject3.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.ASK_ORDER_CANCELLATION.getSubtype()));
/*  76: 75 */     localJSONObject3.put("description", "Ask order cancellation");
/*  77: 76 */     localJSONArray2.add(localJSONObject3);
/*  78: 77 */     localJSONObject3 = new JSONObject();
/*  79: 78 */     localJSONObject3.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.BID_ORDER_CANCELLATION.getSubtype()));
/*  80: 79 */     localJSONObject3.put("description", "Bid order cancellation");
/*  81: 80 */     localJSONArray2.add(localJSONObject3);
/*  82: 81 */     localJSONObject2.put("subtypes", localJSONArray2);
/*  83: 82 */     localJSONArray1.add(localJSONObject2);
/*  84: 83 */     localJSONObject1.put("transactionTypes", localJSONArray1);
/*  85:    */     
/*  86: 85 */     JSONArray localJSONArray3 = new JSONArray();
/*  87: 86 */     JSONObject localJSONObject4 = new JSONObject();
/*  88: 87 */     localJSONObject4.put("value", Integer.valueOf(0));
/*  89: 88 */     localJSONObject4.put("description", "Non-connected");
/*  90: 89 */     localJSONArray3.add(localJSONObject4);
/*  91: 90 */     localJSONObject4 = new JSONObject();
/*  92: 91 */     localJSONObject4.put("value", Integer.valueOf(1));
/*  93: 92 */     localJSONObject4.put("description", "Connected");
/*  94: 93 */     localJSONArray3.add(localJSONObject4);
/*  95: 94 */     localJSONObject4 = new JSONObject();
/*  96: 95 */     localJSONObject4.put("value", Integer.valueOf(2));
/*  97: 96 */     localJSONObject4.put("description", "Disconnected");
/*  98: 97 */     localJSONArray3.add(localJSONObject4);
/*  99: 98 */     localJSONObject1.put("peerStates", localJSONArray3);
/* 100:    */     
/* 101:100 */     CONSTANTS = JSON.prepare(localJSONObject1);
/* 102:    */   }
/* 103:    */   
/* 104:    */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 105:    */   {
/* 106:108 */     return CONSTANTS;
/* 107:    */   }
/* 108:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetConstants
 * JD-Core Version:    0.7.0.1
 */