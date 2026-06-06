package bellatrix.common.discord

import bellatrix.common.Constants
import dev.kord.common.entity.Snowflake

object Channels {
	val global: Snowflake?
		get() = Constants.optionalSnowflake("channels.global")
}
