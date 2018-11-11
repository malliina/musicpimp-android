package org.musicpimp.ui.fragments

import android.app.{Dialog, TimePickerDialog}
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.format.DateFormat
import android.widget.TimePicker
import java.util.Calendar

class TimePickerFragment extends DialogFragment with TimePickerDialog.OnTimeSetListener {
  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val c = Calendar.getInstance()
    val hour = c get Calendar.HOUR_OF_DAY
    val minute = c get Calendar.MINUTE
    new TimePickerDialog(getActivity, this, hour, minute, DateFormat.is24HourFormat(getActivity))
  }

  /**
   *
   * @param view the view
   * @param hourOfDay [0, 23]
   * @param minute [0, 59]
   */
  override def onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int): Unit = {

  }
}
