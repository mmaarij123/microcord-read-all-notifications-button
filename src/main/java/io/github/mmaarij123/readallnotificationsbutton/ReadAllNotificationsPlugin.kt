// SPDX-License-Identifier: MIT
package io.github.mmaarij123.readallnotificationsbutton

import android.app.Application
import android.app.Service
import android.content.Intent
import android.os.IBinder
import dev.mantikafasi.manticord.plugins.api.ManticordPlugin
import dev.mantikafasi.manticord.plugins.api.PluginScope
import rx.Completable

class ReadAllNotificationsPlugin : ManticordPlugin {
    private var shortcut: ServerListShortcut? = null

    override fun start(scope: PluginScope): Completable = Completable.fromAction {
        val application = scope.applicationContext.applicationContext as Application
        val resources = checkNotNull(scope.resources.context).resources
        shortcut = ServerListShortcut(application, resources).also { button ->
            scope.runOnMainThread(button::start)
        }
    }

    override fun stop(): Completable = Completable.fromAction {
        shortcut?.stop()
        shortcut = null
    }
}

class ReadAllNotificationsDiscoveryService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
