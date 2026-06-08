package bellatrix.common.discord

import bellatrix.common.Constants
import dev.kord.common.entity.Snowflake

object DiscordSettings {
	val starboardMinimumStars: Int
		get() = Constants.optionalInt("starboard.minimum-stars") ?: DEFAULT_STARBOARD_MINIMUM_STARS

	val disboardBotId: Snowflake
		get() = Constants.optionalSnowflake("disboard.bot-id") ?: DEFAULT_DISBOARD_BOT_ID

	private const val DEFAULT_STARBOARD_MINIMUM_STARS = 2

	private val DEFAULT_DISBOARD_BOT_ID = Snowflake(DEFAULT_DISBOARD_BOT_ID_VALUE)
	private const val DEFAULT_DISBOARD_BOT_ID_VALUE = "302050872383242240"
}
