package org.example.entity

import jakarta.persistence.*

@Entity
@Table(name = "favorite_genres")
data class FavoriteGenre(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val genreName: String,

    @Column(nullable = false)
    val genreSlug: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User
)