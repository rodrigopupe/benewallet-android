package com.rpstudio.benewallet.core.analytics

sealed class AnalyticsEvent(val name: String, val params: Map<String, String> = emptyMap()) {
    data class ScreenViewed(val screen: String) :
        AnalyticsEvent(
            "screen_viewed",
            mapOf("screen" to screen)
        )

    data class StatementFilterApplied(val category: String?, val period: String) :
        AnalyticsEvent(
            "statement_filter_applied",
            mapOf("category" to (category ?: "all"), "period" to period)
        )

    object GoalCreated : AnalyticsEvent("goal_created")
}
