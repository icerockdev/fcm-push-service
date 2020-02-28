/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.fcmpush

import com.fasterxml.jackson.annotation.JsonProperty

internal data class FCMMessage(
    @JsonProperty("message_id")
    val messageId: String?,
    val error: String?
)
