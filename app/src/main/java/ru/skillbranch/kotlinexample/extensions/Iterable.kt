package ru.skillbranch.kotlinexample.extensions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    if (!isEmpty()) {
        val result = ArrayList<T>(this)
        for (i in size - 1 downTo 0 ) {
            if (predicate(this[i])) {
                result.removeAt(i)
                return result
            } else {
                result.removeAt(i)
            }
        }
    }
    return emptyList()
}