package tech.ydb.topic.write;

import java.util.concurrent.CompletableFuture;

/**
 * An async writer bound to a single existing YDB transaction.
 *
 * The writer never commits or rolls back the transaction and only attaches topic writes to it.
 * Transaction lifecycle is controlled by the caller.
 */
public interface AsyncTransactionalWriter {

    /**
     * Initialize internal threads in the background. Non-blocking.
     *
     * @return {@link CompletableFuture} with {@link InitResult} containing initialization data like lastSeqNo
     */
    CompletableFuture<InitResult> init();

    /**
     * Send message. Non-blocking.
     *
     * @param message message data to write in bound transaction
     * @return {@link CompletableFuture} with {@link WriteAck} for write acknowledgement
     */
    CompletableFuture<WriteAck> send(Message message) throws QueueOverflowException;

    /**
     * Stops internal threads and makes cleanup in background. Non-blocking.
     */
    CompletableFuture<Void> shutdown();
}

