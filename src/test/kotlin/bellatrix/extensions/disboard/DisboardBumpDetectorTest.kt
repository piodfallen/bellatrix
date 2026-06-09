package bellatrix.extensions.disboard

import dev.kord.common.entity.Snowflake
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisboardBumpDetectorTest {
	private val disboardBotId = Snowflake("302050872383242240")
	private val otherBotId = Snowflake("123456789012345678")

	@Test
	fun `detects bump from Disboard application response`() {
		val detected = DisboardBumpDetector.isSuccessfulBump(
			disboardBotId = disboardBotId,
			authorId = null,
			applicationId = disboardBotId,
			embeds = listOf(
				DisboardBumpEmbed(
					description = "Bump done! :thumbsup:\nCheck it out on DISBOARD.",
				),
			),
		)

		assertTrue(detected)
	}

	@Test
	fun `detects bump from Disboard bot author`() {
		val detected = DisboardBumpDetector.isSuccessfulBump(
			disboardBotId = disboardBotId,
			authorId = disboardBotId,
			applicationId = null,
			embeds = listOf(
				DisboardBumpEmbed(
					description = "Bump done! 👍",
				),
			),
		)

		assertTrue(detected)
	}

	@Test
	fun `ignores bump done text outside embed description`() {
		val detected = DisboardBumpDetector.isSuccessfulBump(
			disboardBotId = disboardBotId,
			authorId = disboardBotId,
			applicationId = null,
			embeds = listOf(
				DisboardBumpEmbed(
					description = null,
				),
			),
		)

		assertFalse(detected)
	}

	@Test
	fun `ignores non Disboard messages with bump text`() {
		val detected = DisboardBumpDetector.isSuccessfulBump(
			disboardBotId = disboardBotId,
			authorId = otherBotId,
			applicationId = otherBotId,
			embeds = listOf(
				DisboardBumpEmbed(
					description = "Bump done!",
				),
			),
		)

		assertFalse(detected)
	}

	@Test
	fun `ignores Disboard messages without bump done text`() {
		val detected = DisboardBumpDetector.isSuccessfulBump(
			disboardBotId = disboardBotId,
			authorId = disboardBotId,
			applicationId = null,
			embeds = listOf(
				DisboardBumpEmbed(
					description = "Please wait before bumping again.",
				),
			),
		)

		assertFalse(detected)
	}

	@Test
	fun `detects Disboard interaction response from raw author metadata`() {
		val detected = DisboardBumpDetector.isSuccessfulBump(
			disboardBotId = disboardBotId,
			authorId = disboardBotId,
			applicationId = null,
			embeds = listOf(
				DisboardBumpEmbed(
					description = "Bump done!",
				),
			),
		)

		assertTrue(detected)
	}
}
