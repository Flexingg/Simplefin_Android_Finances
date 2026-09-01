package com.randallengineering.finances.ui.components

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.randallengineering.finances.core.backup.BackupManager
import com.randallengineering.finances.core.backup.DriveBackupManager
import com.randallengineering.finances.core.network.Resource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup & Export section: export the user's real transactions as CSV (share /
 * save to Downloads) or upload a CSV backup to Google Drive (opt-in, real Drive
 * REST upload into a visible "RandallFinances" folder).
 */
@Composable
fun BackupExportSection(
    driveSignInIntent: () -> Intent,
    csvProvider: suspend () -> String,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isBuilding by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    val driveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            scope.launch {
                isUploading = true
                val csv = csvProvider()
                val r = DriveBackupManager.backupCsv(context, csv, backupFileName())
                isUploading = false
                        onMessage(if (r is Resource.Success) "Backed up to Drive: ${r.data}" else (r as Resource.Error).message)
            }
        } else {
            onMessage("Drive backup cancelled.")
        }
    }

    fun exportCsv() {
        scope.launch {
            isBuilding = true
            val csv = csvProvider()
            isBuilding = false
            if (csv.isBlank()) { onMessage("Nothing to export yet — sync a bank or add transactions first."); return@launch }
            BackupManager.shareCsv(context, csv, backupFileName())
        }
    }

    fun saveToDownloads() {
        scope.launch {
            isBuilding = true
            val csv = csvProvider()
            isBuilding = false
            if (csv.isBlank()) { onMessage("Nothing to save yet — sync a bank or add transactions first."); return@launch }
            val uri = BackupManager.saveCsvToDownloads(context, csv, backupFileName())
            onMessage(if (uri != null) "Saved to Downloads/RandallFinances/" else "Saved to cache — use Export to share it.")
        }
    }

    fun backupToDrive() {
        scope.launch {
            isBuilding = true
            val csv = csvProvider()
            isBuilding = false
            if (csv.isBlank()) { onMessage("Nothing to back up yet — sync a bank or add transactions first."); return@launch }
            try {
                DriveBackupManager.getDriveToken(context) // throws if Drive not granted
                isUploading = true
                val r = DriveBackupManager.backupCsv(context, csv, backupFileName())
                isUploading = false
                onMessage(if (r is Resource.Success) "Backed up to Drive: ${r.data}" else (r as Resource.Error).message)
            } catch (e: GoogleAuthException) {
                // Need consent -> launch Drive-scoped sign-in
                driveLauncher.launch(driveSignInIntent())
            }
        }
    }

    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Backup, contentDescription = "Backup", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Backup & Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Export all your transactions as CSV, or back up a copy to Google Drive.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = ::exportCsv,
                    enabled = !isBuilding && !isUploading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export CSV")
                }
                OutlinedButton(
                    onClick = ::saveToDownloads,
                    enabled = !isBuilding && !isUploading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }
            }

            Button(
                onClick = ::backupToDrive,
                enabled = !isBuilding && !isUploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Backing up to Drive…")
                } else {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Backup to Google Drive", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun backupFileName(): String =
    "finances-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.csv"
