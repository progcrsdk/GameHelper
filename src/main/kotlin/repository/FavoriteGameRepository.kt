package org.example.repository

import org.example.entity.FavoriteGame
import org.springframework.data.jpa.repository.JpaRepository

interface FavoriteGameRepository : JpaRepository<FavoriteGame, Long> {
    fun findByTitle(title: String): FavoriteGame?
}