package com.rashid;

/**
 * Класс объединяющий несколько событий, которые можно отнести к одному времени.
 *
 * Created by Rashid.Iaraliev on 30.09.17.
 */
public class EventsBucket {
    private long time;
    private int countOfEvents;

    public EventsBucket(long time, int countOfEvents) {
        this.time = time;
        this.countOfEvents = countOfEvents;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getCountOfEvents() {
        return countOfEvents;
    }

    public void setCountOfEvents(int countOfEvents) {
        this.countOfEvents = countOfEvents;
    }

    public void incrementCountOfEvents(){
        countOfEvents++;
    }
}
