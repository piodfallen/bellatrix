package bellatrix.common.discord

import bellatrix.common.extensions.prefix
import dev.kord.common.Color
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import dev.kordex.core.i18n.withContext
import dev.kordex.core.types.TranslatableContext
import dev.kordex.i18n.Key

object Res {
	suspend fun success(message: Key, context: TranslatableContext): ResponseEmbed =
		response(Emojis.check, message.withContext(context).translate(), Colors.success)

	suspend fun info(message: Key, context: TranslatableContext): ResponseEmbed =
		response(Emojis.info, message.withContext(context).translate(), Colors.info)

	suspend fun warning(message: Key, context: TranslatableContext): ResponseEmbed =
		response(Emojis.warning, message.withContext(context).translate(), Colors.warning)

	suspend fun danger(message: Key, context: TranslatableContext): ResponseEmbed =
		response(Emojis.danger, message.withContext(context).translate(), Colors.danger)

	private fun response(
		emoji: CustomEmoji?,
		message: String,
		color: Color,
	): ResponseEmbed =
		ResponseEmbed(
			description = emoji.prefix(message),
			color = color,
		)
}

class ResponseEmbed(
	val description: String,
	val color: Color,
) {
	fun toEmbed(): EmbedBuilder =
		EmbedBuilder().also { embed ->
			embed.description = description
			embed.color = color
		}

	fun applyTo(builder: MessageBuilder) {
		builder.content = null
		builder.embeds = mutableListOf(toEmbed())
	}

	fun applyTo(builder: MessageModifyBuilder) {
		builder.content = null
		builder.embeds = mutableListOf(toEmbed())
	}
}
