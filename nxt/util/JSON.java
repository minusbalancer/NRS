/*  1:   */ package nxt.util;
/*  2:   */ 
/*  3:   */ import java.io.IOException;
/*  4:   */ import java.io.Writer;
/*  5:   */ import org.json.simple.JSONObject;
/*  6:   */ import org.json.simple.JSONStreamAware;
/*  7:   */ 
/*  8:   */ public final class JSON
/*  9:   */ {
/* 10:13 */   public static final JSONStreamAware emptyJSON = prepare(new JSONObject());
/* 11:   */   
/* 12:   */   public static JSONStreamAware prepare(JSONObject paramJSONObject)
/* 13:   */   {
/* 14:16 */     new JSONStreamAware()
/* 15:   */     {
/* 16:17 */       private final char[] jsonChars = this.val$json.toJSONString().toCharArray();
/* 17:   */       
/* 18:   */       public void writeJSONString(Writer paramAnonymousWriter)
/* 19:   */         throws IOException
/* 20:   */       {
/* 21:20 */         paramAnonymousWriter.write(this.jsonChars);
/* 22:   */       }
/* 23:   */     };
/* 24:   */   }
/* 25:   */   
/* 26:   */   public static JSONStreamAware prepareRequest(JSONObject paramJSONObject)
/* 27:   */   {
/* 28:26 */     paramJSONObject.put("protocol", Integer.valueOf(1));
/* 29:27 */     return prepare(paramJSONObject);
/* 30:   */   }
/* 31:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.util.JSON
 * JD-Core Version:    0.7.0.1
 */