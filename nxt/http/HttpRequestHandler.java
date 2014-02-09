/*   1:    */ package nxt.http;
/*   2:    */ 
/*   3:    */ import java.io.IOException;
/*   4:    */ import java.io.Writer;
/*   5:    */ import java.util.Collections;
/*   6:    */ import java.util.HashMap;
/*   7:    */ import java.util.Map;
/*   8:    */ import java.util.Set;
/*   9:    */ import javax.servlet.ServletException;
/*  10:    */ import javax.servlet.http.HttpServletRequest;
/*  11:    */ import javax.servlet.http.HttpServletResponse;
/*  12:    */ import nxt.Nxt;
/*  13:    */ import nxt.NxtException;
/*  14:    */ import org.json.simple.JSONStreamAware;
/*  15:    */ 
/*  16:    */ public abstract class HttpRequestHandler
/*  17:    */ {
/*  18:    */   private static final Map<String, HttpRequestHandler> httpGetHandlers;
/*  19:    */   
/*  20:    */   static
/*  21:    */   {
/*  22: 25 */     HashMap localHashMap = new HashMap();
/*  23:    */     
/*  24: 27 */     localHashMap.put("assignAlias", AssignAlias.instance);
/*  25: 28 */     localHashMap.put("broadcastTransaction", BroadcastTransaction.instance);
/*  26: 29 */     localHashMap.put("decodeHallmark", DecodeHallmark.instance);
/*  27: 30 */     localHashMap.put("decodeToken", DecodeToken.instance);
/*  28: 31 */     localHashMap.put("getAccount", GetAccount.instance);
/*  29: 32 */     localHashMap.put("getAccountBlockIds", GetAccountBlockIds.instance);
/*  30: 33 */     localHashMap.put("getAccountId", GetAccountId.instance);
/*  31: 34 */     localHashMap.put("getAccountPublicKey", GetAccountPublicKey.instance);
/*  32: 35 */     localHashMap.put("getAccountTransactionIds", GetAccountTransactionIds.instance);
/*  33: 36 */     localHashMap.put("getAlias", GetAlias.instance);
/*  34: 37 */     localHashMap.put("getAliasId", GetAliasId.instance);
/*  35: 38 */     localHashMap.put("getAliasIds", GetAliasIds.instance);
/*  36: 39 */     localHashMap.put("getAliasURI", GetAliasURI.instance);
/*  37: 40 */     localHashMap.put("getAsset", GetAsset.instance);
/*  38: 41 */     localHashMap.put("getAssetIds", GetAssetIds.instance);
/*  39: 42 */     localHashMap.put("getBalance", GetBalance.instance);
/*  40: 43 */     localHashMap.put("getBlock", GetBlock.instance);
/*  41: 44 */     localHashMap.put("getConstants", GetConstants.instance);
/*  42: 45 */     localHashMap.put("getGuaranteedBalance", GetGuaranteedBalance.instance);
/*  43: 46 */     localHashMap.put("getMyInfo", GetMyInfo.instance);
/*  44: 47 */     localHashMap.put("getPeer", GetPeer.instance);
/*  45: 48 */     localHashMap.put("getPeers", GetPeers.instance);
/*  46: 49 */     localHashMap.put("getState", GetState.instance);
/*  47: 50 */     localHashMap.put("getTime", GetTime.instance);
/*  48: 51 */     localHashMap.put("getTransaction", GetTransaction.instance);
/*  49: 52 */     localHashMap.put("getTransactionBytes", GetTransactionBytes.instance);
/*  50: 53 */     localHashMap.put("getUnconfirmedTransactionIds", GetUnconfirmedTransactionIds.instance);
/*  51: 54 */     localHashMap.put("getAccountCurrentAskOrderIds", GetAccountCurrentAskOrderIds.instance);
/*  52: 55 */     localHashMap.put("getAccountCurrentBidOrderIds", GetAccountCurrentBidOrderIds.instance);
/*  53: 56 */     localHashMap.put("getAskOrder", GetAskOrder.instance);
/*  54: 57 */     localHashMap.put("getAskOrderIds", GetAskOrderIds.instance);
/*  55: 58 */     localHashMap.put("getBidOrder", GetBidOrder.instance);
/*  56: 59 */     localHashMap.put("getBidOrderIds", GetBidOrderIds.instance);
/*  57: 60 */     localHashMap.put("listAccountAliases", ListAccountAliases.instance);
/*  58: 61 */     localHashMap.put("markHost", MarkHost.instance);
/*  59: 62 */     localHashMap.put("sendMessage", SendMessage.instance);
/*  60: 63 */     localHashMap.put("sendMoney", SendMoney.instance);
/*  61:    */     
/*  62:    */ 
/*  63:    */ 
/*  64:    */ 
/*  65:    */ 
/*  66:    */ 
/*  67:    */ 
/*  68:    */ 
/*  69:    */ 
/*  70: 73 */     httpGetHandlers = Collections.unmodifiableMap(localHashMap);
/*  71:    */   }
/*  72:    */   
/*  73:    */   abstract JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
/*  74:    */     throws NxtException, IOException;
/*  75:    */   
/*  76:    */   public static void process(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
/*  77:    */     throws ServletException, IOException
/*  78:    */   {
/*  79:    */     JSONStreamAware localJSONStreamAware;
/*  80: 80 */     if ((Nxt.allowedBotHosts != null) && (!Nxt.allowedBotHosts.contains(paramHttpServletRequest.getRemoteHost())))
/*  81:    */     {
/*  82: 81 */       localJSONStreamAware = JSONResponses.ERROR_NOT_ALLOWED;
/*  83:    */     }
/*  84:    */     else
/*  85:    */     {
/*  86: 84 */       localObject1 = paramHttpServletRequest.getParameter("requestType");
/*  87: 85 */       if (localObject1 == null)
/*  88:    */       {
/*  89: 86 */         localJSONStreamAware = JSONResponses.ERROR_INCORRECT_REQUEST;
/*  90:    */       }
/*  91:    */       else
/*  92:    */       {
/*  93: 89 */         localObject2 = (HttpRequestHandler)httpGetHandlers.get(localObject1);
/*  94: 90 */         if (localObject2 != null) {
/*  95:    */           try
/*  96:    */           {
/*  97: 92 */             localJSONStreamAware = ((HttpRequestHandler)localObject2).processRequest(paramHttpServletRequest);
/*  98:    */           }
/*  99:    */           catch (NxtException localNxtException)
/* 100:    */           {
/* 101: 94 */             localJSONStreamAware = JSONResponses.ERROR_INCORRECT_REQUEST;
/* 102:    */           }
/* 103:    */         } else {
/* 104: 97 */           localJSONStreamAware = JSONResponses.ERROR_INCORRECT_REQUEST;
/* 105:    */         }
/* 106:    */       }
/* 107:    */     }
/* 108:104 */     paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
/* 109:    */     
/* 110:106 */     Object localObject1 = paramHttpServletResponse.getWriter();Object localObject2 = null;
/* 111:    */     try
/* 112:    */     {
/* 113:107 */       localJSONStreamAware.writeJSONString((Writer)localObject1);
/* 114:    */     }
/* 115:    */     catch (Throwable localThrowable2)
/* 116:    */     {
/* 117:106 */       localObject2 = localThrowable2;throw localThrowable2;
/* 118:    */     }
/* 119:    */     finally
/* 120:    */     {
/* 121:108 */       if (localObject1 != null) {
/* 122:108 */         if (localObject2 != null) {
/* 123:    */           try
/* 124:    */           {
/* 125:108 */             ((Writer)localObject1).close();
/* 126:    */           }
/* 127:    */           catch (Throwable localThrowable3)
/* 128:    */           {
/* 129:108 */             ((Throwable)localObject2).addSuppressed(localThrowable3);
/* 130:    */           }
/* 131:    */         } else {
/* 132:108 */           ((Writer)localObject1).close();
/* 133:    */         }
/* 134:    */       }
/* 135:    */     }
/* 136:    */   }
/* 137:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.http.HttpRequestHandler
 * JD-Core Version:    0.7.0.1
 */