package org.musicpimp.network

import android.content.Context
import com.malliina.android.network.WifiHelpers
import com.malliina.network.NetworkDevice

import scala.util.Try

trait PimpWifiHelpers extends WifiHelpers {
  /**
    *
    * @param ip an ip address
    * @return the SSID of the WLAN network, if the supplied IP belongs to it
    */
  def ssid(ctx: Context, ip: String): Option[String] =
    if (isWlanIP(ip)) currentSSID(ctx)
    else None

  def isWlanIP(ip: String): Boolean =
    Try {
      EndpointScanner.isNumericalIP(ip) &&
        NetworkDevice.hostAddresses.exists(addr => network(ip) == network(addr))
    }.getOrElse {
      false
    }
}

object PimpWifiHelpers extends PimpWifiHelpers
