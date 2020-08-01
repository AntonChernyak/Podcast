package ru.educationalwork.news

class FeedAPI (
    val items: ArrayList<FeedItemAPI>
)

class FeedItemAPI(
    val title: String,
    val link: String,
    val thumbnail: String,
    val description: String,
    val enclosure: EnclosureAPI
)

class EnclosureAPI(val link: String = "")
