package bellatrix.extensions.modmail

import dev.kord.core.Kord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class ModmailScheduler(
	private val service: ModmailService,
	private val kord: Kord,
) {
	fun start(scope: CoroutineScope): Job =
		scope.launch {
			while (isActive) {
				service.closeInactive(kord)
				delay(CHECK_INTERVAL)
			}
		}

	private companion object {
		val CHECK_INTERVAL = 15.minutes
	}
}
