package com.example.demowschat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux

@SpringBootApplication
class DemoWsChatApplication

fun main(args: Array<String>) {
	runApplication<DemoWsChatApplication>(*args)
}
