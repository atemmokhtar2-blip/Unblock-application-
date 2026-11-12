package com.marayti.unblock

/** Offline Kotlin port of the bot's appeal drafting workflow. */
object AppealTemplates {
    private val severity = mapOf(
        "severe" to listOf(
            "My WhatsApp account is currently completely locked, and the restriction appears to be severe. I can no longer see my chats, my contacts, or any of my groups, and I have been cut off from the people I rely on every single day.",
            "I am locked out of my WhatsApp account entirely, and the restriction appears to be severe. Every time I open the app I am told the account has been restricted, and I lost my full message history, my contacts, and my groups overnight without a clear explanation.",
            "My account is under a restriction that appears to be severe, and I have completely lost access. This has cut me off from my family, my friends, and my work contacts, and it is already causing real disruption to my daily life.",
            "I am writing because my WhatsApp account has been fully restricted, and the situation appears to be severe. I can no longer reach my family, my colleagues, or my customers, and I urgently need a fair review of this decision."
        ),
        "temporary" to listOf(
            "My account appears to be under a temporary or repeated restriction. I want to understand exactly what triggered it and take whatever corrective steps are needed so it does not happen again.",
            "I am currently seeing a temporary restriction on my account, and I want to resolve it promptly and responsibly. I am ready to correct anything I need to and to learn exactly what is expected of me.",
            "My account has been placed under a short-term restriction, and I want to act quickly to make things right and make sure my account stays in good standing going forward.",
            "I have received notice of a temporary restriction, and I am writing to resolve it as quickly as possible. Please let me know precisely what happened and what I should do next so this does not recur."
        ),
        "standard" to listOf(
            "My WhatsApp account is currently restricted and I am unable to use it normally. I am asking for a clear, fair review so I can understand what happened and, where possible, get my access back.",
            "I am currently unable to use my WhatsApp account because of a restriction I do not fully understand, and I am asking for your help to review my account and explain the reason.",
            "My account has been restricted, and I would appreciate a straightforward explanation of the reason, together with a fair review of my circumstances and a clear path back to good standing.",
            "I am writing to ask for a review of my WhatsApp account, which has been restricted. I want to understand the specific reason and to do everything required to return to normal use."
        )
    )
    private val reasons = mapOf(
        "mistake" to listOf(
            "I believe this restriction may have been applied in error. Looking back at my own use of the account, I cannot identify anything that should have triggered it, and I would be grateful if a human reviewer could re-check the decision against my actual account history.",
            "I genuinely believe this is a mistake. I have reviewed how I use my account and can find nothing that I believe breaks WhatsApp's rules, so I am respectfully asking for the decision to be looked at again by a real person rather than left to an automated judgement.",
            "I suspect this restriction was triggered in error. I would like the full history of my account to be examined with proper context, because I am confident a fair review will show my account should not have been restricted.",
            "This restriction seems to have affected my account by mistake. I have nothing to hide, and I am asking for a manual review of my activity so the error can be corrected and my access restored."
        ),
        "unusual" to listOf(
            "I noticed activity on my account that I did not do myself, which makes me worry my account may have been accessed without my permission. I am asking for a security review so that any unauthorized activity is not held against me.",
            "Before the restriction I saw signs that my account may have been used by someone else. I am concerned my number may have been compromised, and I want a security review so this restriction is not treated as something I did.",
            "I believe there may have been unusual or unauthorized activity on my account that I did not initiate. Please examine my account for possible compromise so I can verify my identity and have my access restored.",
            "I do not recognize some of the activity linked to my account, and I am worried it was accessed without my knowledge. I am asking for a security review from your team and I am ready to prove I am the real owner."
        ),
        "reports" to listOf(
            "I understand that reports or complaints may have been made against my account. I would appreciate a fair review of the full context, because I believe there has been a misunderstanding, and I am ready to clarify anything.",
            "I have learned that other users may have reported my number. I believe those reports reflect a misunderstanding of the situation, and I am asking for a review that also considers my side of the story.",
            "It is possible that reports from other users are connected to this restriction. I am asking for a fair chance to explain the circumstances and provide the context that was probably missing from the automated decision.",
            "If my account was restricted because of reports, I would like to understand them and respond. I am asking for a fair review of what actually happened so any misunderstanding can be cleared up."
        ),
        "content" to listOf(
            "I would appreciate clarification about the specific content or activity that led to this restriction, so I can review it, understand what was misunderstood, and correct anything that genuinely needs correcting.",
            "I want to know exactly what content or activity triggered the restriction. Once I understand the specifics, I can review it, fix any real problem, and make sure it never happens again.",
            "Please tell me the precise content or behaviour that caused this restriction. I want to learn from it and make sure my future use of WhatsApp is fully within the rules.",
            "I am asking for a clear explanation of what content or activity led to this, because I want to take responsibility where it is genuinely needed and confirm my account is fully compliant going forward."
        ),
        "unknown" to listOf(
            "I have not been able to identify the specific reason for this restriction, and I want the review to tell me exactly which policy or activity is involved so I can respond properly and prevent any recurrence.",
            "I do not know the specific reason my account was restricted, so I cannot correct it on my own. I am asking for that information so I can understand the issue and act on it.",
            "Because no specific reason has been given to me, I cannot fix what I do not understand. I would be grateful for a clear explanation of the policy involved so I can respond accurately.",
            "I genuinely do not know what triggered this restriction. Please clarify the reason so I can take the right corrective action and make sure my account remains in good standing."
        ),
        "other" to listOf(
            "I would like the opportunity to add important context about this restriction, because the full circumstances may not be reflected in the automated decision, and I want the complete picture to be considered.",
            "There are details about my situation that I believe are relevant but may not have been captured automatically. I am asking for the chance to provide that context before a final decision is made.",
            "I would like to share further information so the review considers the full picture rather than just the automated signal that triggered the restriction.",
            "I believe my circumstances deserve a closer look. Please allow me to provide additional context so a fair and complete review can take place."
        )
    )
    private val requests = mapOf(
        "human" to listOf(
            "I respectfully ask for a review by a member of your support team rather than relying only on the automated decision. I am confident that a manual review by a member of your team will lead to a fair outcome.",
            "I am specifically asking for a review by a member of your support team, because I believe a real person looking at my full context will see that the automated decision does not reflect my actual usage.",
            "Please arrange a review by a member of your team who can examine my account history directly. I am confident that a manual review by a member of your team is the fastest path to a fair result.",
            "I would be very grateful for a review by a member of your team. A careful, human look at my account by a member of your support team will, I believe, clarify the situation and lead to the right result."
        ),
        "urgent" to listOf(
            "I kindly ask for a quick review, because I am currently locked out and this is affecting my ability to communicate with my family, friends, and work. I would be grateful for prioritised help.",
            "Because I am currently unable to access my account and rely on WhatsApp for urgent daily communication, I am respectfully asking for my case to be treated as a priority.",
            "I would be very grateful for a fast review. The restriction is actively disrupting time-sensitive personal and work communication, and a prompt response would mean a great deal to me.",
            "Please treat this as an urgent request. I need my account back for family and work matters, and I am asking for the fastest possible review of my case."
        ),
        "explain" to listOf(
            "Please explain which policy or activity is involved and the specific steps I need to take to resolve this restriction and prevent it from happening again.",
            "I am asking for a clear explanation of which rule was triggered and exactly what I should do to resolve it and keep my account in good standing.",
            "Please tell me the precise reason for the restriction and the corrective steps available to me, so I can act immediately and avoid any future issue.",
            "I would like a full explanation of the reason and the exact steps to fix it, so I can resolve this properly and stay fully compliant going forward."
        ),
        "review" to listOf(
            "Please review my account status, share the outcome, and let me know if any further action or information is needed from me.",
            "I am asking for a full review of my account and a clear result, together with any next steps I should take.",
            "Please review my account carefully and tell me the outcome and anything I need to do next.",
            "I would appreciate a complete review of my account and clear guidance on the result and any follow-up needed from my side."
        )
    )
    private val openings = listOf("Dear WhatsApp Support Team,", "Hello WhatsApp Account Review Team,", "Dear WhatsApp Trust & Safety Team,", "To the WhatsApp Account Review Team,", "Dear WhatsApp Help Center Team,")
    private val closings = listOf("Thank you for taking the time to review my request.", "I deeply appreciate your attention and any help you can provide.", "Thank you for your fairness and for looking into this promptly.", "I am grateful for your time, and for restoring my access where possible.", "Thank you for reviewing my account carefully.")
    private val subjects = listOf("Account restoration request — WhatsApp number {number}", "Urgent appeal: please review and restore WhatsApp account {number}", "Appeal — my WhatsApp account {number} has been restricted", "Request for human review of WhatsApp account {number}", "Please restore WhatsApp account {number} — appeal attached")
    private val cooperation = listOf(
        "To help this review move as quickly as possible, I confirm I am ready to:\n1) Verify that I am the owner of the number {number}.\n2) Provide any information, screenshots, or evidence you require.\n3) Cooperate fully with any further investigation into my account.",
        "I want to make this as easy as possible for your team. I am ready to:\n1) Prove that I own the number {number}.\n2) Supply device details, screenshots, and any other evidence you ask for.\n3) Cooperate fully with every step of the review.",
        "So this review can proceed without delay, please consider me fully available to:\n1) Confirm my ownership of the number {number}.\n2) Provide any documentation or details needed to verify my identity and account.\n3) Cooperate fully with any further investigation into my account.",
        "I am fully prepared to cooperate with your team:\n1) I can verify that the number {number} belongs to me.\n2) I can provide any evidence, screenshots, or device information you need.\n3) I will cooperate fully with any further review or investigation you require."
    )
    private val importance = listOf(
        "WhatsApp is how I stay in touch with my family and do my daily work, so losing access has been genuinely difficult for me.",
        "This account is my main channel for family conversations and important work messages, and being locked out is causing real problems.",
        "I depend on WhatsApp every day to reach my family, my friends, and my work, so restoring access would mean a great deal to me.",
        "Staying connected through WhatsApp matters deeply to me — for my family, my friends, and my job — so I am asking sincerely for your help."
    )
    private val severityShort = mapOf("severe" to "My account is completely inaccessible and the restriction appears to be severe.", "temporary" to "My account appears to be under a temporary or repeated restriction.", "standard" to "My account is currently restricted and I am unable to use it normally.")
    private val reasonShort = mapOf(
        "mistake" to "I believe this may have been applied in error and would like it reviewed against my actual history.",
        "unusual" to "I noticed unrecognized activity and am concerned my account may have been accessed without my permission.",
        "reports" to "I understand reports may be linked to my account and I would like a fair review of the context.",
        "content" to "I would like to know the specific content or activity involved so I can correct it.",
        "unknown" to "I do not know the specific reason for the restriction and would like an explanation.",
        "other" to "I would like the opportunity to provide additional context not reflected in the automated decision."
    )
    private val requestShort = mapOf("human" to "I request a review by a member of the support team.", "urgent" to "I request a timely review because I am currently locked out.", "explain" to "Please explain the policy involved and the steps I should take.", "review" to "Please review my account and share the outcome and next steps.")
    private val severityAr = mapOf(
        "severe" to "حسابي حاليًا غير قابل للوصول تمامًا، ويبدو أن التقييد شديد. لقد فقدت الوصول إلى محادثاتي وجهات اتصالي ومجموعاتي، وهذا يسبب تعطّلًا كبيرًا في تواصلي الشخصي والمهني.",
        "temporary" to "يبدو أن حسابي يخضع لتقييد مؤقت أو متكرر. أود أن أفهم ما المطلوب مني وكيف أتجنب تكرار ذلك مستقبلًا.",
        "standard" to "حسابي على واتساب مقيّد حاليًا ولا أستطيع استخدامه بشكل طبيعي. أود مراجعة هذا التقييد لأفهم ما حدث واستعادة الوصول حيثما أمكن."
    )
    private val reasonAr = mapOf(
        "mistake" to "أعتقد أن هذا التقييد ربما فُرض بالخطأ. بعد مراجعة نشاط حسابي لا أجد ما يستدعي ذلك، وسأكون ممتنًا لو أُعيد تقييم القرار في ضوء سجل حسابي الفعلي.",
        "unusual" to "لاحظت نشاطًا على حسابي لا أعرفه، مما يجعلني قلقًا من أن حسابي ربما تعرّض للاختراق أو استُخدم دون إذني. أرجو مراجعته من الناحية الأمنية حتى لا يُنسب إليّ نشاط غير مصرّح به.",
        "reports" to "أتفهم أن بلاغات أو شكاوى قد تكون مرتبطة بحسابي. سأقدّر مراجعة عادلة للسياق الكامل، لأنني أعتقد أنه ربما حدث سوء فهم، وأنا مستعد لتوضيح أي شيء غير واضح.",
        "content" to "أود توضيحًا للمحتوى أو النشاط المحدد الذي أدى إلى هذا التقييد، حتى أراجعه وأفهم موضع سوء الفهم وأتخذ الخطوات التصحيحية اللازمة.",
        "unknown" to "لم أتمكن من تحديد السبب المحدد لهذا التقييد، وأود أن توضح المراجعة أي سياسة أو نشاط متورط حتى أتمكن من الرد بشكل مناسب ومنع تكراره.",
        "other" to "أود الفرصة لتقديم سياق إضافي حول هذا التقييد، لأن الظروف قد لا تكون منعكسة بالكامل في القرار الآلي، وأريد ضمان النظر في الصورة الكاملة."
    )
    private val requestAr = mapOf(
        "human" to "أطلب بأدب مراجعة من أحد أعضاء فريق الدعم المختص بدلًا من الاعتماد فقط على القرار الآلي.",
        "urgent" to "أطلب مراجعة في أقرب وقت لأنني حاليًا لا أستطيع الوصول إلى حسابي وهذا يؤثر على تواصلي اليومي.",
        "explain" to "أرجو توضيح السياسة أو النشاط المعني والخطوات المحددة التي يمكنني اتخاذها لحل هذا التقييد.",
        "review" to "أرجو مراجعة حالة الحساب وإطلاعي على النتيجة وما إذا كانت هناك حاجة لأي إجراء إضافي من جانبي."
    )

