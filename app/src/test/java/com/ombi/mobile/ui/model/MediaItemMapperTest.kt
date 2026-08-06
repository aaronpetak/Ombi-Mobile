package com.ombi.mobile.ui.model

import com.ombi.mobile.data.api.models.MultiSearchResult
import com.ombi.mobile.data.api.models.RecentlyAddedMovie
import com.ombi.mobile.data.api.models.RecentlyAddedTv
import com.ombi.mobile.data.api.models.SearchMovieViewModel
import com.ombi.mobile.data.api.models.SearchTvShowViewModel
import com.ombi.mobile.data.api.models.TvImages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the [MediaItem] mappers in MediaItem.kt.
 *
 * Pure Kotlin — no Android framework or coroutine dependencies. These cover the
 * edge cases flagged in the code review: `id.toIntOrNull()` for numeric vs.
 * non-numeric multi-search IDs, `releaseDate.take(4)` year extraction, null-title
 * coalescing, the TV poster/backdrop field choice, and per-source availability.
 */
class MediaItemMapperTest {

    // ── SearchMovieViewModel ────────────────────────────────────────────────

    private fun movie(
        id: Int = 100,
        title: String? = "The Matrix",
        releaseDate: String? = "1999-03-31",
        posterPath: String? = "/poster.jpg",
        voteAverage: Double? = 8.7,
        available: Boolean = false,
        requested: Boolean = false,
        approved: Boolean = false
    ) = SearchMovieViewModel(
        id = id,
        title = title,
        releaseDate = releaseDate,
        posterPath = posterPath,
        backdropPath = "/backdrop.jpg",
        voteAverage = voteAverage,
        overview = "overview",
        imdbId = "tt0133093",
        available = available,
        requested = requested,
        approved = approved,
        plexUrl = null,
        quality = null
    )

    @Test
    fun `movie maps id to theMovieDbId and leaves tvDbId null`() {
        val item = movie(id = 603).toMediaItem()
        assertEquals(603, item.theMovieDbId)
        assertNull(item.tvDbId)
        assertTrue(item.isMovie)
    }

    @Test
    fun `movie derives four-digit year from releaseDate`() {
        assertEquals("1999", movie(releaseDate = "1999-03-31").toMediaItem().year)
    }

    @Test
    fun `movie year is null when releaseDate is null`() {
        assertNull(movie(releaseDate = null).toMediaItem().year)
    }

    @Test
    fun `movie null title coalesces to empty string`() {
        assertEquals("", movie(title = null).toMediaItem().title)
    }

    @Test
    fun `movie uses posterPath not backdrop and copies status flags`() {
        val item = movie(posterPath = "/p.jpg", available = true, requested = true, approved = true).toMediaItem()
        assertEquals("/p.jpg", item.posterPath)
        assertTrue(item.available)
        assertTrue(item.requested)
        assertTrue(item.approved)
    }

    // ── SearchTvShowViewModel ───────────────────────────────────────────────

    private fun tv(
        id: Int = 200,
        theMovieDbId: Int? = 1399,
        title: String? = "Game of Thrones",
        firstAired: String? = "2011-04-17",
        posterPath: String? = "/tv-poster.jpg",
        backdropPath: String? = "/tv-backdrop.jpg",
        images: TvImages? = null,
        rating: Double? = 9.3
    ) = SearchTvShowViewModel(
        id = id,
        theMovieDbId = theMovieDbId,
        title = title,
        firstAired = firstAired,
        banner = "/banner.jpg",
        posterPath = posterPath,
        backdropPath = backdropPath,
        images = images,
        rating = rating,
        overview = "overview",
        available = false,
        requested = false,
        approved = false,
        plexUrl = null,
        quality = null,
        seasonRequests = null
    )

    @Test
    fun `tv poster prefers portrait posterPath over backdrop`() {
        val item = tv(posterPath = "/tv-poster.jpg", backdropPath = "/tv-backdrop.jpg").toMediaItem()
        assertEquals("/tv-poster.jpg", item.posterPath)
    }

