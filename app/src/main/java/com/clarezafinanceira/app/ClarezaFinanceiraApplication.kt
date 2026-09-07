package com.clarezafinanceira.app

import android.app.Application

class ClarezaFinanceiraApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
