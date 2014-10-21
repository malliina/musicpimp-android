package org.musicpimp.iap.samsung

import android.app.Activity
import com.mle.android.iap._
import com.mle.concurrent.{PromiseHelpers, Futures}
import com.mle.concurrent.ExecutionContexts.cached
import com.sec.android.iap.lib.helper.SamsungIapHelper
import com.sec.android.iap.lib.listener.{OnGetItemListener, OnGetInboxListener, OnPaymentListener}
import com.sec.android.iap.lib.vo._
import java.text.SimpleDateFormat
import java.util
import java.util.{Locale, Date}
import scala.concurrent._

/**
 *
 * @author mle
 */
object SamsungIapUtils extends SamsungIapUtils {
  val itemGroupId = "100000102860"
  val premiumSku = "000001014214"
}

trait SamsungIapUtils extends IapUtilsBase with PromiseHelpers {
  def itemGroupId: String

  val iapMode = SamsungIapHelper.IAP_MODE_COMMERCIAL

  def helper(activity: Activity) = SamsungIapHelper.getInstance(activity, iapMode)

  override def productInfo(sku: String, activity: Activity): Future[ProductInfo] = {
    def failure = Future.failed[ProductInfo](new NoSuchElementException(s"Unable to find product with ID: $sku"))
    availableSkus(activity).flatMap(_.find(_.productId == sku).fold(failure)(Future.successful))
  }

  override def purchase(sku: String, activity: Activity): Future[String] =
    Futures.promisedFuture[String](p => {
      val listener = new FuturePaymentListener(p)
      helper(activity).startPayment(itemGroupId, sku, true, listener)
    })

  override def hasSku(sku: String, activity: Activity): Future[Boolean] =
    ownedSkus(activity).map(_.exists(i => i.productId == sku))

  protected def ownedSkus(activity: Activity): Future[Set[ProductInfo]] =
    Futures.promisedFuture[Set[ProductInfo]](p => {
      val d = new Date
      val sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault)
      val today = sdf format d
      val listener = new FutureOwnedSkusListener(p)
      helper(activity).getItemInboxList(itemGroupId, 1, 15, "20130101", today, listener)
    })

  protected def availableSkus(activity: Activity): Future[Set[ProductInfo]] =
    Futures.promisedFuture[Set[ProductInfo]](p => {
      val listener = new FutureItemListener(p)
      /**
       * Type of Item
       * 00: Consumable (Consumable product)
       * 01: NonConsumable (Nonconsumable product)
       * 02: Subscription (Short-term product)
       * 10: All (All types)
       */
      val allItemTypes = "10"
      helper(activity).getItemList(itemGroupId, 1, 15, allItemTypes, iapMode, listener)
    })

  /**
   * Listener that completes the supplied [[Promise]] when onPayment() is called.
   *
   * @param p promise to complete
   */
  class FuturePaymentListener(p: Promise[String]) extends OnPaymentListener {
    def onPayment(errorVo: ErrorVo, purchaseVo: PurchaseVo): Unit = {
      def fail(t: Throwable) = tryFailure(t, p)
      errorVo.getErrorCode match {
        case SamsungIapHelper.IAP_ERROR_NONE =>
          trySuccess(purchaseVo.getItemId, p)
        case SamsungIapHelper.IAP_ERROR_ALREADY_PURCHASED =>
          fail(new AlreadyPurchasedException)
        case SamsungIapHelper.IAP_ERROR_PRODUCT_DOES_NOT_EXIST =>
          fail(new InvalidSkuException)
        case SamsungIapHelper.IAP_PAYMENT_IS_CANCELED =>
          fail(new PurchaseCanceledException)
        case _ =>
          fail(new IapException(Option(errorVo.getErrorString).getOrElse("Samsung Apps in-app purchase error.")))
      }
    }
  }

  class FutureOwnedSkusListener(p: Promise[Set[ProductInfo]]) extends FutureListener[InboxVo] with OnGetInboxListener {
    def onGetItemInbox(errorVo: ErrorVo, items: util.ArrayList[InboxVo]): Unit =
      complete(p, errorVo, items)
  }

  class FutureItemListener(p: Promise[Set[ProductInfo]]) extends FutureListener[ItemVo] with OnGetItemListener {
    override def onGetItem(errorVo: ErrorVo, items: util.ArrayList[ItemVo]): Unit =
      complete(p, errorVo, items)
  }

  trait FutureListener[T <: BaseVo] {
    def complete(p: Promise[Set[ProductInfo]], errorVo: ErrorVo, items: util.ArrayList[T]): Unit = {
      import collection.JavaConversions._
      errorVo.getErrorCode match {
        case SamsungIapHelper.IAP_ERROR_NONE =>
          val userItems = Option(items).fold(Set.empty[T])(_.toSet)
          val itemIds: Set[ProductInfo] = userItems map productInfo
          trySuccess(itemIds, p)
        case _ =>
          tryFailure(new IapException("Unable to obtain list of items."), p)
      }
    }
  }

  private def productInfo(item: BaseVo) = ProductInfo(item.getItemId, item.getItemPriceString, item.getItemName, item.getItemDesc)
}