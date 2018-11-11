package com.malliina.andro.ui.adapters

import android.content.Context
import com.malliina.andro.R

/** Adapter that shows an edit icon next to one line of text.
  *
  * @param editableFrom position from which items will be editable: defaults to 0 meaning all items are editable
  */
abstract class EditIconOneLineAdapter(ctx: Context, items: Seq[CharSequence], editableFrom: Int = 0)
  extends IconOneLineAdapter[String](ctx, R.layout.icon_oneline_item2, items.map(_.toString)) {

  def noEditImageResource: Int

  def imageResource(item: String, position: Int): Int =
    if (position <= editableFrom) noEditImageResource
    else android.R.drawable.ic_menu_edit

  def firstRow(item: String, position: Int): String = item
}
