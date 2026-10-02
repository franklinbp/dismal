package com.dismal.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dismal.app.core.ui.common.UiPalette
import com.dismal.app.ui.customers.CustomerViewModel
import com.dismal.app.ui.customers.CustomersScreen
import com.dismal.app.ui.marketing.MarketingScreen
import com.dismal.app.ui.marketing.MarketingViewModel
import com.dismal.app.ui.pricelist.PriceListScreen
import com.dismal.app.ui.pricelist.PriceListViewModel
import com.dismal.app.ui.profile.ProfileScreen
import com.dismal.app.ui.profile.ProfileViewModel
import com.dismal.app.ui.reports.ReportsScreen
import com.dismal.app.ui.reports.ReportsViewModel
import com.dismal.app.ui.sales.SaleViewModel
import com.dismal.app.ui.sales.SalesScreen
import com.dismal.app.ui.billing.BillingScreen
import com.dismal.app.ui.billing.BillingViewModel
import com.dismal.app.ui.collections.CollectionsScreen
import com.dismal.app.ui.collections.CollectionsViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onLoggedOut: () -> Unit,
) {
    val uiState = viewModel.uiState
    val navController = rememberNavController()
    val menuItems = viewModel.visibleMenuItems()
    val mobileMenuItems =
        listOf(
            DashboardRoutes.HOME,
            DashboardRoutes.REPORTS,
            DashboardRoutes.MARKETING,
            DashboardRoutes.SALES,
            DashboardRoutes.PROFILE,
        ).mapNotNull { route -> menuItems.find { it.route == route } }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: DashboardRoutes.HOME
    val palette = UiPalette.current()

    LaunchedEffect(uiState.logoutRequested) {
        if (uiState.logoutRequested) {
            onLoggedOut()
            viewModel.onLogoutHandled()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp
        val backgroundBrush =
            Brush.verticalGradient(
                colors = listOf(palette.background, palette.surface),
            )

        Scaffold(
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = palette.surface.copy(alpha = 0.96f),
                    ) {
                        mobileMenuItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) { launchSingleTop = true }
                                },
                                icon = { Icon(menuIcon(item.route), contentDescription = item.label) },
                                label = {
                                    Text(
                                        mobileMenuLabel(item.route),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                    }
                }
            },
        ) { paddingValues ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(backgroundBrush)
                        .padding(paddingValues),
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = palette.surface.copy(alpha = 0.95f),
                    ) {
                        menuItems.forEach { item ->
                            NavigationRailItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    if (item.route == DashboardRoutes.LOGOUT) {
                                        viewModel.requestLogout()
                                    } else {
                                        navController.navigate(item.route) { launchSingleTop = true }
                                    }
                                },
                                icon = { Icon(menuIcon(item.route), contentDescription = item.label) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                NavHost(
                    navController = navController,
                    startDestination = DashboardRoutes.HOME,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                ) {
                    composable(DashboardRoutes.HOME) {
                        DashboardHomeScreen(
                            state = uiState,
                            onRefresh = viewModel::refreshOverview,
                            onNavigate = { route ->
                                navController.navigate(route) { launchSingleTop = true }
                            },
                            palette = palette,
                        )
                    }
                    composable(DashboardRoutes.CUSTOMERS) {
                        val customerViewModel: CustomerViewModel = hiltViewModel()
                        CustomersScreen(viewModel = customerViewModel)
                    }
                    composable(DashboardRoutes.PRICE_LIST) {
                        val priceListViewModel: PriceListViewModel = hiltViewModel()
                        PriceListScreen(viewModel = priceListViewModel)
                    }
                    composable(DashboardRoutes.SALES) {
                        val salesViewModel: SaleViewModel = hiltViewModel()
                        SalesScreen(viewModel = salesViewModel)
                    }
                    composable(DashboardRoutes.REPORTS) {
                        val reportsViewModel: ReportsViewModel = hiltViewModel()
                        ReportsScreen(viewModel = reportsViewModel)
                    }
                    composable(DashboardRoutes.MARKETING) {
                        val marketingViewModel: MarketingViewModel = hiltViewModel()
                        MarketingScreen(viewModel = marketingViewModel)
                    }
                    composable(DashboardRoutes.BILLING) {
                        val billingViewModel: BillingViewModel = hiltViewModel()
                        BillingScreen(viewModel = billingViewModel)
                    }
                    composable(DashboardRoutes.COLLECTIONS) {
                        val collectionsViewModel: CollectionsViewModel = hiltViewModel()
                        CollectionsScreen(viewModel = collectionsViewModel)
                    }
                    composable(DashboardRoutes.PROFILE) {
                        val profileViewModel: ProfileViewModel = hiltViewModel()
                        ProfileScreen(viewModel = profileViewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHomeScreen(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onNavigate: (String) -> Unit,
    palette: com.dismal.app.core.ui.common.UiColors,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Suite Administrativa", style = MaterialTheme.typography.labelSmall, color = palette.accent)
                    Text("Dismal Admin", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        state.email.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                    Text(
                        "Rol ${state.role.orEmpty().uppercase(Locale.getDefault())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accent,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = onRefresh, enabled = !state.isRefreshing, shape = RoundedCornerShape(14.dp)) {
                        Text(if (state.isRefreshing) "Actualizando..." else "Actualizar dashboard")
                    }
                }
            }
        }

        state.errorMessage?.let { message ->
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Error", color = MaterialTheme.colorScheme.error)
                        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        state.summary?.let { summary ->
            item {
                SummaryCardGrid(summary = summary, palette = palette)
            }
        }

        item {
            QuickActions(onNavigate = onNavigate, palette = palette)
        }

        item {
            Text("Metas prioritarias", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        }

        if (state.targets.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                ) {
                    Text(
                        "No hay metas disponibles para mostrar.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                }
            }
        } else {
            items(state.targets, key = { it.id }) { target ->
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(target.productName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (target.targetAchieved) "Cumplida" else "Activa",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (target.targetAchieved) palette.accent else palette.textSecondary,
                            )
                        }
                        Text(
                            "${target.unitsSoldCurrent}/${target.metaUnits} unidades",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Utilidad esperada $${String.format(Locale.US, "%.2f", target.expectedProfit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                        Text(
                            "Break-even ${target.breakEvenUnits} - vence ${target.deadline}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCardGrid(
    summary: com.dismal.app.domain.models.DashboardSummary,
    palette: com.dismal.app.core.ui.common.UiColors,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Ventas hoy", summary.todaySalesCount.toString(), palette, Modifier.weight(1f))
            MetricCard("Monto hoy", formatMoney(summary.todaySalesAmount), palette, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Ventas mes", summary.monthSalesCount.toString(), palette, Modifier.weight(1f))
            MetricCard("Monto mes", formatMoney(summary.monthSalesAmount), palette, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Utilidad esperada", formatMoney(summary.monthExpectedProfit), palette, Modifier.weight(1f))
            MetricCard("Cartera vencida", formatMoney(summary.overdueArBalance), palette, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    palette: com.dismal.app.core.ui.common.UiColors,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickActions(
    onNavigate: (String) -> Unit,
    palette: com.dismal.app.core.ui.common.UiColors,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Accesos rapidos", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            QuickActionRow("Crear cliente", "Alta directa y gestion de clientes", DashboardRoutes.CUSTOMERS, onNavigate, palette)
            QuickActionRow("Enviar lista de precios", "Email y WhatsApp con plantillas del sistema", DashboardRoutes.PRICE_LIST, onNavigate, palette)
            QuickActionRow("Generar venta", "Crear y confirmar venta con notificaciones", DashboardRoutes.SALES, onNavigate, palette)
            QuickActionRow("Revisar prioridades", "Resumen operativo del dia y metas pendientes", DashboardRoutes.REPORTS, onNavigate, palette)
            QuickActionRow("Ejecutar estrategia", "Productos con alerta comercial y acciones sugeridas", DashboardRoutes.MARKETING, onNavigate, palette)
            QuickActionRow("Cobrar cartera", "Seguimiento rapido a clientes con saldo pendiente", DashboardRoutes.COLLECTIONS, onNavigate, palette)
        }
    }
}

@Composable
private fun QuickActionRow(
    title: String,
    subtitle: String,
    route: String,
    onNavigate: (String) -> Unit,
    palette: com.dismal.app.core.ui.common.UiColors,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onNavigate(route) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.accentSoft),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }
    }
}

private fun menuIcon(route: String): ImageVector =
    when (route) {
        DashboardRoutes.HOME -> Icons.Filled.Home
        DashboardRoutes.CUSTOMERS -> Icons.Filled.Person
        DashboardRoutes.PRICE_LIST -> Icons.Filled.ListAlt
        DashboardRoutes.SALES -> Icons.Filled.PointOfSale
        DashboardRoutes.REPORTS -> Icons.Filled.Assessment
        DashboardRoutes.MARKETING -> Icons.Filled.Insights
        DashboardRoutes.BILLING -> Icons.Filled.ReceiptLong
        DashboardRoutes.COLLECTIONS -> Icons.Filled.Payments
        DashboardRoutes.PROFILE -> Icons.Filled.Person
        DashboardRoutes.LOGOUT -> Icons.AutoMirrored.Filled.Logout
        else -> Icons.Filled.Home
    }

private fun mobileMenuLabel(route: String): String =
    when (route) {
        DashboardRoutes.HOME -> "Inicio"
        DashboardRoutes.REPORTS -> "Reportes"
        DashboardRoutes.MARKETING -> "Plan"
        DashboardRoutes.SALES -> "Ventas"
        DashboardRoutes.PROFILE -> "Perfil"
        else -> "Menú"
    }

private fun formatMoney(value: Double): String = "$" + String.format(Locale.US, "%.2f", value)
