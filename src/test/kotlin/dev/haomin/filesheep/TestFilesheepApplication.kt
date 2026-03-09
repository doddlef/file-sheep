package dev.haomin.filesheep

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<FilesheepApplication>().with(TestcontainersConfiguration::class).run(*args)
}
