package bellatrix.common

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

	fun optionalSnowflake(key: String): Snowflake? =
		optionalString(key)?.let(::Snowflake)

	fun optionalString(key: String): String? =
		properties.getProperty(key)
			?.trim()
			?.takeIf { it.isNotBlank() }
}
