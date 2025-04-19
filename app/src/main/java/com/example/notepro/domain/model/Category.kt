package com.example.notepro.domain.model

import java.util.Date

/**
 * Domain model for a Category
 */
data class Category(
    val id: Long = 0,
    val name: String,
    val color: Int? = null,
    val icon: String? = null,
    val createdAt: Long = Date().time,
    val modifiedAt: Long = Date().time
) 