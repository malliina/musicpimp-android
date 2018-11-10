package org.musicpimp.pimp

import com.mle.json.JsonFormats
import com.mle.storage.StorageSize
import org.musicpimp.audio.MusicItem
import play.api.libs.json.Json
import scala.concurrent.duration.Duration

object Alarms {

  /** Schedule for regularly executing something at a given time of day.
    *
    * @param hour   [0, 23] and *
    * @param minute [0, 59] and *
    * @param days   the weekdays during which this schedule is valid
    * @return the cron pattern
    */
  case class ClockSchedule(hour: Int, minute: Int, days: Seq[WeekDay]) {
    private val daysDescribed = if (days.toSet == WeekDay.EveryDaySet) "every day" else s"on $daysReadable"

    def timeFormatted = maybePrependZero(hour) + ":" + maybePrependZero(minute)

    def daysReadable = days.map(_.shortName).mkString(", ")

    def describe: String = s"at $timeFormatted $daysDescribed"

    private def maybePrependZero(i: Int) = if (i < 10) s"0$i" else s"$i"
  }

  object ClockSchedule {
    implicit val format = Json.format[ClockSchedule]
  }

  //  case class When(hour: Int, minute: Int, days: Seq[String])
  //
  //  object When {
  //    implicit val when = Json.format[When]
  //  }

  case class ShortTrack(track: String)

  object ShortTrack {
    implicit val shortTrack = Json.format[ShortTrack]
  }

  case class IdAlarm(id: Option[String], job: ShortTrack, when: ClockSchedule, enabled: Boolean)

  object IdAlarm {
    implicit val idAlarm = Json.format[IdAlarm]
  }

  case class AlarmCmd(cmd: String, ap: IdAlarm)

  object AlarmCmd {
    implicit val alarmCmd = Json.format[AlarmCmd]
  }

  case class ShortCmd(cmd: String, id: String)

  object ShortCmd {
    implicit val shortCmd = Json.format[ShortCmd]
  }

  case class ShortestCmd(cmd: String)

  object ShortestCmd {
    implicit val shortestCmd = Json.format[ShortestCmd]
  }

  case class VerboseTrack(id: String, title: String, artist: String, album: String, duration: Duration, size: StorageSize) extends MusicItem

  object VerboseTrack {
    implicit val storage = JsonFormats.storageSize
    implicit val dur = JsonFormats.duration
    implicit val verboseTrack = Json.format[VerboseTrack]
  }

  case class PlaybackJob(track: VerboseTrack)

  object PlaybackJob {
    implicit val playbackJob = Json.format[PlaybackJob]
  }

  case class Alarm(id: String, job: PlaybackJob, when: ClockSchedule, enabled: Boolean)

  object Alarm {
    implicit val alarm = Json.format[Alarm]
  }

}
