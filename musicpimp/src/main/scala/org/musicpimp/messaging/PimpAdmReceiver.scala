package org.musicpimp.messaging

import com.amazon.device.messaging.ADMMessageReceiver

class PimpAdmReceiver extends ADMMessageReceiver(classOf[PimpAdmService])
