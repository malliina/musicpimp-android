package org.musicpimp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_player.view.*
import org.musicpimp.endpoints.LocalEndpoint
import org.musicpimp.ui.settings.SettingsViewModel

class MainActivity : AppCompatActivity() {
    private var currentNavController: LiveData<NavController>? = null

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private var latestState: Playstate = Playstate.NoMedia
    private var isFloatingPlaybackBlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        settingsViewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)
        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        } // Else, need to wait for onRestoreInstanceState
        // Sets the app background apparently. Is there no easier way?
        window.decorView.setBackgroundColor(resources.getColor(R.color.colorBackground, theme))

        val view = floating_playback
        view.play_button.setOnClickListener {
            viewModel.resume()
        }
        view.pause_button.setOnClickListener {
            viewModel.pause()
        }
        view.next_button.setOnClickListener {
            viewModel.next()
        }
        view.prev_button.setOnClickListener {
            viewModel.previous()
        }
        viewModel.stateUpdates.observe(this) { state ->
            latestState = state
            if (state == Playstate.Playing) {
                if (!isFloatingPlaybackBlocked) {
                    view.visibility = View.VISIBLE
                }
                view.pause_button.visibility = View.VISIBLE
                view.play_button.visibility = View.GONE
                view.animate().translationY(0f).alpha(1f).setListener(null)
            } else {
                view.pause_button.visibility = View.GONE
                view.play_button.visibility = View.VISIBLE
                // Animates playback controls up/down when playback is playing/not playing
                view.animate().translationY(view.height.toFloat()).alpha(0f)
                    .setListener(object : AnimatorListenerAdapter() {
                        // When changing track, the playstate quickly goes from
                        // playing -> not playing -> playing. In that case, the
                        // animation triggered by "not playing" is cancelled
                        // and ends, but we don't want to hide the view then
                        // since we're playing music again. The Playing playstate
                        // may arrive before onAnimationEnd is called for the
                        // previous animation.
                        var isCancelled = false

                        override fun onAnimationCancel(animation: Animator?) {
                            super.onAnimationCancel(animation)
                            isCancelled = true
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            if (!isCancelled)
                                view.visibility = View.GONE
                        }
                    })
            }
        }
        settingsViewModel.musicSource.observe(this) { source ->
            val isLocal = source.id == LocalEndpoint.local.id
            val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav_view)
            bottomNav.menu.findItem(R.id.nav_playlists).isVisible = !isLocal
        }
    }

    fun toggleControls(block: Boolean) {
        isFloatingPlaybackBlocked = block
        val visibility = if (latestState == Playstate.Playing && !block) View.VISIBLE else View.GONE
        floating_playback.visibility = visibility
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }

    override fun onStart() {
        super.onStart()
        viewModel.openSocket()
    }

    override fun onStop() {
        super.onStop()
        viewModel.closeSocket()
    }

    private fun setupBottomNavigationBar() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav_view)
        val topLevelDestinations =
            listOf(
                R.navigation.music,
                R.navigation.player,
                R.navigation.playlists,
                R.navigation.settings
            )
        val controller = bottomNav.setupWithNavController(
            navGraphIds = topLevelDestinations,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_container,
            intent = intent
        )
        // Whenever the selected controller changes, setup the action bar.
        controller.observe(this, Observer { navController ->
            setupActionBarWithNavController(navController)
        })
        currentNavController = controller
        bottomNav.menu.findItem(R.id.nav_playlists).isVisible = false
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }
}
