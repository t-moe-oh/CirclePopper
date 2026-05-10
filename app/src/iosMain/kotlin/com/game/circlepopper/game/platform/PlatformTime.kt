package com.game.circlepopper.game.platform

import platform.CoreFoundation.CFAbsoluteTimeGetCurrent

actual fun currentTimeMillis(): Long =
    ((CFAbsoluteTimeGetCurrent() + 978307200.0) * 1000.0).toLong()
