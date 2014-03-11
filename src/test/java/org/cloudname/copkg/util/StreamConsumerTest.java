package org.cloudname.copkg.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit test for StreamConsumer.
 *
 * @author borud
 */
public class StreamConsumerTest {
    private static final int NUM_BYTES = 20 * 1024;

    /**
     * Consume entire input without overrun.
     */
    @Test
    public void simpleConsumeTest() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(NUM_BYTES);
        final InputStream in = new ByteArrayInputStream(makeByteArray(NUM_BYTES));

        final CountDownLatch done = new CountDownLatch(1);

        final StreamConsumer consumer = new StreamConsumer(in, out, NUM_BYTES * 2);
        consumer.addListener(new StreamConsumer.Listener() {
                @Override
                public void onNotify(StreamConsumer c, Status s) {
                    assertSame(s, StreamConsumer.Listener.Status.DONE);
                    done.countDown();
                }
            });

        new Thread(consumer).start();

        // Give it 100 milliseconds to finish
        assertTrue(done.await(100L, TimeUnit.MILLISECONDS));
        assertEquals(NUM_BYTES, out.size());
    }

    /**
     * Consume input and overrun limit
     *
     */
    @Test
    public void consumeOverrun() throws Exception {
        final OutputStream out = new ByteArrayOutputStream(NUM_BYTES * 2);
        final InputStream in = new ByteArrayInputStream(makeByteArray(NUM_BYTES * 2));

        final StreamConsumer consumer = new StreamConsumer(in, out, NUM_BYTES);
        consumer.addListener(new StreamConsumer.Listener() {
            final List<Status> expectedNotifications
                    = Arrays.asList(Status.DONE, Status.MAX_READ);
                @Override
                public void onNotify(StreamConsumer c, Status s) {
                    // First the MAX_READ will be triggered and then the DONE.
                    assertTrue(expectedNotifications.contains(s));
                }
            });

        new Thread(consumer).start();
    }

    /**
     * Make array of specified size and fill it recognizable data.
     */
    private static byte[] makeByteArray(int size) {
        byte[] data = new byte[size];
        Arrays.fill(data, (byte)'a');
        return data;
    }
}