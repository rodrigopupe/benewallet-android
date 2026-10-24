package com.rpstudio.benewallet.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkModuleTest {
    @Test
    fun `adds a trailing slash only when missing`() {
        assertEquals("http://10.0.2.2:8080/", "http://10.0.2.2:8080".withTrailingSlash())
        assertEquals("http://10.0.2.2:8080/", "http://10.0.2.2:8080/".withTrailingSlash())
    }
}
