package com.smartagent.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.smartagent.app.data.ContentRequest
import com.smartagent.app.data.ContentPack
import com.smartagent.app.data.ContentSection
import com.smartagent.app.data.ContentStyle
import com.smartagent.app.data.ExtractionConfidence
import com.smartagent.app.data.GenerationRecord
import com.smartagent.app.data.GeminiClient
import com.smartagent.app.data.ImageUtils
import com.smartagent.app.data.InputMode
import com.smartagent.app.data.LocalRepository
import com.smartagent.app.data.OutputLanguage
import com.smartagent.app.data.Platform
import com.smartagent.app.data.ProductAccessBlockedException
import com.smartagent.app.data.ProductDetails
import com.smartagent.app.data.ProductLinkResolver
import com.smartagent.app.data.PromptBuilder
import com.smartagent.app.data.SecureKeyStore
import com.smartagent.app.data.asVariantText
import com.smartagent.app.ui.theme.SmartAgentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONTokener
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val incomingText = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readSharedText(intent)

        val secureKeyStore = SecureKeyStore(applicationContext)
        val repository = LocalRepository(applicationContext)
        val geminiClient = GeminiClient()

        setContent {
            SmartAgentTheme {
                SmartAgentApp(
                    incomingText = incomingText.value,
                    secureKeyStore = secureKeyStore,
                    repository = repository,
                    geminiClient = geminiClient
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readSharedText(intent)
    }

    private fun readSharedText(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            incomingText.value = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        }
    }
}

private enum class AppTab(val label: String, val symbol: String) {
    CREATE("Create", "✦"),
    HISTORY("History", "◷"),
    SETTINGS("Settings", "⚙")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartAgentApp(
    incomingText: String,
    secureKeyStore: SecureKeyStore,
    repository: LocalRepository,
    geminiClient: GeminiClient
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.CREATE) }
    var history by remember { mutableStateOf(repository.loadHistory()) }
    var keyConfigured by remember { mutableStateOf(secureKeyStore.hasApiKey()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SmartAgent", fontWeight = FontWeight.Bold)
                        Text(
                            "Your private content assistant",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.symbol) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                AppTab.CREATE -> CreateScreen(
                    incomingText = incomingText,
                    secureKeyStore = secureKeyStore,
                    repository = repository,
                    geminiClient = geminiClient,
                    keyConfigured = keyConfigured,
                    onOpenSettings = { selectedTab = AppTab.SETTINGS },
                    onRecordCreated = { record ->
                        history = repository.addHistory(record)
                    }
                )

                AppTab.HISTORY -> HistoryScreen(
                    records = history,
                    onClear = {
                        repository.clearHistory()
                        history = emptyList()
                    }
                )

                AppTab.SETTINGS -> SettingsScreen(
                    secureKeyStore = secureKeyStore,
                    repository = repository,
                    keyConfigured = keyConfigured,
                    onKeyStateChanged = { keyConfigured = it }
                )
            }
        }
    }
}

