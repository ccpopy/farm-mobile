package com.qqfarm.mobile.ui.state

import androidx.compose.runtime.Composable

actual typealias AppUpdateViewModel = com.qqfarm.mobile.ui.state.AppUpdateViewModelImpl

@Composable
actual fun rememberAppUpdateViewModel(): AppUpdateViewModel =
    androidx.lifecycle.viewmodel.compose.viewModel()

