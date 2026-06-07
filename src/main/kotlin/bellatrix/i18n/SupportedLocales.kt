package bellatrix.i18n

import java.util.Locale

object SupportedLocales {
	const val PORTUGUESE_BRAZIL_TAG = "pt-BR"
	const val ENGLISH_TAG = "en-US"

	val PORTUGUESE_BRAZIL: Locale = Locale.forLanguageTag(PORTUGUESE_BRAZIL_TAG)

	val supportedTags: Set<String> = setOf(PORTUGUESE_BRAZIL_TAG, ENGLISH_TAG)

	fun normalize(tag: String?): String? =
		tag
			?.let(Locale::forLanguageTag)
			?.toLanguageTag()
			?.takeIf { it in supportedTags }

	fun fromTag(tag: String?): Locale? =
		normalize(tag)?.let(Locale::forLanguageTag)
}
