/*  1:   */ package nxt.util;
/*  2:   */ 
/*  3:   */ import java.io.FilterOutputStream;
/*  4:   */ import java.io.IOException;
/*  5:   */ import java.io.OutputStream;
/*  6:   */ 
/*  7:   */ public class CountingOutputStream
/*  8:   */   extends FilterOutputStream
/*  9:   */ {
/* 10:   */   private long count;
/* 11:   */   
/* 12:   */   public CountingOutputStream(OutputStream paramOutputStream)
/* 13:   */   {
/* 14:12 */     super(paramOutputStream);
/* 15:   */   }
/* 16:   */   
/* 17:   */   public void write(int paramInt)
/* 18:   */     throws IOException
/* 19:   */   {
/* 20:17 */     this.count += 1L;
/* 21:18 */     super.write(paramInt);
/* 22:   */   }
/* 23:   */   
/* 24:   */   public long getCount()
/* 25:   */   {
/* 26:22 */     return this.count;
/* 27:   */   }
/* 28:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.util.CountingOutputStream
 * JD-Core Version:    0.7.0.1
 */