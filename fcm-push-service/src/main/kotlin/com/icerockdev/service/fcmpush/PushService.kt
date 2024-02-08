/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.fcmpush

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidConfig.Priority
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.SendResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.slf4j.LoggerFactory

class PushService(
    private val coroutineScope: CoroutineScope,
    private val pushRepository: IPushRepository,
    config: FCMConfig
) {
    private val firebaseMessaging: FirebaseMessaging

    init {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(config.googleServiceAccountJson.byteInputStream()))
            .build()
        firebaseMessaging = FirebaseMessaging.getInstance(FirebaseApp.initializeApp(options))
    }

    fun sendAsync(payLoad: FCMPayLoad): Deferred<PushResult> {
        if (payLoad.tokenList.isEmpty()) {
            throw PushException("Unsupported empty token list")
        }

        val chunkedTokenList = payLoad.tokenList.chunked(FCM_TOKEN_CHUNK)

        return coroutineScope.async {
            var success = 0
            var failure = 0

            chunkedTokenList.forEach { tokenList ->
                val requestData = prepareRequestBody(payLoad, tokenList)
                val response = if (requestData.condition != null || requestData.topic != null) {
                    send(requestData)
                } else {
                    sendChunk(requestData)
                }

                if (response == null) {
                    failure += tokenList.size
                    return@forEach
                }
                success += response.successCount
                failure += response.failureCount
            }
            return@async PushResult(
                success = success,
                failure = failure
            )
        }
    }

    private fun send(payloadObject: RequestData): BatchResponse? {
        return try {
            val token = payloadObject.registrationTokenList?.firstOrNull()

            val msg = Message.builder()
                .setToken(token)
                .setCondition(payloadObject.condition)
                .setTopic(payloadObject.topic)
                .setNotification(getNotification(payloadObject))
                .setAndroidConfig(getAndroidConfig(payloadObject))
                .setApnsConfig(getApnsConfig(payloadObject))
                .putAllData(payloadObject.data ?: emptyMap())
                .build()

            val messageId = firebaseMessaging.send(msg)
            if (messageId == null && token != null) { // wrong token
                pushRepository.deleteByTokenList(listOf(token))
            }

            return object : BatchResponse {
                override fun getResponses(): MutableList<SendResponse> {
                    return mutableListOf()
                }

                override fun getSuccessCount(): Int {
                    return messageId?.let { 1 } ?: 0
                }

                override fun getFailureCount(): Int {
                    return messageId?.let { 0 } ?: 1
                }
            }
        } catch (t: Throwable) {
            logger.error(t.localizedMessage, t)
            null
        }
    }

    private fun sendChunk(payloadObject: RequestData): BatchResponse? {
        return try {
            val msg = MulticastMessage.builder()
                .addAllTokens(payloadObject.registrationTokenList)
                .setNotification(getNotification(payloadObject))
                .setAndroidConfig(getAndroidConfig(payloadObject))
                .setApnsConfig(getApnsConfig(payloadObject))
                .putAllData(payloadObject.data ?: emptyMap())
                .build()

            val response = firebaseMessaging.sendEachForMulticast(msg)

            if (response.failureCount > 0) { // has wrong tokens
                val invalidTokenList = ArrayList<String>()
                val tokenList = payloadObject.registrationTokenList!!
                response.responses.forEachIndexed { index, message ->
                    if (message.isSuccessful) {
                        return@forEachIndexed
                    }
                    if (tokenList.size < index) {
                        return@forEachIndexed
                    }
                    invalidTokenList.add(tokenList[index])
                }
                pushRepository.deleteByTokenList(invalidTokenList)
            }
            response
        } catch (t: Throwable) {
            logger.error(t.localizedMessage, t)
            null
        }
    }

    private fun getNotification(payloadObject: RequestData): Notification {
        return Notification.builder()
            .setTitle(payloadObject.notification?.title)
            .setBody(payloadObject.notification?.body)
            .setImage(payloadObject.notification?.icon)
            .build()
    }

    private fun getAndroidConfig(payloadObject: RequestData): AndroidConfig {
        return AndroidConfig.builder()
            .setNotification(
                AndroidNotification.builder()
                    .setTitle(payloadObject.notification?.title)
                    .setBody(payloadObject.notification?.body)
                    .setClickAction(payloadObject.notification?.clickAction)
                    .setColor(payloadObject.notification?.color)
                    .setImage(payloadObject.notification?.icon)
                    .setSound(payloadObject.notification?.sound)
                    .setTag(payloadObject.notification?.tag)
                    .build()
            )
            .setPriority(Priority.valueOf(payloadObject.priority.value))
            .build()
    }

    private fun getApnsConfig(payloadObject: RequestData): ApnsConfig {
        return ApnsConfig.builder()
            .setAps(
                Aps.builder()
                    .setSound(payloadObject.notification?.sound)
                    .setBadge(payloadObject.notification?.badge?.toInt() ?: 0)
                    .setCategory(payloadObject.notification?.clickAction)
                    .build()
            )
            .build()
    }

    private fun prepareRequestBody(payLoad: FCMPayLoad, tokenList: List<String>): RequestData {
        return RequestData(
            data = payLoad.dataObject,
            registrationTokenList = tokenList,
            condition = payLoad.condition,
            topic = payLoad.topic,
            notification = payLoad.notificationObject,
            priority = payLoad.priority
        )
    }

    private companion object {
        val logger: org.slf4j.Logger = LoggerFactory.getLogger(PushService::class.java)
        private const val FCM_TOKEN_CHUNK = 1000
    }
}
