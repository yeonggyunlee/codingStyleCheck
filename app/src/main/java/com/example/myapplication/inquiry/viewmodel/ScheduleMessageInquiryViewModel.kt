package com.example.myapplication

class ScheduleMessageInquiryViewModel @Inject constructor(
    private val reservationRepository: MessageReservationRepository,
) : BaseViewModel() {

    private val teamId = TeamInfoLoader.getTeamId()
    private val myId = TeamInfoLoader.getMyId()
    private var reservationId = 0L
    private var roomId = 0L

    private val _fetchReservationInfo = MutableSharedFlow<Pair<Long, Long>>()

    private val _messageReservationDetail = MutableStateFlow(MessageReservationDetail())
    val messageReservationDetail = _messageReservationDetail.asStateFlow()

    private val _imageCount = MutableSharedFlow<Int>()
    val imageCount = _imageCount.asSharedFlow()

    private val _showErrorToast = MutableSharedFlow<Int>()
    val showErrorToast = _showErrorToast.asSharedFlow()

    private val _actionEvent = MutableSharedFlow<ActionEvent>()
    val actionEvent: SharedFlow<ActionEvent> = _actionEvent.asSharedFlow()


    init {
        viewModelScope.launch { _fetchReservationInfo.collectLatest { pair -> setDate(pair) } }
    }

    private suspend fun setDate(pair: Pair<Long, Long>) {
        viewModelScope.launch {
            reservationRepository.getMessageReservationInquiryInfo(
                teamId = teamId,
                memberId = pair.first,
                reservationId = pair.second
            )
                .retryWhen { _, attempt -> attempt > 3 }
                .catch { e -> emit(HttpResult.Failure(e)) }
                .collect { httpResult ->
                    httpResult.onSuccess { result ->
                        result?.let { fetchMessageReservationDetail(it) }
                    }.onFailure { failure ->
                        Timber.e(failure.toString())
                        showErrorToast(R.string.common_errormsg)
                    }
                }
        }
    }

    /**
     * Error Toast Event
     */
    private fun showErrorToast(message: Int) {
        viewModelScope.launch {
            _showErrorToast.emit(message)
        }
    }

    /**
     * MessageReservation 조회 초기값 셋팅
     */
    private fun fetchMessageReservationDetail(result: MessageReservationInquiry) {
        viewModelScope.launch {
            setImageCount(result.attachments ?: listOf())
            _messageReservationDetail.emit(result.toMessageReservationDetail())
        }
    }

    private fun sendActionEvent(actionEvent: ActionEvent) {
        viewModelScope.launch {
            _actionEvent.emit(actionEvent)
        }
    }

    fun sendMovePreviewEvent(pair: Pair<String?, String>, isImageUrl: Boolean) {
        sendActionEvent(ActionEvent.MoveToPreview(pair, isImageUrl))
    }

    /**
     * 미리보기가 가능한 파일은 미리보기 보여주고
     * 불가능한 파일은 지원하지 않는 파일 Toast 보여줌
     */
    fun checkPreviewOrToast(file: MessageReservationAttachment) {
        if (FileUtil.isDocumentPreviewFileExt(file.content.ext)) {
            showPreviewDocument(file.content.filename, file.content.name)
        } else {
            showErrorToast(R.string.file_detail_nopreview)
        }
    }

    fun getDownloadUrl(fileId: Long, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            fileRepository.getDownloadUrl(teamId, fileId, null)
                .catch { e -> emit(HttpResult.Failure(e)) }
                .collect { HttpResult ->
                    HttpResult.onSuccess { result ->
                        result?.let {
                            sendActionEvent(
                                ActionEvent.MoveToPreview(
                                    pair = it.downloadUrl to fileName,
                                    isImageUrl = true
                                )
                            )
                        }
                    }.onFailure { failure ->
                        Timber.e(failure.toString())
                        if (failure?.throwable is HttpException) {
                            val strRes = failure.error?.let {
                                when (it.code) {
                                    ErrorCode.ERROR_CODE_1 -> R.string.permission_download_desc
                                    else -> R.string.common_errormsg
                                }
                            } ?: R.string.common_errormsg
                            showErrorToast(strRes)
                        }
                    }

                }
        }
    }

    private fun sendDeleteEvent(reservationId: Long) {
        sendActionEvent(ActionEvent.DeleteScheduleMessage(reservationId))
    }

    fun initArg(entityId: Long, id: Long) {
        reservationId = id
        roomId = entityId
    }

    /** onViewCreated 에서 호출 **/
    fun fetchWhenOnViewCreated() {
        viewModelScope.launch {
            _fetchReservationInfo.emit(myId to reservationId)
        }
    }

    /** 미리보기 가능한 파일 클릭 했을때 **/
    private fun showPreviewDocument(hashFileName: String, realFileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            documentRepository.getDocumentViewerKey(hashFileName)
                .retryWhen { _, attempt -> attempt > 3 }
                .catch { e -> emit(HttpResult.Failure(e)) }
                .collect { HttpResult ->
                    HttpResult.onSuccess { result ->
                        result?.let {
                            sendMovePreviewEvent(
                                pair = ((Flavors.getDocumentPreviewUrl() + it.key) to realFileName),
                                isImageUrl = false
                            )
                        }
                    }.onFailure { failure ->
                        Timber.e(failure.toString())
                        showErrorToast(R.string.common_errormsg)
                    }
                }
        }
    }

    /** spanCount 계산을 위한 Image 갯수 셋팅 **/
    private fun setImageCount(info: List<MessageReservationAttachment>) {
        viewModelScope.launch {
            _imageCount.emit(info.filter { (it.content.icon == IMAGE) && it.content.extraInfo != null }.size)
        }
    }

    fun copyReservationMessage() {
        viewModelScope.launch {
            _actionEvent.emit(ActionEvent.CopyContent(_messageReservationDetail.value.content.toString()))
        }
    }


    /** 삭제 버튼 눌렀을 때 **/
    fun deleteReservationMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            reservationRepository.deleteMessageReservation(teamId, myId, reservationId)
                .retryWhen { _, attempt -> attempt > 3 }
                .catch { e -> emit(HttpResult.Failure(e)) }
                .collect { httpResult ->
                    httpResult.onSuccess {
                        showSuccessToast(R.string.schedule_msg_toast_deleted)
                        sendDeleteEvent(reservationId)
                    }.onFailure { failure ->
                        Timber.e(failure.toString())
                        checkToShowToastWhenDelete(reservationId)
                    }
                }
        }
    }

    /**
     * 예약된 메시지 삭제시 실패했을때
     * 메시지 예약 조회 API response의 status 값에 따라 처리
     * 이유 : 삭제 실패했을때 ErrorCode가 따로 정의 되어있지 않아서 메시지 예약 조회 status값에 따라 처리해야함
     *
     * status가 DELETE면  삭제된 메시지입니다 Toast 띄움
     * status가 SUCCESS면 이미 발송된 메시지입니다 Toast 띄움
     */
    private fun checkToShowToastWhenDelete(reservationId: Long) {
        val teamId = TeamInfoLoader.getTeamId()
        val memberId = TeamInfoLoader.getMyId()

        viewModelScope.launch {
            reservationRepository.getMessageReservationInquiryInfo(teamId, memberId, reservationId)
                .retryWhen { _, attempt -> attempt > 3 }
                .catch { e -> emit(HttpResult.Failure(e)) }
                .collect { httpResult ->
                    httpResult.onSuccess { result ->
                        result?.let { checkStatusAndShowToast(it.status) }
                        sendDeleteEvent(reservationId)
                    }.onFailure { failure ->
                        Timber.e(failure.toString())
                        showErrorToast(R.string.common_errormsg)
                        sendDeleteEvent(reservationId)
                    }
                }
        }
    }

    private fun checkStatusAndShowToast(status: String) {
        when (status) {
            MessageReservationStatus.DELETED.name -> {
                showErrorToast(R.string.schedule_msg_toast_deleted)
            }
            MessageReservationStatus.SUCCESS.name -> {
                showErrorToast(R.string.schedule_msg_toast_success_sent)
            }
        }
    }
}