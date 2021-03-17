package com.arasthel.sectionedlistadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.util.*

abstract class Section {

    internal var adapter: SectionedListAdapter? = null
    /** Used to update this section's items using [androidx.recyclerview.widget.DiffUtil] */
    val updateCallback = SectionUpdateCallback(this)

    /** Used to distinguish between Sections. [UUID] is used to avoid collisions. */
    open val identifier: String = UUID.randomUUID().toString()

    // region Section customization

    /**
     * If you only need a single item view type, you can provide a layout id so it can be inflated
     * using [createDefaultItemView].
     */
    var defaultItemLayoutId: Int? = null
    /** You can provide a header layout id and inflate it later using [createHeaderView] */
    var headerLayoutId: Int? = null
    /** You can provide a footer layout id and inflate it later using [createFooterView] */
    var footerLayoutId: Int? = null

    /**
     * Controls if this [Section] should be visible. Default value is *true*.
     *
     * **WARNING**: changing this variable will trigger adapter changes.
     */
    var isVisible = true
        set(newValue) {
            if (newValue == field) return
            val sectionPosition = getSectionFirstPosition(false) ?: return
            if (newValue) {
                field = newValue
                adapter?.notifyItemRangeInserted(sectionPosition, getTotalCount())
                adapter?.restoreScrollPositionAfterInsertion()
            } else {
                adapter?.notifyItemRangeRemoved(sectionPosition, getTotalCount())
                field = newValue
            }
        }

    /**
     * Controls if this [Section] has a header. Default value is *false*.
     *
     * **WARNING**: changing this variable will trigger adapter changes.
     */
    var hasHeader = false
        set(newValue) {
            if (newValue == field) return
            if (newValue) {
                adapter?.notifySectionHeaderInserted(this)
                field = newValue
            } else {
                field = newValue
                adapter?.notifySectionHeaderRemoved(this)
            }
        }

    /**
     * Controls if this [Section] has a footer. Default value is *false*.
     *
     * **WARNING**: changing this variable will trigger adapter changes.
     */
    var hasFooter = false
        set(newValue) {
            if (newValue == field) return
            if (newValue) {
                adapter?.notifySectionFooterInserted(this)
                field = newValue
            } else {
                field = newValue
                adapter?.notifySectionFooterRemoved(this)
            }
        }

    /**
     * Controls if this [Section] should display a header item event if it's empty. Default value is *false*.
     *
     * **WARNING**: changing this variable may trigger adapter changes.
     */
    var isHeaderVisibleWhenEmpty = false
        set(newValue) {
            if (newValue == field) return
            if (newValue) {
                if (isEmpty && !isHeaderVisible()) adapter?.notifySectionHeaderInserted(this)
                field = newValue
            } else {
                val shouldRemove = isEmpty && isHeaderVisible()
                field = newValue
                if (shouldRemove) adapter?.notifySectionHeaderRemoved(this)
            }
        }

    /**
     * Controls if this [Section] should display a footer item event if it's empty. Default value is *false*.
     *
     * **WARNING**: changing this variable may trigger adapter changes.
     */
    var isFooterVisibleWhenEmpty = false
        set(newValue) {
            if (newValue == field) return
            if (newValue) {
                if (isEmpty && !isFooterVisible()) adapter?.notifySectionFooterInserted(this)
                field = newValue
            } else {
                val shouldRemove = isEmpty && isFooterVisible()
                field = newValue
                if (shouldRemove) adapter?.notifySectionFooterRemoved(this)
            }
        }

    // endregion

    // region Abstract methods and optional implementations

    /**
     * Must be implemented.
     * @return The number of items in this section, it **MUST NOT INCLUDE** header, footer, etc.
     *  **Only actual items**.
     */
    abstract fun getSectionItemCount(): Int

    /**
     * Must be implemented.
     * @return A [RecyclerView.ViewHolder] wrapping an inflater [View].
     */
    abstract fun createItemViewHolder(parent: ViewGroup, itemViewType: Int): RecyclerView.ViewHolder

    /**
     * Must be implemented.
     * You should configure and recycle your [RecyclerView.ViewHolder] items here.
     */
    abstract fun bindItemViewHolder(index: Int, viewHolder: RecyclerView.ViewHolder, payloads: List<Any>?)

    /**
     * Should only be implemented if [hasHeader] is true and no [headerLayoutId] is provided.
     * Otherwise, a View will be automatically inflated using [headerLayoutId].
     * @return A valid [View] to use as header.
     */
    open fun createHeaderView(parent: ViewGroup): View? {
        val layoutId = headerLayoutId ?: return null
        return LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
    }

    /**
     * Should only be implemented if [hasFooter] is true and no [footerLayoutId] is provided.
     * Otherwise, a View will be automatically inflated using [footerLayoutId].
     * @return A valid [View] to use as footer.
     */
    open fun createFooterView(parent: ViewGroup): View? {
        val layoutId = footerLayoutId ?: return null
        return LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
    }

    /**
     * Should only be implemented if no [defaultItemLayoutId] is provided.
     * Otherwise, a View will be automatically inflated using [defaultItemLayoutId].
     * @return A valid [View] to use for this section's items.
     */
    open fun createDefaultItemView(parent: ViewGroup): View? {
        val layoutId = defaultItemLayoutId ?: return null
        return LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
    }

