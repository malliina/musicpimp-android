package org.musicpimp.ui.adapters

import android.content.Context
import com.mle.andro.ui.adapters.IconTwoLinesAdapter
import org.musicpimp.http.Endpoint

/**
 *
 * @author mle
 */
class EndpointAdapter(ctx: Context, endpoints: Seq[Endpoint])
  extends IconTwoLinesAdapter[Endpoint](ctx, endpoints) {

  def imageResource(item: Endpoint, pos: Int): Int =
    if (pos >= 1) android.R.drawable.ic_menu_edit
    else org.musicpimp.R.drawable.guitar_light

  def firstRow(item: Endpoint, pos: Int): String = item.name

  def secondRow(item: Endpoint, pos: Int): String =
    if (item.port > 0) item.host + ":" + item.port
    else ""
}
