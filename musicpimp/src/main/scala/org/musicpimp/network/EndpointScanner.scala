package org.musicpimp.network

import java.io.IOException

import android.content.Context
import android.preference.PreferenceManager
import com.mle.android.network.WifiHelpers
import com.mle.concurrent.Futures
import com.mle.network.NetworkDevice
import com.mle.util.{Lists, Utils}
import com.mle.concurrent.ExecutionContexts.cached
import org.musicpimp.http.{Endpoint, EndpointTypes}
import org.musicpimp.util.{Keys, PimpSettings}

import scala.concurrent.Future
import scala.concurrent.duration._

trait EndpointScanner {
  def prefs(ctx: Context) = PreferenceManager.getDefaultSharedPreferences(ctx)

  def settings(ctx: Context) = new PimpSettings(prefs(ctx))

  /**
    * This method updates the IPs of saved, active WLAN endpoints as necessary.
    *
    * The goal is to maintain connectivity to certain machines known by
    * numerical IP address in a network configured with DHCP, where IP addresses
    * may change over time.
    *
    * Implementation: This method reads the SSID of the current WLAN connection
    * (if any) and checks if any active endpoint has an address that belongs to
    * this network. Then those endpoints are pinged to check whether
    * connectivity still exists.
    *
    * If the ping fails due to a connectivity problem, a scan of IPs adjacent to
    * the failed IP is initiated. The reasoning for this is that DHCP may have
    * assigned a different IP to the server, in which case the new IP is likely close
    * to the previous one. If pinging an adjacent IP succeeds while the other
    * connectivity parameters remain untouched, the non-working IP is of the endpoint
    * is replaced with the working one.
    *
    * The idea is to call this method whenever the device gains connectivity
    * to a WiFi network, thus ensuring that configured server IPs are valid.
    *
    * @param ctx context
    * @return a future that completes when the sync is complete
    */
  def syncWlanEndpoints(ctx: Context): Future[Unit] = {
    //    info("Syncing WLAN endpoints...")
    WifiHelpers.currentSSID(ctx).fold(Future.successful[Unit]())(ssid => {
      val activeEndpoints = settings(ctx).activeEndpoints(Keys.PREF_PLAYER, Keys.PREF_LIBRARY)
      //      debug(s"Got ${activeEndpointsWithSameSSID.size} endpoints with SSID: $ssid")
      val futures = activeEndpoints
        .filter(_.endpointType != EndpointTypes.Local)
        .distinct
        .map(pingOrElseSync(ctx, _, ssid)
          .recover(Utils.suppresser))
      Future.sequence(futures).map(_ => ())
    })
  }

  def searchEndpoint(ctx: Context): Future[Endpoint] = {
    val ips = NetworkDevice.hostAddresses
    searchEndpoint(ctx, ips, Futures.delay(12 seconds))
  }

  // good enough
  def isNumericalIP(host: String): Boolean =
    host.forall(c => c.isDigit || c == '.') && host.size >= 7 && host.size <= 15

  private def searchEndpoint(ctx: Context, ips: Seq[String], timeout: Future[Unit]): Future[Endpoint] =
    searchEndpoint(ctx, ips, radius = 10, skip = 0, timeout = timeout)

  private def searchEndpoint(ctx: Context, ips: Seq[String], radius: Int, skip: Int, timeout: Future[Unit]): Future[Endpoint] = {
    val allIPs = ips flatMap (ip => scanRange(ip, radius))
    val skipRange = ips flatMap (ip => scanRange(ip, skip))
    val range = allIPs diff skipRange
    if (range.nonEmpty) {
      val endpoints = endpointCandidates(WifiHelpers.currentSSID(ctx), range)
      // each AsyncHttpClient instance only does 10 parallel connections, so we use multiple instances
      withClients(count = (endpoints.size / 10) + 1)(clients => {
        Futures.firstSuccessful(endpoints, timeout = 8 seconds)(e => clients.nextClient.ping(e))
      }).recoverWith {
        case _: NoSuchElementException if !timeout.isCompleted =>
          // expands search to more distant IP addresses if there's time
          searchEndpoint(ctx, ips, radius + 10, skip + 10, timeout)
      }
    } else {
      Future.failed(new NoSuchElementException())
    }
  }

