package com.sahm.pos.domain.usecase

fun interface UseCase<in Params, out Result> {
    suspend operator fun invoke(params: Params): Result
}
