package com.randallengineering.finances.domain.usecase

import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.repository.SimpleFinRepository

class SimpleFinSyncUseCase(
    private val simpleFinRepository: SimpleFinRepository
) {
    suspend fun claimToken(setupToken: String): Resource<String> {
        if (setupToken.isBlank()) {
            return Resource.Error("Setup token cannot be empty")
        }
        return simpleFinRepository.claimSetupToken(setupToken)
    }

    suspend fun syncNow(daysBack: Int = 90): Resource<List<String>> {
        return simpleFinRepository.triggerSync(daysBack)
    }
}
