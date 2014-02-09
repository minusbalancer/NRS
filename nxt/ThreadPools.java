/*  1:   */ package nxt;
/*  2:   */ 
/*  3:   */ import java.util.concurrent.Callable;
/*  4:   */ import java.util.concurrent.ExecutorService;
/*  5:   */ import java.util.concurrent.Executors;
/*  6:   */ import java.util.concurrent.Future;
/*  7:   */ import java.util.concurrent.ScheduledExecutorService;
/*  8:   */ import java.util.concurrent.TimeUnit;
/*  9:   */ import nxt.peer.Peer;
/* 10:   */ import nxt.util.Logger;
/* 11:   */ import org.json.simple.JSONObject;
/* 12:   */ import org.json.simple.JSONStreamAware;
/* 13:   */ 
/* 14:   */ public final class ThreadPools
/* 15:   */ {
/* 16:17 */   private static final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(8);
/* 17:18 */   private static final ExecutorService sendToPeersService = Executors.newFixedThreadPool(10);
/* 18:   */   
/* 19:   */   public static Future<JSONObject> sendInParallel(Peer paramPeer, final JSONStreamAware paramJSONStreamAware)
/* 20:   */   {
/* 21:21 */     sendToPeersService.submit(new Callable()
/* 22:   */     {
/* 23:   */       public JSONObject call()
/* 24:   */       {
/* 25:24 */         return this.val$peer.send(paramJSONStreamAware);
/* 26:   */       }
/* 27:   */     });
/* 28:   */   }
/* 29:   */   
/* 30:   */   static void start()
/* 31:   */   {
/* 32:31 */     scheduledThreadPool.scheduleWithFixedDelay(Peer.peerConnectingThread, 0L, 5L, TimeUnit.SECONDS);
/* 33:   */     
/* 34:33 */     scheduledThreadPool.scheduleWithFixedDelay(Peer.peerUnBlacklistingThread, 0L, 1L, TimeUnit.SECONDS);
/* 35:   */     
/* 36:35 */     scheduledThreadPool.scheduleWithFixedDelay(Peer.getMorePeersThread, 0L, 5L, TimeUnit.SECONDS);
/* 37:   */     
/* 38:37 */     scheduledThreadPool.scheduleWithFixedDelay(Blockchain.processTransactionsThread, 0L, 5L, TimeUnit.SECONDS);
/* 39:   */     
/* 40:39 */     scheduledThreadPool.scheduleWithFixedDelay(Blockchain.removeUnconfirmedTransactionsThread, 0L, 1L, TimeUnit.SECONDS);
/* 41:   */     
/* 42:41 */     scheduledThreadPool.scheduleWithFixedDelay(Blockchain.getMoreBlocksThread, 0L, 1L, TimeUnit.SECONDS);
/* 43:   */     
/* 44:43 */     scheduledThreadPool.scheduleWithFixedDelay(Blockchain.generateBlockThread, 0L, 1L, TimeUnit.SECONDS);
/* 45:   */     
/* 46:45 */     scheduledThreadPool.scheduleWithFixedDelay(Blockchain.rebroadcastTransactionsThread, 0L, 60L, TimeUnit.SECONDS);
/* 47:   */   }
/* 48:   */   
/* 49:   */   static void shutdown()
/* 50:   */   {
/* 51:50 */     shutdownExecutor(scheduledThreadPool);
/* 52:51 */     shutdownExecutor(sendToPeersService);
/* 53:   */   }
/* 54:   */   
/* 55:   */   private static void shutdownExecutor(ExecutorService paramExecutorService)
/* 56:   */   {
/* 57:55 */     paramExecutorService.shutdown();
/* 58:   */     try
/* 59:   */     {
/* 60:57 */       paramExecutorService.awaitTermination(10L, TimeUnit.SECONDS);
/* 61:   */     }
/* 62:   */     catch (InterruptedException localInterruptedException)
/* 63:   */     {
/* 64:59 */       Thread.currentThread().interrupt();
/* 65:   */     }
/* 66:61 */     if (!paramExecutorService.isTerminated())
/* 67:   */     {
/* 68:62 */       Logger.logMessage("some threads didn't terminate, forcing shutdown");
/* 69:63 */       paramExecutorService.shutdownNow();
/* 70:   */     }
/* 71:   */   }
/* 72:   */ }


/* Location:           D:\Downloads\nxt-client-0.6.1\nxt\webapps\root\WEB-INF\classes\
 * Qualified Name:     nxt.ThreadPools
 * JD-Core Version:    0.7.0.1
 */