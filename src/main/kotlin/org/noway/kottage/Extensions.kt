package org.noway.kottage

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.Validation

fun <T, V : Any, E : Exception> Collection<T>.forEachWithException(func: (T) -> V): Validation<E> {
    val list: List<Result<V, Exception>> = this.map({ Result.of( { func(it)} )})

    //spread operator to allow array as vararg parameter
    return Validation(*list.toTypedArray()) as Validation<E>
}