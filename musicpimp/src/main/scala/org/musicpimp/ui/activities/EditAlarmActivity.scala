package org.musicpimp.ui.activities

import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.{CheckedTextView, TimePicker, ArrayAdapter}
import com.mle.andro.ui.adapters.IconTwoLinesAdapter
import com.mle.android.exceptions.AndroidException
import com.mle.android.http.HttpResponse
import com.mle.android.ui.Implicits._
import com.mle.concurrent.FutureImplicits.RichFuture
import com.mle.concurrent.ExecutionContexts.cached
import org.musicpimp.andro.util.Implicits.RichBundle
import org.musicpimp.audio._
import org.musicpimp.pimp.Alarms._
import org.musicpimp.pimp.{WeekDay, PimpServerPlayer, AlarmsClient}
import org.musicpimp.ui.fragments.TimePickerFragment
import org.musicpimp.util.{PimpSettings, PimpLog, Keys}
import org.musicpimp.{R, TR}
import rx.lang.scala.Subscription
import scala.Some
import scala.concurrent.Future

/**
 * @author Michael.
 */
class EditAlarmActivity
  extends FragmentActivity
  with LayoutBaseActivity
  with PimpLog {

  lazy val helper = new PimpSettings(activityHelper.prefs)

  var subscription: Option[Subscription] = None
  val shortDays = Seq("mon", "tue", "wed", "thu", "fri", "sat", "sun")
  val days = Seq("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

  protected def onPlayerEvent(event: PlayerEvent): Unit = event match {
    case PlayStateChanged(newState) =>
      updatePlayPauseText(newState)
    case _ => ()
  }

  var client: Option[AlarmsClient] = None
  var time: Time = Time(8, 0)
  var track = TitledID("", "")
  var alarmId: Option[String] = None

  override def contentView: Int = R.layout.edit_alarm

  def timeView = activityHelper.findView(TR.time_button)

  def submitButton = activityHelper.findView(TR.submit_button)

  def weekdayCheckBoxes = Seq(TR.mon, TR.tue, TR.wed, TR.thu, TR.fri, TR.sat, TR.sun) map activityHelper.findView

  def trackText = activityHelper.findView(TR.track_edit)

  def enabledButton = activityHelper.findView(TR.enabled_button)

  def playPauseButton = activityHelper.findView(TR.playPauseButton)

  override protected def onCreate2(state: Option[Bundle]): Unit = {
    for {
      bundle <- extras
      endpointID <- bundle findString Keys.ENDPOINT
      endpoint <- helper.endpoints.find(_.id == endpointID)
    } yield {
      val alarmsClient = new AlarmsClient(endpoint)
      client = Some(alarmsClient)
      val player = alarmsClient.player
      alarmsClient.connect()
      subscription = Some(player.events.subscribe(e => onPlayerEvent(e)))
      updatePlayPauseText(player.status.state)
      (bundle findString Keys.ALARM_ID).fold({
        for {
          id <- bundle findString Keys.TRACK_ID
          title <- bundle findString Keys.TRACK_TITLE
        } {
          track = TitledID(id, title)
        }
        fillEmpty(track.title)
      })(alarmID => {
        alarmsClient.alarms.map(alarms => alarms.find(_.id == alarmID)
          .fold(warn(s"No alarm found with ID: $alarmID"))(fill))
          .recoverAll(t => warn("Unable to load alarms", t))
      })
    }
    timeView.setOnClickListener(() => showTimePicker())
    submitButton.setOnClickListener(() => validateThenSave())
    weekdayCheckBoxes.foreach(_.setOnClickListener((view: View) => view.asInstanceOf[CheckedTextView].toggle()))
    playPauseButton.setOnClickListener(() => startOrStopPlayback())
  }

  def updatePlayPauseText(state: PlayStates.PlayState) {
    val textResource =
      if (state == PlayStates.Playing || state == PlayStates.Started) R.string.stop_playback
      else R.string.play_now
    activityHelper.onUiThread(playPauseButton setText textResource)
  }

  def unlisten(player: PimpServerPlayer) {
    subscription.foreach(_.unsubscribe())
  }

  def startOrStopPlayback(): Unit = {
    client.map(_.player).foreach(p => {
      if (p.status.isPlaying) stopPlayback()
      else startPlayback()
    })
  }

  def startPlayback() = {
    for {
      c <- client
      id <- alarmId
    } {
      c.start(this, id)
    }
  }

  def stopPlayback() = client.foreach(_.stop(this))

  def validateThenSave(): Unit = {
    val selectedDays = shortDays.zip(weekdayCheckBoxes).filter(p => p._2.isChecked).map(_._1)
    val selectedWeekDays = selectedDays.flatMap(WeekDay.withShortName)
    val errorOpt = validateInput(selectedWeekDays)
    if (errorOpt.isEmpty) {
      saveChanges(selectedWeekDays).onComplete(_ => finish())
    } else {
      activityHelper.showToast(errorOpt.get)
    }
  }

  def validateInput(days: Seq[WeekDay]): Option[String] =
    if (days.isEmpty) Some("Select at least one day.")
    else if (track.id.isEmpty) Some("Please specify a track to play.")
    else None

  def saveChanges(days: Seq[WeekDay]): Future[HttpResponse] = {
    info(s"Saving: Time: ${time.toString} Days: $days Track: $track")
    val updatedAlarm = IdAlarm(alarmId, ShortTrack(track.id), ClockSchedule(time.hour, time.minute, days), enabledButton.isChecked)
    client.fold(Future.failed[HttpResponse](new AndroidException("Unable to find client")))(client => {
      client.save(this, updatedAlarm)
    })
  }

  def fill(alarm: Alarm): Unit = activityHelper.onUiThread {
    alarmId = Some(alarm.id)
    val t = alarm.job.track
    val title = t.title
    track = TitledID(t.id, title)
    val when = alarm.when
    val checkedIndices = when.days.map(day => shortDays.indexOf(day.shortName)).filter(_ != -1)
    try {
      updateTime(when.hour, when.minute)
      checkedIndices.map(weekdayCheckBoxes.apply).foreach(_.setChecked(true))
      trackText setText title
      enabledButton setChecked alarm.enabled
    } catch {
      case e: Exception => warn("Cockup", e)
    }
  }

  def fillEmpty(track: String): Unit = activityHelper.onUiThread {
    updateTime(8, 0)
    weekdayCheckBoxes.take(5).foreach(_.setChecked(true))
    trackText setText track
  }

  def updateTime(hour: Int, minute: Int): Unit = {
    time = Time(hour, minute)
    activityHelper.onUiThread(timeView setText time.timeFormatted)
  }

  private def showTimePicker(): Unit = new AlarmTimePicker().show(getSupportFragmentManager, "timePicker")

  override def onDestroy(): Unit = {
    super.onDestroy()
    client.foreach(c => {
      subscription.foreach(_.unsubscribe())
      c.close()
    })
    client = None
    alarmId = None
  }

  class WeekdaysAdapter(ctx: Context)
    extends ArrayAdapter[String](ctx, android.R.layout.simple_list_item_checked, days.toArray)

  class AlarmTimePicker extends TimePickerFragment {
    /**
     *
     * @param view the view
     * @param hourOfDay [0, 23]
     * @param minute [0, 59]
     */
    override def onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int): Unit = {
      super.onTimeSet(view, hourOfDay, minute)
      updateTime(hourOfDay, minute)
    }
  }

  case class Time(hour: Int, minute: Int) {
    override def toString: String = timeFormatted

    def timeFormatted = maybePrependZero(hour) + ":" + maybePrependZero(minute)

    private def maybePrependZero(i: Int) = if (i < 10) s"0$i" else s"$i"
  }

  case class TitledID(id: String, title: String)

  class TracksAdapter(ctx: Context, tracks: Seq[Track]) extends IconTwoLinesAdapter[Track](ctx, tracks) {
    override def firstRow(item: Track, position: Int): String = item.title

    override def secondRow(item: Track, pos: Int): String = item.id

    override def imageResource(item: Track, position: Int): Int = android.R.drawable.ic_media_play
  }

}
