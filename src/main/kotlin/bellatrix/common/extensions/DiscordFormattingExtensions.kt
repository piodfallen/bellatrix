package bellatrix.common.extensions

import bellatrix.common.discord.CustomEmoji
val String.bold: String
	get() = "**$this**"

val String.strikethrough: String
	get() = "~~$this~~"

fun CustomEmoji?.prefix(message: String): String =
	this
		?.let { "$it $message" }
		?: message
