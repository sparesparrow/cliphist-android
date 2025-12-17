package com.clipboardhistory.di

import android.content.Context
import com.clipboardhistory.data.database.ClipboardDatabase
import com.clipboardhistory.data.database.ClipboardItemDao
import com.clipboardhistory.data.repository.ClipboardRepositoryImpl
import com.clipboardhistory.domain.repository.ClipboardRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for database-related components.
 *
 * This module provides the database instance and DAO for dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the application context.
     *
     * @param context The application context
     * @return The application context
     */
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
}

/**
 * Dependency injection module for database-related components.
 *
 * This module provides the database instance and DAO for dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /**
     * Provides the clipboard database instance.
     *
     * @param context The application context
     * @return The clipboard database instance
     */
    @Provides
    @Singleton
    fun provideClipboardDatabase(
        @ApplicationContext context: Context,
    ): ClipboardDatabase {
        return ClipboardDatabase.create(context)
    }

    /**
     * Provides the clipboard item DAO.
     *
     * @param database The clipboard database instance
     * @return The clipboard item DAO
     */
    @Provides
    fun provideClipboardItemDao(database: ClipboardDatabase): ClipboardItemDao {
        return database.clipboardItemDao()
    }
}

/**
 * Dependency injection module for repository-related components.
 *
 * This module provides repository implementations for dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    /**
     * Provides the clipboard repository implementation.
     *
     * @param repositoryImpl The repository implementation
     * @return The clipboard repository interface
     */
    @Provides
    @Singleton
    fun provideClipboardRepository(repositoryImpl: ClipboardRepositoryImpl): ClipboardRepository {
        return repositoryImpl
    }
}