@Composable
private fun CreateScreen(
    incomingText: String,
    secureKeyStore: SecureKeyStore,
    repository: LocalRepository,
    geminiClient: GeminiClient,
    keyConfigured: Boolean,
    onOpenSettings: () -> Unit,
    onRecordCreated: (GenerationRecord) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by rememberSaveable { mutableStateOf(InputMode.AFFILIATE) }
    var productLink by rememberSaveable { mutableStateOf("") }
    var productName by rememberSaveable { mutableStateOf("") }
    var productPrice by rememberSaveable { mutableStateOf("") }
    var productDescription by rememberSaveable { mutableStateOf("") }
    var productFeatures by rememberSaveable { mutableStateOf("") }
    var productSeller by rememberSaveable { mutableStateOf("") }
    var productPromotion by rememberSaveable { mutableStateOf("") }
    var productFacts by rememberSaveable { mutableStateOf("") }
    var extractionSource by rememberSaveable { mutableStateOf("") }
    var extractionConfidence by remember { mutableStateOf<ExtractionConfidence?>(null) }
    var extractionWarning by rememberSaveable { mutableStateOf("") }
    var verifiedByUser by rememberSaveable { mutableStateOf(false) }
    var platform by rememberSaveable { mutableStateOf(Platform.TIKTOK) }
    var duration by rememberSaveable { mutableStateOf(30) }
    var language by rememberSaveable { mutableStateOf(OutputLanguage.MALAY) }
    var style by rememberSaveable { mutableStateOf(ContentStyle.UGC) }
    var audience by rememberSaveable { mutableStateOf("") }
    var variantCount by rememberSaveable { mutableStateOf(1) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var imageStatus by rememberSaveable { mutableStateOf("No screenshot selected") }
    var isReadingImage by remember { mutableStateOf(false) }
    var isExtracting by remember { mutableStateOf(false) }
    var extractionStatus by rememberSaveable { mutableStateOf("") }
    var browserUrl by rememberSaveable { mutableStateOf("") }
    var browserMessage by rememberSaveable { mutableStateOf("") }
    var showProductBrowser by remember { mutableStateOf(false) }
    var isProcessingCapture by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf<List<ContentPack>>(emptyList()) }
    var selectedVariant by rememberSaveable { mutableStateOf(0) }
    var currentRequest by remember { mutableStateOf<ContentRequest?>(null) }
    var currentRecordId by remember { mutableStateOf<Long?>(null) }
    var regeneratingSection by remember { mutableStateOf<ContentSection?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(incomingText) {
        if (incomingText.isNotBlank() && incomingText != productLink) {
            productLink = ProductLinkResolver.extractUrl(incomingText) ?: incomingText
            mode = InputMode.AFFILIATE
        }
    }

    val applyProductDetails: (ProductDetails) -> Unit = { product ->
        productLink = product.resolvedUrl.ifBlank { productLink }
        productName = product.name
        productPrice = product.price
        productDescription = product.description
        productFeatures = product.features.joinToString("\n")
        productSeller = product.seller
        productPromotion = product.promotion
        extractionSource = product.sourceLabel
        extractionConfidence = product.confidence
        extractionWarning = product.warning
        verifiedByUser = false
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isReadingImage = true
            imageStatus = "Reading screenshot..."
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        ImageUtils.readAndCompress(context.contentResolver, uri)
                    }
                }.onSuccess { encodedImage ->
                    imageBase64 = encodedImage
                    val key = secureKeyStore.loadApiKey()
                    if (key == null) {
                        imageStatus = "Screenshot ready. Add your Gemini API key to extract its details."
                    } else {
                        imageStatus = "Extracting product details from screenshot..."
                        val details = withContext(Dispatchers.IO) {
                            geminiClient.extractProductFromImage(
                                apiKey = key,
                                model = repository.loadModel(),
                                imageBase64 = encodedImage,
                                productUrl = productLink
                            )
                        }
                        details.onSuccess { product ->
                            applyProductDetails(product)
                            imageStatus = "Screenshot extracted. Check every field below."
                            extractionStatus = imageStatus
                            errorMessage = ""
                        }.onFailure { failure ->
                            imageStatus = "Screenshot attached, but its details could not be extracted."
                            errorMessage = failure.message ?: "Could not extract the screenshot"
                        }
                    }
                }.onFailure {
                    imageBase64 = null
                    imageStatus = "Could not read screenshot"
                    errorMessage = it.message ?: "Could not read the screenshot"
                }
                isReadingImage = false
            }
        }
    }

    if (showProductBrowser) {
        ProductBrowserDialog(
            initialUrl = browserUrl.ifBlank { productLink },
            isProcessing = isProcessingCapture,
            externalMessage = browserMessage,
            onDismiss = {
                if (!isProcessingCapture) showProductBrowser = false
            },
            onCapture = { capturedUrl, capturedPage ->
                val key = secureKeyStore.loadApiKey()
                if (key == null) {
                    showProductBrowser = false
                    errorMessage = "Add your Gemini API key in Settings first."
                    onOpenSettings()
                } else {
                    isProcessingCapture = true
                    browserMessage = ""
                    extractionStatus = "Understanding captured product page..."
                    scope.launch {
                        val details = withContext(Dispatchers.IO) {
                            geminiClient.extractProductFromCapturedPage(
                                apiKey = key,
                                model = repository.loadModel(),
                                productUrl = capturedUrl,
                                capturedPage = capturedPage
                            )
                        }
                        details.onSuccess { product ->
                            applyProductDetails(product)
                            extractionStatus = "Product details captured. Check them before generating."
                            errorMessage = ""
                            showProductBrowser = false
                        }.onFailure {
                            extractionStatus = "The open page did not contain enough product details."
                            browserMessage = it.message ?: "Could not understand the captured page"
                        }
                        isProcessingCapture = false
                    }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "From product to publish-ready content",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Paste or share a product link, extract its details, then generate your content pack.")
                }
            }
        }

        if (!keyConfigured) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add your free Gemini API key before generating.", Modifier.weight(1f))
                        TextButton(onClick = onOpenSettings) { Text("Set up") }
                    }
                }
            }
        }

        item {
            SectionTitle("1. What are you creating?")
            ChoiceRow(
                options = InputMode.entries,
                selected = mode,
                label = { it.label },
                onSelected = { mode = it }
            )
        }

        if (mode == InputMode.AFFILIATE) {
            item {
                OutlinedTextField(
                    value = productLink,
                    onValueChange = {
                        productLink = it
                        extractionSource = ""
                        extractionConfidence = null
                        extractionWarning = ""
                        verifiedByUser = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Shopee or TikTok product link") },
                    placeholder = { Text("Paste or share a product link") },
                    minLines = 2
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                errorMessage = ""
                                extractionStatus = ""
                                val key = secureKeyStore.loadApiKey()
                                when {
                                    key == null -> {
                                        errorMessage = "Add your Gemini API key in Settings first."
                                        onOpenSettings()
                                    }
                                    productLink.isBlank() -> {
                                        errorMessage = "Paste a Shopee or TikTok product link first."
                                    }
                                    ProductLinkResolver.extractUrl(productLink) == null -> {
                                        errorMessage = "Enter a complete product link beginning with https://"
                                    }
                                    else -> {
                                        isExtracting = true
                                        extractionStatus = "Reading product page..."
                                        scope.launch {
                                            val details = withContext(Dispatchers.IO) {
                                                geminiClient.extractProduct(
                                                    apiKey = key,
                                                    model = repository.loadModel(),
                                                    productUrl = productLink
                                                )
                                            }
                                            details.onSuccess { product ->
                                                applyProductDetails(product)
                                                if (product.requiresBrowserReview) {
                                                    browserUrl = product.resolvedUrl
                                                    browserMessage = "Only general TikTok information was available. Open the product card or yellow basket, then tap Use this page."
                                                    extractionStatus = "More product details are needed. Opening the page on this phone..."
                                                    showProductBrowser = true
                                                } else {
                                                    extractionStatus = "Product details extracted from ${product.sourceLabel}. Check every field below."
                                                }
                                            }.onFailure { failure ->
                                                if (failure is ProductAccessBlockedException) {
                                                    browserUrl = productLink
                                                    browserMessage = ""
                                                    extractionStatus = "Server access was blocked. Opening the page on this phone..."
                                                    errorMessage = ""
                                                    showProductBrowser = true
                                                } else {
                                                    extractionStatus = "Could not extract this product."
                                                    errorMessage = failure.message ?: "Product extraction failed"
                                                }
                                            }
                                            isExtracting = false
                                        }
                                    }
                                }
                            },
                            enabled = !isExtracting && !isReadingImage
                        ) {
                            Text("Extract product")
                        }
                        OutlinedButton(
                            onClick = { imagePicker.launch("image/*") },
                            enabled = !isReadingImage && !isExtracting
                        ) {
                            Text(if (imageBase64 == null) "Screenshot" else "Replace image")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val extractedUrl = ProductLinkResolver.extractUrl(productLink)
                            if (extractedUrl != null) {
                                browserUrl = extractedUrl
                                browserMessage = "Open the exact product card, then tap Use this page."
                                showProductBrowser = true
                                errorMessage = ""
                            } else {
                                errorMessage = "Enter a complete product link beginning with https://"
                            }
                        },
                        enabled = !isExtracting && !isReadingImage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open product page on this phone")
                    }
                    if (isExtracting || isReadingImage) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.width(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (isExtracting) extractionStatus else imageStatus,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else if (extractionStatus.isNotBlank()) {
                        Text(extractionStatus, style = MaterialTheme.typography.bodySmall)
                    } else if (imageBase64 != null) {
                        Text(imageStatus, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (mode == InputMode.AFFILIATE) {
            item {
                ProductVerificationFields(
                    productName = productName,
                    onProductNameChanged = { productName = it; verifiedByUser = false },
                    price = productPrice,
                    onPriceChanged = { productPrice = it; verifiedByUser = false },
                    seller = productSeller,
                    onSellerChanged = { productSeller = it; verifiedByUser = false },
                    promotion = productPromotion,
                    onPromotionChanged = { productPromotion = it; verifiedByUser = false },
                    description = productDescription,
                    onDescriptionChanged = { productDescription = it; verifiedByUser = false },
                    features = productFeatures,
                    onFeaturesChanged = { productFeatures = it; verifiedByUser = false },
                    additionalFacts = productFacts,
                    onAdditionalFactsChanged = { productFacts = it; verifiedByUser = false },
                    source = extractionSource,
                    confidence = extractionConfidence,
                    warning = extractionWarning,
                    verifiedByUser = verifiedByUser,
                    onVerifiedChanged = { verifiedByUser = it }
                )
            }
        } else {
            item {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Topic or content idea") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = productFacts,
                    onValueChange = { productFacts = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes") },
                    minLines = 4
                )
            }
        }

        item {
            SectionTitle(if (mode == InputMode.AFFILIATE) "3. Content settings" else "2. Content settings")
            Text("Platform", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                options = Platform.entries,
                selected = platform,
                label = { it.label },
                onSelected = { platform = it }
            )
            Spacer(Modifier.height(10.dp))
            Text("Duration", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                options = listOf(10, 15, 30, 60, 120),
                selected = duration,
                label = { "${it}s" },
                onSelected = { duration = it }
            )
            Spacer(Modifier.height(10.dp))
            Text("Language", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                options = OutputLanguage.entries,
                selected = language,
                label = { it.label },
                onSelected = { language = it }
            )
            Spacer(Modifier.height(10.dp))
            Text("Style", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                options = ContentStyle.entries,
                selected = style,
                label = { it.label },
                onSelected = { style = it }
            )
            Spacer(Modifier.height(10.dp))
            Text("Alternatives", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                options = listOf(1, 3),
                selected = variantCount,
                label = { if (it == 1) "1 pack" else "3 alternatives" },
                onSelected = { variantCount = it }
            )
        }

        item {
            OutlinedTextField(
                value = audience,
                onValueChange = { audience = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Target audience, optional") },
                placeholder = { Text("Example: Students, working mothers, new homeowners") },
                singleLine = true
            )
        }

        if (errorMessage.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        errorMessage,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    errorMessage = ""
                    val key = secureKeyStore.loadApiKey()
                    when {
                        key == null -> {
                            errorMessage = "Add your Gemini API key in Settings first."
                            onOpenSettings()
                        }
                        productName.isBlank() && productFacts.isBlank() && imageBase64 == null -> {
                            errorMessage = "Add a product name, product facts, topic, or screenshot."
                        }
                        else -> {
                            val verifiedFacts = if (mode == InputMode.AFFILIATE) {
                                buildString {
                                    fun appendField(label: String, value: String) {
                                        if (value.isNotBlank()) append(label).append(": ").append(value.trim()).append('\n')
                                    }
                                    appendField("Price", productPrice)
                                    appendField("Seller", productSeller)
                                    appendField("Promotion", productPromotion)
                                    appendField("Description", productDescription)
                                    if (productFeatures.isNotBlank()) {
                                        append("Features:\n")
                                        productFeatures.lineSequence()
                                            .map { it.trim().removePrefix("-").trim() }
                                            .filter { it.isNotBlank() }
                                            .forEach { append("- ").append(it).append('\n') }
                                    }
                                    if (productFacts.isNotBlank()) {
                                        append("Additional facts:\n").append(productFacts.trim()).append('\n')
                                    }
                                }.trim()
                            } else {
                                productFacts
                            }
                            val verificationSummary = if (mode == InputMode.AFFILIATE) {
                                buildList {
                                    if (extractionSource.isNotBlank()) add("Extracted from $extractionSource")
                                    extractionConfidence?.let { add(it.label) }
                                    add(if (verifiedByUser) "Confirmed by the user" else "Not yet confirmed by the user")
                                    if (extractionWarning.isNotBlank()) add("Warning: $extractionWarning")
                                }.joinToString(". ")
                            } else {
                                ""
                            }
                            val request = ContentRequest(
                                mode = mode,
                                productLink = productLink,
                                productName = productName,
                                productFacts = verifiedFacts,
                                verificationSummary = verificationSummary,
                                platform = platform,
                                durationSeconds = duration,
                                language = language,
                                style = style,
                                audience = audience,
                                variantCount = variantCount,
                                hasScreenshot = imageBase64 != null
                            )
                            isGenerating = true
                            output = emptyList()
                            selectedVariant = 0
                            currentRequest = request
                            scope.launch {
                                val response = withContext(Dispatchers.IO) {
                                    geminiClient.generate(
                                        apiKey = key,
                                        model = repository.loadModel(),
                                        prompt = PromptBuilder.build(request),
                                        variantCount = request.variantCount,
                                        imageBase64 = imageBase64,
                                        productUrl = productLink.takeIf {
                                            mode == InputMode.AFFILIATE && it.isNotBlank()
                                        }
                                    )
                                }
                                response.onSuccess { generated ->
                                    output = generated
                                    val now = System.currentTimeMillis()
                                    currentRecordId = now
                                    onRecordCreated(
                                        GenerationRecord(
                                            id = now,
                                            createdAt = now,
                                            title = productName.ifBlank { "Untitled content" },
                                            platform = platform.label,
                                            result = generated.asVariantText()
                                        )
                                    )
                                }.onFailure {
                                    errorMessage = it.message ?: "Content generation failed"
                                }
                                isGenerating = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !isGenerating && !isReadingImage && !isExtracting
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Creating content pack...")
                } else {
                    Text("Generate content pack")
                }
            }
        }

        if (output.isNotEmpty()) {
            item {
                ContentPackResult(
                    packs = output,
                    selectedVariant = selectedVariant,
                    onVariantSelected = { selectedVariant = it },
                    regeneratingSection = regeneratingSection,
                    onRegenerate = { section ->
                        val key = secureKeyStore.loadApiKey()
                        val request = currentRequest
                        val variantIndex = selectedVariant
                        val existingPack = output.getOrNull(variantIndex)
                        if (key == null || request == null || existingPack == null) {
                            errorMessage = "The current content settings are unavailable. Generate a new pack first."
                        } else {
                            errorMessage = ""
                            regeneratingSection = section
                            scope.launch {
                                val response = withContext(Dispatchers.IO) {
                                    geminiClient.regenerateSection(
                                        apiKey = key,
                                        model = repository.loadModel(),
                                        prompt = PromptBuilder.buildSection(request, section, existingPack),
                                        imageBase64 = imageBase64.takeIf { request.hasScreenshot },
                                        productUrl = request.productLink.takeIf {
                                            request.mode == InputMode.AFFILIATE && it.isNotBlank()
                                        }
                                    )
                                }
                                response.onSuccess { replacement ->
                                    val updated = existingPack.replace(section, replacement)
                                    val updatedPacks = output.toMutableList().also { packs ->
                                        packs[variantIndex] = updated
                                    }
                                    output = updatedPacks
                                    val recordId = currentRecordId ?: System.currentTimeMillis().also {
                                        currentRecordId = it
                                    }
                                    onRecordCreated(
                                        GenerationRecord(
                                            id = recordId,
                                            createdAt = System.currentTimeMillis(),
                                            title = productName.ifBlank { "Untitled content" },
                                            platform = platform.label,
                                            result = updatedPacks.asVariantText()
                                        )
                                    )
                                }.onFailure { failure ->
                                    errorMessage = failure.message ?: "Section regeneration failed"
                                }
                                regeneratingSection = null
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ContentPackResult(
    packs: List<ContentPack>,
    selectedVariant: Int,
    onVariantSelected: (Int) -> Unit,
    regeneratingSection: ContentSection?,
    onRegenerate: (ContentSection) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val pack = packs.getOrElse(selectedVariant) { packs.first() }
    val fullText = pack.asPlainText()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Your content pack")
        if (packs.size > 1) {
            ChoiceRow(
                options = packs.indices.toList(),
                selected = selectedVariant,
                label = { "Alternative ${it + 1}" },
                onSelected = onVariantSelected
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                clipboard.setText(AnnotatedString(fullText))
            }) { Text("Copy all") }

            OutlinedButton(onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, fullText)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share content pack"))
            }) { Text("Share") }
        }
        if (packs.size > 1) {
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(packs.asVariantText())) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copy all ${packs.size} alternatives")
            }
        }

        ContentSection.entries.forEach { section ->
            val content = pack.contentFor(section)
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(section.label, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(content))
                        }) { Text("Copy") }
                        TextButton(
                            onClick = { onRegenerate(section) },
                            enabled = regeneratingSection == null
                        ) {
                            if (regeneratingSection == section) {
                                CircularProgressIndicator(Modifier.width(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Regenerating")
                            } else {
                                Text("Regenerate")
                            }
                        }
                    }
                    Text(content, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ProductVerificationFields(
    productName: String,
    onProductNameChanged: (String) -> Unit,
    price: String,
    onPriceChanged: (String) -> Unit,
    seller: String,
    onSellerChanged: (String) -> Unit,
    promotion: String,
    onPromotionChanged: (String) -> Unit,
    description: String,
    onDescriptionChanged: (String) -> Unit,
    features: String,
    onFeaturesChanged: (String) -> Unit,
    additionalFacts: String,
    onAdditionalFactsChanged: (String) -> Unit,
    source: String,
    confidence: ExtractionConfidence?,
    warning: String,
    verifiedByUser: Boolean,
    onVerifiedChanged: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("2. Check product details")
        if (source.isNotBlank() || confidence != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(confidence?.label ?: "Extraction completed", fontWeight = FontWeight.Bold)
                    if (source.isNotBlank()) Text("Source: $source", style = MaterialTheme.typography.bodySmall)
                    if (warning.isNotBlank()) {
                        Text(
                            "Check: $warning",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = productName,
            onValueChange = onProductNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Product name") },
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = price,
                onValueChange = onPriceChanged,
                modifier = Modifier.weight(1f),
                label = { Text("Price") },
                placeholder = { Text("RM") },
                singleLine = true
            )
            OutlinedTextField(
                value = seller,
                onValueChange = onSellerChanged,
                modifier = Modifier.weight(1f),
                label = { Text("Seller") },
                singleLine = true
            )
        }
        OutlinedTextField(
            value = promotion,
            onValueChange = onPromotionChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Promotion") },
            placeholder = { Text("Leave blank if not shown") },
            singleLine = true
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description") },
            minLines = 2
        )
        OutlinedTextField(
            value = features,
            onValueChange = onFeaturesChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Verified features") },
            supportingText = { Text("Use one feature per line") },
            minLines = 3
        )
        OutlinedTextField(
            value = additionalFacts,
            onValueChange = onAdditionalFactsChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Additional verified facts, optional") },
            minLines = 2
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = verifiedByUser, onCheckedChange = onVerifiedChanged)
            Text("I checked the product name, price and claims shown above")
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ProductBrowserDialog(
    initialUrl: String,
    isProcessing: Boolean,
    externalMessage: String,
    onDismiss: () -> Unit,
    onCapture: (capturedUrl: String, capturedPage: String) -> Unit
) {
    var browser by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageLoaded by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf("Loading product page...") }
    var captureError by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            browser?.stopLoading()
            browser?.destroy()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isProcessing,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Open the product page", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Wait for the page to load. If TikTok shows a product card or yellow basket, open it first, then tap Use this page.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(pageTitle, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                if (!pageLoaded || isProcessing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.mediaPlaybackRequiresUserGesture = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrBlank()) pageTitle = title
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val scheme = request?.url?.scheme.orEmpty().lowercase()
                                    return scheme != "http" && scheme != "https"
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    pageLoaded = false
                                    if (!url.isNullOrBlank()) currentUrl = url
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (!url.isNullOrBlank()) currentUrl = url
                                    pageLoaded = true
                                }
                            }
                            browser = this
                            loadUrl(initialUrl)
                        }
                    }
                )

                val visibleError = captureError.ifBlank { externalMessage }
                if (visibleError.isNotBlank()) {
                    Text(visibleError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isProcessing
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val activeBrowser = browser
                            if (activeBrowser == null || !pageLoaded) {
                                captureError = "Wait for the page to finish loading."
                            } else {
                                captureError = ""
                                activeBrowser.evaluateJavascript(PAGE_CAPTURE_SCRIPT) { rawResult ->
                                    val capturedPage = decodeJavascriptResult(rawResult)
                                    if (capturedPage.isBlank()) {
                                        captureError = "Nothing was visible on this page. Open the product card and try again."
                                    } else {
                                        onCapture(currentUrl, capturedPage)
                                    }
                                }
                            }
                        },
                        enabled = pageLoaded && !isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isProcessing) "Reading page..." else "Use this page")
                    }
                }
            }
        }
    }
}

