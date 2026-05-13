package com.mibcxb.viewer.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.filled.ArrowBigLeft
import com.composables.icons.tabler.filled.CircleX
import com.composables.icons.tabler.filled.Eye
import com.composables.icons.tabler.filled.Folder
import com.composables.icons.tabler.filled.Lock
import com.composables.icons.tabler.filled.PlayerPlay
import com.composables.icons.tabler.filled.SquareRoundedPlus
import com.composables.icons.tabler.filled.Trash
import com.mibcxb.viewer.app.LocalAppRes
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.SmbConnection
import com.mibcxb.viewer.vm.SettingViewModel
import com.mibcxb.viewer.vm.SmbErrorType
import com.mibcxb.viewer_gui.generated.resources.Res
import com.mibcxb.viewer_gui.generated.resources.btn_cancel
import com.mibcxb.viewer_gui.generated.resources.btn_connect
import kotlinx.coroutines.launch
import com.mibcxb.viewer_gui.generated.resources.cd_add_connection
import com.mibcxb.viewer_gui.generated.resources.cd_back
import com.mibcxb.viewer_gui.generated.resources.cd_connect_saved
import com.mibcxb.viewer_gui.generated.resources.cd_delete_saved
import com.mibcxb.viewer_gui.generated.resources.cd_disconnect
import com.mibcxb.viewer_gui.generated.resources.cd_hide_password
import com.mibcxb.viewer_gui.generated.resources.cd_refresh_sessions
import com.mibcxb.viewer_gui.generated.resources.cd_show_password
import com.mibcxb.viewer_gui.generated.resources.error_smb_connect
import com.mibcxb.viewer_gui.generated.resources.error_smb_empty
import com.mibcxb.viewer_gui.generated.resources.ic_refresh
import com.mibcxb.viewer_gui.generated.resources.label_domain_opt
import com.mibcxb.viewer_gui.generated.resources.label_host
import com.mibcxb.viewer_gui.generated.resources.label_password
import com.mibcxb.viewer_gui.generated.resources.label_share
import com.mibcxb.viewer_gui.generated.resources.label_username
import com.mibcxb.viewer_gui.generated.resources.tab_samba
import com.mibcxb.viewer_gui.generated.resources.text_connected
import com.mibcxb.viewer_gui.generated.resources.text_disconnected
import com.mibcxb.viewer_gui.generated.resources.text_guest_anonymous
import com.mibcxb.viewer_gui.generated.resources.text_no_samba_share
import com.mibcxb.viewer_gui.generated.resources.text_samba_connections
import com.mibcxb.viewer_gui.generated.resources.text_save_connection
import com.mibcxb.viewer_gui.generated.resources.title_add_samba_conn
import com.mibcxb.widget.compose.Divider
import com.mibcxb.widget.compose.file.samba.SmbManager
import com.mibcxb.widget.compose.file.samba.SmbSession
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class SettingTab(val labelRes: StringResource) {
    Samba(Res.string.tab_samba)
}

private data class SmbEntry(
    val session: SmbSession?,
    val connection: SmbConnection?
) {
    val isConnected: Boolean get() = session != null
    val key: String get() = host + ":" + share
    val host: String get() = session?.host ?: connection?.host ?: ""
    val share: String get() = session?.share ?: connection?.share ?: ""
}

