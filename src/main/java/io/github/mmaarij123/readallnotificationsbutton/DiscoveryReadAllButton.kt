// SPDX-License-Identifier: MIT
package io.github.mmaarij123.readallnotificationsbutton

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.RecyclerView
import com.discord.widgets.guilds.list.GuildListViewHolder
import kotlin.math.roundToInt

internal class DiscoveryReadAllButton(
    private val list: RecyclerView,
    private val icon: Picture,
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

        val children = List(row.childCount, row::getChildAt)
        val discoveryIcon = children.filterIsInstance<ImageView>()
            .firstOrNull { it.javaClass == ImageView::class.java }?.drawable
        val metrics = row.resources.displayMetrics
        val fallbackSize = (24 * metrics.density).roundToInt()
        val iconWidth = discoveryIcon?.intrinsicWidth?.takeIf { it > 0 } ?: fallbackSize
        val iconHeight = discoveryIcon?.intrinsicHeight?.takeIf { it > 0 } ?: fallbackSize
        val bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888).apply {
            density = metrics.densityDpi
        }
        Canvas(bitmap).drawPicture(icon, Rect(0, 0, iconWidth, iconHeight))

        val gap = params.bottomMargin.coerceAtLeast(0)
        val button = ImageButton(row.context).apply {
            contentDescription = "Mark all server notifications as read"
            TooltipCompat.setTooltipText(this, "Read All")
            scaleType = ImageView.ScaleType.CENTER
            background = children.firstOrNull()?.background?.constantState
                ?.newDrawable(row.resources, context.theme)?.mutate()
            setPadding(0, 0, 0, 0)
            setImageDrawable(BitmapDrawable(row.resources, bitmap))
            updateEnabled(enabled)
            setOnClickListener { onClick() }
        }

        // Keep the native Discovery content at its original height and extend only its container.
        // This keeps RecyclerView positions and the host's trailing navigation spacer intact.
        val discoveryContent = FrameLayout(row.context)
        children.forEach { child ->
            row.removeView(child)
            discoveryContent.addView(child)
        }
        row.addView(discoveryContent, FrameLayout.LayoutParams(MATCH_PARENT, nativeHeight))
        val shortcutSlot = FrameLayout(row.context).apply {
            // Consume taps in the gutter so neither control gains the extra height.
            isClickable = true
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            addView(button, FrameLayout.LayoutParams(MATCH_PARENT, nativeHeight, Gravity.BOTTOM))
        }
        row.addView(shortcutSlot, FrameLayout.LayoutParams(MATCH_PARENT, gap + nativeHeight, Gravity.TOP).apply {
            topMargin = nativeHeight
        })
        row.layoutParams = params.apply { height = nativeHeight + gap + nativeHeight }
        rows[row] = InstalledRow(row, discoveryContent, children, shortcutSlot, button, nativeHeight)
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
        private val shortcutSlot: FrameLayout,
        val button: ImageButton,
        private val nativeHeight: Int,
    ) {
        fun restore() {
            button.setOnClickListener(null)
            row.removeView(shortcutSlot)
            row.removeView(discoveryContent)
            children.forEach { child ->
                discoveryContent.removeView(child)
                row.addView(child)
            }
            row.layoutParams = row.layoutParams.apply { height = nativeHeight }
        }
    }
}
