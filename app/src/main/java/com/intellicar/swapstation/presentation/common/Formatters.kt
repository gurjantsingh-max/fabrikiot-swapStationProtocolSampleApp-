package com.intellicar.swapstation.presentation.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

fun Instant.toClockTime(): String = timeFormatter.format(this)
