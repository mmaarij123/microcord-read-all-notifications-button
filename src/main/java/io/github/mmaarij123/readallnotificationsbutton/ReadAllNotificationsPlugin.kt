// SPDX-License-Identifier: MIT
package io.github.mmaarij123.readallnotificationsbutton

import android.app.Application
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.caverock.androidsvg.SVG
import dev.mantikafasi.manticord.plugins.api.ManticordPlugin
import dev.mantikafasi.manticord.plugins.api.PluginScope
import rx.Completable

class ReadAllNotificationsPlugin : ManticordPlugin {
    private var shortcut: ServerListShortcut? = null

    override fun start(scope: PluginScope): Completable = Completable.fromAction {
        val application = scope.applicationContext.applicationContext as Application
        val assetPath = "read-all-notifications/read-all.svg"
        val imageStream = javaClass.getResourceAsStream("/assets/$assetPath")
            ?: checkNotNull(scope.resources.context).assets.open(assetPath)
        val icon = imageStream.use { SVG.getFromInputStream(it) }
        shortcut = ServerListShortcut(application, icon).also { shortcut ->
            scope.runOnMainThread(shortcut::start)
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