private fun decodeJavascriptResult(rawResult: String): String = runCatching {
    JSONTokener(rawResult).nextValue() as? String
}.getOrNull().orEmpty()

private val PAGE_CAPTURE_SCRIPT = """
    (function() {
      const meta = {};
      document.querySelectorAll('meta').forEach(function(item) {
        const key = item.getAttribute('property') || item.getAttribute('name');
        const value = item.getAttribute('content');
        if (key && value && /title|description|price|product|og:|twitter:/i.test(key)) {
          meta[key] = value;
        }
      });
      const jsonLd = [];
      document.querySelectorAll('script[type="application/ld+json"]').forEach(function(item) {
        if (item.textContent) jsonLd.push(item.textContent.slice(0, 12000));
      });
      const productText = [];
      document.querySelectorAll('[class*="product" i], [data-testid*="product" i], [class*="price" i], [data-testid*="price" i]').forEach(function(item) {
        const text = item.innerText || item.textContent || '';
        if (text.trim()) productText.push(text.trim().slice(0, 1500));
      });
      return JSON.stringify({
        url: location.href,
        title: document.title || '',
        metadata: meta,
        structuredData: jsonLd.slice(0, 8),
        productElements: productText.slice(0, 40),
        visibleText: (document.body && document.body.innerText ? document.body.innerText : '').slice(0, 24000)
      });
    })();
""".trimIndent()

