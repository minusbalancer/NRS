/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.util.Collection;
/*  4:   */ import java.util.Iterator;
/*  5:   */ import javax.servlet.http.HttpServletRequest;
/*  6:   */ import nxt.Blockchain;
/*  7:   */ import nxt.Transaction;
/*  8:   */ import org.json.simple.JSONArray;
/*  9:   */ import org.json.simple.JSONObject;
/* 10:   */ import org.json.simple.JSONStreamAware;
/* 11:   */ 
/* 12:   */ final class GetUnconfirmedTransactionIds
/* 13:   */   extends HttpRequestHandler
/* 14:   */ {
/* 15:13 */   static final GetUnconfirmedTransactionIds instance = new GetUnconfirmedTransactionIds();
/* 16:   */   
/* 17:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 18:   */   {
/* 19:20 */     JSONArray localJSONArray = new JSONArray();
/* 20:21 */     for (Object localObject = Blockchain.getAllUnconfirmedTransactions().iterator(); ((Iterator)localObject).hasNext();)
/* 21:   */     {
/* 22:21 */       Transaction localTransaction = (Transaction)((Iterator)localObject).next();
/* 23:22 */       localJSONArray.add(localTransaction.getStringId());
/* 24:   */     }
/* 25:25 */     localObject = new JSONObject();
/* 26:26 */     ((JSONObject)localObject).put("unconfirmedTransactionIds", localJSONArray);
/* 27:27 */     return localObject;
/* 28:   */   }
/* 29:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetUnconfirmedTransactionIds
 * JD-Core Version:    0.7.0.1
 */