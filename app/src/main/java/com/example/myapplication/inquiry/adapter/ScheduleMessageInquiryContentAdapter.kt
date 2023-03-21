package com.example.myapplication

class ScheduleMessageInquiryContentAdapter(
    private val onClick: OnClick
) :
    ListAdapter<MessageReservationAttachment, RecyclerView.ViewHolder>(diffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            IMAGE_TYPE -> {
                val binding = ItemReservationInquiryImageBinding.inflate(inflater, parent, false)
                ScheduleMessageInquiryImageViewHolder(binding, onClick)
            }
            FILE_TYPE -> {
                val binding = ItemReservationInquiryFileBinding.inflate(inflater, parent, false)
                ScheduleMessageInquiryFileViewHolder(binding, onClick)
            }
            else -> throw(Throwable("View type not matching"))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.content.type.contains(IMAGE) && item.content.extraInfo != null) {
            true -> (holder as? ScheduleMessageInquiryImageViewHolder)?.bind(item)
            false -> (holder as? ScheduleMessageInquiryFileViewHolder)?.bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item.content.type.contains(IMAGE) && item.content.extraInfo != null) {
            true -> IMAGE_TYPE
            false -> FILE_TYPE
        }
    }

    interface OnClick {
        fun imageClick(file: MessageReservationAttachment)
        fun fileClick(file: MessageReservationAttachment)
    }

    companion object {
        private val diffUtil = object : DiffUtil.ItemCallback<MessageReservationAttachment>() {
            override fun areItemsTheSame(
                oldItem: MessageReservationAttachment,
                newItem: MessageReservationAttachment
            ): Boolean =
                oldItem == newItem

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(
                oldItem: MessageReservationAttachment,
                newItem: MessageReservationAttachment
            ): Boolean =
                oldItem == newItem
        }

        const val IMAGE_TYPE = 1118
        const val FILE_TYPE = 1119
        const val FILE_TYPE_SPAN = 6
        const val DEFAULT_SPAN = 2
    }
}