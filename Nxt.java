/*   1:    */ import java.io.ObjectStreamException;
/*   2:    */ import java.io.Serializable;
/*   3:    */ import java.math.BigInteger;
/*   4:    */ import nxt.Attachment;
/*   5:    */ import nxt.Attachment.ColoredCoinsAskOrderCancellation;
/*   6:    */ import nxt.Attachment.ColoredCoinsAskOrderPlacement;
/*   7:    */ import nxt.Attachment.ColoredCoinsAssetIssuance;
/*   8:    */ import nxt.Attachment.ColoredCoinsAssetTransfer;
/*   9:    */ import nxt.Attachment.ColoredCoinsBidOrderCancellation;
/*  10:    */ import nxt.Attachment.ColoredCoinsBidOrderPlacement;
/*  11:    */ import nxt.Attachment.MessagingAliasAssignment;
/*  12:    */ import nxt.Attachment.MessagingArbitraryMessage;
/*  13:    */ import nxt.Block;
/*  14:    */ import nxt.NxtException.ValidationException;
/*  15:    */ import nxt.Transaction;
/*  16:    */ import nxt.util.Convert;
/*  17:    */ 
/*  18:    */ final class Nxt
/*  19:    */ {
/*  20:    */   static class Transaction
/*  21:    */     implements Serializable
/*  22:    */   {
/*  23:    */     static final long serialVersionUID = 0L;
/*  24:    */     byte type;
/*  25:    */     byte subtype;
/*  26:    */     int timestamp;
/*  27:    */     short deadline;
/*  28:    */     byte[] senderPublicKey;
/*  29:    */     long recipient;
/*  30:    */     int amount;
/*  31:    */     int fee;
/*  32:    */     long referencedTransaction;
/*  33:    */     byte[] signature;
/*  34:    */     Attachment attachment;
/*  35:    */     int index;
/*  36:    */     long block;
/*  37:    */     int height;
/*  38:    */     
/*  39:    */     public Object readResolve()
/*  40:    */       throws ObjectStreamException
/*  41:    */     {
/*  42:    */       try
/*  43:    */       {
/*  44: 32 */         Transaction localTransaction = this.attachment != null ? Transaction.newTransaction(this.timestamp, this.deadline, this.senderPublicKey, Long.valueOf(this.recipient), this.amount, this.fee, Convert.zeroToNull(this.referencedTransaction), this.attachment) : Transaction.newTransaction(this.timestamp, this.deadline, this.senderPublicKey, Long.valueOf(this.recipient), this.amount, this.fee, Convert.zeroToNull(this.referencedTransaction));
/*  45:    */         
/*  46:    */ 
/*  47:    */ 
/*  48:    */ 
/*  49: 37 */         localTransaction.signature = this.signature;
/*  50: 38 */         localTransaction.index = this.index;
/*  51: 39 */         localTransaction.blockId = Convert.zeroToNull(this.block);
/*  52: 40 */         localTransaction.height = this.height;
/*  53: 41 */         return localTransaction;
/*  54:    */       }
/*  55:    */       catch (NxtException.ValidationException localValidationException)
/*  56:    */       {
/*  57: 43 */         throw new RuntimeException(localValidationException.getMessage(), localValidationException);
/*  58:    */       }
/*  59:    */     }
/*  60:    */     
/*  61:    */     public static abstract interface Attachment {}
/*  62:    */     
/*  63:    */     static class MessagingArbitraryMessageAttachment
/*  64:    */       implements Nxt.Transaction.Attachment, Serializable
/*  65:    */     {
/*  66:    */       static final long serialVersionUID = 0L;
/*  67:    */       byte[] message;
/*  68:    */       
/*  69:    */       public Object readResolve()
/*  70:    */         throws ObjectStreamException
/*  71:    */       {
/*  72: 59 */         return new Attachment.MessagingArbitraryMessage(this.message);
/*  73:    */       }
/*  74:    */     }
/*  75:    */     
/*  76:    */     static class MessagingAliasAssignmentAttachment
/*  77:    */       implements Nxt.Transaction.Attachment, Serializable
/*  78:    */     {
/*  79:    */       static final long serialVersionUID = 0L;
/*  80:    */       String alias;
/*  81:    */       String uri;
/*  82:    */       
/*  83:    */       public Object readResolve()
/*  84:    */         throws ObjectStreamException
/*  85:    */       {
/*  86: 72 */         return new Attachment.MessagingAliasAssignment(this.alias, this.uri);
/*  87:    */       }
/*  88:    */     }
/*  89:    */     
/*  90:    */     static class ColoredCoinsAssetIssuanceAttachment
/*  91:    */       implements Nxt.Transaction.Attachment, Serializable
/*  92:    */     {
/*  93:    */       static final long serialVersionUID = 0L;
/*  94:    */       String name;
/*  95:    */       String description;
/*  96:    */       int quantity;
/*  97:    */       
/*  98:    */       public Object readResolve()
/*  99:    */         throws ObjectStreamException
/* 100:    */       {
/* 101: 85 */         return new Attachment.ColoredCoinsAssetIssuance(this.name, this.description, this.quantity);
/* 102:    */       }
/* 103:    */     }
/* 104:    */     
/* 105:    */     static class ColoredCoinsAssetTransferAttachment
/* 106:    */       implements Nxt.Transaction.Attachment, Serializable
/* 107:    */     {
/* 108:    */       static final long serialVersionUID = 0L;
/* 109:    */       long asset;
/* 110:    */       int quantity;
/* 111:    */       
/* 112:    */       public Object readResolve()
/* 113:    */         throws ObjectStreamException
/* 114:    */       {
/* 115: 99 */         return new Attachment.ColoredCoinsAssetTransfer(Convert.zeroToNull(this.asset), this.quantity);
/* 116:    */       }
/* 117:    */     }
/* 118:    */     
/* 119:    */     static class ColoredCoinsAskOrderPlacementAttachment
/* 120:    */       implements Nxt.Transaction.Attachment, Serializable
/* 121:    */     {
/* 122:    */       static final long serialVersionUID = 0L;
/* 123:    */       long asset;
/* 124:    */       int quantity;
/* 125:    */       long price;
/* 126:    */       
/* 127:    */       public Object readResolve()
/* 128:    */         throws ObjectStreamException
/* 129:    */       {
/* 130:114 */         return new Attachment.ColoredCoinsAskOrderPlacement(Convert.zeroToNull(this.asset), this.quantity, this.price);
/* 131:    */       }
/* 132:    */     }
/* 133:    */     
/* 134:    */     static class ColoredCoinsBidOrderPlacementAttachment
/* 135:    */       implements Nxt.Transaction.Attachment, Serializable
/* 136:    */     {
/* 137:    */       static final long serialVersionUID = 0L;
/* 138:    */       long asset;
/* 139:    */       int quantity;
/* 140:    */       long price;
/* 141:    */       
/* 142:    */       public Object readResolve()
/* 143:    */         throws ObjectStreamException
/* 144:    */       {
/* 145:129 */         return new Attachment.ColoredCoinsBidOrderPlacement(Convert.zeroToNull(this.asset), this.quantity, this.price);
/* 146:    */       }
/* 147:    */     }
/* 148:    */     
/* 149:    */     static class ColoredCoinsAskOrderCancellationAttachment
/* 150:    */       implements Nxt.Transaction.Attachment, Serializable
/* 151:    */     {
/* 152:    */       static final long serialVersionUID = 0L;
/* 153:    */       long order;
/* 154:    */       
/* 155:    */       public Object readResolve()
/* 156:    */         throws ObjectStreamException
/* 157:    */       {
/* 158:142 */         return new Attachment.ColoredCoinsAskOrderCancellation(Convert.zeroToNull(this.order));
/* 159:    */       }
/* 160:    */     }
/* 161:    */     
/* 162:    */     static class ColoredCoinsBidOrderCancellationAttachment
/* 163:    */       implements Nxt.Transaction.Attachment, Serializable
/* 164:    */     {
/* 165:    */       static final long serialVersionUID = 0L;
/* 166:    */       long order;
/* 167:    */       
/* 168:    */       public Object readResolve()
/* 169:    */         throws ObjectStreamException
/* 170:    */       {
/* 171:155 */         return new Attachment.ColoredCoinsBidOrderCancellation(Convert.zeroToNull(this.order));
/* 172:    */       }
/* 173:    */     }
/* 174:    */   }
/* 175:    */   
/* 176:    */   static class Block
/* 177:    */     implements Serializable
/* 178:    */   {
/* 179:    */     static final long serialVersionUID = 0L;
/* 180:165 */     static final long[] emptyLong = new long[0];
/* 181:    */     int version;
/* 182:    */     int timestamp;
/* 183:    */     long previousBlock;
/* 184:    */     int totalAmount;
/* 185:    */     int totalFee;
/* 186:    */     int payloadLength;
/* 187:    */     byte[] payloadHash;
/* 188:    */     byte[] generatorPublicKey;
/* 189:    */     byte[] generationSignature;
/* 190:    */     byte[] blockSignature;
/* 191:    */     byte[] previousBlockHash;
/* 192:    */     int index;
/* 193:    */     long[] transactions;
/* 194:    */     long baseTarget;
/* 195:    */     int height;
/* 196:    */     long nextBlock;
/* 197:    */     BigInteger cumulativeDifficulty;
/* 198:    */     
/* 199:    */     public Object readResolve()
/* 200:    */       throws ObjectStreamException
/* 201:    */     {
/* 202:    */       try
/* 203:    */       {
/* 204:189 */         Block localBlock = new Block(this.version, this.timestamp, Convert.zeroToNull(this.previousBlock), this.transactions.length, this.totalAmount, this.totalFee, this.payloadLength, this.payloadHash, this.generatorPublicKey, this.generationSignature, this.blockSignature, this.previousBlockHash);
/* 205:    */         
/* 206:191 */         localBlock.index = this.index;
/* 207:192 */         for (int i = 0; i < this.transactions.length; i++) {
/* 208:193 */           localBlock.getTransactionIds()[i] = Long.valueOf(this.transactions[i]);
/* 209:    */         }
/* 210:195 */         localBlock.baseTarget = this.baseTarget;
/* 211:196 */         localBlock.height = this.height;
/* 212:197 */         localBlock.nextBlockId = Convert.zeroToNull(this.nextBlock);
/* 213:198 */         localBlock.cumulativeDifficulty = this.cumulativeDifficulty;
/* 214:199 */         return localBlock;
/* 215:    */       }
/* 216:    */       catch (NxtException.ValidationException localValidationException)
/* 217:    */       {
/* 218:201 */         throw new RuntimeException(localValidationException.getMessage(), localValidationException);
/* 219:    */       }
/* 220:    */     }
/* 221:    */   }
/* 222:    */ }


/* Location:           D:\Downloads\nxt-client-0.6.2\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     Nxt
 * JD-Core Version:    0.7.0.1
 */