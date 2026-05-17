package com.sahm.pos.domain.sync

import com.sahm.pos.domain.results.SyncProcessorResult

interface SyncOutboxProcessor {
  suspend  fun processPending(): SyncProcessorResult

}