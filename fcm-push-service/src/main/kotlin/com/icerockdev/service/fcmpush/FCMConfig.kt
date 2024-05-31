/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.fcmpush

data class FCMConfig(
    val googleServiceAccountJson: String,
    val lateInit: Boolean = false
)
