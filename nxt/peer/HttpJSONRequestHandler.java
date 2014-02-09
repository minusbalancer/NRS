/*   1:    */ package nxt.peer;
/*   2:    */ 
/*   3:    */ import java.io.BufferedReader;
/*   4:    */ import java.io.BufferedWriter;
/*   5:    */ import java.io.IOException;
/*   6:    */ import java.io.InputStream;
/*   7:    */ import java.io.InputStreamReader;
/*   8:    */ import java.io.OutputStreamWriter;
/*   9:    */ import java.io.Reader;
/*  10:    */ import java.io.Writer;
/*  11:    */ import java.util.Collections;
/*  12:    */ import java.util.HashMap;
/*  13:    */ import java.util.Map;
/*  14:    */ import javax.servlet.ServletException;
/*  15:    */ import javax.servlet.http.HttpServletRequest;
/*  16:    */ import javax.servlet.http.HttpServletResponse;
/*  17:    */ import nxt.util.CountingInputStream;
/*  18:    */ import nxt.util.CountingOutputStream;
/*  19:    */ import nxt.util.JSON;
/*  20:    */ import nxt.util.Logger;
/*  21:    */ import org.json.simple.JSONObject;
/*  22:    */ import org.json.simple.JSONStreamAware;
/*  23:    */ import org.json.simple.JSONValue;
/*  24:    */ 
/*  25:    */ public abstract class HttpJSONRequestHandler
/*  26:    */ {
/*  27:    */   private static final Map<String, HttpJSONRequestHandler> jsonRequestHandlers;
/*  28:    */   private static final JSONStreamAware UNSUPPORTED_REQUEST_TYPE;
/*  29:    */   private static final JSONStreamAware UNSUPPORTED_PROTOCOL;
/*  30:    */   
/*  31:    */   static
/*  32:    */   {
/*  33: 31 */     Object localObject = new HashMap();
/*  34:    */     
/*  35: 33 */     ((Map)localObject).put("getCumulativeDifficulty", GetCumulativeDifficulty.instance);
/*  36: 34 */     ((Map)localObject).put("getInfo", GetInfo.instance);
/*  37: 35 */     ((Map)localObject).put("getMilestoneBlockIds", GetMilestoneBlockIds.instance);
/*  38: 36 */     ((Map)localObject).put("getNextBlockIds", GetNextBlockIds.instance);
/*  39: 37 */     ((Map)localObject).put("getNextBlocks", GetNextBlocks.instance);
/*  40: 38 */     ((Map)localObject).put("getPeers", GetPeers.instance);
/*  41: 39 */     ((Map)localObject).put("getUnconfirmedTransactions", GetUnconfirmedTransactions.instance);
/*  42: 40 */     ((Map)localObject).put("processBlock", ProcessBlock.instance);
/*  43: 41 */     ((Map)localObject).put("processTransactions", ProcessTransactions.instance);
/*  44:    */     
/*  45: 43 */     jsonRequestHandlers = Collections.unmodifiableMap((Map)localObject);
/*  46:    */     
/*  47:    */ 
/*  48:    */ 
/*  49:    */ 
/*  50: 48 */     localObject = new JSONObject();
/*  51: 49 */     ((JSONObject)localObject).put("error", "Unsupported request type!");
/*  52: 50 */     UNSUPPORTED_REQUEST_TYPE = JSON.prepare((JSONObject)localObject);
/*  53:    */     
/*  54:    */ 
/*  55:    */ 
/*  56:    */ 
/*  57: 55 */     localObject = new JSONObject();
/*  58: 56 */     ((JSONObject)localObject).put("error", "Unsupported protocol!");
/*  59: 57 */     UNSUPPORTED_PROTOCOL = JSON.prepare((JSONObject)localObject);
/*  60:    */   }
/*  61:    */   
/*  62:    */   abstract JSONStreamAware processJSONRequest(JSONObject paramJSONObject, Peer paramPeer);
/*  63:    */   
/*  64:    */   public static void process(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
/*  65:    */     throws ServletException, IOException
/*  66:    */   {
/*  67: 62 */     Peer localPeer = null;
/*  68:    */     Object localObject1;
/*  69:    */     try
/*  70:    */     {
/*  71: 67 */       localObject2 = new CountingInputStream(paramHttpServletRequest.getInputStream());
/*  72: 68 */       localObject3 = new BufferedReader(new InputStreamReader((InputStream)localObject2, "UTF-8"));Object localObject4 = null;
/*  73:    */       JSONObject localJSONObject;
/*  74:    */       try
/*  75:    */       {
/*  76: 69 */         localJSONObject = (JSONObject)JSONValue.parse((Reader)localObject3);
/*  77:    */       }
/*  78:    */       catch (Throwable localThrowable4)
/*  79:    */       {
/*  80: 68 */         localObject4 = localThrowable4;throw localThrowable4;
/*  81:    */       }
/*  82:    */       finally
/*  83:    */       {
/*  84: 70 */         if (localObject3 != null) {
/*  85: 70 */           if (localObject4 != null) {
/*  86:    */             try
/*  87:    */             {
/*  88: 70 */               ((Reader)localObject3).close();
/*  89:    */             }
/*  90:    */             catch (Throwable localThrowable5)
/*  91:    */             {
/*  92: 70 */               localObject4.addSuppressed(localThrowable5);
/*  93:    */             }
/*  94:    */           } else {
/*  95: 70 */             ((Reader)localObject3).close();
/*  96:    */           }
/*  97:    */         }
/*  98:    */       }
/*  99: 71 */       if (localJSONObject == null) {
/* 100: 72 */         return;
/* 101:    */       }
/* 102: 75 */       localPeer = Peer.addPeer(paramHttpServletRequest.getRemoteHost(), "");
/* 103: 76 */       if (localPeer != null)
/* 104:    */       {
/* 105: 77 */         if (localPeer.isBlacklisted()) {
/* 106: 78 */           return;
/* 107:    */         }
/* 108: 80 */         if (localPeer.getState() == Peer.State.DISCONNECTED) {
/* 109: 81 */           localPeer.setState(Peer.State.CONNECTED);
/* 110:    */         }
/* 111: 83 */         localPeer.updateDownloadedVolume(((CountingInputStream)localObject2).getCount());
/* 112: 84 */         if (localPeer.analyzeHallmark(paramHttpServletRequest.getRemoteHost(), (String)localJSONObject.get("hallmark"))) {
/* 113: 85 */           localPeer.setState(Peer.State.CONNECTED);
/* 114:    */         }
/* 115:    */       }
/* 116: 89 */       if ((localJSONObject.get("protocol") != null) && (((Number)localJSONObject.get("protocol")).intValue() == 1))
/* 117:    */       {
/* 118: 90 */         localObject3 = (HttpJSONRequestHandler)jsonRequestHandlers.get((String)localJSONObject.get("requestType"));
/* 119: 91 */         if (localObject3 != null) {
/* 120: 92 */           localObject1 = ((HttpJSONRequestHandler)localObject3).processJSONRequest(localJSONObject, localPeer);
/* 121:    */         } else {
/* 122: 94 */           localObject1 = UNSUPPORTED_REQUEST_TYPE;
/* 123:    */         }
/* 124:    */       }
/* 125:    */       else
/* 126:    */       {
/* 127: 97 */         Logger.logDebugMessage("Unsupported protocol " + localJSONObject.get("protocol"));
/* 128: 98 */         localObject1 = UNSUPPORTED_PROTOCOL;
/* 129:    */       }
/* 130:    */     }
/* 131:    */     catch (RuntimeException localRuntimeException)
/* 132:    */     {
/* 133:102 */       Logger.logDebugMessage("Error processing POST request", localRuntimeException);
/* 134:103 */       localObject2 = new JSONObject();
/* 135:104 */       ((JSONObject)localObject2).put("error", localRuntimeException.toString());
/* 136:105 */       localObject1 = localObject2;
/* 137:    */     }
/* 138:108 */     paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
/* 139:109 */     CountingOutputStream localCountingOutputStream = new CountingOutputStream(paramHttpServletResponse.getOutputStream());
/* 140:110 */     Object localObject2 = new BufferedWriter(new OutputStreamWriter(localCountingOutputStream, "UTF-8"));Object localObject3 = null;
/* 141:    */     try
/* 142:    */     {
/* 143:111 */       ((JSONStreamAware)localObject1).writeJSONString((Writer)localObject2);
/* 144:    */     }
/* 145:    */     catch (Throwable localThrowable2)
/* 146:    */     {
/* 147:110 */       localObject3 = localThrowable2;throw localThrowable2;
/* 148:    */     }
/* 149:    */     finally
/* 150:    */     {
/* 151:112 */       if (localObject2 != null) {
/* 152:112 */         if (localObject3 != null) {
/* 153:    */           try
/* 154:    */           {
/* 155:112 */             ((Writer)localObject2).close();
/* 156:    */           }
/* 157:    */           catch (Throwable localThrowable6)
/* 158:    */           {
/* 159:112 */             ((Throwable)localObject3).addSuppressed(localThrowable6);
/* 160:    */           }
/* 161:    */         } else {
/* 162:112 */           ((Writer)localObject2).close();
/* 163:    */         }
/* 164:    */       }
/* 165:    */     }
/* 166:114 */     if (localPeer != null) {
/* 167:115 */       localPeer.updateUploadedVolume(localCountingOutputStream.getCount());
/* 168:    */     }
/* 169:    */   }
/* 170:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.peer.HttpJSONRequestHandler
 * JD-Core Version:    0.7.0.1
 */