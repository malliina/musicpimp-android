package org.musicpimp.messaging

import com.amazon.device.messaging.ADMMessageReceiver

/**
 * @author Michael
 */
class PimpAdmReceiver extends ADMMessageReceiver(classOf[PimpAdmService])