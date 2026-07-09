package com.example.watchlist.di

import javax.inject.Qualifier

/** The IO dispatcher, injected rather than hard-coded so tests can substitute a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * An application-lifetime [kotlinx.coroutines.CoroutineScope] used by singletons (e.g. the price
 * stream) that must outlive any single ViewModel but still be structured/cancellable.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
