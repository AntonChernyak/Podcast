package ru.educationalwork.news

import io.reactivex.Observable
import java.net.HttpURLConnection
import java.net.URL

fun createRequest(url: String): Observable<String> =
    Observable.create<String> {
        // Создаём URL и открываем соединение. Т.к. знаем, что запрос будет http, то результат будет HttpURLConnection
        val urlConnection = URL(url).openConnection() as HttpURLConnection
        try {
            urlConnection.connect() // само обращение в сеть
            // SSLHandshakeException - обновить дату на устройстве

            if (urlConnection.responseCode != HttpURLConnection.HTTP_OK) // проверка результата, что он 200
                it.onError(RuntimeException(urlConnection.responseMessage)) // генерим ошибку
            else {
                val str = urlConnection.inputStream.bufferedReader()
                    .readText() // читаем urlConnection как текст
                it.onNext(str) // отправляем результат дальше по цепочкена обработку
            }
        } finally {
            urlConnection.disconnect()
        }
        // Выбираем потоки. Через subscribeOn выберем поток для исполнения кода (тут io поток),
        // а через observeOn - для выбора потока получения результата (тут в UI потоке)
    }
