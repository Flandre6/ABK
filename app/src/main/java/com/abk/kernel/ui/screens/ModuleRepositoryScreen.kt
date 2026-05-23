@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.abk.kernel.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.abk.kernel.R
import com.abk.kernel.data.model.RuntimeModuleCatalogItem
import com.abk.kernel.data.model.RuntimeModuleRepository
import com.abk.kernel.ui.components.AbkScreenHorizontalPadding
import com.abk.kernel.ui.components.ExpressiveSectionCard
import com.abk.kernel.ui.components.ExpressiveStatusChip
import com.abk.kernel.ui.components.ExpressiveTopBar
import com.abk.kernel.ui.theme.uiSurfaceColor
import com.abk.kernel.utils.DownloadUtils
import com.abk.kernel.utils.RootUtils
import com.abk.kernel.viewmodel.MainViewModel
import java.io.File
import kotlin.math.pow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MODULE_REPOSITORY_BACK_VISUAL_EXPONENT = 1.8f
private const val MODULE_REPOSITORY_BACK_SCALE_DELTA = 0.09f
private const val MODULE_REPOSITORY_BACK_SCRIM_ALPHA = 0.32f
private const val MODULE_REPOSITORY_PAGE_EXIT_DELAY_MS = 280L
private const val RUNTIME_MODULE_DOWNLOAD_RUN_ID = -2_000_000_001L
private val MODULE_REPOSITORY_BACK_MAX_OFFSET = 56.dp
private val MODULE_REPOSITORY_BACK_MAX_CORNER = 32.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleRepositoryScreen(
    vm: MainViewModel,
    outerPadding: PaddingValues = PaddingValues(0.dp),
    onRepositoryPageVisibleChange: (Boolean) -> Unit = {}
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val motionScheme = MaterialTheme.motionScheme
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showRepositorySettings by rememberSaveable { mutableStateOf(false) }
    var pendingInstallModule by remember { mutableStateOf<MergedRuntimeCatalogModule?>(null) }
    var installDialogVisible by remember { mutableStateOf(false) }
    var installRunning by remember { mutableStateOf(false) }
    var installSuccess by remember { mutableStateOf<Boolean?>(null) }
    var installLog by remember { mutableStateOf<List<String>>(emptyList()) }
    var repositoryBackProgress by remember { mutableFloatStateOf(0f) }
    val animatedRepositoryBackProgress by animateFloatAsState(
        targetValue = repositoryBackProgress.coerceIn(0f, 1f),
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "module-repository-back-progress"
    )
    val visualRepositoryBackProgress = animatedRepositoryBackProgress
        .coerceIn(0f, 1f)
        .pow(MODULE_REPOSITORY_BACK_VISUAL_EXPONENT)
    val density = LocalDensity.current
    val repositoryBackOffsetPx = with(density) { MODULE_REPOSITORY_BACK_MAX_OFFSET.toPx() }
    val repositoryBackCorner = with(density) {
        (MODULE_REPOSITORY_BACK_MAX_CORNER.toPx() * visualRepositoryBackProgress).toDp()
    }
    val mergedModules = remember(state.runtimeModuleRepositories) {
        mergeRuntimeCatalogModules(state.runtimeModuleRepositories)
    }
    val filteredModules = remember(mergedModules, searchQuery) {
        mergedModules.filter { it.matchesQuery(searchQuery) }
    }

    fun openRepositorySettings() {
        repositoryBackProgress = 0f
        onRepositoryPageVisibleChange(true)
        showRepositorySettings = true
    }

    fun closeRepositorySettings() {
        showRepositorySettings = false
    }

    fun appendInstallLog(line: String) {
        installLog = installLog + line
    }

    fun startInstall(module: MergedRuntimeCatalogModule) {
        if (installRunning) return
        pendingInstallModule = null
        installDialogVisible = true
        installRunning = true
        installSuccess = null
        installLog = listOf(
            "$ module install",
            "name: ${module.module.name}",
            "source: ${module.module.zipUrl}",
            "",
            context.getString(R.string.module_repo_runtime_downloading)
        )
        scope.launch {
            val downloadName = module.module.downloadFileName()
            val downloadResult = withContext(Dispatchers.IO) {
                DownloadUtils.downloadDirectAsset(
                    context = context,
                    token = null,
                    url = module.module.zipUrl,
                    name = downloadName,
                    sizeBytes = 0L,
                    runId = RUNTIME_MODULE_DOWNLOAD_RUN_ID,
                    runTitle = module.sources.firstOrNull().orEmpty().ifBlank {
                        context.getString(R.string.module_repo_runtime_unknown_source)
                    },
                    downloadDirectoryPath = state.downloadDirectory
                )
            }
            val downloadedFile = downloadResult.artifacts.firstOrNull()?.filePath?.let(::File)
            if (downloadedFile == null || !downloadedFile.exists()) {
                installRunning = false
                installSuccess = false
                installLog = installLog + listOf(
                    "",
                    downloadResult.errorMessage ?: context.getString(R.string.module_repo_runtime_download_failed)
                )
                return@launch
            }

            appendInstallLog("file: ${downloadedFile.absolutePath}")
            appendInstallLog(context.getString(R.string.module_repo_runtime_wait_install))
            val result = withContext(Dispatchers.IO) {
                if (!RootUtils.refreshRootState()) {
                    RootUtils.ShellResult(false, listOf(context.getString(R.string.runtime_manager_inactive)))
                } else {
                    RootUtils.installModule(downloadedFile.absolutePath) { line ->
                        scope.launch(Dispatchers.Main.immediate) {
                            appendInstallLog(line)
                        }
                    }
                }
            }
            installRunning = false
            installSuccess = result.success
            installLog = listOf(
                "$ module install ${downloadedFile.name}",
                "file: ${downloadedFile.absolutePath}",
                ""
            ) + result.output.ifEmpty {
                listOf(
                    if (result.success) {
                        context.getString(R.string.runtime_module_install_done_no_output)
                    } else {
                        context.getString(R.string.runtime_module_install_failed_no_log)
                    }
                )
            }
            if (result.success) vm.refreshAbkRuntimeStatus()
        }
    }

    LaunchedEffect(showRepositorySettings) {
        if (showRepositorySettings) {
            onRepositoryPageVisibleChange(true)
        } else {
            delay(MODULE_REPOSITORY_PAGE_EXIT_DELAY_MS)
            repositoryBackProgress = 0f
            onRepositoryPageVisibleChange(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose { onRepositoryPageVisibleChange(false) }
    }

    PredictiveBackHandler(enabled = showRepositorySettings && state.predictiveBackEnabled) { progress ->
        try {
            progress.collect { backEvent ->
                repositoryBackProgress = backEvent.progress.coerceIn(0f, 1f)
            }
            closeRepositorySettings()
        } catch (_: CancellationException) {
            repositoryBackProgress = 0f
        }
    }

    BackHandler(enabled = showRepositorySettings && !state.predictiveBackEnabled) {
        closeRepositorySettings()
    }

    pendingInstallModule?.let { merged ->
        RuntimeRepositoryInstallConfirmDialog(
            module = merged,
            onDismiss = { pendingInstallModule = null },
            onConfirm = { startInstall(merged) }
        )
    }

    if (installDialogVisible) {
        RuntimeRepositoryInstallDialog(
            running = installRunning,
            success = installSuccess,
            logLines = installLog,
            onClose = { if (!installRunning) installDialogVisible = false },
            onReboot = {
                if (!installRunning) {
                    scope.launch(Dispatchers.IO) { RootUtils.reboot() }
                }
            }
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val childPageTopInset = outerPadding.calculateTopPadding()
        val childPageBottomInset = outerPadding.calculateBottomPadding()
        val childPageModifier = Modifier
            .fillMaxWidth()
            .height(maxHeight + childPageTopInset + childPageBottomInset)
            .offset(y = -childPageTopInset)

        Scaffold(
            containerColor = uiSurfaceColor(MaterialTheme.colorScheme.surface),
            topBar = {
                ExpressiveTopBar(
                    title = stringResource(R.string.module_repo_runtime_title),
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = ::openRepositorySettings) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = stringResource(R.string.module_repo_runtime_configure)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            RuntimeModuleRepositoryListContent(
                padding = padding,
                modules = filteredModules,
                totalModules = mergedModules.size,
                repositories = state.runtimeModuleRepositories,
                refreshing = state.refreshingRuntimeModuleRepositoryIds.isNotEmpty(),
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onOpenRepositorySettings = ::openRepositorySettings,
                onOpenModule = { module ->
                    val url = module.module.preferredOpenUrl()
                    if (url.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.module_repo_open_failed), Toast.LENGTH_SHORT).show()
                    } else {
                        runCatching { uriHandler.openUri(url) }
                            .onFailure {
                                Toast.makeText(context, context.getString(R.string.module_repo_open_failed), Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                onInstallModule = { module ->
                    if (module.module.zipUrl.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.module_repo_runtime_no_zip), Toast.LENGTH_SHORT).show()
                    } else {
                        pendingInstallModule = module
                    }
                },
                scrollBehavior = scrollBehavior,
                bottomPadding = outerPadding.calculateBottomPadding()
            )
        }

        AnimatedVisibility(
            visible = showRepositorySettings,
            enter = fadeIn(animationSpec = motionScheme.defaultEffectsSpec()),
            exit = fadeOut(animationSpec = motionScheme.fastEffectsSpec()),
            modifier = childPageModifier
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = MODULE_REPOSITORY_BACK_SCRIM_ALPHA * visualRepositoryBackProgress))
            )
        }

        AnimatedVisibility(
            visible = showRepositorySettings,
            enter = fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) +
                slideInHorizontally(animationSpec = motionScheme.defaultSpatialSpec()) { width -> width / 4 },
            exit = fadeOut(animationSpec = motionScheme.fastEffectsSpec()) +
                slideOutHorizontally(animationSpec = motionScheme.fastSpatialSpec()) { width -> width },
            modifier = childPageModifier
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = repositoryBackOffsetPx * visualRepositoryBackProgress
                        scaleX = 1f - MODULE_REPOSITORY_BACK_SCALE_DELTA * visualRepositoryBackProgress
                        scaleY = 1f - MODULE_REPOSITORY_BACK_SCALE_DELTA * visualRepositoryBackProgress
                        alpha = 1f - 0.06f * visualRepositoryBackProgress
                        shape = RoundedCornerShape(repositoryBackCorner)
                        clip = visualRepositoryBackProgress > 0.01f
                    }
            ) {
                ModuleRepositoryPageBackground(
                    backgroundUri = state.customBackgroundUri,
                    backgroundImageEnabled = state.backgroundImageEnabled
                )
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        ExpressiveTopBar(
                            title = stringResource(R.string.module_repo_runtime_central),
                            navigationIcon = {
                                IconButton(onClick = ::closeRepositorySettings) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.module_repo_back))
                                }
                            }
                        )
                    }
                ) { padding ->
                    RuntimeModuleRepositorySettingsPage(
                        padding = padding,
                        repositories = state.runtimeModuleRepositories,
                        refreshingRepositoryIds = state.refreshingRuntimeModuleRepositoryIds,
                        onAddRepository = vm::addRuntimeModuleRepository,
                        onRefreshAll = vm::refreshAllRuntimeModuleRepositories,
                        onRefreshRepository = vm::refreshRuntimeModuleRepository,
                        onDeleteRepository = vm::deleteRuntimeModuleRepository
                    )
                }
            }
        }
    }
}

