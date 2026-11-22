// SPDX-License-Identifier: MIT
package io.github.mmaarij123.readallnotificationsbutton

import com.discord.api.channel.Channel
import com.discord.api.channel.isThread
import com.discord.api.notification.ReadStateTypes
import com.discord.restapi.RestAPIParams.BulkAck
import com.discord.stores.StoreStream
import com.discord.utilities.permissions.PermissionUtils
import com.discord.utilities.rest.RestAPI
import java.util.concurrent.TimeUnit
import rx.Completable
import rx.Observable
import rx.schedulers.Schedulers

private const val ACK_BATCH_SIZE = 100

internal fun markServerNotificationsRead(): Observable<Int> = Observable.defer {
    val accountId = currentAccountId()
    check(accountId > 0)
    val api = checkNotNull(RestAPI.apiOrNull)

    StoreStream.getReadStates().getUnreadChannelIds().take(1).flatMap { unreadChannelIds ->
        val batches = collectReadStates(unreadChannelIds).chunked(ACK_BATCH_SIZE)
        Observable.from(batches.withIndex()).concatMap { (index, batch) ->
            val request = Completable.defer {
                check(currentAccountId() == accountId && RestAPI.apiOrNull === api)
                Completable.fromObservable(api.bulkAck(BulkAck(batch)))
            }.subscribeOn(Schedulers.io())

            if (index == 0) request.toObservable<Unit>() else {
                Observable.timer(1, TimeUnit.SECONDS).flatMap { request.toObservable<Unit>() }
            }
        }.toCompletable().andThen(Observable.just(batches.sumOf { it.size }))
    }
}.subscribeOn(Schedulers.io())

private fun currentAccountId(): Long {
    val authentication = StoreStream.getAuthentication()
    if (!authentication.isAuthed() || authentication.isSwitchingAccount()) return 0
    return StoreStream.getUsers().getMe().id
}

private fun collectReadStates(unreadChannelIds: Set<Long>): List<BulkAck.ReadState> {
    val guilds = StoreStream.getGuilds()
    val channelStore = StoreStream.getChannels()
    val guildIds = guilds.getGuilds().keys
    val joinedAt = guilds.getGuildsJoinedAt()
    val joinedThreads = StoreStream.getThreadsActiveJoined().getAllActiveJoinedThreadsById()
    val permissions = StoreStream.getPermissions().getPermissionsByChannel()
    val latestIds = StoreStream.getMessagesMostRecent().getMostRecentIds()
    val ackStore = StoreStream.getMessageAck()
    val acks = ackStore.getAll()

    val serverChannels = guildIds.asSequence().flatMap { guildId ->
        channelStore.getChannelsForGuild(guildId).values.asSequence().filter { it.type in serverChannelTypes }
    }
    val threads = joinedThreads.values.asSequence().map { it.channel }.filter { it.guildId in guildIds }
    return (serverChannels + threads).distinctBy(Channel::id).mapNotNull { channel ->
        if (!PermissionUtils.hasAccess(channel, permissions)) return@mapNotNull null
        val latestId = maxOf(latestIds[channel.id] ?: 0L, channel.lastMessageId)
        if (latestId <= 0) return@mapNotNull null
        val acknowledgedId = acks[channel.id]?.messageId
        val isUnread = when {
            acknowledgedId != null -> latestId > acknowledgedId
            channel.isThread() -> channel.id in unreadChannelIds
            else -> latestId > ackStore.getAckMessageId(channel, joinedAt[channel.guildId])
        }
        if (isUnread) BulkAck.ReadState(channel.id, latestId, ReadStateTypes.CHANNEL) else null
    }.toList()
}

private val serverChannelTypes = setOf(
    Channel.GUILD_TEXT,
    Channel.GUILD_ANNOUNCEMENT,
    Channel.GUILD_VOICE,
    Channel.GUILD_STAGE_VOICE,
    Channel.GUILD_FORUM,
    Channel.GUILD_MEDIA,
)
