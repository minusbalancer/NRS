/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.util.Arrays;
/*  4:   */ import java.util.Collection;
/*  5:   */ import java.util.Iterator;
/*  6:   */ import java.util.PriorityQueue;
/*  7:   */ import javax.servlet.http.HttpServletRequest;
/*  8:   */ import nxt.Account;
/*  9:   */ import nxt.Block;
/* 10:   */ import nxt.Blockchain;
/* 11:   */ import nxt.util.Convert;
/* 12:   */ import org.json.simple.JSONArray;
/* 13:   */ import org.json.simple.JSONObject;
/* 14:   */ import org.json.simple.JSONStreamAware;
/* 15:   */ 
/* 16:   */ public final class GetAccountBlockIds
/* 17:   */   extends HttpRequestHandler
/* 18:   */ {
/* 19:23 */   static final GetAccountBlockIds instance = new GetAccountBlockIds();
/* 20:   */   
/* 21:   */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 22:   */   {
/* 23:30 */     String str1 = paramHttpServletRequest.getParameter("account");
/* 24:31 */     String str2 = paramHttpServletRequest.getParameter("timestamp");
/* 25:32 */     if (str1 == null) {
/* 26:33 */       return JSONResponses.MISSING_ACCOUNT;
/* 27:   */     }
/* 28:34 */     if (str2 == null) {
/* 29:35 */       return JSONResponses.MISSING_TIMESTAMP;
/* 30:   */     }
/* 31:   */     Account localAccount;
/* 32:   */     try
/* 33:   */     {
/* 34:40 */       localAccount = Account.getAccount(Convert.parseUnsignedLong(str1));
/* 35:41 */       if (localAccount == null) {
/* 36:42 */         return JSONResponses.UNKNOWN_ACCOUNT;
/* 37:   */       }
/* 38:   */     }
/* 39:   */     catch (RuntimeException localRuntimeException)
/* 40:   */     {
/* 41:45 */       return JSONResponses.INCORRECT_ACCOUNT;
/* 42:   */     }
/* 43:   */     int i;
/* 44:   */     try
/* 45:   */     {
/* 46:50 */       i = Integer.parseInt(str2);
/* 47:51 */       if (i < 0) {
/* 48:52 */         return JSONResponses.INCORRECT_TIMESTAMP;
/* 49:   */       }
/* 50:   */     }
/* 51:   */     catch (NumberFormatException localNumberFormatException)
/* 52:   */     {
/* 53:55 */       return JSONResponses.INCORRECT_TIMESTAMP;
/* 54:   */     }
/* 55:58 */     PriorityQueue localPriorityQueue = new PriorityQueue(11, Block.heightComparator);
/* 56:59 */     byte[] arrayOfByte = localAccount.getPublicKey();
/* 57:60 */     for (Object localObject1 = Blockchain.getAllBlocks().iterator(); ((Iterator)localObject1).hasNext();)
/* 58:   */     {
/* 59:60 */       localObject2 = (Block)((Iterator)localObject1).next();
/* 60:61 */       if ((((Block)localObject2).getTimestamp() >= i) && (Arrays.equals(((Block)localObject2).getGeneratorPublicKey(), arrayOfByte))) {
/* 61:62 */         localPriorityQueue.offer(localObject2);
/* 62:   */       }
/* 63:   */     }
/* 64:66 */     localObject1 = new JSONArray();
/* 65:67 */     while (!localPriorityQueue.isEmpty()) {
/* 66:68 */       ((JSONArray)localObject1).add(((Block)localPriorityQueue.poll()).getStringId());
/* 67:   */     }
/* 68:71 */     Object localObject2 = new JSONObject();
/* 69:72 */     ((JSONObject)localObject2).put("blockIds", localObject1);
/* 70:   */     
/* 71:74 */     return localObject2;
/* 72:   */   }
/* 73:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetAccountBlockIds
 * JD-Core Version:    0.7.0.1
 */