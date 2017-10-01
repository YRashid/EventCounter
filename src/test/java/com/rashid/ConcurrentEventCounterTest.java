package com.rashid;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Rashid.Iaraliev on 30.09.17.
 */
public class ConcurrentEventCounterTest {
    private EventCounter eventCounter;

    @Before
    public void before() {
        eventCounter = new ConcurrentEventCounter();
    }

    @Test
    public void singleThreadSingleEvent() throws Exception {
        eventCounter.submitEvent();
        assertEquals(1, eventCounter.getQuantityByLastMinute());
        assertEquals(1, eventCounter.getQuantityByLastHour());
        assertEquals(1, eventCounter.getQuantityByLastDay());

        TimeUnit.SECONDS.sleep(1L);

        assertEquals(1, eventCounter.getQuantityByLastMinute());
        assertEquals(1, eventCounter.getQuantityByLastHour());
        assertEquals(1, eventCounter.getQuantityByLastDay());

    }

    @Test
    public void singleThreadMultipleEvent() throws Exception {
        eventCounter.submitEvent();
        TimeUnit.MILLISECONDS.sleep(50L);
        eventCounter.submitEvent();
        TimeUnit.MILLISECONDS.sleep(50L);
        eventCounter.submitEvent();
        TimeUnit.MILLISECONDS.sleep(50L);

        assertEquals(3, eventCounter.getQuantityByLastMinute());
        assertEquals(3, eventCounter.getQuantityByLastHour());
        assertEquals(3, eventCounter.getQuantityByLastDay());

        TimeUnit.SECONDS.sleep(1L);

        assertEquals(3, eventCounter.getQuantityByLastMinute());
        assertEquals(3, eventCounter.getQuantityByLastHour());
        assertEquals(3, eventCounter.getQuantityByLastDay());

    }

