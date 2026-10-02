package com.dismal.app.ui.login

import com.dismal.app.TestDispatcherRule
import com.dismal.app.data.auth.AuthApi
import com.dismal.app.data.auth.SessionManager
import com.dismal.app.data.user.UserRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val authApi = mockk<AuthApi>()
    private val userRepository = mockk<UserRepository>(relaxed = true)

    @Test
    fun `does not auto enter dashboard when backend is unreachable`() =
        runTest {
            coEvery { authApi.authenticate(any()) } throws RuntimeException("timeout")
            every { sessionManager.canUseOfflineLogin() } returns true

            val viewModel = LoginViewModel(sessionManager, authApi, userRepository)
            viewModel.onEmailChange("admin@dismal.com")
            viewModel.onPasswordChange("secret")

            viewModel.login()

            assertFalse(viewModel.navigateToDashboard)
            assertEquals(
                "No se pudo conectar al servidor. Usa Entrar offline para continuar.",
                viewModel.uiState.errorMessage,
            )
        }

    @Test
    fun `offline login requires custom pin`() =
        runTest {
            every { sessionManager.canUseOfflineLogin() } returns true
            every { sessionManager.hasCustomOfflinePin() } returns false

            val viewModel = LoginViewModel(sessionManager, authApi, userRepository)

            viewModel.loginOffline("1234")

            assertFalse(viewModel.navigateToDashboard)
            assertEquals(
                "Configura un PIN offline desde Perfil antes de usar este modo.",
                viewModel.uiState.errorMessage,
            )
        }
}
