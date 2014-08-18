package org.musicpimp.andro.util

import android.content.SharedPreferences
import com.mle.android.util.PreferenceImplicits.RichPrefs
import play.api.libs.json.Json._
import play.api.libs.json.{Reads, Writes}

/**
 * @author Michael
 */
class SettingsHelper(prefs: SharedPreferences) {
  def loadStrings(key: String) = loadSeq[String](key)

  def save[T](key: String, values: Seq[T])(implicit tjs: Writes[T]): Unit =
    savePref(key, stringify(toJson(values)))

  def savePref(key: String, value: String): Unit =
    prefs.put(key, value)

  def loadSeqOrEmpty[T](key: String)(implicit tjs: Reads[T]) =
    loadSeq(key) getOrElse Seq.empty

  def loadSeq[T](key: String)(implicit tjs: Reads[T]): Option[Seq[T]] =
    loadString(key).map(str => parse(str).asOpt[Seq[T]].getOrElse(Seq.empty[T]))

  def loadString(key: String): Option[String] = Option(prefs.getString(key, null))
}