package tech.ydb.topic.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.TopicCodec;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class YdbTopicsCustomCodecIntegrationTest {
    private final static Logger logger = LoggerFactory.getLogger(YdbTopicsCustomCodecIntegrationTest.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final static String TEST_TOPIC1 = "integration_test_custom_codec_topic1";
    private final static String TEST_TOPIC2 = "integration_test_custom_codec_topic2";
    private final static String TEST_CONSUMER1 = "consumer";
    private final static String TEST_CONSUMER2 = "other_consumer";

    private static TopicClient client;

    private final static byte[][] TEST_MESSAGES = new byte[][]{
            "Test message".getBytes(),
            "".getBytes(),
            " ".getBytes(),
            "Other message".getBytes(),
            "Last message".getBytes(),
    };

    @Test
    public void writeDataAndReadDataWithCustomCodec() throws InterruptedException, ExecutionException, TimeoutException {
        try {
            createTopic(TEST_TOPIC1);
            TopicCodec codec = new CustomTopicCode(1);
            writeData(codec, TEST_TOPIC1);
            readData(codec, TEST_TOPIC1);
        } finally {
            deleteTopic(TEST_TOPIC1);
        }
    }

    @Test
    public void writeInTwoTopicsInOneClientWithDifferentCustomCodec() throws ExecutionException, InterruptedException, TimeoutException {
        try {
            createTopic(TEST_TOPIC1);
            createTopic(TEST_TOPIC2);

            TopicCodec codec1 = new CustomTopicCode(1);
            TopicCodec codec2 = new CustomTopicCode(7);

            writeData(codec1, TEST_TOPIC1);
            writeData(codec2, TEST_TOPIC2);

            readData(codec1, TEST_TOPIC1);
            readData(codec2, TEST_TOPIC2);
        } finally {
            deleteTopic(TEST_TOPIC1);
            deleteTopic(TEST_TOPIC2);
        }
    }

    @Test
    public void writeInWrongCode() throws ExecutionException, InterruptedException, TimeoutException {
        try {
            createTopic(TEST_TOPIC1);

            TopicCodec codec1 = new CustomTopicCode(1);
            TopicCodec codec2 = new CustomTopicCode(7);

            writeData(codec1, TEST_TOPIC1);

            readData(codec2, TEST_TOPIC2);
        } finally {
            deleteTopic(TEST_TOPIC1);
        }
    }

    private void createTopic(String topicName) {
        logger.info("Create test topic {} ...", topicName);

        client = TopicClient.newClient(ydbTransport).build();
        client.createTopic(topicName, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER2).build())
                .build()
        ).join().expectSuccess("can't create a new topic");
    }

    private void deleteTopic(String topicName) {
        logger.info("Drop test topic {} ...", topicName);
        Status dropStatus = client.dropTopic(topicName).join();
        client.close();
        dropStatus.expectSuccess("can't drop test topic");
    }

    private void writeData(TopicCodec codec, String topicName) throws ExecutionException, InterruptedException, TimeoutException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(topicName)
                .setCodec(10113, codec)
                .build();
        SyncWriter writer = client.createSyncWriter(settings);
        writer.init();

        for (byte[] testMessage : TEST_MESSAGES) {
            writer.send(Message.newBuilder().setData(testMessage).build());
        }

        writer.flush();
        writer.shutdown(1, TimeUnit.MINUTES);
    }

    private void readData(TopicCodec codec, String topicName) throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(topicName).build())
                .setConsumerName(TEST_CONSUMER1)
                .setCodec(10013, codec)
                .build();

        SyncReader reader = client.createSyncReader(readerSettings);
        reader.initAndWait();

        for (byte[] bytes : TEST_MESSAGES) {
            tech.ydb.topic.read.Message msg = reader.receive();
            Assert.assertArrayEquals(bytes, msg.getData());
        }

        reader.shutdown();
    }

    static class CustomTopicCode implements TopicCodec {

        final int stub;

        public CustomTopicCode(int stub) {
            this.stub = stub;
        }


        @Override
        public InputStream decode(ByteArrayInputStream byteArrayOutputStream) throws IOException {
            final ByteArrayInputStream outputStream = byteArrayOutputStream;
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    for (int i = 0; i < stub; i++) {
                        int stub = outputStream.read();
                    }

                    return outputStream.read();
                }
            };
        }

        @Override
        public OutputStream encode(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
            return new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    for (int i = 0; i < stub; i++) {
                        byteArrayOutputStream.write(stub);
                    }

                    byteArrayOutputStream.write(b);
                }
            };
        }
    }
}
