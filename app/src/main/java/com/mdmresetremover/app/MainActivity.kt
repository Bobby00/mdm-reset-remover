package com.mdmresetremover.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // Package names
    private const val PKG_PLAY_SERVICES = "com.google.android.gms"
    private const val PKG_PLAY_STORE = "com.android.vending"
    private const val PKG_AURORA_STORE = "com.auraos.aurora_store"

    // Aurora Store download URL (latest release from Aurora OSS)
    // Users can update this to point to the latest APK at:
    // https://www.auroraoss.com/files/AuroraStore/Release
    private const val AURORA_STORE_APK_URL =
        "https://www.auroraoss.com/files/AuroraStore/Release/com.auraos.aurora_store.apk"

    // UI references
    private lateinit var tvAdbStatus: TextView
    private lateinit var tvDeviceOwnerStatus: TextView
    private lateinit var tvPlayServicesStatus: TextView
    private lateinit var tvPlayStoreStatus: TextView
    private lateinit var tvAuroraStatus: TextView
    private lateinit var btnCheckAdb: Button
    private lateinit var btnCheckDeviceOwner: Button
    private lateinit var btnClearPlayServices: Button
    private lateinit var btnDisablePlayStore: Button
    private lateinit var btnInstallAurora: Button
    private lateinit var btnDoAll: Button

    // State
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private lateinit var sharedPrefs: SharedPreferences
    private var coroutineJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(this, DeviceAdminReceiver::class.java)
        sharedPrefs = getSharedPreferences("mdm_reset_remover", Context.MODE_PRIVATE)

        initViews()
        updateStatusUI()

        // Click listeners
        btnCheckAdb.setOnClickListener {
            checkAdbDebugging()
            updateStatusUI()
        }

        btnCheckDeviceOwner.setOnClickListener {
            checkDeviceOwner()
            updateStatusUI()
        }

        btnClearPlayServices.setOnClickListener {
            clearPlayServicesData()
        }

        btnDisablePlayStore.setOnClickListener {
            disablePlayStore()
        }

        btnInstallAurora.setOnClickListener {
            installAuroraStore()
        }

        btnDoAll.setOnClickListener {
            runFullSequence()
        }
    }

    private fun initViews() {
        tvAdbStatus = findViewById(R.id.tv_adb_status)
        tvDeviceOwnerStatus = findViewById(R.id.tv_device_owner_status)
        tvPlayServicesStatus = findViewById(R.id.tv_play_services_status)
        tvPlayStoreStatus = findViewById(R.id.tv_play_store_status)
        tvAuroraStatus = findViewById(R.id.tv_aurora_status)

        btnCheckAdb = findViewById(R.id.btn_check_adb)
        btnCheckDeviceOwner = findViewById(R.id.btn_check_device_owner)
        btnClearPlayServices = findViewById(R.id.btn_clear_play_services)
        btnDisablePlayStore = findViewById(R.id.btn_disable_play_store)
        btnInstallAurora = findViewById(R.id.btn_install_aurora)
        btnDoAll = findViewById(R.id.btn_do_all)
    }

    private fun updateStatusUI() {
        // ADB debugging
        val adbEnabled = isAdbDebuggingEnabled()
        tvAdbStatus.text = if (adbEnabled) "ADB Debugging: Enabled" else "ADB Debugging: Disabled"
        tvAdbStatus.setTextColor(
            if (adbEnabled) R.color.status_ok else R.color.status_warning
        )

        // Device Owner
        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        tvDeviceOwnerStatus.text =
            if (isDeviceOwner) "Device Owner: Active" else "Device Owner: Not Set"
        tvDeviceOwnerStatus.setTextColor(
            if (isDeviceOwner) R.color.status_ok else R.color.status_warning
        )

        // Play Services
        val psEnabled = isPackageEnabled(PKG_PLAY_SERVICES)
        tvPlayServicesStatus.text =
            if (psEnabled) "Play Services: Enabled" else "Play Services: Disabled"
        tvPlayServicesStatus.setTextColor(
            if (psEnabled) R.color.status_ok else R.color.status_warning
        )

        // Play Store
        val psStoreEnabled = isPackageEnabled(PKG_PLAY_STORE)
        tvPlayStoreStatus.text =
            if (psStoreEnabled) "Play Store: Enabled" else "Play Store: Disabled"
        tvPlayStoreStatus.setTextColor(
            if (psStoreEnabled) R.color.status_ok else R.color.status_warning
        )

        // Aurora Store
        val auroraInstalled = isPackageInstalled(PKG_AURORA_STORE)
        tvAuroraStatus.text =
            if (auroraInstalled) "Aurora Store: Installed" else "Aurora Store: Not Installed"
        tvAuroraStatus.setTextColor(
            if (auroraInstalled) R.color.status_ok else R.color.status_warning
        )

        // Enable/disable buttons based on state
        val canModifySystemApps = isDeviceOwner
        btnClearPlayServices.isEnabled = canModifySystemApps
        btnDisablePlayStore.isEnabled = canModifySystemApps
        btnDoAll.isEnabled = canModifySystemApps && adbEnabled
    }

    private fun checkAdbDebugging() {
        val enabled = isAdbDebuggingEnabled()
        if (enabled) {
            Toast.makeText(this, "ADB debugging is enabled", Toast.LENGTH_SHORT).show()
        } else {
            showAdbInstructionsDialog()
        }
    }

    private fun checkDeviceOwner() {
        val isOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        if (isOwner) {
            Toast.makeText(this, "This app is set as Device Owner", Toast.LENGTH_SHORT).show()
        } else {
            showDeviceOwnerSetupDialog()
        }
    }

    private fun isAdbDebuggingEnabled(): Boolean {
        return try {
            val adbEnabled =
                Settings.Secure.getInt(contentResolver, Settings.Secure.ADB_ENABLED, 0)
            adbEnabled == 1
        } catch (e: Exception) {
            false
        }
    }

    private fun isPackageEnabled(packageName: String): Boolean {
        return try {
            val info =
                packageManager.getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES)
            info.enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun clearPlayServicesData() {
        if (!devicePolicyManager.isDeviceOwnerApp(packageName)) {
            Toast.makeText(
                this,
                "Device Owner required to clear app data. Set as device owner first.",
                Toast.LENGTH_LONG
            ).show()
            showDeviceOwnerSetupDialog()
            return
        }

        try {
            packageManager.clearApplicationUserData(PKG_PLAY_SERVICES)
            Toast.makeText(this, "Play Services data cleared", Toast.LENGTH_SHORT).show()
            updateStatusUI()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Failed to clear Play Services data: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun disablePlayStore() {
        if (!devicePolicyManager.isDeviceOwnerApp(packageName)) {
            Toast.makeText(
                this,
                "Device Owner required to disable apps. Set as device owner first.",
                Toast.LENGTH_LONG
            ).show()
            showDeviceOwnerSetupDialog()
            return
        }

        try {
            devicePolicyManager.setApplicationHidden(componentName, PKG_PLAY_STORE, true)
            Toast.makeText(this, "Play Store disabled", Toast.LENGTH_SHORT).show()
            updateStatusUI()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Failed to disable Play Store: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun installAuroraStore() {
        // Check REQUEST_INSTALL_PACKAGES permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:$packageName"))
                startActivityForResult(intent, 1001)
                return
            }
        }

        downloadAndInstallApk(AURORA_STORE_APK_URL)
    }

    private fun runFullSequence() {
        // 1. Check ADB
        if (!isAdbDebuggingEnabled()) {
            showAdbInstructionsDialog()
            return
        }

        // 2. Check Device Owner
        if (!devicePolicyManager.isDeviceOwnerApp(packageName)) {
            showDeviceOwnerSetupDialog()
            return
        }

        // 3. Clear Play Services data
        clearPlayServicesData()

        // 4. Disable Play Store
        disablePlayStore()

        // 5. Install Aurora Store
        installAuroraStore()

        Toast.makeText(this, "Full sequence initiated. Check each step.", Toast.LENGTH_LONG).show()
    }

    private fun downloadAndInstallApk(url: String) {
        coroutineJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                withContext(Dispatchers.Main) {
                    // Show downloading state via toast
                    Toast.makeText(this@MainActivity, "Downloading Aurora Store...", Toast.LENGTH_SHORT).show()
                }

                val apkFile = ApkDownloader.downloadApk(
                    context = this@MainActivity,
                    url = url,
                    fileName = "AuroraStore.apk"
                )

                withContext(Dispatchers.Main) {
                    if (apkFile != null) {
                        installApkFile(apkFile)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to download Aurora Store APK",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Download error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun installApkFile(file: java.io.File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                FileProviderUtil.getUriForFile(this@MainActivity, file),
                "application/vnd.android.package-archive"
            )
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun showAdbInstructionsDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("ADB Debugging Not Enabled")
            .setMessage(
                "To use MDM Reset Remover, you need to enable ADB debugging and set this app as Device Owner.\n\n" +
                "1. Go to Settings > About Phone > tap Build Number 7 times to enable Developer Options\n" +
                "2. Go to Settings > System > Developer Options > enable USB Debugging\n" +
                "3. Connect your phone to a computer via USB\n" +
                "4. Run: adb shell dpm set-device-owner com.mdmresetremover.app/.DeviceAdminReceiver\n\n" +
                "Do you want to open Developer Options?"
            )
            .setPositiveButton("Open Developer Options") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Copy ADB Command") { _, _ ->
                android.content.ClipboardManager.getSystemService(this)
                    ?.setPrimaryClip(
                        android.content.ClipData.newPlainText(
                            "adb_command",
                            "adb shell dpm set-device-owner com.mdmresetremover.app/.DeviceAdminReceiver"
                        )
                    )
                Toast.makeText(this, "ADB command copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel") { _, _ -> }
            .create()
        dialog.show()
    }

    private fun showDeviceOwnerSetupDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Device Owner Not Set")
            .setMessage(
                "MDM Reset Remover needs to be set as Device Owner to disable apps and clear system data.\n\n" +
                "Make sure ADB debugging is enabled, then connect to a computer and run:\n" +
                "adb shell dpm set-device-owner com.mdmresetremover.app/.DeviceAdminReceiver\n\n" +
                "This requires the device to be factory reset or newly provisioned if a previous owner exists."
            )
            .setPositiveButton("Copy ADB Command") { _, _ ->
                android.content.ClipboardManager.getSystemService(this)
                    ?.setPrimaryClip(
                        android.content.ClipData.newPlainText(
                            "adb_command",
                            "adb shell dpm set-device-owner com.mdmresetremover.app/.DeviceAdminReceiver"
                        )
                    )
                Toast.makeText(this, "ADB command copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("OK") { _, _ -> }
            .create()
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineJob?.cancel()
    }
}

// Utility class for FileProvider URI
object FileProviderUtil {
    fun getUriForFile(context: Context, file: java.io.File): Uri {
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }
}