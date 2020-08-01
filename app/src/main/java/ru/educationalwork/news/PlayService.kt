package ru.educationalwork.news

import android.R.id
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.*


/**
 * НЕ ЗАБЫТЬ ПРОПИСАТЬ СЕРВИС В МАНИФЕСТЕ
 */

class PlayService : Service() {

    private var player: MediaPlayer? = null
    private var notification: NotificationCompat.Builder? = null

    /*
    Сервис может быть либо запущен, либо к нему можно подключиться  (забиндиться)
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Сервис сам по себе работает в галвном потоке (точнее откуда его запускают). Но обычно в UI потоке
     * мы ничего не делаем, т.к. делаем длительные операции
     * В методе onStartCommand (в стартовой точке) мы что-то запускаем и сразу выходим из запуска сервиса.
     * При выходе мы должны вернуть значение, по которому система узнаеткакого типа сервис был запущен.
     * Сервис может быть
     * 1) START_NOT_STICKY --- в случае  убийства сервиса системой, мы его восстанавливать не будем. Он нам не важен.
     * 2) START_STICKY --- сервис должен быть приклеен к системе. И в случае его убийства система должна его снова перезапустить.
     * 3) START_REDELIVER_INTENT --- похож на STICKY, но тут в случае убийства системой интент будет повторён
     * 4) и др.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Можно перехватить iStop, чтобы понять: запустилось ли новое mp3 или пришла команда из нотификации
        if (intent?.action == "stop") {
            // остановим плеер
            player?.stop()
            // выключим нотификацию по id
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(333)
            // остановим сервис
            stopSelf()
            return START_NOT_STICKY
        }

        // Т.к. стартовых команд может приходить много, то старую mp3 мы должны остановить
        player?.stop()

        val url = intent?.extras?.getString("mp3") ?: ""
        player = MediaPlayer()
        player?.setDataSource(this, Uri.parse(url))
        // Чтобы получить результат асинхронно, добавим oNPreparedListener. Когда мы его получаем, то значит
        // prepareAsync произошел и мы готовы проигрывать mp3
        player?.setOnPreparedListener {
            it.start()

            /**
             *  Добавим прогрессбар в нотификацию
             */
            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    // проверим, не остановлен ли плеер
                    if (!it.isPlaying) {
                        timer.cancel()
                        return
                    }
                    // Меняем текст нотификации
                    notification?.setContentText("${it.currentPosition / 1000} sek / ${it.duration / 1000}")
                    // Выводим новый текст, обновив нотификацию
                    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(333, notification?.build())
                }
            }, 1000, 1000)
        }
        // Приготовиться плееру обязательно асинхронно
        player?.prepareAsync()

        /**
         * Далее настройка нотификации
         * https://stackoverflow.com/questions/46990995/on-android-8-1-api-27-notification-does-not-display/49130786
         * https://developer.android.com/training/notify-user/channels
         */
        // интент на активити
        val notificationIntent = Intent(
            this,
            MainActivity::class.java
        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val iStop = Intent(this, PlayService::class.java).setAction("stop")
        // интент на сервис, который указывает на наш плейсервис
        val piStop = PendingIntent.getService(this, 0, iStop, PendingIntent.FLAG_CANCEL_CURRENT)

        /**
         * C 8 Андроида обязательно указывать канал.
         * При этом нужна проверка на версию, т.к. не поддерживается библиотека совместимомти для нотификаций
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = getString(R.string.channel_name)
                      //val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel("1", name, importance)
                       //mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        // Текст, иконка и заголовок --- обязательные поля, чтобы нотификация появилась
        notification = NotificationCompat.Builder(this, "1")
            .setSmallIcon(R.mipmap.ic_launcher) // Иконка
            .setContentTitle("MP3") // Заголовок
            .setContentText("") // Текст.
            // При нажатии на нотификацию нужно открыть активити, укажем путь
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            )
            .addAction(R.mipmap.ic_launcher, "Stop", piStop)
            .setAutoCancel(true) // При нажатии на нотификацию она исчезнет
            .setOngoing(false) // Нужно ли показывать кнопку Cancel

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            333,
            notification?.build()
        )

        return START_NOT_STICKY
    }

    // Вызывается 1 раз --- когда будет создан сервис. Тут настраиваем сервис
    override fun onCreate() {
        super.onCreate()
    }

    // Тут уничтожаем сервис
    override fun onDestroy() {
        player?.stop()
        super.onDestroy()
    }

}