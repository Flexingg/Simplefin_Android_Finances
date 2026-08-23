package com.randallengineering.finances

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.randallengineering.finances.core.theme.RandallFinancesTheme
import com.randallengineering.finances.ui.navigation.FinanceNavHost

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RandallFinancesTheme {
                FinanceNavHost()
            }
        }
    }
}
