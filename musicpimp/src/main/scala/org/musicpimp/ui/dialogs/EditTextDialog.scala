package org.musicpimp.ui.dialogs

import android.app.{Dialog, AlertDialog}
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.widget.EditText
import com.malliina.android.ui.Implicits._
import org.musicpimp.R
import org.musicpimp.R.string._

abstract class EditTextDialog(positiveLabel: Int = R.string.ok,
                              prefilledValue: Option[String] = None,
                              messageRes: Option[Int] = None,
                              message: Option[String] = None,
                              titleRes: Option[Int] = None,
                              hint: Int = R.string.empty_string)
  extends DialogFragment {

  def decorate(view: EditText): Unit = ()

  def buildHelp(builder: AlertDialog.Builder): Unit = ()

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val builder = new AlertDialog.Builder(getActivity)
    val view = getActivity.getLayoutInflater.inflate(R.layout.edit_text, null)
    val editText = view.findViewById(R.id.dialog_text).asInstanceOf[EditText]
    decorate(editText)
    editText setHint hint
    prefilledValue foreach editText.setText
    builder
      .setView(view)
      .setPositiveButton(positiveLabel, (_: DialogInterface, _: Int) => onPositive(editText.getText.toString))
      .setNegativeButton(cancel, (_: DialogInterface, _: Int) => ())
    message.map(msg => builder.setMessage(msg)) orElse messageRes.map(res => builder.setMessage(res))
    titleRes.map(t => builder setTitle t)
    buildHelp(builder)
    builder.create()
  }

  def onPositive(input: String)
}