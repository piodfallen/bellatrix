package bellatrix.extensions.embed

import bellatrix.common.discord.CustomEmoji
import bellatrix.common.discord.Emojis
import bellatrix.common.discord.Res
import bellatrix.common.extensions.prefix
import bellatrix.i18n.Translations
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageBuilder
import dev.kordex.core.components.ComponentContainer
import dev.kordex.core.components.ComponentContext
import dev.kordex.core.components.buttons.EphemeralInteractionButtonContext
import dev.kordex.core.components.components
import dev.kordex.core.components.ephemeralButton
import dev.kordex.core.components.ephemeralStringSelectMenu
import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.components.forms.widgets.LineTextWidget
import dev.kordex.core.components.forms.widgets.ParagraphTextWidget
import dev.kordex.core.components.menus.string.StringSelectMenu
import dev.kordex.core.types.EphemeralInteractionContext
import dev.kordex.i18n.Key
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

class EmbedEditorService {
	suspend fun renderPanel(
		builder: MessageBuilder,
		state: EmbedEditorState,
		locale: Locale,
	) {
		builder.content = state.statusText(locale)
		builder.embeds = mutableListOf(
			state.currentEmbed.toEmbedBuilder(Translations.Embed.Editor.Preview.empty.withLocale(locale).translate()),
		)

		builder.components(timeout = EDITOR_TIMEOUT) {
			when (state.mode) {
				EditorMode.Main -> mainComponents(state, locale)
				EditorMode.Embeds -> embedComponents(state, locale)
				EditorMode.Fields -> fieldComponents(state, locale)
			}
		}
	}

	private suspend fun ComponentContainer.mainComponents(
		state: EmbedEditorState,
		locale: Locale,
	) {
		ephemeralStringSelectMenu(row = PROPERTY_ROW) {
			placeholder = Translations.Embed.Editor.Select.property.withLocale(locale)
			minimumChoices = 1
			maximumChoices = 1

			EmbedProperty.entries.forEach { property ->
				option(property.label.withLocale(locale), property.value) {
					default = property == state.selectedProperty
					partialEmoji = propertyEmoji(property)?.partial
				}
			}

			action {
				state.selectedProperty = selected.firstOrNull()?.let(EmbedProperty::fromValue)
				state.selectedFieldIndex = null
				editPanel(state)
			}
		}

		ephemeralButton(modal = { PropertyModalForm(state) }, row = ACTION_ROW) {
			label = Translations.Embed.Editor.Button.edit.withLocale(locale)
			style = ButtonStyle.Primary
			disabled = state.selectedProperty == null
			partialEmoji = Emojis.pencil?.partial

			action { modal ->
				modal?.applyTo(state)
				editPanel(state)
			}
		}

		ephemeralButton(row = ACTION_ROW) {
			label = Translations.Embed.Editor.Button.clear.withLocale(locale)
			style = ButtonStyle.Secondary
			disabled = state.selectedProperty == null
			partialEmoji = Emojis.backspace?.partial

			action {
				state.selectedProperty?.let(state.currentEmbed::clear)
				editPanel(state)
			}
		}

		ephemeralButton(row = ACTION_ROW) {
			label = Translations.Embed.Editor.Button.fields.withLocale(locale)
			style = ButtonStyle.Secondary
			partialEmoji = Emojis.hashtag?.partial

			action {
				state.mode = EditorMode.Fields
				state.selectedFieldIndex = null
				editPanel(state)
			}
		}

		ephemeralButton(row = ACTION_ROW) {
			label = Translations.Embed.Editor.Button.embeds.withLocale(locale)
			style = ButtonStyle.Secondary
			partialEmoji = Emojis.gear?.partial

			action {
				state.mode = EditorMode.Embeds
				state.selectedFieldIndex = null
				editPanel(state)
			}
		}

		ephemeralButton(row = ACTION_ROW) {
			label = state.submitButtonLabel().withLocale(locale)
			style = ButtonStyle.Success
			disabled = !state.canSend()
			partialEmoji = Emojis.check?.partial

			action {
				sendOrUpdateEmbeds(this, state)
			}
		}
	}

