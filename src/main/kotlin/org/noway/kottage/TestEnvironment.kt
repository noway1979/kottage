package org.noway.kottage

import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

fun TestResourceManager.testEnvironment(): TestEnvironment {
    return this.getTestResource(TestEnvironment::class.java)
}

fun Path.fileTypeLiteral(): String {
    return when (Files.isDirectory(this)) {
        true  -> "Directory"
        false -> "File"
    }
}


class TestEnvironment(testResourceManager: TestResourceManager) : AbstractTestResource(testResourceManager) {
    fun OSUser(): String {
        return System.getProperty("user.name")
    }
}

fun TestResourceManager.fileResources(): FileResources {
    return getTestResource(FileResources::class.java)
}


