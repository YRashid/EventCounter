# EventCounter

EventCounter - счетчик событий, поддерживающий операции:
  - регистрация события
  - получение количества событий за последний промежуток времени (минута, час, день)
 
# ConcurrentEventCounter
ConcurrentEventCounter это thread-safe реализация EventCounter.
При регистрации нового события, время события записывается в очередь, относящуюся к событиям последней минуты (но не записывается в очереди относящиеся к последнему часу и дню). Одновременно с этим инкрементируются счетчики количества событий за последнюю минуту, час и день. Инкрементируются все, несмотря на то, что событие добавлено только в очередь собтытий последней минуты.

Когда происходит заполнение очереди событий последнего часа и дня:
При вызове конструктора инициализируются три ScheduledExecutorService. 
##### minuteCleaner
Поток создаваемый minuteCleaner проверяет очередь событий последней минуты на появление устаревших событий. Если устаревшие события найдены, то все устаревшие события, произошедшие в течение одной секунды, объединяются в объект класса EventsBucket. Где time - время к которому можно отнести набор событий, countOfEvents - количество объединенных событий. Также все устаревшие события удаляются из очереди последней минуты.
Далее объект EventsBucket с объединенными устаревшими событиями последней минуты помещается в очередь событий последнего часа. При этом счетчик событий последнего часа не изменяется, а счетчик событий последней минуты уменьшается на количество устаревших событий.
##### hourCleaner
Поток создаваемый hourCleaner проверяет очередь событий последнего часа на появление устаревших событий. Все устаревшие события перемещаются в очередь событий последнего дня. При этом счетчик событий последнего дня не изменяется, а счетчик событий последнего часа уменьшается на количество устаревших событий.
##### dayCleaner
Поток создаваемый dayCleaner проверяет очередь событий последнего дня на появление устаревших событий. Все устаревшие события удаляются из очереди. Счетчик событий последнего дня уменьшается на количество устаревших событий.

#### Минусы
Очередь последней минуты содержит время каждого события, без объединения в EventsBucket, как это сделано в очередях для последнего часа и дня. Это может привести к тому, что очередь последней минуты будет содержать большие объемы данных, в случае если за одну минуту придет больше количество событий.
Для исправления этого минуса, нужно создать очередь-буфер из которой объединенные события будут попадать в очередь последней минуты, как это сделано в очередях для последнего часа и дня.