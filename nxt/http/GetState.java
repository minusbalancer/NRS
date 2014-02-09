/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.math.BigInteger;
/*  4:   */ import java.util.Collection;
/*  5:   */ import java.util.Iterator;
/*  6:   */ import javax.servlet.http.HttpServletRequest;
/*  7:   */ import nxt.Account;
/*  8:   */ import nxt.Alias;
/*  9:   */ import nxt.Asset;
/* 10:   */ import nxt.Block;
/* 11:   */ import nxt.Blockchain;
/* 12:   */ import nxt.Order.Ask;
/* 13:   */ import nxt.Order.Bid;
/* 14:   */ import nxt.peer.Peer;
/* 15:   */ import nxt.user.User;
/* 16:   */ import nxt.util.Convert;
/* 17:   */ import org.json.simple.JSONObject;
/* 18:   */ import org.json.simple.JSONStreamAware;
/* 19:   */ 
/* 20:   */ public final class GetState
/* 21:   */   extends HttpRequestHandler
/* 22:   */ {
/* 23:19 */   static final GetState instance = new GetState();
/* 24:   */   
/* 25:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 26:   */   {
/* 27:26 */     JSONObject localJSONObject = new JSONObject();
/* 28:   */     
/* 29:28 */     localJSONObject.put("version", "0.7.1");
/* 30:29 */     localJSONObject.put("time", Integer.valueOf(Convert.getEpochTime()));
/* 31:30 */     localJSONObject.put("lastBlock", Blockchain.getLastBlock().getStringId());
/* 32:31 */     localJSONObject.put("cumulativeDifficulty", Blockchain.getLastBlock().getCumulativeDifficulty().toString());
/* 33:   */     
/* 34:33 */     long l1 = 0L;
/* 35:34 */     for (Iterator localIterator = Account.getAllAccounts().iterator(); localIterator.hasNext();)
/* 36:   */     {
/* 37:34 */       localObject = (Account)localIterator.next();
/* 38:35 */       long l2 = ((Account)localObject).getEffectiveBalance();
/* 39:36 */       if (l2 > 0L) {
/* 40:37 */         l1 += l2;
/* 41:   */       }
/* 42:   */     }
/* 43:40 */     localJSONObject.put("totalEffectiveBalance", Long.valueOf(l1 * 100L));
/* 44:   */     
/* 45:42 */     localJSONObject.put("numberOfBlocks", Integer.valueOf(Blockchain.getBlockCount()));
/* 46:43 */     localJSONObject.put("numberOfTransactions", Integer.valueOf(Blockchain.getTransactionCount()));
/* 47:44 */     localJSONObject.put("numberOfAccounts", Integer.valueOf(Account.getAllAccounts().size()));
/* 48:45 */     localJSONObject.put("numberOfAssets", Integer.valueOf(Asset.getAllAssets().size()));
/* 49:46 */     localJSONObject.put("numberOfOrders", Integer.valueOf(Order.Ask.getAllAskOrders().size() + Order.Bid.getAllBidOrders().size()));
/* 50:47 */     localJSONObject.put("numberOfAliases", Integer.valueOf(Alias.getAllAliases().size()));
/* 51:48 */     localJSONObject.put("numberOfPeers", Integer.valueOf(Peer.getAllPeers().size()));
/* 52:49 */     localJSONObject.put("numberOfUsers", Integer.valueOf(User.getAllUsers().size()));
/* 53:50 */     int i = 0;
/* 54:51 */     for (Object localObject = User.getAllUsers().iterator(); ((Iterator)localObject).hasNext();)
/* 55:   */     {
/* 56:51 */       User localUser = (User)((Iterator)localObject).next();
/* 57:52 */       if (localUser.getSecretPhrase() != null) {
/* 58:53 */         i++;
/* 59:   */       }
/* 60:   */     }
/* 61:56 */     localJSONObject.put("numberOfUnlockedAccounts", Integer.valueOf(i));
/* 62:57 */     localObject = Blockchain.getLastBlockchainFeeder();
/* 63:58 */     localJSONObject.put("lastBlockchainFeeder", localObject == null ? null : ((Peer)localObject).getAnnouncedAddress());
/* 64:59 */     localJSONObject.put("availableProcessors", Integer.valueOf(Runtime.getRuntime().availableProcessors()));
/* 65:60 */     localJSONObject.put("maxMemory", Long.valueOf(Runtime.getRuntime().maxMemory()));
/* 66:61 */     localJSONObject.put("totalMemory", Long.valueOf(Runtime.getRuntime().totalMemory()));
/* 67:62 */     localJSONObject.put("freeMemory", Long.valueOf(Runtime.getRuntime().freeMemory()));
/* 68:   */     
/* 69:64 */     return localJSONObject;
/* 70:   */   }
/* 71:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetState
 * JD-Core Version:    0.7.0.1
 */