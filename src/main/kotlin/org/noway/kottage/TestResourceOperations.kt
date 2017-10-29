package org.noway.kottage

import mu.KotlinLogging
import org.opentest4j.MultipleFailuresError
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * A facility to register reversible operations. A reversible operation consists of a forward and a reverse operation.
 * The forward operation is executed immediately, returning an optional return value. The reverse operation is executed
 * automatically, depending on the current scope when the operation was registered.
 *
 * There exist two scopes:
 * <ol>
 * <li>Class-Scope: An operation is registered before a test instance is started (e.g. @BeforeAll). A class-scoped reverse operation is executed on test instance disposal, i.e. after @AfterAll phase</li>
 * <li>Test-Scope: An operation is registered when setting up or executing a test instance (e.g. @BeforeEach). a test-scoped reverse operation is executed after each test, i.e. after @AfterEach phase</li>
 *
 * Reversal is done in inverse order of registration. This ensures that implicit dependencies between operations are maintained.
 * Exceptions on reversal do not prevent further reversal executions, but are rethrown as MultipleFailureError exception.
 * This ensures that as much reversal has been executed as possible.
 */
open class TestResourceOperations(val manager: TestResourceManager) {

    internal val testScopedOperations = ArrayDeque<ReversibleOperation<*>>()
    internal val classScopedOperations = ArrayDeque<ReversibleOperation<*>>()

    /**
     *
     */
    @Throws(TestResourceException::class)
    fun <R : Any> executeReversibleOperation(
            forward: () -> R, reverse: (result: R) -> Unit, opDescription: String,
            reverseDescription: String = opDescription): R {
        val operation = ReversibleStatefulTestOperation<R>(forward, reverse, opDescription, reverseDescription)
        return executeReversibleOperation(operation)
    }

    /**
     * Register a reversible operation for a given test resource instance with no explicit return value.
     *
     * @param testResource - the originating TestResource instance
     * @param forward - the operation, must not define a return value.
     * @param reverse - operation reverse
     * @param opDescription - A readable description of the operation for logging purposes
     * @param reverseDescription - An optional readable description of the reverse operation for logging purposes
     */
    @Throws(TestResourceException::class)
    fun executeReversibleTestResourceOperation(testResource: TestResource,
                                               forward: (resource: TestResource) -> Unit,
                                               reverse: (resource: TestResource) -> Unit,
                                               opDescription: String, reverseDescription: String = opDescription) {
        val operation = ReversibleTestResourceOperation(testResource, forward, reverse, opDescription,
                                                        reverseDescription)
        executeReversibleOperation<Unit>(operation)
    }

    private fun <R : Any> executeReversibleOperation(operation: ReversibleOperation<R>): R {

        //execute operation first. If it throws exception no reverse operation is registered.
        val result: R = operation.executeOp()

        //register depending on current test state
        when (manager.currentTestPhase) {
            TestLifecyclePhased.TestLifecyclePhase.BEFORE_CLASS, TestLifecyclePhased.TestLifecyclePhase.AFTER_CLASS                                               -> registerClassScopedOperation(
                    operation)
            TestLifecyclePhased.TestLifecyclePhase.BEFORE_TEST, TestLifecyclePhased.TestLifecyclePhase.IN_TEST, TestLifecyclePhased.TestLifecyclePhase.AFTER_TEST -> registerTestScopedOperation(
                    operation)
        }

        return result
    }

    protected fun registerTestScopedOperation(operation: ReversibleOperation<*>) {
        testScopedOperations.add(operation)
    }

    protected fun registerClassScopedOperation(operation: ReversibleOperation<*>) {
        classScopedOperations.add(operation)
    }

    internal fun reverseAfterTest() {
        reverse(testScopedOperations)
    }

    internal fun reverseAfterClass() {
        reverse(classScopedOperations)
    }

