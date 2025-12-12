package org.example.repository

import org.example.entity.FavoriteGenre
import org.springframework.data.jpa.repository.JpaRepository

interface FavoriteGenreRepository : JpaRepository<FavoriteGenre, Long> {
    fun deleteByUserAndGenreSlug(user: org.example.entity.User, genreSlug: String)
}