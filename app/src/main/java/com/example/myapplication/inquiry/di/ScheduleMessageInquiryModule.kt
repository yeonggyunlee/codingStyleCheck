package com.example.myapplication

@Module
abstract class ScheduleMessageInquiryModule {

    @Binds
    @IntoMap
    @ViewModelKey(ScheduleMessageInquiryViewModel::class)
    abstract fun bindMessageReservationInquiryViewModel(viewModel: ScheduleMessageInquiryViewModel): ViewModel
}