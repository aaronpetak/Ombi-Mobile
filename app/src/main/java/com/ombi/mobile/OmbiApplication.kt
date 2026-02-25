package com.ombi.mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Annotated with [HiltAndroidApp] to trigger Hilt's code generation and
 * initialise the application-level dependency injection container. All
 * Hilt-managed singletons (repositories, network clients, preferences, etc.)
 * are created and held here for the lifetime of the process.
 */
@HiltAndroidApp
class OmbiApplication : Application()
