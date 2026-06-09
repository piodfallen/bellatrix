package bellatrix.extensions.disboard

import dev.kord.common.entity.Snowflake

internal object DisboardBumpDetector {
	private const val BUMP_DONE_TEXT = "Bump done"

	fun isSuccessfulBump(
		disboardBotId: Snowflake,
		authorId: Snowflake?,
		applicationId: Snowflake?,
		embeds: List<DisboardBumpEmbed>,
	): Boolean {
		val fromDisboard = applicationId == disboardBotId ||
			authorId == disboardBotId

		if (!fromDisboard) return false

		return embeds.any { it.hasBumpDone() }
	}

	private fun DisboardBumpEmbed.hasBumpDone(): Boolean =
		description?.contains(BUMP_DONE_TEXT, ignoreCase = true) == true
}

internal data class DisboardBumpEmbed(
	val description: String?,
)
