package com.marayti.unblock

/** Initial native Kotlin draft builder; expanded during template parity migration. */
object AppealTemplates {
    private val severityText = mapOf(
        "severe" to "My WhatsApp account is currently fully restricted, and I can no longer use it normally. This is disrupting my important daily communication.",
        "temporary" to "My WhatsApp account appears to be under a temporary or repeated restriction. I want to understand what triggered it and resolve the issue responsibly.",
        "standard" to "My WhatsApp account is currently restricted and I am unable to use it normally. I am asking for a fair review and a clear explanation."
    )
    private val reasonText = mapOf(
        "mistake" to "I believe this restriction may have been applied in error. Please review the account history and the decision carefully.",
        "unusual" to "I am concerned there may have been unusual activity on my account. Please help me understand the activity associated with this restriction.",
        "reports" to "If reports or complaints are connected to this restriction, I would appreciate a fair review of the full context.",
        "content" to "Please clarify the specific content or activity that led to this restriction so I can understand and address the issue.",
        "unknown" to "I have not been able to identify the specific reason for this restriction. Please explain which policy or activity is involved.",
        "other" to "I would appreciate the opportunity to provide any additional context relevant to a fair review."
    )
    private val requestText = mapOf(
        "human" to "I respectfully request a review by a member of your support team.",
        "urgent" to "I would appreciate a prompt review because this restriction is affecting important personal and work communication.",
        "explain" to "Please provide a clear explanation of the reason for this restriction and the steps available to resolve it.",
        "review" to "I respectfully request a review of this restriction and my account status."
    )

    fun build(severity: String, phone: String, reason: String, requestType: String, evidence: VerificationResult? = null): String {
        val evidenceLine = listOfNotNull(
            evidence?.date?.takeIf { it.isNotBlank() }?.let { "Notice date: $it" },
            evidence?.restrictionType?.takeIf { it.isNotBlank() }?.let { "Restriction type shown in the notice: $it" },
            evidence?.restrictionReason?.takeIf { it.isNotBlank() }?.let { "Policy wording recognized in the notice: $it" }
        ).joinToString("\\n")
        return listOf(
            "Subject: Request for review of my WhatsApp account",
            "Dear WhatsApp Support Team,",
            severityText[severity] ?: severityText.getValue("standard"),
            reasonText[reason] ?: reasonText.getValue("unknown"),
            requestText[requestType] ?: requestText.getValue("review"),
            evidenceLine.takeIf { it.isNotBlank() }?.let { "Case reference (read from the supplied notice):\\n$it" } ?: "",
            "Phone number: $phone",
            "I am willing to provide any additional information needed. Thank you for taking the time to review my case.",
            "Kind regards"
        ).joinToString("\n\n")
    }
}
