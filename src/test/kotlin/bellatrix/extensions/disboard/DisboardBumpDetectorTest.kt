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
			authorIsBot = null,
			applicationId = disboardBotId,
			embeds = listOf(
				DisboardBumpEmbed(
					title = "DISBOARD: The Public Server List",
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
			authorIsBot = true,
			applicationId = null,
			embeds = listOf(
				DisboardBumpEmbed(
					title = null,
					description = "Bump done! 👍",
				),
			),
		)

		assertTrue(detected)
	}

	@Test
	fun `detects bump done text in embed title`() {
		val detected = DisboardBumpDetector.isSuccessfulBump(
			disboardBotId = disboardBotId,
			authorId = disboardBotId,
			authorIsBot = true,
			applicationId = null,
			embeds = listOf(
				DisboardBumpEmbed(
					title = "Bump done!",
					description = null,
				),
			),
		)

		assertTrue(detected)
	}

	@Test
	fun `ignores non Disboard messages with bump text`() {
		val detected = DisboardBumpDetector.isSuccessfulBump(
			disboardBotId = disboardBotId,
			authorId = otherBotId,
			authorIsBot = true,
			applicationId = otherBotId,
			embeds = listOf(
				DisboardBumpEmbed(
					title = null,
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
			authorIsBot = true,
			applicationId = null,
			embeds = listOf(
				DisboardBumpEmbed(
					title = "DISBOARD: The Public Server List",
					description = "Please wait before bumping again.",
				),
			),
		)

		assertFalse(detected)
	}

	@Test
	fun `ignores user messages with Disboard id shape`() {
		val detected = DisboardBumpDetector.isSuccessfulBump(
			disboardBotId = disboardBotId,
			authorId = disboardBotId,
			authorIsBot = false,
			applicationId = null,
			embeds = listOf(
				DisboardBumpEmbed(
					title = null,
					description = "Bump done!",
				),
			),
		)

		assertFalse(detected)
	}
}
