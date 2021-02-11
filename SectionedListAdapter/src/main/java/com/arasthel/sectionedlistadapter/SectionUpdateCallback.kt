package com.arasthel.sectionedlistadapter

import androidx.recyclerview.widget.ListUpdateCallback
import java.lang.ref.WeakReference

class SectionUpdateCallback(section: Section): ListUpdateCallback {

    private val section = WeakReference(section)

    override fun onInserted(position: Int, count: Int) {
        section.get()?.notifyItemsInserted(position, count)
    }

    override fun onRemoved(position: Int, count: Int) {
        section.get()?.notifyItemsRemoved(position, count)
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        section.get()?.notifyItemMoved(fromPosition, toPosition)
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        section.get()?.notifyItemsChanged(position, count, payload)
    }

}