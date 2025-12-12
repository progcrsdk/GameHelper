package org.example.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id val telegramId: Long,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    val favorites: MutableList<FavoriteGame> = mutableListOf(),
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val history: MutableList<RequestHistory> = mutableListOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    val favoriteGenres: MutableList<FavoriteGenre> = mutableListOf()
)