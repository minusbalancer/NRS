/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.io.UnsupportedEncodingException;
/*  4:   */ import java.nio.ByteBuffer;
/*  5:   */ import java.nio.ByteOrder;
/*  6:   */ import javax.servlet.http.HttpServletRequest;
/*  7:   */ import nxt.Account;
/*  8:   */ import nxt.crypto.Crypto;
/*  9:   */ import nxt.util.Convert;
/* 10:   */ import org.json.simple.JSONObject;
/* 11:   */ import org.json.simple.JSONStreamAware;
/* 12:   */ 
/* 13:   */ final class DecodeHallmark
/* 14:   */   extends HttpRequestHandler
/* 15:   */ {
/* 16:20 */   static final DecodeHallmark instance = new DecodeHallmark();
/* 17:   */   
/* 18:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 19:   */   {
/* 20:27 */     String str1 = paramHttpServletRequest.getParameter("hallmark");
/* 21:28 */     if (str1 == null) {
/* 22:29 */       return JSONResponses.MISSING_HALLMARK;
/* 23:   */     }
/* 24:   */     try
/* 25:   */     {
/* 26:33 */       byte[] arrayOfByte1 = Convert.convert(str1);
/* 27:   */       
/* 28:35 */       ByteBuffer localByteBuffer = ByteBuffer.wrap(arrayOfByte1);
/* 29:36 */       localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/* 30:   */       
/* 31:38 */       byte[] arrayOfByte2 = new byte[32];
/* 32:39 */       localByteBuffer.get(arrayOfByte2);
/* 33:40 */       int i = localByteBuffer.getShort();
/* 34:41 */       if (i > 300) {
/* 35:42 */         throw new IllegalArgumentException("Invalid host length");
/* 36:   */       }
/* 37:44 */       byte[] arrayOfByte3 = new byte[i];
/* 38:45 */       localByteBuffer.get(arrayOfByte3);
/* 39:46 */       String str2 = new String(arrayOfByte3, "UTF-8");
/* 40:47 */       int j = localByteBuffer.getInt();
/* 41:48 */       int k = localByteBuffer.getInt();
/* 42:49 */       localByteBuffer.get();
/* 43:50 */       byte[] arrayOfByte4 = new byte[64];
/* 44:51 */       localByteBuffer.get(arrayOfByte4);
/* 45:   */       
/* 46:53 */       JSONObject localJSONObject = new JSONObject();
/* 47:54 */       localJSONObject.put("account", Convert.convert(Account.getId(arrayOfByte2)));
/* 48:55 */       localJSONObject.put("host", str2);
/* 49:56 */       localJSONObject.put("weight", Integer.valueOf(j));
/* 50:57 */       int m = k / 10000;
/* 51:58 */       int n = k % 10000 / 100;
/* 52:59 */       int i1 = k % 100;
/* 53:60 */       localJSONObject.put("date", (m < 1000 ? "0" : m < 100 ? "00" : m < 10 ? "000" : "") + m + "-" + (n < 10 ? "0" : "") + n + "-" + (i1 < 10 ? "0" : "") + i1);
/* 54:61 */       byte[] arrayOfByte5 = new byte[arrayOfByte1.length - 64];
/* 55:62 */       System.arraycopy(arrayOfByte1, 0, arrayOfByte5, 0, arrayOfByte5.length);
/* 56:63 */       localJSONObject.put("valid", Boolean.valueOf((str2.length() > 100) || (j <= 0) || (j > 1000000000L) ? false : Crypto.verify(arrayOfByte4, arrayOfByte5, arrayOfByte2)));
/* 57:64 */       return localJSONObject;
/* 58:   */     }
/* 59:   */     catch (RuntimeException|UnsupportedEncodingException localRuntimeException) {}
/* 60:66 */     return JSONResponses.INCORRECT_HALLMARK;
/* 61:   */   }
/* 62:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.DecodeHallmark
 * JD-Core Version:    0.7.0.1
 */