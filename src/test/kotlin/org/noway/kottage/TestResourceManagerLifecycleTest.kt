package org.noway.kottage

import mu.KotlinLogging
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtensionContext

class TestResourceManagerLifecycleTest(manager: TestResourceManager) : KottageTest(manager) {
    class TestResourceLifecyclePrinter : TestResource {
        override fun init() {
            logger.info("TESTRESOURCE: Init")
        }

        override fun postProcessTestInstance(context: ExtensionContext) {
            logger.info("TESTRESOURCE: PostProcessTestInstance")
        }

        override fun setupTestInstance(context: ExtensionContext) {
            logger.info("TESTRESOURCE: setupTestInstance")
        }

        override fun setupTestMethod(context: ExtensionContext) {
            logger.info("TESTRESOURCE: setupTestMethod")
        }

        override fun tearDownTestMethod(context: ExtensionContext) {
            logger.info("TESTRESOURCE: tearDownTestMethod")
        }

        override fun tearDownTestInstance(context: ExtensionContext) {
            logger.info("TESTRESOURCE: tearDownTestInstance")
        }

        override fun dispose(context: ExtensionContext) {
            logger.info("TESTRESOURCE: dispose")
        }
    }

    companion object {

        private val logger = KotlinLogging.logger { }
        @BeforeAll
        @JvmStatic
        fun setupBeforeClass(manager: TestResourceManager) {
            logger.info("TEST: BeforeAll")
            assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.BEFORE_CLASS))
            manager.registerResource(TestResourceLifecyclePrinter::class.java, TestResourceLifecyclePrinter())
        }

        @AfterAll
        @JvmStatic
        fun tearDownAfterClass(manager: TestResourceManager) {
            logger.info("TEST: AfterAll")
            assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.AFTER_CLASS))
        }
    }

    @BeforeEach
    fun setup() {
        logger.info { "TEST: setup in test" }
        assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.BEFORE_CLASS))
    }

    @Test
    fun testLifecyclePhaseShouldBeInTestMethod() {
        logger.info("TEST: Running Test")
        assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.IN_TEST))
    }

    @AfterEach
    fun tearDown() {
        logger.info("TEST:  TearDown test method")
        assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.AFTER_TEST))
    }

}

class TestResourceOperationLifecycleTest(manager: TestResourceManager) : KottageTest(manager) {

    private var testScopedWithTestResourceExecuted = false
    private var testScopedWithoutTestResourceExecuted = false

    class ValidatingTestResource(manager: TestResourceManager) : AbstractTestResource(manager) {
        override fun tearDownTestInstance(context: ExtensionContext) {
            val instance = context.testInstance.kGet() as? TestResourceOperationLifecycleTest
            assertThat("test scoped operations are reverted after test method", instance!!.testScopedWithTestResourceExecuted, `is`(false))
            assertThat("test scoped operations are reverted after test method", instance!!.testScopedWithoutTestResourceExecuted, `is`(false))
        }

        override fun dispose(context: ExtensionContext) {
            logger.info { "TESTRESOURCE: dispose" }
            //static property should have been reset in tearDownAfter class
            logger.info { "static property state: $TestResourceOperationLifecycleTest.classScopedOperationExecuted" }
            assertThat("class-scoped operation should have been reverted", TestResourceOperationLifecycleTest.classScopedOperationExecuted, `is`(false))
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        var classScopedOperationExecuted = false

        @BeforeAll
        @JvmStatic
        fun setupBeforeClass(manager: TestResourceManager) {
            val testResource = ValidatingTestResource(manager)
            logger.info("TEST: BeforeAll")
            manager.registerResource(ValidatingTestResource::class.java, testResource)
            manager.resourceOperations.executeReversibleOperation(testResource, "class-scoped operation", { Companion.classScopedOperationExecuted = true }, { Companion.classScopedOperationExecuted = false })
            assertThat(classScopedOperationExecuted, `is`(true))
        }

        @AfterAll
        @JvmStatic
        fun tearDownAfterClass(manager: TestResourceManager) {
            logger.info("TEST: AfterAll")
            assertThat("class-scoped operations should not have been reverted in afterAll yet", classScopedOperationExecuted, `is`(true))
        }
    }

    @Test
    fun testResourceOperation() {
        val testResource = ValidatingTestResource(manager)
        manager.resourceOperations.executeReversibleOperation(testResource, "test-scoped operation with backing test resource", { testScopedWithTestResourceExecuted = true }, { testScopedWithTestResourceExecuted = false })
        manager.resourceOperations.executeReversibleOperation("test-scoped operation without backing test resource", { testScopedWithoutTestResourceExecuted = true }, { testScopedWithoutTestResourceExecuted = false })

        assertThat(testScopedWithTestResourceExecuted, `is`(true))
        assertThat(testScopedWithoutTestResourceExecuted, `is`(true))
    }


    @AfterEach
    fun afterEach() {
        logger.info("TEST: AfterEach")
        assertThat("test scoped operations effect is not visible in afterEach anymore", testScopedWithTestResourceExecuted, `is`(false))
        assertThat("test scoped operations effect is not visible in afterEach anymore", testScopedWithoutTestResourceExecuted, `is`(false))
    }
}