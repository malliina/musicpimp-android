package org.musicpimp.util

import com.mle.android.events.EventSource

/**
 *
 * @author mle
 */
trait Messaging extends EventSource[UIMessage] {
  /**
   * Fires the message.
   *
   * Exposes the message sending API so we can fire messages from anywhere.
   *
   * @param msg the message
   */
  def send(msg: String) = fire(BasicMessage(msg))

  def reload(silent: Boolean = true) = fire(Reload(silent))

  def limitExceeded() = fire(PlaybackLimitExceeded)
}

object Messaging extends Messaging

trait UIMessage

case class BasicMessage(msg: String) extends UIMessage

case class Reload(silent: Boolean) extends UIMessage

case object PlaybackLimitExceeded extends UIMessage
