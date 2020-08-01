package ru.educationalwork.news

import io.realm.RealmList
import io.realm.RealmObject

/**
Нужно отделить структуру данных, которую мы получаем от API от структуры для хранения в БД, т.е.
нужно, чтобыполучение данных из БД отделено от отображения сохранённых данных.
FeedAPI - используем при запросе, а FeedRealm - при конвертации в структуру БД.
Особенности под БД Realm:
1) open классы
2) поля var
3) должны быть значения по умолчанию
4) классы наследуются от RealmObject()
5) ArrayList -> RealmList

Сохраняем - в subscribe, запрашиваем при установке адаптера
Такое разделение хорошо с точки зрения архитектуры приложения.
Также позволяет показывать последние сохранённые данные из БД, если возникла ошибка с сетью (во 2-й{} метода subscribe)
 */

open class FeedRealm(
    var items: RealmList<FeedItemRealm> = RealmList<FeedItemRealm>()
) : RealmObject()

open class FeedItemRealm(
    var title: String = "",
    var link: String = "",
    var thumbnail: String = "",
    var description: String = "",
    var guide: String = EnclosureAPI().link
) : RealmObject()

// var enclosure: String = EnclosureAPI().link,