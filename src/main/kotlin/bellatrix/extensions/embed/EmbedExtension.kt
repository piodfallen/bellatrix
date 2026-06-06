package bellatrix.extensions.embed

import bellatrix.common.discord.Res
import bellatrix.i18n.Translations
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralMessageCommand
import dev.kordex.core.extensions.ephemeralSlashCommand

class EmbedExtension : Extension() {
	override val name = "embed"

	private val editor = EmbedEditorService()

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = Translations.Commands.Embed.name
			description = Translations.Commands.Embed.description
			allowInDms = false
			defaultMemberPermissions = Permissions(Permission.ManageGuild)

			check {
				anyGuild()
			}

			ephemeralSubCommand {
				name = Translations.Commands.Embed.Create.name
				description = Translations.Commands.Embed.Create.description

				action {
					val state = EmbedEditorState(
						target = EditorTarget.Create(event.interaction.channelId),
					)
					val locale = getLocale()

					respond {
						editor.renderPanel(this, state, locale)
					}
				}
			}
		}

		ephemeralMessageCommand {
			name = Translations.Commands.Embed.Edit.name
			allowInDms = false
			defaultMemberPermissions = Permissions(Permission.ManageGuild)

			check {
				anyGuild()
			}

			action {
				val message = event.interaction.getTarget()

				if (message.embeds.isEmpty()) {
					respond {
						Res.warning(Translations.Embed.Editor.noEmbeds, this@action).applyTo(this)
					}

					return@action
				}

				if (message.author?.id != event.kord.selfId) {
					respond {
						Res.warning(Translations.Embed.Editor.notOwned, this@action).applyTo(this)
					}

					return@action
				}

				val state = EmbedEditorState.fromDiscordEmbeds(
					target = EditorTarget.Edit(
						channelId = message.channelId,
						messageId = message.id,
					),
					embeds = message.embeds,
				)
				val locale = getLocale()

				respond {
					editor.renderPanel(this, state, locale)
				}
			}
		}
	}
}
