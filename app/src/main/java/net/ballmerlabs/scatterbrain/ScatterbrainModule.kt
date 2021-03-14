package net.ballmerlabs.scatterbrain

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScatterbrainModule {
    @Singleton
    @Binds
    abstract fun bindServiceConnectionRepository(
            serviceConnectionRepository: ServiceConnectionRepositoryImpl
    ) : ServiceConnectionRepository
}