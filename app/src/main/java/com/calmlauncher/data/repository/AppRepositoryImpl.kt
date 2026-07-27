package com.calmlauncher.data.repository

import com.calmlauncher.data.db.AppMetaDao
import com.calmlauncher.data.db.entity.AppMetaEntity
import com.calmlauncher.data.db.resolveDistracting
import com.calmlauncher.data.db.toAppCategory
import com.calmlauncher.data.system.LauncherAppCatalog
import com.calmlauncher.di.IoDispatcher
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.model.LauncherTool
import com.calmlauncher.domain.repository.AppRepository
import com.calmlauncher.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The installed-app catalog joined with per-app launcher metadata and the favourites
 * list from settings. The live [LauncherAppCatalog] supplies labels/packages/system flag;
 * Room supplies hidden/category/favourite overrides. Everything heavy runs on
 * [dispatcher]; flows recompute when either metadata or settings change.
 */
class AppRepositoryImpl @Inject constructor(
    private val catalog: LauncherAppCatalog,
    private val appMetaDao: AppMetaDao,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : AppRepository {

    override fun observeApps(): Flow<List<AppEntry>> =
        combine(
            appMetaDao.observeAll(),
            settingsRepository.settings,
        ) { metaRows, settings ->
            val meta = metaRows.associateBy { it.packageName }
            val favoritePackages = resolveFavoritePackages(settings.favorites).toSet()
            catalog.installedApps().map { base ->
                base.applyMeta(meta[base.packageName], favoritePackages)
            }.sortedBy { it.label.lowercase() }
        }.flowOn(dispatcher)

    override fun observeVisibleApps(): Flow<List<AppEntry>> =
        observeApps().map { apps -> apps.filter { !it.isHidden } }

    override fun observeFavorites(): Flow<List<AppEntry>> =
        combine(
            appMetaDao.observeAll(),
            settingsRepository.settings,
        ) { metaRows, settings ->
            val meta = metaRows.associateBy { it.packageName }
            val orderedPackages = resolveFavoritePackages(settings.favorites)
            val byPackage = catalog.installedApps().associateBy { it.packageName }
            val favoriteSet = orderedPackages.toSet()
            // Preserve the settings order; drop favourites that aren't currently installed.
            orderedPackages.mapNotNull { pkg ->
                byPackage[pkg]?.applyMeta(meta[pkg], favoriteSet)
            }
        }.flowOn(dispatcher)

    override suspend fun getApp(packageName: String): AppEntry? = withContext(dispatcher) {
        val base = catalog.installedApps().firstOrNull { it.packageName == packageName }
            ?: return@withContext null
        val settings = settingsRepository.current()
        val favoritePackages = resolveFavoritePackages(settings.favorites).toSet()
        base.applyMeta(appMetaDao.get(packageName), favoritePackages)
    }

    override suspend fun search(query: String): List<AppEntry> = withContext(dispatcher) {
        val trimmed = query.trim()
        val meta = appMetaDao.getAll().associateBy { it.packageName }
        val settings = settingsRepository.current()
        val favoritePackages = resolveFavoritePackages(settings.favorites).toSet()
        // Intentionally searches ALL apps including hidden ones (Minimal Social Layer).
        val all = catalog.installedApps().map { it.applyMeta(meta[it.packageName], favoritePackages) }
        if (trimmed.isEmpty()) return@withContext all
        val needle = trimmed.lowercase()
        all.filter {
            it.label.lowercase().contains(needle) || it.packageName.lowercase().contains(needle)
        }.sortedWith(
            compareByDescending<AppEntry> { it.label.lowercase().startsWith(needle) }
                .thenBy { it.label.lowercase() },
        )
    }

    override suspend fun refreshFromDevice() = withContext(dispatcher) {
        val installed = catalog.installedApps()
        val existing = appMetaDao.getAll().associateBy { it.packageName }

        // 1) Ensure a metadata row exists for every installed app, auto-categorising new ones.
        val newRows = installed
            .filter { it.packageName !in existing }
            .map { app ->
                AppMetaEntity(
                    packageName = app.packageName,
                    category = catalog.categorize(app.packageName).name,
                    isHidden = false,
                    isFavorite = false,
                    favoriteOrder = -1,
                    isDistractingOverride = null,
                )
            }
        if (newRows.isNotEmpty()) {
            appMetaDao.insertIgnore(newRows)
        }

        // 2) Seed default favourites if the user has none set yet.
        seedDefaultFavoritesIfNeeded()
    }

    override suspend fun setHidden(packageName: String, hidden: Boolean) = withContext(dispatcher) {
        ensureMetaRow(packageName)
        appMetaDao.setHidden(packageName, hidden)
    }

    override suspend fun setFavorite(packageName: String, favorite: Boolean) = withContext(dispatcher) {
        ensureMetaRow(packageName)
        appMetaDao.setFavorite(packageName, favorite)
        // Keep settings.favorites (the ordered home list) in sync.
        settingsRepository.update { settings ->
            val current = settings.favorites.toMutableList()
            if (favorite) {
                if (packageName !in current) current.add(packageName)
            } else {
                current.remove(packageName)
            }
            settings.copy(favorites = current)
        }
        // Maintain favoriteOrder to match the new settings order.
        syncFavoriteOrderFromSettings()
    }

    override suspend fun setFavoriteOrder(packageNames: List<String>) = withContext(dispatcher) {
        packageNames.forEach { ensureMetaRow(it) }
        // Persist the ordered list as the source of truth in settings...
        settingsRepository.update { it.copy(favorites = packageNames) }
        // ...and mirror into AppMeta (isFavorite + favoriteOrder) for query convenience.
        val ordered = packageNames.toSet()
        val all = appMetaDao.getAll()
        all.forEach { row ->
            val shouldBeFavorite = row.packageName in ordered
            if (row.isFavorite != shouldBeFavorite) {
                appMetaDao.setFavorite(row.packageName, shouldBeFavorite)
            }
        }
        packageNames.forEachIndexed { index, pkg ->
            appMetaDao.setFavorite(pkg, true)
            appMetaDao.setFavoriteOrder(pkg, index)
        }
    }

    override suspend fun setCategory(packageName: String, category: AppCategory) = withContext(dispatcher) {
        ensureMetaRow(packageName)
        appMetaDao.setCategory(packageName, category.name)
    }

    // -- internals ---------------------------------------------------------------

    /**
     * Resolve the favourites list (which may contain [LauncherTool] placeholder keys like
     * "phone"/"messages"/"camera" before the first device refresh) into concrete package
     * names, de-duplicated and order-preserving. Unresolved placeholders are dropped.
     */
    private fun resolveFavoritePackages(favorites: List<String>): List<String> {
        val out = LinkedHashSet<String>()
        for (entry in favorites) {
            val tool = TOOL_KEYS[entry.lowercase()]
            val pkg = if (tool != null) catalog.resolveTool(tool) else entry
            if (pkg != null) out.add(pkg)
        }
        return out.toList()
    }

    private fun AppEntry.applyMeta(
        meta: AppMetaEntity?,
        favoritePackages: Set<String>,
    ): AppEntry {
        if (meta == null) {
            return copy(isFavorite = packageName in favoritePackages)
        }
        val category = meta.category.toAppCategory()
        return copy(
            category = category,
            isHidden = meta.isHidden,
            isFavorite = meta.isFavorite || packageName in favoritePackages,
            isDistracting = meta.resolveDistracting(),
        )
    }

    private suspend fun ensureMetaRow(packageName: String) {
        if (appMetaDao.get(packageName) == null) {
            appMetaDao.upsert(
                AppMetaEntity(
                    packageName = packageName,
                    category = catalog.categorize(packageName).name,
                ),
            )
        }
    }

    /**
     * Seed Phone/Messages/Camera as starter shortcuts **exactly once**, tracked by
     * [LauncherSettings.favoritesSeeded]. Anything else would re-add the defaults on the next
     * device refresh every time the user unpins them, making them impossible to remove.
     */
    private suspend fun seedDefaultFavoritesIfNeeded() {
        val settings = settingsRepository.current()
        if (settings.favoritesSeeded) return

        // Only the untouched placeholder ("phone"/"messages"/"camera") counts as "never
        // chosen". An empty list is a deliberate choice — the user cleared their shortcuts —
        // and must not be mistaken for a fresh install on upgrade.
        val hasUserChoice = settings.favorites != LauncherSettings.DEFAULT_FAVORITES
        if (hasUserChoice) {
            // Respect an existing list from an older install; just close the seeding window.
            settingsRepository.update { it.copy(favoritesSeeded = true) }
            return
        }

        val seeded = listOfNotNull(
            catalog.resolveTool(LauncherTool.PHONE),
            catalog.resolveTool(LauncherTool.MESSAGES),
            catalog.resolveTool(LauncherTool.CAMERA),
        ).distinct()
        // Nothing resolvable yet (catalog still warming up) — try again on the next refresh.
        if (seeded.isEmpty()) return

        settingsRepository.update { it.copy(favorites = seeded, favoritesSeeded = true) }
        seeded.forEachIndexed { index, pkg ->
            ensureMetaRow(pkg)
            appMetaDao.setFavorite(pkg, true)
            appMetaDao.setFavoriteOrder(pkg, index)
        }
    }

    private suspend fun syncFavoriteOrderFromSettings() {
        val favorites = settingsRepository.current().favorites
        val ordered = resolveFavoritePackages(favorites)
        ordered.forEachIndexed { index, pkg ->
            ensureMetaRow(pkg)
            appMetaDao.setFavorite(pkg, true)
            appMetaDao.setFavoriteOrder(pkg, index)
        }
    }

    private companion object {
        /** Lower-cased [LauncherTool] name → tool, for resolving favourite placeholders. */
        val TOOL_KEYS: Map<String, LauncherTool> =
            LauncherTool.entries.associateBy { it.name.lowercase() }
    }
}
