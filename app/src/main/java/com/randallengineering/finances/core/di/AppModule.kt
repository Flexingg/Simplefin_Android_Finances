package com.randallengineering.finances.core.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
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

import com.randallengineering.finances.data.repository.AmazonRepository
import com.randallengineering.finances.ui.screens.insights.InsightsViewModel

val appModule = module {
    // Firebase instances
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }
    single { FirebaseFunctions.getInstance() }

    // Repositories (with local storage & offline fallback)
    single { TransactionRepository(androidContext(), getOrNull()) }
    single { RuleRepository(androidContext(), getOrNull()) }
    single { BudgetRepository(androidContext(), getOrNull()) }
    single { GoalRepository(androidContext(), getOrNull()) }
    single { StorageRepository(get()) }
    single { CategoryRepository(androidContext(), getOrNull()) }
    single { SimpleFinRepository(androidContext(), get(), getOrNull(), getOrNull()) }
    single { AmazonRepository(androidContext(), getOrNull(), getOrNull()) }
    single { com.randallengineering.finances.data.repository.GamificationRepository(androidContext(), getOrNull()) }

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
    viewModel { SimpleFinOnboardingViewModel(get(), get()) }
    viewModel { TransactionViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { RulesViewModel(get(), get()) }
    viewModel { BudgetsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { InsightsViewModel(get()) }
    viewModel { GoalsViewModel(get(), get()) }
    viewModel { AiAdvisorViewModel(get(), get(), get(), get()) }
    viewModel { AiChatbotViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { com.randallengineering.finances.ui.screens.quest.QuestPathViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { com.randallengineering.finances.ui.screens.queue.ActionQueueViewModel(get(), get(), get(), get()) }
    viewModel { com.randallengineering.finances.ui.screens.gear.GearLoadoutViewModel(get()) }
}
