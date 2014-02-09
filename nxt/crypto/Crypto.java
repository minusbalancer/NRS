/*   1:    */ package nxt.crypto;
/*   2:    */ 
/*   3:    */ import java.io.UnsupportedEncodingException;
/*   4:    */ import java.security.MessageDigest;
/*   5:    */ import java.security.NoSuchAlgorithmException;
/*   6:    */ import java.util.Arrays;
/*   7:    */ import nxt.util.Logger;
/*   8:    */ 
/*   9:    */ public final class Crypto
/*  10:    */ {
/*  11:    */   public static MessageDigest getMessageDigest(String paramString)
/*  12:    */   {
/*  13:    */     try
/*  14:    */     {
/*  15: 16 */       return MessageDigest.getInstance(paramString);
/*  16:    */     }
/*  17:    */     catch (NoSuchAlgorithmException localNoSuchAlgorithmException)
/*  18:    */     {
/*  19: 18 */       Logger.logMessage("Missing message digest algorithm: " + paramString);
/*  20: 19 */       System.exit(1);
/*  21:    */     }
/*  22: 20 */     return null;
/*  23:    */   }
/*  24:    */   
/*  25:    */   public static MessageDigest sha256()
/*  26:    */   {
/*  27: 25 */     return getMessageDigest("SHA-256");
/*  28:    */   }
/*  29:    */   
/*  30:    */   public static byte[] getPublicKey(String paramString)
/*  31:    */   {
/*  32:    */     try
/*  33:    */     {
/*  34: 32 */       byte[] arrayOfByte = new byte[32];
/*  35: 33 */       Curve25519.keygen(arrayOfByte, null, sha256().digest(paramString.getBytes("UTF-8")));
/*  36:    */       
/*  37: 35 */       return arrayOfByte;
/*  38:    */     }
/*  39:    */     catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
/*  40:    */     {
/*  41: 38 */       Logger.logMessage("Error getting public key", localRuntimeException);
/*  42:    */     }
/*  43: 39 */     return null;
/*  44:    */   }
/*  45:    */   
/*  46:    */   public static byte[] sign(byte[] paramArrayOfByte, String paramString)
/*  47:    */   {
/*  48:    */     try
/*  49:    */     {
/*  50: 48 */       byte[] arrayOfByte1 = new byte[32];
/*  51: 49 */       byte[] arrayOfByte2 = new byte[32];
/*  52: 50 */       MessageDigest localMessageDigest = sha256();
/*  53: 51 */       Curve25519.keygen(arrayOfByte1, arrayOfByte2, localMessageDigest.digest(paramString.getBytes("UTF-8")));
/*  54:    */       
/*  55: 53 */       byte[] arrayOfByte3 = localMessageDigest.digest(paramArrayOfByte);
/*  56:    */       
/*  57: 55 */       localMessageDigest.update(arrayOfByte3);
/*  58: 56 */       byte[] arrayOfByte4 = localMessageDigest.digest(arrayOfByte2);
/*  59:    */       
/*  60: 58 */       byte[] arrayOfByte5 = new byte[32];
/*  61: 59 */       Curve25519.keygen(arrayOfByte5, null, arrayOfByte4);
/*  62:    */       
/*  63: 61 */       localMessageDigest.update(arrayOfByte3);
/*  64: 62 */       byte[] arrayOfByte6 = localMessageDigest.digest(arrayOfByte5);
/*  65:    */       
/*  66: 64 */       byte[] arrayOfByte7 = new byte[32];
/*  67: 65 */       Curve25519.sign(arrayOfByte7, arrayOfByte6, arrayOfByte4, arrayOfByte2);
/*  68:    */       
/*  69: 67 */       byte[] arrayOfByte8 = new byte[64];
/*  70: 68 */       System.arraycopy(arrayOfByte7, 0, arrayOfByte8, 0, 32);
/*  71: 69 */       System.arraycopy(arrayOfByte6, 0, arrayOfByte8, 32, 32);
/*  72:    */       
/*  73: 71 */       return arrayOfByte8;
/*  74:    */     }
/*  75:    */     catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
/*  76:    */     {
/*  77: 74 */       Logger.logMessage("Error in signing message", localRuntimeException);
/*  78:    */     }
/*  79: 75 */     return null;
/*  80:    */   }
/*  81:    */   
/*  82:    */   public static boolean verify(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3)
/*  83:    */   {
/*  84:    */     try
/*  85:    */     {
/*  86: 84 */       byte[] arrayOfByte1 = new byte[32];
/*  87: 85 */       byte[] arrayOfByte2 = new byte[32];
/*  88: 86 */       System.arraycopy(paramArrayOfByte1, 0, arrayOfByte2, 0, 32);
/*  89: 87 */       byte[] arrayOfByte3 = new byte[32];
/*  90: 88 */       System.arraycopy(paramArrayOfByte1, 32, arrayOfByte3, 0, 32);
/*  91: 89 */       Curve25519.verify(arrayOfByte1, arrayOfByte2, arrayOfByte3, paramArrayOfByte3);
/*  92:    */       
/*  93: 91 */       MessageDigest localMessageDigest = sha256();
/*  94: 92 */       byte[] arrayOfByte4 = localMessageDigest.digest(paramArrayOfByte2);
/*  95: 93 */       localMessageDigest.update(arrayOfByte4);
/*  96: 94 */       byte[] arrayOfByte5 = localMessageDigest.digest(arrayOfByte1);
/*  97:    */       
/*  98: 96 */       return Arrays.equals(arrayOfByte3, arrayOfByte5);
/*  99:    */     }
/* 100:    */     catch (RuntimeException localRuntimeException)
/* 101:    */     {
/* 102: 99 */       Logger.logMessage("Error in Crypto verify", localRuntimeException);
/* 103:    */     }
/* 104:100 */     return false;
/* 105:    */   }
/* 106:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.crypto.Crypto
 * JD-Core Version:    0.7.0.1
 */