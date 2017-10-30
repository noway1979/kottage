package org.noway.kottage

import io.kotlintest.mock.mock
import mu.KotlinLogging
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.opentest4j.MultipleFailuresError

class TestResourceManagerTest {
    private val logger = KotlinLogging.logger {}


    abstract class FailingTestResource(manager: TestResourceManager) : AbstractTestResource(manager) {
        override fun setupTestMethod(context: ExtensionContext) {
            throw TestResourceException("Deliberate Exception from TestResource")
        }
    }

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

        class TestResource1(manager: TestResourceManager) : FailingTestResource(manager)
        class TestResource2(manager: TestResourceManager) : FailingTestResource(manager)

        val manager = TestResourceManager()
        val testResource1 = TestResource1(manager)
        val testResource2 = TestResource2(manager)

        manager.run({
                        registerResource(TestResource1::class.java, testResource1)
                        registerResource(TestResource2::class.java, testResource2)

                        assertThrows(MultipleFailuresError::class.java, {
                            executeOnResourcesWithFailureValidation({ it.setupTestMethod(mock<ExtensionContext>()) })
                        })
                    })
    }


    @Test
    fun testResourceLifecycleWithValidationShouldExecuteForAllResources() {
        //validation is failure tolerant, so a single exception thrown by any resource does not prevent executing same lifecycle event on other resources

        abstract class ExecutionTracedFailingTestResource(manager: TestResourceManager) : FailingTestResource(manager) {
            var hasExecuted: Boolean = false
            override fun setupTestMethod(context: ExtensionContext) {
                hasExecuted = true
                super.setupTestMethod(context)
            }
        }

        //create two concrete types to register as its key in test resource manager
        class TestResource1(manager: TestResourceManager) : ExecutionTracedFailingTestResource(manager)

        class TestResource2(manager: TestResourceManager) : ExecutionTracedFailingTestResource(manager)

        val manager = TestResourceManager()
        val testResource1 = TestResource1(manager)
        val testResource2 = TestResource2(manager)

        manager.run({
                        registerResource(TestResource1::class.java, testResource1)
                        registerResource(TestResource2::class.java, testResource2)

                        assertThrows(MultipleFailuresError::class.java, {
                            executeOnResourcesWithFailureValidation({ it.setupTestMethod(mock<ExtensionContext>()) })
                        })
                    })

        assertThat(testResource1.hasExecuted, `is`(true))
        assertThat(testResource2.hasExecuted, `is`(true))
    }
}