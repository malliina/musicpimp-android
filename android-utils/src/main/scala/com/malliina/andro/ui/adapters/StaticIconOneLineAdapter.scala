package com.malliina.andro.ui.adapters

import android.content.Context
import android.view.View
import com.malliina.andro.TR

class StaticIconOneLineAdapter(ctx: Context, layout: Int, items: Seq[TwoPartItem])
  extends IconOneLineAdapter[TwoPartItem](ctx, layout, items) {

  def imageResource(item: TwoPartItem, pos: Int): Int = item.firstResource

  def firstRow(item: TwoPartItem, pos: Int): String = "unused"

  override def decorate(view: View, item: TwoPartItem, position: Int): Unit = {
    findTypedView(view, TR.icon) setImageResource imageResource(item, position)
    findTypedView(view, TR.firstLine) setText item.secondResource
  }
}
