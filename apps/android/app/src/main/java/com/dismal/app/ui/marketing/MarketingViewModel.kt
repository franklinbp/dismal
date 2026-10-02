package com.dismal.app.ui.marketing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.repository.MarketingIntelligenceRepository
import com.dismal.app.domain.models.MarketingAnalysis
import com.dismal.app.domain.models.StrategyAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketingViewModel
    @Inject
    constructor(
        private val marketingIntelligenceRepository: MarketingIntelligenceRepository,
    ) : ViewModel() {
        var uiState by mutableStateOf(MarketingUiState())
            private set

        init {
            loadStatus()
        }

        fun loadStatus() {
            if (uiState.isLoading) return
            uiState = uiState.copy(isLoading = true, errorMessage = null, actionMessage = null)
            viewModelScope.launch {
                when (val result = marketingIntelligenceRepository.getAnalysis()) {
                    is AppResult.Success -> {
                        val analysis = result.data
                        val actionsResult = marketingIntelligenceRepository.getOpenActions()
                        val actions =
                            when (actionsResult) {
                                is AppResult.Success -> actionsResult.data
                                is AppResult.Error -> emptyList()
                            }
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                analysis = analysis,
                                openActions = actions,
                                actionMessage = (actionsResult as? AppResult.Error)?.message,
                                highPriorityCount =
                                    analysis.count {
                                        it.priority.equals("HIGH", ignoreCase = true) ||
                                            it.priority.equals("ALTA", ignoreCase = true)
                                    },
                                commercialAlertCount = analysis.count { it.state.equals("ALERTA_COMERCIAL", ignoreCase = true) },
                                pausedCount = analysis.count { it.state.equals("PAUSADO", ignoreCase = true) },
                                lowTractionCount = analysis.count { it.state.equals("BAJA_TRACCION", ignoreCase = true) },
                            )
                    }

                    is AppResult.Error -> {
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                errorMessage = result.message,
                            )
                    }
                }
            }
        }

        fun updateActionStatus(
            actionId: String,
            status: String,
        ) {
            if (uiState.updatingActionId != null) return
            uiState = uiState.copy(updatingActionId = actionId, errorMessage = null, actionMessage = null)
            viewModelScope.launch {
                when (val result = marketingIntelligenceRepository.updateActionStatus(actionId, status)) {
                    is AppResult.Success -> {
                        val updated = result.data
                        val nextActions =
                            if (updated.status.equals("HECHA", ignoreCase = true) ||
                                updated.status.equals("DESCARTADA", ignoreCase = true)
                            ) {
                                uiState.openActions.filterNot { it.id == updated.id }
                            } else {
                                uiState.openActions.map { if (it.id == updated.id) updated else it }
                            }
                        uiState =
                            uiState.copy(
                                updatingActionId = null,
                                openActions = nextActions,
                                actionMessage = "Accion actualizada",
                            )
                    }

                    is AppResult.Error -> {
                        uiState =
                            uiState.copy(
                                updatingActionId = null,
                                actionMessage = result.message,
                            )
                    }
                }
            }
        }
    }

data class MarketingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val analysis: List<MarketingAnalysis> = emptyList(),
    val openActions: List<StrategyAction> = emptyList(),
    val updatingActionId: String? = null,
    val actionMessage: String? = null,
    val highPriorityCount: Int = 0,
    val commercialAlertCount: Int = 0,
    val pausedCount: Int = 0,
    val lowTractionCount: Int = 0,
)
