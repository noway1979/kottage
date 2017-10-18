package org.noway.kottage

import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import io.github.config4k.TypeReference
import io.github.config4k.extract
import io.github.config4k.readers.SelectReader
import kotlin.reflect.KClass

inline fun <C: Any> Config.extract(type: KClass<C>, path: String): C {
    val genericType = object : TypeReference<C>() {}.genericType()
    val clazz = listOf(type)

    val result = SelectReader.getReader(
            genericType?.let { clazz + it } ?: clazz)(this, path)

    return try {
        result as C
    } catch (e: Exception) {
        throw result?.let { e } ?: ConfigException.BadPath(
                path, "take a look at your config")
    }
}

fun Class<*>.convertToTypesafeConfigPath() : String
{
    return this.name.replace('$', '.')
}


open class Configuration(val config: Config) {

    constructor() : this(ConfigFactory.load())

    inline operator fun <reified T> invoke(path: String): T {
        return read(path)
    }

    inline fun <reified T> read(path : String) : T
    {
        return config.extract(path)
    }

    fun <T : Any> read(configType: KClass<T> , path :String) : T
    {
        return config.extract(configType, path)
    }

    override fun toString(): String {
        return config.toString()
    }


}