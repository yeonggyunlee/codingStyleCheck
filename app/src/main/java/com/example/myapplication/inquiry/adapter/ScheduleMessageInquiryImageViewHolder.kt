package com.example.myapplication

class ScheduleMessageInquiryImageViewHolder(
    private val binding: ItemReservationInquiryImageBinding,
    private val onClick: ScheduleMessageInquiryContentAdapter.OnClick
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MessageReservationAttachment) {
        binding.apply {
            attachment = item
            listener = onClick
        }
        Glide.with(binding.root.context).load(item.content.extraInfo?.mediumThumbnailUrl).into(binding.iv)
    }
}