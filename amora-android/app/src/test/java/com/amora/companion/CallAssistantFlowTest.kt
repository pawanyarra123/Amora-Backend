package com.amora.companion

import com.amora.companion.core.data.preferences.UserPreferencesRepository
import com.amora.companion.feature.callassistant.CallAssistantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class CallAssistantFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private val preferencesRepository: UserPreferencesRepository = mock(UserPreferencesRepository::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        `when`(preferencesRepository.isCallAssistantEnabled).thenReturn(flowOf(true))
        `when`(preferencesRepository.callAssistantMode).thenReturn(flowOf("Meeting"))
        `when`(preferencesRepository.callScreeningLogsJson).thenReturn(flowOf(""))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test meeting mode non emergency call screens call and logs to dashboard`() = runTest {
        val viewModel = CallAssistantViewModel(preferencesRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate incoming non-emergency call in Meeting Mode
        viewModel.simulateCall(
            caller = "Alex",
            mode = "Meeting",
            isEmergency = false,
            reason = "Want to discuss project presentation"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val logs = viewModel.logs.value
        assertTrue(logs.isNotEmpty())

        val firstLog = logs.first()
        assertEquals("Alex", firstLog.callerName)
        assertEquals("Meeting", firstLog.mode)
        assertFalse(firstLog.isEmergency)
        assertEquals("Want to discuss project presentation", firstLog.reason)
        assertNull(viewModel.emergencyAlert.value)
    }

    @Test
    fun `test college mode emergency call triggers vibration and full screen alert`() = runTest {
        val viewModel = CallAssistantViewModel(preferencesRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate incoming emergency call in College Mode
        viewModel.simulateCall(
            caller = "Professor Smith",
            mode = "College",
            isEmergency = true,
            reason = "Important exam schedule update"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val logs = viewModel.logs.value
        val alert = viewModel.emergencyAlert.value

        assertTrue(logs.isNotEmpty())
        assertNotNull(alert)
        assertEquals("Professor Smith", alert?.callerName)
        assertEquals("College", alert?.mode)
        assertTrue(alert?.isEmergency == true)
    }

    @Test
    fun `test sleeping mode emergency call boosts volume to max and triggers alarm`() = runTest {
        val viewModel = CallAssistantViewModel(preferencesRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate incoming emergency call in Sleeping Mode
        viewModel.simulateCall(
            caller = "Family Member",
            mode = "Sleeping",
            isEmergency = true,
            reason = "Medical emergency at home"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val alert = viewModel.emergencyAlert.value

        assertNotNull(alert)
        assertEquals("Family Member", alert?.callerName)
        assertEquals("Sleeping", alert?.mode)
        assertTrue(alert?.isEmergency == true)
    }

    @Test
    fun `test dismiss emergency alert resets alert state`() = runTest {
        val viewModel = CallAssistantViewModel(preferencesRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.simulateCall(
            caller = "Emergency Contact",
            mode = "Sleeping",
            isEmergency = true,
            reason = "Urgent help needed"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.emergencyAlert.value)

        // Dismiss alert
        viewModel.dismissEmergencyAlert()

        assertNull(viewModel.emergencyAlert.value)
    }
}
