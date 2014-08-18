package org.musicpimp.json

import org.musicpimp.http.Endpoint

/**
 *
 * @author mle
 */
abstract class JsonReaders(endpoint: Endpoint) {
  val username = endpoint.username
  val password = endpoint.password
}