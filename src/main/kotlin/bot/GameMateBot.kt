package org.example.bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import jakarta.annotation.PostConstruct
import org.example.api.RawgService
import org.example.api.dto.RawgGame
import org.example.entity.FavoriteGame
import org.example.entity.FavoriteGenre
import org.example.entity.User
import org.example.repository.FavoriteGameRepository
import org.example.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GameMateBot(
    private val userRepository: UserRepository,
    private val favoriteGameRepository: FavoriteGameRepository,
    private val rawgService: RawgService,
    @Value("\${telegram.bot.token}") private val botToken: String
) {
    private lateinit var bot: Bot
    private val waitingForGameName = mutableSetOf<Long>()

    @PostConstruct
    fun start() {
        bot = bot {
            token = botToken
            logLevel = LogLevel.Error
            dispatch {
                command("start") {
                    val chatId = message.chat.id
                    val telegramUser = message.from!!
                    userRepository.findByTelegramId(telegramUser.id)
                        ?: userRepository.save(
                            User(
                                telegramId = telegramUser.id,
                                username = telegramUser.username,
                                firstName = telegramUser.firstName,
                                lastName = telegramUser.lastName
                            )
                        )
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "🎮 Привет, ${telegramUser.firstName}!\n\nЯ — *GameMate*, бот для подбора игр по настроению и жанрам!",
                        parseMode = ParseMode.MARKDOWN,
                        replyMarkup = buildMainMenu()
                    )
                }

                callbackQuery {
                    val data = callbackQuery.data
                    val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                    val telegramUser = callbackQuery.from
                    val user = userRepository.findByTelegramId(telegramUser.id) ?: return@callbackQuery

                    when {
                        data.startsWith("add_fav_uuid:") -> {
                            val uuid = data.removePrefix("add_fav_uuid:")
                            val game = GameCache.get(uuid)
                            if (game != null) {
                                val gameName = game.name ?: "Игра без названия"
                                val existing = favoriteGameRepository.findByTitle(gameName)
                                if (existing != null) {
                                    bot.sendMessage(
                                        chatId = ChatId.fromId(chatId),
                                        text = "⛔ Игра '$gameName' уже в избранном!"
                                    )
                                } else {
                                    favoriteGameRepository.save(
                                        FavoriteGame(
                                            gameId = game.id.toString(),
                                            title = gameName, // ← теперь точно String
                                            user = user
                                        )
                                    )
                                    bot.sendMessage(
                                        chatId = ChatId.fromId(chatId),
                                        text = "❤️ Добавлено!"
                                    )
                                }
                            }
                        }

                        data.startsWith("mood:") -> {
                            val mood = data.removePrefix("mood:")
                            val genres = moodToGenres[mood] ?: emptyList()
                            bot.sendMessage(chatId = ChatId.fromId(chatId), text = "⏳ Ищу под настроение *$mood*...", parseMode = ParseMode.MARKDOWN)
                            val games = rawgService.searchGamesByGenres(genres)
                            if (games.isEmpty()) {
                                bot.sendMessage(chatId = ChatId.fromId(chatId), text = "😔 Не нашёл игр.")
                            } else {
                                games.take(3).forEach { sendGameMessage(chatId, it) }
                            }
                        }

                        data.startsWith("toggle_genre:") -> {
                            val slug = data.removePrefix("toggle_genre:")
                            val genreName = slugToName[slug] ?: slug
                            val existing = user.favoriteGenres.find { it.genreSlug == slug }
                            if (existing != null) {
                                user.favoriteGenres.remove(existing)
                            } else {
                                user.favoriteGenres.add(FavoriteGenre(genreName = genreName, genreSlug = slug, user = user))
                            }
                            userRepository.save(user)
                            bot.editMessageReplyMarkup(
                                chatId = ChatId.fromId(chatId),
                                messageId = callbackQuery.message?.messageId,
                                replyMarkup = buildGenreKeyboard(user)
                            )
                        }

                        data == "genres_done" -> {
                            bot.editMessageText(
                                chatId = ChatId.fromId(chatId),
                                messageId = callbackQuery.message?.messageId,
                                text = "✅ Готово!",
                                replyMarkup = null
                            )
                        }
                    }
                }

                text {
                    val chatId = message.chat.id
                    val text = message.text ?: return@text
                    val telegramUser = message.from!!

                    if (text.startsWith("/")) return@text

                    userRepository.findByTelegramId(telegramUser.id)
                        ?: userRepository.save(User(telegramId = telegramUser.id, username = telegramUser.username))

                    when {
                        text == "🔍 Поиск по названию" -> {
                            waitingForGameName.add(chatId)
                            bot.sendMessage(chatId = ChatId.fromId(chatId), text = "Введите название игры:")
                        }

                        chatId in waitingForGameName -> {
                            waitingForGameName.remove(chatId)
                            val games = rawgService.searchGamesByName(text.trim())
                            if (games.isEmpty()) {
                                bot.sendMessage(chatId = ChatId.fromId(chatId), text = "😔 Не найдено.")
                            } else {
                                games.take(3).forEach { sendGameMessage(chatId, it) }
                            }
                        }

                        text == "⚙️ Жанры" -> {
                            val user = userRepository.findByTelegramId(telegramUser.id)
                                ?: userRepository.save(User(telegramId = telegramUser.id))
                            bot.sendMessage(chatId = ChatId.fromId(chatId), text = "Выберите жанры:", replyMarkup = buildGenreKeyboard(user))
                        }

                        text == "🎨 По предпочтениям" -> {
                            val user = userRepository.findByTelegramId(telegramUser.id)
                            val genres = user?.favoriteGenres?.map { it.genreSlug } ?: emptyList()
                            if (genres.isEmpty()) {
                                bot.sendMessage(chatId = ChatId.fromId(chatId), text = "Сначала выберите жанры в меню 'Жанры'.")
                            } else {
                                bot.sendMessage(chatId = ChatId.fromId(chatId), text = "⏳ Ищу по вашим жанрам...")
                                val games = rawgService.searchGamesByGenres(genres)
                                if (games.isEmpty()) {
                                    bot.sendMessage(chatId = ChatId.fromId(chatId), text = "😔 Не нашёл игр.")
                                } else {
                                    games.take(3).forEach { sendGameMessage(chatId, it) }
                                }
                            }
                        }

                        text == "🎮 По настроению" -> {
                            bot.sendMessage(
                                chatId = ChatId.fromId(chatId),
                                text = "Выберите настроение:",
                                replyMarkup = InlineKeyboardMarkup.create(
                                    moodToGenres.keys.chunked(2).map { row ->
                                        row.map { mood ->
                                            InlineKeyboardButton.CallbackData(
                                                text = when (mood) {
                                                    "расслабиться" -> "🧘 Расслабиться"
                                                    "адреналин" -> "💥 Адреналин"
                                                    "погрузиться" -> "🌌 Погрузиться"
                                                    "думать" -> "🧠 Думать"
                                                    "весело" -> "😄 Весело"
                                                    else -> mood
                                                },
                                                callbackData = "mood:$mood"
                                            )
                                        }
                                    }
                                )
                            )
                        }

                        text == "🎲 Случайная игра" -> {
                            val game = rawgService.getRandomGame()
                            if (game != null) sendGameMessage(chatId, game)
                            else bot.sendMessage(chatId = ChatId.fromId(chatId), text = "Не удалось получить игру.")
                        }

                        text == "👤 Профиль" -> {
                            val user = userRepository.findByTelegramId(telegramUser.id)
                            val name = "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim().ifEmpty { "Аноним" }
                            val genres = user?.favoriteGenres?.joinToString(", ") { it.genreName } ?: "Не выбраны"
                            val favs = user?.favorites?.joinToString("\n") { "• ${it.title}" } ?: "Пусто"
                            bot.sendMessage(
                                chatId = ChatId.fromId(chatId),
                                text = "👤 *Профиль*\n📛 Имя: *$name*\n🎮 Жанры: $genres\n❤️ Избранное:\n$favs",
                                parseMode = ParseMode.MARKDOWN
                            )
                        }

                        else -> {
                            bot.sendMessage(
                                chatId = ChatId.fromId(chatId),
                                text = "🎮 Используйте меню!",
                                replyMarkup = buildMainMenu()
                            )
                        }
                    }
                }
            }
        }
        bot.startPolling()
    }

    private fun sendGameMessage(chatId: Long, game: RawgGame) {
        val gameName = game.name ?: "Неизвестная игра"

        val fullGame = try {
            if (game.id > 0) {
                rawgService.getGameById(game.id)
            } else {
                game
            }
        } catch (e: Exception) {
            game
        }

        val uuid = GameCache.put(fullGame)
        val cover = fullGame.background_image
            ?: "https://via.placeholder.com/300x400?text=No+Cover"

        val finalDescription = listOf(
            fullGame.description_raw,
            fullGame.description,
            fullGame.reddit_description
        )
            .firstOrNull { it != null && it.isNotBlank() && it.length > 20 }
            ?.replace("<[^>]*>".toRegex(), "")
            ?.replace("\\s+".toRegex(), " ")
            ?.trim()
            ?.take(500)
            ?.let { "About\n\n$it" }
            ?: run {
                buildString {
                    val genres = fullGame.genres
                        ?.mapNotNull { it.name }
                        ?.joinToString(", ")
                    if (!genres.isNullOrBlank()) append("🎭 Жанры: $genres\n")

                    val tags = fullGame.tags
                        ?.mapNotNull { it.name }
                        ?.filter { it.lowercase() !in listOf("singleplayer", "multiplayer") }
                        ?.take(4)
                        ?.joinToString(", ")
                    if (!tags.isNullOrBlank()) append("🔖 Особенности: $tags\n")

                    if (fullGame.playtime != null && fullGame.playtime > 0) {
                        append("⏱ Время прохождения: ~${fullGame.playtime} ч")
                    }
                }.ifBlank { "Подробности недоступны." }
            }

        val rating = fullGame.rating?.let { "⭐ ${"%.1f".format(it)}" } ?: "—"
        val platforms = fullGame.platforms
            ?.mapNotNull { it.platform?.name }
            ?.distinct()
            ?.take(3)
            ?.joinToString(", ") ?: "—"

        val aboutText = buildString {
            append("🎮 *$gameName*\n")
            append("$rating • 💻 $platforms\n\n")
            append(finalDescription)
        }

        bot.sendPhoto(
            chatId = ChatId.fromId(chatId),
            photo = TelegramFile.ByUrl(cover),
            caption = aboutText,
            parseMode = ParseMode.MARKDOWN,
            replyMarkup = InlineKeyboardMarkup.create(
                listOf(
                    InlineKeyboardButton.CallbackData("❤️ В избранное", "add_fav_uuid:$uuid")
                )
            )
        )
    }
}