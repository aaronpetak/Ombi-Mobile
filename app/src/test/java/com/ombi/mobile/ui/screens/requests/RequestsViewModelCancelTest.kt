package com.ombi.mobile.ui.screens.requests

import com.ombi.mobile.data.api.models.RequestsViewModel as RequestsPage
import com.ombi.mobile.data.api.models.TvParentRequest
import com.ombi.mobile.data.api.models.TvRequest
import com.ombi.mobile.data.repository.OmbiRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [RequestsViewModel.cancelTvRequest], focused on the list-update
 * behaviour after a successful cancel.
 *
 * These lock in that the original filter predicate (`it.parentRequest?.id !=
 * parentRequestId`) is correct: it removes exactly the cancelled parent's
 * children and retains unrelated requests — including those with a null
 * parentRequest. The review's proposed #11 change would have dropped the
 * null-parent item, which these tests would catch as a regression.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RequestsViewModelCancelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun child(id: Int, parentId: Int?) = TvRequest(
        id = id,
        title = "Child $id",
        approved = false,
        denied = null,
        available = false,
        deniedReason = null,
        requestedDate = null,
        requestedUser = null,
        seasonRequests = null,
        parentRequest = parentId?.let {
            TvParentRequest(id = it, title = "Parent $it", posterPath = null, background = null, tvDbId = null)
        }
    )

    /** Builds a ViewModel whose initial load returns [tv], with cancel stubbed to succeed. */
    private fun viewModelWith(tv: List<TvRequest>): RequestsViewModel {
        val repo = mockk<OmbiRepository>(relaxed = true)
        coEvery { repo.getMovieRequests() } returns Result.success(RequestsPage(emptyList(), 0))
        coEvery { repo.getTvRequests() } returns Result.success(RequestsPage(tv, tv.size))
        coEvery { repo.getCurrentUser() } returns Result.failure(RuntimeException("no user"))
        coEvery { repo.cancelTvRequest(any()) } returns Result.success(Unit)
        return RequestsViewModel(repo)
    }

    @Test
    fun `cancel removes only the cancelled parent's children`() = runTest(dispatcher) {
        val vm = viewModelWith(
            listOf(
                child(id = 1, parentId = 42),
                child(id = 2, parentId = 42),
                child(id = 3, parentId = 7)
            )
        )
        advanceUntilIdle() // let init load() complete

        vm.cancelTvRequest(42)
        advanceUntilIdle()

        // Only parent 7's child remains.
        assertEquals(listOf(3), vm.uiState.value.tvRequests.map { it.id })
    }

    @Test
    fun `cancel retains unrelated null-parent requests`() = runTest(dispatcher) {
        val vm = viewModelWith(
            listOf(
                child(id = 1, parentId = 42),   // cancelled
                child(id = 2, parentId = null)  // unrelated, must survive
            )
        )
        advanceUntilIdle()

        vm.cancelTvRequest(42)
        advanceUntilIdle()

        // The null-parent child is NOT belonging to parent 42 and must remain.
        // (The review's proposed #11 fix would have dropped it — regression guard.)
        assertEquals(listOf(2), vm.uiState.value.tvRequests.map { it.id })
    }

    @Test
    fun `cancelling id is cleared after completion`() = runTest(dispatcher) {
        val vm = viewModelWith(listOf(child(id = 1, parentId = 42)))
        advanceUntilIdle()

        vm.cancelTvRequest(42)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.cancellingTvIds.isEmpty())
    }
}
