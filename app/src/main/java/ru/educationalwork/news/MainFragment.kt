package ru.educationalwork.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmList
import kotlinx.android.synthetic.main.activity_main.*

// Конструктор всегда (!) без аргументов
class MainFragment: Fragment() {

    var request: Disposable? = null

    // Используется как конструктор класса для инициализации
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    // Создаём View из UI и вернуть его.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_main, container, false)
    }

    // Этот callBack говорит, что все проинициализировано, подсоединено и готово к использованию
    // Тут лучше и начинать нашу деятельность
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // RxJava. Observable - самый используемый класс, у котого Generic - тип возвращаемого значения
        val o =
            createRequest("https://api.rss2json.com/v1/api.json?rss_url=https%3A%2F%2Flhspodcast.info%2Fcategory%2Fpodcast-mp3%2Ffeed%2F")
                // rss даёт результат в виде xml. Необходимо преобразовать в объект json с помощью map --> библиотека GSON
                // it - строка, которую переводим. Feed - наш класс, с теми же полями, что и по ссылке
                // не забыть запросить разрешение INTERNET в манифесте
                .map { Gson().fromJson(it, FeedAPI::class.java) }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

        // Запускаем код. ({} - результат, {} - функция, которая вызывается при ошибке (нет сети например))
        request = o.subscribe({
            val feed = FeedRealm(it.items.mapTo(RealmList<FeedItemRealm>(), { feed -> FeedItemRealm(feed.title, feed.link, feed.thumbnail, feed.description, feed.enclosure.link) }))

            // Записываем в БД Realm
            Realm.getDefaultInstance().executeTransaction { realm ->
                // Если в БД были старые данные, то стираем их, чтобы не хранить мусор
                // Для начала найдём FeedList
                val oldList = realm.where(FeedRealm::class.java).findAll()
                //Есть он есть, то удаляем элементы
                if (oldList.size > 0) oldList.map { oldItem -> oldItem.deleteFromRealm() }
                // Теперь записываем элементы
                realm.copyToRealm(feed)
            }

            showRecView()
        }, {
            // Если нет сети, то он покажет последние данные из БД
            showRecView()
        })

    }

    fun showRecView() {
        // Запрашиваем данные из БД
        Realm.getDefaultInstance().executeTransaction {realm ->
            if (!isVisible) return@executeTransaction
            val feed = realm.where(FeedRealm::class.java).findAll()
            // Т.к. максимум в БД 1 элемент, т.к. старые feed мы стираем, то можем обратиться сразу к 1-му эл-ту
            if (feed.size > 0){
                // Наполняем адаптер
                recyclerView.adapter = RecyclerAdapter(feed[0]!!.items)
                // Задаём вид
                /**
                 * Контекст для фрагмента берём из активити, но! может вернуться null. Лучше даже делать
                 * не проверку активити на null, а проверку фрагмента isVisible (т.к. там перебрали много вариантов,
                 * когда фрагмент может быть не видимым). Лучше ее делать всегда, когда работаем с многопоточностью
                 *
                 * Хотя поступило замечание: В данном примере isVisible вызывается в onActivityCreate(),
                 * но видимым фрагмент становится после вызова onStart(), onCreateActivity() вызывается до onStart(),
                 * т.е. довольно часто выходит ситуация, когда isVisible==false, а activity!=null,
                 * проверка на isVisible не корректна в данном случае
                 */
                recyclerView.layoutManager = LinearLayoutManager(activity)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // предотвращает утечку памяти (как в AsyncTask). Обрывает цепочку, удаляется ссылка на subscribe и ссылка на activity
        // всё чистит сборщик мусора
        request?.dispose()
    }
}