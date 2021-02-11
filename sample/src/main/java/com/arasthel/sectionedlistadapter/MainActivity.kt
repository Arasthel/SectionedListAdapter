package com.arasthel.sectionedlistadapter

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class MainActivity : AppCompatActivity() {

    private val adapter = SectionedListAdapter()
    private val sections = mutableListOf(TestSection().apply {
        hasHeader = true
        defaultItemLayoutId = R.layout.item_test
        headerLayoutId = R.layout.item_test_header
        footerLayoutId = R.layout.item_test_footer
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter.addSections(sections)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView) ?: return
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.addSectionButton).setOnClickListener {
            val newSection = TestSection().apply {
                hasHeader = true
                defaultItemLayoutId = R.layout.item_test
                headerLayoutId = R.layout.item_test_header
                footerLayoutId = R.layout.item_test_footer
                updateItems((0 until 10).map { Item("Item #$it") })
            }
            adapter.addSection(newSection)
            sections.add(newSection)
        }

        findViewById<Button>(R.id.toggleHeadersButton).setOnClickListener {
            sections.forEach { it.hasHeader = !it.hasHeader }
        }

        findViewById<Button>(R.id.toggleFootersButton).setOnClickListener {
            sections.forEach { it.hasFooter = !it.hasFooter }
        }

        findViewById<Button>(R.id.addItemButton).setOnClickListener {
            sections.forEach { it.updateItems(it.getItems() + Item("Testing")) }
        }

        findViewById<Button>(R.id.removeItemButton).setOnClickListener {
            sections.forEach { it.updateItems(it.getItems().drop(1)) }
        }

        findViewById<Button>(R.id.toggleVisibilityButton).setOnClickListener {
            val firstSection = adapter.sections.firstOrNull() ?: return@setOnClickListener
            firstSection.isVisible = !firstSection.isVisible
        }
    }

    override fun onStart() {
        super.onStart()

        sections.first().updateItems(listOf(Item("Testing")))
    }
}

data class Item(val title: String)
class TestSection: com.arasthel.sectionedlistadapter.Section() {

    private val items = mutableListOf<Item>()

    fun updateItems(newItems: List<Item>) {
        val diff = DiffUtil.calculateDiff(object: DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return items.size
            }

            override fun getNewListSize(): Int {
                return newItems.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] === newItems[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].title == newItems[newItemPosition].title
            }
        }, true)
        diff.dispatchUpdatesTo(updateCallback)
        items.clear()
        items.addAll(newItems)
    }

    fun getItems(): List<Item> {
        return items
    }

    override fun getSectionItemCount(): Int {
        return items.count()
    }

    override fun createItemViewHolder(
        parent: ViewGroup,
        itemViewType: Int
    ): RecyclerView.ViewHolder {
        return TestViewHolder(createDefaultItemView(parent)!!)
    }

    override fun bindItemViewHolder(position: Int, viewHolder: RecyclerView.ViewHolder, payloads: List<Any>?) {
        val item = items[position]
        viewHolder.itemView.findViewById<TextView>(R.id.titleView)?.apply {
            text = "Item #${position}"
        }
    }

    override fun createHeaderViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return TestHeaderViewHolder(createHeaderView(parent)!!)
    }

    override fun bindHeaderViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder.itemView as? TextView)?.text = "HEADER"
    }

    override fun createFooterViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return TestHeaderViewHolder(createFooterView(parent)!!)
    }

    override fun bindFooterViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder.itemView as? TextView)?.text = "Footer"
    }

}

class TestViewHolder(view: View): RecyclerView.ViewHolder(view)
class TestHeaderViewHolder(view: View): RecyclerView.ViewHolder(view)