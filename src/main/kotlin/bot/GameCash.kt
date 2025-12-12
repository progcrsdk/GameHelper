package org.example.bot

import org.example.api.dto.RawgGame
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object GameCache {
    private val cache = ConcurrentHashMap<String, Pair<RawgGame, Long>>()
    private val ttlMillis = TimeUnit.MINUTES.toMillis(5)
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    init {
        scheduler.scheduleAtFixedRate({
            val now = System.currentTimeMillis()
            cache.entries.removeIf { (_, value) -> now - value.second > ttlMillis }
        }, 5, 5, TimeUnit.MINUTES)
    }

    fun put(game: RawgGame): String {
        val uuid = UUID.randomUUID().toString()
        cache[uuid] = game to System.currentTimeMillis()
        return uuid
    }

    fun get(uuid: String): RawgGame? = cache[uuid]?.first
}