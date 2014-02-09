/*   1:    */ package nxt.user;
/*   2:    */ 
/*   3:    */ import java.io.IOException;
/*   4:    */ import java.math.BigInteger;
/*   5:    */ import java.util.Collection;
/*   6:    */ import java.util.Iterator;
/*   7:    */ import java.util.List;
/*   8:    */ import java.util.Set;
/*   9:    */ import javax.servlet.http.HttpServletRequest;
/*  10:    */ import nxt.Block;
/*  11:    */ import nxt.Blockchain;
/*  12:    */ import nxt.Nxt;
/*  13:    */ import nxt.Transaction;
/*  14:    */ import nxt.peer.Peer;
/*  15:    */ import nxt.peer.Peer.State;
/*  16:    */ import nxt.util.Convert;
/*  17:    */ import org.json.simple.JSONArray;
/*  18:    */ import org.json.simple.JSONObject;
/*  19:    */ import org.json.simple.JSONStreamAware;
/*  20:    */ 
/*  21:    */ final class GetInitialData
/*  22:    */   extends UserRequestHandler
/*  23:    */ {
/*  24: 20 */   static final GetInitialData instance = new GetInitialData();
/*  25:    */   
/*  26:    */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
/*  27:    */     throws IOException
/*  28:    */   {
/*  29: 27 */     JSONArray localJSONArray1 = new JSONArray();
/*  30: 28 */     JSONArray localJSONArray2 = new JSONArray();JSONArray localJSONArray3 = new JSONArray();JSONArray localJSONArray4 = new JSONArray();
/*  31: 29 */     JSONArray localJSONArray5 = new JSONArray();
/*  32: 31 */     for (Iterator localIterator = Blockchain.getAllUnconfirmedTransactions().iterator(); localIterator.hasNext();)
/*  33:    */     {
/*  34: 31 */       localObject1 = (Transaction)localIterator.next();
/*  35:    */       
/*  36: 33 */       localObject2 = new JSONObject();
/*  37: 34 */       ((JSONObject)localObject2).put("index", Integer.valueOf(((Transaction)localObject1).getIndex()));
/*  38: 35 */       ((JSONObject)localObject2).put("timestamp", Integer.valueOf(((Transaction)localObject1).getTimestamp()));
/*  39: 36 */       ((JSONObject)localObject2).put("deadline", Short.valueOf(((Transaction)localObject1).getDeadline()));
/*  40: 37 */       ((JSONObject)localObject2).put("recipient", Convert.convert(((Transaction)localObject1).getRecipientId()));
/*  41: 38 */       ((JSONObject)localObject2).put("amount", Integer.valueOf(((Transaction)localObject1).getAmount()));
/*  42: 39 */       ((JSONObject)localObject2).put("fee", Integer.valueOf(((Transaction)localObject1).getFee()));
/*  43: 40 */       ((JSONObject)localObject2).put("sender", Convert.convert(((Transaction)localObject1).getSenderAccountId()));
/*  44:    */       
/*  45: 42 */       localJSONArray1.add(localObject2);
/*  46:    */     }
/*  47:    */     Object localObject2;
/*  48: 46 */     for (localIterator = Peer.getAllPeers().iterator(); localIterator.hasNext();)
/*  49:    */     {
/*  50: 46 */       localObject1 = (Peer)localIterator.next();
/*  51:    */       
/*  52: 48 */       localObject2 = ((Peer)localObject1).getPeerAddress();
/*  53: 50 */       if (((Peer)localObject1).getBlacklistingTime() > 0L)
/*  54:    */       {
/*  55: 52 */         localObject3 = new JSONObject();
/*  56: 53 */         ((JSONObject)localObject3).put("index", Integer.valueOf(((Peer)localObject1).getIndex()));
/*  57: 54 */         ((JSONObject)localObject3).put("announcedAddress", ((Peer)localObject1).getAnnouncedAddress().length() > 0 ? ((Peer)localObject1).getAnnouncedAddress() : ((Peer)localObject1).getAnnouncedAddress().length() > 30 ? ((Peer)localObject1).getAnnouncedAddress().substring(0, 30) + "..." : localObject2);
/*  58: 59 */         if (Nxt.wellKnownPeers.contains(((Peer)localObject1).getAnnouncedAddress())) {
/*  59: 60 */           ((JSONObject)localObject3).put("wellKnown", Boolean.valueOf(true));
/*  60:    */         }
/*  61: 62 */         localJSONArray4.add(localObject3);
/*  62:    */       }
/*  63: 64 */       else if (((Peer)localObject1).getState() == Peer.State.NON_CONNECTED)
/*  64:    */       {
/*  65: 66 */         if (((Peer)localObject1).getAnnouncedAddress().length() > 0)
/*  66:    */         {
/*  67: 68 */           localObject3 = new JSONObject();
/*  68: 69 */           ((JSONObject)localObject3).put("index", Integer.valueOf(((Peer)localObject1).getIndex()));
/*  69: 70 */           ((JSONObject)localObject3).put("announcedAddress", ((Peer)localObject1).getAnnouncedAddress().length() > 30 ? ((Peer)localObject1).getAnnouncedAddress().substring(0, 30) + "..." : ((Peer)localObject1).getAnnouncedAddress());
/*  70: 71 */           if (Nxt.wellKnownPeers.contains(((Peer)localObject1).getAnnouncedAddress())) {
/*  71: 72 */             ((JSONObject)localObject3).put("wellKnown", Boolean.valueOf(true));
/*  72:    */           }
/*  73: 75 */           localJSONArray3.add(localObject3);
/*  74:    */         }
/*  75:    */       }
/*  76:    */       else
/*  77:    */       {
/*  78: 81 */         localObject3 = new JSONObject();
/*  79: 82 */         ((JSONObject)localObject3).put("index", Integer.valueOf(((Peer)localObject1).getIndex()));
/*  80: 83 */         if (((Peer)localObject1).getState() == Peer.State.DISCONNECTED) {
/*  81: 85 */           ((JSONObject)localObject3).put("disconnected", Boolean.valueOf(true));
/*  82:    */         }
/*  83: 88 */         ((JSONObject)localObject3).put("address", ((String)localObject2).length() > 30 ? ((String)localObject2).substring(0, 30) + "..." : localObject2);
/*  84: 89 */         ((JSONObject)localObject3).put("announcedAddress", ((Peer)localObject1).getAnnouncedAddress().length() > 30 ? ((Peer)localObject1).getAnnouncedAddress().substring(0, 30) + "..." : ((Peer)localObject1).getAnnouncedAddress());
/*  85: 90 */         ((JSONObject)localObject3).put("weight", Integer.valueOf(((Peer)localObject1).getWeight()));
/*  86: 91 */         ((JSONObject)localObject3).put("downloaded", Long.valueOf(((Peer)localObject1).getDownloadedVolume()));
/*  87: 92 */         ((JSONObject)localObject3).put("uploaded", Long.valueOf(((Peer)localObject1).getUploadedVolume()));
/*  88: 93 */         ((JSONObject)localObject3).put("software", ((Peer)localObject1).getSoftware());
/*  89: 94 */         if (Nxt.wellKnownPeers.contains(((Peer)localObject1).getAnnouncedAddress())) {
/*  90: 95 */           ((JSONObject)localObject3).put("wellKnown", Boolean.valueOf(true));
/*  91:    */         }
/*  92: 97 */         localJSONArray2.add(localObject3);
/*  93:    */       }
/*  94:    */     }
/*  95:    */     Object localObject3;
/*  96:101 */     int i = Blockchain.getLastBlock().getHeight();
/*  97:102 */     Object localObject1 = Blockchain.getBlocksFromHeight(Math.max(0, i - 59));
/*  98:104 */     for (int j = ((List)localObject1).size() - 1; j >= 0; j--)
/*  99:    */     {
/* 100:105 */       localObject3 = (Block)((List)localObject1).get(j);
/* 101:106 */       JSONObject localJSONObject2 = new JSONObject();
/* 102:107 */       localJSONObject2.put("index", Integer.valueOf(((Block)localObject3).getIndex()));
/* 103:108 */       localJSONObject2.put("timestamp", Integer.valueOf(((Block)localObject3).getTimestamp()));
/* 104:109 */       localJSONObject2.put("numberOfTransactions", Integer.valueOf(((Block)localObject3).getTransactionIds().length));
/* 105:110 */       localJSONObject2.put("totalAmount", Integer.valueOf(((Block)localObject3).getTotalAmount()));
/* 106:111 */       localJSONObject2.put("totalFee", Integer.valueOf(((Block)localObject3).getTotalFee()));
/* 107:112 */       localJSONObject2.put("payloadLength", Integer.valueOf(((Block)localObject3).getPayloadLength()));
/* 108:113 */       localJSONObject2.put("generator", Convert.convert(((Block)localObject3).getGeneratorAccountId()));
/* 109:114 */       localJSONObject2.put("height", Integer.valueOf(((Block)localObject3).getHeight()));
/* 110:115 */       localJSONObject2.put("version", Integer.valueOf(((Block)localObject3).getVersion()));
/* 111:116 */       localJSONObject2.put("block", ((Block)localObject3).getStringId());
/* 112:117 */       localJSONObject2.put("baseTarget", BigInteger.valueOf(((Block)localObject3).getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
/* 113:    */       
/* 114:    */ 
/* 115:120 */       localJSONArray5.add(localJSONObject2);
/* 116:    */     }
/* 117:123 */     JSONObject localJSONObject1 = new JSONObject();
/* 118:124 */     localJSONObject1.put("response", "processInitialData");
/* 119:125 */     localJSONObject1.put("version", "0.7.0e");
/* 120:126 */     if (localJSONArray1.size() > 0) {
/* 121:128 */       localJSONObject1.put("unconfirmedTransactions", localJSONArray1);
/* 122:    */     }
/* 123:131 */     if (localJSONArray2.size() > 0) {
/* 124:133 */       localJSONObject1.put("activePeers", localJSONArray2);
/* 125:    */     }
/* 126:136 */     if (localJSONArray3.size() > 0) {
/* 127:138 */       localJSONObject1.put("knownPeers", localJSONArray3);
/* 128:    */     }
/* 129:141 */     if (localJSONArray4.size() > 0) {
/* 130:143 */       localJSONObject1.put("blacklistedPeers", localJSONArray4);
/* 131:    */     }
/* 132:146 */     if (localJSONArray5.size() > 0) {
/* 133:148 */       localJSONObject1.put("recentBlocks", localJSONArray5);
/* 134:    */     }
/* 135:152 */     return localJSONObject1;
/* 136:    */   }
/* 137:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.GetInitialData
 * JD-Core Version:    0.7.0.1
 */