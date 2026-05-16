package com.sahm.pos.data

import com.sahm.pos.domain.UUIDProvider
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class UUIDProviderImpl : UUIDProvider {
    @OptIn(ExperimentalUuidApi::class)
    override fun randomUuid(): String = Uuid.random().toString()
}