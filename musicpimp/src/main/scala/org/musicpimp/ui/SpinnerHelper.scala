package org.musicpimp.ui

import android.app.Activity
import android.widget.Spinner
import com.mle.andro.ui.adapters.OneLineAdapter
import com.mle.android.ui.Implicits.action2itemSelectedListener
import org.musicpimp.TypedResource
import org.musicpimp.andro.ui.ActivityHelper

/**
 * @author Michael
 */
abstract class SpinnerHelper(activity: Activity) {
  lazy val helper = new ActivityHelper(activity)

  def spinnerChoices: Seq[String]

  def onSpinnerItemSelected(element: String): Unit

  def spinnerResource: TypedResource[Spinner]

  def spinnerView: Option[Spinner] = helper.tryFindView(spinnerResource)

  def reload() = spinnerView foreach loadSpinnerChoices

//  override def onResume(): Unit = {
//    super.onResume()
//    spinnerView foreach loadSpinnerChoices
//  }

  def loadSpinnerChoices(spinner: Spinner): Unit = {
    val choices = spinnerChoices
    val playersAdapter = new OneLineAdapter(activity, choices)
    helper.onUiThread {
      playersAdapter setDropDownViewResource android.R.layout.simple_spinner_dropdown_item
      spinner setAdapter playersAdapter
      initialSpinnerSelection(choices).foreach(elem => {
        val idx = choices indexOf elem
        if (idx >= 0) {
          // setSelection triggers onSpinnerItemSelected, but we don't want
          // it to trigger when setting the initial value, so we temporarily
          // nullify any listener during init
          spinner setOnItemSelectedListener null
          spinner setSelection(idx, false)
        }
      })
    }
    spinner setOnItemSelectedListener ((name: String) => onSpinnerItemSelected(name))
  }

  def initialSpinnerSelection(choices: Seq[String]): Option[String] = choices.headOption

  def onSpinnerInit(spinner: Spinner, choices: Seq[String]) = ()
}
