package org.musicpimp.ui.adapters

import android.content.Context
import android.view.View
import android.widget.TextView
import com.malliina.andro.ui.adapters.{BaseArrayAdapter, TwoPartItem}
import org.musicpimp.R
import org.musicpimp.ui.Assets

/** Adapter in which each row contains two columns of text.
  *
  * Note that a column of "text" may be an icon for example when font-awesome
  * is used, so this may be used to actually display one font awesome icon next
  * to some text.
  */
class FontAwesomeAdapter(ctx: Context, items: Seq[TwoPartItem])
  extends BaseArrayAdapter[TwoPartItem](ctx, R.layout.two_columns_text_item, items) {

  def decorate(view: View, item: TwoPartItem, position: Int): Unit = {
    val firstColumn = view.findViewById[TextView](R.id.firstColumn)
    firstColumn setText item.firstResource
    firstColumn setTypeface Assets.fontAwesome

    view.findViewById[TextView](R.id.secondColumn) setText item.secondResource
  }
}
