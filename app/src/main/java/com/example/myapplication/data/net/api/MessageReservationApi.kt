package com.example.myapplication


interface MessageReservationApi {

    /**
     * Message Reservation 등록
     */
    @Headers(HEADER_V1)
    @POST("v1/teams/{teamId}/members/{memberId}/reservations")
    @JvmSuppressWildcards
    suspend fun createMessageReservations(
        @Path("teamId") teamId: Long,
        @Path("memberId") memberId: Long,
        @Body messageReservationSaveRequest: MessageReservationSaveRequest
    ): Response<MessageReservationCreated>

    /**
     * Message Reservation List
     */
    @Headers(HEADER_V1)
    @GET("v1/teams/{teamId}/members/{memberId}/reservations")
    suspend fun getMessageReservations(
        @Path("teamId") teamId: Long,
        @Path("memberId") memberId: Long
    ): Response<List<MessageReservationItem>>

    @Headers(HEADER_V1)
    @GET("v1/teams/{teamId}/members/{memberId}/reservations/{reservationId}")
    suspend fun getMessageReservationsInquiryInfo(
        @Path("teamId") teamId: Long,
        @Path("memberId") memberId: Long,
        @Path("reservationId") reservationId: Long
    ): Response<MessageReservationInquiry>

    @Headers(HEADER_V1)
    @DELETE("v1/teams/{teamId}/members/{memberId}/reservations/{reservationId}")
    suspend fun deleteMessageReservation(
        @Path("teamId") teamId: Long,
        @Path("memberId") memberId: Long,
        @Path("reservationId") reservationId: Long
    ): Response<ResCommon>
}