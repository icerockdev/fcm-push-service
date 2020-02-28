/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.fcmpush

interface IPushRepository {
    fun deleteByTokenList(tokenList: List<String>): Int
}