@Composable
private fun RuntimeModuleRepositoryListContent(
    padding: PaddingValues,
    modules: List<MergedRuntimeCatalogModule>,
    totalModules: Int,
    repositories: List<RuntimeModuleRepository>,
    refreshing: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenRepositorySettings: () -> Unit,
    onOpenModule: (MergedRuntimeCatalogModule) -> Unit,
    onInstallModule: (MergedRuntimeCatalogModule) -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    bottomPadding: Dp
) {
    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AbkScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CompactModuleSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange
        )

        if (refreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (modules.isEmpty()) {
            RuntimeModuleRepositoryEmptyState(
                totalModules = totalModules,
                repositoryCount = repositories.size,
                hasQuery = searchQuery.isNotBlank(),
                onOpenRepositorySettings = onOpenRepositorySettings
            )
        } else {
            modules.forEach { merged ->
                RuntimeModuleRepositoryListItem(
                    merged = merged,
                    onOpen = { onOpenModule(merged) },
                    onInstall = { onInstallModule(merged) }
                )
            }
        }

        Spacer(Modifier.height(bottomPadding + 24.dp))
    }
}

@Composable
private fun RuntimeModuleRepositoryEmptyState(
    totalModules: Int,
    repositoryCount: Int,
    hasQuery: Boolean,
    onOpenRepositorySettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Extension,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(38.dp)
        )
        Text(
            text = when {
                hasQuery -> stringResource(R.string.module_repo_no_matching)
                repositoryCount == 0 -> stringResource(R.string.module_repo_runtime_empty_title)
                totalModules == 0 -> stringResource(R.string.module_repo_runtime_empty_desc)
                else -> stringResource(R.string.module_repo_no_display)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = onOpenRepositorySettings) {
            Icon(Icons.Default.Dns, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.module_repo_runtime_manage))
        }
    }
}

