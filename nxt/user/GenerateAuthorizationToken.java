/*  1:   */ package nxt.user;
/*  2:   */ 
/*  3:   */ import java.io.IOException;
/*  4:   */ import javax.servlet.http.HttpServletRequest;
/*  5:   */ import nxt.crypto.Crypto;
/*  6:   */ import nxt.util.Convert;
/*  7:   */ import org.json.simple.JSONObject;
/*  8:   */ import org.json.simple.JSONStreamAware;
/*  9:   */ 
/* 10:   */ final class GenerateAuthorizationToken
/* 11:   */   extends UserRequestHandler
/* 12:   */ {
/* 13:15 */   static final GenerateAuthorizationToken instance = new GenerateAuthorizationToken();
/* 14:   */   
/* 15:   */   public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
/* 16:   */     throws IOException
/* 17:   */   {
/* 18:21 */     String str1 = paramHttpServletRequest.getParameter("secretPhrase");
/* 19:22 */     if (!paramUser.getSecretPhrase().equals(str1)) {
/* 20:23 */       return JSONResponses.INVALID_SECRET_PHRASE;
/* 21:   */     }
/* 22:25 */     byte[] arrayOfByte1 = paramHttpServletRequest.getParameter("website").trim().getBytes("UTF-8");
/* 23:26 */     byte[] arrayOfByte2 = new byte[arrayOfByte1.length + 32 + 4];
/* 24:27 */     System.arraycopy(arrayOfByte1, 0, arrayOfByte2, 0, arrayOfByte1.length);
/* 25:28 */     System.arraycopy(paramUser.getPublicKey(), 0, arrayOfByte2, arrayOfByte1.length, 32);
/* 26:29 */     int i = Convert.getEpochTime();
/* 27:30 */     arrayOfByte2[(arrayOfByte1.length + 32)] = ((byte)i);
/* 28:31 */     arrayOfByte2[(arrayOfByte1.length + 32 + 1)] = ((byte)(i >> 8));
/* 29:32 */     arrayOfByte2[(arrayOfByte1.length + 32 + 2)] = ((byte)(i >> 16));
/* 30:33 */     arrayOfByte2[(arrayOfByte1.length + 32 + 3)] = ((byte)(i >> 24));
/* 31:   */     
/* 32:35 */     byte[] arrayOfByte3 = new byte[100];
/* 33:36 */     System.arraycopy(arrayOfByte2, arrayOfByte1.length, arrayOfByte3, 0, 36);
/* 34:37 */     System.arraycopy(Crypto.sign(arrayOfByte2, paramUser.getSecretPhrase()), 0, arrayOfByte3, 36, 64);
/* 35:38 */     String str2 = "";
/* 36:39 */     for (int j = 0; j < 100; j += 5)
/* 37:   */     {
/* 38:41 */       long l = arrayOfByte3[j] & 0xFF | (arrayOfByte3[(j + 1)] & 0xFF) << 8 | (arrayOfByte3[(j + 2)] & 0xFF) << 16 | (arrayOfByte3[(j + 3)] & 0xFF) << 24 | (arrayOfByte3[(j + 4)] & 0xFF) << 32;
/* 39:43 */       if (l < 32L) {
/* 40:45 */         str2 = str2 + "0000000";
/* 41:47 */       } else if (l < 1024L) {
/* 42:49 */         str2 = str2 + "000000";
/* 43:51 */       } else if (l < 32768L) {
/* 44:53 */         str2 = str2 + "00000";
/* 45:55 */       } else if (l < 1048576L) {
/* 46:57 */         str2 = str2 + "0000";
/* 47:59 */       } else if (l < 33554432L) {
/* 48:61 */         str2 = str2 + "000";
/* 49:63 */       } else if (l < 1073741824L) {
/* 50:65 */         str2 = str2 + "00";
/* 51:67 */       } else if (l < 34359738368L) {
/* 52:69 */         str2 = str2 + "0";
/* 53:   */       }
/* 54:72 */       str2 = str2 + Long.toString(l, 32);
/* 55:   */     }
/* 56:76 */     JSONObject localJSONObject = new JSONObject();
/* 57:77 */     localJSONObject.put("response", "showAuthorizationToken");
/* 58:78 */     localJSONObject.put("token", str2);
/* 59:   */     
/* 60:80 */     return localJSONObject;
/* 61:   */   }
/* 62:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.GenerateAuthorizationToken
 * JD-Core Version:    0.7.0.1
 */