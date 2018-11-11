package org.musicpimp.ui.dialogs
import com.mle.andro.ui.dialogs.DefaultDialog

class MessageBoxDialog(message: Int, title: Option[Int])
  extends DefaultDialog(message, title, negativeText = None)
