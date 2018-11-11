package org.musicpimp.andro.util

import android.os.Bundle

object Implicits {

  implicit class RichBundle(bundle: Bundle) {
    def findString(key: String) = Option(bundle getString key)

    def findBoolean(key: String) = Option(bundle getBoolean key)

    def findInt(key: String) = Option(bundle getInt key)
  }

}
