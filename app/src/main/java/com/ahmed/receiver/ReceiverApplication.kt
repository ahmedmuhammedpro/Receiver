package com.ahmed.receiver

import android.app.Application
import timber.log.Timber

class ReceiverApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}