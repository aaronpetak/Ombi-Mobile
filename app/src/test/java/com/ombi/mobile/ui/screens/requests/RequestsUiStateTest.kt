package com.ombi.mobile.ui.screens.requests

import com.ombi.mobile.data.api.models.MovieRequest
import com.ombi.mobile.data.api.models.TvRequest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the derived list properties on [RequestsUiState].
 *
 * Pure Kotlin — no Android or coroutine dependencies. These pin down the
 * pending-vs-processed split, focusing on the `denied` null-vs-true boundary
 * the code review flagged: pending = not available AND denied != true;
 * processed = available OR denied == true. A denied value of `null` (unset)
 * must count as pending, not processed.
 */
class RequestsUiStateTest {

    private fun movie(
        id: Int,
        available: Boolean = false,
        approved: Boolean = false,
        denied: Boolean? = null
    ) = MovieRequest(
        id = id,
        title = "Movie $id",
        posterPath = null,
        overview = null,
        releaseDate = null,
        approved = approved,
        denied = denied,
        available = available,
        deniedReason = null,
        requestedDate = null,
        requestedUser = null
    )

    private fun tv(
        id: Int,
        available: Boolean = false,
        approved: Boolean = false,
        denied: Boolean? = null
    ) = TvRequest(
        id = id,
        title = "Show $id",
        approved = approved,
        denied = denied,
        available = available,
        deniedReason = null,
        requestedDate = null,
        requestedUser = null,
        seasonRequests = null,
        parentRequest = null
    )

    // ── Movies: pending vs processed ────────────────────────────────────────

    @Test
    fun `pending movie is neither available nor denied`() {
        val state = RequestsUiState(movieRequests = listOf(movie(1)))
        assertEquals(listOf(1), state.pendingMovies.map { it.id })
        assertEquals(emptyList<Int>(), state.processedMovies.map { it.id })
    }

    @Test
    fun `available movie is processed not pending`() {
        val state = RequestsUiState(movieRequests = listOf(movie(1, available = true)))
        assertEquals(emptyList<Int>(), state.pendingMovies.map { it.id })
        assertEquals(listOf(1), state.processedMovies.map { it.id })
    }

    @Test
    fun `denied-true movie is processed not pending`() {
        val state = RequestsUiState(movieRequests = listOf(movie(1, denied = true)))
        assertEquals(emptyList<Int>(), state.pendingMovies.map { it.id })
        assertEquals(listOf(1), state.processedMovies.map { it.id })
    }

    @Test
    fun `denied-false movie counts as pending`() {
        val state = RequestsUiState(movieRequests = listOf(movie(1, denied = false)))
        assertEquals(listOf(1), state.pendingMovies.map { it.id })
        assertEquals(emptyList<Int>(), state.processedMovies.map { it.id })
    }

    @Test
    fun `denied-null movie counts as pending`() {
        val state = RequestsUiState(movieRequests = listOf(movie(1, denied = null)))
        assertEquals(listOf(1), state.pendingMovies.map { it.id })
        assertEquals(emptyList<Int>(), state.processedMovies.map { it.id })
    }

    @Test
    fun `approved-but-not-available movie is still pending`() {
        val state = RequestsUiState(movieRequests = listOf(movie(1, approved = true)))
        assertEquals(listOf(1), state.pendingMovies.map { it.id })
        assertEquals(emptyList<Int>(), state.processedMovies.map { it.id })
    }

    @Test
    fun `movies split into pending and processed buckets`() {
        val state = RequestsUiState(
            movieRequests = listOf(
                movie(1),                        // pending
                movie(2, available = true),      // processed
                movie(3, denied = true),         // processed
                movie(4, approved = true)        // pending (approved, not yet available)
            )
        )
        assertEquals(listOf(1, 4), state.pendingMovies.map { it.id })
        assertEquals(listOf(2, 3), state.processedMovies.map { it.id })
    }

    // ── TV: same predicate applied to TvRequest ─────────────────────────────

    @Test
    fun `denied-null tv counts as pending`() {
        val state = RequestsUiState(tvRequests = listOf(tv(1, denied = null)))
        assertEquals(listOf(1), state.pendingTv.map { it.id })
        assertEquals(emptyList<Int>(), state.processedTv.map { it.id })
    }

    @Test
    fun `available tv is processed`() {
        val state = RequestsUiState(tvRequests = listOf(tv(1, available = true)))
        assertEquals(emptyList<Int>(), state.pendingTv.map { it.id })
        assertEquals(listOf(1), state.processedTv.map { it.id })
    }

    // ── visibleMovies / visibleTv follow the status tab ─────────────────────

    @Test
    fun `visibleMovies reflects pending tab`() {
        val state = RequestsUiState(
            movieRequests = listOf(movie(1), movie(2, available = true)),
            selectedStatus = StatusTab.PENDING
        )
        assertEquals(listOf(1), state.visibleMovies.map { it.id })
    }

    @Test
    fun `visibleMovies reflects processed tab`() {
        val state = RequestsUiState(
            movieRequests = listOf(movie(1), movie(2, available = true)),
            selectedStatus = StatusTab.PROCESSED
        )
        assertEquals(listOf(2), state.visibleMovies.map { it.id })
    }

    @Test
    fun `visibleTv reflects the selected status tab`() {
        val requests = listOf(tv(1), tv(2, available = true))
        assertEquals(
            listOf(1),
            RequestsUiState(tvRequests = requests, selectedStatus = StatusTab.PENDING).visibleTv.map { it.id }
        )
        assertEquals(
            listOf(2),
            RequestsUiState(tvRequests = requests, selectedStatus = StatusTab.PROCESSED).visibleTv.map { it.id }
        )
    }
}
