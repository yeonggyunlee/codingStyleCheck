package com.example.myapplication

interface MessageReservationRepository {

    suspend fun createMessageReservations(
        teamId: Long,
        memberId: Long,
        title: String?,
        content: String,
        reservationTime: Long,
        roomId: Long,
        mentions: List<MentionObject>?,
        fileIds: List<Long>?
    ): Flow<HttpResult<MessageReservationCreated>>

    suspend fun getMessageReservations(
        teamId: Long,
        memberId: Long
    ): Flow<HttpResult<List<MessageReservationItem>>>

    suspend fun getMessageReservationInquiryInfo(
        teamId: Long,
        memberId: Long,
        reservationId: Long
    ): Flow<HttpResult<MessageReservationInquiry>>

    suspend fun deleteMessageReservation(
        teamId: Long,
        memberId: Long,
        reservationId: Long
    ): Flow<HttpResult<ResCommon>>
}