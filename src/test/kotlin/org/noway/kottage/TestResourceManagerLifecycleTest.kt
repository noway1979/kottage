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
        logger.info { "TEST: beforeEach" }
        assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.BEFORE_TEST))
    }

    @Test
    fun testLifecyclePhaseShouldBeInTestMethod() {
        logger.info("TEST: Running Test")
        assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.IN_TEST))
    }

    @Test
    fun testLifecyclePhaseShouldBeInTestMethodAgain() {
        logger.info("TEST: Running Test")
        assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.IN_TEST))
    }

    @AfterEach
    fun tearDown() {
        logger.info("TEST:  afterEach")
        assertThat(manager.currentTestPhase, `is`(TestLifecyclePhased.TestLifecyclePhase.AFTER_TEST))
    }

}

class TestResourceOperationLifecycleTest(manager: TestResourceManager) : KottageTest(manager) {

    private var testScopedWithTestResourceExecuted = false
    private var testScopedWithoutTestResourceExecuted = false

    class ValidatingTestResource(manager: TestResourceManager) : AbstractTestResource(manager) {
        override fun tearDownTestInstance(context: ExtensionContext) {
            val instance = context.testInstance.kGet() as? TestResourceOperationLifecycleTest
            assertThat("test scoped operations are reverted after test method",
                       instance!!.testScopedWithTestResourceExecuted, `is`(false))
            assertThat("test scoped operations with test resource are reverted after test method",
                       instance.testScopedWithoutTestResourceExecuted, `is`(false))
        }

        override fun dispose(context: ExtensionContext) {
            logger.info { "TESTRESOURCE: dispose" }
            //static property should have been reset in tearDownAfter class
            logger.info { "static property state: $TestResourceOperationLifecycleTest.classScopedOperationExecuted" }
            assertThat("class-scoped operation should have been reverted",
                       TestResourceOperationLifecycleTest.classScopedOperationExecuted, `is`(false))
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
            manager.resourceOperations.executeReversibleTestResourceOperation(testResource,
                                                                              { Companion.classScopedOperationExecuted = true },
                                                                              { Companion.classScopedOperationExecuted = false },
                                                                              "class-scoped operation")
            assertThat(classScopedOperationExecuted, `is`(true))
        }

        @AfterAll
        @JvmStatic
        fun tearDownAfterClass() {
            logger.info("TEST: AfterAll")
            assertThat("class-scoped operations should not have been reverted in afterAll yet",
                       classScopedOperationExecuted, `is`(true))
        }
    }

    @Test
    fun testResourceOperationShouldRegisterInTestScope() {
        assertThat("No test-scoped operations should be pending at test start",
                   manager.resourceOperations.testScopedOperations.isEmpty(), `is`(true))
        val testResource = ValidatingTestResource(manager)
        manager.resourceOperations.executeReversibleTestResourceOperation(testResource,
                                                                          { testScopedWithTestResourceExecuted = true },
                                                                          { testScopedWithTestResourceExecuted = false },
                                                                          "test-scoped operation with backing test resource")
        manager.resourceOperations.executeReversibleOperation({ testScopedWithoutTestResourceExecuted = true },
                                                              { testScopedWithoutTestResourceExecuted = false },
                                                              "test-scoped operation without backing test resource")

        assertThat(testScopedWithTestResourceExecuted, `is`(true))
        assertThat(testScopedWithoutTestResourceExecuted, `is`(true))
    }

    @Test
    fun testSecondResourceOperationShouldBeRevertedInTestScope() {
        assertThat("No test-scoped operations should be pending at test start",
                   manager.resourceOperations.testScopedOperations.isEmpty(), `is`(true))

        manager.resourceOperations.executeReversibleOperation({ testScopedWithTestResourceExecuted = true },
                                                              { testScopedWithTestResourceExecuted = false },
                                                              "test-scoped operation with backing test resource")

        manager.resourceOperations.executeReversibleOperation({ testScopedWithoutTestResourceExecuted = true },
                                                              { testScopedWithoutTestResourceExecuted = false },
                                                              "test-scoped operation without backing test resource")
    }


    @AfterEach
    fun afterEach() {
        logger.info("TEST: AfterEach")
        assertThat("test scoped operations effect is still visible in afterEach",
                   testScopedWithTestResourceExecuted, `is`(true))
        assertThat("test scoped operations effect is still visible in afterEach",
                   testScopedWithoutTestResourceExecuted, `is`(true))
    }
}