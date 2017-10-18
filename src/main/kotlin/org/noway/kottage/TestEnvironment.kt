package org.noway.kottage

import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.extension.ExtensionContext
import java.nio.file.Path
import java.io.IOException
import java.nio.file.Files

fun TestResourceManager.getTestEnvironment() : TestEnvironment
{
    return this.getTestResource(TestEnvironment::class.java)
}

fun Path.fileTypeLiteral() : String  {
    return when (Files.isDirectory(this))
    {
        true ->  "Directory"
        false -> "File"
    }
}

class TestEnvironment(testResourceManager: TestResourceManager) : AbstractTestResource(testResourceManager) {
    private val logger = KotlinLogging.logger {}

    val fileResources : FileResources = FileResources()

    override fun init() {
        logger.info { "TestEnvironment initialized" }
    }

    override fun setupTestInstance() {
    }

    override fun setupTestMethod() {
    }

    override fun tearDownTestMethod(context: ExtensionContext) {
    }

    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
    }

    override fun tearDownTestInstance(context: ExtensionContext) {
    }

    override fun dispose() {
        fileResources.dispose()
    }

    /**
     * Creates a temporary directory which is removed after current test method automatically.
     *
     * @return Path to temporary directory
     * @throws TestResourceException
     */
    @Throws(TestResourceException::class)
    fun createTempDir(): Path {
        try {
          val tempDirPath = Files.createTempDirectory("testDir")
            fileResources.registerManagedPath(tempDirPath)
            logger.info("Temporary directory created: $tempDirPath. This directory will be automatically deleted.")
            return tempDirPath
        } catch (e: IOException) {
            throw TestResourceException(e)
        }
    }
}

class FileResources {

    private val logger = KotlinLogging.logger {}
    val managedPaths = mutableListOf<Path>()

    fun registerManagedPath(path : Path)
    {
        managedPaths.add(path)
    }

    fun dispose()
    {
        if (!managedPaths.isEmpty())
        {
            val managedPathsOut : String = managedPaths.joinToString(",") {  path : Path ->  "$path (${path.fileTypeLiteral()})"}
            logger.info { "Removing all managed files: $managedPathsOut" }
            managedPaths.forEachWithException<Path, Unit, Exception> {if (Files.exists(it)) delete(it)}
        }
    }

    fun delete(path: Path)
    {
        FileUtils.deleteQuietly(path.toFile())
    }
}
