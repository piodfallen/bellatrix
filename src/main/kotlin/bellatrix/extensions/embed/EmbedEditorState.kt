package bellatrix.extensions.embed

import bellatrix.i18n.Translations
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Embed
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kordex.i18n.Key
import java.net.URI

class EmbedEditorState(
	var mode: EditorMode = EditorMode.Main,
	val target: EditorTarget,
	val embeds: MutableList<EditableEmbed> = mutableListOf(EditableEmbed()),
	var currentEmbedIndex: Int = 0,
	var selectedProperty: EmbedProperty? = null,
	var selectedFieldIndex: Int? = null,
) {
	val currentEmbed: EditableEmbed
		get() = embeds[currentEmbedIndex]

	fun selectEmbed(index: Int) {
		currentEmbedIndex = index.coerceIn(embeds.indices)
		selectedFieldIndex = null
	}

	fun addEmbed() {
		embeds += EditableEmbed()
		selectEmbed(embeds.lastIndex)
	}

	fun removeCurrentEmbed() {
		if (embeds.size == 1) return

		embeds.removeAt(currentEmbedIndex)
		selectEmbed(currentEmbedIndex.coerceAtMost(embeds.lastIndex))
	}

	fun canSend(): Boolean =
		embeds.all(EditableEmbed::hasContent)

	companion object {
		fun fromDiscordEmbeds(
			target: EditorTarget,
			embeds: List<Embed>,
		): EmbedEditorState =
			EmbedEditorState(
				target = target,
				embeds = embeds.map(EditableEmbed::fromDiscordEmbed).toMutableList(),
			)
	}
}

enum class EditorMode {
	Main,
	Embeds,
	Fields,
}

enum class EmbedProperty(
	val value: String,
	val label: Key,
) {
	Title("title", Translations.Embed.Editor.Property.title),
	Description("description", Translations.Embed.Editor.Property.description),
	Image("image", Translations.Embed.Editor.Property.image),
	Thumbnail("thumbnail", Translations.Embed.Editor.Property.thumbnail),
	Footer("footer", Translations.Embed.Editor.Property.footer),
	Color("color", Translations.Embed.Editor.Property.color);

	companion object {
		fun fromValue(value: String): EmbedProperty? =
			entries.firstOrNull { it.value == value }
	}
}

sealed interface EditorTarget {
	data class Create(val channelId: Snowflake) : EditorTarget

	data class Edit(
		val channelId: Snowflake,
		val messageId: Snowflake,
	) : EditorTarget
}

class EditableEmbed(
	var title: String? = null,
	var description: String? = null,
	var image: String? = null,
	var thumbnail: String? = null,
	var footer: EditableFooter? = null,
	var color: Color? = null,
	val fields: MutableList<EditableField> = mutableListOf(),
) {
	fun hasContent(): Boolean =
		!title.isNullOrBlank() ||
			!description.isNullOrBlank() ||
			!image.isNullOrBlank() ||
			!thumbnail.isNullOrBlank() ||
			footer?.hasContent() == true ||
			fields.isNotEmpty()

	fun clear(property: EmbedProperty) {
		when (property) {
			EmbedProperty.Title -> title = null
			EmbedProperty.Description -> description = null
			EmbedProperty.Image -> image = null
			EmbedProperty.Thumbnail -> thumbnail = null
			EmbedProperty.Footer -> footer = null
			EmbedProperty.Color -> color = null
		}
	}

	fun applyTo(
		builder: EmbedBuilder,
		previewFallback: String? = null,
	) {
		builder.title = title
		builder.description = description ?: previewFallback?.takeIf { !hasContent() }
		builder.image = image.validUrlOrNull()
		builder.color = color

		thumbnail.validUrlOrNull()?.let { thumbnailUrl ->
			builder.thumbnail {
				url = thumbnailUrl
			}
		}

		footer?.takeIf(EditableFooter::hasContent)?.let { editableFooter ->
			builder.footer {
				text = editableFooter.text.orEmpty()
				icon = editableFooter.iconUrl.validUrlOrNull()
			}
		}

		fields.forEach { editableField ->
			builder.field {
				name = editableField.name
				value = editableField.value
				inline = editableField.inline
			}
		}
	}

	companion object {
		fun fromDiscordEmbed(embed: Embed): EditableEmbed =
			EditableEmbed(
				title = embed.title,
				description = embed.description,
				image = embed.image?.url,
				thumbnail = embed.thumbnail?.url,
				footer = embed.footer?.let { footer ->
					EditableFooter(
						text = footer.text,
						iconUrl = footer.iconUrl,
					)
				},
				color = embed.color,
				fields = embed.fields.map { field ->
					EditableField(
						name = field.name,
						value = field.value,
						inline = field.inline == true,
					)
				}.toMutableList(),
			)
	}
}

internal fun String?.validUrlOrNull(): String? =
	this
		?.trim()
		?.takeIf(String::isNotBlank)
		?.takeIf { value ->
			runCatching {
				val uri = URI(value)

				uri.scheme in VALID_URL_SCHEMES && !uri.host.isNullOrBlank()
			}.getOrDefault(false)
		}

private val VALID_URL_SCHEMES = setOf("http", "https")

class EditableFooter(
	var text: String? = null,
	var iconUrl: String? = null,
) {
	fun hasContent(): Boolean =
		!text.isNullOrBlank() || !iconUrl.isNullOrBlank()
}

data class EditableField(
	val name: String,
	val value: String,
	val inline: Boolean = false,
)
