package com.alvinhkh.buseta.service

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

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