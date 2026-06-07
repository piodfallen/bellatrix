package bellatrix.i18n

import bellatrix.database.repositories.UserRepository
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.UserBehavior
import java.util.Locale

object UserLocaleResolver {
	fun resolvePreference(user: UserBehavior?): Locale? =
		user
			?.id
			?.let(UserRepository::getOrCreate)
			?.locale
			?.let(SupportedLocales::fromTag)

	suspend fun resolve(user: UserBehavior?, guild: GuildBehavior?): Locale =
		resolvePreference(user) ?: GuildLocaleResolver.resolve(guild)

	fun resolve(user: UserBehavior?, guildLocale: Locale): Locale =
		resolvePreference(user) ?: guildLocale
}
