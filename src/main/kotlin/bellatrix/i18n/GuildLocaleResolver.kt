package bellatrix.i18n

import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.entity.Guild
import java.util.Locale
import dev.kord.common.Locale as DiscordLocale

object GuildLocaleResolver {
	fun resolve(guild: Guild?): Locale =
		guild
			?.preferredLocale
			?.toLanguageTag()
			?.let(SupportedLocales::fromTag)
			?: SupportedLocales.PORTUGUESE_BRAZIL

	suspend fun resolve(guild: GuildBehavior?): Locale =
		resolve(guild?.let { it.kord.getGuildOrNull(it.id) })

	private fun DiscordLocale.toLanguageTag(): String =
		buildString {
			append(language)

			country?.let {
				append("-")
				append(it)
			}
		}
}
