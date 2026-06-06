package bellatrix.extensions.commands

import bellatrix.database.repositories.UserRepository
import bellatrix.i18n.Translations
import bellatrix.i18n.UserLocales
import bellatrix.utils.Res
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand

class LanguageExtension : Extension() {
	override val name = "language"

	override suspend fun setup() {
		ephemeralSlashCommand(arguments = ::LanguageArguments) {
			name = Translations.Commands.Language.name
			description = Translations.Commands.Language.description
			allowInDms = false

			check {
				anyGuild()
			}

			action {
				val user = event.interaction.user

				val storedUser = UserRepository.updateLocale(
					userId = user.id,
					locale = arguments.locale,
				)

				resolvedLocale = UserLocales.fromTag(storedUser.locale)

				val localeNameKey = if (storedUser.locale == UserLocales.ENGLISH_TAG) {
					Translations.Commands.Language.Choice.english
				} else {
					Translations.Commands.Language.Choice.portugueseBrazil
				}
				val response = Translations.Commands.Language.updated.withNamedPlaceholders(
					"locale" to localeNameKey,
				)

				respond {
					Res.success(response, this@action).applyTo(this)
				}
			}
		}
	}

	class LanguageArguments : Arguments() {
		val locale by stringChoice {
			name = Translations.Commands.Language.Arguments.Locale.name
			description = Translations.Commands.Language.Arguments.Locale.description

			choice(Translations.Commands.Language.Choice.portugueseBrazil, UserLocales.DEFAULT_TAG)
			choice(Translations.Commands.Language.Choice.english, UserLocales.ENGLISH_TAG)
		}
	}
}
