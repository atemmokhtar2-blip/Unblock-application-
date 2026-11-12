package com.marayti.unblock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalScreenshotVerifierTest {
    private fun verify(text: String, confidence: Int = 95) =
        LocalScreenshotVerifier(null as android.content.Context?).verifyRecognizedTextForTest(text, confidence)

    @Test
    fun rejectsUnrelatedImagesImmediatelyWithWhatsAppSpecificMessage() {
        val result = verify("Instagram profile settings edit profile followers messages privacy account center")
        assertFalse(result.accepted)
        assertTrue(result.message.contains("لا تحتوي على ما يثبت ارتباطها بواتساب"))
    }

    @Test
    fun rejectsWhatsAppScreenshotsThatDoNotShowABanNotice() {
        val result = verify("WhatsApp chats calls status communities settings profile account privacy")
        assertFalse(result.accepted)
        assertTrue(result.message.contains("لا يوجد نص يدل على الحظر"))
    }

    @Test
    fun acceptsEnglishWhatsAppBanEvidence() {
        val result = verify(
            "WhatsApp this account is not allowed to use WhatsApp due to violation of terms of service. Request a review from help center.",
            95
        )
        assertTrue(result.accepted)
    }

    @Test
    fun acceptsArabicWhatsAppBanEvidenceWithSeparatedBrandWords() {
        val result = verify(
            "واتس اب لا يمكن لهذا الحساب استخدام واتساب بسبب مخالفة شروط الخدمة. يمكنك طلب مراجعة من مركز المساعدة والدعم.",
            95
        )
        assertTrue(result.accepted)
    }
}
