/*  1:   */ package nxt.util;
/*  2:   */ 
/*  3:   */ import java.math.BigInteger;
/*  4:   */ import nxt.Nxt;
/*  5:   */ 
/*  6:   */ public final class Convert
/*  7:   */ {
/*  8:   */   public static final String alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";
/*  9:10 */   public static final BigInteger two64 = new BigInteger("18446744073709551616");
/* 10:   */   
/* 11:   */   public static byte[] convert(String paramString)
/* 12:   */   {
/* 13:15 */     byte[] arrayOfByte = new byte[paramString.length() / 2];
/* 14:16 */     for (int i = 0; i < arrayOfByte.length; i++)
/* 15:   */     {
/* 16:17 */       int j = "0123456789abcdefghijklmnopqrstuvwxyz".indexOf(paramString.charAt(i * 2));
/* 17:18 */       int k = "0123456789abcdefghijklmnopqrstuvwxyz".indexOf(paramString.charAt(i * 2 + 1));
/* 18:19 */       if ((j < 0) || (k < 0) || (j > 15)) {
/* 19:20 */         throw new NumberFormatException("Invalid hex number: " + paramString);
/* 20:   */       }
/* 21:22 */       arrayOfByte[i] = ((byte)((j << 4) + k));
/* 22:   */     }
/* 23:25 */     return arrayOfByte;
/* 24:   */   }
/* 25:   */   
/* 26:   */   public static String convert(byte[] paramArrayOfByte)
/* 27:   */   {
/* 28:30 */     StringBuilder localStringBuilder = new StringBuilder();
/* 29:31 */     for (int k : paramArrayOfByte)
/* 30:   */     {
/* 31:   */       int m;
/* 32:33 */       localStringBuilder.append("0123456789abcdefghijklmnopqrstuvwxyz".charAt((m = k & 0xFF) >> 4)).append("0123456789abcdefghijklmnopqrstuvwxyz".charAt(m & 0xF));
/* 33:   */     }
/* 34:35 */     return localStringBuilder.toString();
/* 35:   */   }
/* 36:   */   
/* 37:   */   public static String convert(long paramLong)
/* 38:   */   {
/* 39:41 */     BigInteger localBigInteger = BigInteger.valueOf(paramLong);
/* 40:42 */     if (paramLong < 0L) {
/* 41:43 */       localBigInteger = localBigInteger.add(two64);
/* 42:   */     }
/* 43:45 */     return localBigInteger.toString();
/* 44:   */   }
/* 45:   */   
/* 46:   */   public static String convert(Long paramLong)
/* 47:   */   {
/* 48:50 */     return convert(nullToZero(paramLong));
/* 49:   */   }
/* 50:   */   
/* 51:   */   public static Long parseUnsignedLong(String paramString)
/* 52:   */   {
/* 53:55 */     if (paramString == null) {
/* 54:56 */       throw new IllegalArgumentException("trying to parse null");
/* 55:   */     }
/* 56:58 */     BigInteger localBigInteger = new BigInteger(paramString.trim());
/* 57:59 */     if ((localBigInteger.signum() < 0) || (localBigInteger.compareTo(two64) != -1)) {
/* 58:60 */       throw new IllegalArgumentException("overflow: " + paramString);
/* 59:   */     }
/* 60:62 */     return zeroToNull(localBigInteger.longValue());
/* 61:   */   }
/* 62:   */   
/* 63:   */   public static int getEpochTime()
/* 64:   */   {
/* 65:68 */     return (int)((System.currentTimeMillis() - Nxt.epochBeginning + 500L) / 1000L);
/* 66:   */   }
/* 67:   */   
/* 68:   */   public static Long zeroToNull(long paramLong)
/* 69:   */   {
/* 70:73 */     return paramLong == 0L ? null : Long.valueOf(paramLong);
/* 71:   */   }
/* 72:   */   
/* 73:   */   public static long nullToZero(Long paramLong)
/* 74:   */   {
/* 75:77 */     return paramLong == null ? 0L : paramLong.longValue();
/* 76:   */   }
/* 77:   */   
/* 78:   */   public static String truncate(String paramString1, String paramString2, int paramInt, boolean paramBoolean)
/* 79:   */   {
/* 80:81 */     return paramString1.length() > paramInt ? paramString1.substring(0, paramBoolean ? paramInt - 3 : paramInt) + (paramBoolean ? "..." : "") : paramString1 == null ? paramString2 : paramString1;
/* 81:   */   }
/* 82:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.util.Convert
 * JD-Core Version:    0.7.0.1
 */