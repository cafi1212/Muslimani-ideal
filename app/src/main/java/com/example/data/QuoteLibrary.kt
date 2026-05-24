package com.example.data

import kotlin.math.abs

data class Quote(
    val id: String,
    val text: String,
    val arabic: String = "",
    val source: String,
    val category: String // "Quran", "Hadith", "Wisdom"
)

object QuoteLibrary {
    val quotes = listOf(
        // QURAN
        Quote(
            id = "quran_1",
            text = "Indeed, with hardship [will be] ease. Yes, with hardship [will be] ease.",
            arabic = "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            source = "Surah Ash-Sharh [94:5-6]",
            category = "Quran"
        ),
        Quote(
            id = "quran_2",
            text = "My mercy encompasses all things.",
            arabic = "وَرَحْمَتِي وَسِعَتْ كُلَّ شَيْءٍ",
            source = "Surah Al-A'raf [7:156]",
            category = "Quran"
        ),
        Quote(
            id = "quran_3",
            text = "So remember Me; I will remember you. And be grateful to Me and do not deny Me.",
            arabic = "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
            source = "Surah Al-Baqarah [2:152]",
            category = "Quran"
        ),
        Quote(
            id = "quran_4",
            text = "And He found you lost and guided [you].",
            arabic = "وَوَجَدَكَ ضَالًّا فَهَدَىٰ",
            source = "Surah Ad-Duha [93:7]",
            category = "Quran"
        ),
        // HADITH
        Quote(
            id = "hadith_1",
            text = "The most beloved of deeds to Allah are those that are most consistent, even if they are small.",
            arabic = "أَحَبُّ الأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ",
            source = "Sahih al-Bukhari",
            category = "Hadith"
        ),
        Quote(
            id = "hadith_2",
            text = "Verily, actions are judged by intentions, and every person will have only what they intended.",
            arabic = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ",
            source = "Sahih al-Bukhari & Muslim",
            category = "Hadith"
        ),
        Quote(
            id = "hadith_3",
            text = "Be in this world as if you were a stranger or a traveler.",
            arabic = "كُنْ فِي الدُّنْيَا كَأَنَّكَ غَرِيبٌ أَوْ عَابِرُ سَبِيلٍ",
            source = "Sahih al-Bukhari",
            category = "Hadith"
        ),
        Quote(
            id = "hadith_4",
            text = "No fatigue, nor disease, nor sorrow, nor sadness, nor hurt, nor distress befalls a Muslim, even if it were the prick of a thorn, but that Allah expiates some of his sins for it.",
            arabic = "مَا يُصِيبُ الْمُسْلِمَ مِنْ نَصَبٍ وَلاَ وَصَبٍ وَلاَ هَمٍّ وَلاَ حُزْنٍ... إِلاَّ كَفَّارَةٌ",
            source = "Sahih al-Bukhari",
            category = "Hadith"
        ),
        // WISDOM
        Quote(
            id = "wisdom_1",
            text = "O soul, you are not a body finding a spirit. You are a spirit making itself a body.",
            source = "Imam Al-Ghazali",
            category = "Wisdom"
        ),
        Quote(
            id = "wisdom_2",
            text = "Do not show pleasure at your brother's misfortune lest Allah have mercy upon him and afflict you.",
            source = "Hasan al-Basri",
            category = "Wisdom"
        ),
        Quote(
            id = "wisdom_3",
            text = "Busy yourself with your own faults, and you will have no time to notice the faults of others.",
            source = "Imam Ash-Shafi'i",
            category = "Wisdom"
        ),
        Quote(
            id = "wisdom_4",
            text = "The tongue is like a lion; if you let it loose, it will wound someone.",
            source = "Ali ibn Abi Talib (R.A)",
            category = "Wisdom"
        )
    )

    fun getQuoteOfTheDay(timestampMs: Long): Quote {
        // Deterministically select a quote depending on the current day index
        val dayIndex = (timestampMs / (1000 * 60 * 60 * 24)).toInt()
        val index = abs(dayIndex) % quotes.size
        return quotes[index]
    }
}
