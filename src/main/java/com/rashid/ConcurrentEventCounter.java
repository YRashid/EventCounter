package com.rashid;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Описание: https://github.com/YRashid/EventCounter/blob/master/readme.md
 * Created by Rashid.Iaraliev on 30.09.17.
 */
public class ConcurrentEventCounter implements EventCounter {
    private final static long MILLIS_IN_SECOND = TimeUnit.SECONDS.toMillis(1);
    private final static long MILLIS_IN_MINUTE = TimeUnit.MINUTES.toMillis(1);
    private final static long MILLIS_IN_HOUR = TimeUnit.HOURS.toMillis(1);
    private final static long MILLIS_IN_DAY = TimeUnit.DAYS.toMillis(1);

    private AtomicLong quantityByLastMinute = new AtomicLong();
    private AtomicLong quantityByLastHour = new AtomicLong();
    private AtomicLong quantityByLastDay = new AtomicLong();

    private Queue<Long> eventsTimeForMinuteQueue = new ConcurrentLinkedQueue<>();
    private Queue<EventsBucket> eventsTimeForHourQueue = new ConcurrentLinkedQueue<>();
    private Queue<EventsBucket> eventsTimeForDayQueue = new ConcurrentLinkedQueue<>();

    private ScheduledExecutorService minuteCleaner = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService hourCleaner = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService dayCleaner = Executors.newSingleThreadScheduledExecutor();


    public ConcurrentEventCounter() {
        initMinuteCleaner();
        initHourCleaner();
        initDayCleaner();
    }

    /**
     * Устаревшие события из очереди последней минуты перекидываются в очередь для последнего часа.
     */
    private void initMinuteCleaner() {
        minuteCleaner.scheduleWithFixedDelay(() -> {
            Long eventTime;
            EventsBucket eventsBucket = null;

            // удаляем устаревшее событие из очереди событий последней минуты
            // объединяем в eventsBucket устаревшие события, которые отличаются по времени меньше чем на секунду
            // добавляем объединенные события (eventsBucket) в очередь событий последнего часа
            while ((eventTime = eventsTimeForMinuteQueue.peek()) != null && !isInLastPeriod(TimeUnit.MINUTES, eventTime) && !Thread.interrupted()) {
                eventsTimeForMinuteQueue.poll();
                quantityByLastMinute.decrementAndGet();

                if (eventsBucket == null) {
                    eventsBucket = new EventsBucket(eventTime, 1);
                    continue;
                }

                if (Math.abs(eventTime - eventsBucket.getTime()) <= MILLIS_IN_SECOND) {
                    eventsBucket.incrementCountOfEvents();
                } else {
                    eventsTimeForHourQueue.offer(eventsBucket);
                    eventsBucket = new EventsBucket(eventTime, 1);
                }
            }

            if (eventsBucket != null) {
                eventsTimeForHourQueue.offer(eventsBucket);
            }


        }, 0, MILLIS_IN_SECOND, TimeUnit.MILLISECONDS);
    }

    /**
     * Устаревшие события из очереди последнего часа перекидываются в очередь для последнего дня.
     */
    private void initHourCleaner() {
        hourCleaner.scheduleWithFixedDelay(() -> {
            EventsBucket eventsBucket;
            while ((eventsBucket = eventsTimeForHourQueue.peek()) != null && !isInLastPeriod(TimeUnit.HOURS, eventsBucket.getTime()) && !Thread.interrupted()) {
                eventsTimeForHourQueue.poll();
                final long countOfEvents = eventsBucket.getCountOfEvents();
                quantityByLastHour.updateAndGet(x -> x - countOfEvents);
                eventsTimeForDayQueue.offer(eventsBucket);
            }

        }, MILLIS_IN_HOUR, MILLIS_IN_SECOND, TimeUnit.MILLISECONDS);
    }

    /**
     * Устаревшие события из очереди последнего дня удялаются.
     */
    private void initDayCleaner() {
        dayCleaner.scheduleWithFixedDelay(() -> {
            EventsBucket eventsBucket;
            while ((eventsBucket = eventsTimeForDayQueue.peek()) != null && !isInLastPeriod(TimeUnit.DAYS, eventsBucket.getTime()) && !Thread.interrupted()) {
                eventsTimeForDayQueue.poll();
                final long countOfEvents = eventsBucket.getCountOfEvents();
                quantityByLastDay.updateAndGet(x -> x - countOfEvents);
            }

        }, MILLIS_IN_DAY, MILLIS_IN_SECOND, TimeUnit.MILLISECONDS);
    }

    /**
     * Определяет, находится ли время millis во временном интервале текущей минуты/часа/дня
     *
     * @param timeUnit - тип временного интервала: минута/час/день
     * @param millis   - время в миллисекундах
     * @return true/false - находится/не находится
     */
    private boolean isInLastPeriod(TimeUnit timeUnit, long millis) {
        switch (timeUnit) {
            case MINUTES:
                return System.currentTimeMillis() - millis <= MILLIS_IN_MINUTE;
            case HOURS:
                return System.currentTimeMillis() - millis <= MILLIS_IN_HOUR;
            case DAYS:
                return System.currentTimeMillis() - millis <= MILLIS_IN_DAY;
            default:
                throw new IllegalArgumentException("Unsupported TimeUnit: " + timeUnit);
        }
    }

    @Override
    public boolean submitEvent() {
        boolean result = eventsTimeForMinuteQueue.offer(System.currentTimeMillis());
        if (result) {
            quantityByLastMinute.incrementAndGet();
            quantityByLastHour.incrementAndGet();
            quantityByLastDay.incrementAndGet();
        }
        return result;
    }

    @Override
    public long getQuantityByLastMinute() {
        return quantityByLastMinute.get();
    }

    @Override
    public long getQuantityByLastHour() {
        return quantityByLastHour.get();
    }

    @Override
    public long getQuantityByLastDay() {
        return quantityByLastDay.get();
    }

    /**
     * Остановить потоки созданные для разбора очередей
     */
    public void stopWorkers() {
        minuteCleaner.shutdownNow();
        hourCleaner.shutdownNow();
        dayCleaner.shutdownNow();
    }
}
