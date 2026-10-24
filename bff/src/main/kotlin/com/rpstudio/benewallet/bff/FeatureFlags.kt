package com.rpstudio.benewallet.bff

private const val BUCKETS = 100
private const val FULL_ROLLOUT = 100

private val DEFAULT_ROLLOUTS = mapOf(
    "goals" to 100,
    "statement_filters" to 100
)

internal object FeatureFlags {
    /** Stable bucket in [0, 100). The flag key is part of the hash so canary groups differ per flag. */
    fun bucket(key: String, userId: String): Int = Math.floorMod("$key:$userId".hashCode(), BUCKETS)

    fun resolve(userId: String?, rollouts: Map<String, Int> = DEFAULT_ROLLOUTS): List<FeatureFlagDto> =
        rollouts.map { (key, percent) ->
            val enabled = when {
                percent >= FULL_ROLLOUT -> true
                percent <= 0 || userId == null -> false
                else -> bucket(key, userId) < percent
            }
            FeatureFlagDto(key, enabled, percent)
        }
}
