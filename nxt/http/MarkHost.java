/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.io.UnsupportedEncodingException;
/*  4:   */ import java.nio.ByteBuffer;
/*  5:   */ import java.nio.ByteOrder;
/*  6:   */ import java.util.concurrent.ThreadLocalRandom;
/*  7:   */ import javax.servlet.http.HttpServletRequest;
/*  8:   */ import nxt.crypto.Crypto;
/*  9:   */ import nxt.util.Convert;
/* 10:   */ import org.json.simple.JSONObject;
/* 11:   */ import org.json.simple.JSONStreamAware;
/* 12:   */ 
/* 13:   */ final class MarkHost
/* 14:   */   extends HttpRequestHandler
/* 15:   */ {
/* 16:26 */   static final MarkHost instance = new MarkHost();
/* 17:   */   
/* 18:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 19:   */   {
/* 20:33 */     String str1 = paramHttpServletRequest.getParameter("secretPhrase");
/* 21:34 */     String str2 = paramHttpServletRequest.getParameter("host");
/* 22:35 */     String str3 = paramHttpServletRequest.getParameter("weight");
/* 23:36 */     String str4 = paramHttpServletRequest.getParameter("date");
/* 24:37 */     if (str1 == null) {
/* 25:38 */       return JSONResponses.MISSING_SECRET_PHRASE;
/* 26:   */     }
/* 27:39 */     if (str2 == null) {
/* 28:40 */       return JSONResponses.MISSING_HOST;
/* 29:   */     }
/* 30:41 */     if (str3 == null) {
/* 31:42 */       return JSONResponses.MISSING_WEIGHT;
/* 32:   */     }
/* 33:43 */     if (str4 == null) {
/* 34:44 */       return JSONResponses.MISSING_DATE;
/* 35:   */     }
/* 36:47 */     if (str2.length() > 100) {
/* 37:48 */       return JSONResponses.INCORRECT_HOST;
/* 38:   */     }
/* 39:   */     int i;
/* 40:   */     try
/* 41:   */     {
/* 42:53 */       i = Integer.parseInt(str3);
/* 43:54 */       if ((i <= 0) || (i > 1000000000L)) {
/* 44:55 */         return JSONResponses.INCORRECT_WEIGHT;
/* 45:   */       }
/* 46:   */     }
/* 47:   */     catch (NumberFormatException localNumberFormatException1)
/* 48:   */     {
/* 49:58 */       return JSONResponses.INCORRECT_WEIGHT;
/* 50:   */     }
/* 51:   */     int j;
/* 52:   */     try
/* 53:   */     {
/* 54:63 */       j = Integer.parseInt(str4.substring(0, 4)) * 10000 + Integer.parseInt(str4.substring(5, 7)) * 100 + Integer.parseInt(str4.substring(8, 10));
/* 55:   */     }
/* 56:   */     catch (NumberFormatException localNumberFormatException2)
/* 57:   */     {
/* 58:65 */       return JSONResponses.INCORRECT_DATE;
/* 59:   */     }
/* 60:   */     try
/* 61:   */     {
/* 62:70 */       byte[] arrayOfByte1 = Crypto.getPublicKey(str1);
/* 63:71 */       byte[] arrayOfByte2 = str2.getBytes("UTF-8");
/* 64:   */       
/* 65:73 */       ByteBuffer localByteBuffer = ByteBuffer.allocate(34 + arrayOfByte2.length + 4 + 4 + 1);
/* 66:74 */       localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/* 67:75 */       localByteBuffer.put(arrayOfByte1);
/* 68:76 */       localByteBuffer.putShort((short)arrayOfByte2.length);
/* 69:77 */       localByteBuffer.put(arrayOfByte2);
/* 70:78 */       localByteBuffer.putInt(i);
/* 71:79 */       localByteBuffer.putInt(j);
/* 72:   */       
/* 73:81 */       byte[] arrayOfByte3 = localByteBuffer.array();
/* 74:   */       byte[] arrayOfByte4;
/* 75:   */       do
/* 76:   */       {
/* 77:84 */         arrayOfByte3[(arrayOfByte3.length - 1)] = ((byte)ThreadLocalRandom.current().nextInt());
/* 78:85 */         arrayOfByte4 = Crypto.sign(arrayOfByte3, str1);
/* 79:86 */       } while (!Crypto.verify(arrayOfByte4, arrayOfByte3, arrayOfByte1));
/* 80:88 */       JSONObject localJSONObject = new JSONObject();
/* 81:89 */       localJSONObject.put("hallmark", Convert.convert(arrayOfByte3) + Convert.convert(arrayOfByte4));
/* 82:90 */       return localJSONObject;
/* 83:   */     }
/* 84:   */     catch (RuntimeException|UnsupportedEncodingException localRuntimeException) {}
/* 85:93 */     return JSONResponses.INCORRECT_HOST;
/* 86:   */   }
/* 87:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.MarkHost
 * JD-Core Version:    0.7.0.1
 */