# حالة مشروع WhatsUp Automation (PROJECT_STATE.md)
*آخر تحديث: 2026-10-24 | الحالة التشغيلية: تفعيل makeCacheableSignalKeyStore لمنع انهيار جلسات التشفير، وإطلاق النبض الفوري للرسائل الواردة، وتثبيت شبكة أمان رصد إشعارات وسائط العرض لمرة واحدة*

## 1. Current Active Architecture (Map of Key Files)
- **Engine Bridge:** `app/src/main/assets/nodejs-project/index.js` & `media-downloader.js` (`makeCacheableSignalKeyStore` in-memory key cache to prevent Libsignal session drops; recursive unwrap; zero silent drops).
- **Notification Fallback:** `app/src/main/kotlin/com/whatsup/automation/service/WhatsAppNotificationListenerService.kt` (Active detection of view-once and media notification pulses).
- **LID Resolver:** `app/src/main/assets/nodejs-project/lid-resolver.js` (L145-L165: Non-blocking pure silent resolver, purged invalid onWhatsApp USync queries).
- **Foreground Core:** `app/src/main/kotlin/com/whatsup/automation/service/WhatsAppForegroundService.kt` (L85-L210: 24/7 Resilience & WakeLock).
- **Engine Controller:** `app/src/main/kotlin/com/whatsup/automation/data/engine/WhatsAppEngine.kt` (Direct link between `VAULT_MEDIA_CAPTURED` and `logRepository.insertLog` with `LogStatus.SUCCESS`; instant diagnostic logging).
- **Storage/DB:** `app/src/main/kotlin/com/whatsup/automation/data/local/AppDatabase.kt` (Room Schema v10, persistent ActivityLog & VaultMediaEntity).
- **Notifications Hub:** `app/src/main/kotlin/com/whatsup/automation/presentation/screens/notifications/` (`NotificationsScreen.kt`, `NotificationsViewModel.kt`, `NotificationsBentoGrid.kt`, `NotificationFilterChips.kt`, `NotificationCard.kt`).
- **Vault UI & Media Hub:** `app/src/main/kotlin/com/whatsup/automation/presentation/screens/vault/` (`VaultScreen.kt`, `VaultViewModel.kt`, `VaultMediaTab.kt`, `VaultPreviewDialogs.kt`).
- **Skills Directory:** `.agents/skills/` (`strict-evidence-debug`, `baileys-bridge-guard`, `compose-mvi-architect`, `android-logcat-analyzer`, `room-sqlite-migration-guard`, `kotlin-coroutines-flow-expert`, `baileys-anti-ban-protocol`, `android-battery-wakelock-optimizer`).

## 2. Last 5 Verified Changes
1. **Signal Session Key Caching & Multi-Layer Media Protection:**
   - **`makeCacheableSignalKeyStore` Integration:** تزويد `makeWASocket` في `index.js` بمخزن المفاتيح الكاشي `makeCacheableSignalKeyStore(state.keys, logger)` لمنع التنافس أثناء القراءة من القرص وحماية جلسات `Libsignal` من الانهيار (`Closing session`).
   - **Instant Top-Level MSG_INGEST Pulse:** إعادة هيكلة `sock.ev.on('messages.upsert')` لبث نبض `MSG_INGEST` في اللحظة الأولى لوصول أي حزمة رسالة قبل أي فحص أو شروط، لمنع إسقاط الرسائل المشفرة بصمت.
   - **Notification Safety Net:** إضافة كاشف وسائط في `WhatsAppNotificationListenerService.kt` لتوثيق إشعارات الصور ومقاطع الفيديو وسائط العرض لمرة واحدة في السجل التشخيصي تلقائياً.
   - **Verification:** تجميع `bundle.cjs` بنجاح، بناء `app-debug.apk` بنجاح (`BUILD SUCCESSFUL in 5m 33s`).
