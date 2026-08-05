package com.ombi.mobile.ui.model

import com.ombi.mobile.data.api.models.RequestEngineResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [toRequestOutcome], the request-result mapping shared by
 * HomeViewModel and SearchViewModel (#22). The three branches must preserve the
 * original per-screen behaviour, especially isSuccess=false leaving the selected
 * item untouched.
 */
class RequestOutcomeTest {

    private fun engineResult(result: Boolean, errorMessage: String? = null) =
        RequestEngineResult(
            result = result, message = null, isError = !result,
            errorMessage = errorMessage, requestId = if (result) 1 else null
        )

    @Test
    fun `null result means unresolved id, not success`() {
        val outcome = (null as Result<RequestEngineResult>?).toRequestOutcome()

        assertEquals("Missing ID for request", outcome.message)
        assertFalse(outcome.isSuccess)
        assertFalse(outcome.markRequested)
    }

    @Test
    fun `engine accepted marks requested and succeeds`() {
        val outcome = Result.success(engineResult(result = true)).toRequestOutcome()

        assertEquals("Request submitted!", outcome.message)
        assertTrue(outcome.isSuccess)
        assertTrue(outcome.markRequested)
    }

    @Test
    fun `engine rejection is a successful call but not requested`() {
        val outcome = Result.success(engineResult(result = false, errorMessage = "Already requested"))
            .toRequestOutcome()

        assertEquals("Failed: Already requested", outcome.message)
        assertTrue(outcome.isSuccess)   // the call completed; selectedItem may be rewritten
        assertFalse(outcome.markRequested)
    }

    @Test
    fun `thrown failure is neither success nor requested`() {
        val outcome = Result.failure<RequestEngineResult>(RuntimeException("network down"))
            .toRequestOutcome()

        assertEquals("Error: network down", outcome.message)
        assertFalse(outcome.isSuccess)  // selectedItem must be left untouched
        assertFalse(outcome.markRequested)
    }
}
