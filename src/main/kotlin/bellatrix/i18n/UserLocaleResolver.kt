package bellatrix.i18n

import bellatrix.database.repositories.UserRepository
import dev.kord.core.behavior.UserBehavior
import java.util.Locale

object UserLocaleResolver {
	fun resolve(user: UserBehavior?): Locale? =
		user
			?.id
			?.let(UserRepository::getOrCreate)
			?.locale
			?.let(UserLocales::fromTag)
}