	private suspend fun ComponentContainer.embedComponents(
		state: EmbedEditorState,
		locale: Locale,
	) {
		ephemeralStringSelectMenu(row = PROPERTY_ROW) {
			placeholder = Translations.Embed.Editor.Select.embed.withLocale(locale)
			minimumChoices = 1
			maximumChoices = 1

			state.embeds.forEachIndexed { index, embed ->
				option(
					Translations.Embed.Editor.Option.embed
						.withNamedPlaceholders("index" to index + 1)
						.withLocale(locale),
					index.toString(),
				) {
					description = if (embed.hasContent()) {
						Translations.Embed.Editor.Option.hasContent
					} else {
						Translations.Embed.Editor.Option.empty
					}.withLocale(locale)

					default = index == state.currentEmbedIndex
					partialEmoji = Emojis.hashtag?.partial
				}
			}

			action {
				state.selectEmbed(selected.firstOrNull()?.toIntOrNull() ?: 0)
				editPanel(state)
			}
		}

		ephemeralButton(row = ACTION_ROW) {
			label = Translations.Embed.Editor.Button.add.withLocale(locale)
			style = ButtonStyle.Success
			disabled = state.embeds.size >= MAX_EMBEDS
			partialEmoji = Emojis.add?.partial

			action {
				state.addEmbed()
				editPanel(state)
			}
		}

		ephemeralButton(row = ACTION_ROW) {
			label = Translations.Embed.Editor.Button.remove.withLocale(locale)
			style = ButtonStyle.Danger
			disabled = state.embeds.size == 1
			partialEmoji = Emojis.remove?.partial

			action {
				state.removeCurrentEmbed()
				editPanel(state)
			}
		}

		backButton(state, locale)
	}

	private suspend fun ComponentContainer.fieldComponents(
		state: EmbedEditorState,
		locale: Locale,
	) {
		ephemeralStringSelectMenu(row = PROPERTY_ROW) {
			placeholder = Translations.Embed.Editor.Select.field.withLocale(locale)
			minimumChoices = 1
			maximumChoices = 1
			disabled = state.currentEmbed.fields.isEmpty()

			fieldOptions(state, locale)

			action {
				state.selectedFieldIndex = selected.firstOrNull()?.toIntOrNull()
				editPanel(state)
			}
		}

		ephemeralButton(modal = { FieldModalForm(state, null) }, row = ACTION_ROW) {
			label = Translations.Embed.Editor.Button.add.withLocale(locale)
			style = ButtonStyle.Success
			disabled = state.currentEmbed.fields.size >= MAX_FIELDS
			partialEmoji = Emojis.add?.partial

			action { modal ->
				modal?.applyTo(state)
				editPanel(state)
			}
		}

		ephemeralButton(modal = { FieldModalForm(state, state.selectedFieldIndex) }, row = ACTION_ROW) {
			label = Translations.Embed.Editor.Button.edit.withLocale(locale)
			style = ButtonStyle.Primary
			disabled = state.selectedFieldIndex !in state.currentEmbed.fields.indices
			partialEmoji = Emojis.pencil?.partial

			action { modal ->
				modal?.applyTo(state)
				editPanel(state)
			}
		}

		ephemeralButton(row = ACTION_ROW) {
			label = Translations.Embed.Editor.Button.remove.withLocale(locale)
			style = ButtonStyle.Danger
			disabled = state.selectedFieldIndex !in state.currentEmbed.fields.indices
			partialEmoji = Emojis.remove?.partial

			action {
				state.selectedFieldIndex
					?.takeIf { it in state.currentEmbed.fields.indices }
					?.let(state.currentEmbed.fields::removeAt)

				state.selectedFieldIndex = null
				editPanel(state)
			}
		}

		backButton(state, locale)
	}

	private suspend fun ComponentContainer.backButton(
		state: EmbedEditorState,
		locale: Locale,
	) {
		ephemeralButton(row = ACTION_ROW) {
			label = Translations.Embed.Editor.Button.back.withLocale(locale)
			style = ButtonStyle.Secondary
			partialEmoji = Emojis.home?.partial

			action {
				state.mode = EditorMode.Main
				state.selectedFieldIndex = null
				editPanel(state)
			}
		}
	}

	private suspend fun StringSelectMenu.fieldOptions(
		state: EmbedEditorState,
		locale: Locale,
	) {
		if (state.currentEmbed.fields.isEmpty()) {
			option(Translations.Embed.Editor.Option.noFields.withLocale(locale), NONE_VALUE) {
				partialEmoji = Emojis.hashtag?.partial
			}
			return
		}

		state.currentEmbed.fields.take(MAX_SELECT_OPTIONS).forEachIndexed { index, field ->
			option(Key("${index + 1}. ${field.name}".take(MAX_SELECT_LABEL)), index.toString()) {
				default = index == state.selectedFieldIndex
				partialEmoji = Emojis.hashtag?.partial
			}
		}
	}

