/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import javax.servlet.http.HttpServletRequest;
/*  4:   */ import nxt.Block;
/*  5:   */ import nxt.Blockchain;
/*  6:   */ import nxt.util.Convert;
/*  7:   */ import org.json.simple.JSONArray;
/*  8:   */ import org.json.simple.JSONObject;
/*  9:   */ import org.json.simple.JSONStreamAware;
/* 10:   */ 
/* 11:   */ final class GetBlock
/* 12:   */   extends HttpRequestHandler
/* 13:   */ {
/* 14:18 */   static final GetBlock instance = new GetBlock();
/* 15:   */   
/* 16:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 17:   */   {
/* 18:25 */     String str = paramHttpServletRequest.getParameter("block");
/* 19:26 */     if (str == null) {
/* 20:27 */       return JSONResponses.MISSING_BLOCK;
/* 21:   */     }
/* 22:   */     Block localBlock;
/* 23:   */     try
/* 24:   */     {
/* 25:32 */       localBlock = Blockchain.getBlock(Convert.parseUnsignedLong(str));
/* 26:33 */       if (localBlock == null) {
/* 27:34 */         return JSONResponses.UNKNOWN_BLOCK;
/* 28:   */       }
/* 29:   */     }
/* 30:   */     catch (RuntimeException localRuntimeException)
/* 31:   */     {
/* 32:37 */       return JSONResponses.INCORRECT_BLOCK;
/* 33:   */     }
/* 34:40 */     JSONObject localJSONObject = new JSONObject();
/* 35:41 */     localJSONObject.put("height", Integer.valueOf(localBlock.getHeight()));
/* 36:42 */     localJSONObject.put("generator", Convert.convert(localBlock.getGeneratorAccountId()));
/* 37:43 */     localJSONObject.put("timestamp", Integer.valueOf(localBlock.getTimestamp()));
/* 38:44 */     localJSONObject.put("numberOfTransactions", Integer.valueOf(localBlock.getTransactionIds().length));
/* 39:45 */     localJSONObject.put("totalAmount", Integer.valueOf(localBlock.getTotalAmount()));
/* 40:46 */     localJSONObject.put("totalFee", Integer.valueOf(localBlock.getTotalFee()));
/* 41:47 */     localJSONObject.put("payloadLength", Integer.valueOf(localBlock.getPayloadLength()));
/* 42:48 */     localJSONObject.put("version", Integer.valueOf(localBlock.getVersion()));
/* 43:49 */     localJSONObject.put("baseTarget", Convert.convert(localBlock.getBaseTarget()));
/* 44:50 */     if (localBlock.getPreviousBlockId() != null) {
/* 45:51 */       localJSONObject.put("previousBlock", Convert.convert(localBlock.getPreviousBlockId()));
/* 46:   */     }
/* 47:53 */     if (localBlock.getNextBlockId() != null) {
/* 48:54 */       localJSONObject.put("nextBlock", Convert.convert(localBlock.getNextBlockId()));
/* 49:   */     }
/* 50:56 */     localJSONObject.put("payloadHash", Convert.convert(localBlock.getPayloadHash()));
/* 51:57 */     localJSONObject.put("generationSignature", Convert.convert(localBlock.getGenerationSignature()));
/* 52:58 */     if (localBlock.getVersion() > 1) {
/* 53:59 */       localJSONObject.put("previousBlockHash", Convert.convert(localBlock.getPreviousBlockHash()));
/* 54:   */     }
/* 55:61 */     localJSONObject.put("blockSignature", Convert.convert(localBlock.getBlockSignature()));
/* 56:62 */     JSONArray localJSONArray = new JSONArray();
/* 57:63 */     for (Long localLong : localBlock.getTransactionIds()) {
/* 58:64 */       localJSONArray.add(Convert.convert(localLong));
/* 59:   */     }
/* 60:66 */     localJSONObject.put("transactions", localJSONArray);
/* 61:   */     
/* 62:68 */     return localJSONObject;
/* 63:   */   }
/* 64:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.GetBlock
 * JD-Core Version:    0.7.0.1
 */