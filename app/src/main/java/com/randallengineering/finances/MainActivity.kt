package com.randallengineering.finances

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.core.theme.RandallFinancesTheme
import com.randallengineering.finances.data.repository.AmazonRepository
import com.randallengineering.finances.ui.navigation.FinanceNavHost
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val amazonRepository: AmazonRepository by inject()
    private val processedAuthCodes = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            RandallFinancesTheme {
                FinanceNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null) {
            val code = data.getQueryParameter("code") ?: data.getQueryParameter("spapi_oauth_code")
            val error = data.getQueryParameter("error")
            val errorDesc = data.getQueryParameter("error_description")

            if (!code.isNullOrBlank() && !processedAuthCodes.contains(code)) {
                processedAuthCodes.add(code)
                lifecycleScope.launch {
                    when (val result = amazonRepository.exchangeOAuthCode(code)) {
                        is Resource.Success -> {
                            Toast.makeText(this@MainActivity, "✅ Amazon Account Connected Successfully!", Toast.LENGTH_LONG).show()
                        }
                        is Resource.Error -> {
                            // If already connected, suppress error
                            if (!amazonRepository.isConnected()) {
                                Toast.makeText(this@MainActivity, "Amazon Linking: ${result.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        is Resource.Loading -> Unit
                    }
                }
            } else if (!error.isNullOrBlank()) {
                Toast.makeText(this, "Amazon Authorization: $errorDesc", Toast.LENGTH_LONG).show()
            }
        }
    }
}
