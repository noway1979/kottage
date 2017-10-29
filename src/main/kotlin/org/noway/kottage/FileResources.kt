package org.noway.kottage

import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.Path

/**
 *  Provides an API abstraction to manage local filesystem resources.
 */
open class FileResources(manager: TestResourceManager) : AbstractTestResource(manager) {

    fun registerForDeletion(path: Path) {
        testResourceManager.resourceOperations.executeReversibleOperation({}, { delete(path) },
                                                                          "Register auto-delete: $path",
                                                                          "Deleting: $path")
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
                { Files.createTempDirectory("testDir") },
                { file -> delete(file) }, "Create temporary directory", "Delete temporary directory")
    }

    protected fun delete(path: Path) {
        FileUtils.deleteQuietly(path.toFile())
    }
}