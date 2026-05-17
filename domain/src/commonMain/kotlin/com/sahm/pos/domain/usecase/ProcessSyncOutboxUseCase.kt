package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.sync.SyncOutboxProcessor
import com.sahm.pos.domain.results.SyncProcessorResult

class ProcessSyncOutboxUseCase(
    private val processor: SyncOutboxProcessor,
) {
    suspend operator fun invoke(): SyncProcessorResult = processor.processPending()
}
