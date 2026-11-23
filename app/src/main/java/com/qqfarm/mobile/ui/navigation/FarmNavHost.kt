package com.qqfarm.mobile.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.qqfarm.mobile.ui.components.pressable
import com.qqfarm.mobile.ui.components.rememberAppHaptics
import com.qqfarm.mobile.ui.components.spaceAccent
import com.qqfarm.mobile.ui.components.spaceIcon
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import com.qqfarm.mobile.data.model.GameSpace
import com.qqfarm.mobile.ui.components.AppleSpaceSwitcher
import com.qqfarm.mobile.ui.components.AppleSubPageHeader
import com.qqfarm.mobile.ui.screens.DiscoverScreen
import com.qqfarm.mobile.ui.screens.RanchWorkspaceScreen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qqfarm.mobile.ui.components.AccountSwitcherSheet
import com.qqfarm.mobile.ui.screens.AutomationScreen
import com.qqfarm.mobile.ui.screens.FarmRanchScreen
import com.qqfarm.mobile.ui.screens.FriendsScreen
import com.qqfarm.mobile.ui.screens.IllustratedScreen
import com.qqfarm.mobile.ui.screens.OverviewScreen
import com.qqfarm.mobile.ui.screens.ServerSetupScreen
import com.qqfarm.mobile.ui.screens.SettingsScreen
import com.qqfarm.mobile.ui.state.AppSessionViewModel
import com.qqfarm.mobile.ui.theme.LocalAppleColors
import androidx.compose.foundation.layout.WindowInsets
import com.qqfarm.mobile.data.prefs.StoredConfig

private data class NavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

