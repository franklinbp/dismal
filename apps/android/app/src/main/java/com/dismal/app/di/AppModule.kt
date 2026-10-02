package com.dismal.app.di

import android.content.Context
import com.dismal.app.BuildConfig
import com.dismal.app.data.auth.AuthApi
import com.dismal.app.data.auth.TokenStore
import com.dismal.app.data.db.AppDatabase
import com.dismal.app.data.db.CustomerDao
import com.dismal.app.data.db.LicenseDao
import com.dismal.app.data.db.ProductDao
import com.dismal.app.data.db.SaleDao
import com.dismal.app.data.db.SalesTargetDao
import com.dismal.app.data.db.SyncOutboxDao
import com.dismal.app.data.network.AuthInterceptor
import com.dismal.app.data.network.AccountsReceivableApi
import com.dismal.app.data.network.AdminCatalogApi
import com.dismal.app.data.network.CustomerApi
import com.dismal.app.data.network.DashboardApi
import com.dismal.app.data.network.InventoryApi
import com.dismal.app.data.network.InvoicesApi
import com.dismal.app.data.network.MarketingIntelligenceApi
import com.dismal.app.data.network.PaymentsApi
import com.dismal.app.data.network.ReportsApi
import com.dismal.app.data.network.SalesApi
import com.dismal.app.data.network.SalesTargetApi
import com.dismal.app.data.network.StoreApi
import com.dismal.app.data.repository.AdminCatalogRepository
import com.dismal.app.data.repository.AccountsReceivableRepository
import com.dismal.app.data.repository.CustomerRepository
import com.dismal.app.data.repository.DashboardRepository
import com.dismal.app.data.repository.InventoryRepository
import com.dismal.app.data.repository.InvoiceRepository
import com.dismal.app.data.repository.MarketingIntelligenceRepository
import com.dismal.app.data.repository.PriceListRepository
import com.dismal.app.data.repository.SaleRepository
import com.dismal.app.data.repository.SalesTargetRepository
import com.dismal.app.data.repository.StoreRepository
import com.dismal.app.data.repository.SyncOutboxRepository
import com.dismal.app.data.user.UserApi
import com.dismal.app.data.user.UserRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideTokenStore(
        @ApplicationContext context: Context,
    ): TokenStore {
        return TokenStore(context)
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenStore: TokenStore): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .apply {
                if (BuildConfig.DEBUG) {
                    val logging =
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                            redactHeader("Authorization")
                        }
                    addInterceptor(logging)
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        moshi: Moshi,
        okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
    }

    @Provides
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    fun provideStoreApi(retrofit: Retrofit): StoreApi = retrofit.create(StoreApi::class.java)

    @Provides
    fun provideAdminCatalogApi(retrofit: Retrofit): AdminCatalogApi = retrofit.create(AdminCatalogApi::class.java)

    @Provides
    fun provideSalesApi(retrofit: Retrofit): SalesApi = retrofit.create(SalesApi::class.java)

    @Provides
    fun provideAccountsReceivableApi(retrofit: Retrofit): AccountsReceivableApi =
        retrofit.create(AccountsReceivableApi::class.java)

    @Provides
    fun providePaymentsApi(retrofit: Retrofit): PaymentsApi = retrofit.create(PaymentsApi::class.java)

    @Provides
    fun provideInventoryApi(retrofit: Retrofit): InventoryApi = retrofit.create(InventoryApi::class.java)

    @Provides
    fun provideInvoicesApi(retrofit: Retrofit): InvoicesApi = retrofit.create(InvoicesApi::class.java)

    @Provides
    fun provideSalesTargetApi(retrofit: Retrofit): SalesTargetApi = retrofit.create(SalesTargetApi::class.java)

    @Provides
    fun provideCustomerApi(retrofit: Retrofit): CustomerApi = retrofit.create(CustomerApi::class.java)

    @Provides
    fun provideDashboardApi(retrofit: Retrofit): DashboardApi = retrofit.create(DashboardApi::class.java)

    @Provides
    fun provideReportsApi(retrofit: Retrofit): ReportsApi = retrofit.create(ReportsApi::class.java)

    @Provides
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    fun provideMarketingIntelligenceApi(retrofit: Retrofit): MarketingIntelligenceApi =
        retrofit.create(MarketingIntelligenceApi::class.java)

    @Provides
    @Singleton
    fun provideUserRepository(
        userApi: UserApi,
        tokenStore: TokenStore,
    ): UserRepository {
        return UserRepository(userApi, tokenStore)
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideProductDao(database: AppDatabase): ProductDao = database.productDao()

    @Provides
    fun provideCustomerDao(database: AppDatabase): CustomerDao = database.customerDao()

    @Provides
    fun provideSaleDao(database: AppDatabase): SaleDao = database.saleDao()

    @Provides
    @Singleton
    fun provideStoreRepository(
        storeApi: StoreApi,
        productDao: ProductDao,
    ): StoreRepository {
        return StoreRepository(storeApi, productDao)
    }

    @Provides
    @Singleton
    fun provideAdminCatalogRepository(adminCatalogApi: AdminCatalogApi): AdminCatalogRepository {
        return AdminCatalogRepository(adminCatalogApi)
    }

    @Provides
    fun provideLicenseDao(database: AppDatabase): LicenseDao = database.licenseDao()

    @Provides
    fun provideSalesTargetDao(database: AppDatabase): SalesTargetDao = database.salesTargetDao()

    @Provides
    fun provideSyncOutboxDao(database: AppDatabase): SyncOutboxDao = database.syncOutboxDao()

    @Provides
    @Singleton
    fun provideCustomerRepository(
        customerDao: CustomerDao,
        customerApi: CustomerApi,
        syncOutboxRepository: SyncOutboxRepository,
    ): CustomerRepository {
        return CustomerRepository(customerDao, customerApi, syncOutboxRepository)
    }

    @Provides
    @Singleton
    fun provideSaleRepository(
        saleDao: SaleDao,
        salesApi: SalesApi,
        paymentsApi: PaymentsApi,
        syncOutboxRepository: SyncOutboxRepository,
    ): SaleRepository {
        return SaleRepository(saleDao, salesApi, paymentsApi, syncOutboxRepository)
    }

    @Provides
    @Singleton
    fun provideAccountsReceivableRepository(
        accountsReceivableApi: AccountsReceivableApi,
    ): AccountsReceivableRepository {
        return AccountsReceivableRepository(accountsReceivableApi)
    }

    @Provides
    @Singleton
    fun provideInventoryRepository(
        inventoryApi: InventoryApi,
        licenseDao: LicenseDao,
    ): InventoryRepository {
        return InventoryRepository(inventoryApi, licenseDao)
    }

    @Provides
    @Singleton
    fun provideInvoiceRepository(invoicesApi: InvoicesApi): InvoiceRepository {
        return InvoiceRepository(invoicesApi)
    }

    @Provides
    @Singleton
    fun provideSalesTargetRepository(
        salesTargetApi: SalesTargetApi,
        salesTargetDao: SalesTargetDao,
    ): SalesTargetRepository {
        return SalesTargetRepository(salesTargetApi, salesTargetDao)
    }

    @Provides
    @Singleton
    fun provideDashboardRepository(dashboardApi: DashboardApi): DashboardRepository {
        return DashboardRepository(dashboardApi)
    }

    @Provides
    @Singleton
    fun providePriceListRepository(reportsApi: ReportsApi): PriceListRepository {
        return PriceListRepository(reportsApi)
    }

    @Provides
    @Singleton
    fun provideMarketingIntelligenceRepository(
        marketingIntelligenceApi: MarketingIntelligenceApi,
    ): MarketingIntelligenceRepository {
        return MarketingIntelligenceRepository(marketingIntelligenceApi)
    }

    @Provides
    @Singleton
    fun provideSyncOutboxRepository(syncOutboxDao: SyncOutboxDao): SyncOutboxRepository {
        return SyncOutboxRepository(syncOutboxDao)
    }
}
