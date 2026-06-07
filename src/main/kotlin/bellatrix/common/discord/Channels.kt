package bellatrix.common.discord

import bellatrix.common.Constants
import dev.kord.common.entity.Snowflake

object Channels {
	val global: Snowflake?
		get() = Constants.optionalSnowflake("channels.global")

	val modmail: Snowflake?
		get() = Constants.optionalSnowflake("channels.modmail")

	val mousetrap: Snowflake?
		get() = Constants.optionalSnowflake("channels.mousetrap")
}