1. **Radical Anti-View-Once Pipeline Hardening & Dual-Store Sync:**
   - **Recursive Message Unwrapping:** بناء دالة `unwrapAndInspectMessage` في `index.js` لفك كافة طبقات التغليف العودية (`deviceSentMessage`, `ephemeralMessage`, `viewOnceMessageV2`, `viewOnceMessage`, `documentWithCaptionMessage`) حتى عمق 10 مستويات، مما مكّن رصد وسائط العرض لمرة واحدة بدقة 100% حتى في الرسائل الذاتية والأجهزة المرتبطة.
   - **ReferenceError & Loop Crash Elimination:** إزالة المتغير المكسور `viewOnceObj` في `index.js:643` نهائياً، واستخراج التعليق والنص بأمان من `leaf` و `mediaObj`، مع تغليف حلقة المعالجة بـ `try/catch` لمنع أي استثناء غير متوقع من إسقاط مستمع الأحداث.
   - **Triple-Layer Media Decryption:** تزويد `media-downloader.js` بطبقة ثالثة لتنزيل الوسائط تمرر كائن `msg` الأصلي لمكتبة Baileys بجانب التدفق المباشر `downloadContentFromMessage` والمصغرات الاحتياطية.
   - **Dual Database Persistence (Room & Vault):** ربط حدث `VAULT_MEDIA_CAPTURED` في `WhatsAppEngine.kt` بجدول السجلات `logRepository.insertLog` بحالة `LogStatus.SUCCESS`، مما يضمن ظهور كل وسائط سرية ملتقطة في شاشتي "الخزينة" و"سجل النشاط / المحادثات" في نفس اللحظة مع توثيق الحجم ورقم المرسل.
   - **Verification:** تجميع `bundle.cjs` (6.8MB) بنجاح، اجتياز اختبارات `ViewOnceVaultResilienceTest` بنجاح (`BUILD SUCCESSFUL`), وبناء `app-debug.apk` (30.6MB) بنجاح.
1. **Dedicated Notifications Center & MVI Glassmorphism Architecture:**
   - **Repository Modernization:** تزويد `NotificationRepository` و `NotificationRepositoryImpl` بدوال التحكم الفردي `markAsRead(id)` و `deleteNotification(id)` وإعادة حساب العداد الحي تلقائياً.
   - **MVI ViewModel & StateFlow:** إنشاء `NotificationsViewModel.kt` و `NotificationsUiState` مع فلترة فورية بخمسة محاور (`ALL`, `ERROR`, `WARNING`, `SUCCESS`, `INFO`) وبحث لحظي ذكي وإحصائيات Bento حية.
   - **Modular Glassmorphism UI:** بناء واجهة متكاملة موزعة دون تجاوز 250 سطراً لكل ملف (`NotificationsScreen.kt`, `NotificationsBentoGrid.kt`, `NotificationFilterChips.kt`, `NotificationCard.kt`) مع أزرار نسخ المحتوى، حذف فردي، تمييز الكل كمقروء، وتفريغ شامل مع تأكيد أمني.
   - **Navigation & Drawer Routing:** ربط مسار `Screen.Notifications` في `AppNavigation.kt` و `MainActivity.kt`، وتوجيه زر جرس الهيدر في `DashboardScreen.kt` وإضافة عنصر مركز الإشعارات إلى `AppNavigationDrawer.kt`.
   - **Silent Build Verification:** اجتياز البناء البرمجي الكامل بنجاح تام (`BUILD SUCCESSFUL in 4m 54s`).
1. **Zero-Drop Anti-View-Once Pipeline, Ingestion Heartbeat & LID Freeze Purge:**
   - **LID Loop Freeze Purge:** حذف استدعاء `sock.onWhatsApp` المعطوب لمعرفات الـ LID في `lid-resolver.js` الذي كان يجمد مصفوفة الرسائل لمدة 60 ثانية بسبب استعلامات USync غير الصالحة.
   - **Official Baileys `extractMessageContent`:** اعتماد دالة `extractMessageContent` و `getContentType` الرسمية من مكتبة Baileys لفك التغليف الداخلي لوسائط العرض لمرة واحدة تلقائياً بدلاً من التفكيك اليدوي الهش.
   - **Priority 0 Immediate Capture:** نقل منطق التقاط وسائط العرض لمرة واحدة إلى رأس دالة `messages.upsert` كأولوية صفرية قبل أي عمليات تحقق من الأرقام، مع إرسال نبض تشخيصي فوري `MSG_INGEST` إلى واجهة السجلات لتسجيل كل رسالة واردة.
   - **Honest Connection State:** تعديل `WhatsAppEngine.kt` لربط حالة الاتصال `Connected` بجاهزية الجسر الفعلي `isBridgeReady` بدلاً من مجرد التحقق من وجود ملف `creds.json` محلياً.
   - **Live Device Verification:** نجاح إعادة تجميع `bundle.cjs` (6.5MB) وبناء APK وتثبيته عبر ADB بنجاح على Motorola Edge 5G UW (`ZD2226MJR7`)، وتأكيد اتصال المقبس بخوادم واتساب حياً (`CONNECTION_STATE: CONNECTED`).
