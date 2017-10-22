package org.noway.kottage

import io.kotlintest.mock.mock
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import java.nio.file.Files

class TestEnvironmentTest {

    @Test
    fun testManagedDirectoryShouldBeDeletedOnDispose()
    {
        //given
        val environment = TestEnvironment(mock<TestResourceManager>())
        val directory = Files.createTempDirectory("test")
        environment.fileResources.registerManagedPath(directory)

        //when
        environment.dispose(mock<ExtensionContext>())

        //then
        assertThat(Files.exists(directory), `is`(false))
    }


    @Test
    fun testManagedFileShouldBeDeletedOnDispose()
    {
        //given
        val environment = TestEnvironment(mock<TestResourceManager>())
        val file = Files.createTempFile("test", "")
        environment.fileResources.registerManagedPath(file)

        //when
        environment.dispose(mock<ExtensionContext>())

        //then
        assertThat(Files.exists(file), `is`(false))
    }


    @Test
    fun testManagedDirectoryWithContentShouldBeDeletedOnDispose()
    {
        //given
        val environment = TestEnvironment(mock<TestResourceManager>())
        val directory= Files.createTempDirectory("test")
        val createdFile = Files.createFile(directory.resolve("testfile"))

        assertThat(Files.exists(createdFile), `is`(true) )

        environment.fileResources.registerManagedPath(directory)

        //when
        environment.dispose(mock<ExtensionContext>())

        //then
        assertThat(Files.exists(directory), `is`(false))
        assertThat(Files.exists(createdFile), `is`(false) )
    }

    @Test
    fun testHamcrestCore()
    {

    }

}