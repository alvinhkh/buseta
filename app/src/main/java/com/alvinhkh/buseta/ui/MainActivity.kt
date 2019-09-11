package com.alvinhkh.buseta.ui

import android.app.ActivityManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.annotation.ColorInt
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.core.content.ContextCompat

import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.ui.FollowGroupFragment
import com.alvinhkh.buseta.mtr.ui.MtrLineStatusFragment
import com.alvinhkh.buseta.search.ui.HistoryFragment
import com.alvinhkh.buseta.service.ProviderUpdateService
import com.alvinhkh.buseta.utils.AdViewUtil
import com.alvinhkh.buseta.utils.ColorUtil
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber
import timber.log.Timber.log


class MainActivity : BaseActivity(), InstallStateUpdatedListener {

    private val APP_UPDATE_REQUEST_CODE = 1100
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow)
        setToolbar()
        supportActionBar?.run {
            setTitle(R.string.app_name)
            subtitle = null
            setDisplayHomeAsUpEnabled(false)
        }
        adViewContainer = findViewById(R.id.adView_container)
        adView = AdViewUtil.banner(adViewContainer)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            @ColorInt var colorRes = 0
            var title = getString(R.string.app_name)
            val fm = supportFragmentManager
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            when (item.itemId) {
                R.id.action_follow -> {
                    if (fm.findFragmentByTag("follow_list") == null) {
                        title = getString(R.string.app_name)
                        colorRes = ContextCompat.getColor(this, R.color.colorPrimary)
                        val ft = fm.beginTransaction()
                        ft.replace(R.id.fragment_container, FollowGroupFragment())
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        ft.addToBackStack("follow_list")
                        ft.commit()
                    }
                }
                R.id.action_search_history -> {
                    if (fm.findFragmentByTag("search_history") == null) {
                        title = getString(R.string.app_name)
                        colorRes = ContextCompat.getColor(this, R.color.colorPrimary)
                        val ft = fm.beginTransaction()
                        ft.replace(R.id.fragment_container, HistoryFragment())
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        ft.addToBackStack("search_history")
                        ft.commit()
                    }
                }
                R.id.action_railway -> {
                    if (fm.findFragmentByTag("railway") == null) {
                        title = getString(R.string.provider_mtr)
                        colorRes = ContextCompat.getColor(this, R.color.provider_mtr)
                        val ft = fm.beginTransaction()
                        ft.replace(R.id.fragment_container, MtrLineStatusFragment())
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        ft.addToBackStack("railway")
                        ft.commit()
                    }
                }
                else -> finish()
            }
            supportActionBar?.title = title
            supportActionBar?.subtitle = null
            if (colorRes != 0) {
                val darkenColor = ColorUtil.darkenColor(colorRes)
                supportActionBar?.setBackgroundDrawable(ColorDrawable(colorRes))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
                    window.statusBarColor = darkenColor
                    window.navigationBarColor = darkenColor
                }
                adViewContainer.setBackgroundColor(colorRes)
                adView = AdViewUtil.banner(adViewContainer, adView, false)
            }
            if (Build.VERSION.SDK_INT >= 28) {
                setTaskDescription(ActivityManager.TaskDescription(title, R.mipmap.ic_launcher,
                        ContextCompat.getColor(this, R.color.colorPrimary600)))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val bm = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                @Suppress("DEPRECATION")
                setTaskDescription(ActivityManager.TaskDescription(title, bm,
                        ContextCompat.getColor(this, R.color.colorPrimary600)))
            }
            true
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val f = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (f != null && bottomNavigationView != null) {
                when (f.javaClass.name) {
                    "FollowFragment" -> bottomNavigationView.selectedItemId = R.id.action_follow
                    "HistoryFragment" -> bottomNavigationView.selectedItemId = R.id.action_search_history
                    "MtrLineStatusFragment" -> bottomNavigationView.selectedItemId = R.id.action_railway
                }
            }
        }
        if (bottomNavigationView != null) {
            val followDatabase = FollowDatabase.getInstance(applicationContext)
            if (followDatabase != null && followDatabase.followDao().count() > 0) {
                bottomNavigationView?.selectedItemId = R.id.action_follow
            } else {
                bottomNavigationView?.selectedItemId = R.id.action_search_history
            }
        }
        try {
            startService(Intent(this, ProviderUpdateService::class.java))
        } catch (ignored: Throwable) {
        }

        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(this)
        appUpdateManager.unregisterListener(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this,
                        APP_UPDATE_REQUEST_CODE)
            }
        }
    }

    override fun onBackPressed() {
        when {
            supportFragmentManager.backStackEntryCount < 2 -> finish()
            supportFragmentManager.backStackEntryCount > 0 -> supportFragmentManager.popBackStack()
            else -> super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager
                .appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate()
                    }
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Timber.d("Update flow failed! Result code: $resultCode")
            }
        }
    }

    override fun onStateUpdate(state: InstallState) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            popupSnackbarForCompleteUpdate()
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
                findViewById(R.id.constraint_layout),
                "An update has just been downloaded.",
                Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("RESTART") { appUpdateManager.completeUpdate() }
            show()
        }
    }
}
