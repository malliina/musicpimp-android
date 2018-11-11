package com.malliina.andro.ui.adapters

import android.content.Context
import com.malliina.andro.R

/** Adapter in which each row contains two columns of text.
  *
  * Note that a column of "text" may be an icon for example when font-awesome
  * is used, so this may be used to actually display one font awesome icon next
  * to some text.
  */
abstract class TwoColumnsAdapter(ctx: Context, items: Seq[TwoPartItem])
  extends BaseArrayAdapter[TwoPartItem](ctx, R.layout.two_columns_text_item, items)
