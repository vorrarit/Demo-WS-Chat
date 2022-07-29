package com.example.demowschat

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.core.MessageSendingOperations
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.util.MultiValueMap
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.server.HandshakeInterceptor
import java.lang.Exception
import java.security.Principal
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.ArrayList

@Component
class ChatSocketHandler: TextWebSocketHandler() {
    companion object {
        val log = LoggerFactory.getLogger(ChatSocketHandler::class.java)
    }

    private val sessions = CopyOnWriteArrayList<WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions.add(session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        session.sendMessage(TextMessage("Hello " + session.attributes["suffix"] + " " + message.payload))
    }

    @Scheduled(initialDelay = 0, fixedDelay = 10000)
    fun sendPeriodicMessages() {
        sessions.forEach { session ->
            if (session.isOpen) {
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                val suffix = session.attributes["suffix"]
                val message = "Hello $suffix from WebSocket at $date"
                session.sendMessage(TextMessage(message))
            }
        }
    }
}

@Configuration
@EnableWebSocket
class WebSocketConfig: WebSocketConfigurer {
    @Autowired
    private lateinit var chatSocketHandler: ChatSocketHandler

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(chatSocketHandler, "/ws/greetings")
            .addInterceptors(PathSuffixInterceptor())
            .setAllowedOrigins("*")
        registry.addHandler(chatSocketHandler, "/sockjs/greetings")
//            .setAllowedOrigins("https://frontend.local")
            .setAllowedOriginPatterns("*")
            .addInterceptors(PathSuffixInterceptor())
            .withSockJS()
    }
}

class PathSuffixInterceptor: HandshakeInterceptor {
    companion object {
        val log = LoggerFactory.getLogger(PathSuffixInterceptor::class.java)
    }

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
//        log.info(getHeaders(request))
        attributes["suffix"] = getQueryString(request, "suffix")
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
//        TODO("Not yet implemented")
    }

    private fun getQueryString(request: ServerHttpRequest, queryStringKey: String): String {
        val query = request.uri.query ?: return ""
        val indexOfKey = query.indexOf("$queryStringKey=")

        if (indexOfKey < 0) return ""

        val suffixKey = query.substring(indexOfKey + queryStringKey.length+1)
        val suffix = if (suffixKey.indexOf("&") >= 0) {
            suffixKey.substring(0, suffixKey.indexOf("&"))
        } else {
            suffixKey
        }
        return suffix
    }

    private fun getHeaders(request: ServerHttpRequest): String {
        val sb = StringBuilder(
            """
                {
                    url: "${request.uri.path}",
                    headers: {
            """.trimIndent()
        )

        request.headers.forEach {
            sb.append("        ${it.key}: \"${it.value.joinToString(", ")}\",\n")
        }
        sb.append(
            """
                    }
                }
            """.trimIndent()
        )
        return sb.toString()
    }
}

@Controller
class StompController {

    companion object {
        val log = LoggerFactory.getLogger(StompController::class.java)
    }

    @MessageMapping("/stomp/greetings")
    fun greetings(accessor: SimpMessageHeaderAccessor, message: String): String {
        log.info("## ${accessor.user?.name} ##")
        return "Hello ${message}"
    }

    @Autowired
    private lateinit var messageSendingOperations: MessageSendingOperations<String>

    @Scheduled(initialDelay = 0, fixedDelay = 10000)
    fun sendPeriodicMessages() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val message = "Hello from Stomp at $date"
        this.messageSendingOperations.convertAndSend("/topic/stomp/periodic", message)
    }

    @Autowired
    private lateinit var simpMessagingTemplate: SimpMessagingTemplate

    @Autowired
    private lateinit var userList: UserList

    @Scheduled(initialDelay = 0, fixedDelay = 10000)
    fun sendPeriodicUserMessages() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        userList.list.forEach {
            val message = "Hello $it from Stomp at $date"
            this.simpMessagingTemplate.convertAndSendToUser(it, "/queue/message", message)
        }
    }
}

@Configuration
@EnableWebSocketMessageBroker
class WebSocketMessageConfig: WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/stomp/connect")
        registry.addEndpoint("/stomp/connect")
            .setAllowedOriginPatterns("*")
            .addInterceptors(PathSuffixInterceptor()).withSockJS()
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/queue", "/topic")
        registry.setApplicationDestinationPrefixes("/app")
        registry.setUserDestinationPrefix("/user")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(getUserInterceptor())
    }

    @Bean
    fun getUserInterceptor() = UserInterceptor()
}

class UserInterceptor: ChannelInterceptor {
    companion object {
        val log = LoggerFactory.getLogger(UserInterceptor::class.java)
    }

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)?.let { accessor ->
            log.info("** ${accessor.command} **")
            if (StompCommand.CONNECT == accessor.command) {
                val raw = message.headers[SimpMessageHeaderAccessor.NATIVE_HEADERS]
                if (raw is Map<*, *>) {
                    val name = raw["username"]
                    if (name is ArrayList<*>) {
                        log.info("## $name ##")
                        accessor.user = Principal { name[0].toString() }
                    }
                }
            }

        }

        return message
    }

}

@Component
class UserList {
    val list = mutableListOf<String>()
}

@Component
class StompEventListener: ApplicationListener<SessionConnectEvent> {
    companion object {
        val log = LoggerFactory.getLogger(StompEventListener::class.java)
    }

    @Autowired
    private lateinit var userList: UserList


    override fun onApplicationEvent(event: SessionConnectEvent) {
        val sha = StompHeaderAccessor.wrap(event.message)
        val username = event.user?.name
        log.info("*# app event ${username} ${sha.command}")

        username?.let {
            userList.list.add(it)
        }
    }

    @org.springframework.context.event.EventListener
    fun onSessionConnected(event: SessionConnectedEvent) {
        val sha = StompHeaderAccessor.wrap(event.message)
        val username = event.user?.name
        log.info("*# connected event ${username} ${sha.command}")

    }

    @org.springframework.context.event.EventListener
    fun onSessionDisconnected(event: SessionDisconnectEvent) {
        val sha = StompHeaderAccessor.wrap(event.message)
        val username = event.user?.name
        log.info("*# disconnected event ${username} ${sha.command}")
        username?.let { username ->
            userList.list.removeIf {
                it == username
            }
        }
    }

}