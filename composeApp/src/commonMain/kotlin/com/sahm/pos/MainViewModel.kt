package com.sahm.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahm.pos.domain.CheckPhoneTimeResult
import com.sahm.pos.domain.usecase.CheckPhoneTimeUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val checkPhoneTimeUseCase: CheckPhoneTimeUseCase,
) : ViewModel() {
    private val _isTimeCorrect = MutableStateFlow(true)
    val isTimeCorrect: StateFlow<Boolean> = _isTimeCorrect.asStateFlow()
    private var checkPhoneTimeJob: Job? = null

    fun checkPhoneTime() {
        if (checkPhoneTimeJob?.isActive == true) return

        checkPhoneTimeJob = viewModelScope.launch {
            val result = checkPhoneTimeUseCase()

            when (result) {
                is CheckPhoneTimeResult.Failed -> Unit
                is CheckPhoneTimeResult.Invalid -> {
                    _isTimeCorrect.update { false }
                }

                is CheckPhoneTimeResult.Valid -> {
                    _isTimeCorrect.update { true }
                }
            }
        }
    }
}
