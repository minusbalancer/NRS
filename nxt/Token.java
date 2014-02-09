/*   1:    */ package nxt;
/*   2:    */ 
/*   3:    */ import java.io.UnsupportedEncodingException;
/*   4:    */ import nxt.crypto.Crypto;
/*   5:    */ import nxt.util.Convert;
/*   6:    */ 
/*   7:    */ public final class Token
/*   8:    */ {
/*   9:    */   private final byte[] publicKey;
/*  10:    */   private final int timestamp;
/*  11:    */   private final boolean isValid;
/*  12:    */   
/*  13:    */   public static String generateToken(String paramString1, String paramString2)
/*  14:    */   {
/*  15:    */     try
/*  16:    */     {
/*  17: 13 */       byte[] arrayOfByte1 = paramString2.getBytes("UTF-8");
/*  18: 14 */       byte[] arrayOfByte2 = new byte[arrayOfByte1.length + 32 + 4];
/*  19: 15 */       System.arraycopy(arrayOfByte1, 0, arrayOfByte2, 0, arrayOfByte1.length);
/*  20: 16 */       System.arraycopy(Crypto.getPublicKey(paramString1), 0, arrayOfByte2, arrayOfByte1.length, 32);
/*  21: 17 */       int i = Convert.getEpochTime();
/*  22: 18 */       arrayOfByte2[(arrayOfByte1.length + 32)] = ((byte)i);
/*  23: 19 */       arrayOfByte2[(arrayOfByte1.length + 32 + 1)] = ((byte)(i >> 8));
/*  24: 20 */       arrayOfByte2[(arrayOfByte1.length + 32 + 2)] = ((byte)(i >> 16));
/*  25: 21 */       arrayOfByte2[(arrayOfByte1.length + 32 + 3)] = ((byte)(i >> 24));
/*  26:    */       
/*  27: 23 */       byte[] arrayOfByte3 = new byte[100];
/*  28: 24 */       System.arraycopy(arrayOfByte2, arrayOfByte1.length, arrayOfByte3, 0, 36);
/*  29: 25 */       System.arraycopy(Crypto.sign(arrayOfByte2, paramString1), 0, arrayOfByte3, 36, 64);
/*  30:    */       
/*  31: 27 */       StringBuilder localStringBuilder = new StringBuilder();
/*  32: 28 */       for (int j = 0; j < 100; j += 5)
/*  33:    */       {
/*  34: 30 */         long l = arrayOfByte3[j] & 0xFF | (arrayOfByte3[(j + 1)] & 0xFF) << 8 | (arrayOfByte3[(j + 2)] & 0xFF) << 16 | (arrayOfByte3[(j + 3)] & 0xFF) << 24 | (arrayOfByte3[(j + 4)] & 0xFF) << 32;
/*  35: 33 */         if (l < 32L) {
/*  36: 34 */           localStringBuilder.append("0000000");
/*  37: 35 */         } else if (l < 1024L) {
/*  38: 36 */           localStringBuilder.append("000000");
/*  39: 37 */         } else if (l < 32768L) {
/*  40: 38 */           localStringBuilder.append("00000");
/*  41: 39 */         } else if (l < 1048576L) {
/*  42: 40 */           localStringBuilder.append("0000");
/*  43: 41 */         } else if (l < 33554432L) {
/*  44: 42 */           localStringBuilder.append("000");
/*  45: 43 */         } else if (l < 1073741824L) {
/*  46: 44 */           localStringBuilder.append("00");
/*  47: 45 */         } else if (l < 34359738368L) {
/*  48: 46 */           localStringBuilder.append("0");
/*  49:    */         }
/*  50: 48 */         localStringBuilder.append(Long.toString(l, 32));
/*  51:    */       }
/*  52: 52 */       return localStringBuilder.toString();
/*  53:    */     }
/*  54:    */     catch (UnsupportedEncodingException localUnsupportedEncodingException)
/*  55:    */     {
/*  56: 55 */       throw new RuntimeException(localUnsupportedEncodingException.toString(), localUnsupportedEncodingException);
/*  57:    */     }
/*  58:    */   }
/*  59:    */   
/*  60:    */   public static Token parseToken(String paramString1, String paramString2)
/*  61:    */   {
/*  62:    */     try
/*  63:    */     {
/*  64: 62 */       byte[] arrayOfByte1 = paramString2.getBytes("UTF-8");
/*  65: 63 */       byte[] arrayOfByte2 = new byte[100];
/*  66: 64 */       int i = 0;
/*  67: 64 */       for (int j = 0; i < paramString1.length(); j += 5)
/*  68:    */       {
/*  69: 68 */         long l = Long.parseLong(paramString1.substring(i, i + 8), 32);
/*  70: 69 */         arrayOfByte2[j] = ((byte)(int)l);
/*  71: 70 */         arrayOfByte2[(j + 1)] = ((byte)(int)(l >> 8));
/*  72: 71 */         arrayOfByte2[(j + 2)] = ((byte)(int)(l >> 16));
/*  73: 72 */         arrayOfByte2[(j + 3)] = ((byte)(int)(l >> 24));
/*  74: 73 */         arrayOfByte2[(j + 4)] = ((byte)(int)(l >> 32));i += 8;
/*  75:    */       }
/*  76: 77 */       if (i != 160) {
/*  77: 78 */         throw new IllegalArgumentException("Invalid token string: " + paramString1);
/*  78:    */       }
/*  79: 80 */       byte[] arrayOfByte3 = new byte[32];
/*  80: 81 */       System.arraycopy(arrayOfByte2, 0, arrayOfByte3, 0, 32);
/*  81: 82 */       int k = arrayOfByte2[32] & 0xFF | (arrayOfByte2[33] & 0xFF) << 8 | (arrayOfByte2[34] & 0xFF) << 16 | (arrayOfByte2[35] & 0xFF) << 24;
/*  82: 83 */       byte[] arrayOfByte4 = new byte[64];
/*  83: 84 */       System.arraycopy(arrayOfByte2, 36, arrayOfByte4, 0, 64);
/*  84:    */       
/*  85: 86 */       byte[] arrayOfByte5 = new byte[arrayOfByte1.length + 36];
/*  86: 87 */       System.arraycopy(arrayOfByte1, 0, arrayOfByte5, 0, arrayOfByte1.length);
/*  87: 88 */       System.arraycopy(arrayOfByte2, 0, arrayOfByte5, arrayOfByte1.length, 36);
/*  88: 89 */       boolean bool = Crypto.verify(arrayOfByte4, arrayOfByte5, arrayOfByte3);
/*  89:    */       
/*  90: 91 */       return new Token(arrayOfByte3, k, bool);
/*  91:    */     }
/*  92:    */     catch (UnsupportedEncodingException localUnsupportedEncodingException)
/*  93:    */     {
/*  94: 94 */       throw new RuntimeException(localUnsupportedEncodingException.toString(), localUnsupportedEncodingException);
/*  95:    */     }
/*  96:    */   }
/*  97:    */   
/*  98:    */   private Token(byte[] paramArrayOfByte, int paramInt, boolean paramBoolean)
/*  99:    */   {
/* 100:103 */     this.publicKey = paramArrayOfByte;
/* 101:104 */     this.timestamp = paramInt;
/* 102:105 */     this.isValid = paramBoolean;
/* 103:    */   }
/* 104:    */   
/* 105:    */   public byte[] getPublicKey()
/* 106:    */   {
/* 107:109 */     return this.publicKey;
/* 108:    */   }
/* 109:    */   
/* 110:    */   public int getTimestamp()
/* 111:    */   {
/* 112:113 */     return this.timestamp;
/* 113:    */   }
/* 114:    */   
/* 115:    */   public boolean isValid()
/* 116:    */   {
/* 117:117 */     return this.isValid;
/* 118:    */   }
/* 119:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.Token
 * JD-Core Version:    0.7.0.1
 */