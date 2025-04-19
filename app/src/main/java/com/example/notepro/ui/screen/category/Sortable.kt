package com.example.notepro.ui.screen.category

/**
 * Interface implemented by components that support sorting
 */
interface Sortable {
    fun applySorting(sortType: SortType)
}

/**
 * Enum defining available sort types
 */
enum class SortType {
    NAME, NOTES_COUNT, DATE_CREATED
} 