package dev.haomin.filesheep

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FilesheepApplication

fun main(args: Array<String>) {
	runApplication<FilesheepApplication>(*args)
}
