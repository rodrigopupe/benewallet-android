package com.rpstudio.benewallet.feature.wallet.data.remote

import com.rpstudio.benewallet.core.network.networkJson
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.BalanceResponseDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.TransactionsResponseDto
import com.rpstudio.benewallet.feature.wallet.domain.model.Benefit
import com.rpstudio.benewallet.feature.wallet.domain.model.Category
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletMappersTest {
    @Test
    fun `maps a BFF page and tolerates unknown values`() {
        val page = networkJson.decodeFromString<TransactionsResponseDto>(TRANSACTIONS_JSON).toDomain()

        assertEquals(2, page.items.size)
        val first = page.items.first()
        assertEquals(Category.GREENGROCER, first.category)
        assertEquals(-4398L, first.amount.cents)
        assertTrue(first.amount.isNegative)
        assertEquals(Instant.parse("2026-10-23T14:38:00Z"), first.occurredAt)
        assertEquals(Category.OTHER, page.items[1].category)
        assertEquals("abc123", page.nextCursor)
    }

    @Test
    fun `last page has no cursor`() {
        val dto = networkJson.decodeFromString<TransactionsResponseDto>("""{"items":[],"nextCursor":null}""")

        assertNull(dto.toDomain().nextCursor)
    }

    @Test
    fun `balances drop unknown benefits`() {
        val json = """
            {"balances":[{"benefit":"MEAL","balanceCents":12345},{"benefit":"CRYPTO","balanceCents":1}],
            "updatedAt":"2026-10-24T15:00:00Z"}
        """.trimIndent()

        val balances = networkJson.decodeFromString<BalanceResponseDto>(json).toDomain()

        assertEquals(listOf(Benefit.MEAL), balances.map { it.benefit })
        assertEquals(12345L, balances.first().balance.cents)
    }
}

private const val TRANSACTIONS_JSON = """
{
  "items": [
    {
      "id": 145,
      "benefit": "FOOD",
      "category": "GREENGROCER",
      "merchant": "Hortifruti Verde Vida",
      "amountCents": -4398,
      "occurredAt": "2026-10-23T14:38:00Z"
    },
    {
      "id": 144,
      "benefit": "MEAL",
      "category": "TELEPORT",
      "merchant": "Novo Lugar",
      "amountCents": -1000,
      "occurredAt": "2026-10-23T14:00:00Z",
      "futureField": true
    },
    {
      "id": 143,
      "benefit": "VOUCHER",
      "category": "RESTAURANT",
      "merchant": "Desconhecido",
      "amountCents": -1,
      "occurredAt": "2026-10-23T13:00:00Z"
    }
  ],
  "nextCursor": "abc123"
}
"""
