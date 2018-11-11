package com.malliina.andro.ui.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener

/** This view has a bug: the seekbar "ball" goes to zero if the seekbar value is changed
  * programmatically. The bar itself displays the correct height.
  *
  * @see http://stackoverflow.com/questions/4892179/how-can-i-get-a-working-vertical-seekbar-in-android
  */
class VerticalSeekBar(context: Context, attrs: AttributeSet, defaultStyle: Int)
  extends SeekBar(context, attrs, defaultStyle) {

  def this(context: Context, attrs: AttributeSet) = this(context, attrs, android.R.attr.seekBarStyle)

  def this(context: Context) = this(context, null, android.R.attr.seekBarStyle)

  var listener: Option[OnSeekBarChangeListener] = None

  override def setOnSeekBarChangeListener(l: OnSeekBarChangeListener) {
    listener = Option(l)
  }

  override def onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
    super.onSizeChanged(h, w, oldH, oldW)
  }

  protected override def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    synchronized {
      super.onMeasure(heightMeasureSpec, widthMeasureSpec)
      setMeasuredDimension(getMeasuredHeight, getMeasuredWidth)
    }
  }

  override protected def onDraw(c: Canvas) {
    c.rotate(-90)
    c.translate(-getHeight, 0)
    super.onDraw(c)
  }

  @Override
  override def onTouchEvent(event: MotionEvent): Boolean = {
    if (!isEnabled) {
      false
    } else {
      event.getAction match {
        case MotionEvent.ACTION_DOWN | MotionEvent.ACTION_MOVE | MotionEvent.ACTION_UP =>
          setProgress(getMax - (getMax * event.getY / getHeight).toInt)
          onSizeChanged(getWidth, getHeight, 0, 0)
          // is fromUser (the last parameter) really always true?
          listener.foreach(_.onProgressChanged(this, getProgress, true))
        case MotionEvent.ACTION_CANCEL => ()
        case _ => ()
      }
      true
    }
  }
}
