package com.sahm.pos.domain.results

sealed interface CreateRefundResult {
    data class Success(val refundId: String) : CreateRefundResult
    data object OrderNotFound : CreateRefundResult
    data object OrderNotRefundable : CreateRefundResult
    data object EmptySelection : CreateRefundResult
    data object InvalidQuantity : CreateRefundResult
    data object ItemNotInOrder : CreateRefundResult
    data object QuantityExceedsRefundable : CreateRefundResult
    data class Failed(val message: String) : CreateRefundResult
}