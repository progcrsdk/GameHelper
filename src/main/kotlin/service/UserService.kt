package org.example.service

import org.example.entity.User
import org.example.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class UserService(private val userRepository: UserRepository) {

    fun registerIfNotExists(telegramId: Long, username: String?, firstName: String?, lastName: String?): User {
        return userRepository.findByTelegramId(telegramId)
            ?: userRepository.save(User(telegramId, username, firstName, lastName))
    }

    fun getUser(telegramId: Long): User? = userRepository.findByTelegramId(telegramId)
}