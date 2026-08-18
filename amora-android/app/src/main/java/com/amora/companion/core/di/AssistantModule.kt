package com.amora.companion.core.di

import com.amora.companion.core.assistant.actions.ActionExecutor
import com.amora.companion.core.assistant.actions.IActionExecutor
import com.amora.companion.core.assistant.intent.IIntentEngine
import com.amora.companion.core.assistant.intent.IntentEngine
import com.amora.companion.core.assistant.speech.AndroidSpeechOutputManager
import com.amora.companion.core.assistant.speech.ISpeechOutputManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantModule {

    @Binds
    @Singleton
    abstract fun bindSpeechOutputManager(impl: AndroidSpeechOutputManager): ISpeechOutputManager

    @Binds
    @Singleton
    abstract fun bindIntentEngine(impl: IntentEngine): IIntentEngine

    @Binds
    @Singleton
    abstract fun bindActionExecutor(impl: ActionExecutor): IActionExecutor
}
