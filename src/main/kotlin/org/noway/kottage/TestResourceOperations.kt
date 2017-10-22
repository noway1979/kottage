package org.noway.kottage

import mu.KotlinLogging
import org.opentest4j.MultipleFailuresError
import java.util.*

//SAM Interface for Java Interoperability
//TODO use and test
interface TestResourceOperation {
    @Throws(TestResourceException::class)
    fun execute(resource: TestResource)
}

//TODO use and test
interface TestReverseResourceOperation {
    @Throws(TestResourceException::class)
    fun reverse(resource: TestResource)
}


private val logger = KotlinLogging.logger {}

open class TestResourceOperations(val manager: TestResourceManager) {

    internal val testScopedOperations = ArrayDeque<ReversibleOperation<*>>()
    internal val classScopedOperations = ArrayDeque<ReversibleOperation<*>>()

    @Throws(TestResourceException::class)
    fun <R : Any> executeReversibleOperation(description: String, forward: () -> R, reverse: (result: R) -> Unit): R {
        val operation = ReversibleStatefulTestOperation<R>(description, forward, reverse)
        return executeReversibleOperation(operation)
    }

    @Throws(TestResourceException::class)
    fun executeReversibleOperation(testResource: TestResource, description: String,
                                   forward: (resource: TestResource) -> Unit,
                                   reverse: (resource: TestResource) -> Unit) {
        val operation = ReversibleTestResourceOperation(testResource, description, forward, reverse)
        executeReversibleOperation<Unit>(operation)
    }

    @Throws(TestResourceException::class)
    fun executeReversibleOperation(description: String, forward: () -> Unit, reverse: () -> Unit) {
        val operation = ReversibleTestOperation(description, forward, reverse)
        executeReversibleOperation(operation)
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

class ReversibleStatefulTestOperation<R>(description: String, val forward: () -> R, val reverse: (R) -> Unit) :
        AbstractReversibleOperation<R>(description) {
    var result: Optional<R> = Optional.empty()

    override fun executeOp(): R {
        result = Optional.of(forward())
        logger.info("Executed operation '${description}' with result: ${result.get()}")
        return result.get()
    }

    override fun reverseOp() {
        logger.info("Reversing operation '$description' for result: $result")
        reverse(result.orElseThrow({ TestResourceException("No result of operation stored.") }))
    }
}

interface ReversibleOperation<R> {
    fun executeOp(): R
    fun reverseOp()
}

abstract class AbstractReversibleOperation<R>(val description: String) : ReversibleOperation<R> {
    override fun toString(): String {
        return description
    }
}

open class ReversibleTestOperation(description: String, val forward: () -> Unit, val reverse: () -> Unit) :
        AbstractReversibleOperation<Unit>(description) {
    override fun executeOp() {
        logger.info { "Executing operation: $description" }
        forward()
    }

    override fun reverseOp() {
        logger.info { "Executing reverse operation: $description" }
        reverse()
    }
}


open class ReversibleTestResourceOperation(val resource: TestResource, val description: String,
                                           val forward: (resource: TestResource) -> Unit,
                                           val reverse: (resource: TestResource) -> Unit) : ReversibleOperation<Unit> {
    override fun executeOp() {
        logger.info { "Executing reversable operation: $description (Resource: ${resource.displayName}) " }
        forward(resource)
    }

    override fun reverseOp() {
        logger.info { "Reverting operation: $description (${resource.displayName}) " }
        reverse(resource)
    }

    override fun toString(): String {
        return description
    }
}