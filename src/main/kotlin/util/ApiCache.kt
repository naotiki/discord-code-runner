package util

import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ApiCache<R> private constructor(private val cacheDuration: Duration, private val block: suspend () -> R, initialValue: R) {
    private var lastUpdated = System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS)
    private var cache: R = initialValue

    companion object {
        suspend fun <R> cache(cacheDuration: Duration = 1.minutes, block: suspend () -> R): ApiCache<R> {
            return ApiCache(cacheDuration, block, block())
        }
    }
    private var updatedCallbacks = mutableListOf<Pair<((R)->Unit),Boolean>>()
    fun onUpdated(ignoreSameValue:Boolean = false,block:(R)->Unit):ApiCache<R>{
        updatedCallbacks.add(block to ignoreSameValue)
        return this
    }
    val shouldUpdate get() = (System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS) - lastUpdated )>cacheDuration
    suspend fun update(){
        val result=block()
        cache = result
        updatedCallbacks.forEach {(callback,ignoreSame)->
            if (!ignoreSame || result != cache){
                callback(result)
            }
        }
    }
    suspend fun get(): R {
        if (shouldUpdate){
            update()
        }
        return cache
    }
}
