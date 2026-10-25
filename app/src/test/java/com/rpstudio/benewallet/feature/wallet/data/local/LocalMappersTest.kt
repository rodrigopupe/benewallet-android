package com.rpstudio.benewallet.feature.wallet.data.local

import com.rpstudio.benewallet.feature.wallet.domain.model.Benefit
import com.rpstudio.benewallet.feature.wallet.domain.model.BenefitBalance
import com.rpstudio.benewallet.feature.wallet.domain.model.Category
import com.rpstudio.benewallet.feature.wallet.domain.model.Money
import com.rpstudio.benewallet.feature.wallet.domain.model.Transaction
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalMappersTest {
    private fun entity(benefit: String = "MEAL", category: String = "DELIVERY") = TransactionEntity(
        id = 1,
        benefit = benefit,
        category = category,
        merchant = "Burger Place",
        amountCents = -2590,
        occurredAtMillis = 1_761_230_280_000
    )

    @Test
    fun `transaction round trip preserves all fields`() {
        val transaction = Transaction(
            id = 1,
            benefit = Benefit.MEAL,
            category = Category.DELIVERY,
            merchant = "Burger Place",
            amount = Money(-2590),
            occurredAt = Instant.parse("2026-10-23T14:38:00Z")
        )

        assertEquals(
            "round trip should be lossless",
            transaction,
            transaction.toEntity().toDomainOrNull()
        )
    }

    @Test
    fun `unknown category falls back to other`() {
        assertEquals(Category.OTHER, entity(category = "SOMETHING_NEW").toDomainOrNull()?.category)
    }

    @Test
    fun `unknown benefit drops the row`() {
        assertNull(
            "rows with an unknown benefit must be discarded",
            entity(benefit = "VOUCHER").toDomainOrNull()
        )
    }

    @Test
    fun `sub millisecond precision is truncated`() {
        val precise = Transaction(
            id = 2,
            benefit = Benefit.FOOD,
            category = Category.GROCERY,
            merchant = "Market",
            amount = Money(-100),
            occurredAt = Instant.parse("2026-10-23T14:38:00.123456Z")
        )

        assertEquals(
            "precision below a millisecond is lost on purpose",
            Instant.parse("2026-10-23T14:38:00.123Z"),
            precise.toEntity().toDomainOrNull()?.occurredAt
        )
    }

    @Test
    fun `balance round trip and unknown benefit`() {
        val balance = BenefitBalance(Benefit.FOOD, Money(80_000))

        assertEquals(balance, balance.toEntity().toDomainOrNull())
        assertNull(BalanceEntity(benefit = "VOUCHER", balanceCents = 1).toDomainOrNull())
    }
}
