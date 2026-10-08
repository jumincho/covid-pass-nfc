package com.jumincho.cvpass.ui

import kotlinx.coroutines.flow.SharingStarted

/**
 * How view models share their UI state: upstream flows stay active while the UI collects
 * the state and for five seconds after, so that a configuration change does not restart them.
 */
internal val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)
