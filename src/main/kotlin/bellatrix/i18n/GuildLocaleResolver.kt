package bellatrix.i18n

import dev.kord.core.entity.Guild
import java.util.Locale
import dev.kord.common.Locale as DiscordLocale

object GuildLocaleResolver {
	fun resolve(guild: Guild?): Locale =
		guild
			?.preferredLocale
			?.toLanguageTag()
			?.let(SupportedLocales::fromTag)
			?: SupportedLocales.default

	private fun DiscordLocale.toLanguageTag(): String =
		buildString {
			append(language)

			country?.let {
				append("-")
				append(it)
			}
		}
}
