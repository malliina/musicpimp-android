package org.musicpimp.ui.activities

import android.app.Activity
import android.support.v4.app.Fragment
import org.musicpimp.util.{Messaging, UIMessage}
import com.malliina.android.events.EventSource

/** TODO investigate if we can elegantly not repeat ourselves
  */
trait MessageHandlerActivity extends Activity with BasicProxy {
  override def onResume() {
    source addHandler handler
    super.onResume()
  }

  override def onPause() {
    source removeHandler handler
    super.onPause()
  }
}

trait MessageHandlerFragment extends Fragment with BasicProxy {
  override def onResume() {
    source addHandler handler
    super.onResume()
  }

  override def onPause() {
    source removeHandler handler
    super.onPause()
  }
}

trait MessagingProxy {
  protected val source: EventSource[UIMessage] = Messaging
}

trait BasicProxy extends com.malliina.events.Proxy[UIMessage] with MessagingProxy
