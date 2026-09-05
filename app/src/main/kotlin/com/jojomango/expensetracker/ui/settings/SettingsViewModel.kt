package com.jojomango.expensetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.domain.Settings
import com.jojomango.expensetracker.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        val settings: StateFlow<Settings> =
            settingsRepository.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

        /** SPEC.md §3.5：變更後所有錢包的週分組即時重算，不改變任何交易資料——這件事
         * 天生成立，因為 Week/Budget 的計算永遠即時、不快取任何跟 weekStartDay 有關的值。 */
        fun setWeekStartDay(day: DayOfWeek) {
            viewModelScope.launch {
                settingsRepository.update(settingsRepository.get().copy(weekStartDay = day))
            }
        }
    }
