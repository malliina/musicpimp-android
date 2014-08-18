package org.musicpimp.messaging

import org.musicpimp.andro.messaging.{GcmUtils, JsonMessagingUtils, GooglePlayMessagingUtils}
import org.musicpimp.http.Endpoint

/**
 * @author Michael
 */
class PimpGoogleMessaging(endpoint: Endpoint) extends GooglePlayMessagingUtils {
  override val serverMessenger: JsonMessagingUtils = new GcmUtils(endpoint)
}
