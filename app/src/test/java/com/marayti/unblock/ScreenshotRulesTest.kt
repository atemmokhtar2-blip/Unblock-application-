package com.marayti.unblock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotRulesTest {
    @Test fun acceptsTextThatPassesEveryWhatsAppGate() {
        val result = RulesProbe.verify(
            "WhatsApp This account has been banned due to a violation of the Terms of Service. Request a review through the Help Center."
        )
        assertTrue(result.accepted)
    }

    @Test fun rejectsAnotherPlatformEvenWhenBanWordsAppear() {
        val result = RulesProbe.verify(
            "Telegram This account has been banned due to a violation of the Terms of Service. Request a review through the Help Center."
        )
        assertFalse(result.accepted)
    }
}
