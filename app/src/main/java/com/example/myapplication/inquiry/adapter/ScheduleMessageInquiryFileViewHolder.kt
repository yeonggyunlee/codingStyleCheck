package com.example.myapplication

class ScheduleMessageInquiryFileViewHolder(
    private val binding: ItemReservationInquiryFileBinding,
    private val onClick: ScheduleMessageInquiryContentAdapter.OnClick
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: MessageReservationAttachment) {
        binding.apply {
            attachment = item
            listener = onClick
            vReservationInquiry.fileType = getFileType(item.content.filterType)
        }
    }

    private fun getFileType(type: String): Int {
        return FileResourceUtil.getFileTypeByNameOrExtension(type).ordinal
    }
}