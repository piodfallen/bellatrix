package bellatrix.extensions

import bellatrix.common.discord.Channels
import bellatrix.common.discord.DiscordSettings
import bellatrix.common.discord.Emojis
import bellatrix.common.discord.Roles
import bellatrix.common.extensions.prefix
import bellatrix.database.models.DisboardBumpReminder
import bellatrix.database.repositories.DisboardBumpReminderRepository
import bellatrix.i18n.GuildLocaleResolver
import bellatrix.i18n.Translations
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.RoleBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

class DisboardBumpExtension : Extension() {
	override val name = "disboard-bump"

	private val repository = DisboardBumpReminderRepository
	private val jobs = mutableMapOf<Snowflake, Job>()
	private val jobsMutex = Mutex()
	private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	override suspend fun setup() {
		restorePendingReminders()

		event<MessageCreateEvent> {
			action {
				handleMessage(event.message)
			}
		}
	}

	override suspend fun unload() {
		schedulerScope.cancel()
	}

	private suspend fun restorePendingReminders() {
		repository.findAll().forEach { reminder ->
			scheduleReminder(reminder)
		}
	}

	private suspend fun handleMessage(message: Message) {
		val guildId = message.getGuildOrNull()?.id ?: return
		if (!message.isSuccessfulDisboardBump()) return

		val reminderChannelId = Channels.disboardBump ?: message.channelId
		val reminderRoleId = Roles.bumper ?: Roles.moderator ?: return
		val now = Instant.now()

		val reminder = repository.upsert(
			guildId = guildId,
			channelId = reminderChannelId,
			roleId = reminderRoleId,
			lastBumpAt = now,
			nextReminderAt = now.plus(BUMP_COOLDOWN),
		)

		scheduleReminder(reminder)
	}

	private suspend fun scheduleReminder(reminder: DisboardBumpReminder) {
		jobsMutex.withLock {
			jobs.remove(reminder.guildId)?.cancel()
			jobs[reminder.guildId] = schedulerScope.launch {
				delayUntil(reminder.nextReminderAt)
				sendReminder(kord, reminder)
			}
		}
	}

	private suspend fun sendReminder(
		kord: Kord,
		reminder: DisboardBumpReminder,
	) {
		val current = repository.findByGuildId(reminder.guildId) ?: return
		if (current.nextReminderAt != reminder.nextReminderAt) return

		val sent = runCatching {
			val locale = GuildLocaleResolver.resolve(kord.getGuildOrNull(current.guildId))
			val roleMention = RoleBehavior(current.guildId, current.roleId, kord).mention

			MessageChannelBehavior(current.channelId, kord).createMessage(
				Emojis.warning.prefix(
					Translations.Disboard.BumpReminder.message
						.withNamedPlaceholders("role" to roleMention)
						.withLocale(locale)
						.translate(),
				),
			)
		}.isSuccess

		if (sent) {
			repository.deleteByGuildId(current.guildId)
			jobsMutex.withLock {
				jobs.remove(current.guildId)
			}
		} else {
			val retry = repository.upsert(
				guildId = current.guildId,
				channelId = current.channelId,
				roleId = current.roleId,
				lastBumpAt = current.lastBumpAt,
				nextReminderAt = Instant.now().plus(RETRY_AFTER),
			)

			scheduleReminder(retry)
		}
	}

	private suspend fun delayUntil(target: Instant) {
		val delay = Duration.between(Instant.now(), target)
			.takeIf { !it.isNegative && !it.isZero }
			?: return

		delay(delay.toKotlinDuration())
	}

	private fun Message.isSuccessfulDisboardBump(): Boolean {
		if (author?.id != DiscordSettings.disboardBotId) return false
		if (author?.isBot != true) return false

		return embeds.any { embed ->
			embed.description.orEmpty().contains(BUMP_DONE_TEXT, ignoreCase = true)
		}
	}

	private companion object {
		val BUMP_COOLDOWN: Duration = Duration.ofHours(2)
		val RETRY_AFTER: Duration = Duration.ofMinutes(30)

		const val BUMP_DONE_TEXT = "Bump done!"
	}
}
