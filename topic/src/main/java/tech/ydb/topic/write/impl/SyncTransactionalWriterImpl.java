package tech.ydb.topic.write.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncTransactionalWriter;
import tech.ydb.topic.write.SyncWriter;

public class SyncTransactionalWriterImpl implements SyncTransactionalWriter {
    private static final Logger logger = LoggerFactory.getLogger(SyncTransactionalWriterImpl.class);

    private final SyncWriter delegate;
    private final YdbTransaction transaction;
    private final SendSettings sendSettings;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public SyncTransactionalWriterImpl(SyncWriter delegate, YdbTransaction transaction) {
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
        transaction.getStatusFuture().whenComplete((status, error) -> shutdownSilently());
    }

    @Override
    public void init() {
        ensureTransactionIsActive();
        delegate.init();
    }

    @Override
    public InitResult initAndWait() {
        ensureTransactionIsActive();
        return delegate.initAndWait();
    }

    @Override
    public void send(Message message) {
        ensureTransactionIsActive();
        delegate.send(message, sendSettings);
    }

    @Override
    public void send(Message message, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        ensureTransactionIsActive();
        delegate.send(message, sendSettings, timeout, unit);
    }

    @Override
    public void flush() {
        ensureTransactionIsActive();
        delegate.flush();
    }

    @Override
    public void shutdown(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        delegate.shutdown(timeout, unit);
        isShutdown.set(true);
    }

    private void shutdownSilently() {
        if (!isShutdown.compareAndSet(false, true)) {
            return;
        }

        try {
            delegate.shutdown(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("Interrupted while closing transactional writer", e);
        } catch (ExecutionException | TimeoutException e) {
            logger.debug("Cannot close transactional writer on transaction finish", e);
        }
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

