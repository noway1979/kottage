package org.noway.kottage

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class FileResourcesTest(manager: TestResourceManager) : KottageTest(manager) {

    companion object {
        val temporaryFiles = mutableListOf<Path>()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            temporaryFiles.forEach({ assertThat("$it should not exist", Files.exists(it), `is`(false)) })
        }
    }

    @Test
    fun testManagedDirectoryShouldBeDeletedOnDispose() {
        //given
        val fileResources = FileResources(manager)
        val directory = Files.createTempDirectory("test")

        //when
        fileResources.registerForDeletion(directory)
        temporaryFiles.add(directory)

        //then is deleted
    }


    @Test
    fun testManagedFileShouldBeDeletedOnDispose() {
        //given
        val fileResources = FileResources(manager)
        val file = fileResources.createTempDir()

        //when
        fileResources.registerForDeletion(file)
        temporaryFiles.add(file)
    }


    @Test
    fun testManagedDirectoryWithContentShouldBeDeletedOnDispose() {
        //given
        val fileResources = FileResources(manager)
        val directory = Files.createTempDirectory("test")
        val createdFile = Files.createFile(directory.resolve("testfile"))

        assertThat(Files.exists(createdFile), `is`(true))

        //when
        fileResources.registerForDeletion(directory)
        temporaryFiles.addAll(listOf(directory, createdFile))
    }
}