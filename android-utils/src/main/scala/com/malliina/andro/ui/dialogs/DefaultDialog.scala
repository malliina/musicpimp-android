package com.malliina.andro.ui.dialogs

import com.malliina.andro.R.string.{cancel, ok}
import com.malliina.android.ui.dialogs.AbstractDialog

abstract class DefaultDialog(val message: Int,
                             val title: Option[Int] = None,
                             val positiveText: Option[Int] = Some(ok),
                             val negativeText: Option[Int] = Some(cancel))
  extends AbstractDialog
