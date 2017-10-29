package org.noway.kottage

import com.github.kittinunf.result.Validation
import mu.KotlinLogging
import org.junit.jupiter.api.extension.*
import org.opentest4j.MultipleFailuresError
import org.slf4j.LoggerFactory
import java.lang.reflect.Parameter

class TestResourceException(message: String, cause: Throwable?) : Exception(message, cause) {
    constructor(cause: Throwable) : this("", cause)
    constructor(message: String) : this(message, null)
}


class TestFrameworkExtension : Extension, TestInstancePostProcessor, BeforeAllCallback, BeforeEachCallback,
                               BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterEachCallback,
                               AfterAllCallback, TestExecutionExceptionHandler, ParameterResolver {

    companion object {
        private val logger = LoggerFactory.getLogger(TestFrameworkExtension::class.java)
    }

    override fun resolveParameter(p0: ParameterContext?, p1: ExtensionContext?): Any {
        return when (p0!!.parameter.type) {
            TestResourceManager::class.java -> getTestResourceManager(p1!!)
            else                            -> throw TestResourceException("Could not resolve a parameter: $p0")
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext?, p1: ExtensionContext?): Boolean {
        val parameter: Parameter? = parameterContext?.parameter
        return when (parameter?.type) {
            TestResourceManager::class.java -> true
            else                            -> false
        }
    }


    @Throws(Exception::class)
    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        getTestResourceManager(context).postProcessTestInstance(context)
    }

    @Throws(TestResourceException::class)
    private fun initializeTestResourceManager(context: ExtensionContext): TestResourceManager {
        //deliberately put init out of constructor to have a dependency-free TestResourceManager constructor
        val testResourceManager = TestResourceManager()
        testResourceManager.init()
        storeTestResourceManager(context, testResourceManager)
        return testResourceManager
    }

    private fun getTestResourceManager(context: ExtensionContext): TestResourceManager {
        return context.getStore(ExtensionContext.Namespace.GLOBAL).get(
                TestResourceManager::class.java) as TestResourceManager
    }

    private fun storeTestResourceManager(context: ExtensionContext, testResourceManager: TestResourceManager) {
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(TestResourceManager::class.java, testResourceManager)
    }

    @Throws(Exception::class)
    override fun beforeAll(context: ExtensionContext) {
        initializeTestResourceManager(context)
    }

    @Throws(Exception::class)
    override fun beforeEach(context: ExtensionContext) {
        getTestResourceManager(context).setupTestInstance(context)
    }

    @Throws(Exception::class)
    override fun beforeTestExecution(context: ExtensionContext) {
        getTestResourceManager(context).setupTestMethod(context)
    }

    @Throws(Exception::class)
    override fun afterTestExecution(context: ExtensionContext) {
        getTestResourceManager(context).tearDownTestMethod(context)
    }

    @Throws(Exception::class)
    override fun afterEach(context: ExtensionContext) {
        getTestResourceManager(context).tearDownTestInstance(context)
    }

    @Throws(Exception::class)
    override fun afterAll(context: ExtensionContext) {
        getTestResourceManager(context).dispose(context)
    }

    @Throws(Throwable::class)
    override fun handleTestExecutionException(context: ExtensionContext, throwable: Throwable) {
        getTestResourceManager(context).handleTestException(context, throwable)
    }
}


open class TestResourceManager : TestResource, TestLifecyclePhased {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var currentTestPhase = TestLifecyclePhased.TestLifecyclePhase.BEFORE_CLASS

    init {
        transitionTo(TestLifecyclePhased.TestLifecyclePhase.BEFORE_CLASS)
    }

    //TODO make this an explicit object ResourceRegistration and move register-methods to that object
    private val resources: MutableMap<Class<TestResource>, TestResource> = mutableMapOf()
    val resourceOperations = TestResourceOperations(this)

    override final fun transitionTo(phase: TestLifecyclePhased.TestLifecyclePhase) {
        logger.debug { "Transitioning to: $phase" }
        currentTestPhase = phase
    }

