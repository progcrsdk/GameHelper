package org.example.entity

import jakarta.persistence.*

@Entity
@Table(name = "favorite_games")
data class FavoriteGame(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val gameId: String,
    val title: String,

    @ManyToOne @JoinColumn(name = "user_id")
    val user: User
)