@Composable
private fun RuntimeModuleRepositoryListItem(
    merged: MergedRuntimeCatalogModule,
    onOpen: () -> Unit,
    onInstall: () -> Unit
) {
    val module = merged.module
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = uiSurfaceColor(MaterialTheme.colorScheme.surfaceContainer)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = module.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = module.metaLine()
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (merged.sources.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.Source,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = merged.sources.size.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (module.description.isNotBlank()) {
                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ModuleTagChip(label = module.id.ifBlank { module.name }, maxWidth = 170.dp)
                module.minApi?.let { ModuleTagChip(label = "API >= $it", secondary = true) }
                module.maxApi?.let { ModuleTagChip(label = "API <= $it", secondary = true) }
                if (module.verified) {
                    ModuleTagChip(label = stringResource(R.string.module_repo_runtime_verified), secondary = true)
                }
                if (merged.sources.size > 1) {
                    ModuleTagChip(label = stringResource(R.string.module_repo_source_count, merged.sources.size), secondary = true)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactModuleActionButton(
                    icon = Icons.Default.OpenInBrowser,
                    contentDescription = stringResource(R.string.module_repo_runtime_open),
                    onClick = onOpen
                )
                Spacer(Modifier.width(6.dp))
                CompactModuleActionButton(
                    icon = Icons.Default.UploadFile,
                    contentDescription = stringResource(R.string.module_repo_runtime_install),
                    enabled = module.zipUrl.isNotBlank(),
                    onClick = onInstall
                )
            }
        }
    }
}