    fun validatePhone(phone: String): Boolean = phone.length == 11 && phone.all(Char::isDigit) &&
        listOf("010", "011", "012", "015").any(phone::startsWith)

    private fun validate(s: String, p: String, r: String, q: String) {
        require(s in severity) { "نوع الحالة غير صالح" }
        require(r in reasons) { "سبب الحالة غير صالح" }
        require(q in requests) { "نوع الطلب غير صالح" }
        require(validatePhone(p)) { "رقم الهاتف المصري يجب أن يتكون من 11 رقمًا ويبدأ بـ 010 أو 011 أو 012 أو 015" }
    }
    private fun sign(name: String) = name.trim().ifBlank { "[Your full name]" }
    private fun evidenceSummary(phone: String, name: String, ev: VerificationResult?): String {
        if (ev == null) return ""
        val lines = mutableListOf("Case reference for quick review:", "- Account: $phone")
        if (name.isNotBlank()) lines += "- Name: $name"
        if (ev.date.isNotBlank()) lines += "- Restricted on: " + ev.date
        if (ev.restrictionType.isNotBlank()) lines += "- Restriction type shown: " + ev.restrictionType
        if (ev.restrictionReason.isNotBlank()) lines += "- Reason shown on notice: " + ev.restrictionReason
        return if (lines.size > 2) lines.joinToString("\n") else ""
    }
    private fun evidenceNarrative(ev: VerificationResult?): String {
        if (ev == null || (ev.date.isBlank() && ev.restrictionType.isBlank() && ev.restrictionReason.isBlank())) return ""
        var s = "I received the in-app restriction notice"
        if (ev.date.isNotBlank()) s += " on " + ev.date
        s += "."
        val detail = mutableListOf<String>()
        if (ev.restrictionType.isNotBlank()) detail += "describes a " + ev.restrictionType + " restriction"
        if (ev.restrictionReason.isNotBlank()) detail += "refers to \"" + ev.restrictionReason + "\""
        if (detail.isNotEmpty()) s += " The notice " + detail.joinToString(" and ") + "."
        return s + " I have kept an exact copy of this notice and can provide it immediately on request."
    }
    private fun evidenceNarrativeAr(ev: VerificationResult?): String {
        if (ev == null || (ev.date.isBlank() && ev.restrictionType.isBlank() && ev.restrictionReason.isBlank())) return ""
        val parts = mutableListOf<String>()
        if (ev.date.isNotBlank()) parts += "تلقيت إشعار التقييد داخل التطبيق بتاريخ " + ev.date + "."
        if (ev.restrictionType.isNotBlank()) parts += "الإشعار يوضح أن التقييد " + if (ev.restrictionType == "permanent") "دائم." else "مؤقت."
        if (ev.restrictionReason.isNotBlank()) parts += "ويشير إلى \"" + ev.restrictionReason + "\"."
        parts += "احتفظت بنسخة من هذا الإشعار ويمكنني تقديمها فورًا عند الطلب."
        return parts.joinToString(" ")
    }

