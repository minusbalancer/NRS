/*   1:    */ package nxt.user;
/*   2:    */ 
/*   3:    */ import java.io.IOException;
/*   4:    */ import java.math.BigInteger;
/*   5:    */ import java.util.Collection;
/*   6:    */ import java.util.Iterator;
/*   7:    */ import javax.servlet.http.HttpServletRequest;
/*   8:    */ import nxt.Block;
/*   9:    */ import nxt.Blockchain;
/*  10:    */ import nxt.Genesis;
/*  11:    */ import nxt.Transaction;
/*  12:    */ import nxt.peer.Peer;
/*  13:    */ import nxt.peer.Peer.State;
/*  14:    */ import nxt.util.Convert;
/*  15:    */ import org.json.simple.JSONArray;
/*  16:    */ import org.json.simple.JSONObject;
/*  17:    */ import org.json.simple.JSONStreamAware;
/*  18:    */ 
/*  19:    */ final class GetInitialData
/*  20:    */   extends UserRequestHandler
/*  21:    */ {
/*  22: 20 */   static final GetInitialData instance = new GetInitialData();
/*  23:    */   
/*  24:    */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
/*  25:    */     throws IOException
/*  26:    */   {
/*  27: 27 */     JSONArray localJSONArray1 = new JSONArray();
/*  28: 28 */     JSONArray localJSONArray2 = new JSONArray();JSONArray localJSONArray3 = new JSONArray();JSONArray localJSONArray4 = new JSONArray();
/*  29: 29 */     JSONArray localJSONArray5 = new JSONArray();
/*  30: 31 */     for (Object localObject1 = Blockchain.getAllUnconfirmedTransactions().iterator(); ((Iterator)localObject1).hasNext();)
/*  31:    */     {
/*  32: 31 */       localObject2 = (Transaction)((Iterator)localObject1).next();
/*  33:    */       
/*  34: 33 */       localObject3 = new JSONObject();
/*  35: 34 */       ((JSONObject)localObject3).put("index", Integer.valueOf(((Transaction)localObject2).getIndex()));
/*  36: 35 */       ((JSONObject)localObject3).put("timestamp", Integer.valueOf(((Transaction)localObject2).getTimestamp()));
/*  37: 36 */       ((JSONObject)localObject3).put("deadline", Short.valueOf(((Transaction)localObject2).getDeadline()));
/*  38: 37 */       ((JSONObject)localObject3).put("recipient", Convert.convert(((Transaction)localObject2).getRecipientId()));
/*  39: 38 */       ((JSONObject)localObject3).put("amount", Integer.valueOf(((Transaction)localObject2).getAmount()));
/*  40: 39 */       ((JSONObject)localObject3).put("fee", Integer.valueOf(((Transaction)localObject2).getFee()));
/*  41: 40 */       ((JSONObject)localObject3).put("sender", Convert.convert(((Transaction)localObject2).getSenderAccountId()));
/*  42:    */       
/*  43: 42 */       localJSONArray1.add(localObject3);
/*  44:    */     }
/*  45:    */     Object localObject2;
/*  46: 46 */     for (localObject1 = Peer.getAllPeers().iterator(); ((Iterator)localObject1).hasNext();)
/*  47:    */     {
/*  48: 46 */       localObject2 = (Peer)((Iterator)localObject1).next();
/*  49:    */       
/*  50: 48 */       localObject3 = ((Peer)localObject2).getPeerAddress();
/*  51: 50 */       if (((Peer)localObject2).isBlacklisted())
/*  52:    */       {
/*  53: 52 */         localJSONObject = new JSONObject();
/*  54: 53 */         localJSONObject.put("index", Integer.valueOf(((Peer)localObject2).getIndex()));
/*  55: 54 */         localJSONObject.put("announcedAddress", Convert.truncate(((Peer)localObject2).getAnnouncedAddress(), (String)localObject3, 25, true));
/*  56: 55 */         if (((Peer)localObject2).isWellKnown()) {
/*  57: 56 */           localJSONObject.put("wellKnown", Boolean.valueOf(true));
/*  58:    */         }
/*  59: 58 */         localJSONArray4.add(localJSONObject);
/*  60:    */       }
/*  61: 60 */       else if (((Peer)localObject2).getState() == Peer.State.NON_CONNECTED)
/*  62:    */       {
/*  63: 62 */         if (((Peer)localObject2).getAnnouncedAddress() != null)
/*  64:    */         {
/*  65: 64 */           localJSONObject = new JSONObject();
/*  66: 65 */           localJSONObject.put("index", Integer.valueOf(((Peer)localObject2).getIndex()));
/*  67: 66 */           localJSONObject.put("announcedAddress", Convert.truncate(((Peer)localObject2).getAnnouncedAddress(), "", 25, true));
/*  68: 67 */           if (((Peer)localObject2).isWellKnown()) {
/*  69: 68 */             localJSONObject.put("wellKnown", Boolean.valueOf(true));
/*  70:    */           }
/*  71: 71 */           localJSONArray3.add(localJSONObject);
/*  72:    */         }
/*  73:    */       }
/*  74:    */       else
/*  75:    */       {
/*  76: 77 */         localJSONObject = new JSONObject();
/*  77: 78 */         localJSONObject.put("index", Integer.valueOf(((Peer)localObject2).getIndex()));
/*  78: 79 */         if (((Peer)localObject2).getState() == Peer.State.DISCONNECTED) {
/*  79: 81 */           localJSONObject.put("disconnected", Boolean.valueOf(true));
/*  80:    */         }
/*  81: 84 */         localJSONObject.put("address", Convert.truncate((String)localObject3, "", 25, true));
/*  82: 85 */         localJSONObject.put("announcedAddress", Convert.truncate(((Peer)localObject2).getAnnouncedAddress(), "", 25, true));
/*  83: 86 */         localJSONObject.put("weight", Integer.valueOf(((Peer)localObject2).getWeight()));
/*  84: 87 */         localJSONObject.put("downloaded", Long.valueOf(((Peer)localObject2).getDownloadedVolume()));
/*  85: 88 */         localJSONObject.put("uploaded", Long.valueOf(((Peer)localObject2).getUploadedVolume()));
/*  86: 89 */         localJSONObject.put("software", ((Peer)localObject2).getSoftware());
/*  87: 90 */         if (((Peer)localObject2).isWellKnown()) {
/*  88: 91 */           localJSONObject.put("wellKnown", Boolean.valueOf(true));
/*  89:    */         }
/*  90: 93 */         localJSONArray2.add(localJSONObject);
/*  91:    */       }
/*  92:    */     }
/*  93:    */     JSONObject localJSONObject;
/*  94: 97 */     localObject1 = Blockchain.getLastBlock().getId();
/*  95: 98 */     int i = 0;
/*  96: 99 */     while (i < 60)
/*  97:    */     {
/*  98:101 */       i++;
/*  99:    */       
/* 100:103 */       localObject3 = Blockchain.getBlock((Long)localObject1);
/* 101:104 */       localJSONObject = new JSONObject();
/* 102:105 */       localJSONObject.put("index", Integer.valueOf(((Block)localObject3).getIndex()));
/* 103:106 */       localJSONObject.put("timestamp", Integer.valueOf(((Block)localObject3).getTimestamp()));
/* 104:107 */       localJSONObject.put("numberOfTransactions", Integer.valueOf(((Block)localObject3).getTransactionIds().length));
/* 105:108 */       localJSONObject.put("totalAmount", Integer.valueOf(((Block)localObject3).getTotalAmount()));
/* 106:109 */       localJSONObject.put("totalFee", Integer.valueOf(((Block)localObject3).getTotalFee()));
/* 107:110 */       localJSONObject.put("payloadLength", Integer.valueOf(((Block)localObject3).getPayloadLength()));
/* 108:111 */       localJSONObject.put("generator", Convert.convert(((Block)localObject3).getGeneratorAccountId()));
/* 109:112 */       localJSONObject.put("height", Integer.valueOf(((Block)localObject3).getHeight()));
/* 110:113 */       localJSONObject.put("version", Integer.valueOf(((Block)localObject3).getVersion()));
/* 111:114 */       localJSONObject.put("block", ((Block)localObject3).getStringId());
/* 112:115 */       localJSONObject.put("baseTarget", BigInteger.valueOf(((Block)localObject3).getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
/* 113:    */       
/* 114:    */ 
/* 115:118 */       localJSONArray5.add(localJSONObject);
/* 116:120 */       if (((Long)localObject1).equals(Genesis.GENESIS_BLOCK_ID)) {
/* 117:    */         break;
/* 118:    */       }
/* 119:124 */       localObject1 = ((Block)localObject3).getPreviousBlockId();
/* 120:    */     }
/* 121:128 */     Object localObject3 = new JSONObject();
/* 122:129 */     ((JSONObject)localObject3).put("response", "processInitialData");
/* 123:130 */     ((JSONObject)localObject3).put("version", "0.6.2");
/* 124:131 */     if (localJSONArray1.size() > 0) {
/* 125:133 */       ((JSONObject)localObject3).put("unconfirmedTransactions", localJSONArray1);
/* 126:    */     }
/* 127:136 */     if (localJSONArray2.size() > 0) {
/* 128:138 */       ((JSONObject)localObject3).put("activePeers", localJSONArray2);
/* 129:    */     }
/* 130:141 */     if (localJSONArray3.size() > 0) {
/* 131:143 */       ((JSONObject)localObject3).put("knownPeers", localJSONArray3);
/* 132:    */     }
/* 133:146 */     if (localJSONArray4.size() > 0) {
/* 134:148 */       ((JSONObject)localObject3).put("blacklistedPeers", localJSONArray4);
/* 135:    */     }
/* 136:151 */     if (localJSONArray5.size() > 0) {
/* 137:153 */       ((JSONObject)localObject3).put("recentBlocks", localJSONArray5);
/* 138:    */     }
/* 139:157 */     return localObject3;
/* 140:    */   }
/* 141:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.GetInitialData
 * JD-Core Version:    0.7.0.1
 */