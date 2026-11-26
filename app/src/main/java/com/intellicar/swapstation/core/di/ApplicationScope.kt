package com.intellicar.swapstation.core.di

import javax.inject.Qualifier

/** CoroutineScope that lives as long as the app process (owns the MQTT connection). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
