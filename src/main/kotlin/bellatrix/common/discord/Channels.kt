package bellatrix.common.discord

import bellatrix.common.Constants
import dev.kord.common.entity.Snowflake

object Channels {
	val welcome: Snowflake?
		get() = Constants.optionalSnowflake("channels.global")
}
