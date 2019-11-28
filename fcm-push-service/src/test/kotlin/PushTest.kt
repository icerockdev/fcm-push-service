/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.icerockdev.service.fcmpush.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PushTest {
    private val validServerKey = "VALID_SERVER_KEY"
    private val validClientToken = "VALID_TOKEN"
    private val notificationObject = NotificationData(
        title = "title",
        body = "some push body"
    )
    private val dataObject = mapOf("field1" to "data1", "field2" to "data2")

    private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
    private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"

    private val mapper = jacksonObjectMapper().apply {
        configure(SerializationFeature.INDENT_OUTPUT, true)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        dateFormat = StdDateFormat()
        registerKotlinModule()
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val fcmConfig = FCMConfig(serverKey = validServerKey)

    private val client = HttpClient(MockEngine) {
        engine {
            addHandler { request ->

                when (request.url.fullUrl) {
                    fcmConfig.apiUrl -> {
                        val responseHeaders =
                            headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

                        val authorization = request.headers[HttpHeaders.Authorization]

                        if (authorization != "key=$validServerKey") {
                            return@addHandler respond(
                                "The request's Authentication (Server-) Key contained an invalid or malformed FCM-Token (a.k.a. IID-Token).",
                                HttpStatusCode.Unauthorized
                            )
                        }

                        val textContent = request.body as TextContent
                        val data = mapper.readValue(textContent.text, RequestData::class.java)

                        if (data.data === null || data.data.isEmpty()) {
                            return@addHandler respond(
                                "Empty data!",
                                HttpStatusCode.BadRequest
                            )
                        }

                        if (data.registrationTokenList === null || data.registrationTokenList.isEmpty()) {
                            return@addHandler respond(
                                "Empty token list!",
                                HttpStatusCode.BadRequest
                            )
                        }

                        var success = 0;
                        var failure = 0;
                        val messageList = ArrayList<FCMMessage>()
                        for (token in data.registrationTokenList) {
                            if (token == validClientToken) {
                                success++
                                messageList.add(FCMMessage(messageId = "rand message", error = null))
                                continue
                            }

                            failure++
                            messageList.add(FCMMessage(messageId = null, error = "InvalidRegistration"))
                        }

                        val response = FCMResponse(
                            multicastId = 0L,
                            success = success,
                            failure = failure,
                            canonicalIdList = 0,
                            results = messageList
                        )

                        return@addHandler respond(mapper.writeValueAsString(response), headers = responseHeaders)
                    }
                    else -> error("Unhandled ${request.url.fullUrl}")
                }
            }
        }
    }

    private val pushService = PushService(
        coroutineScope = scope,
        config = fcmConfig,
        client = client
    )

    @Test
    fun testValidSend() {
        val result = runBlocking {
            return@runBlocking pushService.sendAsync(
                payLoad = FCMPayLoad(
                    notificationObject = notificationObject,
                    dataObject = dataObject,
                    tokenList = listOf(validClientToken)
                )
            ).await()
        }

        assertNotNull(result)
        assertEquals(1, result.success)
        assertEquals(0, result.failure)
    }

    @Test
    fun testValidListSend() {
        val result = runBlocking {
            return@runBlocking pushService.sendAsync(
                payLoad = FCMPayLoad(
                    notificationObject = notificationObject,
                    dataObject = dataObject,
                    tokenList = listOf(validClientToken, "INVALID_DEVICE_ID", "INVALID_DEVICE_ID2")
                )
            ).await()
        }

        assertNotNull(result)
        assertEquals(1, result.success)
        assertEquals(2, result.failure)
        assertEquals(3, result.results.size)
        assertEquals(2, result.invalidTokenList.size)
    }

    @Test
    fun testInValidSend() {
        val result = runBlocking {
            return@runBlocking pushService.sendAsync(
                payLoad = FCMPayLoad(
                    notificationObject = notificationObject,
                    dataObject = dataObject,
                    tokenList = listOf("INVALID_DEVICE_ID")
                )
            ).await()
        }

        assertNotNull(result)
        assertEquals(0, result.success)
        assertEquals(1, result.failure)
    }
}