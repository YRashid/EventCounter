package com.rashid;

/**
 * Created by Rashid.Iaraliev on 30.09.17.
 */
public interface EventCounter {

    /**
     * Учесть событие
     *
     * @return true/false - событие учтено/не учтено
     */
    boolean submitEvent();

    /**
     * Выдать число событий за последнюю минуту (60 секунд)
     *
     * @return - количество событий
     */
    long getQuantityByLastMinute();

    /**
     * Выдать число событий за последний час (60 минут)
     *
     * @return - количество событий
     */
    long getQuantityByLastHour();

    /**
     * Выдать число событий за последние сутки (24 часа)
     *
     * @return - количество событий
     */
    long getQuantityByLastDay();
}
