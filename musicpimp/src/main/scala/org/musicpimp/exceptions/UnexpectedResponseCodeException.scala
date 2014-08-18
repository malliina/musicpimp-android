package org.musicpimp.exceptions

import com.mle.android.exceptions.ExplainedHttpException

/**
 *
 * @author mle
 */
class UnexpectedResponseCodeException(val responseCode: Int)
  extends ExplainedHttpException(Some(s"Unexpected response code: $responseCode")) {
  val reason = Option(getMessage) getOrElse s"Unexpected response code: $responseCode"
}