@Composable
fun SettingScreenView(
    cacheApi: CacheApi,
    smbManager: SmbManager,
    vm: SettingViewModel = viewModel { SettingViewModel(cacheApi, smbManager) },
    nav: NavController
) {
    val appRes = LocalAppRes.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabHeaderView(
            selectedTab = selectedTab,
            onBack = { nav.popBackStack() },
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(appRes.dimen.functionHeight)
                .padding(horizontal = appRes.dimen.paddingLarge)
        )
        Divider(appRes.dimen.dividerWidth, appRes.color.dividerNormal)
        when (SettingTab.entries[selectedTab]) {
            SettingTab.Samba -> SmbTabView(
                vm = vm,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun TabHeaderView(
    selectedTab: Int,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val appRes = LocalAppRes.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(appRes.dimen.iconButtonSize)
        ) {
            Icon(
                imageVector = Tabler.Filled.ArrowBigLeft,
                contentDescription = stringResource(Res.string.cd_back),
                modifier = Modifier.size(appRes.dimen.iconButtonSize)
            )
        }
        Spacer(modifier = Modifier.width(appRes.dimen.paddingPanel))
        val entries = SettingTab.entries
        entries.forEachIndexed { index, tab ->
            TextButton(
                onClick = { onTabSelected(index) },
                modifier = Modifier.padding(start = appRes.dimen.paddingSmall)
            ) {
                Text(
                    text = stringResource(tab.labelRes),
                    fontSize = 16.sp,
                    color = if (index == selectedTab) Color.Black else Color.Gray
                )
            }
        }
    }
}

@Composable
private fun SmbTabView(
    vm: SettingViewModel,
    modifier: Modifier = Modifier
) {
    val appRes = LocalAppRes.current
    val showDialog by remember { vm.showAddDialog }
    val smbError by remember { vm.smbError }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(appRes.dimen.functionHeight)
                .padding(horizontal = appRes.dimen.paddingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.text_samba_connections),
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { vm.refreshSessions() },
                modifier = Modifier.size(appRes.dimen.iconButtonSize)
            ) {
                Image(
                    painterResource(Res.drawable.ic_refresh),
                    contentDescription = stringResource(Res.string.cd_refresh_sessions),
                    modifier = Modifier.wrapContentSize()
                )
            }
            IconButton(
                onClick = { vm.showAddDialog() },
                modifier = Modifier.size(appRes.dimen.iconButtonSize)
            ) {
                Icon(
                    imageVector = Tabler.Filled.SquareRoundedPlus,
                    contentDescription = stringResource(Res.string.cd_add_connection),
                    modifier = Modifier.size(appRes.dimen.iconButtonSize)
                )
            }
        }
        Divider(appRes.dimen.dividerWidth, appRes.color.dividerNormal)
        val entries by remember {
            derivedStateOf {
                val savedList = vm.savedConnections
                val sessions = vm.smbSessions
                val connectedKeys = sessions.map { it.key }.toSet()
                buildList {
                    for (session in sessions) {
                        val saved = savedList.find { it.key == session.key }
                        add(SmbEntry(session, saved))
                    }
                    for (conn in savedList) {
                        if (conn.key !in connectedKeys) {
                            add(SmbEntry(null, conn))
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = appRes.dimen.paddingLarge)
        ) {
            if (entries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.text_no_samba_share),
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = appRes.dimen.paddingLarge)
                    )
                }
            }
            items(entries, key = { it.key }) { entry ->
                if (entry.isConnected) {
                    ConnectedEntryRow(entry.session!!, entry.connection, vm)
                } else {
                    SavedEntryRow(entry.connection!!, vm)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.loadSavedConnections()
        vm.refreshSessions()
    }

    if (showDialog) {
        SmbAddDialog(
            host = remember { vm.addHost }.value,
            share = remember { vm.addShare }.value,
            domain = remember { vm.addDomain }.value,
            username = remember { vm.addUsername }.value,
            password = remember { vm.addPassword }.value,
            anonymous = remember { vm.addAnonymous }.value,
            saveConnection = remember { vm.addSaveConnection }.value,
            error = smbError,
            onHostChange = { vm.changeAddHost(it) },
            onShareChange = { vm.changeAddShare(it) },
            onDomainChange = { vm.changeAddDomain(it) },
            onUsernameChange = { vm.changeAddUsername(it) },
            onPasswordChange = { vm.changeAddPassword(it) },
            onAnonymousChange = { vm.changeAddAnonymous(it) },
            onSaveConnectionChange = { vm.changeAddSaveConnection(it) },
            onConfirm = { coroutineScope.launch { vm.connectSmb() } },
            onDismiss = { vm.dismissAddDialog() }
        )
    }
}

@Composable
private fun SavedEntryRow(
    conn: SmbConnection,
    vm: SettingViewModel
) {
    val appRes = LocalAppRes.current
    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = appRes.dimen.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Tabler.Filled.Folder,
            contentDescription = null,
            modifier = Modifier.size(appRes.dimen.iconButtonSize),
            tint = Color.Gray
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = appRes.dimen.paddingPanel)
        ) {
            Text(text = "//${conn.host}/${conn.share}", fontSize = 14.sp)
            Text(
                text = stringResource(Res.string.text_disconnected),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        IconButton(
            onClick = { coroutineScope.launch { vm.connectFromSaved(conn) } },
            modifier = Modifier.size(appRes.dimen.iconButtonSize)
        ) {
            Icon(
                imageVector = Tabler.Filled.PlayerPlay,
                contentDescription = stringResource(Res.string.cd_connect_saved),
                modifier = Modifier.size(appRes.dimen.iconButtonSize),
                tint = Color(0xFF4CAF50)
            )
        }
        IconButton(
            onClick = { coroutineScope.launch { vm.deleteSavedConnection(conn) } },
            modifier = Modifier.size(appRes.dimen.iconButtonSize)
        ) {
            Icon(
                imageVector = Tabler.Filled.Trash,
                contentDescription = stringResource(Res.string.cd_delete_saved),
                modifier = Modifier.size(appRes.dimen.iconButtonSize)
            )
        }
    }
}

