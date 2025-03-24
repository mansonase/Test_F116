package com.example.testf116

import android.app.Application
import android.content.Context
import android.os.Environment
import io.realm.Realm
import io.realm.RealmConfiguration
import java.io.File

class F116Application:Application() {

    override fun onCreate() {
        super.onCreate()
        setupRealm()
    }

    private fun setupRealm(){
        Realm.init(this)
        val mRealmConfiguration=RealmConfiguration.Builder()
            .name("F116DB")
            .schemaVersion(1)
            .directory(File(getDiskCacheDir(this)+"/f116_db"))
            .migration(MigrationHelper())
            .build()

        Realm.setDefaultConfiguration(mRealmConfiguration)

    }

    private fun getDiskCacheDir(context: Context):String{

        return if (Environment.MEDIA_MOUNTED==Environment.getExternalStorageState()
            ||!Environment.isExternalStorageRemovable()){
            context.externalCacheDir!!.path
        }else{
            context.cacheDir.path
        }
    }
}