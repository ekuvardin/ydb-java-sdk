package tech.ydb.topic.write.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.write.AsyncTransactionalWriter;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

public class AsyncTransactionalWriterImpl implements AsyncTransactionalWriter {
    private final AsyncWriter delegate;
    private final YdbTransaction transaction;
    private final SendSettings sendSettings;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public AsyncTransactionalWriterImpl(AsyncWriter delegate, YdbTransaction transaction) {
        if (delegate == null) {
            throw new NullPointerException("delegate must not be null");
        }
        if (transaction == null) {
            throw new NullPointerException("transaction must not be null");
        }
        if (!transaction.isActive()) {
            throw new IllegalArgumentException("Transaction is not active. "
                    + "Can only write topic messages in already running transactions from other services");
        }
        this.delegate = delegate;
        this.transaction = transaction;
        this.sendSettings = SendSettings.newBuilder().setTransaction(transaction).build();
        transaction.getStatusFuture().whenComplete((status, error) -> shutdown());
    }

    @Override
    public CompletableFuture<InitResult> init() {
        ensureTransactionIsActive();
        return delegate.init();
    }

    @Override
    public CompletableFuture<WriteAck> send(Message message) throws QueueOverflowException {
        ensureTransactionIsActive();
        return delegate.send(message, sendSettings);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        isShutdown.set(true);
        return delegate.shutdown();
    }

    private void ensureTransactionIsActive() {
        if (!transaction.isActive()) {
            throw new IllegalStateException("Transaction is not active anymore");
        }
        if (transaction.getStatusFuture().isDone()) {
            throw new IllegalStateException("Transaction is already finished");
        }
    }
}
