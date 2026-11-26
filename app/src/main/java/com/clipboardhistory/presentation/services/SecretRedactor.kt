package com.clipboardhistory.presentation.services

/**
 * Removes secrets from clipboard text before the Local API hands it to another program.
 *
 * The clipboard history is the densest store of one-time codes, passwords and API tokens on the
 * phone, and the Local API's clients are agents: whatever it returns ends up in a model's context.
 * Every response that carries item content goes through [redact]. The app's own UI does not use the
 * Local API and still shows the original text.
 *
 * It is deterministic and errs towards redacting: a false positive costs the agent one value, a
 * false negative leaks a credential.
 */
object SecretRedactor {
    const val MARK = "[redacted]"

    private val patterns: List<Regex> = listOf(
        // PEM private keys, the whole block.
        Regex("-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----"),
        // JSON Web Tokens.
        Regex("\\beyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}"),
        // Provider key formats: OpenAI/Anthropic-style sk-, GitHub, GitLab, AWS, Google, Slack.
        Regex("\\bsk-[A-Za-z0-9_-]{16,}"),
        Regex("\\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})"),
        Regex("\\bglpat-[A-Za-z0-9_-]{20,}"),
        Regex("\\bAKIA[0-9A-Z]{16}\\b"),
        Regex("\\bAIza[0-9A-Za-z_-]{35}"),
        Regex("\\bxox[abprs]-[A-Za-z0-9-]{10,}"),
    )

    // "Bearer <token>" and "password: <value>" keep the label and lose the value.
    private val bearer = Regex("(?i)(\\bbearer\\s+)[A-Za-z0-9._~+/-]{12,}=*")
    private val labelled = Regex(
        "(?i)(\\b(?:password|passwd|pwd|passphrase|heslo|secret|api[_-]?key|token)\\b\\s*[:=]\\s*)(\"[^\"]*\"|'[^']*'|\\S+)"
    )

    // One-time codes: 4 to 8 digits near a word that says it is a code.
    private val otpNearKeyword = Regex(
        "(?i)(\\b(?:code|kód|kod|otp|pin|passcode|verification|ověřovací|overovaci|2fa|one-time)(?![\\p{L}\\d])[^\\d\\n]{0,40})(\\d{4,8})\\b"
    )
    private val otpBeforeKeyword = Regex(
        "(?i)\\b(\\d{4,8})(\\s+(?:is your|je váš|je vas)(?![\\p{L}\\d]))"
    )

    // An item that is nothing but a code or a password-looking token is redacted as a whole.
    private val onlyDigits = Regex("^\\s*\\d{4,8}\\s*$")
    private val singleToken = Regex("^\\S{10,128}$")

    fun redact(text: String): String {
        if (onlyDigits.matches(text) || looksLikePassword(text)) return MARK
        var out = text
        for (p in patterns) out = p.replace(out, MARK)
        out = bearer.replace(out) { it.groupValues[1] + MARK }
        out = labelled.replace(out) { it.groupValues[1] + MARK }
        out = otpNearKeyword.replace(out) { it.groupValues[1] + MARK }
        out = otpBeforeKeyword.replace(out) { MARK + it.groupValues[2] }
        return out
    }

    /** A single token with lower and upper case letters, digits and a symbol, and no URL or path shape. */
    internal fun looksLikePassword(text: String): Boolean {
        val t = text.trim()
        if (!singleToken.matches(t)) return false
        if (t.contains("://") || t.startsWith("/") || t.startsWith("www.") || t.contains("@")) return false
        val classes = listOf(
            t.any { it.isLowerCase() },
            t.any { it.isUpperCase() },
            t.any { it.isDigit() },
            t.any { !it.isLetterOrDigit() },
        ).count { it }
        return classes == 4
    }
}
