package tech.ydb.topic;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.StatusCode;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryTransaction;
import tech.ydb.query.tools.QueryReader;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncTransactionalWriter;

/**
 * Transactional write tests for Topic + Query transactions.
 */
public class TopicQueryTransactionIntegrationTest {
    @ClassRule
    public static final GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private static final String TEST_TOPIC = "query_tx_writer_test_topic";
    private static final String TEST_TABLE = "query_tx_writer_test_table";
    private static final String TEST_PRODUCER = "producer";
    private static final String TEST_CONSUMER = "consumer";

    private static final Duration SESSION_TIMEOUT = Duration.ofSeconds(5);

    private static TopicClient topicClient;
    private static QueryClient queryClient;

    @BeforeClass
    public static void initClients() {
        topicClient = TopicClient.newClient(ydbTransport).build();
        queryClient = QueryClient.newClient(ydbTransport).build();

        try (QuerySession session = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
            QueryReader.readFrom(session.createQuery(
                    "CREATE TABLE " + TEST_TABLE + " (id Int32 NOT NULL, value Text, PRIMARY KEY(id))",
                    TxMode.NONE
            )).join().getStatus().expectSuccess("can't create test table");
        }
    }

    @AfterClass
    public static void closeClients() {
        try (QuerySession session = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
            QueryReader.readFrom(session.createQuery("DROP TABLE " + TEST_TABLE, TxMode.NONE)).join();
        }
        queryClient.close();
        topicClient.close();
    }

