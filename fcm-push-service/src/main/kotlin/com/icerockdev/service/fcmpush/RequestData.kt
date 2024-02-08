/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.fcmpush

internal data class RequestData(
    val data: Map<String, String>?,
    val registrationTokenList: List<String>?,
    val condition: String?,
    val topic: String?,
    val notification: NotificationData?,
    val priority: PushPriority
)
