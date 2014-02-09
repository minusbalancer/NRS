/*  1:   */ package nxt.util;
/*  2:   */ 
/*  3:   */ import java.io.FilterInputStream;
/*  4:   */ import java.io.IOException;
/*  5:   */ import java.io.InputStream;
/*  6:   */ 
/*  7:   */ public class CountingInputStream
/*  8:   */   extends FilterInputStream
/*  9:   */ {
/* 10:   */   private long count;
/* 11:   */   
/* 12:   */   public CountingInputStream(InputStream paramInputStream)
/* 13:   */   {
/* 14:12 */     super(paramInputStream);
/* 15:   */   }
/* 16:   */   
/* 17:   */   public int read()
/* 18:   */     throws IOException
/* 19:   */   {
/* 20:17 */     int i = super.read();
/* 21:18 */     if (i >= 0) {
/* 22:19 */       this.count += 1L;
/* 23:   */     }
/* 24:21 */     return i;
/* 25:   */   }
/* 26:   */   
/* 27:   */   public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
/* 28:   */     throws IOException
/* 29:   */   {
/* 30:26 */     int i = super.read(paramArrayOfByte, paramInt1, paramInt2);
/* 31:27 */     if (i >= 0) {
/* 32:28 */       this.count += 1L;
/* 33:   */     }
/* 34:30 */     return i;
/* 35:   */   }
/* 36:   */   
/* 37:   */   public long skip(long paramLong)
/* 38:   */     throws IOException
/* 39:   */   {
/* 40:35 */     long l = super.skip(paramLong);
/* 41:36 */     if (l >= 0L) {
/* 42:37 */       this.count += l;
/* 43:   */     }
/* 44:39 */     return l;
/* 45:   */   }
/* 46:   */   
/* 47:   */   public long getCount()
/* 48:   */   {
/* 49:43 */     return this.count;
/* 50:   */   }
/* 51:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.util.CountingInputStream
 * JD-Core Version:    0.7.0.1
 */