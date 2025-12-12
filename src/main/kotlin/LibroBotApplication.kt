package org.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LibroBotApplication

fun main(args: Array<String>) {
    runApplication<LibroBotApplication>(*args)
}