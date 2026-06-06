package bellatrix.utils

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

	private fun customEmoji(name: String): CustomEmoji? =
		Constants.optionalSnowflake("emojis.$name")
			?.let { id -> CustomEmoji(name, id) }
}

data class CustomEmoji(
	val name: String,
	val id: Snowflake,
) {
	val mention: String
		get() = "<:$name:$id>"

	val partial: DiscordPartialEmoji
		get() = DiscordPartialEmoji(
			id = id,
			name = name,
		)

	override fun toString(): String =
		mention
}
