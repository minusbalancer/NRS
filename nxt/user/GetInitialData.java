/*   1:    */ package nxt.user;
/*   2:    */ 
/*   3:    */ import java.io.IOException;
/*   4:    */ import java.math.BigInteger;
/*   5:    */ import java.util.Collection;
/*   6:    */ import java.util.Iterator;
/*   7:    */ import java.util.Set;
/*   8:    */ import javax.servlet.http.HttpServletRequest;
/*   9:    */ import nxt.Block;
/*  10:    */ import nxt.Blockchain;
/*  11:    */ import nxt.Genesis;
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
/*  32: 31 */     for (Object localObject1 = Blockchain.getAllUnconfirmedTransactions().iterator(); ((Iterator)localObject1).hasNext();)
/*  33:    */     {
/*  34: 31 */       localObject2 = (Transaction)((Iterator)localObject1).next();
/*  35:    */       
/*  36: 33 */       localObject3 = new JSONObject();
/*  37: 34 */       ((JSONObject)localObject3).put("index", Integer.valueOf(((Transaction)localObject2).getIndex()));
/*  38: 35 */       ((JSONObject)localObject3).put("timestamp", Integer.valueOf(((Transaction)localObject2).getTimestamp()));
/*  39: 36 */       ((JSONObject)localObject3).put("deadline", Short.valueOf(((Transaction)localObject2).getDeadline()));
/*  40: 37 */       ((JSONObject)localObject3).put("recipient", Convert.convert(((Transaction)localObject2).getRecipientId()));
/*  41: 38 */       ((JSONObject)localObject3).put("amount", Integer.valueOf(((Transaction)localObject2).getAmount()));
/*  42: 39 */       ((JSONObject)localObject3).put("fee", Integer.valueOf(((Transaction)localObject2).getFee()));
/*  43: 40 */       ((JSONObject)localObject3).put("sender", Convert.convert(((Transaction)localObject2).getSenderAccountId()));
/*  44:    */       
/*  45: 42 */       localJSONArray1.add(localObject3);
/*  46:    */     }
/*  47:    */     Object localObject2;
/*  48: 46 */     for (localObject1 = Peer.getAllPeers().iterator(); ((Iterator)localObject1).hasNext();)
/*  49:    */     {
/*  50: 46 */       localObject2 = (Peer)((Iterator)localObject1).next();
/*  51:    */       
/*  52: 48 */       localObject3 = ((Peer)localObject2).getPeerAddress();
/*  53: 50 */       if (((Peer)localObject2).getBlacklistingTime() > 0L)
/*  54:    */       {
/*  55: 52 */         localJSONObject = new JSONObject();
/*  56: 53 */         localJSONObject.put("index", Integer.valueOf(((Peer)localObject2).getIndex()));
/*  57: 54 */         localJSONObject.put("announcedAddress", ((Peer)localObject2).getAnnouncedAddress().length() > 0 ? ((Peer)localObject2).getAnnouncedAddress() : ((Peer)localObject2).getAnnouncedAddress().length() > 30 ? ((Peer)localObject2).getAnnouncedAddress().substring(0, 30) + "..." : localObject3);
/*  58: 59 */         if (Nxt.wellKnownPeers.contains(((Peer)localObject2).getAnnouncedAddress())) {
/*  59: 60 */           localJSONObject.put("wellKnown", Boolean.valueOf(true));
/*  60:    */         }
/*  61: 62 */         localJSONArray4.add(localJSONObject);
/*  62:    */       }
/*  63: 64 */       else if (((Peer)localObject2).getState() == Peer.State.NON_CONNECTED)
/*  64:    */       {
/*  65: 66 */         if (((Peer)localObject2).getAnnouncedAddress().length() > 0)
/*  66:    */         {
/*  67: 68 */           localJSONObject = new JSONObject();
/*  68: 69 */           localJSONObject.put("index", Integer.valueOf(((Peer)localObject2).getIndex()));
/*  69: 70 */           localJSONObject.put("announcedAddress", ((Peer)localObject2).getAnnouncedAddress().length() > 30 ? ((Peer)localObject2).getAnnouncedAddress().substring(0, 30) + "..." : ((Peer)localObject2).getAnnouncedAddress());
/*  70: 71 */           if (Nxt.wellKnownPeers.contains(((Peer)localObject2).getAnnouncedAddress())) {
/*  71: 72 */             localJSONObject.put("wellKnown", Boolean.valueOf(true));
/*  72:    */           }
/*  73: 75 */           localJSONArray3.add(localJSONObject);
/*  74:    */         }
/*  75:    */       }
/*  76:    */       else
/*  77:    */       {
/*  78: 81 */         localJSONObject = new JSONObject();
/*  79: 82 */         localJSONObject.put("index", Integer.valueOf(((Peer)localObject2).getIndex()));
/*  80: 83 */         if (((Peer)localObject2).getState() == Peer.State.DISCONNECTED) {
/*  81: 85 */           localJSONObject.put("disconnected", Boolean.valueOf(true));
/*  82:    */         }
/*  83: 88 */         localJSONObject.put("address", ((String)localObject3).length() > 30 ? ((String)localObject3).substring(0, 30) + "..." : localObject3);
/*  84: 89 */         localJSONObject.put("announcedAddress", ((Peer)localObject2).getAnnouncedAddress().length() > 30 ? ((Peer)localObject2).getAnnouncedAddress().substring(0, 30) + "..." : ((Peer)localObject2).getAnnouncedAddress());
/*  85: 90 */         localJSONObject.put("weight", Integer.valueOf(((Peer)localObject2).getWeight()));
/*  86: 91 */         localJSONObject.put("downloaded", Long.valueOf(((Peer)localObject2).getDownloadedVolume()));
/*  87: 92 */         localJSONObject.put("uploaded", Long.valueOf(((Peer)localObject2).getUploadedVolume()));
/*  88: 93 */         localJSONObject.put("software", ((Peer)localObject2).getSoftware());
/*  89: 94 */         if (Nxt.wellKnownPeers.contains(((Peer)localObject2).getAnnouncedAddress())) {
/*  90: 95 */           localJSONObject.put("wellKnown", Boolean.valueOf(true));
/*  91:    */         }
/*  92: 97 */         localJSONArray2.add(localJSONObject);
/*  93:    */       }
/*  94:    */     }
/*  95:    */     JSONObject localJSONObject;
/*  96:101 */     localObject1 = Blockchain.getLastBlock().getId();
/*  97:102 */     int i = 0;
/*  98:103 */     while (i < 60)
/*  99:    */     {
/* 100:105 */       i++;
/* 101:    */       
/* 102:107 */       localObject3 = Blockchain.getBlock((Long)localObject1);
/* 103:108 */       localJSONObject = new JSONObject();
/* 104:109 */       localJSONObject.put("index", Integer.valueOf(((Block)localObject3).getIndex()));
/* 105:110 */       localJSONObject.put("timestamp", Integer.valueOf(((Block)localObject3).getTimestamp()));
/* 106:111 */       localJSONObject.put("numberOfTransactions", Integer.valueOf(((Block)localObject3).getTransactionIds().length));
/* 107:112 */       localJSONObject.put("totalAmount", Integer.valueOf(((Block)localObject3).getTotalAmount()));
/* 108:113 */       localJSONObject.put("totalFee", Integer.valueOf(((Block)localObject3).getTotalFee()));
/* 109:114 */       localJSONObject.put("payloadLength", Integer.valueOf(((Block)localObject3).getPayloadLength()));
/* 110:115 */       localJSONObject.put("generator", Convert.convert(((Block)localObject3).getGeneratorAccountId()));
/* 111:116 */       localJSONObject.put("height", Integer.valueOf(((Block)localObject3).getHeight()));
/* 112:117 */       localJSONObject.put("version", Integer.valueOf(((Block)localObject3).getVersion()));
/* 113:118 */       localJSONObject.put("block", ((Block)localObject3).getStringId());
/* 114:119 */       localJSONObject.put("baseTarget", BigInteger.valueOf(((Block)localObject3).getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
/* 115:    */       
/* 116:    */ 
/* 117:122 */       localJSONArray5.add(localJSONObject);
/* 118:124 */       if (((Long)localObject1).equals(Genesis.GENESIS_BLOCK_ID)) {
/* 119:    */         break;
/* 120:    */       }
/* 121:128 */       localObject1 = ((Block)localObject3).getPreviousBlockId();
/* 122:    */     }
/* 123:132 */     Object localObject3 = new JSONObject();
/* 124:133 */     ((JSONObject)localObject3).put("response", "processInitialData");
/* 125:134 */     ((JSONObject)localObject3).put("version", "0.6.1");
/* 126:135 */     if (localJSONArray1.size() > 0) {
/* 127:137 */       ((JSONObject)localObject3).put("unconfirmedTransactions", localJSONArray1);
/* 128:    */     }
/* 129:140 */     if (localJSONArray2.size() > 0) {
/* 130:142 */       ((JSONObject)localObject3).put("activePeers", localJSONArray2);
/* 131:    */     }
/* 132:145 */     if (localJSONArray3.size() > 0) {
/* 133:147 */       ((JSONObject)localObject3).put("knownPeers", localJSONArray3);
/* 134:    */     }
/* 135:150 */     if (localJSONArray4.size() > 0) {
/* 136:152 */       ((JSONObject)localObject3).put("blacklistedPeers", localJSONArray4);
/* 137:    */     }
/* 138:155 */     if (localJSONArray5.size() > 0) {
/* 139:157 */       ((JSONObject)localObject3).put("recentBlocks", localJSONArray5);
/* 140:    */     }
/* 141:161 */     return localObject3;
/* 142:    */   }
/* 143:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.GetInitialData
 * JD-Core Version:    0.7.0.1
 */