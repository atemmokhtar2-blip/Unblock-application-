package com.marayti.unblock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppealTemplatesTest {
    private val phone = "01012345678"

    @Test fun acceptsEgyptianMobileFormatsOnly() {
        assertTrue(AppealTemplates.validatePhone(phone))
        assertTrue(AppealTemplates.validatePhone("01112345678"))
        assertFalse(AppealTemplates.validatePhone("02012345678"))
        assertFalse(AppealTemplates.validatePhone("0101234567"))
        assertFalse(AppealTemplates.validatePhone("0101234567A"))
    }

    @Test fun buildsAllSixOutputTypes() {
        val long = AppealTemplates.build("standard", phone, "unknown", "review")
        val short = AppealTemplates.buildShort("standard", phone, "unknown", "review")
        val arabic = AppealTemplates.buildArabic("standard", phone, "unknown", "review")
        val followUp = AppealTemplates.buildFollowUp("standard", phone, "unknown", "review")
        val escalation = AppealTemplates.buildEscalation("standard", phone, "unknown", "review")
        val finalNotice = AppealTemplates.buildFinalNotice("standard", phone, "unknown", "review")

        assertTrue(long.contains(phone))
        assertTrue(short.contains(phone))
        assertTrue(arabic.contains(phone))
        assertTrue(arabic.contains("طلب مراجعة"))
        assertTrue(followUp.contains("second request"))
        assertTrue(escalation.contains("Escalation"))
        assertTrue(finalNotice.contains("Final notice"))
    }

    @Test fun longTemplateContainsEvidenceWithoutInventingMissingFields() {
        val evidence = VerificationResult(
            accepted = true,
            message = "verified",
            date = "2026-11-12",
            restrictionType = "temporary",
            restrictionReason = "spam"
        )
        val draft = AppealTemplates.build("temporary", phone, "unknown", "review", evidence)
        assertTrue(draft.contains("2026-11-12"))
        assertTrue(draft.contains("temporary restriction"))
        assertTrue(draft.contains("spam"))
        assertFalse(draft.contains("Name:"))
    }

    @Test fun variantChangesWording() {
        val first = AppealTemplates.build("standard", phone, "unknown", "review", variant = 0)
        val second = AppealTemplates.build("standard", phone, "unknown", "review", variant = 1)
        assertTrue(first != second)
        assertTrue(AppealTemplates.variantCount() > 100000L)
    }
}
