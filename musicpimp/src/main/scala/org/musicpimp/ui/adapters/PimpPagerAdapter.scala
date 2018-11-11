package org.musicpimp.ui.adapters

import android.support.v4.app.{FragmentManager, Fragment, FragmentPagerAdapter}
import com.malliina.android.exceptions.ExplainedException
import org.musicpimp.ui.fragments.{PlayerFragment, PlaylistFragment, LibraryFragment}

class PimpPagerAdapter(fm: FragmentManager, combinedPlayerAndPlaylist: Boolean = false) extends FragmentPagerAdapter(fm) {

  val firstTwo = Seq(TabInfo("Music", selected = true), TabInfo("Player"))
  val tabs = if (combinedPlayerAndPlaylist) firstTwo else firstTwo :+ TabInfo("Playlist")
  val LIBRARY_TAB_POS = 0
  val PLAYER_TAB_POS = 1
  val PLAYLIST_TAB_POS = 2

  val getCount: Int = tabs.size

  /**
   * Note that this method is not called every time a tab change occurs,
   * therefore I do not see a need to cache fragments.
   *
   * @param position tab position
   * @return
   */
  def getItem(position: Int): Fragment = position match {
    case LIBRARY_TAB_POS => new LibraryFragment
    case PLAYER_TAB_POS => new PlayerFragment
    case PLAYLIST_TAB_POS => new PlaylistFragment
    case pos =>
      throw new ExplainedException(s"Invalid tab position: $pos. There are $getCount tabs available.")
  }
}

case class TabInfo(title: String, selected: Boolean = false)