  private def pingOrElseSync(ctx: Context, e: Endpoint): Unit =
    WifiHelpers.currentSSID(ctx).foreach(ssid => pingOrElseSync(ctx, e, ssid))

  /**
    *
    * @return a working endpoint, if any
    */
  private def pingOrElseSync(ctx: Context, e: Endpoint, ssid: String): Future[Endpoint] = {
    UtilHttpClient.ping(e).map(_ => e).recoverWith {
      // The endpoint could not be reached. This suggests that the endpoint's
      // IP has changed (or it might just be offline), so we look for nearby endpoints
      // and modify the address of the saved endpoint if we can reach any.
      case _: IOException if isSyncable(e, ssid) =>
        //        info(s"Unable to ping ${e.host}:${e.port}, searching for nearby hosts...")
        sync(ctx, e)
    }
  }

  /** Searches for endpoints close to the host of `notFound` and replaces its host with a working one if any is found.
    *
    * @param ctx      context
    * @param notFound endpoint whose host may need replacing
    * @return the working endpoint, if found
    */
  private def sync(ctx: Context, notFound: Endpoint): Future[Endpoint] = {
    if (isNumericalIP(notFound.host)) {
      searchHost(notFound).map(working => {
        val errorOpt = settings(ctx).updateEndpoint(old = notFound, updated = working)
        //        info(s"Changed host of endpoint: ${notFound.name} from: ${notFound.host} to: ${working.host}.")
        errorOpt.fold(working)(error => throw new Exception(error))
      })
    } else {
      Future.failed(new Exception("Unable to scan endpoint with non-numerical IP address."))
    }
  }

  /**
    * Fails with a [[NoSuchElementException]] if no suitable endpoint was found, and a
    * [[scala.concurrent.TimeoutException]] if the timeout is reached.
    */
  private def searchHost(notFound: Endpoint): Future[Endpoint] = {
    val ip = notFound.host
    if (isNumericalIP(ip)) {
      val range = scanRange(ip, radius = 10)
      //      info(s"Scanning the following IPs: ${range.mkString(", ")}...")
      val candidates = endpointCandidates(range, notFound)
      withClients(count = (candidates.size / 10) + 1, notFound.username, notFound.password)(clients => {
        Futures.firstSuccessful(candidates, timeout = 8 seconds)(e => clients.nextClient.ping(e, pingResource = "/pingauth").filter(_.statusCode == 200))
      })
    } else {
      Future.failed(new Exception(s"Unable to determine IP range to scan because IP is not in numerical format: $ip."))
    }
  }

  private def withClients[T](count: Int, init: ClientProvider => Unit = _ => ())(f: ClientProvider => Future[T]): Future[T] = {
    val provider = new ClientProvider(count)
    init(provider)
    val ret = f(provider)
    ret.onComplete(_ => provider.close())
    ret
  }

  private def withClients[T](count: Int, username: String, password: String)(f: ClientProvider => Future[T]): Future[T] =
    withClients(count, _.clients.foreach(_.setBasicAuth(username, password)))(f)

  private def isSyncable(e: Endpoint, deviceSsid: String): Boolean =
    e.autoSync && isNumericalIP(e.host) && e.ssid.contains(deviceSsid) && e.endpointType == EndpointTypes.MusicPimp

  private def scanRange(ip: String, radius: Int): List[String] = {
    val adjacentIPs = NetworkDevice.adjacentIPs(ip, radius)
    // orders the adjacent IPs by proximity to the IP of this device
    val (smaller, larger) = adjacentIPs splitAt (adjacentIPs.size / 2)
    Lists.interleave(smaller.reverse, larger)
  }

  private def endpointCandidates(range: Seq[String], e: Endpoint): Seq[Endpoint] =
    endpoints(range, ip => e.copy(host = ip))

  private def endpointCandidates(ssid: Option[String], range: Seq[String]): Seq[Endpoint] =
    endpoints(range, ip => Endpoint(Endpoint.newID, ip, ip, 8456, "admin", "", EndpointTypes.MusicPimp, ssid))

  private def endpoints(range: Seq[String], f: String => Endpoint) =
    range map f
}

object EndpointScanner extends EndpointScanner
