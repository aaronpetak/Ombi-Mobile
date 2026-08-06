package com.ombi.mobile.data.api.models

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the JSON-number coercion contract the app relies on.
 *
 * A live Ombi V2 server returns several numeric fields as JSON *strings* rather
 * than numbers — e.g. the TV endpoints send `theMovieDbId: "60059"`, `rating: "7.3"`,
 * and `tvDbId: "273181"`, while the recently-added endpoints send `theMovieDbId`
 * as a quoted string too. Our models type these as `Int?` / `Double?`, so
 * deserialization only works because Moshi coerces quoted numerics.
 *
 * This is a load-bearing assumption discovered during PR 7 verification. These
 * tests lock it in against the exact payloads observed on the server, so a Moshi
 * upgrade that tightened number parsing would fail here instead of silently
 * breaking search/discover/recently-added at runtime.
 *
 * The Moshi instance is configured identically to
 * [com.ombi.mobile.di.NetworkModule.provideMoshi].
 */
class JsonNumberCoercionTest {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `SearchTvShowViewModel coerces string ids and rating to numbers`() {
        // Trimmed from a real GET /api/v2/search/tv/moviedb/60059 response.
        val json = """
            {
              "id": 60059,
              "theMovieDbId": "60059",
              "theTvDbId": "273181",
              "title": "Better Call Saul",
              "firstAired": "2015-02-08",
              "banner": "/banner.jpg",
              "posterPath": null,
              "backdropPath": null,
              "images": { "original": "/orig.jpg", "medium": null },
              "rating": "7.3",
              "overview": "overview",
              "available": false,
              "requested": true,
              "approved": false,
              "plexUrl": null,
              "quality": null,
              "seasonRequests": null
            }
        """.trimIndent()

        val model = moshi.adapter(SearchTvShowViewModel::class.java).fromJson(json)!!

        assertEquals(60059, model.id)
        assertEquals(60059, model.theMovieDbId)
        assertEquals(273181, model.theTvDbId)
        assertEquals(7.3, model.rating!!, 0.0001)
        assertEquals("/orig.jpg", model.images?.original)
    }

    @Test
    fun `SearchTvShowViewModel allows null theMovieDbId from list endpoints`() {
        // Discover/list items frequently omit theMovieDbId (id still holds the TMDB ID).
        val json = """
            {
              "id": 289324,
              "theMovieDbId": null,
              "theTvDbId": null,
              "title": "Star Wars: Visions",
              "firstAired": null,
              "banner": "/b.jpg",
              "posterPath": "/p.jpg",
              "backdropPath": "/bd.jpg",
              "images": null,
              "rating": "6",
              "overview": "overview",
              "available": false,
              "requested": false,
              "approved": false,
              "plexUrl": null,
              "quality": null,
              "seasonRequests": null
            }
        """.trimIndent()

        val model = moshi.adapter(SearchTvShowViewModel::class.java).fromJson(json)!!

        assertEquals(289324, model.id)
        assertNull(model.theMovieDbId)
        assertEquals(6.0, model.rating!!, 0.0001)
    }

    @Test
    fun `RecentlyAddedTv coerces string theMovieDbId and tvDbId to Int`() {
        // Trimmed from a real GET /api/v1/recentlyadded/tv response.
        val json = """
            {
              "id": 610909,
              "theMovieDbId": "60059",
              "tvDbId": "273181",
              "title": "Better Call Saul",
              "posterPath": null,
              "overview": null,
              "addedAt": "2026-08-01T12:00:00.6879404Z"
            }
        """.trimIndent()

        val model = moshi.adapter(RecentlyAddedTv::class.java).fromJson(json)!!

        assertEquals(610909, model.id)
        assertEquals(60059, model.theMovieDbId)
        assertEquals(273181, model.tvDbId)
    }

    @Test
    fun `RecentlyAddedMovie coerces string theMovieDbId to Int`() {
        val json = """
            {
              "id": 610859,
              "theMovieDbId": "1339713",
              "imdbId": "tt37287335",
              "title": "Obsession",
              "posterPath": null,
              "overview": null,
              "addedAt": "2026-08-01T12:00:00.0683338Z"
            }
        """.trimIndent()

        val model = moshi.adapter(RecentlyAddedMovie::class.java).fromJson(json)!!

        assertEquals(1339713, model.theMovieDbId)
    }
}
