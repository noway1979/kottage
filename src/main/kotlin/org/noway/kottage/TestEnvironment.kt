package org.noway.kottage

import mu.KotlinLogging
import org.apache.commons.io.FileUtils
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

open class FileResources(manager: TestResourceManager) : AbstractTestResource(manager) {
    val managedPaths = mutableListOf<Path>()

    fun registerForDeletion(path: Path) {
        testResourceManager.resourceOperations.executeReversibleOperation(
                "Registering $path for deletion after current test unit (method/class)", {}, { delete(path) })
    }

    /**
     * Creates a temporary directory which is removed after current test method automatically.
     *
     * @return Path to temporary directory
     * @throws TestResourceException
     */
    @Throws(TestResourceException::class)
    fun createTempDir(): Path {
        return testResourceManager.resourceOperations.executeReversibleOperation(
                "Create temporary directory",
                { Files.createTempDirectory("testDir") },
                { file -> delete(file) })
    }

    fun delete(path: Path) {
        FileUtils.deleteQuietly(path.toFile())
    }
}
