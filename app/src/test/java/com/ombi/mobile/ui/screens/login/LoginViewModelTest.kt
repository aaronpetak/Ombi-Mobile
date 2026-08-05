package com.ombi.mobile.ui.screens.login

import com.ombi.mobile.data.repository.AuthRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [LoginViewModel]'s success-path state handling.
 *
 * Covers the two fixes in this PR: isLoading is reset on success before
 * onSuccess() is invoked (#5, so a throwing onSuccess cannot lock the button)
 * and the password is cleared from the retained state (#21).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(result: Result<Unit>): Pair<LoginViewModel, AuthRepository> {
        val repo = mockk<AuthRepository>()
        coEvery { repo.loginWithCredentials(any(), any()) } returns result
        return LoginViewModel(repo) to repo
    }

    @Test
    fun `success resets isLoading and clears password`() = runTest(dispatcher) {
        val (vm, _) = viewModel(Result.success(Unit))
        vm.onUsernameChange("alice")
        vm.onPasswordChange("secret")

        vm.loginWithCredentials(onSuccess = {})
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("", vm.uiState.value.password)
    }

    @Test
    fun `failure resets isLoading and surfaces error`() = runTest(dispatcher) {
        val (vm, _) = viewModel(Result.failure(RuntimeException("bad credentials")))
        vm.onUsernameChange("alice")

        vm.loginWithCredentials(onSuccess = {})
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("bad credentials", vm.uiState.value.error)
    }

    @Test
    fun `blank username is rejected without calling the repository`() = runTest(dispatcher) {
        val (vm, _) = viewModel(Result.success(Unit))
        var navigated = false

        vm.loginWithCredentials(onSuccess = { navigated = true })
        advanceUntilIdle()

        assertFalse(navigated)
        assertEquals("Username is required", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }
}