    /**
     * Must be implemented if [hasHeader] is true.
     * @return A valid [RecyclerView.ViewHolder] for header items.
     */
    open fun createHeaderViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        throw UnsupportedOperationException("You need to implement createHeaderView method if you are using headers")
    }

    /** Should be used to configure the header item's ViewHolder */
    open fun bindHeaderViewHolder(viewHolder: RecyclerView.ViewHolder) {}

    /**
     * Must be implemented if [hasFooter] is true.
     * @return A valid [RecyclerView.ViewHolder] for footer items.
     */
    open fun createFooterViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        throw UnsupportedOperationException("You need to implement createFooterView method if you are using footers")
    }

    /** Should be used to configure the footer item's ViewHolder */
    open fun bindFooterViewHolder(viewHolder: RecyclerView.ViewHolder) {}

    /**
     * Should be overridden if several item view types will be used.
     * Must be in range: 0..[SectionedListAdapter.ITEM_VIEW_TYPES_COUNT]
     */
    open fun getItemViewType(position: Int): Int {
        return 0
    }

    // endregion

    // region Utils

    /** @return *true* if there are no items, *false* otherwise */
    val isEmpty: Boolean get() = getSectionItemCount() == 0
    /** @return *false* if there are no items, *true* otherwise */
    val isNotEmpty: Boolean get() = !isEmpty

    /**
     * @param fromFirstItem If the returned position should be the first actual item or the
     *  header's position
     * @return The position in the parent [adapter] where this section begins, null if [isVisible]
     *  is false or the section is not in the [adapter].
     */
    fun getSectionFirstPosition(fromFirstItem: Boolean): Int? {
        val adapter = this.adapter ?: return null
        var total = 0
        for (section in adapter._sections) {
            if (fromFirstItem && section.isHeaderVisible()) total += 1
            if (this == section) return total
            total += section.getTotalCount()
        }
        return null
    }

    /**
     * Used to notify the parent [adapter] when items are inserted. Positions will be offsetted.
     */
    fun notifyItemsInserted(start: Int, count: Int) {
        if (!isVisible) return
        val startPosition = getSectionFirstPosition(true) ?: return
        adapter?.notifyItemRangeInserted(startPosition + start, count)
        adapter?.restoreScrollPositionAfterInsertion()
    }

    /**
     * Used to notify the parent [adapter] when items are removed. Positions will be offsetted.
     */
    fun notifyItemsRemoved(start: Int, count: Int) {
        if (!isVisible) return
        val startPosition = getSectionFirstPosition(true) ?: return
        val willBeEmptyAfterRemoval = (getSectionItemCount() - count) == 0
        if (willBeEmptyAfterRemoval && isHeaderVisible() && !isHeaderVisibleWhenEmpty) {
            adapter?.notifySectionHeaderRemoved(this)
        }
        if (willBeEmptyAfterRemoval && isFooterVisible() && !isFooterVisibleWhenEmpty) {
            adapter?.notifySectionHeaderRemoved(this)
        }
        adapter?.notifyItemRangeRemoved(startPosition + start, count)
    }

    /**
     * Used to notify the parent [adapter] when items have changed. Positions will be offsetted.
     * @param payload A partial payload to apply diffing to existing elements.
     */
    fun notifyItemsChanged(start: Int, count: Int, payload: Any? = null) {
        if (!isVisible) return
        val startPosition = getSectionFirstPosition(true) ?: return
        adapter?.notifyItemRangeChanged(startPosition + start, count, payload)
    }

    /**
     * Used to notify the parent [adapter] when items are moved. Positions will be offsetted.
     */
    fun notifyItemMoved(from: Int, to: Int) {
        if (!isVisible) return
        val startPosition = getSectionFirstPosition(true) ?: return
        adapter?.notifyItemMoved(startPosition + from, to)
    }

    /**
     * Used to notify the parent [adapter] when the header needs to be reloaded.
     * @param payload A partial payload to apply diffing to existing elements.
     */
    fun notifyHeaderChanged(payload: Any? = null) {
        if (!isHeaderVisible()) return
        val firstPosition = getSectionFirstPosition(false) ?: return
        adapter?.notifyItemChanged(firstPosition, payload)
    }

    /**
     * Used to notify the parent [adapter] when the footer needs to be reloaded.
     * @param payload A partial payload to apply diffing to existing elements.
     */
    fun notifyFooterChanged(payload: Any? = null) {
        if (!isFooterVisible()) return
        val firstPosition = getSectionFirstPosition(false) ?: return
        adapter?.notifyItemChanged(firstPosition + getTotalCount()-1, payload)
    }

    /**
     * @param position Position of the item inside this section.
     * @return *true* if the item at the provided [position] is a header item
     */
    fun isHeader(position: Int): Boolean {
        return position == 0 && isHeaderVisible()
    }

    /**
     * @param position Position of the item inside this section.
     * @return *true* if the item at the provided [position] is a footer item
     */
    fun isFooter(position: Int): Boolean {
        return position == getTotalCount()-1 && isFooterVisible()
    }

    /** @return *true* if header item should be visible */
    fun isHeaderVisible(): Boolean {
        return isVisible && hasHeader && (isHeaderVisibleWhenEmpty || isNotEmpty)
    }

    /** @return *true* if footer item should be visible */
    fun isFooterVisible(): Boolean {
        return isVisible && hasFooter && (isFooterVisibleWhenEmpty || isNotEmpty)
    }

    /** @return Total count of actual items and other 'decorators' such as headers, footers, etc. */
    fun getTotalCount(): Int {
        if (!isVisible) return 0

        val itemCount = getSectionItemCount()
        val headerItemCount = when {
            isHeaderVisible() -> 1
            else -> 0
        }
        val footerItemCount = when {
            isFooterVisible() -> 1
            else -> 0
        }
        return itemCount + headerItemCount + footerItemCount
    }

    // endregion

    // region Equality and HashCode

    override fun hashCode(): Int {
        return identifier.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Section -> identifier == other.identifier
            else -> false
        }
    }

    // endregion
}