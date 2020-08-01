package ru.educationalwork.news

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmList
import kotlinx.android.synthetic.main.activity_fragment.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Тут теперь разметка фрагмента!!!
        setContentView(R.layout.activity_fragment)
        // Инициализация БД Realm
        Realm.init(this)

        /**
         * Если мы что-то меняем структура данных, хранящихся в Realm, мы создаём себе проблемы совместимости со старыми версиями.
         * Нужно делать миграцию старых данных в новые, иначе всё упадёт. Но т.к. тут Realm используется только как кеш, то для этого
         * напишем конфигурацию, которая скажет удалять весь Realm, если нужна миграция
         */
        val realmConfig = RealmConfiguration.Builder()
            .schemaVersion(1)
            .deleteRealmIfMigrationNeeded()
            .build()
        Realm.setDefaultConfiguration(realmConfig)

        /**
         * Важно --- не передавать параметры через конструктор. Только через Bundle
         * val bundle = Bundle()
         * bundle.putString("key", "value")
         * val fragment = MainFragment()
         * fragment.arguments = bundle
         * Теперь вместо MainFragment() передаём fragment
         *
         * Данные доставать лучше в onCreate фрагмента:
         * val key = arguments.getString("key")
         */

        /**
         * Т.к. commit не происходит сразу, то если между/во время этой операции, если что-то случиться
         * (кто-то позвонит и приложение перейдёт в onPause), то будут проблемы и падения
         * Варианты
         * 1) try-catch, но что делать при такой проблеме - не понятно
         * 2) this@MainActivity.supportFragmentManager.executePendingTransactions() --- коммит обязан будет
         *      произойти здесь и сейчас, но добавляет тормозов и падает при вложенных фрагментах
         * 3) ЛУЧШЕ! Вместо commit() использовать commitAllowingStateLoss() --- в плохом случае мы получим
         *      неопределённое состояние, когда коммит просто потеряется.
         */
        // Система, если происходит восстановление активити (savedInstanceState != null), сама создаст Fragment
        if (savedInstanceState == null)
            this@MainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_place, MainFragment()).commitAllowingStateLoss()

    }

    fun showArticle(url: String) {
        val bundle = Bundle()
        bundle.putString("url", url)
        val fragment = WebBrowserFragment()
        fragment.arguments = bundle

        if (fragment_place_webView != null) {
            fragment_place_webView?.visibility = View.VISIBLE
            this.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_place_webView, fragment).commitAllowingStateLoss()
        } else {
            /**
             * addToBackStack("name") - позволяет упорядочить фрагменты, и сделать так,
             * чтобы наш фрагмент закрывался по нажатию кнопки назад
             */
            this.supportFragmentManager.beginTransaction().add(R.id.fragment_place, fragment)
                .addToBackStack("main").commitAllowingStateLoss()
        }
    }

    // запускает сервис и передаёт в него url на mp3
    fun playMusic(url: String) {
        val intent = Intent(this, PlayService::class.java)
        intent.putExtra("mp3", url)
        startService(intent)
    }

}