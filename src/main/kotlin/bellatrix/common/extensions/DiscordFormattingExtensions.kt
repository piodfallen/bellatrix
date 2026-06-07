package bellatrix.common.extensions

import bellatrix.common.discord.CustomEmoji
import dev.kord.common.entity.Snowflake

val String.bold: String
	get() = "**$this**"

val String.strikethrough: String
	get() = "~~$this~~"

val Snowflake.roleMention: String
	get() = "<@&$this>"

val Snowflake.userMention: String
	get() = "<@$this>"

fun CustomEmoji?.prefix(message: String): String =
	this
		?.let { "$it $message" }
		?: message
