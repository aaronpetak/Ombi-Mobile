package com.ombi.mobile.ui.components

/**
 * Acronyms that must stay fully upper-cased in UI labels rather than being
 * title-cased. Without this, an enum entry like `TV` renders as "Tv".
 */
private val ACRONYMS = setOf("TV")

/**
 * Converts an enum entry name into a human-readable label for display.
 *
 * Default behaviour title-cases the name (`MOVIES` → "Movies", `ALL` → "All"),
 * but entries listed in [ACRONYMS] are kept fully upper-cased (`TV` → "TV").
 *
 * Replaces the previous `name.lowercase().replaceFirstChar { it.uppercase() }`
 * idiom, which produced "Tv" for the TV entries.
 */
fun Enum<*>.toDisplayLabel(): String =
    if (name in ACRONYMS) name
    else name.lowercase().replaceFirstChar { it.uppercase() }
