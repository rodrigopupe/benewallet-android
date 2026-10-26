package com.rpstudio.benewallet.feature.auth.presentation.login

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginFormTest {
    @Test
    fun `a typical account is accepted`() {
        assertTrue(isLoginFormValid("rodrigo@benewallet.demo", "benewallet123"))
    }

    @Test
    fun `an empty e-mail is refused`() {
        assertFalse(isLoginFormValid("", "benewallet123"))
        assertFalse(isLoginFormValid("   ", "benewallet123"))
    }

    @Test
    fun `an empty password is refused`() {
        assertFalse(isLoginFormValid("rodrigo@benewallet.demo", ""))
    }

    @Test
    fun `an e-mail without the expected shape is refused`() {
        listOf("rodrigo", "rodrigo@", "@benewallet.demo", "rodrigo@benewallet", "rodrigo benewallet@demo.com")
            .forEach { assertFalse("$it must be refused", isLoginFormValid(it, "benewallet123")) }
    }

    @Test
    fun `spaces around the e-mail are ignored, as the session manager trims them too`() {
        assertTrue(isLoginFormValid("  rodrigo@benewallet.demo ", "benewallet123"))
    }

    @Test
    fun `the password is not trimmed or checked for strength`() {
        assertTrue(isLoginFormValid("rodrigo@benewallet.demo", "x"))
        assertTrue(isLoginFormValid("rodrigo@benewallet.demo", "  "))
    }
}