@Composable
fun FarmNavHost(viewModel: AppSessionViewModel) {
    val apple = LocalAppleColors.current
    val session by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var spaceName by rememberSaveable { mutableStateOf(GameSpace.FARM.name) }
    var discoverSubTab by rememberSaveable { mutableIntStateOf(0) }
    var mallSubTab by rememberSaveable { mutableIntStateOf(0) }
    var ranchSubTab by rememberSaveable { mutableIntStateOf(0) }
    var showAutomationPage by rememberSaveable { mutableStateOf(false) }
    var showIllustratedPage by rememberSaveable { mutableStateOf(false) }
    var hideMainChrome by remember { mutableStateOf(false) }
    var showAccountSwitcher by remember { mutableStateOf(false) }

    val gameSpace = runCatching { GameSpace.valueOf(spaceName) }.getOrDefault(GameSpace.FARM)
    fun selectSpace(target: GameSpace) {
        if (target.name != spaceName) {
            spaceName = target.name
            mallSubTab = 0
            hideMainChrome = false
        }
    }

    LaunchedEffect(selectedTab, showAutomationPage, showIllustratedPage) {
        if (selectedTab != 2 || showAutomationPage || showIllustratedPage) {
            hideMainChrome = false
        }
    }

    val destinations = remember(gameSpace) {
        listOf(
            NavDestination("总览", Icons.Rounded.SpaceDashboard, Icons.Outlined.SpaceDashboard),
            NavDestination(gameSpace.label, spaceIcon(gameSpace, filled = true), spaceIcon(gameSpace, filled = false)),
            NavDestination("发现", Icons.Rounded.Explore, Icons.Outlined.Explore),
            NavDestination("好友", Icons.Rounded.People, Icons.Outlined.People),
            NavDestination("我的", Icons.Rounded.AccountCircle, Icons.Outlined.AccountCircle),
        )
    }

    LaunchedEffect(Unit) {
        viewModel.snackbars.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (session.initializing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(apple.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (!session.config.isAuthenticated || session.showReconfigureServer) {
        val isReconfiguring = session.config.isAuthenticated && session.showReconfigureServer
        BackHandler(enabled = isReconfiguring, onBack = viewModel::closeReconfigureServer)
        key(session.config.activeProfileId, session.addingServer) {
            ServerSetupScreen(
                // 添加服务器时从空白表单开始，不带出当前服务器的地址与凭证。
                initialConfig = if (session.addingServer) StoredConfig() else session.config,
                isReconfiguring = isReconfiguring,
                isAddingServer = session.addingServer && isReconfiguring,
                otherServers = session.config.serverProfiles.filter { it.id != session.config.activeProfileId },
                onSwitchServer = { id -> viewModel.switchServer(id) },
                onBack = { viewModel.closeReconfigureServer() },
                onTestConnection = { url -> viewModel.testServerConnection(url) },
                onConnectWithToken = { url, token -> viewModel.connectWithApiToken(url, token) },
                onConnectWithPassword = { url, user, pass, remember ->
                    viewModel.connectWithPassword(url, user, pass, remember)
                },
                onForgetPassword = viewModel::forgetPassword,
                onSuccessMessage = { msg -> viewModel.emitMessage(msg) },
            )
        }
        return
    }

    BackHandler(enabled = showAutomationPage) {
        showAutomationPage = false
    }

    BackHandler(enabled = showIllustratedPage) {
        showIllustratedPage = false
    }

    BackHandler(enabled = !showAutomationPage && !showIllustratedPage && selectedTab != 0) {
        selectedTab = 0
    }

    val currentDest = destinations.getOrElse(selectedTab) { destinations.first() }
    val activeAccount = session.currentAccount

    val bottomBarVisible = !showAutomationPage && !showIllustratedPage && !hideMainChrome
    Scaffold(
        containerColor = apple.background,
        // 系统栏留白由各层自己处理：顶栏/子页头部自带状态栏留白，底栏自带导航栏留白。
        // 默认的 contentWindowInsets 会在隐藏顶栏时再叠一次状态栏高度，导致活动详情顶部大片空白。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (showAutomationPage) {
                AppleSubPageHeader(
                    title = "自动化设置",
                    subtitle = activeAccount?.name?.ifBlank { activeAccount.displayTitle },
                    onBack = { showAutomationPage = false },
                )
            } else if (showIllustratedPage) {
                AppleSubPageHeader(
                    title = "图鉴",
                    subtitle = activeAccount?.name?.ifBlank { activeAccount.displayTitle },
                    onBack = { showIllustratedPage = false },
                )
            } else if (!hideMainChrome) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(apple.background)
                        .statusBarsPadding(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selectedTab in 0..3) {
                            AppleSpaceSwitcher(
                                space = gameSpace,
                                onSelect = ::selectSpace,
                            )
                        } else {
                            Text(
                                text = currentDest.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = apple.primaryText,
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .pressable { showAccountSwitcher = true },
                            shape = CircleShape,
                            color = apple.card,
                            border = BorderStroke(0.5.dp, apple.separator),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                session.status?.connected == true -> apple.green
                                                session.realtimeConnected -> apple.orange
                                                else -> apple.secondaryText.copy(alpha = 0.5f)
                                            }
                                        ),
                                )
                                Text(
                                    text = activeAccount?.name?.ifBlank { activeAccount.displayTitle }
                                        ?: if (session.accounts.isEmpty()) "添加账号" else "选择账号",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = apple.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.UnfoldMore,
                                    contentDescription = "切换账号",
                                    tint = apple.secondaryText,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (bottomBarVisible) {
                AppleBottomBar(
                    destinations = destinations,
                    selectedIndex = selectedTab,
                    gameSpace = gameSpace,
                    onSelect = { selectedTab = it },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(if (bottomBarVisible) Modifier else Modifier.navigationBarsPadding()),
        ) {
            val refreshOverviewSilent: () -> Unit = {
                viewModel.refreshOverview(silent = true)
                if (gameSpace == GameSpace.RANCH) {
                    viewModel.refreshRanchOverview(silent = true)
                }
            }
            key(session.config.activeProfileId, session.config.serverUrl, session.config.currentAccountId) {
                if (showAutomationPage) {
                    AutomationScreen(
                        session = session,
                        apiClient = viewModel.apiClient,
                        onMessage = { msg -> viewModel.emitMessage(msg) },
                    )
                } else if (showIllustratedPage) {
                    key(session.config.serverUrl, session.config.currentAccountId) {
                        IllustratedScreen(
                            session = session,
                            apiClient = viewModel.apiClient,
                            onRefreshOverview = refreshOverviewSilent,
                            onMessage = { msg -> viewModel.emitMessage(msg) },
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(tween(durationMillis = 180, delayMillis = 40)) togetherWith
                                fadeOut(tween(durationMillis = 120))
                        },
                        label = "mainTab",
                    ) { tab ->
                        when (tab) {
                            0 -> OverviewScreen(
                                session = session,
                                apiClient = viewModel.apiClient,
                                gameSpace = gameSpace,
                                onRefreshInspectionStatus = viewModel::refreshInspectionStatus,
                                onRefresh = {
                                    viewModel.refreshAccounts()
                                    viewModel.refreshOverview()
                                },
                                onRefreshSilent = { viewModel.refreshOverview(silent = true) },
                                onRefreshRanchOverview = { silent -> viewModel.refreshRanchOverview(silent) },
                                onOpenAccountSwitcher = { showAccountSwitcher = true },
                                onStartAccount = { id -> viewModel.startAccount(id) },
                                onClearLogs = viewModel::clearLogs,
                                onMessage = { msg -> viewModel.emitMessage(msg) },
                                onSwitchSpace = ::selectSpace,
                                onOpenDiscover = { subTab, mallIdx ->
                                    discoverSubTab = subTab
                                    mallSubTab = mallIdx
                                    selectedTab = 2
                                },
                                onOpenWorkspace = { subIdx ->
                                    ranchSubTab = subIdx
                                    selectedTab = 1
                                },
                                onOpenAutomation = {
                                    showIllustratedPage = false
                                    showAutomationPage = true
                                },
                            )

                            1 -> if (gameSpace == GameSpace.RANCH) {
                                RanchWorkspaceScreen(
                                    session = session,
                                    apiClient = viewModel.apiClient,
                                    initialSubTab = ranchSubTab,
                                    onSubTabChanged = { ranchSubTab = it },
                                    onOpenRanchMall = {
                                        discoverSubTab = 1
                                        mallSubTab = 0
                                        selectedTab = 2
                                    },
                                    onRefreshOverview = refreshOverviewSilent,
                                    onMessage = { msg -> viewModel.emitMessage(msg) },
                                )
                            } else {
                                FarmRanchScreen(
                                    session = session,
                                    apiClient = viewModel.apiClient,
                                    onRefreshOverview = refreshOverviewSilent,
                                    onMessage = { msg -> viewModel.emitMessage(msg) },
                                )
                            }

                            2 -> DiscoverScreen(
                                session = session,
                                apiClient = viewModel.apiClient,
                                gameSpace = gameSpace,
                                selectedSubTab = discoverSubTab,
                                onSelectSubTab = { discoverSubTab = it },
                                mallTab = mallSubTab,
                                onSelectMallTab = { mallSubTab = it },
                                onFullScreenOverlayChange = { hideMainChrome = it },
                                onRefreshOverview = refreshOverviewSilent,
                                onMessage = { msg -> viewModel.emitMessage(msg) },
                            )

                            3 -> FriendsScreen(
                                session = session,
                                apiClient = viewModel.apiClient,
                                gameSpace = gameSpace,
                                onRefreshOverview = refreshOverviewSilent,
                                onMessage = { msg -> viewModel.emitMessage(msg) },
                            )

                            4 -> SettingsScreen(
                                session = session,
                                apiClient = viewModel.apiClient,
                                onOpenReconfigureServer = { viewModel.openReconfigureServer() },
                                onAddServer = { viewModel.openAddServer() },
                                onSwitchServer = { id -> viewModel.switchServer(id) },
                                onRenameServer = { id, name -> viewModel.renameServer(id, name) },
                                onRemoveServer = { id -> viewModel.removeServer(id) },
                                onOpenAccountSwitcher = { showAccountSwitcher = true },
                                onOpenAutomation = {
                                    showIllustratedPage = false
                                    showAutomationPage = true
                                },
                                onOpenIllustrated = {
                                    showAutomationPage = false
                                    showIllustratedPage = true
                                },
                                onDisconnect = { viewModel.disconnect() },
                                onUpdateThemeMode = { mode -> viewModel.updateThemeMode(mode) },
                                onUpdateDynamicColor = { enabled -> viewModel.updateDynamicColor(enabled) },
                                onUpdateCustomAppearance = { appearance -> viewModel.updateCustomAppearance(appearance) },
                                onToggleOfflineAlerts = { enabled -> viewModel.setOfflineAlertsEnabled(enabled) },
                                onToggleActivityPush = { enabled -> viewModel.setActivityPushEnabled(enabled) },
                                onToggleNotifyCollapse = { enabled -> viewModel.setNotifyCollapseEnabled(enabled) },
                                onToggleNotifySilent = { enabled -> viewModel.setNotifySilentUpdate(enabled) },
                                onToggleNotifyAllAccounts = { enabled -> viewModel.setNotifyAllAccounts(enabled) },
                                onToggleNotifyType = { key, enabled -> viewModel.toggleNotifyType(key, enabled) },
                                onApplyNotifyTypes = { keys -> viewModel.applyNotifyTypes(keys) },
                                onSendTestNotification = { viewModel.sendTestActivityNotification() },
                                onClearLogs = viewModel::clearLogs,
                                onServerUpdated = {
                                    viewModel.refreshAccounts()
                                    viewModel.refreshOverview(silent = true)
                                },
                                onMessage = { msg -> viewModel.emitMessage(msg) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAccountSwitcher) {
        AccountSwitcherSheet(
            accounts = session.accounts,
            currentAccountId = session.config.currentAccountId,
            apiClient = viewModel.apiClient,
            onDismiss = { showAccountSwitcher = false },
            onSelectAccount = { id -> viewModel.selectAccount(id) },
            onStartAccount = { id -> viewModel.startAccount(id) },
            onStopAccount = { id -> viewModel.stopAccount(id) },
            onDeleteAccount = { id -> viewModel.deleteAccount(id) },
            onAccountsChanged = viewModel::refreshAccounts,
        )
    }
}

@Composable
private fun AppleBottomBar(
    destinations: List<NavDestination>,
    selectedIndex: Int,
    gameSpace: GameSpace,
    onSelect: (Int) -> Unit,
) {
    val apple = LocalAppleColors.current
    val haptics = rememberAppHaptics()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = apple.card,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(thickness = 0.5.dp, color = apple.separator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 6.dp)
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEachIndexed { index, dest ->
                    val selected = index == selectedIndex
                    // “我的”不随空间变色；其余标签跟随当前空间主色（农场绿 / 牧场橙）。
                    val accent = if (index < 4) spaceAccent(gameSpace) else MaterialTheme.colorScheme.primary
                    val tint by animateColorAsState(
                        targetValue = if (selected) accent else apple.secondaryText,
                        label = "tabTint",
                    )
                    val indicatorAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        label = "tabIndicator",
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.92f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tabIconScale",
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = selected,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                            ) {
                                if (!selected) {
                                    haptics.tick()
                                    onSelect(index)
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 28.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.14f * indicatorAlpha)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = dest.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            color = tint,
                        )
                    }
                }
            }
        }
    }
}
