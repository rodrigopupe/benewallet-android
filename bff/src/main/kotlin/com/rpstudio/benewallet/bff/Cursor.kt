package com.rpstudio.benewallet.bff

import java.util.Base64

internal data class Cursor(val epochMillis: Long, val id: Long) {
    fun encode(): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString("$epochMillis:$id".toByteArray())

    /** True if [tx] comes strictly after this cursor in newest-first order. */
    fun isBefore(tx: Transaction): Boolean {
        val txMillis = tx.occurredAt.toEpochMilli()
        return txMillis < epochMillis || (txMillis == epochMillis && tx.id < id)
    }

    companion object {
        fun decode(raw: String): Cursor? = runCatching {
            val (millis, id) = String(Base64.getUrlDecoder().decode(raw)).split(":")
            Cursor(millis.toLong(), id.toLong())
        }.getOrNull()

        fun of(tx: Transaction) = Cursor(tx.occurredAt.toEpochMilli(), tx.id)
    }
}
