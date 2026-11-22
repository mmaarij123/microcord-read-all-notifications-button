// SPDX-License-Identifier: MIT
package io.github.mmaarij123.readallnotificationsbutton

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.discord.R as HostR
import com.discord.app.AppActivity
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.guilds.list.WidgetGuildsList
import kotlin.math.roundToInt
import rx.Subscription

internal class ServerListShortcut(
    private val application: Application,
    private val resources: Resources,
) : Application.ActivityLifecycleCallbacks {
    private val main = Handler(Looper.getMainLooper())
    private val activities = mutableSetOf<AppActivity>()
    private val buttons = mutableMapOf<WidgetGuildsList, Attachment>()
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
            if (fragment is WidgetGuildsList) buttons.remove(fragment)?.remove()
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
            buttons.values.forEach(Attachment::remove)
            buttons.clear()
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
        if (stopped || buttons.containsKey(fragment)) return
        val list = view.findViewById<View>(HostR.id.guild_list) ?: return
        val parent = list.parent as? RelativeLayout ?: return
        val original = list.layoutParams as? RelativeLayout.LayoutParams ?: return

        val button = Button(list.context).apply {
            id = View.generateViewId()
            text = resources.getText(R.string.read_all)
            contentDescription = resources.getText(R.string.read_all_description)
            isAllCaps = false
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            minWidth = 0
            minimumWidth = 0
            minHeight = (48 * resources.displayMetrics.density).roundToInt()
            setPadding(0, 0, 0, 0)
            val ripple = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)
            setBackgroundResource(ripple.resourceId)
            val textColor = ColorCompat.getThemedColorStateListDefault(context, HostR.attr.colorTextNormal)
            if (textColor != 0) setTextColor(textColor)
            isEnabled = pending == null
            setOnClickListener { if (isShown) readAll() }
        }

        val listBelowButton = RelativeLayout.LayoutParams(original).apply {
            addRule(RelativeLayout.BELOW, button.id)
        }
        parent.addView(button, 0, RelativeLayout.LayoutParams(original.width, WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
            addRule(RelativeLayout.ALIGN_START, list.id)
            addRule(RelativeLayout.ALIGN_END, list.id)
        })
        list.layoutParams = listBelowButton
        buttons[fragment] = Attachment(parent, button, list, original, listBelowButton)
    }

    private fun readAll() {
        if (stopped || pending != null) return
        buttons.values.forEach { it.button.isEnabled = false }
        pending = markServerNotificationsRead().subscribe({ count ->
            finish(if (count == 0) R.string.read_all_empty else R.string.read_all_done)
        }, { error ->
            Log.w("ReadAllNotifications", "Bulk acknowledgement failed (${error.javaClass.simpleName})")
            finish(R.string.read_all_failed)
        })
    }

    private fun finish(message: Int) {
        main.post {
            if (stopped) return@post
            pending = null
            buttons.values.forEach { it.button.isEnabled = true }
            Toast.makeText(application, resources.getText(message), Toast.LENGTH_SHORT).show()
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

    private class Attachment(
        val parent: RelativeLayout,
        val button: Button,
        val list: View,
        val original: RelativeLayout.LayoutParams,
        val installed: RelativeLayout.LayoutParams,
    ) {
        fun remove() {
            if (list.layoutParams === installed) list.layoutParams = original
            button.setOnClickListener(null)
            parent.removeView(button)
        }
    }
}
