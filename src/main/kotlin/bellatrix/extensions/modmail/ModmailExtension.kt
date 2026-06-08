package bellatrix.extensions.modmail

import bellatrix.common.discord.Res
import bellatrix.common.discord.Roles
import bellatrix.i18n.Translations
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasRole
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class ModmailExtension : Extension() {
	override val name = "modmail"

	private val service = ModmailService()
	private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	private var schedulerJob: Job? = null

	override suspend fun setup() {
		val moderatorRole = Roles.moderator ?: return

		schedulerJob = ModmailScheduler(service, kord).start(schedulerScope)

		event<MessageCreateEvent> {
			action {
				val message = event.message

				if (event.guildId == null) {
					service.handleUserDm(message)
				} else {
					val member = event.member ?: return@action
					if (moderatorRole !in member.roleIds) return@action

					service.handleThreadMessage(message)
				}
			}
		}

		ephemeralSlashCommand {
			name = Translations.Commands.Thread.name
			description = Translations.Commands.Thread.description
			allowInDms = false
			defaultMemberPermissions = Permissions(Permission.ManageThreads)

			check {
				anyGuild()
				hasRole(moderatorRole)
			}

			ephemeralSubCommand {
				name = Translations.Commands.Thread.Close.name
				description = Translations.Commands.Thread.Close.description

				action {
					val closed = service.closeFromCommand(
						kord = event.kord,
						threadId = event.interaction.channelId,
					)

					respond {
						if (closed) {
							Res.success(Translations.Commands.Thread.Close.success, this@action).applyTo(this)
						} else {
							Res.warning(Translations.Commands.Thread.Close.notModmail, this@action).applyTo(this)
						}
					}
				}
			}
		}
	}

	override suspend fun unload() {
		schedulerJob?.cancel()
		schedulerScope.cancel()
		service.close()
	}
}
