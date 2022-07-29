package com.example.demowschat.config

import com.example.demowschat.ChatSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping

@Configuration
class UrlMappingConfig {

    @Bean
    fun simpleUrlHandlerMapping(chatSocketHandler: ChatSocketHandler): SimpleUrlHandlerMapping {
        return SimpleUrlHandlerMapping().apply {
            urlMap = mapOf("/ws/greetings" to chatSocketHandler)
            order = 10
        }
    }
}
