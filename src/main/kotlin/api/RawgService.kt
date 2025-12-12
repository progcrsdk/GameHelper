package org.example.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.example.api.dto.RawgGame
import org.example.bot.genreNameToId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Random

@Service
class RawgService(
    @Value("\${rawg.api.key}") private val rawgApiKey: String
) {
    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper()
    private val baseUrl = "https://api.rawg.io/api"
    private val random = Random()

    /**
     * Возвращает одну случайную игру из всего каталога.
     */
    fun getRandomGame(): RawgGame? {
        // Случайная страница от 1 до 100
        val randomPage = random.nextInt(100) + 1
        val url = "$baseUrl/games?key=$rawgApiKey&page=$randomPage&page_size=1&ordering=-rating"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("User-Agent", "GameMateBot")
            .build()

        return fetchGamesList(request).firstOrNull()
    }

    /**
     * Поиск игр по названию.
     */
    fun searchGamesByName(query: String, limit: Int = 3): List<RawgGame> {
        val encoded = java.net.URLEncoder.encode(query, Charsets.UTF_8)
        val url = "$baseUrl/games?key=$rawgApiKey&search=$encoded&page_size=$limit"
        val request = Request.Builder().url(url).get().build()
        return fetchGamesList(request)
    }

    /**
     * Поиск игр по жанрам — ВОЗВРАЩАЕТ СЛУЧАЙНУЮ ВЫБОРКУ из подходящих игр.
     */
    fun searchGamesByGenres(genres: List<String>, limit: Int = 5): List<RawgGame> {
        if (genres.isEmpty()) return emptyList()

        // Преобразуем слаги в ID жанров (например, "RPG" → 5)
        val genreIds = genres.mapNotNull { slug -> genreNameToId[slug] }.distinct()
        if (genreIds.isEmpty()) return emptyList()

        val idsParam = genreIds.joinToString(",")
        // ← КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: используем случайную страницу!
        val randomPage = random.nextInt(50) + 1
        val url = "$baseUrl/games?key=$rawgApiKey&genres=$idsParam&page=$randomPage&page_size=$limit&ordering=-rating"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("User-Agent", "GameMateBot")
            .build()

        return fetchGamesList(request)
    }

    fun getGameById(id: Int): RawgGame {
        // ВАЖНО: добавьте ?key=... — без него RAWG часто не отдаёт даже метаданные!
        val url = "$baseUrl/games/$id/?key=$rawgApiKey"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("User-Agent", "GameMateBot")
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("HTTP ${response.code}: ${response.message}")
            }
            val body = response.body?.string() ?: throw RuntimeException("Empty body")
            try {
                mapper.readValue(body, RawgGame::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException("JSON parse error", e)
            }
        }
    }

    // ------------------ Вспомогательные методы ------------------

    private fun fetchGamesList(request: Request): List<RawgGame> {
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("RAWG API error: ${response.code} ${response.message}")
                return@use emptyList()
            }
            val body = response.body?.string() ?: return@use emptyList()
            try {
                val node = mapper.readTree(body)
                val results = node["results"] ?: return@use emptyList()
                (0 until results.size()).map {
                    mapper.treeToValue(results[it], RawgGame::class.java)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}