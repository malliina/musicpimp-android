package org.musicpimp.ui.adapters

import android.content.Context
import android.view.View
import android.widget.TextView
import com.mle.andro.ui.adapters.{TwoPartItem, BaseArrayAdapter}
import org.musicpimp.ui.Assets
import org.musicpimp.{TR, R}

/** Adapter in which each row contains two columns of text.
  *
  * Note that a column of "text" may be an icon for example when font-awesome
  * is used, so this may be used to actually display one font awesome icon next
  * to some text.
  */
class FontAwesomeAdapter(ctx: Context, items: Seq[TwoPartItem])
  extends BaseArrayAdapter[TwoPartItem](ctx, R.layout.two_columns_text_item, items) {

  def decorate(view: View, item: TwoPartItem, position: Int): Unit = {
    val firstColumn = findView(view, TR.firstColumn.id).asInstanceOf[TextView]
    firstColumn setText item.firstResource
    firstColumn setTypeface Assets.fontAwesome
    findView(view, TR.secondColumn.id).asInstanceOf[TextView] setText item.secondResource
  }
}