    internal fun executeOnResourcesWithFailureValidation(func: (TestResource) -> Unit) {
        val validation: Validation<TestResourceException> = resources.values.forEachWithException(func)
        if (validation.hasFailure) {
            throw MultipleFailuresError("Failures during resource tear down", validation.failures)
        }
    }

    override fun init() {
        registerResource(TestEnvironment::class.java, TestEnvironment(this))
        registerResource(FileResources::class.java, FileResources(this))
    }

    @Throws(MultipleFailuresError::class)
    override fun postProcessTestInstance(context: ExtensionContext) {
        transitionTo(TestLifecyclePhased.TestLifecyclePhase.BEFORE_TEST)
        //executed before setupTestInstance
        executeOnResourcesWithFailureValidation({ it.postProcessTestInstance(context) })
    }

    @Throws(MultipleFailuresError::class)
    override fun setupTestInstance(context: ExtensionContext) {
        executeOnResourcesWithFailureValidation({ it.setupTestInstance(context) })
    }

    @Throws(MultipleFailuresError::class)
    override fun setupTestMethod(context: ExtensionContext) {
        transitionTo(TestLifecyclePhased.TestLifecyclePhase.BEFORE_TEST)

        //setup resources for test
        executeOnResourcesWithFailureValidation({ it.setupTestMethod(context) })

        //must be last statement in this method
        transitionTo(TestLifecyclePhased.TestLifecyclePhase.IN_TEST)
    }

    @Throws(MultipleFailuresError::class)
    override fun tearDownTestMethod(context: ExtensionContext) {
        transitionTo(TestLifecyclePhased.TestLifecyclePhase.AFTER_TEST)
        executeOnResourcesWithFailureValidation({ it.tearDownTestMethod(context) })
        //afterEach methods come here
    }

    @Throws(MultipleFailuresError::class)
    override fun tearDownTestInstance(context: ExtensionContext) {
        resourceOperations.reverseAfterTest()
        executeOnResourcesWithFailureValidation({ it.tearDownTestInstance(context) })

        //after each test method transition prematurely to AFTER_CLASS because there is no other hook point before @AfterAll after last test method.
        // In case another test follows the phase is immediately transitioned back to BEFORE_TEST.
        transitionTo(TestLifecyclePhased.TestLifecyclePhase.AFTER_CLASS)
    }

    @Throws(MultipleFailuresError::class)
    override fun dispose(context: ExtensionContext) {
        //afterAll method comes here
        //reverting late to keep operation effects for @AfterAll
        resourceOperations.reverseAfterClass()
        executeOnResourcesWithFailureValidation({ it.dispose(context) })
    }

    fun handleTestException(context: ExtensionContext, throwable: Throwable) {
        throw throwable
    }

    fun <T : TestResource> registerResources(vararg entries: Pair<Class<T>, T>) {
        entries.forEach { registerResource(it.first, it.second) }
    }

    fun <T : TestResource> registerResource(key: Class<T>, instance: T) {
        @Suppress("unchecked")
        resources.put(key as Class<TestResource>, instance)
        instance.init()
        logger.info { "Registered instance: ${instance.displayName} for class '${key.simpleName}'" }
    }

    //try reified form: http://kotlinlang.org/docs/reference/idioms.html#convenient-form-for-a-generic-function-that-requires-the-generic-type-information
    //inline fun <reified T: Any> Gson.fromJson(json: JsonElement): T = this.fromJson(json, T::class.java) --> does not work because cannot access non-public API in inline functions
    fun <T : TestResource> getTestResource(testResourceClass: Class<T>): T {
        @Suppress("unchecked")
        return resources.get(testResourceClass as Class<TestResource>) as T
    }

    fun printResources(): String {
        return resources.toString()
    }

    override fun toString(): String {
        return "TestResourceManager: \r\n ${printResources()}"
    }

}

interface TestLifecyclePhased {
    enum class TestLifecyclePhase {
        BEFORE_CLASS, BEFORE_TEST, IN_TEST, AFTER_TEST, AFTER_CLASS
    }

    fun transitionTo(phase: TestLifecyclePhase)

    fun resetLifecycle() {
        transitionTo(TestLifecyclePhase.BEFORE_CLASS)
    }
}