    fun build(s: String, phone: String, r: String, q: String, evidence: VerificationResult? = null, name: String = "", variant: Int = 0): String {
        validate(s, phone, r, q)
        val i = kotlin.math.abs(variant)
        var subject = subjects[(i / 25) % subjects.size].replace("{number}", phone)
        if (s == "severe" || evidence?.restrictionType == "permanent") subject = "URGENT — " + subject
        val parts = mutableListOf(openings[i % openings.size], "",
            "I am writing to appeal the restriction on my WhatsApp account registered to the number " + phone + "." +
                if (name.isNotBlank()) " My name is " + name.trim() + " and I am the owner of this account." else "")
        evidenceSummary(phone, name, evidence).takeIf(String::isNotBlank)?.let { parts += listOf("", it) }
        parts += listOf("", severity.getValue(s)[i % 4], "", reasons.getValue(r)[(i / 4) % 4], "",
            requests.getValue(q)[(i / 16) % 4], "", cooperation[(i / 64) % 4].replace("{number}", phone),
            "", importance[(i / 256) % 4])
        evidenceNarrative(evidence).takeIf(String::isNotBlank)?.let { parts += listOf("", it) }
        parts += listOf("", "I have kept a record of the in-app restriction notice and I am ready to provide screenshots, device details, and ownership verification at any time. I have also submitted a review request through the in-app option.",
            "", "I am not asking for anything beyond a fair, honest review of my account. I would be genuinely grateful for a clear outcome and for my access to be restored as soon as possible.",
            "", closings[(i / 5) % closings.size], "", "Kind regards,", sign(name), "WhatsApp number: " + phone)
        return "Subject: " + subject + "\n\n" + parts.joinToString("\n")
    }

