package net.ballmerlabs.scatterroutingservice

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.DaggerSdkComponent
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver
import net.ballmerlabs.scatterbrainsdk.SdkComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScatterbrainModule {

    @Provides
    @Singleton
    fun providesComponent(@ApplicationContext context: Context): SdkComponent {
        return DaggerSdkComponent.builder()
                .applicationContext(context)!!.build()!!
    }

    @Provides
    @Singleton
    fun provideSdk(component: SdkComponent): BinderWrapper {
        return component.sdk()
    }

    @Provides
    @Singleton
    fun providesBroadcastReceiver(component: SdkComponent): ScatterbrainBroadcastReceiver {
        return component.broadcastReceiver()
    }
}