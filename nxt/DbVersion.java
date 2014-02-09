/*  1:   */ package nxt;
/*  2:   */ 
/*  3:   */ import java.sql.Connection;
/*  4:   */ import java.sql.ResultSet;
/*  5:   */ import java.sql.SQLException;
/*  6:   */ import java.sql.Statement;
/*  7:   */ import nxt.util.Logger;
/*  8:   */ 
/*  9:   */ final class DbVersion
/* 10:   */ {
/* 11:   */   static void init()
/* 12:   */   {
/* 13:   */     try
/* 14:   */     {
/* 15:13 */       Connection localConnection = Db.getConnection();Object localObject1 = null;
/* 16:   */       try
/* 17:   */       {
/* 18:13 */         Statement localStatement = localConnection.createStatement();Object localObject2 = null;
/* 19:   */         try
/* 20:   */         {
/* 21:14 */           int i = 1;
/* 22:   */           try
/* 23:   */           {
/* 24:16 */             ResultSet localResultSet = localStatement.executeQuery("SELECT next_update FROM version");
/* 25:17 */             if (!localResultSet.next()) {
/* 26:18 */               throw new RuntimeException("Invalid version table");
/* 27:   */             }
/* 28:20 */             i = localResultSet.getInt("next_update");
/* 29:21 */             if (!localResultSet.isLast()) {
/* 30:22 */               throw new RuntimeException("Invalid version table");
/* 31:   */             }
/* 32:24 */             localResultSet.close();
/* 33:25 */             Logger.logMessage("Database is at level " + (i - 1));
/* 34:   */           }
/* 35:   */           catch (SQLException localSQLException2)
/* 36:   */           {
/* 37:27 */             Logger.logMessage("Initializing an empty database");
/* 38:28 */             localStatement.executeUpdate("CREATE TABLE version (next_update INT NOT NULL)");
/* 39:29 */             localStatement.executeUpdate("INSERT INTO version VALUES (1)");
/* 40:   */           }
/* 41:31 */           i = update(i);
/* 42:32 */           localStatement.executeUpdate("UPDATE version SET next_update=" + i);
/* 43:33 */           Logger.logMessage("Updated database is at level " + (i - 1));
/* 44:34 */           localConnection.commit();
/* 45:   */         }
/* 46:   */         catch (Throwable localThrowable4)
/* 47:   */         {
/* 48:13 */           localObject2 = localThrowable4;throw localThrowable4;
/* 49:   */         }
/* 50:   */         finally {}
/* 51:   */       }
/* 52:   */       catch (Throwable localThrowable2)
/* 53:   */       {
/* 54:13 */         localObject1 = localThrowable2;throw localThrowable2;
/* 55:   */       }
/* 56:   */       finally
/* 57:   */       {
/* 58:35 */         if (localConnection != null) {
/* 59:35 */           if (localObject1 != null) {
/* 60:   */             try
/* 61:   */             {
/* 62:35 */               localConnection.close();
/* 63:   */             }
/* 64:   */             catch (Throwable localThrowable6)
/* 65:   */             {
/* 66:35 */               localObject1.addSuppressed(localThrowable6);
/* 67:   */             }
/* 68:   */           } else {
/* 69:35 */             localConnection.close();
/* 70:   */           }
/* 71:   */         }
/* 72:   */       }
/* 73:   */     }
/* 74:   */     catch (SQLException localSQLException1)
/* 75:   */     {
/* 76:36 */       throw new RuntimeException(localSQLException1.toString(), localSQLException1);
/* 77:   */     }
/* 78:   */   }
/* 79:   */   
/* 80:   */   private static void apply(String paramString)
/* 81:   */   {
/* 82:   */     try
/* 83:   */     {
/* 84:42 */       Connection localConnection = Db.getConnection();Object localObject1 = null;
/* 85:   */       try
/* 86:   */       {
/* 87:42 */         Statement localStatement = localConnection.createStatement();Object localObject2 = null;
/* 88:   */         try
/* 89:   */         {
/* 90:   */           try
/* 91:   */           {
/* 92:44 */             Logger.logDebugMessage("Will apply sql:\n" + paramString);
/* 93:45 */             localStatement.executeUpdate(paramString);
/* 94:46 */             localConnection.commit();
/* 95:   */           }
/* 96:   */           catch (SQLException localSQLException2)
/* 97:   */           {
/* 98:48 */             localConnection.rollback();
/* 99:49 */             throw localSQLException2;
/* :0:   */           }
/* :1:   */         }
/* :2:   */         catch (Throwable localThrowable4)
/* :3:   */         {
/* :4:42 */           localObject2 = localThrowable4;throw localThrowable4;
/* :5:   */         }
/* :6:   */         finally {}
/* :7:   */       }
/* :8:   */       catch (Throwable localThrowable2)
/* :9:   */       {
/* ;0:42 */         localObject1 = localThrowable2;throw localThrowable2;
/* ;1:   */       }
/* ;2:   */       finally
/* ;3:   */       {
/* ;4:51 */         if (localConnection != null) {
/* ;5:51 */           if (localObject1 != null) {
/* ;6:   */             try
/* ;7:   */             {
/* ;8:51 */               localConnection.close();
/* ;9:   */             }
/* <0:   */             catch (Throwable localThrowable6)
/* <1:   */             {
/* <2:51 */               localObject1.addSuppressed(localThrowable6);
/* <3:   */             }
/* <4:   */           } else {
/* <5:51 */             localConnection.close();
/* <6:   */           }
/* <7:   */         }
/* <8:   */       }
/* <9:   */     }
/* =0:   */     catch (SQLException localSQLException1)
/* =1:   */     {
/* =2:52 */       throw new RuntimeException("Database error executing " + paramString, localSQLException1);
/* =3:   */     }
/* =4:   */   }
/* =5:   */   
/* =6:   */   private static int update(int paramInt)
/* =7:   */   {
/* =8:57 */     switch (paramInt)
/* =9:   */     {
/* >0:   */     case 1: 
/* >1:59 */       apply("CREATE TABLE IF NOT EXISTS block (db_id INT IDENTITY, id BIGINT NOT NULL, version INT NOT NULL, timestamp INT NOT NULL, previous_block_id BIGINT, FOREIGN KEY (previous_block_id) REFERENCES block (id) ON DELETE CASCADE, total_amount INT NOT NULL, total_fee INT NOT NULL, payload_length INT NOT NULL, generator_public_key BINARY(32) NOT NULL, previous_block_hash BINARY(32), cumulative_difficulty VARBINARY NOT NULL, base_target BIGINT NOT NULL, next_block_id BIGINT, FOREIGN KEY (next_block_id) REFERENCES block (id) ON DELETE SET NULL, index INT NOT NULL, height INT NOT NULL, generation_signature BINARY(64) NOT NULL, block_signature BINARY(64) NOT NULL, payload_hash BINARY(32) NOT NULL, generator_account_id BIGINT NOT NULL)");
/* >2:   */     case 2: 
/* >3:68 */       apply("CREATE UNIQUE INDEX IF NOT EXISTS block_id_idx ON block (id)");
/* >4:   */     case 3: 
/* >5:70 */       apply("CREATE TABLE IF NOT EXISTS transaction (db_id INT IDENTITY, id BIGINT NOT NULL, deadline SMALLINT NOT NULL, sender_public_key BINARY(32) NOT NULL, recipient_id BIGINT NOT NULL, amount INT NOT NULL, fee INT NOT NULL, referenced_transaction_id BIGINT, index INT NOT NULL, height INT NOT NULL, block_id BIGINT NOT NULL, FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE, signature BINARY(64) NOT NULL, timestamp INT NOT NULL, type TINYINT NOT NULL, subtype TINYINT NOT NULL, sender_account_id BIGINT NOT NULL, attachment OTHER)");
/* >6:   */     case 4: 
/* >7:77 */       apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_id_idx ON transaction (id)");
/* >8:   */     case 5: 
/* >9:79 */       apply("CREATE UNIQUE INDEX IF NOT EXISTS block_height_idx ON block (height)");
/* ?0:   */     case 6: 
/* ?1:81 */       apply("CREATE INDEX IF NOT EXISTS transaction_timestamp_idx ON transaction (timestamp)");
/* ?2:   */     case 7: 
/* ?3:83 */       return 7;
/* ?4:   */     }
/* ?5:85 */     throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");
/* ?6:   */   }
/* ?7:   */ }


/* Location:           D:\Downloads\nxt-client-0.7.0e\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.DbVersion
 * JD-Core Version:    0.7.0.1
 */