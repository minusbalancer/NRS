/*   1:    */ package nxt.peer;
/*   2:    */ 
/*   3:    */ import java.io.UnsupportedEncodingException;
/*   4:    */ import java.nio.ByteBuffer;
/*   5:    */ import java.nio.ByteOrder;
/*   6:    */ import java.util.concurrent.ThreadLocalRandom;
/*   7:    */ import nxt.crypto.Crypto;
/*   8:    */ import nxt.util.Convert;
/*   9:    */ 
/*  10:    */ public final class Hallmark
/*  11:    */ {
/*  12:    */   private final String host;
/*  13:    */   private final int weight;
/*  14:    */   private final int date;
/*  15:    */   private final byte[] publicKey;
/*  16:    */   private final byte[] signature;
/*  17:    */   private final boolean isValid;
/*  18:    */   
/*  19:    */   public static int parseDate(String paramString)
/*  20:    */   {
/*  21: 15 */     return Integer.parseInt(paramString.substring(0, 4)) * 10000 + Integer.parseInt(paramString.substring(5, 7)) * 100 + Integer.parseInt(paramString.substring(8, 10));
/*  22:    */   }
/*  23:    */   
/*  24:    */   public static String formatDate(int paramInt)
/*  25:    */   {
/*  26: 21 */     int i = paramInt / 10000;
/*  27: 22 */     int j = paramInt % 10000 / 100;
/*  28: 23 */     int k = paramInt % 100;
/*  29: 24 */     return (i < 1000 ? "0" : i < 100 ? "00" : i < 10 ? "000" : "") + i + "-" + (j < 10 ? "0" : "") + j + "-" + (k < 10 ? "0" : "") + k;
/*  30:    */   }
/*  31:    */   
/*  32:    */   public static String generateHallmark(String paramString1, String paramString2, int paramInt1, int paramInt2)
/*  33:    */   {
/*  34:    */     try
/*  35:    */     {
/*  36: 31 */       if ((paramString2.length() == 0) || (paramString2.length() > 100)) {
/*  37: 32 */         throw new IllegalArgumentException("Hostname length should be between 1 and 100");
/*  38:    */       }
/*  39: 34 */       if ((paramInt1 <= 0) || (paramInt1 > 1000000000L)) {
/*  40: 35 */         throw new IllegalArgumentException("Weight should be between 1 and 1000000000");
/*  41:    */       }
/*  42: 38 */       byte[] arrayOfByte1 = Crypto.getPublicKey(paramString1);
/*  43: 39 */       byte[] arrayOfByte2 = paramString2.getBytes("UTF-8");
/*  44:    */       
/*  45: 41 */       ByteBuffer localByteBuffer = ByteBuffer.allocate(34 + arrayOfByte2.length + 4 + 4 + 1);
/*  46: 42 */       localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/*  47: 43 */       localByteBuffer.put(arrayOfByte1);
/*  48: 44 */       localByteBuffer.putShort((short)arrayOfByte2.length);
/*  49: 45 */       localByteBuffer.put(arrayOfByte2);
/*  50: 46 */       localByteBuffer.putInt(paramInt1);
/*  51: 47 */       localByteBuffer.putInt(paramInt2);
/*  52:    */       
/*  53: 49 */       byte[] arrayOfByte3 = localByteBuffer.array();
/*  54:    */       byte[] arrayOfByte4;
/*  55:    */       do
/*  56:    */       {
/*  57: 52 */         arrayOfByte3[(arrayOfByte3.length - 1)] = ((byte)ThreadLocalRandom.current().nextInt());
/*  58: 53 */         arrayOfByte4 = Crypto.sign(arrayOfByte3, paramString1);
/*  59: 54 */       } while (!Crypto.verify(arrayOfByte4, arrayOfByte3, arrayOfByte1));
/*  60: 56 */       return Convert.convert(arrayOfByte3) + Convert.convert(arrayOfByte4);
/*  61:    */     }
/*  62:    */     catch (UnsupportedEncodingException localUnsupportedEncodingException)
/*  63:    */     {
/*  64: 59 */       throw new RuntimeException(localUnsupportedEncodingException.toString(), localUnsupportedEncodingException);
/*  65:    */     }
/*  66:    */   }
/*  67:    */   
/*  68:    */   public static Hallmark parseHallmark(String paramString)
/*  69:    */   {
/*  70:    */     try
/*  71:    */     {
/*  72: 67 */       byte[] arrayOfByte1 = Convert.convert(paramString);
/*  73:    */       
/*  74: 69 */       ByteBuffer localByteBuffer = ByteBuffer.wrap(arrayOfByte1);
/*  75: 70 */       localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
/*  76:    */       
/*  77: 72 */       byte[] arrayOfByte2 = new byte[32];
/*  78: 73 */       localByteBuffer.get(arrayOfByte2);
/*  79: 74 */       int i = localByteBuffer.getShort();
/*  80: 75 */       if (i > 300) {
/*  81: 76 */         throw new IllegalArgumentException("Invalid host length");
/*  82:    */       }
/*  83: 78 */       byte[] arrayOfByte3 = new byte[i];
/*  84: 79 */       localByteBuffer.get(arrayOfByte3);
/*  85: 80 */       String str = new String(arrayOfByte3, "UTF-8");
/*  86: 81 */       int j = localByteBuffer.getInt();
/*  87: 82 */       int k = localByteBuffer.getInt();
/*  88: 83 */       localByteBuffer.get();
/*  89: 84 */       byte[] arrayOfByte4 = new byte[64];
/*  90: 85 */       localByteBuffer.get(arrayOfByte4);
/*  91:    */       
/*  92: 87 */       byte[] arrayOfByte5 = new byte[arrayOfByte1.length - 64];
/*  93: 88 */       System.arraycopy(arrayOfByte1, 0, arrayOfByte5, 0, arrayOfByte5.length);
/*  94:    */       
/*  95: 90 */       boolean bool = (str.length() < 100) && (j > 0) && (j <= 1000000000L) && (Crypto.verify(arrayOfByte4, arrayOfByte5, arrayOfByte2));
/*  96:    */       
/*  97: 92 */       return new Hallmark(arrayOfByte2, arrayOfByte4, str, j, k, bool);
/*  98:    */     }
/*  99:    */     catch (UnsupportedEncodingException localUnsupportedEncodingException)
/* 100:    */     {
/* 101: 95 */       throw new RuntimeException(localUnsupportedEncodingException.toString(), localUnsupportedEncodingException);
/* 102:    */     }
/* 103:    */   }
/* 104:    */   
/* 105:    */   private Hallmark(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, String paramString, int paramInt1, int paramInt2, boolean paramBoolean)
/* 106:    */   {
/* 107:107 */     this.host = paramString;
/* 108:108 */     this.publicKey = paramArrayOfByte1;
/* 109:109 */     this.signature = paramArrayOfByte2;
/* 110:110 */     this.weight = paramInt1;
/* 111:111 */     this.date = paramInt2;
/* 112:112 */     this.isValid = paramBoolean;
/* 113:    */   }
/* 114:    */   
/* 115:    */   public String getHost()
/* 116:    */   {
/* 117:116 */     return this.host;
/* 118:    */   }
/* 119:    */   
/* 120:    */   public int getWeight()
/* 121:    */   {
/* 122:120 */     return this.weight;
/* 123:    */   }
/* 124:    */   
/* 125:    */   public int getDate()
/* 126:    */   {
/* 127:124 */     return this.date;
/* 128:    */   }
/* 129:    */   
/* 130:    */   public byte[] getSignature()
/* 131:    */   {
/* 132:128 */     return this.signature;
/* 133:    */   }
/* 134:    */   
/* 135:    */   public byte[] getPublicKey()
/* 136:    */   {
/* 137:132 */     return this.publicKey;
/* 138:    */   }
/* 139:    */   
/* 140:    */   public boolean isValid()
/* 141:    */   {
/* 142:136 */     return this.isValid;
/* 143:    */   }
/* 144:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.Hallmark
 * JD-Core Version:    0.7.0.1
 */