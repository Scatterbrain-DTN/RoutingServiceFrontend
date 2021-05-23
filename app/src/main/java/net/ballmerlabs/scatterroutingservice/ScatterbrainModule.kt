package net.ballmerlabs.scatterroutingservice

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScatterbrainModule {

    @Provides
    @Singleton
    fun providesComponent(@ApplicationContext context: Context): ScatterbrainApi {
        return ScatterbrainApi(context)
    }

    @Provides
    @Singleton
    fun provideSdk(component: ScatterbrainApi): BinderWrapper {
        return component.binderWrapper
    }

    @Provides
    @Singleton
    fun providesBroadcastReceiver(component: ScatterbrainApi): ScatterbrainBroadcastReceiver {
        return component.broadcastReceiver
    }
}