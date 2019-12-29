package org.musicpimp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.musicpimp.media.LocalPlayer

class MainActivity : AppCompatActivity() {
    private var currentNavController: LiveData<NavController>? = null

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var local: LocalPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        } // Else, need to wait for onRestoreInstanceState
//        local.registerCallback(MediaBrowserListener())
        local = (application as PimpApp).conf.local
//        local.browser.registerCallback()
        window.decorView.setBackgroundColor(resources.getColor(R.color.colorBackground, theme))
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
        local.browser.onStart()
        viewModel.openSocket()
    }

    override fun onStop() {
        super.onStop()
        local.browser.onStop()
        viewModel.closeSocket()
    }

    private fun setupBottomNavigationBar() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav_view)
        val topLevelDestinations = listOf(R.navigation.music, R.navigation.player, R.navigation.settings)
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
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }
}
