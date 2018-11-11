package org.musicpimp.ui.receivers

import android.content.Context
import android.net.NetworkInfo
import com.malliina.android.receivers.ConnectivityReceiver
import org.musicpimp.network.EndpointScanner

class SyncingWLANConnectivityReceiver extends ConnectivityReceiver {
  override def onWifiConnected(ctx: Context, activeNetwork: NetworkInfo): Unit =
    EndpointScanner.syncWlanEndpoints(ctx)
}
