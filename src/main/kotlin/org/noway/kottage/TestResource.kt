package org.noway.kottage

import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.reflect.KClass


interface TestResource {

    val displayName: String
        get() = javaClass.name

    @Throws(TestResourceException::class)
    fun init()

    @Throws(TestResourceException::class)
    fun postProcessTestInstance(context: ExtensionContext)

    @Throws(TestResourceException::class)
    fun setupTestInstance(context: ExtensionContext)

    @Throws(TestResourceException::class)
    fun setupTestMethod(context: ExtensionContext)

    @Throws(TestResourceException::class)
    fun tearDownTestMethod(context: ExtensionContext)

    @Throws(TestResourceException::class)
    fun tearDownTestInstance(context: ExtensionContext)

    @Throws(TestResourceException::class)
    fun dispose(context: ExtensionContext)
}

abstract class AbstractTestResource(val testResourceManager: TestResourceManager) : TestResource {
    override fun init() {
    }

    override fun setupTestInstance(context: ExtensionContext) {
    }

    override fun setupTestMethod(context: ExtensionContext) {
    }

    override fun tearDownTestMethod(context: ExtensionContext) {
    }

    override fun postProcessTestInstance(context: ExtensionContext) {
    }

    override fun tearDownTestInstance(context: ExtensionContext) {
    }

    override fun dispose(context: ExtensionContext) {
    }
}

abstract class AbstractConfigurableTestResource<C : Any>(testResourceManager: TestResourceManager,
                                                         config: Configuration) :
        AbstractTestResource(testResourceManager) {
    protected abstract val reifiedConfigType: KClass<C>

    //https@ //stackoverflow.com/questions/36012190/kotlin-abstract-class-with-generic-param-and-methods-which-use-type-param
    //Class type parameters are not reified, only available for inline functions. Must pass a Class token around which has to be concrete in every subclass
    val resourceConfig: C = config.read(reifiedConfigType, configPath())

    //inner classes' separator '$' is interpreted as key separator '.'
    private fun configPath() = javaClass.canonicalName.replace('$', '.')

}


