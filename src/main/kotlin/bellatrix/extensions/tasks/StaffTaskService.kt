package bellatrix.extensions.tasks

import bellatrix.common.discord.Channels
import bellatrix.common.discord.Colors
import bellatrix.common.discord.Emojis
import bellatrix.common.discord.Roles
import bellatrix.common.extensions.prefix
import bellatrix.database.models.StaffTask
import bellatrix.database.models.StaffTaskStatus
import bellatrix.database.repositories.StaffTaskRepository
import bellatrix.i18n.GuildLocaleResolver
import bellatrix.i18n.Translations
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.GuildButtonInteraction
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.Locale
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class StaffTaskService(
	private val repository: StaffTaskRepository = StaffTaskRepository,
) {
	private val creationMutex = Mutex()
	private val locks = mutableMapOf<Long, Mutex>()
	private val locksMutex = Mutex()

	private val StaffTaskStatus.color
		get() = when (this) {
			StaffTaskStatus.Open -> Colors.info
			StaffTaskStatus.InProgress -> Colors.warning
			StaffTaskStatus.Resolved -> Colors.success
			StaffTaskStatus.Canceled -> Colors.danger
		}

	private val StaffTaskStatus.emoji
		get() = when (this) {
			StaffTaskStatus.Open -> Emojis.starBlue
			StaffTaskStatus.InProgress -> Emojis.starYellow
			StaffTaskStatus.Resolved -> Emojis.starGreen
			StaffTaskStatus.Canceled -> Emojis.starRed
		}

	suspend fun handleMessage(event: MessageCreateEvent) {
		val taskChannelId = Channels.tasks ?: return
		val message = event.message
		val member = event.member ?: return

		if (message.channelId != taskChannelId) return
		if (message.author?.isBot != false) return
		if (!member.hasModeratorRole()) return

		val content = message.content.trim().takeIf(String::isNotBlank) ?: return
		val guildId = event.guildId ?: return
		val locale = GuildLocaleResolver.resolve(event.getGuildOrNull())

		creationMutex.withLock {
			createTaskFromMessage(
				kord = message.kord,
				guildId = guildId,
				channelId = message.channelId,
				createdBy = member.id,
				content = content,
				locale = locale,
			)
		}
	}

	suspend fun handleButton(interaction: ButtonInteraction) {
		if (interaction !is GuildButtonInteraction) return

		interaction.deferPublicMessageUpdate()

		val action = TaskAction.fromComponentId(interaction.componentId)
		val taskId = interaction.componentId.substringAfterLast(':').toLongOrNull()

		if (action == null || taskId == null) return

		lockFor(taskId).withLock {
			handleAction(
				kord = interaction.kord,
				taskId = taskId,
				staffId = interaction.user.id,
				action = action,
				locale = GuildLocaleResolver.resolve(interaction.getGuildOrNull()),
			)
		}
	}

	private suspend fun createTaskFromMessage(
		kord: Kord,
		guildId: Snowflake,
		channelId: Snowflake,
		createdBy: Snowflake,
		content: String,
		locale: Locale,
	) {
		val task = repository.create(
			guildId = guildId,
			channelId = channelId,
			createdBy = createdBy,
			content = content,
			now = Instant.now(),
		)

		val taskMessage = createTaskMessage(kord, task, locale)
		val thread = createTaskThread(kord, task, taskMessage)

		val taskWithResources = repository.attachDiscordResources(
			taskId = task.taskId,
			messageId = taskMessage.id,
			threadId = thread.id,
			now = Instant.now(),
		) ?: return

		editTaskMessage(taskMessage, taskWithResources, kord, locale)
		sendThreadOpenedMessage(thread, locale)
	}

	private suspend fun createTaskMessage(
		kord: Kord,
		task: StaffTask,
		locale: Locale,
	): Message =
		requireNotNull(kord.getChannelOf<TextChannel>(task.channelId))
			.createMessage {
				applyTaskMessage(task, kord, locale)
			}

	private suspend fun createTaskThread(
		kord: Kord,
		task: StaffTask,
		taskMessage: Message,
	): TextChannelThread =
		requireNotNull(kord.getChannelOf<TextChannel>(task.channelId))
			.startPublicThreadWithMessage(taskMessage.id, task.threadName()) {
				autoArchiveDuration = ArchiveDuration.Week
			}

	private suspend fun sendThreadOpenedMessage(
		thread: TextChannelThread,
		locale: Locale,
	) {
		thread.createMessage(
			Translations.StaffTask.Thread.opened
				.withLocale(locale)
				.translate(),
		)
	}

	private suspend fun handleAction(
		kord: Kord,
		taskId: Long,
		staffId: Snowflake,
		action: TaskAction,
		locale: Locale,
	) {
		val now = Instant.now()
		val task = when (action) {
			TaskAction.Claim -> repository.assignIfAvailable(taskId, staffId, now)
			TaskAction.Resolve -> repository.updateStatus(taskId, StaffTaskStatus.Resolved, staffId, now)
			TaskAction.Cancel -> repository.updateStatus(taskId, StaffTaskStatus.Canceled, staffId, now)
			TaskAction.Reopen -> repository.updateStatus(taskId, StaffTaskStatus.Open, staffId, now)
		} ?: return

		editTaskMessage(kord, task, locale)
	}

	private suspend fun editTaskMessage(
		kord: Kord,
		task: StaffTask,
		locale: Locale,
	) {
		val messageId = task.messageId ?: return

		runCatching {
			MessageBehavior(task.channelId, messageId, kord).edit {
				applyTaskMessage(task, kord, locale)
			}
		}
	}

	private suspend fun editTaskMessage(
		message: Message,
		task: StaffTask,
		kord: Kord,
		locale: Locale,
	) {
		message.edit {
			applyTaskMessage(task, kord, locale)
		}
	}

	private suspend fun lockFor(taskId: Long): Mutex =
		locksMutex.withLock {
			locks.getOrPut(taskId) { Mutex() }
		}

	private fun MessageBuilder.applyTaskMessage(
		task: StaffTask,
		kord: Kord,
		locale: Locale,
	) {
		content = null
		embeds = mutableListOf()
		components = mutableListOf()

		embed {
			applyTaskEmbed(task, kord, locale)
		}

		actionRow {
			applyTaskButtons(task, locale)
		}
	}

	private fun EmbedBuilder.applyTaskEmbed(
		task: StaffTask,
		kord: Kord,
		locale: Locale,
	) {
		title = Translations.StaffTask.title
			.withNamedPlaceholders("id" to task.taskId)
			.withLocale(locale)
			.translate()

		description = task.content.take(MAX_DESCRIPTION_LENGTH)
		color = task.status.color
		timestamp = kotlin.time.Instant.fromEpochMilliseconds(task.createdAt.toEpochMilli())

		field(Emojis.info.prefix(Translations.StaffTask.Field.status.withLocale(locale).translate()), inline = true) {
			task.status.label(locale)
		}

		field(
			Emojis.userCheck.prefix(Translations.StaffTask.Field.assignedTo.withLocale(locale).translate()),
			inline = true,
		) {
			task.assignedTo
				?.let { UserBehavior(it, kord).mention }
				?: Translations.StaffTask.none.withLocale(locale).translate()
		}

		field(Emojis.user.prefix(Translations.StaffTask.Field.createdBy.withLocale(locale).translate()), inline = true) {
			UserBehavior(task.createdBy, kord).mention
		}

		field(
			Emojis.calendar.prefix(Translations.StaffTask.Field.createdAt.withLocale(locale).translate()),
			inline = true,
		) {
			task.createdAt.discordTimestamp()
		}

		field(
			Emojis.calendar.prefix(Translations.StaffTask.Field.updatedAt.withLocale(locale).translate()),
			inline = true,
		) {
			task.updatedAt.discordTimestamp()
		}
	}

	private fun ActionRowBuilder.applyTaskButtons(
		task: StaffTask,
		locale: Locale,
	) {
		when (task.status) {
			StaffTaskStatus.Open, StaffTaskStatus.InProgress -> applyOpenTaskButtons(task, locale)
			StaffTaskStatus.Resolved, StaffTaskStatus.Canceled -> applyClosedTaskButtons(task, locale)
		}
	}

	private fun ActionRowBuilder.applyOpenTaskButtons(
		task: StaffTask,
		locale: Locale,
	) {
		interactionButton(ButtonStyle.Primary, TaskAction.Claim.customId(task.taskId)) {
			label = Translations.StaffTask.Button.claim.withLocale(locale).translate()
			emoji = Emojis.userCheck?.partial
			disabled = task.assignedTo != null || task.status != StaffTaskStatus.Open
		}

		interactionButton(ButtonStyle.Success, TaskAction.Resolve.customId(task.taskId)) {
			label = Translations.StaffTask.Button.resolve.withLocale(locale).translate()
			emoji = Emojis.check?.partial
		}

		interactionButton(ButtonStyle.Danger, TaskAction.Cancel.customId(task.taskId)) {
			label = Translations.StaffTask.Button.cancel.withLocale(locale).translate()
			emoji = Emojis.remove?.partial
		}
	}

	private fun ActionRowBuilder.applyClosedTaskButtons(
		task: StaffTask,
		locale: Locale,
	) {
		interactionButton(ButtonStyle.Secondary, TaskAction.Reopen.customId(task.taskId)) {
			label = Translations.StaffTask.Button.reopen.withLocale(locale).translate()
			emoji = Emojis.unlock?.partial
		}
	}

	private fun StaffTask.threadName(): String =
		"todo-$taskId-$content"
			.lowercase()
			.replace(THREAD_NAME_DISALLOWED, "-")
			.take(MAX_THREAD_NAME_LENGTH)
			.trim('-')
			.ifBlank { "todo-$taskId" }

	private fun StaffTaskStatus.label(locale: Locale): String =
		emoji.prefix(
			when (this) {
				StaffTaskStatus.Open -> Translations.StaffTask.Status.open
				StaffTaskStatus.InProgress -> Translations.StaffTask.Status.inProgress
				StaffTaskStatus.Resolved -> Translations.StaffTask.Status.resolved
				StaffTaskStatus.Canceled -> Translations.StaffTask.Status.canceled
			}.withLocale(locale).translate(),
		)

	private fun Instant.discordTimestamp(): String =
		"<t:$epochSecond:f>"

	private fun dev.kord.core.entity.Member.hasModeratorRole(): Boolean =
		Roles.moderator?.let { it in roleIds } == true

	private enum class TaskAction(
		val value: String,
	) {
		Claim("claim"),
		Resolve("resolve"),
		Cancel("cancel"),
		Reopen("reopen");

		fun customId(taskId: Long): String =
			"$CUSTOM_ID_PREFIX$value:$taskId"

		companion object {
			fun fromComponentId(componentId: String): TaskAction? {
				val action = componentId
					.removePrefix(CUSTOM_ID_PREFIX)
					.substringBefore(':')

				return entries.firstOrNull { it.value == action }
			}
		}
	}

	private companion object {
		const val CUSTOM_ID_PREFIX = "staff-task:"
		const val MAX_DESCRIPTION_LENGTH = 4000
		const val MAX_THREAD_NAME_LENGTH = 100
		val THREAD_NAME_DISALLOWED = Regex("[^a-z0-9_-]+")
	}
}