@Composable
private fun HistoryScreen(
    records: List<GenerationRecord>,
    onClear: () -> Unit
) {
    var selectedRecord by remember { mutableStateOf<GenerationRecord?>(null) }
    val clipboard = LocalClipboardManager.current

    if (selectedRecord != null) {
        val record = selectedRecord!!
        AlertDialog(
            onDismissRequest = { selectedRecord = null },
            title = { Text(record.title) },
            text = {
                Box(
                    Modifier
                        .height(420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(record.result)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(record.result))
                }) { Text("Copy") }
            },
            dismissButton = {
                TextButton(onClick = { selectedRecord = null }) { Text("Close") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Generation history")
                if (records.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
            }
        }

        if (records.isEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(20.dp)) {
                        Text("No content yet", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("Your latest 50 content packs will be stored privately on this phone.")
                    }
                }
            }
        } else {
            items(records, key = { it.id }) { record ->
                Card(onClick = { selectedRecord = record }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(record.title, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${record.platform}  |  ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.createdAt))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            record.result.take(150).replace('\n', ' '),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    secureKeyStore: SecureKeyStore,
    repository: LocalRepository,
    keyConfigured: Boolean,
    onKeyStateChanged: (Boolean) -> Unit
) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf(repository.loadModel()) }
    var message by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionTitle("Private AI setup")
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (keyConfigured) "Gemini API key is configured" else "Gemini API key is not configured",
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
                Text("Your key is encrypted with Android Keystore and is not included in app backups.")
            }
        }

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (keyConfigured) "Replace Gemini API key" else "Gemini API key") },
            placeholder = { Text("Paste your key here") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )

        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Gemini model") },
            supportingText = { Text("Default: ${LocalRepository.DEFAULT_MODEL}") },
            singleLine = true
        )

        Button(
            onClick = {
                runCatching {
                    if (apiKey.isNotBlank()) secureKeyStore.saveApiKey(apiKey)
                    repository.saveModel(model)
                }.onSuccess {
                    apiKey = ""
                    onKeyStateChanged(secureKeyStore.hasApiKey())
                    message = "Settings saved"
                }.onFailure {
                    message = it.message ?: "Could not save settings"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = apiKey.isNotBlank() || keyConfigured
        ) {
            Text("Save settings")
        }

        if (keyConfigured) {
            OutlinedButton(
                onClick = {
                    secureKeyStore.clearApiKey()
                    onKeyStateChanged(false)
                    message = "API key removed"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remove API key")
            }
        }

        if (message.isNotBlank()) {
            Text(message, color = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider()
        Text("How to get a free key", fontWeight = FontWeight.Bold)
        Text(
            "Open Google AI Studio in your browser, create a Gemini API key, then paste it above. SmartAgent sends product information only when you tap Generate."
        )
        Text(
            "SmartAgent 0.5.0  |  Personal build",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(label(option)) }
            )
        }
    }
}
