package bellatrix.extensions

import bellatrix.common.discord.Channels
import dev.kord.core.behavior.ban
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.canInteract
import dev.kordex.core.utils.isNullOrBot
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
				if (message.author.isNullOrBot()) return@action

				val self = event.getGuildOrNull()?.getMemberOrNull(event.kord.selfId) ?: return@action
				if (!self.canInteract(member)) return@action

				runCatching {
					member.ban {
						deleteMessageDuration = 1.days
					}
				}
			}
		}
	}
}
