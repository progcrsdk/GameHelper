package org.example.bot

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import org.example.entity.User

fun buildGenreKeyboard(user: User): InlineKeyboardMarkup {
    val genres = listOf(
        "RPG" to "🧙 RPG",
        "шутер" to "🔫 Шутер",
        "стратегия" to "🧠 Стратегия",
        "гонки" to "🏎️ Гонки",
        "хоррор" to "👻 Хоррор",
        "инди" to "🎨 Инди",
        "приключения" to "🗺️ Приключения",
        "платформер" to "🧍 Платформер",
        "спорт" to "⚽ Спорт",
        "симулятор" to "🚜 Симулятор"
    )

    val rows = genres.chunked(2).map { chunk ->
        chunk.map { (slug, name) ->
            val selected = user.favoriteGenres.any { it.genreSlug == slug }
            InlineKeyboardButton.CallbackData(
                text = if (selected) "✅ $name" else name,
                callbackData = "toggle_genre:$slug"
            )
        }
    }.toMutableList()

    rows.add(listOf(InlineKeyboardButton.CallbackData("✅ Готово", "genres_done")))
    return InlineKeyboardMarkup.create(*rows.toTypedArray())
}

fun buildMainMenu(): KeyboardReplyMarkup = KeyboardReplyMarkup(
    keyboard = listOf(
        listOf(KeyboardButton("🎲 Случайная игра"), KeyboardButton("🔍 Поиск по названию")),
        listOf(KeyboardButton("🎨 По предпочтениям"), KeyboardButton("🎮 По настроению")),
        listOf(KeyboardButton("⚙️ Жанры"), KeyboardButton("👤 Профиль"))
    ),
    resizeKeyboard = true
)

val moodToGenres = mapOf(
    "расслабиться" to listOf("инди", "симулятор"),
    "адреналин" to listOf("шутер", "гонки", "хоррор"),
    "погрузиться" to listOf("RPG", "приключения"),
    "думать" to listOf("стратегия", "головоломка"),
    "весело" to listOf("платформер", "спорт")
)

val slugToName = mapOf(
    "RPG" to "RPG",
    "шутер" to "Шутер",
    "стратегия" to "Стратегия",
    "гонки" to "Гонки",
    "хоррор" to "Хоррор",
    "инди" to "Инди",
    "приключения" to "Приключения",
    "платформер" to "Платформер",
    "спорт" to "Спорт",
    "симулятор" to "Симулятор"
)

val genreNameToId = mapOf(
    "RPG" to 5,
    "шутер" to 4,
    "стратегия" to 11,
    "гонки" to 13,
    "хоррор" to 27,
    "инди" to 3,
    "приключения" to 31,
    "платформер" to 83,
    "спорт" to 14,
    "симулятор" to 40,
    "боевик" to 28,
    "головоломка" to 9,
    "музыка" to 7,
    "карточные" to 51,
    "фэнтези" to 4,
    "экшен" to 4
)