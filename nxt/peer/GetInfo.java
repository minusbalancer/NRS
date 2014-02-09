/*  1:   */ package nxt.peer;
/*  2:   */ 
/*  3:   */ import java.math.BigInteger;
/*  4:   */ import nxt.Block;
/*  5:   */ import nxt.Blockchain;
/*  6:   */ import nxt.Nxt;
/*  7:   */ import org.json.simple.JSONObject;
/*  8:   */ 
/*  9:   */ final class GetInfo
/* 10:   */   extends HttpJSONRequestHandler
/* 11:   */ {
/* 12: 9 */   static final GetInfo instance = new GetInfo();
/* 13:   */   
/* 14:   */   JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 15:   */   {
/* 16:17 */     JSONObject localJSONObject = new JSONObject();
/* 17:19 */     if (paramPeer != null)
/* 18:   */     {
/* 19:20 */       String str1 = (String)paramJSONObject.get("announcedAddress");
/* 20:21 */       if (str1 != null)
/* 21:   */       {
/* 22:22 */         str1 = str1.trim();
/* 23:23 */         if (str1.length() > 0) {
/* 24:24 */           paramPeer.setAnnouncedAddress(str1);
/* 25:   */         }
/* 26:   */       }
/* 27:27 */       String str2 = (String)paramJSONObject.get("application");
/* 28:28 */       if (str2 == null) {
/* 29:29 */         str2 = "?";
/* 30:   */       }
/* 31:31 */       paramPeer.setApplication(str2.trim());
/* 32:   */       
/* 33:33 */       String str3 = (String)paramJSONObject.get("version");
/* 34:34 */       if (str3 == null) {
/* 35:35 */         str3 = "?";
/* 36:   */       }
/* 37:37 */       paramPeer.setVersion(str3.trim());
/* 38:   */       
/* 39:39 */       String str4 = (String)paramJSONObject.get("platform");
/* 40:40 */       if (str4 == null) {
/* 41:41 */         str4 = "?";
/* 42:   */       }
/* 43:43 */       paramPeer.setPlatform(str4.trim());
/* 44:   */       
/* 45:45 */       paramPeer.setShareAddress(Boolean.TRUE.equals(paramJSONObject.get("shareAddress")));
/* 46:   */     }
/* 47:49 */     if ((Nxt.myHallmark != null) && (Nxt.myHallmark.length() > 0)) {
/* 48:51 */       localJSONObject.put("hallmark", Nxt.myHallmark);
/* 49:   */     }
/* 50:54 */     localJSONObject.put("application", "NRS");
/* 51:55 */     localJSONObject.put("version", "0.7.1");
/* 52:56 */     localJSONObject.put("platform", Nxt.myPlatform);
/* 53:57 */     localJSONObject.put("shareAddress", Boolean.valueOf(Nxt.shareMyAddress));
/* 54:   */     
/* 55:59 */     localJSONObject.put("cumulativeDifficulty", Blockchain.getLastBlock().getCumulativeDifficulty().toString());
/* 56:   */     
/* 57:61 */     return localJSONObject;
/* 58:   */   }
/* 59:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.GetInfo
 * JD-Core Version:    0.7.0.1
 */