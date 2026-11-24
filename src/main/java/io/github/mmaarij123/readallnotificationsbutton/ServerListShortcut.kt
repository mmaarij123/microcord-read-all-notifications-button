// SPDX-License-Identifier: MIT
package io.github.mmaarij123.readallnotificationsbutton

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Picture
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.discord.R as HostR
import com.discord.app.AppActivity
import com.discord.widgets.guilds.list.WidgetGuildsList
import rx.Subscription

internal class ServerListShortcut(
    private val application: Application,
    private val icon: Picture,
) : Application.ActivityLifecycleCallbacks {
    private val main = Handler(Looper.getMainLooper())
    private val activities = mutableSetOf<AppActivity>()
    private val shortcuts = mutableMapOf<WidgetGuildsList, DiscoveryReadAllButton>()
    private var pending: Subscription? = null
    private var registered = false
    @Volatile private var stopped = false

    private val fragments = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(
            manager: FragmentManager,
            fragment: Fragment,
            view: View,
            savedState: Bundle?,
        ) {
            if (fragment is WidgetGuildsList) attach(fragment, view)
        }

        override fun onFragmentViewDestroyed(manager: FragmentManager, fragment: Fragment) {
            if (fragment is WidgetGuildsList) shortcuts.remove(fragment)?.remove()
        }
    }

    fun start(context: Context) {
        if (stopped || registered) return
        application.registerActivityLifecycleCallbacks(this)
        registered = true
        val activity = context as? AppActivity ?: return
        observe(activity)
    }

    fun stop() {
        stopped = true
        main.post {
            if (registered) application.unregisterActivityLifecycleCallbacks(this)
            registered = false
            activities.forEach { it.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragments) }
            activities.clear()
            pending?.unsubscribe()
            pending = null
            shortcuts.values.forEach(DiscoveryReadAllButton::remove)
            shortcuts.clear()
        }
    }

    private fun observe(activity: AppActivity) {
        if (stopped || !activities.add(activity)) return
        val manager = activity.supportFragmentManager
        manager.registerFragmentLifecycleCallbacks(fragments, true)
        attachExisting(manager)
    }

    private fun attachExisting(manager: FragmentManager) {
        for (fragment in manager.fragments) {
            if (!fragment.isAdded) continue
            if (fragment is WidgetGuildsList) fragment.view?.let { attach(fragment, it) }
            attachExisting(fragment.childFragmentManager)
        }
    }

    private fun attach(fragment: WidgetGuildsList, view: View) {
        if (stopped || shortcuts.containsKey(fragment)) return
        val list = view.findViewById<RecyclerView>(HostR.id.guild_list) ?: return
        shortcuts[fragment] = DiscoveryReadAllButton(list, icon, pending == null, ::readAll)
    }

    private fun readAll() {
        if (stopped || pending != null) return
        shortcuts.values.forEach { it.enabled = false }
        pending = markServerNotificationsRead().subscribe({ count ->
            finish(if (count == 0) "No unread server notifications." else "Server notifications marked as read.")
        }, { error ->
            Log.w("ReadAllNotifications", "Bulk acknowledgement failed (${error.javaClass.simpleName})")
            finish("Couldn’t finish marking server notifications as read. Tap Read All to retry.")
        })
    }

    private fun finish(message: String) {
        main.post {
            if (stopped) return@post
            pending = null
            shortcuts.values.forEach { it.enabled = true }
            Toast.makeText(application, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityCreated(activity: Activity, savedState: Bundle?) {
        if (activity is AppActivity) observe(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity is AppActivity) observe(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is AppActivity && activities.remove(activity)) {
            activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragments)
        }
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
