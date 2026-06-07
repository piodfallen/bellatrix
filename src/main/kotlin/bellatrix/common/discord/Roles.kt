package bellatrix.common.discord

import bellatrix.common.Constants
import dev.kord.common.entity.Snowflake

object Roles {
	val moderator: Snowflake?
		get() = Constants.optionalSnowflake("roles.moderator")
}