    @Throws(MultipleFailuresError::class)
    protected fun reverse(operations: ArrayDeque<ReversibleOperation<*>>) {
        val reverseOrderOperations = operations.reversed()
        val validation = reverseOrderOperations.forEachWithException<ReversibleOperation<*>, Unit, TestResourceException> { it.reverseOp() }
        operations.clear()

        if (validation.hasFailure) {
            throw MultipleFailuresError("Error(s) occurred during operation reversal of: $operations",
                                        validation.failures)
        }
    }
}

/**
 * Interface for all reversible operations. This interface should not be implemented directly.
 * @see AbstractReversibleOperation<R> and concrete implementations
 *
 *     @param <R>: Return type of operation
 */
interface ReversibleOperation<out R> {
    /**
     * Execute an operation with a result
     */
    fun executeOp(): R

    /**
     * Reverse executed operation
     */
    fun reverseOp()
}

/**
 *  An abstraction for execution of reversible operations which simply logs execution.
 *
 * @param opDescription - A readable description of the operation for logging purposes
 * @param reverseDescription - An optional readable description of the reverse operation for logging purposes
 *
 */
abstract class AbstractReversibleOperation<R>(protected val opDescription: String,
                                              protected val reverseDescription: String = opDescription) :
        ReversibleOperation<R> {
    override fun executeOp(): R {
        logger.info { "Executing operation '$opDescription' ${appendExecutionLogMessage()}" }
        return execForward()
    }

    override fun reverseOp() {
        logger.info { "Reverting operation '$opDescription': $reverseDescription ${appendRevertingLogMessage()}" }
        execReverse()
    }

    protected abstract fun execForward(): R
    protected abstract fun execReverse()

    open protected fun appendExecutionLogMessage() = ""
    open protected fun appendRevertingLogMessage() = ""

    override fun toString(): String {
        return "'$opDescription' is reverted by $reverseDescription"
    }
}

/**
 * A reversible operation to be called from a @link{TestResource}. The test resource is supplied to operation execution and reversal.
 *
 * @param resource - the originating TestResource instance
 * @param forward - the operation, must not define a return value.
 * @param reverse - operation reverse
 * @param opDescription - A readable description of the operation for logging purposes
 * @param reverseDescription - An optional readable description of the reverse operation for logging purposes
 *
 */
open class ReversibleTestResourceOperation(val resource: TestResource,
                                           val forward: (resource: TestResource) -> Unit,
                                           val reverse: (resource: TestResource) -> Unit,
                                           opDescription: String,
                                           reverseDescription: String = opDescription) :
        AbstractReversibleOperation<Unit>(opDescription, reverseDescription) {
    override fun execForward() {
        forward(resource)
    }

    override fun execReverse() {
        reverse(resource)
    }

    override fun appendExecutionLogMessage() = logTestResourceDisplay()
    override fun appendRevertingLogMessage() = logTestResourceDisplay()

    private fun logTestResourceDisplay() = "(TestResource: $resource)"
}

/**
 * A stateful reversible operation. The operation's result is supplied as parameter to the reverse operation.
 *
 * @param forward - the operation with a result type of R
 * @param reverse - operation reverse
 * @param opDescription - A readable description of the operation for logging purposes
 * @param reverseDescription - An optional readable description of the reverse operation for logging purposes
 * @param <R> the result type of the operation
 */
open class ReversibleStatefulTestOperation<R>(val forward: () -> R, val reverse: (R) -> Unit,
                                              opDescription: String,
                                              reverseDescription: String = opDescription) :
        AbstractReversibleOperation<R>(opDescription, reverseDescription) {
    protected var storedResult: R? = null

    override fun execForward(): R {
        val result = forward()

        val resultLog = when (result) {
            is Unit -> "No result"
            else    -> result.toString()
        }
        logger.info("Operation result of $opDescription: ${resultLog}")
        storedResult = result
        return result
    }

    override fun execReverse() {
        reverse(storedResult ?: throw TestResourceException(
                "Unexpected null result from operation. Should have never happened."))
    }
}
