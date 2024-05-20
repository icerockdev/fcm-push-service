/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.fcmpush

data class FCMPayLoad(
    val tokenList: List<String>,
    val notificationObject: NotificationData? = null,
    val dataObject: Map<String, String>? = null,
    val priority: PushPriority = PushPriority.NORMAL
)