@Composable
private fun CompactModuleSearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isBlank()) {
                    Text(
                        text = stringResource(R.string.module_repo_search),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CompactModuleActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 42.dp, height = 36.dp),
        shape = RoundedCornerShape(18.dp),
        color = colors.secondaryContainer.copy(alpha = if (enabled) 0.82f else 0.44f),
        contentColor = if (enabled) colors.onSecondaryContainer else colors.onSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun ModuleTagChip(
    label: String,
    secondary: Boolean = false,
    maxWidth: Dp = 140.dp
) {
    val color = if (secondary) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    val contentColor = if (secondary) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = color.copy(alpha = if (secondary) 0.78f else 0.88f),
        contentColor = contentColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = maxWidth)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun RuntimeModuleRepositorySettingsPage(
    padding: PaddingValues,
    repositories: List<RuntimeModuleRepository>,
    refreshingRepositoryIds: Set<String>,
    onAddRepository: (String) -> Unit,
    onRefreshAll: () -> Unit,
    onRefreshRepository: (String) -> Unit,
    onDeleteRepository: (String) -> Unit
) {
    var repositoryUrl by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AbkScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExpressiveSectionCard(
            title = stringResource(R.string.module_repo_runtime_central),
            subtitle = stringResource(R.string.module_repo_runtime_central_desc),
            icon = Icons.Default.Dns
        ) {
            OutlinedTextField(
                value = repositoryUrl,
                onValueChange = { repositoryUrl = it },
                label = { Text(stringResource(R.string.module_repo_runtime_url)) },
                placeholder = { Text("https://example.com/modules.json") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onAddRepository(repositoryUrl)
                        repositoryUrl = ""
                    },
                    enabled = repositoryUrl.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add))
                }
                OutlinedButton(
                    onClick = onRefreshAll,
                    enabled = repositories.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.refresh_all))
                }
            }
        }

        if (repositories.isEmpty()) {
            ExpressiveSectionCard(
                title = stringResource(R.string.module_repo_runtime_empty_title),
                subtitle = stringResource(R.string.module_repo_runtime_empty_desc),
                icon = Icons.Default.Extension
            ) {
                Text(
                    text = stringResource(R.string.module_repo_runtime_central_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            repositories.forEach { repository ->
                RuntimeModuleRepositoryCard(
                    repository = repository,
                    refreshing = repository.id in refreshingRepositoryIds,
                    onRefresh = { onRefreshRepository(repository.id) },
                    onDelete = { onDeleteRepository(repository.id) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RuntimeModuleRepositoryCard(
    repository: RuntimeModuleRepository,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    ExpressiveSectionCard(
        title = repository.name.ifBlank { repository.url },
        subtitle = repository.url,
        icon = Icons.Default.Dns
    ) {
        if (refreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpressiveStatusChip(
                label = stringResource(R.string.module_repo_module_count, repository.modules.size),
                icon = Icons.Default.Extension,
                color = MaterialTheme.colorScheme.primary
            )
            if (repository.skippedCount > 0) {
                ExpressiveStatusChip(
                    label = stringResource(R.string.module_repo_skipped_count, repository.skippedCount),
                    icon = Icons.Default.Link,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        repository.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        val indexUrl = repository.indexJsonUrl.ifBlank { repository.url }
        Text(
            text = indexUrl,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onRefresh,
                enabled = !refreshing,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.refresh))
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun RuntimeRepositoryInstallConfirmDialog(
    module: MergedRuntimeCatalogModule,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.UploadFile, null) },
        title = { Text(stringResource(R.string.module_repo_runtime_confirm_install)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = module.module.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                module.module.metaLine().takeIf { it.isNotBlank() }?.let { meta ->
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(
                        R.string.module_repo_runtime_source,
                        module.sources.firstOrNull()
                            ?: stringResource(R.string.module_repo_runtime_unknown_source)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = module.module.zipUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (module.module.description.isNotBlank()) {
                    Text(
                        text = module.module.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.module_repo_runtime_confirm_install_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.module_repo_runtime_install))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun RuntimeRepositoryInstallDialog(
    running: Boolean,
    success: Boolean?,
    logLines: List<String>,
    onClose: () -> Unit,
    onReboot: () -> Unit
) {
    val terminalScroll = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    val isLightTheme = colorScheme.surface.luminance() > 0.5f
    val terminalContainer = if (isLightTheme) {
        colorScheme.surfaceContainerHighest
    } else {
        colorScheme.surfaceContainerLowest
    }

    LaunchedEffect(logLines.size) {
        terminalScroll.animateScrollTo(terminalScroll.maxValue)
    }

    AlertDialog(
        onDismissRequest = { if (!running) onClose() },
        icon = {
            when {
                running -> LoadingIndicator(modifier = Modifier.size(24.dp))
                success == true -> Icon(Icons.Default.CheckCircle, null, tint = colorScheme.primary)
                success == false -> Icon(Icons.Default.Error, null, tint = colorScheme.error)
                else -> Icon(Icons.Default.UploadFile, null)
            }
        },
        title = {
            Text(
                if (running) {
                    stringResource(R.string.runtime_installing_module)
                } else {
                    stringResource(R.string.module_repo_runtime_install)
                }
            )
        },
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 190.dp, max = 360.dp),
                shape = RoundedCornerShape(12.dp),
                color = terminalContainer,
                contentColor = colorScheme.onSurface,
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(terminalScroll)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    logLines.ifEmpty { listOf(stringResource(R.string.runtime_waiting_output)) }.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (line.startsWith("$")) colorScheme.primary else colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (running) {
                TextButton(onClick = {}, enabled = false) {
                    Text(stringResource(R.string.runtime_running))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClose) {
                        Text(stringResource(R.string.close))
                    }
                    if (success == true) {
                        Button(
                            onClick = onReboot,
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                        ) {
                            Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.runtime_reboot))
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ModuleRepositoryPageBackground(
    backgroundUri: String?,
    backgroundImageEnabled: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val hasBackground = backgroundImageEnabled && !backgroundUri.isNullOrBlank()
    val scrimColor = if (colorScheme.surface.luminance() > 0.5f) {
        colorScheme.surface.copy(alpha = 0.28f)
    } else {
        Color.Black.copy(alpha = 0.38f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        if (hasBackground) {
            AsyncImage(
                model = backgroundUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
            )
        }
    }
}

private data class MergedRuntimeCatalogModule(
    val module: RuntimeModuleCatalogItem,
    val sources: List<String>
)

private fun mergeRuntimeCatalogModules(repositories: List<RuntimeModuleRepository>): List<MergedRuntimeCatalogModule> =
    repositories
        .flatMap { repository ->
            repository.modules.map { module -> repository.name.ifBlank { repository.url } to module }
        }
        .groupBy { (_, module) -> module.id.trim().lowercase().ifBlank { module.name.trim().lowercase() } }
        .values
        .map { entries ->
            MergedRuntimeCatalogModule(
                module = entries.first().second,
                sources = entries.map { it.first }.distinct()
            )
        }
        .sortedBy { it.module.name.lowercase() }

private fun MergedRuntimeCatalogModule.matchesQuery(query: String): Boolean {
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank()) return true
    val module = module
    return listOf(
        module.id,
        module.name,
        module.version,
        module.author,
        module.description,
        module.support,
        module.website,
        module.zipUrl,
        sources.joinToString(" ")
    ).any { it.contains(cleanQuery, ignoreCase = true) }
}

@Composable
private fun RuntimeModuleCatalogItem.metaLine(): String =
    listOfNotNull(
        version.takeIf { it.isNotBlank() }?.let { stringResource(R.string.module_repo_version, it) },
        author.takeIf { it.isNotBlank() }?.let { stringResource(R.string.runtime_module_author, it) }
    ).joinToString("\n")

private fun RuntimeModuleCatalogItem.preferredOpenUrl(): String =
    support.takeIf { it.isNotBlank() }
        ?: website.takeIf { it.isNotBlank() }
        ?: donate.takeIf { it.isNotBlank() }
        ?: zipUrl

private fun RuntimeModuleCatalogItem.downloadFileName(): String {
    val base = id.ifBlank { name }
        .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
        .trim('_')
        .ifBlank { "module" }
    return if (base.endsWith(".zip", ignoreCase = true)) base else "${base}-module.zip"
}
