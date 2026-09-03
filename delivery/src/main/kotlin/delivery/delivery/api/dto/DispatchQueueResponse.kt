package delivery.delivery.api.dto

import delivery.delivery.application.dto.DispatchQueueItem

data class DispatchQueueItemResponse(
    val deliveryId: Long,
    val orderId: Long,
    val shopId: Long,
) {
    companion object {
        fun from(item: DispatchQueueItem): DispatchQueueItemResponse =
            DispatchQueueItemResponse(item.deliveryId, item.orderId, item.shopId)
    }
}

data class DispatchQueueResponse(
    val items: List<DispatchQueueItemResponse>,
) {
    companion object {
        fun from(items: List<DispatchQueueItem>): DispatchQueueResponse =
            DispatchQueueResponse(items.map(DispatchQueueItemResponse::from))
    }
}
