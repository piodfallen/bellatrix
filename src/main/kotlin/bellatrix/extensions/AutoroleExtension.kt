package bellatrix.extensions

import bellatrix.common.discord.Roles
import bellatrix.i18n.GuildLocaleResolver
import bellatrix.i18n.Translations
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event

class AutoroleExtension : Extension() {
	override val name = "autorole"

	override suspend fun setup() {
		val roleId = Roles.autorole ?: return

		event<MemberJoinEvent> {
			action {
				event.member.addRole(
					roleId = roleId,
					reason = Translations.Autorole.reason
						.withLocale(GuildLocaleResolver.resolve(event.getGuildOrNull()))
						.translate(),
				)
			}
		}
	}
}
