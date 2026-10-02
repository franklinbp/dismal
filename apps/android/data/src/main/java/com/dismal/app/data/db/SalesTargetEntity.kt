package com.dismal.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dismal.app.domain.models.SalesTarget

@Entity(tableName = "sales_targets")
data class SalesTargetEntity(
    @PrimaryKey val id: String,
    val softwareId: String,
    val softwareName: String?,
    val metaUnits: Int,
    val salePrice: Double?,
    val variableCost: Double?,
    val fixedCostProduct: Double?,
    val unitsSoldCurrent: Int?,
    val deadline: String,
    val notes: String?,
    val marginUnit: Double?,
    val targetRevenue: Double?,
    val variableCostTotal: Double?,
    val contributionTotal: Double?,
    val breakEvenUnits: Int?,
    val breakEvenRevenue: Double?,
    val expectedProfit: Double?,
    val profitable: Boolean?,
    val targetAchieved: Boolean?,
    val fixedCostApplied: Double?,
    val createdAt: String?,
    val updatedAt: String?,
)

fun SalesTargetEntity.asDomainModel(): SalesTarget {
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

fun SalesTarget.asDatabaseModel(): SalesTargetEntity {
    return SalesTargetEntity(
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
