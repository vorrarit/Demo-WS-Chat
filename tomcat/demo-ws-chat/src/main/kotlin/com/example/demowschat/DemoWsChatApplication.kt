package com.example.demowschat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class DemoWsChatApplication

fun main(args: Array<String>) {
	runApplication<DemoWsChatApplication>(*args)
}
