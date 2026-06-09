package bellatrix

import bellatrix.database.DatabaseFactory
import bellatrix.extensions.AutoroleExtension
import bellatrix.extensions.LanguageExtension
import bellatrix.extensions.MousetrapExtension
import bellatrix.extensions.PingExtension
import bellatrix.extensions.WelcomeExtension
import bellatrix.extensions.disboard.DisboardBumpExtension
import bellatrix.extensions.embed.EmbedExtension
import bellatrix.extensions.modmail.ModmailExtension
import bellatrix.extensions.starboard.StarboardExtension
import bellatrix.extensions.tasks.StaffTaskExtension
import bellatrix.i18n.SupportedLocales
import bellatrix.i18n.UserLocaleResolver
import dev.kord.common.entity.PresenceStatus
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.utils.env
import java.io.File
import dev.kord.common.Locale as DiscordLocale

private val TOKEN = env("TOKEN")

suspend fun main() {
	DatabaseFactory.init()

	val bot = ExtensibleBot(TOKEN) {
		chatCommands {
			defaultPrefix = "?"
			enabled = true
		}

		i18n {
			defaultLocale = SupportedLocales.PORTUGUESE_BRAZIL
			applicationCommandLocale(DiscordLocale.ENGLISH_UNITED_STATES)
			localeResolver { guild, _, user, _ ->
				UserLocaleResolver.resolve(user, guild)
			}
		}

		presence {
			status = PresenceStatus.DoNotDisturb

			listening("\uD83C\uDF83 Abobrinhas")
		}

		extensions {
			help {
				enableBundledExtension = false
			}

			add(::LanguageExtension)
			add(::AutoroleExtension)
			add(::DisboardBumpExtension)
			add(::EmbedExtension)
			add(::ModmailExtension)
			add(::MousetrapExtension)
			add(::PingExtension)
			add(::StarboardExtension)
			add(::StaffTaskExtension)
			add(::WelcomeExtension)
		}

		if (devMode) {
			plugins {
				if (File("src/main/dist/plugins").isDirectory) {
					pluginPath("src/main/dist/plugins")
				}
			}
		}
	}

	bot.start()
}
