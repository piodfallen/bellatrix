package bellatrix.extensions.starboard

import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveAllEvent
import dev.kord.core.event.message.ReactionRemoveEmojiEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event

class StarboardExtension : Extension() {
	override val name = "starboard"

	private val service = StarboardService()

	override suspend fun setup() {
		event<ReactionAddEvent> {
			action {
				service.handleReaction(
					kord = event.kord,
					guildId = event.guildId,
					channelId = event.channelId,
					messageId = event.messageId,
					emoji = event.emoji,
				)
			}
		}

		event<ReactionRemoveEvent> {
			action {
				service.handleReaction(
					kord = event.kord,
					guildId = event.guildId,
					channelId = event.channelId,
					messageId = event.messageId,
					emoji = event.emoji,
				)
			}
		}

		event<ReactionRemoveEmojiEvent> {
			action {
				service.handleReaction(
					kord = event.kord,
					guildId = event.guildId,
					channelId = event.channelId,
					messageId = event.messageId,
					emoji = event.emoji,
				)
			}
		}

		event<ReactionRemoveAllEvent> {
			action {
				service.removeEntry(
					kord = event.kord,
					originalMessageId = event.messageId,
				)
			}
		}

		event<MessageDeleteEvent> {
			action {
				service.removeEntry(
					kord = event.kord,
					originalMessageId = event.messageId,
				)
			}
		}

		event<MessageUpdateEvent> {
			action {
				service.refreshEntry(
					kord = event.kord,
					channelId = event.channelId,
					originalMessageId = event.messageId,
				)
			}
		}
	}
}
