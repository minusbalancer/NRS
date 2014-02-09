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
/* 14:   */   public JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
/* 15:   */   {
/* 16:17 */     JSONObject localJSONObject = new JSONObject();
/* 17:19 */     if (paramPeer != null)
/* 18:   */     {
/* 19:20 */       String str1 = (String)paramJSONObject.get("announcedAddress");
/* 20:21 */       if (str1 != null)
/* 21:   */       {
/* 22:22 */         str1 = str1.trim();
/* 23:23 */         if (str1.length() > 0) {
/* 24:25 */           paramPeer.setAnnouncedAddress(str1);
/* 25:   */         }
/* 26:   */       }
/* 27:29 */       String str2 = (String)paramJSONObject.get("application");
/* 28:30 */       if (str2 == null)
/* 29:   */       {
/* 30:32 */         str2 = "?";
/* 31:   */       }
/* 32:   */       else
/* 33:   */       {
/* 34:36 */         str2 = str2.trim();
/* 35:37 */         if (str2.length() > 20) {
/* 36:39 */           str2 = str2.substring(0, 20) + "...";
/* 37:   */         }
/* 38:   */       }
/* 39:44 */       paramPeer.setApplication(str2);
/* 40:   */       
/* 41:46 */       String str3 = (String)paramJSONObject.get("version");
/* 42:47 */       if (str3 == null)
/* 43:   */       {
/* 44:49 */         str3 = "?";
/* 45:   */       }
/* 46:   */       else
/* 47:   */       {
/* 48:53 */         str3 = str3.trim();
/* 49:54 */         if (str3.length() > 10) {
/* 50:56 */           str3 = str3.substring(0, 10) + "...";
/* 51:   */         }
/* 52:   */       }
/* 53:61 */       paramPeer.setVersion(str3);
/* 54:   */       
/* 55:63 */       String str4 = (String)paramJSONObject.get("platform");
/* 56:64 */       if (str4 == null)
/* 57:   */       {
/* 58:66 */         str4 = "?";
/* 59:   */       }
/* 60:   */       else
/* 61:   */       {
/* 62:70 */         str4 = str4.trim();
/* 63:71 */         if (str4.length() > 10) {
/* 64:73 */           str4 = str4.substring(0, 10) + "...";
/* 65:   */         }
/* 66:   */       }
/* 67:78 */       paramPeer.setPlatform(str4);
/* 68:   */       
/* 69:80 */       paramPeer.setShareAddress(Boolean.TRUE.equals(paramJSONObject.get("shareAddress")));
/* 70:   */     }
/* 71:84 */     if ((Nxt.myHallmark != null) && (Nxt.myHallmark.length() > 0)) {
/* 72:86 */       localJSONObject.put("hallmark", Nxt.myHallmark);
/* 73:   */     }
/* 74:89 */     localJSONObject.put("application", "NRS");
/* 75:90 */     localJSONObject.put("version", "0.7.0e");
/* 76:91 */     localJSONObject.put("platform", Nxt.myPlatform);
/* 77:92 */     localJSONObject.put("shareAddress", Boolean.valueOf(Nxt.shareMyAddress));
/* 78:   */     
/* 79:94 */     localJSONObject.put("cumulativeDifficulty", Blockchain.getLastBlock().getCumulativeDifficulty().toString());
/* 80:   */     
/* 81:96 */     return localJSONObject;
/* 82:   */   }
/* 83:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.GetInfo
 * JD-Core Version:    0.7.0.1
 */