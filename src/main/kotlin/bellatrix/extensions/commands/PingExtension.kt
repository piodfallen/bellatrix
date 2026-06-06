package bellatrix.extensions.commands

import bellatrix.i18n.Translations
import bellatrix.utils.Res
import dev.kord.core.Kord
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.chatCommand
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.utils.respond

class PingExtension : Extension() {
	override val name = "ping"

	override suspend fun setup() {
		chatCommand {
			name = Translations.Commands.Ping.name
			description = Translations.Commands.Ping.description

			check {
				anyGuild()
			}

			action {
				val response = Translations.Commands.Ping.response.withNamedPlaceholders(
					"latency" to gatewayLatencyMillis(event.kord),
				)

				message.respond {
					Res.success(response, this@action)
						.applyTo(this)
				}
			}
		}

		ephemeralSlashCommand {
			name = Translations.Commands.Ping.name
			description = Translations.Commands.Ping.description
			allowInDms = false

			check {
				anyGuild()
			}

			action {
				val response = Translations.Commands.Ping.response.withNamedPlaceholders(
					"latency" to gatewayLatencyMillis(event.kord),
				)

				respond {
					Res.success(response, this@action)
						.applyTo(this)
				}
			}
		}
	}

	private fun gatewayLatencyMillis(kord: Kord): String =
		kord.gateway.averagePing
			?.inWholeMilliseconds
			?.coerceAtLeast(0)
			?.let { "**$it** ms" }
			?: "N/A"
}
