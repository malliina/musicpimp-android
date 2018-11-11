package org.musicpimp.ui.activities

import android.app.Activity
import android.content.Intent
import com.google.zxing.client.android.CaptureActivity

/** Scans a barcode and provides the scanned code in a result to the activity
  * that started this activity with Activity.startActivityForResult(...)
  */
class ScannerActivity extends CaptureActivity {
  override def onTextScanned(text: String): Unit = {
    val resultData = new Intent
    resultData putExtra(BeamScanActivity.SCAN_RESULT, text)
    setResult(Activity.RESULT_OK, resultData)
    finish()
  }
}
