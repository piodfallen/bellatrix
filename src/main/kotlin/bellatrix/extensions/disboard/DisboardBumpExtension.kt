package bellatrix.extensions.disboard

import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class DisboardBumpExtension : Extension() {
	override val name = "disboard-bump"

	private val service = DisboardBumpService()
	private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	override suspend fun setup() {
		service.restorePendingReminders(kord, schedulerScope)

		event<MessageCreateEvent> {
			action {
				service.handleMessage(event.message, schedulerScope)
			}
		}
	}

	override suspend fun unload() {
		schedulerScope.cancel()
	}
}