    fun buildShort(s: String, phone: String, r: String, q: String, ev: VerificationResult? = null): String {
        validate(s, phone, r, q)
        val extra = if (!ev?.date.isNullOrBlank()) " I received this notice on " + ev?.date + "."
            else if (!ev?.restrictionType.isNullOrBlank()) " The notice indicates a " + ev?.restrictionType + " restriction." else ""
        return "I am the owner of WhatsApp number " + phone + " and I am asking for a review of the restriction on my account. " +
            severityShort.getValue(s) + " " + reasonShort.getValue(r) + " " + requestShort.getValue(q) + extra +
            " I am ready to prove my ownership and provide any evidence required, and I will cooperate fully. Thank you for giving my account a fair review."
    }

    fun buildFollowUp(s: String, phone: String, r: String, q: String, ev: VerificationResult? = null, name: String = "", variant: Int = 0): String {
        validate(s, phone, r, q)
        val i = kotlin.math.abs(variant)
        val extra = ev?.date?.takeIf(String::isNotBlank)?.let { "\n\nFor reference, the restriction notice I received was dated " + it + "." } ?: ""
        val body = openings[i % openings.size] + "\n\nI am writing to follow up on the appeal I submitted two days ago regarding my WhatsApp account registered to the number " + phone +
            ". I have not yet received a response, and the account remains restricted.\n\nI want to respectfully reiterate my request for a review and confirm that I remain ready to verify my ownership of " + phone +
            ", provide any additional evidence, and cooperate fully with your investigation. This account is important to me for daily personal and work communication, and I am hoping for a resolution as soon as possible.\n\nIf there is anything specific I can provide to move the review forward, please let me know and I will send it immediately." + extra +
            "\n\n" + closings[(i / openings.size) % closings.size] + "\n\nKind regards,\n" + sign(name) + "\nWhatsApp number: " + phone
        return "Subject: Follow-up — account review request for WhatsApp number " + phone + " (second request)\n\n" + body
    }

