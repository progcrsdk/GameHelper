package org.example.api.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RawgGame(
    val id: Int,
    val slug: String?,
    val name: String?,
    val name_original: String?,
    val description: String?,
    val description_raw: String?,
    val released: String?,
    val background_image: String?,
    val website: String?,
    val rating: Double?,
    val playtime: Int?,
    val genres: List<RawgGenre>?,
    val platforms: List<RawgPlatformEntry>?,
    val publishers: List<RawgCompany>?,
    val metacritic: Int?,
    val tags: List<RawgTag>?,
    val reddit_description: String?,
    val reddit_name: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RawgGenre(val name: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RawgPlatformEntry(val platform: RawgPlatform?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RawgPlatform(val name: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RawgCompany(val name: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RawgTag(val name: String?)