# SmartAgent

SmartAgent is a private Android content assistant for Malaysian creators. It converts a product, screenshot, or content idea into a publish-ready content pack for TikTok, Facebook, Threads, or WhatsApp.

## Current personal build 0.6.0

- Affiliate and organic content modes
- Product link input through paste or Android Share
- Layered product extraction using public page metadata, TikTok oEmbed, Gemini URL Context, and an on-phone browser fallback
- Structured product verification for name, price, seller, promotion, description, and features
- Extraction source, confidence guidance, warnings, and an explicit user-check confirmation
- Automatic screenshot-to-product extraction with Gemini image understanding
- Product JSON-LD reading before generic page metadata
- Shopee and TikTok short-link redirect resolution
- Product screenshot input with Gemini image understanding
- Bahasa Melayu Malaysia and British English output
- TikTok, Shopee Video, Instagram Reels, Facebook, Threads, and WhatsApp formats
- 10, 15, 30, 60, and 120 second durations
- UGC, soft sell, storytelling, honest review, faceless, and problem solution styles
- Three hooks, a timed storyboard, full voice-over, caption, hashtags, pinned comment, and creator checklist
- Encrypted Gemini API key using Android Keystore
- Local history for the latest 50 generations
- Copy and Android Share actions
- Light and dark themes
- One or three complete creative alternatives per generation
- Structured result cards for title, hooks, storyboard, voice-over, caption, hashtags, pinned comment, and checklist
- Copy and regenerate controls for every individual content section
- Duration-aware voice-over word targets
- TikTok, Shopee Video, Instagram Reels, Facebook, Threads, and WhatsApp output
- Thirteen content styles including UGC, demo, testimonial, comparison, POV, and myth buster
- Saved personal brand voice with tone, preferred call to action, phrases to use, and phrases to avoid
- Per-result voice-over word counting with duration-fit guidance

## Privacy model

The app has no SmartAgent account, analytics, advertisement SDK, remote database, or payment system. The Gemini key is encrypted on the device and excluded from Android backups. Product information is sent to the selected Gemini model only after the user taps Extract product, Use this page, or Generate.

## Extract a TikTok or Shopee product

1. Paste or share the product link into SmartAgent. Shared text containing a link is supported.
2. Tap **Extract product**.
3. SmartAgent first tries public metadata without opening a browser.
4. If the page blocks that request, SmartAgent opens a private in-app browser.
5. Wait for the page to load. On TikTok, open the product card or yellow basket if it is shown.
6. Tap **Use this page** to fill the structured product fields.
7. Check the product name, price, seller, promotion, description, and features.
8. Select the confirmation box only after checking the displayed claims.

If link access is blocked, choose **Screenshot**. SmartAgent reads the screenshot immediately and fills the same editable verification fields.

## Build the APK with GitHub

1. Upload this project to a GitHub repository.
2. Open the repository's Actions tab.
3. Select **Build SmartAgent APK**.
4. Tap **Run workflow**.
5. When the build finishes, open it and download **SmartAgent-debug-apk** from Artifacts.
6. Extract the ZIP file and install `app-debug.apk` on the Android phone.

Android may ask for permission to install applications from the browser or file manager used to open the APK.

## Set up AI generation

1. Create a Gemini API key in [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Open SmartAgent and select **Settings**.
3. Paste the key and tap **Save settings**.
4. Return to **Create** and generate a content pack.

The default model is `gemini-3.5-flash-lite`. SmartAgent automatically migrates the earlier `gemini-2.5-flash-lite` setting.

## Development configuration

- Android Gradle Plugin 8.13.0
- Gradle 8.13
- Kotlin 2.2.10
- Jetpack Compose BOM 2025.08.00
- Minimum Android version: Android 8.0, API 26
- Target Android version: API 36

The GitHub workflow builds the debug APK automatically. No Gemini API key is stored in the repository or required during the build.

## Planned next improvements

- Optional content calendar
- Search and favourites in history
- Multiple named brand voice profiles
- Release signing and automated device tests