    fun buildEscalation(s: String, phone: String, r: String, q: String, ev: VerificationResult? = null, name: String = "", variant: Int = 0): String {
        validate(s, phone, r, q)
        val i = kotlin.math.abs(variant)
        val extra = ev?.date?.takeIf(String::isNotBlank)?.let { "\n\nFor reference, the restriction notice I received was dated " + it + "." } ?: ""
        val body = openings[i % openings.size] + "\n\nI am writing to escalate my case for WhatsApp account " + phone +
            ". I have submitted an initial appeal and a follow-up without receiving a substantive response, and my account remains fully or partially restricted.\n\nI respectfully request that this matter be escalated to a senior member of the review team. I remain ready to verify that I am the owner of " + phone +
            ", to provide any documentation or evidence required, and to cooperate fully with the review. My goal is simply a fair, transparent examination of my account.\n\nI would be grateful for a clear status update and the specific next steps available to me. Please treat this as a request for a thorough manual review rather than another automated response." + extra +
            "\n\n" + closings[(i / openings.size) % closings.size] + "\n\nKind regards,\n" + sign(name) + "\nWhatsApp number: " + phone
        return "Subject: Escalation — unanswered appeal for WhatsApp account " + phone + "\n\n" + body
    }

    fun buildFinalNotice(s: String, phone: String, r: String, q: String, ev: VerificationResult? = null, name: String = "", variant: Int = 0): String {
        validate(s, phone, r, q)
        val i = kotlin.math.abs(variant)
        val extra = ev?.date?.takeIf(String::isNotBlank)?.let { "\n\nFor reference, the restriction notice I received was dated " + it + "." } ?: ""
        val body = openings[i % openings.size] + "\n\nI am writing one more time, as a final notice, regarding WhatsApp account " + phone +
            ". I have submitted an initial appeal, a follow-up, and an escalation request, and my account remains restricted without a substantive explanation of the specific reason.\n\nI am respectfully asking for this message to be treated as a formal, final request for a complete manual review of my account by a senior reviewer. I remain ready to verify that I am the owner of " + phone +
            ", to provide any documentation or evidence required, and to cooperate fully with the review process.\n\nI would be very grateful for a definitive written outcome and the specific steps I should take next. My only goal is a fair, transparent, and complete review of my account." + extra +
            "\n\n" + closings[(i / openings.size) % closings.size] + "\n\nKind regards,\n" + sign(name) + "\nWhatsApp number: " + phone
        return "Subject: Final notice — appeal for WhatsApp account " + phone + " remains unresolved\n\n" + body
    }

