package com.malliina.andro.ui.adapters

import android.content.Context
import android.view.View
import com.malliina.andro.{R, TR}

class OneLineAdapter(ctx: Context, items: Seq[String])
  extends BaseArrayAdapter[String](ctx, R.layout.one_line, items) {

  def decorate(view: View, item: String, position: Int): Unit =
    findTypedView(view, TR.firstLine) setText item
}
