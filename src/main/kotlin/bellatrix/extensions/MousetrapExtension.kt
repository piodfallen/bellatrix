package bellatrix.extensions

import bellatrix.common.discord.Channels
import dev.kord.core.behavior.ban
import dev.kord.core.entity.Member
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import kotlinx.coroutines.flow.toList
import kotlin.time.Duration.Companion.days

class MousetrapExtension : Extension() {
	override val name = "mousetrap"

	override suspend fun setup() {
		val channelId = Channels.mousetrap ?: return

		event<MessageCreateEvent> {
			action {
				val message = event.message
				val member = event.member ?: return@action

				if (message.channelId != channelId) return@action
				if (message.author?.isBot != false) return@action
				if (!member.canBePunished()) return@action

				runCatching {
					member.ban {
						deleteMessageDuration = 1.days
					}
				}
			}
		}
	}

	private suspend fun Member.canBePunished(): Boolean {
		val guild = getGuildOrNull() ?: return false

		if (guild.ownerId == id) return false

		val self = guild.getMemberOrNull(kord.selfId) ?: return false

		return highestRolePosition() < self.highestRolePosition()
	}

	private suspend fun Member.highestRolePosition(): Int =
		roles
			.toList()
			.maxOfOrNull { it.rawPosition }
			?: EVERYONE_ROLE_POSITION

	private companion object {
		const val EVERYONE_ROLE_POSITION = 0
	}
}
