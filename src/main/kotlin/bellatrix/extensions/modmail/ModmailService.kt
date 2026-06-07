package bellatrix.extensions.modmail

import bellatrix.common.discord.Channels
import bellatrix.common.discord.Colors
import bellatrix.common.discord.CustomEmoji
import bellatrix.common.discord.Emojis
import bellatrix.common.discord.Roles
import bellatrix.database.models.ModmailMessageSender
import bellatrix.database.models.ModmailThread
import bellatrix.database.repositories.ModmailThreadRepository
import bellatrix.i18n.GuildLocaleResolver
import bellatrix.i18n.SupportedLocales
import bellatrix.i18n.Translations
import bellatrix.i18n.UserLocaleResolver
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Attachment
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.i18n.Key
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.get
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.util.Locale

class ModmailService(
	private val repository: ModmailThreadRepository = ModmailThreadRepository,
	private val httpClient: HttpClient = HttpClient(),
) {
	private val creationMutex = Mutex()
	private val assignmentMutex = Mutex()

	fun close() =
		httpClient.close()

	suspend fun handleUserDm(message: Message) {
		val user = message.author?.takeUnless(User::isBot) ?: return
		val attachments = message.attachments.download()
		val modmailThread = getOrCreateThread(user)
		val includeMention = modmailThread.lastSender != ModmailMessageSender.User

		repository.touch(modmailThread.threadId, Instant.now(), ModmailMessageSender.User)
		forwardToThread(message, user, modmailThread, attachments, includeMention)
	}

	suspend fun handleThreadMessage(message: Message) {
		val staff = message.author?.takeUnless(User::isBot) ?: return
		val modmailThread = repository.findOpenByThreadId(message.channelId) ?: return
		val attachments = message.attachments.download()
		val includeMention = modmailThread.lastSender != ModmailMessageSender.Staff

		assignIfNeeded(modmailThread, staff)
		repository.touch(modmailThread.threadId, Instant.now(), ModmailMessageSender.Staff)
		forwardToUser(message, staff, modmailThread, attachments, includeMention)
	}

	suspend fun closeFromCommand(
		kord: Kord,
		threadId: Snowflake,
		notifyUser: Boolean = true,
	): Boolean {
		val modmailThread = repository.close(threadId) ?: return false

		closeDiscordThread(kord, modmailThread, notifyUser)
		return true
	}

	suspend fun closeInactive(kord: Kord) {
		val cutoff = Instant.now().minus(AUTO_CLOSE_AFTER)

		repository.findInactiveOpen(cutoff).forEach { modmailThread ->
			repository.close(modmailThread.threadId)?.let {
				closeDiscordThread(kord, it, notifyUser = true, automatic = true)
			}
		}
	}

	private suspend fun getOrCreateThread(user: User): ModmailThread =
		creationMutex.withLock {
			repository.findOpenByUserId(user.id) ?: createThread(user)
		}

	private suspend fun createThread(user: User): ModmailThread {
		val channelId = requireNotNull(Channels.modmail) {
			"Missing required modmail channel resource: channels.modmail"
		}

		val parent = requireNotNull(user.kord.getChannelOf<TextChannel>(channelId)) {
			"Configured modmail channel was not found: $channelId"
		}

		val locale = GuildLocaleResolver.resolve(parent.getGuildOrNull())
		val parentMessage = parent.createMessage(openParentMessageContent(user, locale))
		val thread = parent.startPublicThreadWithMessage(
			messageId = parentMessage.id,
			name = threadName(user),
		) {
			autoArchiveDuration = ArchiveDuration.Day
		}

		val now = Instant.now()
		val modmailThread = repository.create(
			userId = user.id,
			threadId = thread.id,
			parentMessageId = parentMessage.id,
			now = now,
		)

		thread.createMessage {
			content = withEmoji(
				emoji = Emojis.info,
				message = Translations.Modmail.Thread.opened
					.withNamedPlaceholders(
						"role" to Roles.moderatorMention(),
						"user" to user.mention,
					)
					.withLocale(locale)
					.translate(),
			)
		}

		sendOpenedDm(user)

		return modmailThread
	}

	private suspend fun forwardToThread(
		message: Message,
		user: User,
		modmailThread: ModmailThread,
		attachments: List<AttachmentPayload>,
		includeMention: Boolean,
	) {
		val thread = user.kord.getChannelOf<TextChannelThread>(modmailThread.threadId) ?: return

		thread.createMessage {
			applyForwardedMessage(user.mention, message.content, attachments, includeMention)
		}
	}

	private suspend fun forwardToUser(
		message: Message,
		staff: User,
		modmailThread: ModmailThread,
		attachments: List<AttachmentPayload>,
		includeMention: Boolean,
	) {
		val dm = UserBehavior(modmailThread.userId, staff.kord).getDmChannelOrNull() ?: return

		dm.createMessage {
			applyForwardedMessage(staff.mention, message.content, attachments, includeMention)
		}
	}

	private suspend fun assignIfNeeded(
		modmailThread: ModmailThread,
		staff: User,
	) {
		if (modmailThread.assignedStaffId != null) return

		val assigned = assignmentMutex.withLock {
			repository.assignIfUnassigned(modmailThread.threadId, staff.id)
		}

		if (assigned) {
			val thread = staff.kord.getChannelOf<TextChannelThread>(modmailThread.threadId) ?: return
			val locale = GuildLocaleResolver.resolve(thread.getGuildOrNull())

			thread.createMessage(
				withEmoji(
					emoji = Emojis.check,
					message = Translations.Modmail.Thread.assigned
						.withNamedPlaceholders("staff" to staff.mention)
						.withLocale(locale)
						.translate(),
				),
			)
		}
	}

	private suspend fun closeDiscordThread(
		kord: Kord,
		modmailThread: ModmailThread,
		notifyUser: Boolean,
		automatic: Boolean = false,
	) {
		val thread = kord.getChannelOf<TextChannelThread>(modmailThread.threadId) ?: return
		val locale = GuildLocaleResolver.resolve(thread.getGuildOrNull())
		val message = if (automatic) {
			Translations.Modmail.Thread.closedAutomatic
		} else {
			Translations.Modmail.Thread.closed
		}.withLocale(locale).translate()

		thread.createMessage(
			withEmoji(
				emoji = if (automatic) Emojis.warning else Emojis.check,
				message = message,
			),
		)
		markParentMessageClosed(kord, modmailThread, locale)

		if (notifyUser) {
			val user = UserBehavior(modmailThread.userId, kord)
			val locale = UserLocaleResolver.resolve(user) ?: SupportedLocales.default

			user.getDmChannelOrNull()?.createMessage {
				embed {
					description = withEmoji(
						emoji = Emojis.lock,
						message = Translations.Modmail.User.closed
							.withLocale(locale)
							.translate(),
					)
					color = Colors.success
				}
			}
		}

		thread.edit {
			archived = true
			locked = true
		}
	}

	private suspend fun sendOpenedDm(user: User) {
		val locale = UserLocaleResolver.resolve(user) ?: SupportedLocales.default
		val dm = user.getDmChannelOrNull() ?: return

		dm.createMessage {
			embed {
				description = Emojis.hashtag
					?.let {
						"$it ${Translations.Modmail.User.opened.withLocale(locale).translate()}"
					}
					?: Translations.Modmail.User.opened.withLocale(locale).translate()
				color = Colors.success
			}
		}
	}

	private suspend fun Set<Attachment>.download(): List<AttachmentPayload> =
		map { attachment ->
			AttachmentPayload(
				name = attachment.filename,
				size = attachment.size.toLong(),
				bytes = httpClient.get(attachment.url).body(),
			)
		}

	private fun MessageBuilder.applyForwardedMessage(
		mention: String,
		message: String,
		attachments: List<AttachmentPayload>,
		includeMention: Boolean,
	) {
		val text = message.trim().takeIf(String::isNotBlank)

		content = buildString {
			if (includeMention) append(mention)

			text?.let {
				if (includeMention) append(", ")

				append(it)
			}
		}.takeIf(String::isNotBlank)

		attachments.forEach { attachment ->
			attachment.applyTo(this)
		}
	}

	private fun threadName(user: User): String =
		"modmail-${user.username}-${user.id}"
			.lowercase()
			.replace(THREAD_NAME_DISALLOWED, "-")
			.take(MAX_THREAD_NAME_LENGTH)
			.trim('-')
			.ifBlank { "modmail-${user.id}" }

	private fun openParentMessageContent(
		user: User,
		locale: Locale,
	): String =
		withEmoji(
			emoji = Emojis.hashtag,
			message = parentMessageContent(
				userMention = user.mention,
				userId = user.id,
				key = Translations.Modmail.Thread.parentOpen,
				locale = locale,
			),
		)

	private fun closedParentMessageContent(
		userId: Snowflake,
		locale: Locale,
	): String =
		"~~${
			withEmoji(
				emoji = Emojis.check,
				message = parentMessageContent(
					userMention = "<@$userId>",
					userId = userId,
					key = Translations.Modmail.Thread.parentClosed,
					locale = locale,
				),
			)
		}~~"

	private fun parentMessageContent(
		userMention: String,
		userId: Snowflake,
		key: Key,
		locale: Locale,
	): String =
		key
			.withNamedPlaceholders(
				"user" to userMention,
				"userId" to userId.toString(),
			)
			.withLocale(locale)
			.translate()

	private fun withEmoji(
		emoji: CustomEmoji?,
		message: String,
	): String =
		emoji
			?.let { "$it $message" }
			?: message

	private fun Roles.moderatorMention(): String =
		moderator
			?.let { "<@&$it>" }
			?: "@here"

	private suspend fun markParentMessageClosed(
		kord: Kord,
		modmailThread: ModmailThread,
		locale: Locale,
	) {
		val parentMessageId = modmailThread.parentMessageId ?: return
		val channelId = Channels.modmail ?: return

		MessageBehavior(channelId, parentMessageId, kord).edit {
			content = closedParentMessageContent(modmailThread.userId, locale)
		}
	}

	private class AttachmentPayload(
		val name: String,
		val size: Long,
		val bytes: ByteArray,
	) {
		fun applyTo(builder: MessageBuilder) {
			builder.addFile(
				name = name,
				contentProvider = ChannelProvider(size) {
					ByteReadChannel(bytes)
				},
			)
		}
	}

	private companion object {
		val AUTO_CLOSE_AFTER: Duration = Duration.ofHours(24)
		const val MAX_THREAD_NAME_LENGTH = 100
		val THREAD_NAME_DISALLOWED = Regex("[^a-z0-9_-]+")
	}
}
