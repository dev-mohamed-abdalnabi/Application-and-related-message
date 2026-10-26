package com.mohamed.phone

import android.app.Application
import com.mohamed.phone.data.Repo
import com.mohamed.phone.ui.Photos

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Repo.init(this)
        Photos.app = applicationContext
        Notifs.createChannels(this)
        Repo.start()
    }
}
