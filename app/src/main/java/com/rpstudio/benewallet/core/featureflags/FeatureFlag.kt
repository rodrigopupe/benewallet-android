package com.rpstudio.benewallet.core.featureflags

/**
 * Flags known by the app. [defaultEnabled] applies while there is no cached value (first launch offline, BFF down):
 * stable features default to on, so a failed request never removes them; unfinished ones default to off.
 */
internal enum class FeatureFlag(val key: String, val defaultEnabled: Boolean) {
    GOALS("goals", defaultEnabled = false),
    STATEMENT_FILTERS("statement_filters", defaultEnabled = true)
}

/** Last known values by flag key. Keys the app does not know are kept but ignored. */
internal data class FeatureFlags(private val values: Map<String, Boolean> = emptyMap()) {
    fun isEnabled(flag: FeatureFlag): Boolean = values[flag.key] ?: flag.defaultEnabled
}
