package com.malliina.andro.ui.adapters

import android.content.Context
import android.view.View
import com.malliina.andro.{R, TR}

/** An adapter for items with one icon and one text view.
  *
  * @param ctx              context
  * @param layoutResourceId layout resource for one row
  * @param items            sequence of items
  * @tparam T type of item
  */
abstract class IconOneLineAdapter[T](ctx: Context, layoutResourceId: Int, items: Seq[T])
  extends BaseArrayAdapter[T](ctx, layoutResourceId, items) {
  def this(ctx: Context, items: Seq[T]) = this(ctx, R.layout.icon_oneline_item, items)

  def decorate(view: View, item: T, position: Int): Unit = {
    findTypedView(view, TR.icon) setImageResource imageResource(item, position)
    findTypedView(view, TR.firstLine) setText firstRow(item, position)
  }

  def imageResource(item: T, position: Int): Int

  def firstRow(item: T, position: Int): String
}
