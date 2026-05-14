package com.sahm.pos.data.mapper

fun interface DataMapper<in Input, out Output> {
    fun map(input: Input): Output
}
