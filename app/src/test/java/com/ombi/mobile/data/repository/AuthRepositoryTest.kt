package com.ombi.mobile.data.repository

import com.ombi.mobile.data.api.OmbiApiService
import com.ombi.mobile.data.auth.AuthManager
import com.ombi.mobile.data.auth.SessionEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [AuthRepository]'s session handling.
 *
 * Guards the #8 fix: logout must go through [AuthManager.endSession] so a
 * LOGGED_OUT event is emitted (which the nav collector uses to tear down the
 * main screen and cancel in-flight coroutines), not the old clearToken-only
 * path that left navigation and cancellation unhandled.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    @Test
    fun `logout ends the session with LOGGED_OUT`() {
        val authManager = mockk<AuthManager>(relaxed = true)
        val repo = AuthRepository(mockk<OmbiApiService>(), authManager)

        repo.logout()

        verify(exactly = 1) { authManager.endSession(SessionEvent.LOGGED_OUT) }
    }

    @Test
    fun `sessionEvents delegates to AuthManager`() = runTest {
        val flow = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
        val authManager = mockk<AuthManager> { every { sessionEvents } returns flow }
        val repo = AuthRepository(mockk<OmbiApiService>(), authManager)

        flow.tryEmit(SessionEvent.EXPIRED)

        assertEquals(SessionEvent.EXPIRED, repo.sessionEvents.first())
    }
}
