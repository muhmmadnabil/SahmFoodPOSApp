package com.sahm.pos.data.printing

import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.domain.ReceiptPrinter
import com.sahm.pos.domain.repository.OrderRepo

expect fun createReceiptPrinter(
    platformContext: PlatformContext,
    orderRepo: OrderRepo,
): ReceiptPrinter