@Composable
private fun ConnectedEntryRow(
    session: SmbSession,
    saved: SmbConnection?,
    vm: SettingViewModel
) {
    val appRes = LocalAppRes.current
    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = appRes.dimen.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Tabler.Filled.Folder,
            contentDescription = null,
            modifier = Modifier.size(appRes.dimen.iconButtonSize),
            tint = Color(0xFF4CAF50)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = appRes.dimen.paddingPanel)
        ) {
            Text(text = "//${session.host}/${session.share}", fontSize = 14.sp)
            Text(
                text = stringResource(Res.string.text_connected),
                fontSize = 12.sp,
                color = Color(0xFF4CAF50)
            )
        }
        IconButton(
            onClick = { coroutineScope.launch { vm.disconnectSmb(session) } },
            modifier = Modifier.size(appRes.dimen.iconButtonSize)
        ) {
            Icon(
                imageVector = Tabler.Filled.CircleX,
                contentDescription = stringResource(Res.string.cd_disconnect),
                modifier = Modifier.size(appRes.dimen.iconButtonSize),
                tint = Color(0xFFFF9800)
            )
        }
        if (saved != null) {
            IconButton(
                onClick = { coroutineScope.launch { vm.deleteSavedConnection(saved) } },
                modifier = Modifier.size(appRes.dimen.iconButtonSize)
            ) {
                Icon(
                    imageVector = Tabler.Filled.Trash,
                    contentDescription = stringResource(Res.string.cd_delete_saved),
                    modifier = Modifier.size(appRes.dimen.iconButtonSize)
                )
            }
        }
    }
}

@Composable
private fun SmbAddDialog(
    host: String,
    share: String,
    domain: String,
    username: String,
    password: String,
    anonymous: Boolean,
    saveConnection: Boolean,
    error: SmbErrorType?,
    onHostChange: (String) -> Unit,
    onShareChange: (String) -> Unit,
    onDomainChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAnonymousChange: (Boolean) -> Unit,
    onSaveConnectionChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val appRes = LocalAppRes.current
    val errorText = when (error) {
        SmbErrorType.EmptyFields -> stringResource(Res.string.error_smb_empty)
        SmbErrorType.ConnectionFailed -> stringResource(Res.string.error_smb_connect)
        null -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.title_add_samba_conn)) },
        text = {
            Column {
                OutlinedTextField(
                    value = host,
                    onValueChange = onHostChange,
                    label = { Text(stringResource(Res.string.label_host)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(appRes.dimen.paddingPanel))
                OutlinedTextField(
                    value = share,
                    onValueChange = onShareChange,
                    label = { Text(stringResource(Res.string.label_share)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(appRes.dimen.paddingPanel))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = anonymous, onCheckedChange = onAnonymousChange)
                    Spacer(modifier = Modifier.width(appRes.dimen.paddingSmall))
                    Text(stringResource(Res.string.text_guest_anonymous))
                }
                if (!anonymous) {
                    Spacer(modifier = Modifier.height(appRes.dimen.paddingPanel))
                    OutlinedTextField(
                        value = domain,
                        onValueChange = onDomainChange,
                        label = { Text(stringResource(Res.string.label_domain_opt)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(appRes.dimen.paddingPanel))
                    OutlinedTextField(
                        value = username,
                        onValueChange = onUsernameChange,
                        label = { Text(stringResource(Res.string.label_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(appRes.dimen.paddingPanel))
                    var passwordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text(stringResource(Res.string.label_password)) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Tabler.Filled.Eye else Tabler.Filled.Lock,
                                    contentDescription = if (passwordVisible) {
                                        stringResource(Res.string.cd_hide_password)
                                    } else {
                                        stringResource(Res.string.cd_show_password)
                                    },
                                    modifier = Modifier.size(appRes.dimen.iconButtonSize)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(appRes.dimen.paddingPanel))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = saveConnection, onCheckedChange = onSaveConnectionChange)
                    Spacer(modifier = Modifier.width(appRes.dimen.paddingSmall))
                    Text(stringResource(Res.string.text_save_connection))
                }
                if (errorText != null) {
                    Spacer(modifier = Modifier.height(appRes.dimen.paddingPanel))
                    Text(
                        text = errorText,
                        color = Color.Red,
                        fontSize = appRes.dimen.menuTextSize
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.btn_connect))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.btn_cancel))
            }
        }
    )
}
