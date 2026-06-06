package bellatrix.i18n

import java.util.Locale

object UserLocales {
	const val DEFAULT_TAG = "pt-BR"
	const val ENGLISH_TAG = "en-US"

	val default: Locale = Locale.forLanguageTag(DEFAULT_TAG)

	val supportedTags: Set<String> = setOf(DEFAULT_TAG, ENGLISH_TAG)

	fun normalize(tag: String): String? =
		Locale.forLanguageTag(tag)
			.toLanguageTag()
			.takeIf { it in supportedTags }

	fun fromTag(tag: String): Locale =
		Locale.forLanguageTag(normalize(tag) ?: DEFAULT_TAG)
}
