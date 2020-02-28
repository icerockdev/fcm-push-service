/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.service.fcmpush

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.DefaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.slf4j.LoggerFactory

class PushService(
    private val coroutineScope: CoroutineScope,
    private val pushRepository: IPushRepository,
    private val config: FCMConfig,
    private val logLevel: LogLevel = LogLevel.INFO,
    private var client: HttpClient = HttpClient(Apache)
) : AutoCloseable {
    init {
        client = client.config {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    setSerializationInclusion(JsonInclude.Include.NON_NULL);
                }
            }
            install(DefaultRequest) {
                headers.append("Authorization", "key=${config.serverKey}")
            }
            install(Logging) {
                logger = Logger.DEFAULT
                this.level = logLevel
            }
        }
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
                val response = sendChunk(requestData)
                if (response == null) {
                    failure += tokenList.size
                    return@forEach
                }
                success += response.success
                failure += response.failure
            }
            return@async PushResult(
                success = success,
                failure = failure
            )

        }
    }

    private suspend fun sendChunk(payloadObject: RequestData): FCMResponse? {
        return try {
            val response = client.post<FCMResponse>(config.apiUrl) {
                contentType(ContentType.Application.Json)
                body = payloadObject
            }
            if (response.failure > 0) { // has wrong tokens
                val invalidTokenList = ArrayList<String>()
                val tokenList = payloadObject.registrationTokenList!!
                response.results.forEachIndexed { index, message ->
                    if (message.error === null) {
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
            logger.error(t.localizedMessage)
            null
        }
    }

    private fun prepareRequestBody(payLoad: FCMPayLoad, tokenList: List<String>): RequestData {
        return RequestData(
            data = payLoad.dataObject,
            registrationTokenList = tokenList,
            condition = payLoad.condition,
            notification = payLoad.notificationObject,
            priority = payLoad.priority.value
        )
    }

    private companion object {
        val logger: org.slf4j.Logger = LoggerFactory.getLogger(PushService::class.java)
        private const val FCM_TOKEN_CHUNK = 1000
    }

    override fun close() {
        client.close()
    }
}