	private suspend fun ComponentContext<*>.editPanel(state: EmbedEditorState) {
		val locale = getLocale()

		(this as EphemeralInteractionContext).edit {
			renderPanel(this, state, locale)
		}
	}

	private suspend fun sendOrUpdateEmbeds(
		context: EphemeralInteractionButtonContext<ModalForm>,
		state: EmbedEditorState,
	) {
		if (!state.canSend()) {
			context.editPanel(state)
			return
		}

		when (val target = state.target) {
			is EditorTarget.Create ->
				MessageChannelBehavior(target.channelId, context.event.interaction.kord)
					.createMessage {
						embeds = state.toEmbedBuilders()
					}

			is EditorTarget.Edit ->
				MessageBehavior(
					target.channelId,
					target.messageId,
					context.event.interaction.kord,
					EntitySupplyStrategy.cacheWithCachingRestFallback,
				).edit {
					content = null
					embeds = state.toEmbedBuilders()
					components = mutableListOf()
				}
		}

		context.edit {
			Res.success(
				when (state.target) {
					is EditorTarget.Create -> Translations.Embed.Editor.sent
					is EditorTarget.Edit -> Translations.Embed.Editor.updated
				},
				context,
			).applyTo(this)
			components = mutableListOf()
		}
	}

	private fun EmbedEditorState.statusText(locale: Locale): String =
		Emojis.hashtag.prefix(
			Translations.Embed.Editor.Status.current.withNamedPlaceholders(
				"current" to currentEmbedIndex + 1,
				"total" to embeds.size,
			).withLocale(locale).translate(),
		)

	private fun EmbedEditorState.submitButtonLabel(): Key =
		when (target) {
			is EditorTarget.Create -> Translations.Embed.Editor.Button.send
			is EditorTarget.Edit -> Translations.Embed.Editor.Button.edit
		}

	private fun propertyEmoji(property: EmbedProperty): CustomEmoji? =
		when (property) {
			EmbedProperty.Title -> Emojis.text
			EmbedProperty.Description -> Emojis.article
			EmbedProperty.Image -> Emojis.image
			EmbedProperty.Thumbnail -> Emojis.thumbnail
			EmbedProperty.Footer -> Emojis.foot
			EmbedProperty.Color -> Emojis.palette
		}

	private fun EditableEmbed.toEmbedBuilder(previewFallback: String? = null): EmbedBuilder =
		EmbedBuilder().also { applyTo(it, previewFallback) }

	private fun EmbedEditorState.toEmbedBuilders(): MutableList<EmbedBuilder> =
		embeds.map { it.toEmbedBuilder() }.toMutableList()

	private fun parseColor(value: String): Color? {
		val normalized = value.trim().removePrefix("#")
		val rgb = normalized.toIntOrNull(HEX_RADIX) ?: return null

		return Color(rgb)
	}

