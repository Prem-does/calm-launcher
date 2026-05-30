package com.calmlauncher.data.repository

import com.calmlauncher.data.db.RiskStateDao
import com.calmlauncher.data.db.toDomain
import com.calmlauncher.data.db.toEntity
import com.calmlauncher.domain.model.RiskState
import com.calmlauncher.domain.repository.RiskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Single-row risk state backed by Room. Emits a [RiskState] with CALM defaults when the
 * row has never been written.
 */
class RiskRepositoryImpl @Inject constructor(
    private val riskStateDao: RiskStateDao,
) : RiskRepository {

    override val state: Flow<RiskState> =
        riskStateDao.observe().map { it?.toDomain() ?: RiskState() }

    override suspend fun current(): RiskState =
        riskStateDao.get()?.toDomain() ?: RiskState()

    override suspend fun set(state: RiskState) {
        riskStateDao.upsert(state.toEntity())
    }
}
