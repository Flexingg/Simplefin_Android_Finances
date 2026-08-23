package com.randallengineering.finances.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.AmazonDomExtractor
import com.randallengineering.finances.core.util.AmazonTransactionMatchCandidate
import com.randallengineering.finances.core.util.AmazonTransactionMatcher
import com.randallengineering.finances.data.repository.AmazonRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.MatchedAmazonOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val AmazonAmber = Color(0xFFFF9900)
private val AmazonDark = Color(0xFF131A22)

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmazonOrdersBrowserSheet(
    targetAmount: Double? = null,
    onOrdersImported: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val amazonRepository: AmazonRepository = koinInject()
    val transactionRepository: TransactionRepository = koinInject()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isScanning by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    var reviewCandidates by remember { mutableStateOf<List<AmazonTransactionMatchCandidate>?>(null) }
    var reviewOrders by remember { mutableStateOf<List<MatchedAmazonOrder>?>(null) }

    val initialUrl = "https://www.amazon.com/your-orders/orders"

    // If review state is active, show the Review & Match Dialog
    if (reviewCandidates != null && reviewOrders != null) {
        AmazonScanReviewDialog(
            candidates = reviewCandidates!!,
            allScannedOrders = reviewOrders!!,
            onDismiss = {
                reviewCandidates = null
                reviewOrders = null
                onOrdersImported(reviewOrders?.size ?: 0)
                onDismiss()
            }
        )
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(Shapes.small)
                                    .background(AmazonAmber),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = AmazonDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Amazon Orders Scanner",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Scroll to orders & tap ⚡ AI Ingest below",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { webViewInstance?.goBack() },
                            enabled = canGoBack
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        IconButton(
                            onClick = { webViewInstance?.goForward() },
                            enabled = canGoForward
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                        }
                        IconButton(onClick = { webViewInstance?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1-Tap AI Ingest Button
                        Button(
                            onClick = {
                                val webView = webViewInstance ?: return@Button
                                isScanning = true
                                webView.evaluateJavascript(AmazonDomExtractor.EXTRACTION_JS) { resultJson ->
                                    scope.launch {
                                        val parsed = AmazonDomExtractor.parseExtractedJson(resultJson.orEmpty())
                                        if (parsed.isNotEmpty()) {
                                            amazonRepository.importExtractedOrders(parsed)
                                            
                                            // Get bank transactions and compute matches
                                            val txResource = transactionRepository.getTransactionsFlow().first()
                                            val transactions = (txResource as? Resource.Success)?.data.orEmpty()
                                            val matches = AmazonTransactionMatcher.findMatches(parsed, transactions)

                                            reviewOrders = parsed
                                            reviewCandidates = matches
                                        } else {
                                            // Fallback to screen capture
                                            captureAndScanScreenshot(
                                                webView = webView,
                                                repository = amazonRepository,
                                                context = context,
                                                onSuccess = {
                                                    onOrdersImported(1)
                                                    onDismiss()
                                                },
                                                onDismiss = onDismiss
                                            )
                                        }
                                        isScanning = false
                                    }
                                }
                            },
                            enabled = !isScanning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = Shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmazonAmber,
                                contentColor = AmazonDark
                            )
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AmazonDark)
                                Spacer(Modifier.width(10.dp))
                                Text("AI Ingesting & Categorizing...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("⚡ 1-Tap AI Ingest & Categorize", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Full Screen Native Scrolling WebView
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            isVerticalScrollBarEnabled = true
                            isHorizontalScrollBarEnabled = false
                            isNestedScrollingEnabled = true

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.userAgentString = settings.userAgentString.replace("; wv", "")

                            CookieManager.getInstance().setAcceptCookie(true)

                            // Ensure touches always route to WebView scroll
                            setOnTouchListener { v, event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                        v.parent?.requestDisallowInterceptTouchEvent(true)
                                    }
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        v.parent?.requestDisallowInterceptTouchEvent(false)
                                    }
                                }
                                false
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    canGoBack = canGoBack()
                                    canGoForward = canGoForward()
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    canGoBack = canGoBack()
                                    canGoForward = canGoForward()
                                }
                            }

                            loadUrl(initialUrl)
                            webViewInstance = this
                        }
                    }
                )

                // Top Loading Indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = AmazonAmber
                    )
                }

                // Quick Scroll Helper Floating Buttons (Right Side)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FloatingActionButton(
                        onClick = { webViewInstance?.scrollBy(0, -700) },
                        modifier = Modifier.size(42.dp),
                        shape = Shapes.small,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Scroll Up", modifier = Modifier.size(18.dp))
                    }

                    FloatingActionButton(
                        onClick = { webViewInstance?.scrollBy(0, 700) },
                        modifier = Modifier.size(42.dp),
                        shape = Shapes.small,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll Down", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun captureAndScanScreenshot(
    webView: WebView,
    repository: AmazonRepository,
    context: android.content.Context,
    onSuccess: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    try {
        val width = webView.width.coerceAtLeast(1)
        val height = webView.height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webView.draw(canvas)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            when (val res = repository.parseScreenshotWithAi(bitmap)) {
                is Resource.Success -> {
                    Toast.makeText(context, "✅ AI Vision Ingested ${res.data} Amazon Orders!", Toast.LENGTH_LONG).show()
                    onSuccess(res.data ?: 0)
                    onDismiss()
                }
                is Resource.Error -> {
                    Toast.makeText(context, "Please scroll to the order list and tap AI Ingest.", Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> Unit
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to capture screen: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
