/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.sample

import com.icerockdev.service.fcmpush.FCMConfig
import com.icerockdev.service.fcmpush.FCMPayLoad
import com.icerockdev.service.fcmpush.IPushRepository
import com.icerockdev.service.fcmpush.NotificationData
import com.icerockdev.service.fcmpush.PushService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

object Main {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private const val googleServiceAccountJson = "VALID_GOOGLE_SERVICE_ACCOUNT_JSON"
    private const val validClientToken = "VALID_TOKEN"

    private val notificationObject = NotificationData(
        title = "title",
        body = "some push body"
    )

    private val pushService = PushService(
        coroutineScope = scope,
        config = FCMConfig(
            googleServiceAccountJson = googleServiceAccountJson
        ),
        pushRepository = object : IPushRepository {
            override fun deleteByTokenList(tokenList: List<String>): Int {
                println(tokenList)
                return 0
            }
        }
    )

    @JvmStatic
    fun main(args: Array<String>) {
        println("Run example")
        runBlocking {
            send(listOf(1, 2, 3), notificationObject, mapOf("test" to "data"))
        }
        scope.cancel()
        println("End")
    }

    private fun getTokenByIdList(userIds: List<Int>): List<String> {
        return listOf(validClientToken, "invalid token")
    }

    private suspend fun send(userIds: List<Int>, notification: NotificationData, data: Map<String, String>? = null) {
        val tokens: List<String> = getTokenByIdList(userIds)

        if (tokens.isEmpty()) {
            return
        }

        pushService.sendAsync(
            payLoad = FCMPayLoad(
                dataObject = data,
                notificationObject = notification,
                tokenList = tokens
            )
        ).await()

        println("Push sent")
    }
}
