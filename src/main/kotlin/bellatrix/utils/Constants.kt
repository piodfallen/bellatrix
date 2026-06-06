package bellatrix.utils

import dev.kord.common.entity.Snowflake
import java.util.Properties

object Constants {
	private val properties: Properties by lazy {
		Properties().also { properties ->
			Thread.currentThread()
				.contextClassLoader
				.getResourceAsStream("bellatrix.properties")
				?.use(properties::load)
		}
	}

	val welcomeChannelId: Snowflake?
		get() = optionalSnowflake("channels.global")

	internal fun optionalSnowflake(key: String): Snowflake? =
		optionalString(key)?.let(::Snowflake)

	internal fun optionalString(key: String): String? =
		properties.getProperty(key)
			?.trim()
			?.takeIf { it.isNotBlank() }
}
