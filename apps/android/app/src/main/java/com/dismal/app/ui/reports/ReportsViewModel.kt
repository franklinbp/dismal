package com.dismal.app.ui.reports

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.repository.DashboardRepository
import com.dismal.app.data.repository.MarketingIntelligenceRepository
import com.dismal.app.data.repository.SalesTargetRepository
import com.dismal.app.domain.models.DashboardSummary
import com.dismal.app.domain.models.DashboardTarget
import com.dismal.app.domain.models.MarketingAnalysis
import com.dismal.app.domain.models.SalesTargetSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel
    @Inject
    constructor(
        private val dashboardRepository: DashboardRepository,
        private val salesTargetRepository: SalesTargetRepository,
        private val marketingIntelligenceRepository: MarketingIntelligenceRepository,
    ) : ViewModel() {
        var uiState by mutableStateOf(ReportsUiState())
            private set

        init {
            loadReports()
        }

        fun loadReports() {
            if (uiState.isLoading) return
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            viewModelScope.launch {
                val summaryResult = dashboardRepository.getSummary()
                val targetsResult = dashboardRepository.getTopTargets()
                val targetSummaryResult = salesTargetRepository.getSummary()
                val marketingResult = marketingIntelligenceRepository.getAnalysis()

                val summary =
                    (summaryResult as? AppResult.Success)?.data
                        ?: uiState.summary
                        ?: DashboardSummary.empty()
                val targets = (targetsResult as? AppResult.Success)?.data.orEmpty()
                val targetSummary = (targetSummaryResult as? AppResult.Success)?.data
                val marketingAnalysis = (marketingResult as? AppResult.Success)?.data.orEmpty()

                uiState =
                    uiState.copy(
                        isLoading = false,
                        summary = summary,
                        topTargets = targets,
                        targetSummary = targetSummary,
                        priorities = buildPriorities(summary, targets, targetSummary, marketingAnalysis),
                        errorMessage =
                            listOfNotNull(
                                (targetsResult as? AppResult.Error)?.message,
                                (targetSummaryResult as? AppResult.Error)?.message,
                                (marketingResult as? AppResult.Error)?.message,
                            ).firstOrNull(),
                    )
            }
        }

        private fun buildPriorities(
            summary: DashboardSummary?,
            targets: List<DashboardTarget>,
            targetSummary: SalesTargetSummary?,
            marketingAnalysis: List<MarketingAnalysis>,
        ): List<String> {
            val priorities = mutableListOf<String>()

            if ((summary?.overdueArBalance ?: 0.0) > 0.0) {
                priorities += "Cobrar cartera vencida por ${formatMoney(summary?.overdueArBalance ?: 0.0)} antes de abrir mas credito."
            }

            if ((summary?.outboxFailedCount ?: 0L) > 0L) {
                priorities += "Revisar ${summary?.outboxFailedCount} eventos fallidos del outbox para no perder automatizaciones."
            }

            if ((targetSummary?.pendingTargets ?: 0) > 0) {
                priorities += "Empujar ${targetSummary?.pendingTargets} metas pendientes y vigilar el cierre del mes."
            }

            targets
                .filterNot { it.targetAchieved }
                .sortedByDescending { it.metaUnits - it.unitsSoldCurrent }
                .take(2)
                .forEach { target ->
                    priorities += "Priorizar ${target.productName}: va ${target.unitsSoldCurrent}/${target.metaUnits} unidades."
                }

            marketingAnalysis
                .filter { it.priority.equals("HIGH", ignoreCase = true) || it.priority.equals("ALTA", ignoreCase = true) }
                .take(2)
                .forEach { analysis ->
                    val nextAction = analysis.suggestedActions.firstOrNull() ?: analysis.explanation
                    priorities += "Mover ${analysis.productName} hoy: $nextAction"
                }

            if (priorities.isEmpty()) {
                priorities += "Sin alertas criticas. Mantener el enfoque en cerrar ventas y monitorear metas activas."
            }

            return priorities
        }
    }

data class ReportsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val summary: DashboardSummary? = null,
    val targetSummary: SalesTargetSummary? = null,
    val topTargets: List<DashboardTarget> = emptyList(),
    val priorities: List<String> = emptyList(),
)

private fun formatMoney(value: Double): String = "$" + String.format(java.util.Locale.US, "%.2f", value)
