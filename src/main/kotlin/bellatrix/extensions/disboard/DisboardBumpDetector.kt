package bellatrix.extensions.disboard

import dev.kord.common.entity.Snowflake

internal object DisboardBumpDetector {
	private const val BUMP_DONE_TEXT = "Bump done"

	fun isSuccessfulBump(
		disboardBotId: Snowflake,
		authorId: Snowflake?,
		authorIsBot: Boolean?,
		applicationId: Snowflake?,
		embeds: List<DisboardBumpEmbed>,
	): Boolean {
		val fromDisboard = applicationId == disboardBotId ||
			authorId == disboardBotId && authorIsBot == true

		if (!fromDisboard) return false

		return embeds.any { it.hasBumpDone() }
	}

	private fun DisboardBumpEmbed.hasBumpDone(): Boolean =
		listOfNotNull(title, description).any {
			it.contains(BUMP_DONE_TEXT, ignoreCase = true)
		}
}

internal data class DisboardBumpEmbed(
	val title: String?,
	val description: String?,
)