	private inner class PropertyModalForm(private val state: EmbedEditorState) : ModalForm() {
		override var title: Key = Translations.Embed.Editor.Modal.edit.withNamedPlaceholders(
			"property" to propertyLabel(),
		)

		val value: LineTextWidget?
		val description: ParagraphTextWidget?
		val footerText: LineTextWidget?
		val footerIcon: LineTextWidget?

		init {
			val property = state.selectedProperty
			val embed = state.currentEmbed

			value = when (property) {
				EmbedProperty.Title -> lineInput(Translations.Embed.Editor.Property.title, embed.title)
				EmbedProperty.Image -> lineInput(Translations.Embed.Editor.Input.imageUrl, embed.image)
				EmbedProperty.Thumbnail -> lineInput(Translations.Embed.Editor.Input.thumbnailUrl, embed.thumbnail)
				EmbedProperty.Color -> lineInput(
					Translations.Embed.Editor.Input.color,
					embed.color
						?.rgb
						?.toString(HEX_RADIX)
						?.padStart(COLOR_HEX_LENGTH, '0')
						?.let { "#$it" },
					placeholder = Translations.Embed.Editor.Placeholder.color,
				)
				else -> null
			}

			description = if (property == EmbedProperty.Description) {
				paragraphInput(Translations.Embed.Editor.Property.description, embed.description)
			} else {
				null
			}

			footerText = if (property == EmbedProperty.Footer) {
				lineInput(Translations.Embed.Editor.Input.footerText, embed.footer?.text)
			} else {
				null
			}

			footerIcon = if (property == EmbedProperty.Footer) {
				lineInput(Translations.Embed.Editor.Input.footerIcon, embed.footer?.iconUrl)
			} else {
				null
			}
		}

		fun applyTo(state: EmbedEditorState) {
			val embed = state.currentEmbed

			when (state.selectedProperty) {
				EmbedProperty.Title -> embed.title = value.inputValue()
				EmbedProperty.Description -> embed.description = description.inputValue()
				EmbedProperty.Image -> embed.image = value.inputValue()
				EmbedProperty.Thumbnail -> embed.thumbnail = value.inputValue()

				EmbedProperty.Footer -> {
					val footer = EditableFooter(
						text = footerText.inputValue(),
						iconUrl = footerIcon.inputValue(),
					)
					embed.footer = footer.takeIf(EditableFooter::hasContent)
				}
				EmbedProperty.Color -> embed.color = value.inputValue()?.let(::parseColor)
				null -> Unit
			}
		}

		private fun propertyLabel(): Key =
			state.selectedProperty
				?.label
				?: Translations.Embed.Editor.Button.edit
	}

	private inner class FieldModalForm(
		state: EmbedEditorState,
		private val fieldIndex: Int?,
	) : ModalForm() {
		private val field: EditableField? =
			fieldIndex
				?.takeIf { it in state.currentEmbed.fields.indices }
				?.let { state.currentEmbed.fields[it] }

		override var title: Key =
			if (field == null) {
				Translations.Embed.Editor.Modal.fieldAdd
			} else {
				Translations.Embed.Editor.Modal.fieldEdit
			}

		val fieldName = lineInput(Translations.Embed.Editor.Input.fieldName, field?.name, required = true)
		val fieldValue = paragraphInput(Translations.Embed.Editor.Input.fieldValue, field?.value, required = true)

		val inline = lineInput(
			Translations.Embed.Editor.Input.fieldInline,
			field?.inline?.toString(),
			placeholder = Translations.Embed.Editor.Placeholder.boolean,
		)

		fun applyTo(state: EmbedEditorState) {
			val name = fieldName.inputValue() ?: return
			val value = fieldValue.inputValue() ?: return

			val field = EditableField(
				name = name,
				value = value,
				inline = inline.inputValue()?.toBooleanStrictOrNull() ?: false,
			)

			if (fieldIndex != null && fieldIndex in state.currentEmbed.fields.indices) {
				state.currentEmbed.fields[fieldIndex] = field
			} else {
				state.currentEmbed.fields += field
				state.selectedFieldIndex = state.currentEmbed.fields.lastIndex
			}
		}
	}

	private fun ModalForm.lineInput(
		labelKey: Key,
		value: String?,
		required: Boolean = false,
		placeholder: Key? = null,
	): LineTextWidget =
		lineText {
			label = labelKey
			this.required = required
			value?.takeIf(String::isNotBlank)?.let {
				initialValue = Key(it)
				translateInitialValue = false
			}
			placeholder?.let { this.placeholder = it }
		}

	private fun ModalForm.paragraphInput(
		labelKey: Key,
		value: String?,
		required: Boolean = false,
	): ParagraphTextWidget =
		paragraphText {
			label = labelKey
			this.required = required
			value?.takeIf(String::isNotBlank)?.let {
				initialValue = Key(it)
				translateInitialValue = false
			}
		}

	private fun LineTextWidget?.inputValue(): String? =
		this
			?.value
			?.trim()
			?.takeIf { it.isNotBlank() }

	private fun ParagraphTextWidget?.inputValue(): String? =
		this
			?.value
			?.trim()
			?.takeIf { it.isNotBlank() }

	private companion object {
		const val PROPERTY_ROW = 0
		const val ACTION_ROW = 1
		const val NONE_VALUE = "none"
		const val HEX_RADIX = 16
		const val COLOR_HEX_LENGTH = 6
		const val MAX_EMBEDS = 10
		const val MAX_FIELDS = 25
		const val MAX_SELECT_OPTIONS = 25
		const val MAX_SELECT_LABEL = 100
		val EDITOR_TIMEOUT = 15.minutes
	}
}
