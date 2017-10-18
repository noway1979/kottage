package org.noway.kottage

import mu.KotlinLogging
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.opentest4j.MultipleFailuresError

class TestResourceManagerTest() {
    private val logger = KotlinLogging.logger {}


    @Test
    fun testTestResourceCanBeRegistered() {
        val manager = TestResourceManager()
        val testResource: TestResource = object : AbstractTestResource(manager) {}

        manager.registerResource(TestResource::class.java, testResource)

        logger.info(manager.printResources())
    }


    @Test
    fun testMultipleTestResourceShouldBeAbleToRegisterWithDifferentKeys() {
        class TestResource1(manager: TestResourceManager) : AbstractTestResource(manager)
        class TestResource2(manager: TestResourceManager) : AbstractTestResource(manager)

        val manager = TestResourceManager()

        val testResource1 = TestResource1(manager)
        val testResource2 = TestResource2(manager)

        with(manager) {
            registerResource(TestResource1::class.java, testResource1)
            registerResource(TestResource2::class.java, testResource2)

            logger.info(printResources())
            assertThat(getTestResource(TestResource1::class.java), `is`(testResource1))
            assertThat(getTestResource(TestResource2::class.java), `is`(testResource2))
        }
    }


    @Test
    fun testRegisterMultipleTestResourcesOnSameKeyShouldOverwrite() {
        class TestResource(manager: TestResourceManager) : AbstractTestResource(manager)


        val manager = TestResourceManager()
        val testResource1 = TestResource(manager)
        val testResource2 = TestResource(manager)

        with(manager) {
            registerResource(TestResource::class.java, testResource1)
            registerResource(TestResource::class.java, testResource2)
            logger.info(printResources())
            assertThat(getTestResource(TestResource::class.java), `is`(testResource2))
        }
    }

    @Test
    fun testResourceLifecycleWithValidationShouldThrowMultipleExceptions() {
        abstract class FailingTestResource(manager: TestResourceManager) : AbstractTestResource(manager) {
            override fun setupTestMethod() {
                throw TestResourceException("Deliberate Exception from TestResource")
            }
        }

        class TestResource1(manager: TestResourceManager) : FailingTestResource(manager)
        class TestResource2(manager: TestResourceManager) : FailingTestResource(manager)

        val manager = TestResourceManager()
        val testResource1 = TestResource1(manager)
        val testResource2 = TestResource2(manager)

        manager.run({
            registerResource(TestResource1::class.java, testResource1)
            registerResource(TestResource2::class.java, testResource2)

            assertThrows(MultipleFailuresError::class.java, { executeOnResourcesWithFailureValidation({ it.setupTestMethod() }) })
        })
    }


    @Test
    fun testResourceLifecycleWithValidationShouldExecuteForAllResources() {

    }
}