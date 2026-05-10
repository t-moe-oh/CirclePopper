package com.game.circlepopper.game.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate

@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
