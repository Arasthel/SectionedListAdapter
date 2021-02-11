package com.arasthel.sectionedlistadapter

import android.view.ViewGroup
import androidx.core.view.isEmpty
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference

class SectionedListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_FOOTER = 1
        const val VIEW_TYPE_EMPTY = 2
        const val VIEW_TYPE_LOADING = 3
        const val VIEW_TYPE_ERROR = 4
        const val VIEW_TYPE_ITEM = 5

        /** Each [Section] can allocate up to [VIEW_TYPES_PER_SECTION] view types, some of them are reserved */
        const val VIEW_TYPES_PER_SECTION = 255
        /** Each [Section] can have custom item view types between [VIEW_TYPE_ITEM] and [VIEW_TYPES_PER_SECTION]. */
        const val ITEM_VIEW_TYPES_COUNT = VIEW_TYPES_PER_SECTION - VIEW_TYPE_ITEM
    }

    private var lastSectionAddedCount = 0
    private var sectionViewTypeStarts = mutableMapOf<String, Int>()
    private var sectionMap = mutableMapOf<String, Section>()
    internal val _sections = mutableListOf<Section>()

    /** Read only property for [_sections] */
    val sections: List<Section> = _sections

    // region RecyclerView attach and detach

    /** We hold a reference to track scrolling and other events **/
    private var recyclerViewReference = WeakReference<RecyclerView>(null)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerViewReference = WeakReference(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerViewReference.clear()
    }

    // endregion

    // region Section operations

    /**
     * **WARNING**: This will trigger adapter changes.
     *
     * @param section [Section] to add
     * @param atIndex index where the [Section] will be inserted, if `null`, it will be appended at
     *  the end
     */
    fun addSection(section: Section, atIndex: Int? = null) {
        sectionViewTypeStarts[section.identifier] = generateViewTypeRangeForSection()
        sectionMap[section.identifier] = section
        section.adapter = this
        if (atIndex != null) {
            this._sections.add(atIndex, section)
        } else {
            this._sections.add(section)
        }
        notifyItemRangeInserted(section.getSectionFirstPosition(false)!!, section.getTotalCount())
        restoreScrollPositionAfterInsertion()
    }

    /**
     * **WARNING**: This will trigger adapter changes.
     *
     * @param sections [Section]s to add
     * @param atIndex index where the [Section]s will be inserted, if `null`, it will be appended at
     *  the end
     */
    fun addSections(sections: List<Section>, atIndex: Int? = null) {
        if (sections.isEmpty()) return

        sections.forEach {
            sectionViewTypeStarts[it.identifier] = generateViewTypeRangeForSection()
            sectionMap[it.identifier] = it
            it.adapter = this
        }
        if (atIndex != null) {
            this._sections.addAll(atIndex, sections)
        } else {
            this._sections.addAll(sections)
        }

        val startIndex = sections.first().getSectionFirstPosition(false)!!
        val totalItems = sections.sumBy { it.getTotalCount() }
        notifyItemRangeInserted(startIndex, totalItems)
        restoreScrollPositionAfterInsertion()
    }

    /**
     * **WARNING**: This will trigger adapter changes.
     *
     * @param section [Section] to remove
     */
    fun removeSection(section: Section): Boolean {
        val removedSection = removeSectionAt(_sections.indexOf(section))
        return removedSection != null
    }

    /**
     * **WARNING**: This will trigger adapter changes.
     *
     * @param index Index of the [Section] that will be removed
     */
    fun removeSectionAt(index: Int): Section? {
        if (!(0 until _sections.count()).contains(index)) return null
        val section = _sections[index]

        val startIndex = section.getSectionFirstPosition(false)!!
        val totalItems = section.getTotalCount()

        sectionMap.remove(section.identifier)
        sectionViewTypeStarts.remove(section.identifier)
        val removedSection = _sections.removeAt(index)

        notifyItemRangeRemoved(startIndex, totalItems)
        return removedSection
    }

    /**
     * **WARNING**: This will trigger adapter changes.
     *
     * @param currentPosition Current index of the [Section] to move
     * @param newPosition Index where the [Section] will be moved to
     */
    fun moveSectionAt(currentPosition: Int, newPosition: Int): Boolean {
        val validRange = 0 until _sections.count()
        if (!validRange.contains(currentPosition) || !validRange.contains(newPosition)) return false

        val section = _sections[currentPosition]
        val startIndex = section.getSectionFirstPosition(false)!!
        val totalItems = section.getTotalCount()

        _sections.removeAt(currentPosition)
        _sections.add(newPosition, section)

        val newStartIndex = section.getSectionFirstPosition(false)!!
        for (i in 0 until totalItems) {
            notifyItemMoved(startIndex + i, newStartIndex + i)
        }
        return true
    }

    /**
     * **WARNING**: This will trigger adapter changes.
     *
     * Removes all sections from the adapter.
     */
    fun removeAllSections() {
        val count = this.itemCount
        _sections.clear()
        sectionMap.clear()
        sectionViewTypeStarts.clear()
        lastSectionAddedCount = 0
        notifyItemRangeRemoved(0, count)
    }

    // endregion

    // region Adapter methods

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val section = getSectionForViewType(viewType)!!
        return when (val actualViewType = viewType % VIEW_TYPES_PER_SECTION) {
            VIEW_TYPE_HEADER -> section.createHeaderViewHolder(parent)
            VIEW_TYPE_FOOTER -> section.createFooterViewHolder(parent)
            else -> section.createItemViewHolder(parent, actualViewType - VIEW_TYPE_ITEM)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        internalBindViewHolder(holder, position, null)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        internalBindViewHolder(holder, position, payloads)
    }

    private fun internalBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>?) {
        val (section, offset) = getSectionAndOffset(position) ?: return
        val offsetWithoutHeader = if (section.isHeaderVisible()) offset-1 else offset
        when {
            section.isHeader(offset) -> section.bindHeaderViewHolder(holder)
            section.isFooter(offset) -> section.bindFooterViewHolder(holder)
            else -> section.bindItemViewHolder(offsetWithoutHeader, holder, payloads)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val (section: Section, index: Int) = getSectionAndOffset(position) ?: return VIEW_TYPE_ITEM
        val startIndex = sectionViewTypeStarts[section.identifier] ?: return VIEW_TYPE_ITEM
        return when {
            section.isHeader(index) -> startIndex + VIEW_TYPE_HEADER
            section.isFooter(index) -> startIndex + VIEW_TYPE_FOOTER
            else -> startIndex + VIEW_TYPE_ITEM + section.getItemViewType(position)
        }
    }

    override fun getItemCount(): Int {
        return _sections.sumOf { it.getTotalCount() }
    }

    // endregion

    private fun generateViewTypeRangeForSection(): Int {
        val startViewTypeIndex = lastSectionAddedCount * VIEW_TYPES_PER_SECTION
        lastSectionAddedCount++
        return startViewTypeIndex
    }

    private fun getSectionForViewType(viewType: Int): Section? {
        for (entry in sectionViewTypeStarts.entries) {
            if (viewType >= entry.value && viewType < entry.value + VIEW_TYPES_PER_SECTION) {
                return sectionMap[entry.key]
            }
        }
        return null
    }

    fun notifySectionHeaderInserted(changedSection: Section) {
        val position = changedSection.getSectionFirstPosition(false) ?: return
        notifyItemInserted(position)
        restoreScrollPositionAfterInsertion()
    }

    fun notifySectionHeaderRemoved(changedSection: Section) {
        val position = changedSection.getSectionFirstPosition(false) ?: return
        notifyItemRemoved(position)
    }

    fun notifySectionFooterInserted(changedSection: Section) {
        val position = changedSection.getSectionFirstPosition(false) ?: return
        val footerPosition = position + changedSection.getTotalCount()
        notifyItemInserted(footerPosition)
        restoreScrollPositionAfterInsertion()
    }

    fun notifySectionFooterRemoved(changedSection: Section) {
        val position = changedSection.getSectionFirstPosition(false) ?: return
        val footerPosition = position + changedSection.getTotalCount()
        notifyItemRemoved(footerPosition)
    }

    /**
     * Returns the [Section] and index inside that [Section] of an item.
     * @param position Position in this adapter, independent of sections.
     */
    private fun getSectionAndOffset(position: Int): Pair<Section, Int>? {
        var total = 0
        for (section in _sections) {
            val itemCount = section.getTotalCount()
            val maxIndex = total + itemCount
            if (position < maxIndex) {
                val offset = position - total
                return section to offset
            }
            total = maxIndex
        }
        return null
    }

    /**
     * Used to scroll the [RecyclerView] to top if scroll is at 0 and new items are inserted
     *  at position 0.
     */
    internal fun restoreScrollPositionAfterInsertion() {
        val recyclerView = recyclerViewReference.get() ?: return
        // Try to take into account LinearLayout horizontal orientation
        val linearLayoutManager = recyclerView.layoutManager as? LinearLayoutManager
        val isVertical = linearLayoutManager?.orientation != LinearLayoutManager.HORIZONTAL
        val scrollOffset = if (isVertical)
            recyclerView.computeVerticalScrollOffset()
            else recyclerView.computeHorizontalScrollOffset()
        if (recyclerView.isEmpty() || scrollOffset != 0) return
        // If offset was 0, scroll to position 0
        recyclerView.layoutManager?.scrollToPosition(0)
    }
}

