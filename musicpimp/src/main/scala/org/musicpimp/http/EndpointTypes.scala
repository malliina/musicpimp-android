package org.musicpimp.http

/**
 *
 * @author mle
 */
object EndpointTypes extends Enumeration {
  type EndpointType = Value
  val MusicPimp, MusicBeamer, Cloud, Subsonic, Local = Value
}