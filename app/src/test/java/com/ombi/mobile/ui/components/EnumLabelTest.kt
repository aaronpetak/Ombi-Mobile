package com.ombi.mobile.ui.components

import com.ombi.mobile.ui.screens.requests.RequestTab
import com.ombi.mobile.ui.screens.search.SearchFilter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [toDisplayLabel] — the enum-to-label helper used by the Search filter
 * chips and the Requests tab row. Guards the "TV" acronym against being rendered
 * as "Tv" while still title-casing ordinary entries.
 */
class EnumLabelTest {

    @Test
    fun `TV entry stays fully upper-cased`() {
        assertEquals("TV", SearchFilter.TV.toDisplayLabel())
        assertEquals("TV", RequestTab.TV.toDisplayLabel())
    }

    @Test
    fun `ordinary entries are title-cased`() {
        assertEquals("All", SearchFilter.ALL.toDisplayLabel())
        assertEquals("Movies", SearchFilter.MOVIES.toDisplayLabel())
        assertEquals("Movies", RequestTab.MOVIES.toDisplayLabel())
    }
}
