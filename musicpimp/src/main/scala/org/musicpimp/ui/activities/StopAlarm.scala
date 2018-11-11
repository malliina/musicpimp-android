package org.musicpimp.ui.activities

import android.content.Intent
import android.os.Bundle
import com.malliina.android.ui.activities.BaseActivity
import com.malliina.concurrent.ExecutionContexts.cached
import org.musicpimp.andro.ui.ActivityHelper
import org.musicpimp.andro.util.Implicits.RichBundle
import org.musicpimp.http.Endpoint
import org.musicpimp.pimp.AlarmsClient
import org.musicpimp.util.{Keys, PimpSettings, PimpLog}

class StopAlarm extends BaseActivity with PimpLog {
  lazy val settings = new PimpSettings(new ActivityHelper(this).prefs)

  def extras = Option(getIntent.getExtras)

  override protected def onCreate2(savedInstanceState: Option[Bundle]): Unit = {
    val key = StopAlarm.tagKey
    val tagOpt = extras.flatMap(_ findString key)
    tagOpt.fold(warn(s"No key: $key in intent extras."))(onMessage)
    val intent = new Intent(this, classOf[AlarmsActivity])
    tagOpt.foreach(tag => intent.putExtra(Keys.ENDPOINT, tag))
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    startActivity(intent)
    finish()
  }

  def onMessage(tag: String): Unit = {
    settings.endpoints.find(_.id == tag).fold(warn(s"Unable to find endpoint with ID: $tag"))(stopPlayback)
  }

  def stopPlayback(e: Endpoint): Unit = {
    val client = new AlarmsClient(e)
    client.stop(this).onComplete(_ => client.close())
  }
}

object StopAlarm {
  val tagKey = "tag"
}
