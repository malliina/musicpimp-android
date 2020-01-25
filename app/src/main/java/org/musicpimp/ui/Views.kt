package org.musicpimp.ui

import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
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
