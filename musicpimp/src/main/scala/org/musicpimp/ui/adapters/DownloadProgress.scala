package org.musicpimp.ui.adapters

/**
 * @author Michael
 */
case class DownloadProgress(bytes: Long, total: Long, transferring: Boolean)

object DownloadProgress {
  val empty = new DownloadProgress(0, 0, transferring = false)
}