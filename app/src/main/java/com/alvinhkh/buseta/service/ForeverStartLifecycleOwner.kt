package com.alvinhkh.buseta.service

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry

// https://stackoverflow.com/a/53174818
enum class ForeverStartLifecycleOwner : LifecycleOwner {
    INSTANCE;

    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    init {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}