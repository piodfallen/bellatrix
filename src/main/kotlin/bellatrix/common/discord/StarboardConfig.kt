package bellatrix.common.discord

import bellatrix.common.Constants

object StarboardConfig {
	val minimumStars: Int
		get() = Constants.optionalInt("starboard.minimum-stars") ?: DEFAULT_MINIMUM_STARS

	private const val DEFAULT_MINIMUM_STARS = 2
}
