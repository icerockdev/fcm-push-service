/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.fcmpush

data class FCMPayLoad(
    val tokenList: List<String>,
    /**
     * @see <a href="https://firebase.google.com/docs/cloud-messaging/send-message#send-messages-to-topics">link</a>
     */
    val condition: String? = null,
    val notificationObject: NotificationData? = null,
    val dataObject: Map<String, String>? = null,
    /**
     * @see <a href="https://firebase.google.com/docs/cloud-messaging/send-message#send-messages-to-topics">link</a>
     */
    val topic: String? = null,
    val priority: PushPriority = PushPriority.NORMAL
)
