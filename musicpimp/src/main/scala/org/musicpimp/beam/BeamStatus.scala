package org.musicpimp.beam

import play.api.libs.json.Json

/**
 *
 * @author mle
 */
/**
 *
 * @param user the user
 * @param exists true if a player with the given user exists, false otherwise
 * @param ready true if the player assigned to the given user is not receiving a stream currently
 */
case class BeamStatus(user: String, exists: Boolean, ready: Boolean)

object BeamStatus {
  implicit val jsonReader = Json.format[BeamStatus]
}
