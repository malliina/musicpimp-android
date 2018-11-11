package org.musicpimp.ui

import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener

object Implicits {
  implicit def fun2checkedChangeListener(f: Boolean => Unit): OnCheckedChangeListener = {
    new OnCheckedChangeListener {
      override def onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean): Unit = f(isChecked)
    }
  }
}
