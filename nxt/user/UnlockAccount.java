/*   1:    */ package nxt.user;
/*   2:    */ 
/*   3:    */ import java.io.IOException;
/*   4:    */ import java.math.BigInteger;
/*   5:    */ import java.security.MessageDigest;
/*   6:    */ import java.util.Arrays;
/*   7:    */ import java.util.Collection;
/*   8:    */ import java.util.Iterator;
/*   9:    */ import java.util.SortedMap;
/*  10:    */ import java.util.TreeMap;
/*  11:    */ import javax.servlet.http.HttpServletRequest;
/*  12:    */ import nxt.Account;
/*  13:    */ import nxt.Block;
/*  14:    */ import nxt.Blockchain;
/*  15:    */ import nxt.Transaction;
/*  16:    */ import nxt.crypto.Crypto;
/*  17:    */ import nxt.util.Convert;
/*  18:    */ import nxt.util.DbIterator;
/*  19:    */ import org.json.simple.JSONArray;
/*  20:    */ import org.json.simple.JSONObject;
/*  21:    */ import org.json.simple.JSONStreamAware;
/*  22:    */ 
/*  23:    */ final class UnlockAccount
/*  24:    */   extends UserRequestHandler
/*  25:    */ {
/*  26: 28 */   static final UnlockAccount instance = new UnlockAccount();
/*  27:    */   
/*  28:    */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
/*  29:    */     throws IOException
/*  30:    */   {
/*  31: 34 */     String str = paramHttpServletRequest.getParameter("secretPhrase");
/*  32: 36 */     for (Object localObject1 = User.getAllUsers().iterator(); ((Iterator)localObject1).hasNext();)
/*  33:    */     {
/*  34: 36 */       localObject2 = (User)((Iterator)localObject1).next();
/*  35: 37 */       if (str.equals(((User)localObject2).getSecretPhrase()))
/*  36:    */       {
/*  37: 38 */         ((User)localObject2).deinitializeKeyPair();
/*  38: 39 */         if (!((User)localObject2).isInactive()) {
/*  39: 40 */           ((User)localObject2).enqueue(JSONResponses.LOCK_ACCOUNT);
/*  40:    */         }
/*  41:    */       }
/*  42:    */     }
/*  43: 45 */     localObject1 = paramUser.initializeKeyPair(str);
/*  44: 46 */     Object localObject2 = Long.valueOf(((BigInteger)localObject1).longValue());
/*  45:    */     
/*  46: 48 */     JSONObject localJSONObject1 = new JSONObject();
/*  47: 49 */     localJSONObject1.put("response", "unlockAccount");
/*  48: 50 */     localJSONObject1.put("account", ((BigInteger)localObject1).toString());
/*  49: 52 */     if (str.length() < 30) {
/*  50: 54 */       localJSONObject1.put("secretPhraseStrength", Integer.valueOf(1));
/*  51:    */     } else {
/*  52: 58 */       localJSONObject1.put("secretPhraseStrength", Integer.valueOf(5));
/*  53:    */     }
/*  54: 62 */     Account localAccount = Account.getAccount((Long)localObject2);
/*  55: 63 */     if (localAccount == null)
/*  56:    */     {
/*  57: 65 */       localJSONObject1.put("balance", Integer.valueOf(0));
/*  58:    */     }
/*  59:    */     else
/*  60:    */     {
/*  61: 69 */       localJSONObject1.put("balance", Long.valueOf(localAccount.getUnconfirmedBalance()));
/*  62:    */       
/*  63: 71 */       long l = localAccount.getEffectiveBalance();
/*  64:    */       Object localObject6;
/*  65: 72 */       if (l > 0L)
/*  66:    */       {
/*  67: 74 */         localObject3 = new JSONObject();
/*  68: 75 */         ((JSONObject)localObject3).put("response", "setBlockGenerationDeadline");
/*  69:    */         
/*  70: 77 */         localObject4 = Blockchain.getLastBlock();
/*  71: 78 */         localObject5 = Crypto.sha256();
/*  72: 80 */         if (((Block)localObject4).getHeight() < 30000)
/*  73:    */         {
/*  74: 82 */           localObject7 = Crypto.sign(((Block)localObject4).getGenerationSignature(), paramUser.getSecretPhrase());
/*  75: 83 */           localObject6 = ((MessageDigest)localObject5).digest((byte[])localObject7);
/*  76:    */         }
/*  77:    */         else
/*  78:    */         {
/*  79: 87 */           ((MessageDigest)localObject5).update(((Block)localObject4).getGenerationSignature());
/*  80: 88 */           localObject6 = ((MessageDigest)localObject5).digest(paramUser.getPublicKey());
/*  81:    */         }
/*  82: 91 */         localObject7 = new BigInteger(1, new byte[] { localObject6[7], localObject6[6], localObject6[5], localObject6[4], localObject6[3], localObject6[2], localObject6[1], localObject6[0] });
/*  83: 92 */         ((JSONObject)localObject3).put("deadline", Long.valueOf(((BigInteger)localObject7).divide(BigInteger.valueOf(((Block)localObject4).getBaseTarget()).multiply(BigInteger.valueOf(l))).longValue() - (Convert.getEpochTime() - ((Block)localObject4).getTimestamp())));
/*  84:    */         
/*  85: 94 */         paramUser.enqueue((JSONStreamAware)localObject3);
/*  86:    */       }
/*  87: 98 */       Object localObject3 = new JSONArray();
/*  88: 99 */       Object localObject4 = localAccount.getPublicKey();
/*  89:100 */       for (Object localObject5 = Blockchain.getAllUnconfirmedTransactions().iterator(); ((Iterator)localObject5).hasNext();)
/*  90:    */       {
/*  91:100 */         localObject6 = (Transaction)((Iterator)localObject5).next();
/*  92:102 */         if (Arrays.equals(((Transaction)localObject6).getSenderPublicKey(), (byte[])localObject4))
/*  93:    */         {
/*  94:104 */           localObject7 = new JSONObject();
/*  95:105 */           ((JSONObject)localObject7).put("index", Integer.valueOf(((Transaction)localObject6).getIndex()));
/*  96:106 */           ((JSONObject)localObject7).put("transactionTimestamp", Integer.valueOf(((Transaction)localObject6).getTimestamp()));
/*  97:107 */           ((JSONObject)localObject7).put("deadline", Short.valueOf(((Transaction)localObject6).getDeadline()));
/*  98:108 */           ((JSONObject)localObject7).put("account", Convert.convert(((Transaction)localObject6).getRecipientId()));
/*  99:109 */           ((JSONObject)localObject7).put("sentAmount", Integer.valueOf(((Transaction)localObject6).getAmount()));
/* 100:110 */           if (((Long)localObject2).equals(((Transaction)localObject6).getRecipientId())) {
/* 101:112 */             ((JSONObject)localObject7).put("receivedAmount", Integer.valueOf(((Transaction)localObject6).getAmount()));
/* 102:    */           }
/* 103:115 */           ((JSONObject)localObject7).put("fee", Integer.valueOf(((Transaction)localObject6).getFee()));
/* 104:116 */           ((JSONObject)localObject7).put("numberOfConfirmations", Integer.valueOf(0));
/* 105:117 */           ((JSONObject)localObject7).put("id", ((Transaction)localObject6).getStringId());
/* 106:    */           
/* 107:119 */           ((JSONArray)localObject3).add(localObject7);
/* 108:    */         }
/* 109:121 */         else if (((Long)localObject2).equals(((Transaction)localObject6).getRecipientId()))
/* 110:    */         {
/* 111:123 */           localObject7 = new JSONObject();
/* 112:124 */           ((JSONObject)localObject7).put("index", Integer.valueOf(((Transaction)localObject6).getIndex()));
/* 113:125 */           ((JSONObject)localObject7).put("transactionTimestamp", Integer.valueOf(((Transaction)localObject6).getTimestamp()));
/* 114:126 */           ((JSONObject)localObject7).put("deadline", Short.valueOf(((Transaction)localObject6).getDeadline()));
/* 115:127 */           ((JSONObject)localObject7).put("account", Convert.convert(((Transaction)localObject6).getSenderAccountId()));
/* 116:128 */           ((JSONObject)localObject7).put("receivedAmount", Integer.valueOf(((Transaction)localObject6).getAmount()));
/* 117:129 */           ((JSONObject)localObject7).put("fee", Integer.valueOf(((Transaction)localObject6).getFee()));
/* 118:130 */           ((JSONObject)localObject7).put("numberOfConfirmations", Integer.valueOf(0));
/* 119:131 */           ((JSONObject)localObject7).put("id", ((Transaction)localObject6).getStringId());
/* 120:    */           
/* 121:133 */           ((JSONArray)localObject3).add(localObject7);
/* 122:    */         }
/* 123:    */       }
/* 124:139 */       localObject5 = new TreeMap();
/* 125:    */       
/* 126:141 */       int i = Blockchain.getLastBlock().getHeight();
/* 127:142 */       Object localObject7 = Blockchain.getAllBlocks(localAccount, 0);Object localObject8 = null;
/* 128:    */       JSONObject localJSONObject2;
/* 129:    */       try
/* 130:    */       {
/* 131:143 */         while (((DbIterator)localObject7).hasNext())
/* 132:    */         {
/* 133:144 */           Block localBlock = (Block)((DbIterator)localObject7).next();
/* 134:145 */           if (localBlock.getTotalFee() > 0)
/* 135:    */           {
/* 136:146 */             localJSONObject2 = new JSONObject();
/* 137:147 */             localJSONObject2.put("index", localBlock.getStringId());
/* 138:148 */             localJSONObject2.put("blockTimestamp", Integer.valueOf(localBlock.getTimestamp()));
/* 139:149 */             localJSONObject2.put("block", localBlock.getStringId());
/* 140:150 */             localJSONObject2.put("earnedAmount", Integer.valueOf(localBlock.getTotalFee()));
/* 141:151 */             localJSONObject2.put("numberOfConfirmations", Integer.valueOf(i - localBlock.getHeight()));
/* 142:152 */             localJSONObject2.put("id", "-");
/* 143:153 */             ((SortedMap)localObject5).put(Integer.valueOf(-localBlock.getTimestamp()), localJSONObject2);
/* 144:    */           }
/* 145:    */         }
/* 146:    */       }
/* 147:    */       catch (Throwable localThrowable2)
/* 148:    */       {
/* 149:142 */         localObject8 = localThrowable2;throw localThrowable2;
/* 150:    */       }
/* 151:    */       finally
/* 152:    */       {
/* 153:156 */         if (localObject7 != null) {
/* 154:156 */           if (localObject8 != null) {
/* 155:    */             try
/* 156:    */             {
/* 157:156 */               ((DbIterator)localObject7).close();
/* 158:    */             }
/* 159:    */             catch (Throwable localThrowable5)
/* 160:    */             {
/* 161:156 */               ((Throwable)localObject8).addSuppressed(localThrowable5);
/* 162:    */             }
/* 163:    */           } else {
/* 164:156 */             ((DbIterator)localObject7).close();
/* 165:    */           }
/* 166:    */         }
/* 167:    */       }
/* 168:158 */       localObject7 = Blockchain.getAllTransactions(localAccount, (byte)-1, (byte)-1, 0);localObject8 = null;
/* 169:    */       try
/* 170:    */       {
/* 171:159 */         while (((DbIterator)localObject7).hasNext())
/* 172:    */         {
/* 173:160 */           Transaction localTransaction = (Transaction)((DbIterator)localObject7).next();
/* 174:161 */           if (localTransaction.getSenderAccountId().equals(localObject2))
/* 175:    */           {
/* 176:162 */             localJSONObject2 = new JSONObject();
/* 177:163 */             localJSONObject2.put("index", Integer.valueOf(localTransaction.getIndex()));
/* 178:164 */             localJSONObject2.put("blockTimestamp", Integer.valueOf(localTransaction.getBlock().getTimestamp()));
/* 179:165 */             localJSONObject2.put("transactionTimestamp", Integer.valueOf(localTransaction.getTimestamp()));
/* 180:166 */             localJSONObject2.put("account", Convert.convert(localTransaction.getRecipientId()));
/* 181:167 */             localJSONObject2.put("sentAmount", Integer.valueOf(localTransaction.getAmount()));
/* 182:168 */             if (((Long)localObject2).equals(localTransaction.getRecipientId())) {
/* 183:169 */               localJSONObject2.put("receivedAmount", Integer.valueOf(localTransaction.getAmount()));
/* 184:    */             }
/* 185:171 */             localJSONObject2.put("fee", Integer.valueOf(localTransaction.getFee()));
/* 186:172 */             localJSONObject2.put("numberOfConfirmations", Integer.valueOf(i - localTransaction.getBlock().getHeight()));
/* 187:173 */             localJSONObject2.put("id", localTransaction.getStringId());
/* 188:174 */             ((SortedMap)localObject5).put(Integer.valueOf(-localTransaction.getTimestamp()), localJSONObject2);
/* 189:    */           }
/* 190:175 */           else if (localTransaction.getRecipientId().equals(localObject2))
/* 191:    */           {
/* 192:176 */             localJSONObject2 = new JSONObject();
/* 193:177 */             localJSONObject2.put("index", Integer.valueOf(localTransaction.getIndex()));
/* 194:178 */             localJSONObject2.put("blockTimestamp", Integer.valueOf(localTransaction.getBlock().getTimestamp()));
/* 195:179 */             localJSONObject2.put("transactionTimestamp", Integer.valueOf(localTransaction.getTimestamp()));
/* 196:180 */             localJSONObject2.put("account", Convert.convert(localTransaction.getSenderAccountId()));
/* 197:181 */             localJSONObject2.put("receivedAmount", Integer.valueOf(localTransaction.getAmount()));
/* 198:182 */             localJSONObject2.put("fee", Integer.valueOf(localTransaction.getFee()));
/* 199:183 */             localJSONObject2.put("numberOfConfirmations", Integer.valueOf(i - localTransaction.getBlock().getHeight()));
/* 200:184 */             localJSONObject2.put("id", localTransaction.getStringId());
/* 201:185 */             ((SortedMap)localObject5).put(Integer.valueOf(-localTransaction.getTimestamp()), localJSONObject2);
/* 202:    */           }
/* 203:    */         }
/* 204:    */       }
/* 205:    */       catch (Throwable localThrowable4)
/* 206:    */       {
/* 207:158 */         localObject8 = localThrowable4;throw localThrowable4;
/* 208:    */       }
/* 209:    */       finally
/* 210:    */       {
/* 211:188 */         if (localObject7 != null) {
/* 212:188 */           if (localObject8 != null) {
/* 213:    */             try
/* 214:    */             {
/* 215:188 */               ((DbIterator)localObject7).close();
/* 216:    */             }
/* 217:    */             catch (Throwable localThrowable6)
/* 218:    */             {
/* 219:188 */               ((Throwable)localObject8).addSuppressed(localThrowable6);
/* 220:    */             }
/* 221:    */           } else {
/* 222:188 */             ((DbIterator)localObject7).close();
/* 223:    */           }
/* 224:    */         }
/* 225:    */       }
/* 226:190 */       localObject7 = ((SortedMap)localObject5).values().iterator();
/* 227:191 */       while ((((JSONArray)localObject3).size() < 1000) && (((Iterator)localObject7).hasNext())) {
/* 228:192 */         ((JSONArray)localObject3).add(((Iterator)localObject7).next());
/* 229:    */       }
/* 230:195 */       if (((JSONArray)localObject3).size() > 0)
/* 231:    */       {
/* 232:196 */         localObject8 = new JSONObject();
/* 233:197 */         ((JSONObject)localObject8).put("response", "processNewData");
/* 234:198 */         ((JSONObject)localObject8).put("addedMyTransactions", localObject3);
/* 235:199 */         paramUser.enqueue((JSONStreamAware)localObject8);
/* 236:    */       }
/* 237:    */     }
/* 238:202 */     return localJSONObject1;
/* 239:    */   }
/* 240:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.UnlockAccount
 * JD-Core Version:    0.7.0.1
 */