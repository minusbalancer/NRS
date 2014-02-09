/*   1:    */ package nxt.user;
/*   2:    */ 
/*   3:    */ import java.io.IOException;
/*   4:    */ import java.math.BigInteger;
/*   5:    */ import java.security.MessageDigest;
/*   6:    */ import java.util.Arrays;
/*   7:    */ import java.util.Collection;
/*   8:    */ import java.util.Iterator;
/*   9:    */ import javax.servlet.http.HttpServletRequest;
/*  10:    */ import nxt.Account;
/*  11:    */ import nxt.Block;
/*  12:    */ import nxt.Blockchain;
/*  13:    */ import nxt.Genesis;
/*  14:    */ import nxt.Transaction;
/*  15:    */ import nxt.crypto.Crypto;
/*  16:    */ import nxt.util.Convert;
/*  17:    */ import org.json.simple.JSONArray;
/*  18:    */ import org.json.simple.JSONObject;
/*  19:    */ import org.json.simple.JSONStreamAware;
/*  20:    */ 
/*  21:    */ final class UnlockAccount
/*  22:    */   extends UserRequestHandler
/*  23:    */ {
/*  24: 25 */   static final UnlockAccount instance = new UnlockAccount();
/*  25:    */   
/*  26:    */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
/*  27:    */     throws IOException
/*  28:    */   {
/*  29: 31 */     String str = paramHttpServletRequest.getParameter("secretPhrase");
/*  30: 33 */     for (Object localObject1 = User.getAllUsers().iterator(); ((Iterator)localObject1).hasNext();)
/*  31:    */     {
/*  32: 33 */       localObject2 = (User)((Iterator)localObject1).next();
/*  33: 34 */       if (str.equals(((User)localObject2).getSecretPhrase()))
/*  34:    */       {
/*  35: 35 */         ((User)localObject2).deinitializeKeyPair();
/*  36: 36 */         if (!((User)localObject2).isInactive()) {
/*  37: 37 */           ((User)localObject2).enqueue(JSONResponses.LOCK_ACCOUNT);
/*  38:    */         }
/*  39:    */       }
/*  40:    */     }
/*  41: 42 */     localObject1 = paramUser.initializeKeyPair(str);
/*  42: 43 */     Object localObject2 = Long.valueOf(((BigInteger)localObject1).longValue());
/*  43:    */     
/*  44: 45 */     JSONObject localJSONObject1 = new JSONObject();
/*  45: 46 */     localJSONObject1.put("response", "unlockAccount");
/*  46: 47 */     localJSONObject1.put("account", ((BigInteger)localObject1).toString());
/*  47: 49 */     if (str.length() < 30) {
/*  48: 51 */       localJSONObject1.put("secretPhraseStrength", Integer.valueOf(1));
/*  49:    */     } else {
/*  50: 55 */       localJSONObject1.put("secretPhraseStrength", Integer.valueOf(5));
/*  51:    */     }
/*  52: 59 */     Account localAccount = Account.getAccount((Long)localObject2);
/*  53: 60 */     if (localAccount == null)
/*  54:    */     {
/*  55: 62 */       localJSONObject1.put("balance", Integer.valueOf(0));
/*  56:    */     }
/*  57:    */     else
/*  58:    */     {
/*  59: 66 */       localJSONObject1.put("balance", Long.valueOf(localAccount.getUnconfirmedBalance()));
/*  60:    */       
/*  61: 68 */       long l = localAccount.getEffectiveBalance();
/*  62:    */       Object localObject7;
/*  63:    */       Object localObject6;
/*  64: 69 */       if (l > 0L)
/*  65:    */       {
/*  66: 71 */         localObject3 = new JSONObject();
/*  67: 72 */         ((JSONObject)localObject3).put("response", "setBlockGenerationDeadline");
/*  68:    */         
/*  69: 74 */         localObject4 = Blockchain.getLastBlock();
/*  70: 75 */         localObject5 = Crypto.sha256();
/*  71: 77 */         if (((Block)localObject4).getHeight() < 30000)
/*  72:    */         {
/*  73: 79 */           localObject7 = Crypto.sign(((Block)localObject4).getGenerationSignature(), paramUser.getSecretPhrase());
/*  74: 80 */           localObject6 = ((MessageDigest)localObject5).digest((byte[])localObject7);
/*  75:    */         }
/*  76:    */         else
/*  77:    */         {
/*  78: 84 */           ((MessageDigest)localObject5).update(((Block)localObject4).getGenerationSignature());
/*  79: 85 */           localObject6 = ((MessageDigest)localObject5).digest(paramUser.getPublicKey());
/*  80:    */         }
/*  81: 88 */         localObject7 = new BigInteger(1, new byte[] { localObject6[7], localObject6[6], localObject6[5], localObject6[4], localObject6[3], localObject6[2], localObject6[1], localObject6[0] });
/*  82: 89 */         ((JSONObject)localObject3).put("deadline", Long.valueOf(((BigInteger)localObject7).divide(BigInteger.valueOf(((Block)localObject4).getBaseTarget()).multiply(BigInteger.valueOf(l))).longValue() - (Convert.getEpochTime() - ((Block)localObject4).getTimestamp())));
/*  83:    */         
/*  84: 91 */         paramUser.enqueue((JSONStreamAware)localObject3);
/*  85:    */       }
/*  86: 95 */       Object localObject3 = new JSONArray();
/*  87: 96 */       Object localObject4 = localAccount.getPublicKey();
/*  88: 97 */       for (Object localObject5 = Blockchain.getAllUnconfirmedTransactions().iterator(); ((Iterator)localObject5).hasNext();)
/*  89:    */       {
/*  90: 97 */         localObject6 = (Transaction)((Iterator)localObject5).next();
/*  91: 99 */         if (Arrays.equals(((Transaction)localObject6).getSenderPublicKey(), (byte[])localObject4))
/*  92:    */         {
/*  93:101 */           localObject7 = new JSONObject();
/*  94:102 */           ((JSONObject)localObject7).put("index", Integer.valueOf(((Transaction)localObject6).getIndex()));
/*  95:103 */           ((JSONObject)localObject7).put("transactionTimestamp", Integer.valueOf(((Transaction)localObject6).getTimestamp()));
/*  96:104 */           ((JSONObject)localObject7).put("deadline", Short.valueOf(((Transaction)localObject6).getDeadline()));
/*  97:105 */           ((JSONObject)localObject7).put("account", Convert.convert(((Transaction)localObject6).getRecipientId()));
/*  98:106 */           ((JSONObject)localObject7).put("sentAmount", Integer.valueOf(((Transaction)localObject6).getAmount()));
/*  99:107 */           if (((Long)localObject2).equals(((Transaction)localObject6).getRecipientId())) {
/* 100:109 */             ((JSONObject)localObject7).put("receivedAmount", Integer.valueOf(((Transaction)localObject6).getAmount()));
/* 101:    */           }
/* 102:112 */           ((JSONObject)localObject7).put("fee", Integer.valueOf(((Transaction)localObject6).getFee()));
/* 103:113 */           ((JSONObject)localObject7).put("numberOfConfirmations", Integer.valueOf(0));
/* 104:114 */           ((JSONObject)localObject7).put("id", ((Transaction)localObject6).getStringId());
/* 105:    */           
/* 106:116 */           ((JSONArray)localObject3).add(localObject7);
/* 107:    */         }
/* 108:118 */         else if (((Long)localObject2).equals(((Transaction)localObject6).getRecipientId()))
/* 109:    */         {
/* 110:120 */           localObject7 = new JSONObject();
/* 111:121 */           ((JSONObject)localObject7).put("index", Integer.valueOf(((Transaction)localObject6).getIndex()));
/* 112:122 */           ((JSONObject)localObject7).put("transactionTimestamp", Integer.valueOf(((Transaction)localObject6).getTimestamp()));
/* 113:123 */           ((JSONObject)localObject7).put("deadline", Short.valueOf(((Transaction)localObject6).getDeadline()));
/* 114:124 */           ((JSONObject)localObject7).put("account", Convert.convert(((Transaction)localObject6).getSenderAccountId()));
/* 115:125 */           ((JSONObject)localObject7).put("receivedAmount", Integer.valueOf(((Transaction)localObject6).getAmount()));
/* 116:126 */           ((JSONObject)localObject7).put("fee", Integer.valueOf(((Transaction)localObject6).getFee()));
/* 117:127 */           ((JSONObject)localObject7).put("numberOfConfirmations", Integer.valueOf(0));
/* 118:128 */           ((JSONObject)localObject7).put("id", ((Transaction)localObject6).getStringId());
/* 119:    */           
/* 120:130 */           ((JSONArray)localObject3).add(localObject7);
/* 121:    */         }
/* 122:    */       }
/* 123:136 */       localObject5 = Blockchain.getLastBlock().getId();
/* 124:137 */       int i = 1;
/* 125:138 */       while (((JSONArray)localObject3).size() < 1000)
/* 126:    */       {
/* 127:140 */         localObject7 = Blockchain.getBlock((Long)localObject5);
/* 128:    */         Object localObject8;
/* 129:142 */         if ((((Block)localObject7).getTotalFee() > 0) && (Arrays.equals(((Block)localObject7).getGeneratorPublicKey(), (byte[])localObject4)))
/* 130:    */         {
/* 131:144 */           localObject8 = new JSONObject();
/* 132:145 */           ((JSONObject)localObject8).put("index", ((Block)localObject7).getStringId());
/* 133:146 */           ((JSONObject)localObject8).put("blockTimestamp", Integer.valueOf(((Block)localObject7).getTimestamp()));
/* 134:147 */           ((JSONObject)localObject8).put("block", ((Block)localObject7).getStringId());
/* 135:148 */           ((JSONObject)localObject8).put("earnedAmount", Integer.valueOf(((Block)localObject7).getTotalFee()));
/* 136:149 */           ((JSONObject)localObject8).put("numberOfConfirmations", Integer.valueOf(i));
/* 137:150 */           ((JSONObject)localObject8).put("id", "-");
/* 138:    */           
/* 139:152 */           ((JSONArray)localObject3).add(localObject8);
/* 140:    */         }
/* 141:156 */         for (Object localObject9 : ((Block)localObject7).getTransactions())
/* 142:    */         {
/* 143:    */           JSONObject localJSONObject2;
/* 144:158 */           if (Arrays.equals(localObject9.getSenderPublicKey(), (byte[])localObject4))
/* 145:    */           {
/* 146:160 */             localJSONObject2 = new JSONObject();
/* 147:161 */             localJSONObject2.put("index", Integer.valueOf(localObject9.getIndex()));
/* 148:162 */             localJSONObject2.put("blockTimestamp", Integer.valueOf(((Block)localObject7).getTimestamp()));
/* 149:163 */             localJSONObject2.put("transactionTimestamp", Integer.valueOf(localObject9.getTimestamp()));
/* 150:164 */             localJSONObject2.put("account", Convert.convert(localObject9.getRecipientId()));
/* 151:165 */             localJSONObject2.put("sentAmount", Integer.valueOf(localObject9.getAmount()));
/* 152:166 */             if (((Long)localObject2).equals(localObject9.getRecipientId())) {
/* 153:168 */               localJSONObject2.put("receivedAmount", Integer.valueOf(localObject9.getAmount()));
/* 154:    */             }
/* 155:171 */             localJSONObject2.put("fee", Integer.valueOf(localObject9.getFee()));
/* 156:172 */             localJSONObject2.put("numberOfConfirmations", Integer.valueOf(i));
/* 157:173 */             localJSONObject2.put("id", localObject9.getStringId());
/* 158:    */             
/* 159:175 */             ((JSONArray)localObject3).add(localJSONObject2);
/* 160:    */           }
/* 161:177 */           else if (((Long)localObject2).equals(localObject9.getRecipientId()))
/* 162:    */           {
/* 163:179 */             localJSONObject2 = new JSONObject();
/* 164:180 */             localJSONObject2.put("index", Integer.valueOf(localObject9.getIndex()));
/* 165:181 */             localJSONObject2.put("blockTimestamp", Integer.valueOf(((Block)localObject7).getTimestamp()));
/* 166:182 */             localJSONObject2.put("transactionTimestamp", Integer.valueOf(localObject9.getTimestamp()));
/* 167:183 */             localJSONObject2.put("account", Convert.convert(localObject9.getSenderAccountId()));
/* 168:184 */             localJSONObject2.put("receivedAmount", Integer.valueOf(localObject9.getAmount()));
/* 169:185 */             localJSONObject2.put("fee", Integer.valueOf(localObject9.getFee()));
/* 170:186 */             localJSONObject2.put("numberOfConfirmations", Integer.valueOf(i));
/* 171:187 */             localJSONObject2.put("id", localObject9.getStringId());
/* 172:    */             
/* 173:189 */             ((JSONArray)localObject3).add(localJSONObject2);
/* 174:    */           }
/* 175:    */         }
/* 176:192 */         if (((Long)localObject5).equals(Genesis.GENESIS_BLOCK_ID)) {
/* 177:    */           break;
/* 178:    */         }
/* 179:195 */         localObject5 = ((Block)localObject7).getPreviousBlockId();
/* 180:196 */         i++;
/* 181:    */       }
/* 182:198 */       if (((JSONArray)localObject3).size() > 0)
/* 183:    */       {
/* 184:199 */         localObject7 = new JSONObject();
/* 185:200 */         ((JSONObject)localObject7).put("response", "processNewData");
/* 186:201 */         ((JSONObject)localObject7).put("addedMyTransactions", localObject3);
/* 187:202 */         paramUser.enqueue((JSONStreamAware)localObject7);
/* 188:    */       }
/* 189:    */     }
/* 190:205 */     return localJSONObject1;
/* 191:    */   }
/* 192:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.UnlockAccount
 * JD-Core Version:    0.7.0.1
 */