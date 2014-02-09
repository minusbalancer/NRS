/*   1:    */ package nxt.user;
/*   2:    */ 
/*   3:    */ import java.io.IOException;
/*   4:    */ import java.math.BigInteger;
/*   5:    */ import java.util.Collection;
/*   6:    */ import java.util.Iterator;
/*   7:    */ import java.util.List;
/*   8:    */ import javax.servlet.http.HttpServletRequest;
/*   9:    */ import nxt.Block;
/*  10:    */ import nxt.Blockchain;
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
/*  30: 31 */     for (Iterator localIterator = Blockchain.getAllUnconfirmedTransactions().iterator(); localIterator.hasNext();)
/*  31:    */     {
/*  32: 31 */       localObject1 = (Transaction)localIterator.next();
/*  33:    */       
/*  34: 33 */       localObject2 = new JSONObject();
/*  35: 34 */       ((JSONObject)localObject2).put("index", Integer.valueOf(((Transaction)localObject1).getIndex()));
/*  36: 35 */       ((JSONObject)localObject2).put("timestamp", Integer.valueOf(((Transaction)localObject1).getTimestamp()));
/*  37: 36 */       ((JSONObject)localObject2).put("deadline", Short.valueOf(((Transaction)localObject1).getDeadline()));
/*  38: 37 */       ((JSONObject)localObject2).put("recipient", Convert.convert(((Transaction)localObject1).getRecipientId()));
/*  39: 38 */       ((JSONObject)localObject2).put("amount", Integer.valueOf(((Transaction)localObject1).getAmount()));
/*  40: 39 */       ((JSONObject)localObject2).put("fee", Integer.valueOf(((Transaction)localObject1).getFee()));
/*  41: 40 */       ((JSONObject)localObject2).put("sender", Convert.convert(((Transaction)localObject1).getSenderAccountId()));
/*  42:    */       
/*  43: 42 */       localJSONArray1.add(localObject2);
/*  44:    */     }
/*  45:    */     Object localObject2;
/*  46: 46 */     for (localIterator = Peer.getAllPeers().iterator(); localIterator.hasNext();)
/*  47:    */     {
/*  48: 46 */       localObject1 = (Peer)localIterator.next();
/*  49:    */       
/*  50: 48 */       localObject2 = ((Peer)localObject1).getPeerAddress();
/*  51: 50 */       if (((Peer)localObject1).isBlacklisted())
/*  52:    */       {
/*  53: 52 */         localObject3 = new JSONObject();
/*  54: 53 */         ((JSONObject)localObject3).put("index", Integer.valueOf(((Peer)localObject1).getIndex()));
/*  55: 54 */         ((JSONObject)localObject3).put("announcedAddress", Convert.truncate(((Peer)localObject1).getAnnouncedAddress(), (String)localObject2, 25, true));
/*  56: 55 */         if (((Peer)localObject1).isWellKnown()) {
/*  57: 56 */           ((JSONObject)localObject3).put("wellKnown", Boolean.valueOf(true));
/*  58:    */         }
/*  59: 58 */         localJSONArray4.add(localObject3);
/*  60:    */       }
/*  61: 60 */       else if (((Peer)localObject1).getState() == Peer.State.NON_CONNECTED)
/*  62:    */       {
/*  63: 62 */         if (((Peer)localObject1).getAnnouncedAddress() != null)
/*  64:    */         {
/*  65: 64 */           localObject3 = new JSONObject();
/*  66: 65 */           ((JSONObject)localObject3).put("index", Integer.valueOf(((Peer)localObject1).getIndex()));
/*  67: 66 */           ((JSONObject)localObject3).put("announcedAddress", Convert.truncate(((Peer)localObject1).getAnnouncedAddress(), "", 25, true));
/*  68: 67 */           if (((Peer)localObject1).isWellKnown()) {
/*  69: 68 */             ((JSONObject)localObject3).put("wellKnown", Boolean.valueOf(true));
/*  70:    */           }
/*  71: 71 */           localJSONArray3.add(localObject3);
/*  72:    */         }
/*  73:    */       }
/*  74:    */       else
/*  75:    */       {
/*  76: 77 */         localObject3 = new JSONObject();
/*  77: 78 */         ((JSONObject)localObject3).put("index", Integer.valueOf(((Peer)localObject1).getIndex()));
/*  78: 79 */         if (((Peer)localObject1).getState() == Peer.State.DISCONNECTED) {
/*  79: 81 */           ((JSONObject)localObject3).put("disconnected", Boolean.valueOf(true));
/*  80:    */         }
/*  81: 84 */         ((JSONObject)localObject3).put("address", Convert.truncate((String)localObject2, "", 25, true));
/*  82: 85 */         ((JSONObject)localObject3).put("announcedAddress", Convert.truncate(((Peer)localObject1).getAnnouncedAddress(), "", 25, true));
/*  83: 86 */         ((JSONObject)localObject3).put("weight", Integer.valueOf(((Peer)localObject1).getWeight()));
/*  84: 87 */         ((JSONObject)localObject3).put("downloaded", Long.valueOf(((Peer)localObject1).getDownloadedVolume()));
/*  85: 88 */         ((JSONObject)localObject3).put("uploaded", Long.valueOf(((Peer)localObject1).getUploadedVolume()));
/*  86: 89 */         ((JSONObject)localObject3).put("software", ((Peer)localObject1).getSoftware());
/*  87: 90 */         if (((Peer)localObject1).isWellKnown()) {
/*  88: 91 */           ((JSONObject)localObject3).put("wellKnown", Boolean.valueOf(true));
/*  89:    */         }
/*  90: 93 */         localJSONArray2.add(localObject3);
/*  91:    */       }
/*  92:    */     }
/*  93:    */     Object localObject3;
/*  94: 97 */     int i = Blockchain.getLastBlock().getHeight();
/*  95: 98 */     Object localObject1 = Blockchain.getBlocksFromHeight(Math.max(0, i - 59));
/*  96:100 */     for (int j = ((List)localObject1).size() - 1; j >= 0; j--)
/*  97:    */     {
/*  98:101 */       localObject3 = (Block)((List)localObject1).get(j);
/*  99:102 */       JSONObject localJSONObject2 = new JSONObject();
/* 100:103 */       localJSONObject2.put("index", Integer.valueOf(((Block)localObject3).getIndex()));
/* 101:104 */       localJSONObject2.put("timestamp", Integer.valueOf(((Block)localObject3).getTimestamp()));
/* 102:105 */       localJSONObject2.put("numberOfTransactions", Integer.valueOf(((Block)localObject3).getTransactionIds().length));
/* 103:106 */       localJSONObject2.put("totalAmount", Integer.valueOf(((Block)localObject3).getTotalAmount()));
/* 104:107 */       localJSONObject2.put("totalFee", Integer.valueOf(((Block)localObject3).getTotalFee()));
/* 105:108 */       localJSONObject2.put("payloadLength", Integer.valueOf(((Block)localObject3).getPayloadLength()));
/* 106:109 */       localJSONObject2.put("generator", Convert.convert(((Block)localObject3).getGeneratorAccountId()));
/* 107:110 */       localJSONObject2.put("height", Integer.valueOf(((Block)localObject3).getHeight()));
/* 108:111 */       localJSONObject2.put("version", Integer.valueOf(((Block)localObject3).getVersion()));
/* 109:112 */       localJSONObject2.put("block", ((Block)localObject3).getStringId());
/* 110:113 */       localJSONObject2.put("baseTarget", BigInteger.valueOf(((Block)localObject3).getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
/* 111:    */       
/* 112:    */ 
/* 113:116 */       localJSONArray5.add(localJSONObject2);
/* 114:    */     }
/* 115:119 */     JSONObject localJSONObject1 = new JSONObject();
/* 116:120 */     localJSONObject1.put("response", "processInitialData");
/* 117:121 */     localJSONObject1.put("version", "0.7.1");
/* 118:122 */     if (localJSONArray1.size() > 0) {
/* 119:124 */       localJSONObject1.put("unconfirmedTransactions", localJSONArray1);
/* 120:    */     }
/* 121:127 */     if (localJSONArray2.size() > 0) {
/* 122:129 */       localJSONObject1.put("activePeers", localJSONArray2);
/* 123:    */     }
/* 124:132 */     if (localJSONArray3.size() > 0) {
/* 125:134 */       localJSONObject1.put("knownPeers", localJSONArray3);
/* 126:    */     }
/* 127:137 */     if (localJSONArray4.size() > 0) {
/* 128:139 */       localJSONObject1.put("blacklistedPeers", localJSONArray4);
/* 129:    */     }
/* 130:142 */     if (localJSONArray5.size() > 0) {
/* 131:144 */       localJSONObject1.put("recentBlocks", localJSONArray5);
/* 132:    */     }
/* 133:148 */     return localJSONObject1;
/* 134:    */   }
/* 135:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.GetInitialData
 * JD-Core Version:    0.7.0.1
 */