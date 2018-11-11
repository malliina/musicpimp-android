package org.musicpimp.http

object EndpointTypes extends Enumeration {
  type EndpointType = Value
  val MusicPimp, MusicBeamer, Cloud, Subsonic, Local = Value
}