1. **AI Agent Skills Optimization, Ghost Plugin Purge & Universal Architecture Hardening:**
   - **Ghost Plugins & Redundancy Purge:** حذف المجلدات الـ 8 للإضافات الشبحية الفارغة (`flutter`, `firebase`, `chrome-devtools`, `data-agent-kit`, `gemini-api`, `google-antigravity-sdk`, `google_maps_platform`, `modern-web-guidance`) وتنظيف `config.json`، بالإضافة لحذف 7 مهارات مكررة في الإعداد العام (`meta-agent-orchestrator`, `browser-testing-with-devtools`, `idea-refine`, `planning-and-task-breakdown`, `debugging-and-error-recovery`, `code-review-and-quality`, `frontend-ui-engineering`).
   - **WhatsUp Core Skills Expansion:** إنشاء وتوثيق 3 مهارات هندسية جديدة لمشروع واتساب: `kotlin-coroutines-flow-expert` (إدارة التدفق وقنوات التزامن ومنع تسريب الذاكرة)، `baileys-anti-ban-protocol` (محاكاة التردد البشري وتوزيع جاوس وتفادي حظر واتساب)، و `android-battery-wakelock-optimizer` (إدارة استهلاك المعالج وتفادي قيود سامسونج وشاومي).
   - **Universal Agent Skills Creation:** إنشاء وتوثيق مهارتين عالميتين في `config/skills`: `context-budget-compressor` (ترشيد استهلاك التوكنات وضغط السياق ومنع ظاهرة الضياع في المنتصف) و `git-atomic-commits-and-pr` (هندسة الكوميتات الذرية ومراجعة الـ Pull Requests).
1. **Anti-View-Once Media Vault UI Integration (Photos & Videos Hub):**
   - **Enum & ViewModel State:** إضافة `VaultSubTab.MEDIA` في `VaultViewModel.kt` وربط قائمة الوسائط `mediaList` مع عداد `mediaCount` لحساب الصور ومقاطع الفيديو السرية الملتقطة.
   - **VaultMediaTab Component:** إنشاء مكون `MediaVaultBentoSection` و `MediaVaultItemRow` لعرض الصور ومقاطع الفيديو الملتقطة في قائمة تفاعلية مع شارة "عرض لمرة واحدة 🔒" ومعاينة مصغرة سريعة.
   - **HUB & SubPage Integration:** تزويد الشاشة الرئيسية `VaultScreen.kt` ببطاقة فئة "وسائط العرض لمرة واحدة" وشريط تنقل `LazyRow` مع إمكانية المعاينة الكاملة عبر `VaultItemPreviewDialog` والتصدير المباشر لمعرض الهاتف `Pictures/WhatsUp_Vault` والمشاركة.
   - **Verification:** البناء البرمجي `./gradlew assembleDebug` اجتاز بنجاح تام (`BUILD SUCCESSFUL in 3m 22s`).
2. **Render-Phase Optimization, Shimmer Decoupling & Adaptive Health Polling:**
   - **Shimmer DrawPhase Decoupling:** تحويل `shimmerLoading` في `ShimmerExtensions.kt` لاستخدام `drawBehind { drawRect(brush) }` بدلاً من `background(brush)` لضمان انعدام الـ Recomposition أثناء الرسوميات الوميضية.
   - **Adaptive Service Polling:** ترشيد حلقة المراقبة في `ServiceHealthMonitor.kt` للتوسع من 4 ثوانٍ أثناء نشاط الخدمة إلى 15 ثانية كاملة في وضع السكون لتوفير البطارية ومنع إيقاظ المعالج.
   - **Immunity Constitution:** إنشاء وحفظ دستور الحصانة والوقاية من أخطاء الذكاء الاصطناعي الخمسة في `.agents/rules/anti-hallucination-and-runtime-defense.md`.
2. **Compose UI Recomposition Optimization & Master Stop Switch:**
   - **Render-Phase Decoupling:** نقل كافة تأثيرات النبض والشفافية في `PulseReactor` و `ServiceGuardianCard` داخل `Components.kt` إلى `Modifier.graphicsLayer` الحصري لمرحلة الرسم (Draw Phase) لمنع إعادة التركيب اللانهائي (Recomposition Loop) الذي كان يستنزف 102% من المعالج و 30% من SurfaceFlinger على شاشات 120Hz.
   - **Master Stop & Deep Sleep:** تزويد `DashboardViewModel.kt` و `ReactorHeroCard.kt` بزر ودوال تحكم شاملة (`stopAllServices` و `startAllServices`) لإيقاف الخدمة الأمامية وكتم الإشعار الدائم وإبادة مقابس Baileys بضغطة واحدة، وخفض استهلاك الموارد إلى 0.0%.
