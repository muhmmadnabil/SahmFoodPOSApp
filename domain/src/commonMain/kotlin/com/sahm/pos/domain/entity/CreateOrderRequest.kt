package com.sahm.pos.domain.entity

data class CreateOrderRequest(
    val items: List<Item>,
    val promoCode: String? = null,
    val orderType: OrderType = OrderType.TAKEAWAY,
) {
    data class Item(
        val menuItem: MenuItem,
        val quantity: Int,
    )
}
