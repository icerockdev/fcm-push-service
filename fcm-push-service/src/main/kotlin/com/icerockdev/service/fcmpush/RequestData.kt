/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.fcmpush

import com.fasterxml.jackson.annotation.JsonProperty

internal data class RequestData(
    val data: Map<String, String>?,
    @JsonProperty("registration_ids")
    val registrationTokenList: List<String>?,
    val condition: String?,
    val notification: NotificationData?,
    val priority: String
)