    @Test
    fun `tv poster falls back to images original when posterPath absent`() {
        val item = tv(
            posterPath = null,
            backdropPath = "/tv-backdrop.jpg",
            images = TvImages(original = "/img-original.jpg", medium = null)
        ).toMediaItem()
        assertEquals("/img-original.jpg", item.posterPath)
    }

    @Test
    fun `tv poster falls back to backdrop when posterPath and images absent`() {
        val item = tv(posterPath = null, backdropPath = "/tv-backdrop.jpg", images = null).toMediaItem()
        assertEquals("/tv-backdrop.jpg", item.posterPath)
    }

    @Test
    fun `tv maps id to tvDbId and theMovieDbId separately`() {
        val item = tv(id = 200, theMovieDbId = 1399).toMediaItem()
        assertEquals(200, item.tvDbId)
        assertEquals(1399, item.theMovieDbId)
        assertFalse(item.isMovie)
    }

    @Test
    fun `tv preserves null theMovieDbId`() {
        assertNull(tv(theMovieDbId = null).toMediaItem().theMovieDbId)
    }

    @Test
    fun `tv derives year from firstAired`() {
        assertEquals("2011", tv(firstAired = "2011-04-17").toMediaItem().year)
    }

    // ── MultiSearchResult ───────────────────────────────────────────────────

    private fun multi(id: String, mediaType: String) = MultiSearchResult(
        id = id,
        mediaType = mediaType,
        title = "Title",
        poster = "/multi.jpg",
        overview = "overview"
    )

    @Test
    fun `multiSearch numeric id parses to theMovieDbId`() {
        assertEquals(550, multi(id = "550", mediaType = "movie").toMediaItem().theMovieDbId)
    }

    @Test
    fun `multiSearch non-numeric id yields null theMovieDbId`() {
        assertNull(multi(id = "abc", mediaType = "movie").toMediaItem().theMovieDbId)
    }

    @Test
    fun `multiSearch movie mediaType sets isMovie true`() {
        assertTrue(multi(id = "1", mediaType = "movie").toMediaItem().isMovie)
    }

    @Test
    fun `multiSearch tv mediaType sets isMovie false`() {
        assertFalse(multi(id = "1", mediaType = "tv").toMediaItem().isMovie)
    }

    @Test
    fun `multiSearch defaults status flags to false`() {
        val item = multi(id = "1", mediaType = "movie").toMediaItem()
        assertFalse(item.available)
        assertFalse(item.requested)
        assertFalse(item.approved)
        assertNull(item.tvDbId)
    }

    // ── RecentlyAdded ───────────────────────────────────────────────────────

    @Test
    fun `recentlyAddedMovie is available and movie-typed`() {
        val item = RecentlyAddedMovie(
            id = 1, theMovieDbId = 42, imdbId = null, title = "Movie",
            posterPath = "/rp.jpg", overview = null, addedAt = null
        ).toMediaItem()
        assertTrue(item.available)
        assertTrue(item.isMovie)
        assertEquals(42, item.theMovieDbId)
        assertNull(item.tvDbId)
    }

    @Test
    fun `recentlyAddedTv is available with tvDbId only`() {
        val item = RecentlyAddedTv(
            id = 1, tvDbId = 121361, title = "Show",
            posterPath = "/rt.jpg", overview = null, addedAt = null
        ).toMediaItem()
        assertTrue(item.available)
        assertFalse(item.isMovie)
        assertEquals(121361, item.tvDbId)
        assertNull(item.theMovieDbId)
    }

    @Test
    fun `recentlyAddedMovie null title coalesces to empty string`() {
        val item = RecentlyAddedMovie(
            id = 1, theMovieDbId = null, imdbId = null, title = null,
            posterPath = null, overview = null, addedAt = null
        ).toMediaItem()
        assertEquals("", item.title)
    }
}
