package com.amora.companion

import com.amora.companion.core.data.network.AmoraApiService
import com.amora.companion.core.data.network.HealthApiResponse
import com.amora.companion.core.data.preferences.UserPreferencesRepository
import com.amora.companion.feature.home.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val apiService: AmoraApiService = mock(AmoraApiService::class.java)
    private val preferencesRepository: UserPreferencesRepository = mock(UserPreferencesRepository::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        `when`(preferencesRepository.isMasterSwitchOn).thenReturn(flowOf(true))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checkBackendHealth updates state to Connected on HTTP 200`() = runTest {
        val healthResponse = HealthApiResponse(
            status = "Connected",
            groq_api_valid = true,
            database_connected = true,
            environment = "development",
            backend_version = "1.0.0"
        )
        `when`(apiService.checkHealth()).thenReturn(Response.success(healthResponse))

        val viewModel = DashboardViewModel(apiService, preferencesRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Connected", viewModel.uiState.value.backendStatus)
    }
}
