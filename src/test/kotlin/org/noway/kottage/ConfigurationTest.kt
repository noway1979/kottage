package org.noway.kottage

import com.typesafe.config.ConfigFactory
import io.kotlintest.mock.mock
import mu.KotlinLogging
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import kotlin.reflect.KClass

class ConfigurationTest(testResourceManager: TestResourceManager) : KottageTest(testResourceManager) {

    private val logger = KotlinLogging.logger {}

    data class Person(val name: String, val age: Int)

    class TestConfigResource(config: Configuration) :
            AbstractConfigurableTestResource<Person>(mock<TestResourceManager>(), config) {
        override val reifiedConfigType: KClass<Person>
            get() = Person::class

    }

    @Test
    fun testReadGenericDataClassShouldYieldAPopulatedObject() {

        val config = ConfigFactory.parseString("""
                                          |key {
                                          |  name = "foo"
                                          |  age = 20
                                          |}""".trimMargin())

        val person = Configuration(config)<Person>("key")

        assertAll("testdata",
                  Executable({ assertThat(person.name, `is`("foo")) }),
                  Executable({ assertThat(person.age, `is`(20)) })

                 )
    }

    @Test
    fun testConfigShouldBeLoadedFromClassPath() {
        //this test relies on application.conf loaded from test "resources" root
        val conf = Configuration()
        assertThat(conf.config, notNullValue())

        logger.info { conf }
    }

    @Test
    fun testResourceWithConfigShallLoadConfigDataClass() {
        val config = ConfigFactory.parseString("""
                                          |${TestConfigResource::class.java.convertToTypesafeConfigPath()} {
                                          |  name = "foo"
                                          |  age = 20
                                          |}""".trimMargin())

        logger.info { config }

        val resource = TestConfigResource(Configuration(config))

        assertAll("testResource with config",
                  Executable({ assertThat(resource.resourceConfig.name, `is`("foo")) }),
                  Executable({ assertThat(resource.resourceConfig.age, `is`(20)) })
                 )
    }
}