    @Before
    public void createTopic() {
        topicClient.createTopic(TEST_TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER).build())
                .build())
                .join().expectSuccess("can't create a new topic");
    }

    @After
    public void cleanUp() {
        topicClient.dropTopic(TEST_TOPIC).join();
        try (QuerySession session = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
            QueryReader.readFrom(session.createQuery(
                    "DELETE FROM " + TEST_TABLE,
                    TxMode.SERIALIZABLE_RW
            )).join().getStatus().expectSuccess("can't clean test table");
        }
    }

    @Test
    public void successfulAtomicCommitTest() throws Exception {
        try (QuerySession session = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
            QueryTransaction tx = session.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            SyncTransactionalWriter writer = topicClient.createSyncTransactionalWriter(tx, writerSettings());
            writer.initAndWait();

            try {
                upsert(tx, 1, "first");
                writer.send(Message.of("msg1".getBytes()));
                writer.flush();
                tx.commit().join().getStatus().expectSuccess("can't commit transaction");
            } finally {
                writer.shutdown(5, TimeUnit.SECONDS);
            }
        }

        Assert.assertEquals(Collections.singletonList(1), readTableIds());
        assertTopicContent("msg1");
    }

    @Test
    public void rollbackTest() throws Exception {
        try (QuerySession session = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
            QueryTransaction tx = session.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            SyncTransactionalWriter writer = topicClient.createSyncTransactionalWriter(tx, writerSettings());
            writer.initAndWait();

            try {
                upsert(tx, 2, "second");
                writer.send(Message.of("msg1".getBytes()));
                writer.flush();
                tx.rollback().join().expectSuccess("can't rollback transaction");
            } finally {
                writer.shutdown(5, TimeUnit.SECONDS);
            }
        }

        Assert.assertEquals(Collections.emptyList(), readTableIds());
        assertTopicContent();
    }

    @Test
    public void transactionCommitAfterConcurrentBlindWriteTest() throws Exception {
        byte[] payload = "retry-msg".getBytes();

        try (QuerySession session1 = queryClient.createSession(SESSION_TIMEOUT).join().getValue();
                QuerySession session2 = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {

            QueryTransaction tx1 = session1.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            SyncTransactionalWriter writer1 = topicClient.createSyncTransactionalWriter(
                    tx1, writerSettings(TEST_PRODUCER + "-abort-1"));
            writer1.initAndWait();
            try {
                upsert(tx1, 3, "first-attempt");
                writer1.send(Message.of(payload));

                QueryTransaction blocker = session2.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
                upsert(blocker, 3, "blocker");
                blocker.commit().join().getStatus().expectSuccess("can't commit blocker tx");

                tx1.commit().join().getStatus().expectSuccess("first tx must not be aborted");
            } finally {
                writer1.shutdown(5, TimeUnit.SECONDS);
            }
        }

        Assert.assertEquals(Collections.singletonList(3), readTableIds());
        assertTopicContent("retry-msg");
    }

    @Test
    public void transactionAbortAndRetryTest() throws Exception {
        int conflictId = 6;
        byte[] firstPayload = "aborted-msg".getBytes();
        byte[] retryPayload = "retry-msg".getBytes();

        try (QuerySession setupSession = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
            QueryTransaction setupTx = setupSession.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            upsert(setupTx, conflictId, "initial");
            setupTx.commit().join().getStatus().expectSuccess("can't prepare conflict row");
        }

        try (QuerySession session1 = queryClient.createSession(SESSION_TIMEOUT).join().getValue();
                QuerySession session2 = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {

            QueryTransaction tx1 = session1.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            SyncTransactionalWriter writer1 = topicClient.createSyncTransactionalWriter(tx1, writerSettings());
            writer1.initAndWait();
            try {
                readValueById(tx1, conflictId, "initial");
                writer1.send(Message.of(firstPayload));

                QueryTransaction blocker = session2.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
                upsert(blocker, conflictId, "blocker");
                blocker.commit().join().getStatus().expectSuccess("can't commit blocker tx");

                upsert(tx1, conflictId, "first-attempt");
                writer1.flush();
                Assert.assertEquals(StatusCode.ABORTED, tx1.commit().join().getStatus().getCode());
            } finally {
                writer1.shutdown(5, TimeUnit.SECONDS);
            }

            QueryTransaction tx2 = session1.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            SyncTransactionalWriter writer2 = topicClient.createSyncTransactionalWriter(
                    tx2, writerSettings(TEST_PRODUCER + "-abort-2"));
            writer2.initAndWait();
            try {
                upsert(tx2, conflictId, "second-attempt");
                writer2.send(Message.of(retryPayload));
                writer2.flush();
                tx2.commit().join().getStatus().expectSuccess("can't commit retry tx");
            } finally {
                writer2.shutdown(5, TimeUnit.SECONDS);
            }
        }

        Assert.assertEquals(Collections.singletonList(conflictId), readTableIds());
        assertTopicContent("retry-msg");
    }

    @Test
    public void multipleTopicWritesTest() throws Exception {
        try (QuerySession session = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
            QueryTransaction tx = session.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            SyncTransactionalWriter writer = topicClient.createSyncTransactionalWriter(tx, writerSettings());
            writer.initAndWait();

            try {
                writer.send(Message.of("msg1".getBytes()));
                writer.send(Message.of("msg2".getBytes()));
                writer.send(Message.of("msg3".getBytes()));
                writer.flush();
                tx.commit().join().getStatus().expectSuccess("can't commit transaction");
            } finally {
                writer.shutdown(5, TimeUnit.SECONDS);
            }
        }

        assertTopicContent("msg1", "msg2", "msg3");
    }

    @Test
    public void multipleOperationsInSameTransactionTest() throws Exception {
        try (QuerySession session = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
            QueryTransaction tx = session.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            SyncTransactionalWriter writer = topicClient.createSyncTransactionalWriter(tx, writerSettings());
            writer.initAndWait();

            try {
                upsert(tx, 4, "a");
                writer.send(Message.of("msg1".getBytes()));
                upsert(tx, 5, "b");
                writer.send(Message.of("msg2".getBytes()));
                writer.flush();
                tx.commit().join().getStatus().expectSuccess("can't commit transaction");
            } finally {
                writer.shutdown(5, TimeUnit.SECONDS);
            }
        }

        Assert.assertEquals(java.util.Arrays.asList(4, 5), readTableIds());
        assertTopicContent("msg1", "msg2");
    }

    @Test
    public void errorHandlingRequiresActiveTransactionTest() {
        try (QuerySession session = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
            QueryTransaction tx = session.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            tx.rollback().join().expectSuccess("can't rollback transaction");

            IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                    () -> topicClient.createSyncTransactionalWriter(tx, writerSettings()));
            Assert.assertEquals("Transaction is not active. Can only write topic messages in already running "
                    + "transactions from other services", ex.getMessage());
        }
    }

    private static WriterSettings writerSettings() {
        return writerSettings(TEST_PRODUCER);
    }

    private static WriterSettings writerSettings(String producerId) {
        return WriterSettings.newBuilder().setTopicPath(TEST_TOPIC).setProducerId(producerId).build();
    }

    private static void upsert(QueryTransaction tx, int id, String value) {
        Params params = Params.of("$id", PrimitiveValue.newInt32(id), "$value", PrimitiveValue.newText(value));
        QueryReader.readFrom(tx.createQuery("DECLARE $id AS Int32; DECLARE $value AS Text; "
                + "UPSERT INTO " + TEST_TABLE + " (id, value) VALUES ($id, $value)", params))
                .join().getStatus().expectSuccess("can't upsert");
    }

    private static void readValueById(QueryTransaction tx, int id, String expectedValue) {
        Params params = Params.of("$id", PrimitiveValue.newInt32(id));
        ResultSetReader rs = QueryReader.readFrom(
                tx.createQuery("DECLARE $id AS Int32; SELECT value FROM " + TEST_TABLE + " WHERE id = $id", params)
        ).join().getValue().getResultSet(0);
        Assert.assertTrue("Expected row with id=" + id, rs.next());
        Assert.assertEquals(expectedValue, rs.getColumn(0).getText());
    }

    private static List<Integer> readTableIds() {
        try (QuerySession session = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
            ResultSetReader rs = QueryReader.readFrom(
                    session.createQuery("SELECT id FROM " + TEST_TABLE + " ORDER BY id", TxMode.SNAPSHOT_RO)
            ).join().getValue().getResultSet(0);

            List<Integer> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getColumn(0).getInt32());
            }
            return ids;
        }
    }

    private static void assertTopicContent(String... messages) throws InterruptedException {
        SyncReader reader = topicClient.createSyncReader(ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER)
                .build());
        reader.initAndWait();
        try {
            for (String expected : messages) {
                tech.ydb.topic.read.Message next = reader.receive(5, TimeUnit.SECONDS);
                Assert.assertNotNull("Expected message " + expected, next);
                Assert.assertEquals(expected, new String(next.getData()));
                next.commit().join();
            }
            Assert.assertNull("Unexpected message in topic", reader.receive(500, TimeUnit.MILLISECONDS));
        } finally {
            reader.shutdown();
        }
    }
}
