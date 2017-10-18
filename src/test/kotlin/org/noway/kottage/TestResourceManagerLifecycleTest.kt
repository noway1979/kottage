package org.noway.kottage

import mu.KotlinLogging
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtensionContext

class TestResourceManagerLifecycleTest(manager: TestResourceManager) : KottageTest(manager)
{
    class TestResourceLifecyclePrinter : TestResource
    {
        override fun init() {
            logger.info("TESTRESOURCE: Init")
        }

        override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
            logger.info("TESTRESOURCE: PostProcessTestInstance")
        }

        override fun setupTestInstance() {
            logger.info("TESTRESOURCE: setupTestInstance")
        }

        override fun setupTestMethod() {
            logger.info("TESTRESOURCE: setupTestMethod")
        }

        override fun tearDownTestMethod(context: ExtensionContext) {
            logger.info("TESTRESOURCE: tearDownTestMethod")
        }

        override fun tearDownTestInstance(context: ExtensionContext) {
            logger.info("TESTRESOURCE: tearDownTestInstance")
        }

        override fun dispose() {
            logger.info("TESTRESOURCE: dispose")
        }
    }

    companion object {

        private val logger = KotlinLogging.logger {  }
        @BeforeAll @JvmStatic
        fun setupBeforeClass(manager: TestResourceManager)
        {
            logger.info("TEST: BeforeAll")
            assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.BEFORE_CLASS))
            manager.registerResource(TestResourceLifecyclePrinter::class.java, TestResourceLifecyclePrinter())
        }

        @AfterAll @JvmStatic
        fun tearDownAfterClass(manager : TestResourceManager)
        {
            logger.info("TEST: AfterAll")
            assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.AFTER_CLASS))
        }
    }

    @BeforeEach
    fun setup()
    {
        logger.info { "TEST: setup in test" }
        assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.BEFORE_CLASS))
    }

    @Test
    fun testLifecyclePhaseShouldBeInTestMethod()
    {
        logger.info("TEST: Running Test")
        assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.IN_TEST))
    }

    @AfterEach
    fun tearDown()
    {
        logger.info("TEST:  TearDown test method")
        assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.AFTER_TEST))
    }

}