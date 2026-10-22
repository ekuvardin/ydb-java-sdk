package tech.ydb.topic.write.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.SyncWriter;
import tech.ydb.topic.write.WriteAck;

public class TransactionalWriterImplTest {

    @Test
    public void asyncWriterUsesBoundTransaction() throws Exception {
        StubTransaction tx = new StubTransaction(true, new CompletableFuture<>());
        StubAsyncWriter delegate = new StubAsyncWriter();
        Message message = Message.of("data".getBytes());

        AsyncTransactionalWriterImpl writer = new AsyncTransactionalWriterImpl(delegate, tx);

        CompletableFuture<WriteAck> actual = writer.send(message);
        Assert.assertSame(delegate.sendFuture, actual);
        Assert.assertSame(message, delegate.lastMessage);
        Assert.assertNotNull(delegate.lastSettings);
        Assert.assertSame(tx, delegate.lastSettings.getTransaction());
    }

    @Test
    public void asyncWriterRejectsCompletedTransaction() {
        StubTransaction tx = new StubTransaction(true, CompletableFuture.completedFuture(Status.SUCCESS));
        StubAsyncWriter delegate = new StubAsyncWriter();
        AsyncTransactionalWriterImpl writer = new AsyncTransactionalWriterImpl(delegate, tx);

        IllegalStateException ex = Assert.assertThrows(IllegalStateException.class,
                () -> writer.send(Message.of("data".getBytes())));
        Assert.assertEquals("Transaction is already finished", ex.getMessage());
        Assert.assertNull(delegate.lastSettings);
    }

    @Test
    public void asyncWriterIsClosedOnTransactionCompletion() {
        CompletableFuture<Status> txStatusFuture = new CompletableFuture<>();
        StubTransaction tx = new StubTransaction(true, txStatusFuture);
        StubAsyncWriter delegate = new StubAsyncWriter();

        new AsyncTransactionalWriterImpl(delegate, tx);
        txStatusFuture.complete(Status.SUCCESS);

        Assert.assertEquals(1, delegate.shutdownCount.get());
    }

    @Test
    public void syncWriterUsesBoundTransaction() {
        StubTransaction tx = new StubTransaction(true, new CompletableFuture<>());
        StubSyncWriter delegate = new StubSyncWriter();
        Message message = Message.of("data".getBytes());

        SyncTransactionalWriterImpl writer = new SyncTransactionalWriterImpl(delegate, tx);
        writer.send(message);

        Assert.assertSame(message, delegate.lastMessage);
        Assert.assertNotNull(delegate.lastSettings);
        Assert.assertSame(tx, delegate.lastSettings.getTransaction());
    }

    @Test
    public void syncWriterIsClosedOnTransactionCompletion() throws Exception {
        CompletableFuture<Status> txStatusFuture = new CompletableFuture<>();
        StubTransaction tx = new StubTransaction(true, txStatusFuture);
        StubSyncWriter delegate = new StubSyncWriter();

        SyncTransactionalWriterImpl writer = new SyncTransactionalWriterImpl(delegate, tx);
        txStatusFuture.complete(Status.SUCCESS);

        for (int i = 0; i < 100; i++) {
            if (delegate.shutdownCount.get() > 0) {
                break;
            }
            Thread.sleep(10);
        }

        Assert.assertEquals(1, delegate.shutdownCount.get());
        writer.shutdown(1, TimeUnit.SECONDS);
        Assert.assertEquals(2, delegate.shutdownCount.get());
    }

    private static final class StubTransaction implements YdbTransaction {
        private final boolean active;
        private final CompletableFuture<Status> statusFuture;

        private StubTransaction(boolean active, CompletableFuture<Status> statusFuture) {
            this.active = active;
            this.statusFuture = statusFuture;
        }

        @Override
        public String getId() {
            return active ? "tx-id" : null;
        }

        @Override
        public TxMode getTxMode() {
            return TxMode.SERIALIZABLE_RW;
        }

        @Override
        public String getSessionId() {
            return "session-id";
        }

        @Override
        public CompletableFuture<Status> getStatusFuture() {
            return statusFuture;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }

    private static final class StubAsyncWriter implements AsyncWriter {
        private final CompletableFuture<WriteAck> sendFuture = CompletableFuture.completedFuture(
                new WriteAck(1L, WriteAck.State.WRITTEN_IN_TX, null, null)
        );
        private final AtomicInteger shutdownCount = new AtomicInteger(0);
        private Message lastMessage;
        private SendSettings lastSettings;

        @Override
        public CompletableFuture<InitResult> init() {
            return CompletableFuture.completedFuture(new InitResult(0));
        }

        @Override
        public CompletableFuture<WriteAck> send(Message message, SendSettings settings) throws QueueOverflowException {
            this.lastMessage = message;
            this.lastSettings = settings;
            return sendFuture;
        }

        @Override
        public CompletableFuture<WriteAck> send(Message message) throws QueueOverflowException {
            this.lastMessage = message;
            this.lastSettings = null;
            return sendFuture;
        }

        @Override
        public CompletableFuture<Void> shutdown() {
            shutdownCount.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class StubSyncWriter implements SyncWriter {
        private final AtomicInteger shutdownCount = new AtomicInteger(0);
        private Message lastMessage;
        private SendSettings lastSettings;

        @Override
        public void init() {
            // no-op
        }

        @Override
        public InitResult initAndWait() {
            return new InitResult(0);
        }

        @Override
        public void send(Message message, SendSettings settings) {
            this.lastMessage = message;
            this.lastSettings = settings;
        }

        @Override
        public void send(Message message, SendSettings settings, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            this.lastMessage = message;
            this.lastSettings = settings;
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void shutdown(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            shutdownCount.incrementAndGet();
        }
    }
}
