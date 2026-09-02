package com.randallengineering.finances.core.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.randallengineering.finances.core.auth.SessionManager
import com.randallengineering.finances.core.firebase.FirebaseEmulator.connectEmulatorIfPlaceholder
import com.randallengineering.finances.data.repository.BudgetRepository
import com.randallengineering.finances.data.repository.GoalRepository
import com.randallengineering.finances.data.repository.RuleRepository
import com.randallengineering.finances.data.repository.SimpleFinRepository
import com.randallengineering.finances.data.repository.StorageRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.usecase.AiAdvisorUseCase
import com.randallengineering.finances.domain.usecase.BudgetCalculatorUseCase
import com.randallengineering.finances.domain.usecase.GoalPacingUseCase
import com.randallengineering.finances.domain.usecase.RuleMatcherUseCase
import com.randallengineering.finances.domain.usecase.SimpleFinSyncUseCase
import com.randallengineering.finances.domain.usecase.TransactionSplitUseCase
import com.randallengineering.finances.ui.screens.ai.AiAdvisorViewModel
import com.randallengineering.finances.ui.screens.budgets.BudgetsViewModel
import com.randallengineering.finances.ui.screens.goals.GoalsViewModel
import com.randallengineering.finances.ui.screens.onboarding.SimpleFinOnboardingViewModel
import com.randallengineering.finances.ui.screens.rules.RulesViewModel
import com.randallengineering.finances.ui.screens.transactions.TransactionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

import com.randallengineering.finances.ui.screens.settings.SettingsViewModel

import org.koin.android.ext.koin.androidContext

import com.randallengineering.finances.data.repository.CategoryRepository

import com.randallengineering.finances.core.ai.FinancialMcpTools
import com.randallengineering.finances.domain.usecase.AiChatbotUseCase
import com.randallengineering.finances.ui.screens.ai.AiChatbotViewModel

import com.randallengineering.finances.data.repository.AccountRepository
import com.randallengineering.finances.data.repository.AmazonRepository
import com.randallengineering.finances.data.repository.SyncStatusRepository
import com.randallengineering.finances.data.local.FinanceDatabase
import com.randallengineering.finances.ui.screens.insights.InsightsViewModel

val appModule = module {
    // Firebase instances
    // When running against the placeholder demo project, connect Firestore to the
    // local emulator so the sync loop is testable without a real Firebase project.
    single { FirebaseFirestore.getInstance().also { it.connectEmulatorIfPlaceholder() } }
    single { FirebaseStorage.getInstance() }
    single { FirebaseFunctions.getInstance() }

    // Cross-platform auth + sync scope
    single { SessionManager(androidContext(), get()) }
    single { com.randallengineering.finances.core.prefs.DashboardLayoutRepository(androidContext()) }

    // Local Room database
    single { androidx.room.Room.databaseBuilder(androidContext(), FinanceDatabase::class.java, "randall_finances.db").build() }
    single { get<FinanceDatabase>().transactionDao() }

    // Repositories (with local storage & offline fallback)
    single { TransactionRepository(androidContext(), get(), getOrNull()) }
    single { RuleRepository(androidContext(), getOrNull()) }
    single { BudgetRepository(androidContext(), getOrNull()) }
    single { GoalRepository(androidContext(), getOrNull()) }
    single { StorageRepository(get()) }
    single { CategoryRepository(androidContext(), getOrNull()) }
    single { AccountRepository(androidContext()) }
    single { SyncStatusRepository(androidContext()) }
    single { SimpleFinRepository(androidContext(), get(), get(), getOrNull(), getOrNull()) }
    single { AmazonRepository(androidContext()) }

    // MCP Tools Suite
    single { FinancialMcpTools(get(), get(), get(), get(), get(), get(), get()) }

    // UseCases
    single { RuleMatcherUseCase() }
    single { BudgetCalculatorUseCase() }
    single { GoalPacingUseCase() }
    single { TransactionSplitUseCase(get()) }
    single { AiAdvisorUseCase(get(), get()) }
    single { SimpleFinSyncUseCase(get()) }
    single { AiChatbotUseCase(get()) }

    // ViewModels
    viewModel { com.randallengineering.finances.core.auth.AuthViewModel(get()) }
    viewModel { SimpleFinOnboardingViewModel(get(), get()) }
    viewModel { com.randallengineering.finances.ui.screens.dashboard.DashboardViewModel(get(), get(), get(), get(), get()) }
    viewModel { TransactionViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { RulesViewModel(get(), get()) }
    viewModel { BudgetsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { InsightsViewModel(get()) }
    viewModel { GoalsViewModel(get(), get()) }
    viewModel { AiAdvisorViewModel(get(), get(), get(), get()) }
    viewModel { AiChatbotViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { com.randallengineering.finances.ui.screens.queue.ActionQueueViewModel(get(), get(), get()) }
}
