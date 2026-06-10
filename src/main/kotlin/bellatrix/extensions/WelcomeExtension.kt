package bellatrix.extensions

import bellatrix.common.discord.Channels
import bellatrix.i18n.GuildLocaleResolver
import bellatrix.i18n.Translations
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event

class WelcomeExtension : Extension() {
	override val name = "welcome"

	private suspend fun sendWelcome(member: Member, guild: Guild) {
		val channelId = Channels.welcome ?: return

		val channel = guild.getChannelOrNull(channelId) as? MessageChannelBehavior ?: return
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

	override suspend fun setup() {
		event<MemberUpdateEvent> {
			action {
				val member = event.member
				if (member.isBot) return@action

				val old = event.old
				val completedOnboarding = old?.isPending == true && !member.isPending
				if (!completedOnboarding) return@action

				val guild = event.getGuildOrNull() ?: return@action
				sendWelcome(member, guild)
			}
		}
	}
}
