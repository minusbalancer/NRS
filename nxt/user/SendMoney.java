/*   1:    */ package nxt.user;
/*   2:    */ 
/*   3:    */ import java.io.IOException;
/*   4:    */ import javax.servlet.http.HttpServletRequest;
/*   5:    */ import nxt.Account;
/*   6:    */ import nxt.Blockchain;
/*   7:    */ import nxt.NxtException.ValidationException;
/*   8:    */ import nxt.Transaction;
/*   9:    */ import nxt.util.Convert;
/*  10:    */ import org.json.simple.JSONObject;
/*  11:    */ import org.json.simple.JSONStreamAware;
/*  12:    */ 
/*  13:    */ final class SendMoney
/*  14:    */   extends UserRequestHandler
/*  15:    */ {
/*  16: 19 */   static final SendMoney instance = new SendMoney();
/*  17:    */   
/*  18:    */   JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
/*  19:    */     throws NxtException.ValidationException, IOException
/*  20:    */   {
/*  21: 25 */     if (paramUser.getSecretPhrase() == null) {
/*  22: 26 */       return null;
/*  23:    */     }
/*  24: 29 */     String str1 = paramHttpServletRequest.getParameter("recipient");
/*  25: 30 */     String str2 = paramHttpServletRequest.getParameter("amount");
/*  26: 31 */     String str3 = paramHttpServletRequest.getParameter("fee");
/*  27: 32 */     String str4 = paramHttpServletRequest.getParameter("deadline");
/*  28: 33 */     String str5 = paramHttpServletRequest.getParameter("secretPhrase");
/*  29:    */     
/*  30:    */ 
/*  31: 36 */     int i = 0;
/*  32: 37 */     int j = 0;
/*  33: 38 */     short s = 0;
/*  34:    */     Long localLong;
/*  35:    */     try
/*  36:    */     {
/*  37: 42 */       localLong = Convert.parseUnsignedLong(str1);
/*  38: 43 */       if (localLong == null) {
/*  39: 43 */         throw new IllegalArgumentException("invalid recipient");
/*  40:    */       }
/*  41: 44 */       i = Integer.parseInt(str2.trim());
/*  42: 45 */       j = Integer.parseInt(str3.trim());
/*  43: 46 */       s = (short)(int)(Double.parseDouble(str4) * 60.0D);
/*  44:    */     }
/*  45:    */     catch (RuntimeException localRuntimeException)
/*  46:    */     {
/*  47: 50 */       localObject2 = new JSONObject();
/*  48: 51 */       ((JSONObject)localObject2).put("response", "notifyOfIncorrectTransaction");
/*  49: 52 */       ((JSONObject)localObject2).put("message", "One of the fields is filled incorrectly!");
/*  50: 53 */       ((JSONObject)localObject2).put("recipient", str1);
/*  51: 54 */       ((JSONObject)localObject2).put("amount", str2);
/*  52: 55 */       ((JSONObject)localObject2).put("fee", str3);
/*  53: 56 */       ((JSONObject)localObject2).put("deadline", str4);
/*  54:    */       
/*  55: 58 */       return localObject2;
/*  56:    */     }
/*  57: 62 */     if (!paramUser.getSecretPhrase().equals(str5))
/*  58:    */     {
/*  59: 64 */       localObject1 = new JSONObject();
/*  60: 65 */       ((JSONObject)localObject1).put("response", "notifyOfIncorrectTransaction");
/*  61: 66 */       ((JSONObject)localObject1).put("message", "Wrong secret phrase!");
/*  62: 67 */       ((JSONObject)localObject1).put("recipient", str1);
/*  63: 68 */       ((JSONObject)localObject1).put("amount", str2);
/*  64: 69 */       ((JSONObject)localObject1).put("fee", str3);
/*  65: 70 */       ((JSONObject)localObject1).put("deadline", str4);
/*  66:    */       
/*  67: 72 */       return localObject1;
/*  68:    */     }
/*  69: 74 */     if ((i <= 0) || (i > 1000000000L))
/*  70:    */     {
/*  71: 76 */       localObject1 = new JSONObject();
/*  72: 77 */       ((JSONObject)localObject1).put("response", "notifyOfIncorrectTransaction");
/*  73: 78 */       ((JSONObject)localObject1).put("message", "\"Amount\" must be greater than 0!");
/*  74: 79 */       ((JSONObject)localObject1).put("recipient", str1);
/*  75: 80 */       ((JSONObject)localObject1).put("amount", str2);
/*  76: 81 */       ((JSONObject)localObject1).put("fee", str3);
/*  77: 82 */       ((JSONObject)localObject1).put("deadline", str4);
/*  78:    */       
/*  79: 84 */       return localObject1;
/*  80:    */     }
/*  81: 86 */     if ((j <= 0) || (j > 1000000000L))
/*  82:    */     {
/*  83: 88 */       localObject1 = new JSONObject();
/*  84: 89 */       ((JSONObject)localObject1).put("response", "notifyOfIncorrectTransaction");
/*  85: 90 */       ((JSONObject)localObject1).put("message", "\"Fee\" must be greater than 0!");
/*  86: 91 */       ((JSONObject)localObject1).put("recipient", str1);
/*  87: 92 */       ((JSONObject)localObject1).put("amount", str2);
/*  88: 93 */       ((JSONObject)localObject1).put("fee", str3);
/*  89: 94 */       ((JSONObject)localObject1).put("deadline", str4);
/*  90:    */       
/*  91: 96 */       return localObject1;
/*  92:    */     }
/*  93: 98 */     if ((s < 1) || (s > 1440))
/*  94:    */     {
/*  95:100 */       localObject1 = new JSONObject();
/*  96:101 */       ((JSONObject)localObject1).put("response", "notifyOfIncorrectTransaction");
/*  97:102 */       ((JSONObject)localObject1).put("message", "\"Deadline\" must be greater or equal to 1 minute and less than 24 hours!");
/*  98:103 */       ((JSONObject)localObject1).put("recipient", str1);
/*  99:104 */       ((JSONObject)localObject1).put("amount", str2);
/* 100:105 */       ((JSONObject)localObject1).put("fee", str3);
/* 101:106 */       ((JSONObject)localObject1).put("deadline", str4);
/* 102:    */       
/* 103:108 */       return localObject1;
/* 104:    */     }
/* 105:112 */     Object localObject1 = Account.getAccount(paramUser.getPublicKey());
/* 106:113 */     if ((localObject1 == null) || ((i + j) * 100L > ((Account)localObject1).getUnconfirmedBalance()))
/* 107:    */     {
/* 108:115 */       localObject2 = new JSONObject();
/* 109:116 */       ((JSONObject)localObject2).put("response", "notifyOfIncorrectTransaction");
/* 110:117 */       ((JSONObject)localObject2).put("message", "Not enough funds!");
/* 111:118 */       ((JSONObject)localObject2).put("recipient", str1);
/* 112:119 */       ((JSONObject)localObject2).put("amount", str2);
/* 113:120 */       ((JSONObject)localObject2).put("fee", str3);
/* 114:121 */       ((JSONObject)localObject2).put("deadline", str4);
/* 115:    */       
/* 116:123 */       return localObject2;
/* 117:    */     }
/* 118:127 */     Object localObject2 = Transaction.newTransaction(Convert.getEpochTime(), s, paramUser.getPublicKey(), localLong, i, j, null);
/* 119:128 */     ((Transaction)localObject2).sign(paramUser.getSecretPhrase());
/* 120:    */     
/* 121:130 */     Blockchain.broadcast((Transaction)localObject2);
/* 122:    */     
/* 123:132 */     return JSONResponses.NOTIFY_OF_ACCEPTED_TRANSACTION;
/* 124:    */   }
/* 125:    */ }


/* Location:           D:\Downloads\nxt-client-0.7.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.user.SendMoney
 * JD-Core Version:    0.7.0.1
 */