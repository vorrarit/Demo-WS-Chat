package com.example.demowschat

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
class ChatSocketHandler: WebSocketHandler {

    companion object {
        val log = LoggerFactory.getLogger(ChatSocketHandler::class.java)
    }

    override fun handle(session: WebSocketSession): Mono<Void> {
        val response = session.receive()
            .map { webSocketMessage ->
                log.info("Receiving ${webSocketMessage.payloadAsText}");
                session.textMessage("Hello ${webSocketMessage.payloadAsText}")
            }
        return session.send(response)
    }
}

data class GreetingRequest(val name: String)
data class GreetingResponse(val message: String)

//@Configuration
//@EnableWebSocketMessageBroker
//class WebSocketConfig: WebSocketMessageBrokerConfigurer {
//    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
//        registry.enableSimpleBroker("/topic")
//        registry.setApplicationDestinationPrefixes("/app")
//    }
//
//    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
//        registry.addEndpoint("/ws1")
//        registry.addEndpoint("/ws1").withSockJS()
//    }
//}