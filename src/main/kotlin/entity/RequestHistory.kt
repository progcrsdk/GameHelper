package org.example.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "request_history")
data class RequestHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    val moodOrGenre: String,
    val responseMovieId: Long,
    @ManyToOne @JoinColumn(name = "user_id") val user: User,
    val requestedAt: LocalDateTime = LocalDateTime.now()
)