    @Test
    public void millionEventsByThousandThreads() throws Exception {
        int THREADS_COUNT = 1000;
        int EVENTS_COUNT = 1000;
        CyclicBarrier barrier1 = new CyclicBarrier(THREADS_COUNT);
        CyclicBarrier barrier2 = new CyclicBarrier(THREADS_COUNT);

        doCountEventInThreads(THREADS_COUNT, EVENTS_COUNT, barrier1);

        TimeUnit.SECONDS.sleep(3L);
        assertEquals(THREADS_COUNT * EVENTS_COUNT, eventCounter.getQuantityByLastMinute());
        assertEquals(THREADS_COUNT * EVENTS_COUNT, eventCounter.getQuantityByLastHour());
        assertEquals(THREADS_COUNT * EVENTS_COUNT, eventCounter.getQuantityByLastDay());


        doCountEventInThreads(THREADS_COUNT, EVENTS_COUNT, barrier2);

        TimeUnit.SECONDS.sleep(10L);
        assertEquals(2 * THREADS_COUNT * EVENTS_COUNT, eventCounter.getQuantityByLastMinute());
        assertEquals(2 * THREADS_COUNT * EVENTS_COUNT, eventCounter.getQuantityByLastHour());
        assertEquals(2 * THREADS_COUNT * EVENTS_COUNT, eventCounter.getQuantityByLastDay());

    }

//    @Ignore("Using reflection to modify static final fields")
    @Test
    public void testWithDangerousReflection_singleThread() throws Exception {
        setFinalStatic(ConcurrentEventCounter.class.getDeclaredField("MILLIS_IN_MINUTE"), 5000L);
        setFinalStatic(ConcurrentEventCounter.class.getDeclaredField("MILLIS_IN_HOUR"), 10000L);
        setFinalStatic(ConcurrentEventCounter.class.getDeclaredField("MILLIS_IN_DAY"), 15000L);
        eventCounter = new ConcurrentEventCounter();

        eventCounter.submitEvent();

        assertEquals(1, eventCounter.getQuantityByLastMinute());
        assertEquals(1, eventCounter.getQuantityByLastHour());
        assertEquals(1, eventCounter.getQuantityByLastDay());

        TimeUnit.SECONDS.sleep(6L);
        assertEquals(0, eventCounter.getQuantityByLastMinute());
        assertEquals(1, eventCounter.getQuantityByLastHour());
        assertEquals(1, eventCounter.getQuantityByLastDay());

        TimeUnit.SECONDS.sleep(6L);
        assertEquals(0, eventCounter.getQuantityByLastMinute());
        assertEquals(0, eventCounter.getQuantityByLastHour());
        assertEquals(1, eventCounter.getQuantityByLastDay());

        TimeUnit.SECONDS.sleep(6L);
        assertEquals(0, eventCounter.getQuantityByLastMinute());
        assertEquals(0, eventCounter.getQuantityByLastHour());
        assertEquals(0, eventCounter.getQuantityByLastDay());

        resetDefaultTimeConstants();
    }

//    @Ignore("Using reflection to modify static final fields")
    @Test
    public void testWithDangerousReflection_multiThreads() throws Exception {
        setFinalStatic(ConcurrentEventCounter.class.getDeclaredField("MILLIS_IN_MINUTE"), 5000L);
        setFinalStatic(ConcurrentEventCounter.class.getDeclaredField("MILLIS_IN_HOUR"), 12000L);
        setFinalStatic(ConcurrentEventCounter.class.getDeclaredField("MILLIS_IN_DAY"), 17000L);
        eventCounter = new ConcurrentEventCounter();

        int THREADS_COUNT = 1000;
        int EVENTS_COUNT = 1000;
        CyclicBarrier barrier1 = new CyclicBarrier(THREADS_COUNT);
        CyclicBarrier barrier2 = new CyclicBarrier(THREADS_COUNT);

        doCountEventInThreads(THREADS_COUNT, EVENTS_COUNT, barrier1);

        TimeUnit.SECONDS.sleep(3L);
        assertEquals(THREADS_COUNT * EVENTS_COUNT, eventCounter.getQuantityByLastMinute());
        assertEquals(THREADS_COUNT * EVENTS_COUNT, eventCounter.getQuantityByLastHour());
        assertEquals(THREADS_COUNT * EVENTS_COUNT, eventCounter.getQuantityByLastDay());

        doCountEventInThreads(THREADS_COUNT, EVENTS_COUNT, barrier2);


        TimeUnit.SECONDS.sleep(5L);
        assertTrue(eventCounter.getQuantityByLastMinute() >= THREADS_COUNT * EVENTS_COUNT && eventCounter.getQuantityByLastMinute() < 2 * THREADS_COUNT * EVENTS_COUNT);
        assertEquals(2 * THREADS_COUNT * EVENTS_COUNT, eventCounter.getQuantityByLastHour());
        assertEquals(2 * THREADS_COUNT * EVENTS_COUNT, eventCounter.getQuantityByLastDay());
        TimeUnit.SECONDS.sleep(17L);
        assertEquals(0, eventCounter.getQuantityByLastMinute());
        assertEquals(0, eventCounter.getQuantityByLastHour());
        assertEquals(0, eventCounter.getQuantityByLastDay());


        resetDefaultTimeConstants();
    }

    private void setFinalStatic(Field field, long newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }

    private void resetDefaultTimeConstants() throws Exception {
        setFinalStatic(ConcurrentEventCounter.class.getDeclaredField("MILLIS_IN_MINUTE"), TimeUnit.MINUTES.toMillis(1));
        setFinalStatic(ConcurrentEventCounter.class.getDeclaredField("MILLIS_IN_HOUR"), TimeUnit.HOURS.toMillis(1));
        setFinalStatic(ConcurrentEventCounter.class.getDeclaredField("MILLIS_IN_DAY"), TimeUnit.DAYS.toMillis(1));
    }

    private void doCountEventInThreads(int THREADS_COUNT, int EVENTS_COUNT, CyclicBarrier barrier1) {
        for (int i = 0; i < THREADS_COUNT; i++) {
            new Thread(() -> {
                try {
                    barrier1.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
                for (int j = 0; j < EVENTS_COUNT; j++) {
                    eventCounter.submitEvent();
                }
            }).start();
        }
    }



}