    fun buildArabic(s: String, phone: String, r: String, q: String, ev: VerificationResult? = null, name: String = ""): String {
        validate(s, phone, r, q)
        val parts = mutableListOf("إلى فريق مراجعة حسابات واتساب المحترم،", "",
            "أكتب إليكم لطلب مراجعة التقييد المفروض على حسابي المسجّل بالرقم " + phone + ".", "",
            severityAr.getValue(s), "", reasonAr.getValue(r), "", requestAr.getValue(q), "",
            "ولتسريع هذه المراجعة أؤكد استعدادي التام لما يلي:\n1) إثبات أنني مالك الرقم " + phone +
                ".\n2) تقديم أي معلومات أو أدلة إضافية تطلبونها.\n3) التعاون الكامل مع أي تحقيق إضافي في حسابي.")
        evidenceNarrativeAr(ev).takeIf(String::isNotBlank)?.let { parts += listOf("", it) }
        parts += listOf("", "أعتمد على واتساب في تواصلي اليومي وألتزم بشروط الخدمة ومعايير المجتمع، وسأكون ممتنًا لنتيجة واضحة واستعادة حسابي حيثما أمكن، ولأي خطوات يتعيّن عليّ اتخاذها.",
            "", "شكرًا لكم على وقتكم ومراجعتكم العادلة.", "", "مع خالص التحية،", name.trim().ifBlank { "[الاسم الكامل]" }, "رقم واتساب: " + phone)
        return "الموضوع: طلب مراجعة حساب واتساب — " + phone + "\n\n" + parts.joinToString("\n")
    }

    fun variantCount(): Long = 3L * 6L * 4L * 4L * 4L * 4L * openings.size * closings.size * subjects.size * cooperation.size * importance.size
}
