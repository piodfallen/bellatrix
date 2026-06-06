package bellatrix.utils

import dev.kord.common.Color

object Colors {
	private const val HEX_RADIX = 16

	val success: Color
		get() = color("colors.response.success")

	val info: Color
		get() = color("colors.response.info")

	val warning: Color
		get() = color("colors.response.warning")

	val danger: Color
		get() = color("colors.response.danger")

	private fun color(key: String): Color {
		val value = Constants.optionalString(key)
			?: error("Missing required color resource: $key")

		val rgb = value
			.trim()
			.removePrefix("#")
			.toIntOrNull(HEX_RADIX)
			?: error("Invalid color resource '$key': $value")

		return Color(rgb)
	}
}
