/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.fcmpush

import com.fasterxml.jackson.annotation.JsonProperty

internal data class FCMResponse(
    @JsonProperty("multicast_id")
    val multicastId: Long,
    val success: Int,
    val failure: Int,
    @JsonProperty("canonical_ids")
    val canonicalIdList: Int,
    val results: List<FCMMessage>
)