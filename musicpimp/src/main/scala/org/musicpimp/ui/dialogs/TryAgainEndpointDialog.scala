package org.musicpimp.ui.dialogs

import org.musicpimp.http.Endpoint

/**
 *
 * @author mle
 */
class TryAgainEndpointDialog(e: Endpoint) extends EndpointDialog(e) {
  override protected val msg: String = "Invalid password. Please try again."
}
