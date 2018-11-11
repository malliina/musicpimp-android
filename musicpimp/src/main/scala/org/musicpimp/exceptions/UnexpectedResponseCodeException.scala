package org.musicpimp.exceptions

import com.malliina.android.exceptions.ExplainedHttpException

class UnexpectedResponseCodeException(val responseCode: Int)
  extends ExplainedHttpException(Some(s"Unexpected response code: $responseCode")) {
  val reason = Option(getMessage) getOrElse s"Unexpected response code: $responseCode"
}
