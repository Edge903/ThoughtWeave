package io.wit117.thoughtweave.feature.legal

import androidx.annotation.StringRes
import io.wit117.thoughtweave.R

/** Documents listed on S05. Texts are verbatim copies in `assets/legal`. */
enum class LegalDocument(@param:StringRes val titleRes: Int, val assets: List<String>) {
    GEMMA_TERMS(R.string.license_gemma_terms, listOf("legal/gemma_terms_of_use.txt")),
    GEMMA_POLICY(R.string.license_gemma_policy, listOf("legal/gemma_prohibited_use_policy.txt")),
    GEMMA_NOTICE(R.string.license_gemma_notice, listOf("legal/gemma_notice.txt")),
    LITERT_LM(R.string.license_litert_lm, listOf("legal/litert_lm.txt", "legal/apache-2.0.txt")),
    OTHER_OPEN_SOURCE(
        R.string.license_other_open_source,
        listOf(
            "legal/other_open_source.txt",
            "legal/apache-2.0.txt",
            "legal/gpl-2.0-classpath-exception.txt",
        ),
    ),
}

/** [indentLevel] is the nesting depth of a list item; 0 for plain paragraphs and top-level items. */
data class LegalParagraph(val text: String, val indentLevel: Int = 0)

/** Splits on blank lines and joins hard-wrapped lines, so the text reflows on narrow screens. */
fun parseLegalParagraphs(text: String): List<LegalParagraph> =
    text.replace("\r\n", "\n")
        .split(Regex("\\n\\s*\\n"))
        .filter { it.isNotBlank() }
        .map { block ->
            val trimmed = block.trimStart('\n')
            val leadingSpaces = trimmed.length - trimmed.trimStart(' ').length
            LegalParagraph(
                text = trimmed.trim().replace(Regex("\\s*\\n\\s*"), " "),
                indentLevel = if (trimmed.trimStart().startsWith("- ")) leadingSpaces / 2 else 0,
            )
        }
