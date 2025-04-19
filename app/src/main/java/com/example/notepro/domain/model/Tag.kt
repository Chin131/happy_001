package com.example.notepro.domain.model

/**
 * Domain model for a Tag
 */
data class Tag(
    val id: Long = 0,
    val name: String,
    val color: Int? = null
) 