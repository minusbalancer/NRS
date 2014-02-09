/*  1:   */ package nxt.http;
/*  2:   */ 
/*  3:   */ import java.io.UnsupportedEncodingException;
/*  4:   */ import javax.servlet.http.HttpServletRequest;
/*  5:   */ import nxt.Account;
/*  6:   */ import nxt.crypto.Crypto;
/*  7:   */ import nxt.util.Convert;
/*  8:   */ import org.json.simple.JSONObject;
/*  9:   */ import org.json.simple.JSONStreamAware;
/* 10:   */ 
/* 11:   */ final class DecodeToken
/* 12:   */   extends HttpRequestHandler
/* 13:   */ {
/* 14:19 */   static final DecodeToken instance = new DecodeToken();
/* 15:   */   
/* 16:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/* 17:   */   {
/* 18:26 */     String str1 = paramHttpServletRequest.getParameter("website");
/* 19:27 */     String str2 = paramHttpServletRequest.getParameter("token");
/* 20:28 */     if (str1 == null) {
/* 21:29 */       return JSONResponses.MISSING_WEBSITE;
/* 22:   */     }
/* 23:30 */     if (str2 == null) {
/* 24:31 */       return JSONResponses.MISSING_TOKEN;
/* 25:   */     }
/* 26:   */     try
/* 27:   */     {
/* 28:35 */       byte[] arrayOfByte1 = str1.trim().getBytes("UTF-8");
/* 29:36 */       byte[] arrayOfByte2 = new byte[100];
/* 30:37 */       int i = 0;int j = 0;
/* 31:   */       try
/* 32:   */       {
/* 33:40 */         for (; i < str2.length(); j += 5)
/* 34:   */         {
/* 35:42 */           long l = Long.parseLong(str2.substring(i, i + 8), 32);
/* 36:43 */           arrayOfByte2[j] = ((byte)(int)l);
/* 37:44 */           arrayOfByte2[(j + 1)] = ((byte)(int)(l >> 8));
/* 38:45 */           arrayOfByte2[(j + 2)] = ((byte)(int)(l >> 16));
/* 39:46 */           arrayOfByte2[(j + 3)] = ((byte)(int)(l >> 24));
/* 40:47 */           arrayOfByte2[(j + 4)] = ((byte)(int)(l >> 32));i += 8;
/* 41:   */         }
/* 42:   */       }
/* 43:   */       catch (NumberFormatException localNumberFormatException)
/* 44:   */       {
/* 45:52 */         return JSONResponses.INCORRECT_TOKEN;
/* 46:   */       }
/* 47:55 */       if (i != 160) {
/* 48:56 */         return JSONResponses.INCORRECT_TOKEN;
/* 49:   */       }
/* 50:58 */       byte[] arrayOfByte3 = new byte[32];
/* 51:59 */       System.arraycopy(arrayOfByte2, 0, arrayOfByte3, 0, 32);
/* 52:60 */       int k = arrayOfByte2[32] & 0xFF | (arrayOfByte2[33] & 0xFF) << 8 | (arrayOfByte2[34] & 0xFF) << 16 | (arrayOfByte2[35] & 0xFF) << 24;
/* 53:61 */       byte[] arrayOfByte4 = new byte[64];
/* 54:62 */       System.arraycopy(arrayOfByte2, 36, arrayOfByte4, 0, 64);
/* 55:   */       
/* 56:64 */       byte[] arrayOfByte5 = new byte[arrayOfByte1.length + 36];
/* 57:65 */       System.arraycopy(arrayOfByte1, 0, arrayOfByte5, 0, arrayOfByte1.length);
/* 58:66 */       System.arraycopy(arrayOfByte2, 0, arrayOfByte5, arrayOfByte1.length, 36);
/* 59:67 */       boolean bool = Crypto.verify(arrayOfByte4, arrayOfByte5, arrayOfByte3);
/* 60:   */       
/* 61:69 */       JSONObject localJSONObject = new JSONObject();
/* 62:70 */       localJSONObject.put("account", Convert.convert(Account.getId(arrayOfByte3)));
/* 63:71 */       localJSONObject.put("timestamp", Integer.valueOf(k));
/* 64:72 */       localJSONObject.put("valid", Boolean.valueOf(bool));
/* 65:   */       
/* 66:74 */       return localJSONObject;
/* 67:   */     }
/* 68:   */     catch (RuntimeException|UnsupportedEncodingException localRuntimeException) {}
/* 69:77 */     return JSONResponses.INCORRECT_WEBSITE;
/* 70:   */   }
/* 71:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.DecodeToken
 * JD-Core Version:    0.7.0.1
 */