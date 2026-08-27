package com.smartagent.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import com.smartagent.app.data.BrandVoiceProfile
import com.smartagent.app.data.ContentStyle
import com.smartagent.app.data.ExtractionConfidence
import com.smartagent.app.data.FlowPromptPack
import com.smartagent.app.data.GenerationRecord
import com.smartagent.app.data.GeminiClient
import com.smartagent.app.data.ImageUtils
import com.smartagent.app.data.InputMode
import com.smartagent.app.data.LocalRepository
import com.smartagent.app.data.MarketingAngle
import com.smartagent.app.data.OutputLanguage
import com.smartagent.app.data.Platform
import com.smartagent.app.data.ProductAccessBlockedException
import com.smartagent.app.data.ProductDetails
import com.smartagent.app.data.ProductLinkResolver
import com.smartagent.app.data.PromptBuilder
import com.smartagent.app.data.SecureKeyStore
import com.smartagent.app.data.SavedProduct
import com.smartagent.app.data.ThreadLength
import com.smartagent.app.data.ThreadLinkPlacement
import com.smartagent.app.data.ThreadsPack
import com.smartagent.app.data.asVariantText
import com.smartagent.app.data.asThreadsVariantText
import com.smartagent.app.data.recommendedVoiceOverWords
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
    PRODUCTS("Products", "▣"),
    HISTORY("Library", "◷"),
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
    var savedProducts by remember { mutableStateOf(repository.loadSavedProducts()) }
    var productToLoad by remember { mutableStateOf<SavedProduct?>(null) }
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
                    productToLoad = productToLoad,
                    onProductLoaded = { productToLoad = null },
                    onProductSaved = { product ->
                        savedProducts = repository.saveProduct(product)
                    },
                    onOpenSettings = { selectedTab = AppTab.SETTINGS },
                    onRecordCreated = { record ->
                        history = repository.addHistory(record)
                    }
                )

                AppTab.PRODUCTS -> ProductsScreen(
                    products = savedProducts,
                    onUse = { product ->
                        productToLoad = product
                        selectedTab = AppTab.CREATE
                    },
                    onDelete = { product ->
                        savedProducts = repository.deleteProduct(product.id)
                    }
                )

                AppTab.HISTORY -> HistoryScreen(
                    records = history,
                    onToggleFavourite = { record ->
                        history = repository.toggleHistoryFavourite(record.id)
                    },
                    onDelete = { record ->
                        history = repository.deleteHistory(record.id)
                    },
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
    productToLoad: SavedProduct?,
    onProductLoaded: () -> Unit,
    onProductSaved: (SavedProduct) -> Unit,
    onOpenSettings: () -> Unit,
    onRecordCreated: (GenerationRecord) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedBrandVoice = remember { repository.loadBrandVoice() }

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
    var marketingAngle by rememberSaveable { mutableStateOf(MarketingAngle.AUTO) }
    var audience by rememberSaveable { mutableStateOf("") }
    var variantCount by rememberSaveable { mutableStateOf(1) }
    var threadLength by rememberSaveable { mutableStateOf(ThreadLength.STANDARD) }
    var threadLinkPlacement by rememberSaveable { mutableStateOf(ThreadLinkPlacement.FINAL_REPLY) }
    var useBrandVoice by rememberSaveable { mutableStateOf(savedBrandVoice.isConfigured()) }
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
    var threadsOutput by remember { mutableStateOf<List<ThreadsPack>>(emptyList()) }
    var flowPromptPack by remember { mutableStateOf<FlowPromptPack?>(null) }
    var isGeneratingFlowPrompts by remember { mutableStateOf(false) }
    var flowPromptError by rememberSaveable { mutableStateOf("") }
    var selectedVariant by rememberSaveable { mutableStateOf(0) }
    var currentRequest by remember { mutableStateOf<ContentRequest?>(null) }
    var currentRecordId by remember { mutableStateOf<Long?>(null) }
    var regeneratingSection by remember { mutableStateOf<ContentSection?>(null) }
    var regeneratingThreadPost by remember { mutableStateOf<Int?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var editingSavedProductId by rememberSaveable { mutableStateOf<Long?>(null) }
    var savedProductMessage by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(incomingText) {
        if (incomingText.isNotBlank() && incomingText != productLink) {
            productLink = ProductLinkResolver.extractUrl(incomingText) ?: incomingText
            mode = InputMode.AFFILIATE
            editingSavedProductId = null
            savedProductMessage = ""
        }
    }

    LaunchedEffect(productToLoad?.id, productToLoad?.updatedAt) {
        productToLoad?.let { product ->
            mode = InputMode.AFFILIATE
            productLink = product.link
            productName = product.name
            productPrice = product.price
            productSeller = product.seller
            productPromotion = product.promotion
            productDescription = product.description
            productFeatures = product.features
            productFacts = product.additionalFacts
            extractionSource = "Saved product library"
            extractionConfidence = ExtractionConfidence.MEDIUM
            extractionWarning = "Check whether the price or promotion has changed since this product was saved."
            verifiedByUser = false
            editingSavedProductId = product.id
            savedProductMessage = "Loaded from your product library."
            imageBase64 = null
            output = emptyList()
            threadsOutput = emptyList()
            flowPromptPack = null
            flowPromptError = ""
            currentRequest = null
            onProductLoaded()
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
        savedProductMessage = ""
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
                        editingSavedProductId = null
                        savedProductMessage = ""
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
                    onVerifiedChanged = { verifiedByUser = it },
                    savedProductMessage = savedProductMessage,
                    isSavedProduct = editingSavedProductId != null,
                    onSaveProduct = {
                        val now = System.currentTimeMillis()
                        val product = SavedProduct(
                            id = editingSavedProductId ?: now,
                            updatedAt = now,
                            link = productLink.trim(),
                            name = productName.trim(),
                            price = productPrice.trim(),
                            seller = productSeller.trim(),
                            promotion = productPromotion.trim(),
                            description = productDescription.trim(),
                            features = productFeatures.trim(),
                            additionalFacts = productFacts.trim()
                        )
                        onProductSaved(product)
                        editingSavedProductId = product.id
                        savedProductMessage = "Product saved privately on this phone."
                    }
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
                onSelected = {
                    platform = it
                    output = emptyList()
                    threadsOutput = emptyList()
                    flowPromptPack = null
                    flowPromptError = ""
                    currentRequest = null
                }
            )
            if (platform == Platform.THREADS) {
                Spacer(Modifier.height(10.dp))
                Text("Thread length", style = MaterialTheme.typography.labelLarge)
                ChoiceRow(
                    options = ThreadLength.entries,
                    selected = threadLength,
                    label = { it.label },
                    onSelected = { threadLength = it }
                )
                if (mode == InputMode.AFFILIATE) {
                    Spacer(Modifier.height(10.dp))
                    Text("Affiliate link placement", style = MaterialTheme.typography.labelLarge)
                    ChoiceRow(
                        options = ThreadLinkPlacement.entries,
                        selected = threadLinkPlacement,
                        label = { it.label },
                        onSelected = { threadLinkPlacement = it }
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "SmartAgent will generate one main post followed by separate replies in posting order.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Spacer(Modifier.height(10.dp))
                Text("Duration", style = MaterialTheme.typography.labelLarge)
                ChoiceRow(
                    options = listOf(10, 15, 30, 60, 120),
                    selected = duration,
                    label = { "${it}s" },
                    onSelected = { duration = it }
                )
            }
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
            Text("Marketing angle", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                options = MarketingAngle.entries,
                selected = marketingAngle,
                label = { it.label },
                onSelected = { marketingAngle = it }
            )
            Spacer(Modifier.height(10.dp))
            Text("Alternatives", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                options = listOf(1, 3),
                selected = variantCount,
                label = { if (it == 1) "1 pack" else "3 alternatives" },
                onSelected = { variantCount = it }
            )
            if (savedBrandVoice.isConfigured()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = useBrandVoice, onCheckedChange = { useBrandVoice = it })
                    Column {
                        Text("Use saved brand voice", fontWeight = FontWeight.Bold)
                        Text(
                            savedBrandVoice.name.ifBlank { "Personal profile" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
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
                                marketingAngle = marketingAngle,
                                audience = audience,
                                variantCount = variantCount,
                                threadReplyCount = threadLength.replyCount,
                                threadLinkPlacement = if (mode == InputMode.AFFILIATE) {
                                    threadLinkPlacement
                                } else {
                                    ThreadLinkPlacement.OMIT
                                },
                                brandVoice = if (useBrandVoice) savedBrandVoice else BrandVoiceProfile(),
                                hasScreenshot = imageBase64 != null
                            )
                            isGenerating = true
                            output = emptyList()
                            threadsOutput = emptyList()
                            flowPromptPack = null
                            flowPromptError = ""
                            selectedVariant = 0
                            currentRequest = request
                            scope.launch {
                                if (request.platform == Platform.THREADS) {
                                    val response = withContext(Dispatchers.IO) {
                                        geminiClient.generateThreads(
                                            apiKey = key,
                                            model = repository.loadModel(),
                                            prompt = PromptBuilder.buildThreads(request),
                                            variantCount = request.variantCount,
                                            replyCount = request.threadReplyCount,
                                            imageBase64 = imageBase64,
                                            productUrl = productLink.takeIf {
                                                mode == InputMode.AFFILIATE && it.isNotBlank()
                                            }
                                        )
                                    }
                                    response.onSuccess { generated ->
                                        threadsOutput = generated
                                        val now = System.currentTimeMillis()
                                        currentRecordId = now
                                        onRecordCreated(
                                            GenerationRecord(
                                                id = now,
                                                createdAt = now,
                                                title = productName.ifBlank { "Untitled thread" },
                                                platform = platform.label,
                                                result = generated.asThreadsVariantText()
                                            )
                                        )
                                    }.onFailure {
                                        errorMessage = it.message ?: "Threads generation failed"
                                    }
                                } else {
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
                                                result = generated.asVariantText(platform)
                                            )
                                        )
                                    }.onFailure {
                                        errorMessage = it.message ?: "Content generation failed"
                                    }
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
                    Text(if (platform == Platform.THREADS) "Creating thread..." else "Creating content pack...")
                } else {
                    Text(if (platform == Platform.THREADS) "Generate Threads chain" else "Generate content pack")
                }
            }
        }

        if (threadsOutput.isNotEmpty()) {
            item {
                ThreadsPackResult(
                    packs = threadsOutput,
                    selectedVariant = selectedVariant,
                    onVariantSelected = { selectedVariant = it },
                    regeneratingPostIndex = regeneratingThreadPost,
                    onRegenerate = { postIndex ->
                        val key = secureKeyStore.loadApiKey()
                        val request = currentRequest
                        val variantIndex = selectedVariant
                        val existingPack = threadsOutput.getOrNull(variantIndex)
                        if (key == null || request == null || existingPack == null) {
                            errorMessage = "The current thread settings are unavailable. Generate a new thread first."
                        } else {
                            errorMessage = ""
                            regeneratingThreadPost = postIndex
                            scope.launch {
                                val response = withContext(Dispatchers.IO) {
                                    geminiClient.regenerateSection(
                                        apiKey = key,
                                        model = repository.loadModel(),
                                        prompt = PromptBuilder.buildThreadPost(request, postIndex, existingPack),
                                        imageBase64 = imageBase64.takeIf { request.hasScreenshot },
                                        productUrl = request.productLink.takeIf {
                                            request.mode == InputMode.AFFILIATE && it.isNotBlank()
                                        }
                                    )
                                }
                                response.onSuccess { replacement ->
                                    val updated = existingPack.replacePost(postIndex, replacement)
                                    val updatedPacks = threadsOutput.toMutableList().also { packs ->
                                        packs[variantIndex] = updated
                                    }
                                    threadsOutput = updatedPacks
                                    val recordId = currentRecordId ?: System.currentTimeMillis().also {
                                        currentRecordId = it
                                    }
                                    onRecordCreated(
                                        GenerationRecord(
                                            id = recordId,
                                            createdAt = System.currentTimeMillis(),
                                            title = productName.ifBlank { "Untitled thread" },
                                            platform = Platform.THREADS.label,
                                            result = updatedPacks.asThreadsVariantText()
                                        )
                                    )
                                }.onFailure { failure ->
                                    errorMessage = failure.message ?: "Thread post regeneration failed"
                                }
                                regeneratingThreadPost = null
                            }
                        }
                    }
                )
            }
        }

        if (output.isNotEmpty()) {
            item {
                ContentPackResult(
                    packs = output,
                    selectedVariant = selectedVariant,
                    onVariantSelected = {
                        selectedVariant = it
                        flowPromptPack = null
                        flowPromptError = ""
                    },
                    platform = currentRequest?.platform ?: platform,
                    durationSeconds = currentRequest?.durationSeconds ?: duration,
                    regeneratingSection = regeneratingSection,
                    flowPromptPack = flowPromptPack,
                    isGeneratingFlowPrompts = isGeneratingFlowPrompts,
                    flowPromptError = flowPromptError,
                    onGenerateFlowPrompts = {
                        val key = secureKeyStore.loadApiKey()
                        val request = currentRequest
                        val existingPack = output.getOrNull(selectedVariant)
                        if (key == null || request == null || existingPack == null) {
                            flowPromptError = "Generate a content pack first."
                        } else {
                            flowPromptError = ""
                            isGeneratingFlowPrompts = true
                            scope.launch {
                                val response = withContext(Dispatchers.IO) {
                                    geminiClient.generateFlowPromptPack(
                                        apiKey = key,
                                        model = repository.loadModel(),
                                        prompt = PromptBuilder.buildFlowPromptPack(request, existingPack)
                                    )
                                }
                                response.onSuccess { generated ->
                                    flowPromptPack = generated
                                }.onFailure { failure ->
                                    flowPromptError = failure.message ?: "Flow prompt generation failed"
                                }
                                isGeneratingFlowPrompts = false
                            }
                        }
                    },
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
                                    flowPromptPack = null
                                    flowPromptError = ""
                                    val recordId = currentRecordId ?: System.currentTimeMillis().also {
                                        currentRecordId = it
                                    }
                                    onRecordCreated(
                                        GenerationRecord(
                                            id = recordId,
                                            createdAt = System.currentTimeMillis(),
                                            title = productName.ifBlank { "Untitled content" },
                                            platform = platform.label,
                                            result = updatedPacks.asVariantText(platform)
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
    platform: Platform,
    durationSeconds: Int,
    regeneratingSection: ContentSection?,
    flowPromptPack: FlowPromptPack?,
    isGeneratingFlowPrompts: Boolean,
    flowPromptError: String,
    onGenerateFlowPrompts: () -> Unit,
    onRegenerate: (ContentSection) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val pack = packs.getOrElse(selectedVariant) { packs.first() }
    val fullText = pack.asPlainText(platform)

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
                onClick = { clipboard.setText(AnnotatedString(packs.asVariantText(platform))) },
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
                    Text(section.displayLabel(platform), fontWeight = FontWeight.Bold)
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
                    if (
                        section == ContentSection.VOICE_OVER &&
                        platform in listOf(Platform.TIKTOK, Platform.SHOPEE_VIDEO, Platform.INSTAGRAM_REELS)
                    ) {
                        val words = content.trim().split(Regex("\\s+")).count { it.isNotBlank() }
                        val target = recommendedVoiceOverWords(durationSeconds)
                        val fitsTarget = words in target
                        Text(
                            "$words words. Target for ${durationSeconds}s: ${target.first} to ${target.last} words.",
                            color = if (fitsTarget) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(content, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (platform in listOf(Platform.TIKTOK, Platform.SHOPEE_VIDEO, Platform.INSTAGRAM_REELS)) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Optional Flow Prompt Pack", fontWeight = FontWeight.Bold)
                    Text(
                        "Turn this approved script into visual prompts to copy into Flow or another production platform. SmartAgent does not create video or audio.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = onGenerateFlowPrompts,
                        enabled = !isGeneratingFlowPrompts,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGeneratingFlowPrompts) {
                            CircularProgressIndicator(Modifier.width(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Creating prompts")
                        } else {
                            Text(if (flowPromptPack == null) "Create Flow prompts" else "Regenerate Flow prompts")
                        }
                    }
                    if (flowPromptError.isNotBlank()) {
                        Text(
                            flowPromptError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            flowPromptPack?.let { FlowPromptPackResult(it) }
        }
    }
}

@Composable
private fun FlowPromptPackResult(pack: FlowPromptPack) {
    val clipboard = LocalClipboardManager.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = { clipboard.setText(AnnotatedString(pack.asPlainText())) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Copy complete prompt pack")
        }

        PromptCopyCard(
            title = "Master visual prompt",
            content = pack.masterPrompt
        )
        pack.scenes.forEach { scene ->
            PromptCopyCard(
                title = "Scene ${scene.sceneNumber}: ${scene.sceneTitle}",
                content = scene.prompt
            )
        }
        PromptCopyCard(
            title = "Continuity prompt",
            content = pack.continuityPrompt
        )
        PromptCopyCard(
            title = "Negative prompt",
            content = pack.negativePrompt
        )
        PromptCopyCard(
            title = "Usage notes",
            content = pack.usageNotes
        )
    }
}

@Composable
private fun PromptCopyCard(title: String, content: String) {
    val clipboard = LocalClipboardManager.current
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            TextButton(onClick = { clipboard.setText(AnnotatedString(content)) }) {
                Text("Copy prompt")
            }
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ThreadsPackResult(
    packs: List<ThreadsPack>,
    selectedVariant: Int,
    onVariantSelected: (Int) -> Unit,
    regeneratingPostIndex: Int?,
    onRegenerate: (Int) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val pack = packs.getOrElse(selectedVariant) { packs.first() }
    val posts = pack.posts()
    var nextCopyIndex by remember(selectedVariant, posts.size) { mutableStateOf(0) }
    var copyMessage by remember(selectedVariant) { mutableStateOf("") }

    fun copyPost(index: Int) {
        val content = posts.getOrNull(index) ?: return
        clipboard.setText(AnnotatedString(content))
        copyMessage = if (index == 0) "Main post copied." else "Reply $index copied."
        nextCopyIndex = (index + 1).coerceAtMost(posts.size)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Your Threads chain")
        if (packs.size > 1) {
            ChoiceRow(
                options = packs.indices.toList(),
                selected = selectedVariant,
                label = { "Alternative ${it + 1}" },
                onSelected = onVariantSelected
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Posting assistant", fontWeight = FontWeight.Bold)
                Text("Copy the main post, open Threads, paste it, then return here and copy each reply in order.")
                val nextLabel = when {
                    nextCopyIndex == 0 -> "Copy main post"
                    nextCopyIndex < posts.size -> "Copy Reply $nextCopyIndex"
                    else -> "Copy main post again"
                }
                Button(
                    onClick = {
                        val index = if (nextCopyIndex >= posts.size) 0 else nextCopyIndex
                        copyPost(index)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(nextLabel)
                }
                OutlinedButton(
                    onClick = { openThreads(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Threads")
                }
                if (copyMessage.isNotBlank()) {
                    Text(
                        copyMessage,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(pack.asPlainText()))
                    copyMessage = "Complete thread copied for reference."
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Copy complete thread") }
            if (packs.size > 1) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(packs.asThreadsVariantText()))
                        copyMessage = "All alternatives copied."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Copy alternatives") }
            }
        }

        Text(pack.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        posts.forEachIndexed { index, content ->
            val label = if (index == 0) "Post utama" else "Reply $index"
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontWeight = FontWeight.Bold)
                        Text(
                            "${content.length} characters",
                            color = if (content.length <= 450) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { copyPost(index) }) { Text("Copy") }
                        TextButton(
                            onClick = { onRegenerate(index) },
                            enabled = regeneratingPostIndex == null
                        ) {
                            if (regeneratingPostIndex == index) {
                                CircularProgressIndicator(Modifier.width(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Regenerating")
                            } else {
                                Text("Regenerate")
                            }
                        }
                    }
                    Text(content)
                    if (content.length > 450) {
                        Text(
                            "Longer than SmartAgent's 450-character readability target. Regenerate or shorten before posting.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Creator checklist", fontWeight = FontWeight.Bold)
                Text(pack.checklist)
            }
        }
    }
}

private fun openThreads(context: Context) {
    val appIntent = context.packageManager.getLaunchIntentForPackage("com.instagram.barcelona")
    if (appIntent != null) {
        context.startActivity(appIntent)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.threads.com/")))
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
    onVerifiedChanged: (Boolean) -> Unit,
    savedProductMessage: String,
    isSavedProduct: Boolean,
    onSaveProduct: () -> Unit
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
        OutlinedButton(
            onClick = onSaveProduct,
            enabled = productName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSavedProduct) "Update saved product" else "Save product for later")
        }
        if (savedProductMessage.isNotBlank()) {
            Text(
                savedProductMessage,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
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
private fun ProductsScreen(
    products: List<SavedProduct>,
    onUse: (SavedProduct) -> Unit,
    onDelete: (SavedProduct) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var productToDelete by remember { mutableStateOf<SavedProduct?>(null) }
    val filteredProducts = remember(products, query) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            products
        } else {
            products.filter { product ->
                listOf(product.name, product.seller, product.link, product.features)
                    .any { value -> value.contains(cleanQuery, ignoreCase = true) }
            }
        }
    }

    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete saved product?") },
            text = { Text(product.name) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(product)
                    productToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionTitle("Saved products")
            Text(
                "Save verified products once, then reuse them without extracting the same page again.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (products.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search products") },
                    placeholder = { Text("Name, seller, link, or feature") },
                    singleLine = true
                )
            }
        }

        if (products.isEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(20.dp)) {
                        Text("No saved products yet", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("Extract or enter a product in Create, then tap Save product for later.")
                    }
                }
            }
        } else if (filteredProducts.isEmpty()) {
            item {
                Card {
                    Text("No products match your search.", Modifier.padding(20.dp))
                }
            }
        } else {
            items(filteredProducts, key = { it.id }) { product ->
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(product.name, fontWeight = FontWeight.Bold)
                        val summary = listOf(product.price, product.seller)
                            .filter { it.isNotBlank() }
                            .joinToString("  |  ")
                        if (summary.isNotBlank()) Text(summary)
                        if (product.promotion.isNotBlank()) {
                            Text(
                                product.promotion,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            "Updated ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(product.updatedAt))}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onUse(product) }) { Text("Use product") }
                            TextButton(onClick = { productToDelete = product }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    records: List<GenerationRecord>,
    onToggleFavourite: (GenerationRecord) -> Unit,
    onDelete: (GenerationRecord) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedRecord by remember { mutableStateOf<GenerationRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<GenerationRecord?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    val filters = remember(records) {
        listOf("All", "Favourites") + records.map { it.platform }.distinct().sorted()
    }
    LaunchedEffect(filters) {
        if (selectedFilter !in filters) selectedFilter = "All"
    }
    val filteredRecords = remember(records, query, selectedFilter) {
        records.filter { record ->
            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Favourites" -> record.isFavourite
                else -> record.platform == selectedFilter
            }
            val cleanQuery = query.trim()
            val matchesQuery = cleanQuery.isBlank() || listOf(
                record.title,
                record.platform,
                record.result
            ).any { it.contains(cleanQuery, ignoreCase = true) }
            matchesFilter && matchesQuery
        }
    }

    fun shareRecord(record: GenerationRecord) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, record.result)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share saved content"))
    }

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

    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete this saved content?") },
            text = { Text(record.title) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(record)
                    if (selectedRecord?.id == record.id) selectedRecord = null
                    recordToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear the Content Library?") },
            text = { Text("This permanently removes all saved generations, including favourites, from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    selectedRecord = null
                    showClearConfirmation = false
                }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") }
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
                SectionTitle("Content Library")
                if (records.isNotEmpty()) {
                    TextButton(onClick = { showClearConfirmation = true }) { Text("Clear all") }
                }
            }
        }

        if (records.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search saved content") },
                    placeholder = { Text("Title, platform, hook, or product") },
                    singleLine = true
                )
            }
            item {
                ChoiceRow(
                    options = filters,
                    selected = selectedFilter.takeIf { it in filters } ?: "All",
                    label = { it },
                    onSelected = { selectedFilter = it }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${filteredRecords.size} of ${records.size} saved generations",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (records.isEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(20.dp)) {
                        Text("No content yet", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("Your latest 100 content packs and Threads chains will be stored privately on this phone.")
                    }
                }
            }
        } else if (filteredRecords.isEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(20.dp)) {
                        Text("No matching content", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("Try another search or choose a different filter.")
                    }
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                record.title,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (record.isFavourite) "★" else "☆",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { selectedRecord = record }) { Text("Open") }
                            TextButton(onClick = { onToggleFavourite(record) }) {
                                Text(if (record.isFavourite) "Unfavourite" else "Favourite")
                            }
                            TextButton(onClick = {
                                clipboard.setText(AnnotatedString(record.result))
                            }) { Text("Copy") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { shareRecord(record) }) { Text("Share") }
                            TextButton(onClick = { recordToDelete = record }) { Text("Delete") }
                        }
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
    val existingBrandVoice = remember { repository.loadBrandVoice() }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf(repository.loadModel()) }
    var message by rememberSaveable { mutableStateOf("") }
    var brandName by rememberSaveable { mutableStateOf(existingBrandVoice.name) }
    var brandTone by rememberSaveable { mutableStateOf(existingBrandVoice.tone) }
    var brandCallToAction by rememberSaveable { mutableStateOf(existingBrandVoice.preferredCallToAction) }
    var brandPhrasesToUse by rememberSaveable { mutableStateOf(existingBrandVoice.phrasesToUse) }
    var brandPhrasesToAvoid by rememberSaveable { mutableStateOf(existingBrandVoice.phrasesToAvoid) }
    var brandMessage by rememberSaveable { mutableStateOf("") }

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
        SectionTitle("Personal brand voice")
        Text("SmartAgent will reuse this profile in new content packs when you enable it on the Create screen.")
        OutlinedTextField(
            value = brandName,
            onValueChange = { brandName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Profile name") },
            placeholder = { Text("Example: My affiliate voice") },
            singleLine = true
        )
        OutlinedTextField(
            value = brandTone,
            onValueChange = { brandTone = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tone and personality") },
            placeholder = { Text("Example: Friendly Malaysian UGC, direct and natural") },
            minLines = 2
        )
        OutlinedTextField(
            value = brandCallToAction,
            onValueChange = { brandCallToAction = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Preferred call to action") },
            placeholder = { Text("Example: Tekan bakul kuning untuk tengok harga") },
            minLines = 2
        )
        OutlinedTextField(
            value = brandPhrasesToUse,
            onValueChange = { brandPhrasesToUse = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phrases to use, optional") },
            minLines = 2
        )
        OutlinedTextField(
            value = brandPhrasesToAvoid,
            onValueChange = { brandPhrasesToAvoid = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phrases to avoid, optional") },
            placeholder = { Text("Example: confirm viral, wajib beli") },
            minLines = 2
        )
        Button(
            onClick = {
                repository.saveBrandVoice(
                    BrandVoiceProfile(
                        name = brandName,
                        tone = brandTone,
                        preferredCallToAction = brandCallToAction,
                        phrasesToUse = brandPhrasesToUse,
                        phrasesToAvoid = brandPhrasesToAvoid
                    )
                )
                brandMessage = "Brand voice saved"
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = listOf(
                brandName,
                brandTone,
                brandCallToAction,
                brandPhrasesToUse,
                brandPhrasesToAvoid
            ).any { it.isNotBlank() }
        ) {
            Text("Save brand voice")
        }
        if (existingBrandVoice.isConfigured() || brandMessage.isNotBlank()) {
            OutlinedButton(
                onClick = {
                    repository.clearBrandVoice()
                    brandName = ""
                    brandTone = ""
                    brandCallToAction = ""
                    brandPhrasesToUse = ""
                    brandPhrasesToAvoid = ""
                    brandMessage = "Brand voice removed"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remove brand voice")
            }
        }
        if (brandMessage.isNotBlank()) {
            Text(brandMessage, color = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider()
        Text("How to get a free key", fontWeight = FontWeight.Bold)
        Text(
            "Open Google AI Studio in your browser, create a Gemini API key, then paste it above. SmartAgent sends product information only when you tap Generate."
        )
        Text(
            "SmartAgent 1.0.0  |  Personal build",
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
