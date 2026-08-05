package br.com.oficjus.drive.di

import br.com.oficjus.drive.data.repository.AuthRepositoryImpl
import br.com.oficjus.drive.data.repository.CnefeRepositoryImpl
import br.com.oficjus.drive.data.repository.EnderecoRepositoryImpl
import br.com.oficjus.drive.data.repository.ViaCepRepositoryImpl
import br.com.oficjus.drive.domain.repository.AuthRepository
import br.com.oficjus.drive.domain.repository.CnefeRepository
import br.com.oficjus.drive.domain.repository.EnderecoRepository
import br.com.oficjus.drive.domain.repository.ViaCepRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCnefeRepository(impl: CnefeRepositoryImpl): CnefeRepository

    @Binds
    @Singleton
    abstract fun bindViaCepRepository(impl: ViaCepRepositoryImpl): ViaCepRepository

    @Binds
    @Singleton
    abstract fun bindEnderecoRepository(impl: EnderecoRepositoryImpl): EnderecoRepository
}