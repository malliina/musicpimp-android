package org.musicpimp.network

import android.graphics.drawable.Drawable
import android.graphics.{Bitmap, BitmapFactory}
import java.io.File

trait CoverService {
  def client = DiscoGs.client

  def coverBitmap(artist: String, album: String) = client.cover(artist, album)

  def drawable(file: File) = Drawable.createFromPath(file.getAbsolutePath)

  def bitmap(file: File): Bitmap = BitmapFactory.decodeFile(file.getAbsolutePath)
}

object CoverService extends CoverService
