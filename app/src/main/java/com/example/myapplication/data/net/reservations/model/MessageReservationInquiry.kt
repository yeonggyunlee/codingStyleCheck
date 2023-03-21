package com.example.myapplication


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MessageReservationInquiry(
    var roomId: Long,
    var id: Long,
    var createdAt: Long,
    var attachments: List<MessageReservationAttachment>?,
    var mentions: List<MentionObject>?,
    var status: String,
    var title: String?,
    var content: String,
    var reservationTime: Long,
    var failReason: String?
)

fun MessageReservationInquiry.toMessageReservationItem() = MessageReservationItem(
    id = id,
    roomId = roomId,
    createdAt = createdAt,
    title = title,
    content = content,
    reservationTime = reservationTime,
    status = status,
    failReason = failReason
)