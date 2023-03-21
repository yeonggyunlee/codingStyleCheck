package com.example.myapplication


/**
 * 예약 메시지 조회 화면
 */
class ScheduleMessageInquiryFragment :
    BaseFragment<FragmentMessageReservationInquiryBinding, ScheduleMessageInquiryViewModel>() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    override val viewModel: ScheduleMessageInquiryViewModel by viewModels { factory }
    override val layoutResId: Int = R.layout.fragment_message_reservation_inquiry
    override val useBackPressedCallback: Boolean = false

    /** 예약 메시지 조회 화면에서 Attachment 어댑터 **/
    private val reservationInquiryAdapter by lazy {
        ScheduleMessageInquiryContentAdapter(object : ScheduleMessageInquiryContentAdapter.OnClick {
            override fun imageClick(file: MessageReservationAttachment) {
                onImageClick(file.content.extraInfo?.thumbnailUrl to file.content.filename)
            }

            override fun fileClick(file: MessageReservationAttachment) {
                if (file.content.extraInfo == null && file.content.icon == IMAGE) {
                    ColoredToast.showError(R.string.file_detail_nopreview)
                } else {
                    onFileClick(file)
                }
            }
        })
    }

    /**
     * 일반 파일인 경우
     * 갯수 상관없이 span size 6
     *
     * 이미지 파일인 경우
     * 이미지 파일 1개 : span size 6
     * 이미지 파일 2개 : span size 3
     * 이미지 파일 3개 : span size 2
     */
    private lateinit var spanSize: GridLayoutManager.SpanSizeLookup

    override fun initDataBinding() {
        binding.run { viewModel = this@ScheduleMessageInquiryFragment.viewModel }
    }

    override fun initView() {
        setHasOptionsMenu(true)
        binding.run {
            setupToolbar(tbMessageReservationInquiry)
            setupReservationInquiryAdapter(rvReservationInquiryFileGroup)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val roomId = it.getLong(ROOM_ID)
            val reservationId = it.getLong(RESERVATION_ID)
            viewModel.initArg(entityId = roomId, id = reservationId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_reservation_inquiry, menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initEvent()
        registerEventBus()
        viewModel.fetchWhenOnViewCreated()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterEventBus()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> activity?.onBackPressed()
            R.id.menu_reservation_inquiry_delete -> showDeleteDialog(requireContext())
            R.id.menu_reservation_inquiry_copy -> copyMessage()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initEvent() {

        launchOnCreated {
            viewModel.messageReservationDetail.collectLatest { reservationInquiryAdapter.submitList(it.attachment) }
        }
        launchOnCreated {
            viewModel.imageCount.collectLatest { imageCount -> initSpanSize(imageCount) }
        }
        launchOnCreated {
            viewModel.showErrorToast.collectLatest { event -> ColoredToast.showError(event) }
        }
        launchOnCreated {
            viewModel.actionEvent.collectLatest { actionEvent ->
                when (actionEvent) {
                    is ActionEvent.MoveToPreview -> moveToPreviewActivity(actionEvent.pair, actionEvent.isImageUrl)
                    is ActionEvent.CopyContent -> copy(actionEvent.content)
                    is ActionEvent.DeleteScheduleMessage -> deleteScheduleMessage(actionEvent.reservationId)

                }
            }
        }
    }

    /** 예약 메시지 삭제 **/
    private fun deleteMessage() = viewModel.deleteReservationMessage()

    /** 예약 메시지 삭제 ActionEvent **/
    private fun deleteScheduleMessage(reservationId: Long) {
        if (SplitViewUtil.useSplitView(activity)) {
            parentFragmentManager.setFragmentResult(
                MessageContentConstants.REQUEST_KEY_SCHEDULE_MESSAGE,
                Bundle().apply { putLong(EXTRA_DELETE_RESULT, reservationId) }
            )
        } else {
            activity?.setResult(
                Activity.RESULT_OK,
                Intent().apply { putExtra(EXTRA_DELETE_RESULT, reservationId) })
        }
        activity?.onBackPressed()
    }

    /** 예약 메시지 복사 **/
    private fun copyMessage() = viewModel.copyReservationMessage()

    /** 예약 메시지 복사 ActionEvent **/
    private fun copy(content: String) {
        val clipboardManager =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, content))
        ColoredToast.show(R.string.copied_to_clipboard)
    }

    /** 이미지 파일 갯수에 따른 Span Size 셋팅 **/
    private fun initSpanSize(imageCount: Int) {
        spanSize = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (reservationInquiryAdapter.getItemViewType(position)) {
                    IMAGE_TYPE -> DynamicSpanUtil.getSpan(imageCount, position)
                    FILE_TYPE -> FILE_TYPE_SPAN
                    else -> DEFAULT_SPAN
                }
            }
        }
        binding.rvReservationInquiryFileGroup.layoutManager =
            GridLayoutManager(context, 6).apply { spanSizeLookup = spanSize }
    }

    /** Adapter Setting **/
    private fun setupReservationInquiryAdapter(recyclerView: RecyclerView) {
        recyclerView.apply {
            adapter = reservationInquiryAdapter
            itemAnimator = null
        }
    }

    private fun onImageClick(pair: Pair<String?, String>) {
        viewModel.sendMovePreviewEvent(pair = pair, false)
    }

    private fun onFileClick(file: MessageReservationAttachment) {
        viewModel.checkPreviewOrToast(file)
    }

    /** 사이냅 프리뷰로 이동 **/
    private fun moveToPreviewActivity(pair: Pair<String?, String>, isImageUrl: Boolean) {
        pair.first?.let { url ->
            val henson = Henson.with(requireActivity())
                .gotoPreviewActivity()
                .fileName(pair.second)
                .url(url)
                .isImageUrl(isImageUrl)

            startActivity(henson.build())
        }
    }

    private fun showDeleteDialog(context: Context) {
        AlertDialog.Builder(context, R.style.Theme_AlertDialog_FixWidth_300)
            .setMessage(R.string.schedule_msg_confirm_delete)
            .setNegativeButton(R.string.btn_cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.btn_confirm) { dialog, _ ->
                deleteMessage()
                dialog.dismiss()
            }.show()
    }

    /** 툴바 Setting **/
    private fun setupToolbar(toolbar: Toolbar) {
        if (SplitViewUtil.useSplitView(activity)) {
            toolbar.run {
                navigationIcon = requireContext().getDrawable(R.drawable.ic_arrow_left)
                menu.clear()
                inflateMenu(R.menu.menu_reservation_inquiry)
                setNavigationOnClickListener { activity?.onBackPressed() }
                setOnMenuItemClickListener { onOptionsItemSelected(it) }
            }
        } else {
            toolbar.navigationIcon = requireContext().getDrawable(R.drawable.ic_arrow_left)
            (activity as AppCompatActivity).setSupportActionBar(toolbar)
            setHasOptionsMenu(true)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(event: ShowProfileEvent) {
        activity?.also { ProfileUtil.startProfileActivity(it, event.userId) }
    }
}