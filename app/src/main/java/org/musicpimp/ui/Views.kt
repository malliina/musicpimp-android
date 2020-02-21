package org.musicpimp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import org.musicpimp.R

fun <VH : RecyclerView.ViewHolder> RecyclerView.init(
    layout: RecyclerView.LayoutManager,
    vhAdapter: RecyclerView.Adapter<VH>
) {
    setHasFixedSize(false)
    layoutManager = layout
    adapter = vhAdapter
    addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
        setDrawable(resources.getDrawable(R.drawable.horizontal_divider, context.theme))
    })
}

class Controls(val progress: MaterialProgressBar?, val list: RecyclerView, val feedback: TextView) {
    fun enableLoading() {
        progress?.let { it.visibility = View.VISIBLE }
        list.visibility = View.GONE
        feedback.visibility = View.GONE
    }

    fun showList() {
        progress?.let { it.visibility = View.GONE }
        list.visibility = View.VISIBLE
        feedback.visibility = View.GONE
    }

    fun display(message: String) {
        progress?.let { it.visibility = View.GONE }
        list.visibility = View.GONE
        feedback.visibility = View.VISIBLE
        feedback.text = message
    }
}

abstract class ResourceFragment(private val layoutResource: Int): Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutResource, container, false)
    }
}
