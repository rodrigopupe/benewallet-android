package com.rpstudio.benewallet.bff

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

internal data class Transaction(
    val id: Long,
    val benefit: Benefit,
    val category: Category,
    val merchant: String,
    val amountCents: Long,
    val occurredAt: Instant
) {
    fun toDto() = TransactionDto(id, benefit, category, merchant, amountCents, occurredAt.toString())
}

internal data class TransactionFilter(
    val category: Category? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null
)

internal data class TransactionPage(val items: List<Transaction>, val nextCursor: Cursor?)

private data class Spec(
    val category: Category,
    val benefit: Benefit,
    val chancePerDay: Double,
    val amountCents: LongRange,
    val hours: IntRange,
    val merchants: List<String>
)

private data class Spend(val spec: Spec, val amountCents: Long, val at: Instant, val merchant: String)

private const val DEFAULT_SEED = 42L
private const val HISTORY_DAYS = 90
private const val RECHARGE_HOUR = 6
private const val MINUTES_PER_HOUR = 60

private val MONTHLY_RECHARGE_CENTS = mapOf(Benefit.MEAL to 120_000L, Benefit.FOOD to 80_000L)

private val SPECS = listOf(
    Spec(
        Category.RESTAURANT,
        Benefit.MEAL,
        0.50,
        2_800L..6_500L,
        11..14,
        listOf("Restaurante Sabor da Casa", "Cantina Bella Massa", "Self-Service Prato Cheio", "Cozinha da Vó")
    ),
    Spec(
        Category.DELIVERY,
        Benefit.MEAL,
        0.15,
        3_500L..8_500L,
        18..22,
        listOf("Burger Central", "Sushi do Bairro", "Pizzaria Forno a Lenha")
    ),
    Spec(
        Category.SNACKS,
        Benefit.MEAL,
        0.30,
        800L..2_200L,
        9..17,
        listOf("Lanchonete Esquina", "Café do Centro", "Padoca Express")
    ),
    Spec(
        Category.GROCERY,
        Benefit.FOOD,
        0.12,
        6_000L..20_000L,
        10..20,
        listOf("Supermercado Bom Preço", "Mercado Família", "Atacado Econômico")
    ),
    Spec(
        Category.BAKERY,
        Benefit.FOOD,
        0.25,
        600L..2_500L,
        7..10,
        listOf("Padaria Pão Quente", "Padaria Estrela")
    ),
    Spec(
        Category.GREENGROCER,
        Benefit.FOOD,
        0.12,
        1_500L..6_000L,
        8..18,
        listOf("Hortifruti Verde Vida", "Sacolão do Zé")
    )
)

private val NEWEST_FIRST = compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.id }

/** Draws one candidate purchase for [spec] on [day], or null if there is none that day. */
private fun Random.drawSpend(spec: Spec, day: LocalDate, zone: ZoneId): Spend? {
    if (nextDouble() >= spec.chancePerDay) return null
    val amount = nextLong(spec.amountCents.first, spec.amountCents.last + 1)
    val hour = nextInt(spec.hours.first, spec.hours.last + 1)
    val at = day.atTime(hour, nextInt(MINUTES_PER_HOUR)).atZone(zone).toInstant()
    val merchant = spec.merchants[nextInt(spec.merchants.size)]
    return Spend(spec, amount, at, merchant)
}

/**
 * Deterministic mock data: fixed seed, dates relative to the injected [clock].
 * The same day always yields the same data, and "this month" is never empty.
 */
internal data class MockWalletData(private val clock: Clock, val seed: Long = DEFAULT_SEED) {
    val transactions: List<Transaction> = generate(seed)

    fun balanceOf(benefit: Benefit): Long = transactions.filter { it.benefit == benefit }.sumOf { it.amountCents }

    fun page(filter: TransactionFilter, cursor: Cursor?, limit: Int): TransactionPage {
        val zone = clock.zone
        val matching = transactions.asSequence()
            .filter { filter.category == null || it.category == filter.category }
            .filter { tx ->
                val date = tx.occurredAt.atZone(zone).toLocalDate()
                (filter.from == null || date >= filter.from) &&
                    (filter.to == null || date <= filter.to)
            }
            .filter { cursor == null || cursor.isBefore(it) }
            .take(limit + 1)
            .toList()
        val items = matching.take(limit)
        val next = if (matching.size > limit) Cursor.of(items.last()) else null
        return TransactionPage(items, next)
    }

    fun generate(seed: Long): List<Transaction> {
        val random = Random(seed)
        val zone = clock.zone
        val now = clock.instant()
        val today = LocalDate.now(clock)
        val balances = Benefit.entries.associateWith { 0L }.toMutableMap()
        val result = mutableListOf<Transaction>()
        var nextId = 1L

        for (offset in HISTORY_DAYS downTo 0) {
            val day = today.minusDays(offset.toLong())

            if (offset == HISTORY_DAYS || day.dayOfMonth == 1) {
                val at = day.atTime(RECHARGE_HOUR, 0).atZone(zone).toInstant()
                if (!at.isAfter(now)) {
                    MONTHLY_RECHARGE_CENTS.forEach { (benefit, amount) ->
                        balances[benefit] = balances.getValue(benefit) + amount
                        result.add(
                            Transaction(
                                id = nextId++,
                                benefit = benefit,
                                category = Category.RECHARGE,
                                merchant = "Recarga mensal",
                                amountCents = amount,
                                occurredAt = at
                            )
                        )
                    }
                }
            }

            val spends = SPECS.mapNotNull { random.drawSpend(it, day, zone) }
            spends.forEach { spend ->
                val benefit = spend.spec.benefit
                val affordable = balances.getValue(benefit) >= spend.amountCents
                if (affordable && spend.at.isAfter(now).not()) {
                    balances[benefit] = balances.getValue(benefit) - spend.amountCents
                    result.add(
                        Transaction(
                            id = nextId++,
                            benefit = benefit,
                            category = spend.spec.category,
                            merchant = spend.merchant,
                            amountCents = -spend.amountCents,
                            occurredAt = spend.at
                        )
                    )
                }
            }
        }

        return result.sortedWith(NEWEST_FIRST)
    }
}