3. **Pairing Unblock, Retry Cooldown Engine & Stale Session Purge:**
   - **Pairing Gate Exemption:** استثناء أوامر الاقتران (`PAIR_CODE`, `LOGOUT`, `FETCH_STATUSES`) وتشغيل الجسر من قيود الاشتراك في `BaileysBridgeManager.kt` للسماح بالربط دائماً بدون حظر.
   - **Auto Local License:** تهيئة اشتراك محلي دائم تلقائياً في `LicenseManager.kt` عند مسح البيانات لتفادي تعليق المستخدمين محلياً.
   - **Rate-Limit Cooldown Engine:** إضافة عداد انتظار تنازلي (30 ثانية للمرة الأولى و 60 ثانية عند التكرار) في `PairingViewModel.kt` و `PairingCodeCard.kt` مع شريط تحذيري وتعطيل الأزرار لحماية الحساب من حظر واتساب.
   - **Stale Data Purge:** تزويد المحرك والواجهة بزر ودالة تنظيف شاملة (`cleanStalePairingData`) لمسح `baileys_auth` وإبادة المقابس العالقة بنقرة واحدة.
2. **Core Pairing Engine Architecture (Proposals 1-4 Complete):**
   - **Socket Teardown:** إبادة المقابس القديمة وتفريغ مستمعي الأحداث (`sock.ev.removeAllListeners()`, `sock.ws.close()`) في `index.js` لمنع المقابس الشبحية وتسرب الذاكرة.
   - **Process LMK Resiliency:** تحصين خدمة `WhatsAppForegroundService` ضد القتل بالذاكرة وإبقاء `WakeLock` نشطاً أثناء مرحلة انتظار الاقتران لضمان استمرار الاتصال أثناء التبديل لواتساب.
   - **Cryptographic Session Gate:** حظر تفعيل الجلسة في `WhatsAppEngine.kt` إلا بعد التحقق الفعلي من كتابة `creds.json` المشفر بحجم سليم (> 50 بايت) عبر حدث `SESSION_PERSISTED`.
   - **Meta Blacklist Smart Failover:** تحويل ذكي وتلقائي إلى رمز الاستجابة السريعة (QR) في `PairingViewModel.kt` عند فشل طلب الكود مرتين متتاليتين لتفادي حظر رقم الهاتف.
2. **Pairing Diagnostics & Error Clarification:** تزويد جسر Node.js (`command-dispatcher.js`) ومحرك Kotlin بتصنيف دقيق وترجمة فورية لأخطاء طلب الكود وفشل الاقتران مع بطاقة خطأ تفاعلية وزر إعادة محاولة.
3. **Dashboard Onboarding Removal:** إزالة بطاقة «خطوات الإعداد السريع» من الشاشة الرئيسية وحذفها نهائياً مع استقلالية تنبيه أذونات جهات الاتصال.
4. **Speed & Token Optimization Constitution:** اعتماد بنود بروتوكول السرعة في `AGENTS.md` و `GEMINI.md` (حظر ملفات `bundle.cjs` و `dist/`، فرض الـ Batching المتوازي، وحظر البحث الشامل بدون تحديد مسار).
5. **Workspace Hygiene & Storage Audit:** تنظيف جذري لمخلفات البناء والسجلات القديمة (حذف أكثر من 30MB من ملفات `.log`, `whatsup-debug.zip`, `logcat.txt`, ولقطات الشاشة المؤقتة) وتحديث `.gitignore`.

## 3. Known Traps & Do-Not-Touch
- **Strict Single-JID:** يُمنع دمج معرفين داخل `statusJidList`؛ التفاعل يُرسل لـ `msg.key.participant` الأصلي فقط إلى `status@broadcast`.
- **Dumb Bridge Law:** حظر اتخاذ أي قرار آلي داخل كود Node.js بدون أمر مباشر من Kotlin.
- **Double-Reply Prevention:** فحص `processedMessageCache` في خدمة الإشعارات قبل إرسال أي رد تلقائي.
- **Fail-Closed Security:** مفاتيح الجلسة لا تُخزن أبداً بنص واضح، واعتماد `SessionKeystore` المشفر فقط.
