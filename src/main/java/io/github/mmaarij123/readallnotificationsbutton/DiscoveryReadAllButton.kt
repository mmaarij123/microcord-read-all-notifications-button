// SPDX-License-Identifier: MIT
package io.github.mmaarij123.readallnotificationsbutton

import android.graphics.drawable.PictureDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.discord.widgets.guilds.list.GuildListViewHolder
import kotlin.math.roundToInt

internal class DiscoveryReadAllButton(
    private val list: RecyclerView,
    private val icon: SVG,
    initiallyEnabled: Boolean,
    private val onClick: () -> Unit,
) : RecyclerView.OnChildAttachStateChangeListener {
    private val rows = mutableMapOf<View, InstalledRow>()

    var enabled = initiallyEnabled
        set(value) {
            field = value
            rows.values.forEach { it.button.updateEnabled(value) }
        }

    init {
        list.addOnChildAttachStateChangeListener(this)
        for (index in 0 until list.childCount) onChildViewAttachedToWindow(list.getChildAt(index))
    }

    override fun onChildViewAttachedToWindow(view: View) {
        if (view in rows || list.getChildViewHolder(view) !is GuildListViewHolder.DiscoveryViewHolder) return
        val row = view as? FrameLayout ?: return
        val params = row.layoutParams as? MarginLayoutParams ?: return
        val nativeHeight = params.height
        if (nativeHeight <= 0) return

        val density = row.resources.displayMetrics.density
        val gap = params.bottomMargin.coerceAtLeast(0)
        val buttonHeight = maxOf(nativeHeight, (48 * density).roundToInt())
        val iconSize = (24 * density).roundToInt()
        val button = ImageButton(row.context).apply {
            contentDescription = "Mark all server notifications as read"
            TooltipCompat.setTooltipText(this, "Read All")
            scaleType = ImageView.ScaleType.CENTER
            setPadding(0, gap, 0, 0)
            val ripple = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)
            setBackgroundResource(ripple.resourceId)
            setImageDrawable(PictureDrawable(icon.renderToPicture(iconSize, iconSize)))
            updateEnabled(enabled)
            setOnClickListener { onClick() }
        }

        // Keep the native Discovery content at its original height and extend only its container.
        // This keeps RecyclerView positions and the host's trailing navigation spacer intact.
        val discoveryContent = FrameLayout(row.context)
        val children = List(row.childCount, row::getChildAt)
        children.forEach { child ->
            row.removeView(child)
            discoveryContent.addView(child)
        }
        row.addView(discoveryContent, FrameLayout.LayoutParams(MATCH_PARENT, nativeHeight))
        row.addView(button, FrameLayout.LayoutParams(MATCH_PARENT, gap + buttonHeight, Gravity.TOP).apply {
            topMargin = nativeHeight
        })
        row.layoutParams = params.apply { height = nativeHeight + gap + buttonHeight }
        rows[row] = InstalledRow(row, discoveryContent, children, button, nativeHeight)
    }

    override fun onChildViewDetachedFromWindow(view: View) {
        rows.remove(view)?.restore()
    }

    fun remove() {
        list.removeOnChildAttachStateChangeListener(this)
        rows.values.forEach(InstalledRow::restore)
        rows.clear()
    }

    private fun ImageButton.updateEnabled(enabled: Boolean) {
        isEnabled = enabled
        alpha = if (enabled) 1f else 0.45f
    }

    private class InstalledRow(
        private val row: FrameLayout,
        private val discoveryContent: FrameLayout,
        private val children: List<View>,
        val button: ImageButton,
        private val nativeHeight: Int,
    ) {
        fun restore() {
            button.setOnClickListener(null)
            row.removeView(button)
            row.removeView(discoveryContent)
            children.forEach { child ->
                discoveryContent.removeView(child)
                row.addView(child)
            }
            row.layoutParams = row.layoutParams.apply { height = nativeHeight }
        }
    }
}
