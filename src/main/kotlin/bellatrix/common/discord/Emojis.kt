package bellatrix.common.discord

import bellatrix.common.Constants
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake

object Emojis {
	val check: CustomEmoji?
		get() = customEmoji("check")

	val add: CustomEmoji?
		get() = customEmoji("add")

	val remove: CustomEmoji?
		get() = customEmoji("remove")

	val gear: CustomEmoji?
		get() = customEmoji("gear")

	val hashtag: CustomEmoji?
		get() = customEmoji("hashtag")

	val backspace: CustomEmoji?
		get() = customEmoji("backspace")

	val home: CustomEmoji?
		get() = customEmoji("home")

	val pencil: CustomEmoji?
		get() = customEmoji("pencil")

	val image: CustomEmoji?
		get() = customEmoji("image")

	val thumbnail: CustomEmoji?
		get() = customEmoji("thumbnail")

	val foot: CustomEmoji?
		get() = customEmoji("foot")

	val palette: CustomEmoji?
		get() = customEmoji("palette")

	val article: CustomEmoji?
		get() = customEmoji("article")

	val warning: CustomEmoji?
		get() = customEmoji("warning")

	val danger: CustomEmoji?
		get() = customEmoji("danger")

	val info: CustomEmoji?
		get() = customEmoji("info")

	val text: CustomEmoji?
		get() = customEmoji("text")

	val lock: CustomEmoji?
		get() = customEmoji("lock")

	val unlock: CustomEmoji?
		get() = customEmoji("unlock")

	val user: CustomEmoji?
		get() = customEmoji("user")

	val userCheck: CustomEmoji?
		get() = customEmoji("user-check")

	val calendar: CustomEmoji?
		get() = customEmoji("calendar")

	val starYellow: CustomEmoji?
		get() = customEmoji("star-yellow")

	val starRed: CustomEmoji?
		get() = customEmoji("star-red")

	val starGreen: CustomEmoji?
		get() = customEmoji("star-green")

	val starBlue: CustomEmoji?
		get() = customEmoji("star-blue")

	private fun customEmoji(name: String): CustomEmoji? =
		Constants.optionalSnowflake("emojis.$name")
			?.let { id -> CustomEmoji(name, id) }
}

data class CustomEmoji(
	val name: String,
	val id: Snowflake,
) {
	private val discordName: String
		get() = name.replace(INVALID_EMOJI_NAME_CHARACTERS, "_")

	val mention: String
		get() = "<:$discordName:$id>"

	val partial: DiscordPartialEmoji
		get() = DiscordPartialEmoji(
			id = id,
			name = discordName,
		)

	override fun toString(): String =
		mention

	private companion object {
		val INVALID_EMOJI_NAME_CHARACTERS = Regex("[^A-Za-z0-9_]")
	}
}
