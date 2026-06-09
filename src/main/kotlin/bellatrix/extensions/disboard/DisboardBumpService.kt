package bellatrix.extensions.disboard

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

class DisboardBumpService(
	private val repository: DisboardBumpReminderRepository = DisboardBumpReminderRepository,
) {
	private val jobs = mutableMapOf<Snowflake, Job>()
	private val jobsMutex = Mutex()

	suspend fun restorePendingReminders(
		kord: Kord,
		scope: CoroutineScope,
	) {
		repository.findAll().forEach { reminder ->
			scheduleReminder(kord, scope, reminder)
		}
	}

	suspend fun handleMessage(
		message: Message,
		scope: CoroutineScope,
	) {
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

		scheduleReminder(message.kord, scope, reminder)
		thankBumper(message, guildId)
	}

	private suspend fun scheduleReminder(
		kord: Kord,
		scope: CoroutineScope,
		reminder: DisboardBumpReminder,
	) {
		jobsMutex.withLock {
			jobs.remove(reminder.guildId)?.cancel()
			jobs[reminder.guildId] = scope.launch {
				delayUntil(reminder.nextReminderAt)
				sendReminder(kord, scope, reminder)
			}
		}
	}

	private suspend fun sendReminder(
		kord: Kord,
		scope: CoroutineScope,
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

			scheduleReminder(kord, scope, retry)
		}
	}

	private suspend fun delayUntil(target: Instant) {
		val delay = Duration.between(Instant.now(), target)
			.takeIf { !it.isNegative && !it.isZero }
			?: return

		delay(delay.toKotlinDuration())
	}

	private fun Message.isSuccessfulDisboardBump(): Boolean {
		val embeds = embeds.map { embed ->
			DisboardBumpEmbed(
				title = embed.title,
				description = embed.description,
			)
		}

		return DisboardBumpDetector.isSuccessfulBump(
			disboardBotId = DiscordSettings.disboardBotId,
			authorId = author?.id,
			authorIsBot = author?.isBot,
			applicationId = applicationId,
			embeds = embeds,
		)
	}

	private suspend fun thankBumper(
		message: Message,
		guildId: Snowflake,
	) {
		val bumperMention = message.interaction?.user?.mention ?: return
		val locale = GuildLocaleResolver.resolve(message.kord.getGuildOrNull(guildId))

		MessageChannelBehavior(message.channelId, message.kord).createMessage(
			Emojis.check.prefix(
				Translations.Disboard.BumpReminder.thanks
					.withNamedPlaceholders("user" to bumperMention)
					.withLocale(locale)
					.translate(),
			),
		)
	}

	private companion object {
		val BUMP_COOLDOWN: Duration = Duration.ofHours(2)
		val RETRY_AFTER: Duration = Duration.ofMinutes(30)
	}
}
