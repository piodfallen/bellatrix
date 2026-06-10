package bellatrix.extensions

import bellatrix.common.discord.Channels
import bellatrix.i18n.GuildLocaleResolver
import bellatrix.i18n.Translations
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event

class WelcomeExtension : Extension() {
	override val name = "welcome"

	override suspend fun setup() {
		event<MemberJoinEvent> {
			action {
				val member = event.member
				if (member.isBot) return@action

				val channelId = Channels.welcome ?: return@action
				val guild = event.getGuildOrNull() ?: return@action

				val channel = guild.getChannelOrNull(channelId) as? MessageChannelBehavior ?: return@action

				val locale = GuildLocaleResolver.resolve(guild)

				val response = Translations.Events.Welcome.message
					.withLocale(locale)
					.translateNamed(
						"member" to member.mention,
						"server" to guild.name,
					)

				channel.createMessage {
					content = response
				}
			}
		}
	}
}
