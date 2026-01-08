package com.example.media3uamp

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.media3uamp.databinding.ActivityMainBinding
import com.example.media3uamp.ui.view.MinibarPlayerComponent

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var minibarComponent: MinibarPlayerComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHost.navController
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        minibarComponent = MinibarPlayerComponent(
            activity = this,
            minibarBinding = binding.minibar,
            navController = navController,
            navHostFragmentId = R.id.nav_host,
            navHostView = binding.navHost,
            playerDestinationId = R.id.playerFragment,
        ).also { it.bind() }

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    override fun onDestroy() {
        minibarComponent?.unbind()
        minibarComponent = null
        super.onDestroy()
    }
}
