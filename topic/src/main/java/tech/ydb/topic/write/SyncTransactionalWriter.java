package tech.ydb.topic.write;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A sync writer bound to a single existing YDB transaction.
 *
 * The writer never commits or rolls back the transaction and only attaches topic writes to it.
 * Transaction lifecycle is controlled by the caller.
 */
public interface SyncTransactionalWriter {

    /**
     * Initialize internal threads in the background. Non-blocking.
     */
    void init();

    /**
     * Initialize internal threads and wait for server init response. Blocking.
     *
     * @return {@link InitResult} containing initialization data like lastSeqNo
     */
    InitResult initAndWait();

    /**
     * Send message. Blocks infinitely until the message is put into sending buffer.
     *
     * @param message message data to write in bound transaction
     */
    void send(Message message);

    /**
     * Send message. Blocks until the message is put into sending buffer.
     * If in-flight or memory usage limits are reached, waits until timeout expires and then
     * throws {@link TimeoutException} if message was not put into queue.
     *
     * @param message message data to write in bound transaction
     * @param timeout timeout to wait until message is put into sending buffer
     * @param unit {@link TimeUnit} for timeout
     */
    void send(Message message, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Waits until all current writes are sent to server and response is received. Blocking.
     */
    void flush();

    /**
     * Stops internal threads and makes cleanup in background. Blocking.
     */
    void shutdown(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}

