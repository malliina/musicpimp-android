package org.musicpimp.messaging

import org.musicpimp.andro.messaging.{AdmUtils, AmazonMessaging}
import org.musicpimp.http.Endpoint

class PimpAmazonMessaging(endpoint: Endpoint) extends AmazonMessaging {
  override val serverMessenger = new AdmUtils(endpoint)
}
