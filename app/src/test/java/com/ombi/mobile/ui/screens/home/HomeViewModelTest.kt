package com.ombi.mobile.ui.screens.home

import com.ombi.mobile.data.api.models.RecentlyAddedMovie
import com.ombi.mobile.data.api.models.RequestEngineResult
import com.ombi.mobile.data.repository.OmbiRepository
import com.ombi.mobile.ui.model.MediaItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Tests for [HomeViewModel] covering the load() error handling (#18) and the
 * request double-tap guard (#15).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** A repository whose five discovery rows all succeed with empty lists. */
    private fun successRepo() = mockk<OmbiRepository>().apply {
        coEvery { getRecentlyAddedMovies() } returns Result.success(emptyList())
        coEvery { getRecentlyAddedTv() } returns Result.success(emptyList())
        coEvery { getPopularMovies() } returns Result.success(emptyList())
        coEvery { getTrendingTv() } returns Result.success(emptyList())
        coEvery { getUpcomingMovies() } returns Result.success(emptyList())
    }

    @Test
    fun `all rows failing surfaces an error and clears loading`() = runTest(dispatcher) {
        val repo = mockk<OmbiRepository>()
        val boom = RuntimeException("network down")
        coEvery { repo.getRecentlyAddedMovies() } returns Result.failure(boom)
        coEvery { repo.getRecentlyAddedTv() } returns Result.failure(boom)
        coEvery { repo.getPopularMovies() } returns Result.failure(boom)
        coEvery { repo.getTrendingTv() } returns Result.failure(boom)
        coEvery { repo.getUpcomingMovies() } returns Result.failure(boom)

        val vm = HomeViewModel(repo)
        advanceUntilIdle()

        assertEquals("network down", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `partial failure shows no error`() = runTest(dispatcher) {
        val repo = successRepo()
        coEvery { repo.getPopularMovies() } returns Result.failure(RuntimeException("one row"))

        val vm = HomeViewModel(repo)
        advanceUntilIdle()

        assertEquals(null, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `duplicate ids in a row are deduped so LazyRow keys stay unique`() = runTest(dispatcher) {
        // Ombi's recentlyadded endpoint can return the same title twice; the row keys
        // on it.id, so a duplicate id would crash Compose ("Key … was already used").
        val repo = successRepo()
        fun movie(id: Int) = RecentlyAddedMovie(
            id = id, theMovieDbId = id, imdbId = null, title = "M$id",
            posterPath = null, overview = null, addedAt = null
        )
        coEvery { repo.getRecentlyAddedMovies() } returns
            Result.success(listOf(movie(610909), movie(610909), movie(42)))

        val vm = HomeViewModel(repo)
        advanceUntilIdle()

        val ids = vm.uiState.value.recentMovies.map { it.id }
        assertEquals(listOf(610909, 42), ids)
    }

    @Test
    fun `double-tap request submits only one call`() = runTest(dispatcher) {
        val repo = successRepo()
        // Hold the request in flight so the second tap arrives while isRequesting is true.
        val gate = CompletableDeferred<RequestEngineResult>()
        coEvery { repo.requestMovie(any()) } coAnswers { Result.success(gate.await()) }

        val vm = HomeViewModel(repo)
        advanceUntilIdle() // finish initial load

        vm.selectItem(
            MediaItem(
                title = "A movie", posterPath = null, overview = null, year = null,
                rating = null, isMovie = true, theMovieDbId = 42, tvDbId = null,
                available = false, requested = false, approved = false
            )
        )

        vm.requestSelected()
        vm.requestSelected() // second tap while the first is in flight
        advanceUntilIdle()

        gate.complete(RequestEngineResult(result = true, message = null, isError = false, errorMessage = null, requestId = 1))
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.requestMovie(42) }
        assertNotNull(vm.uiState.value.requestMessage)
    }
}
