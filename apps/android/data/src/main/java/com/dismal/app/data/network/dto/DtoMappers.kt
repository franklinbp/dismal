package com.dismal.app.data.network.dto

import com.dismal.app.domain.models.AdminProduct
import com.dismal.app.domain.models.Customer
import com.dismal.app.domain.models.DashboardSummary
import com.dismal.app.domain.models.DashboardTarget
import com.dismal.app.domain.models.License
import com.dismal.app.domain.models.MarketingAnalysis
import com.dismal.app.domain.models.Product
import com.dismal.app.domain.models.SalesTarget
import com.dismal.app.domain.models.SalesTargetSummary

fun LicenseDto.asDomainModel(): License {
    return License(
        id = id,
        licenseKey = licenseKey,
        softwareId = softwareId,
        softwareName = softwareName,
        purchasePrice = purchasePrice,
        maxActivations = maxActivations,
        usedActivations = usedActivations,
        status = status,
        ownerId = ownerId,
        ownerEmail = ownerEmail,
        available = available,
    )
}

fun ProductDto.asDomainModel(): Product {
    return Product(
        id = id,
        name = name,
        description = description.orEmpty(),
        price = price,
        platform = platform,
        imageUrl = imageUrl,
        licenses = licenses.map { it.asDomainModel() },
    )
}

fun SalesTargetDto.asDomainModel(): SalesTarget {
    return SalesTarget(
        id = id,
        softwareId = softwareId,
        softwareName = softwareName,
        metaUnits = metaUnits,
        salePrice = salePrice,
        variableCost = variableCost,
        fixedCostProduct = fixedCostProduct,
        unitsSoldCurrent = unitsSoldCurrent,
        deadline = deadline,
        notes = notes,
        marginUnit = marginUnit,
        targetRevenue = targetRevenue,
        variableCostTotal = variableCostTotal,
        contributionTotal = contributionTotal,
        breakEvenUnits = breakEvenUnits,
        breakEvenRevenue = breakEvenRevenue,
        expectedProfit = expectedProfit,
        profitable = profitable,
        targetAchieved = targetAchieved,
        fixedCostApplied = fixedCostApplied,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun AdminUserResponseDto.asCustomer(): Customer {
    val fullName = listOfNotNull(firstname, lastname).joinToString(" ").ifBlank { email }
    return Customer(
        id = id,
        name = fullName,
        email = email,
        phone = phone,
        updatedAt = updatedAt ?: updatedAtAlt,
        saldoActual = arSummary?.saldoActual,
        diasAtraso = arSummary?.diasAtraso ?: 0L,
        estado = arSummary?.estado,
    )
}

fun AdminProductDto.asDomainModel(): AdminProduct {
    return AdminProduct(
        id = id,
        name = name,
        description = description.orEmpty(),
        price = price,
        platform = platform,
        imageUrl = imageUrl,
        ecFinalPrice = ecFinalPrice,
        ecDistributorPrice = ecDistributorPrice,
        peFinalPrice = peFinalPrice,
        peDistributorPrice = peDistributorPrice,
    )
}

fun DashboardSummaryDto.asDomainModel(): DashboardSummary {
    return DashboardSummary(
        todaySalesCount = todaySalesCount,
        todaySalesAmount = todaySalesAmount,
        todayProfitAmount = todayProfitAmount,
        monthSalesCount = monthSalesCount,
        monthSalesAmount = monthSalesAmount,
        monthProfitAmount = monthProfitAmount,
        monthExpectedProfit = monthExpectedProfit,
        overdueArCount = overdueArCount,
        overdueArBalance = overdueArBalance,
        openArCount = openArCount,
        openArBalance = openArBalance,
        nonProfitableProductsCount = nonProfitableProductsCount,
        outboxFailedCount = outboxFailedCount,
    )
}

fun DashboardTargetDto.asDomainModel(): DashboardTarget {
    return DashboardTarget(
        id = id,
        productName = productName,
        metaUnits = metaUnits,
        unitsSoldCurrent = unitsSoldCurrent,
        marginUnit = marginUnit,
        expectedProfit = expectedProfit,
        breakEvenUnits = breakEvenUnits,
        deadline = deadline,
        profitable = profitable,
        targetAchieved = targetAchieved,
    )
}

fun MarketingAnalysisDto.asDomainModel(): MarketingAnalysis {
    return MarketingAnalysis(
        productId = productId,
        productName = productName,
        state = state,
        diagnosis = diagnosis,
        priority = priority,
        suggestedActions = suggestedActions,
        explanation = explanation,
    )
}

fun SalesTargetSummaryDto.asDomainModel(): SalesTargetSummary {
    return SalesTargetSummary(
        totalTargets = totalTargets,
        achievedTargets = achievedTargets,
        pendingTargets = pendingTargets,
        totalMetaUnits = totalMetaUnits,
        totalTargetRevenue = totalTargetRevenue,
        totalContribution = totalContribution,
        totalFixedCost = totalFixedCost,
        totalExpectedProfit = totalExpectedProfit,
    )
}
