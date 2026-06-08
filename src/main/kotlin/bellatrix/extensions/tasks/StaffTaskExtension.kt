package bellatrix.extensions.tasks

import bellatrix.common.discord.Roles
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.checks.hasRole
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event

class StaffTaskExtension : Extension() {
	override val name = "staff-tasks"

	private val service = StaffTaskService()

	override suspend fun setup() {
		val moderatorRole = Roles.moderator ?: return

		event<MessageCreateEvent> {
			action {
				service.handleMessage(event)
			}
		}

		event<GuildButtonInteractionCreateEvent> {
			check {
				failIf(!TASK_BUTTON_ID.matches(event.interaction.componentId))
				hasRole(moderatorRole)
			}

			action {
				service.handleButton(event.interaction)
			}
		}
	}

	private companion object {
		val TASK_BUTTON_ID = Regex("""^staff-task:(claim|resolve|cancel|reopen):\d+$""")
	}
}
