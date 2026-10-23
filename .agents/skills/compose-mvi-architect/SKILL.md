---
name: compose-mvi-architect
description: >-
  Use this skill when creating, editing, or reviewing Jetpack Compose screens, ViewModels,
  MVI architecture, StateFlow, UI components, or Glassmorphism design in Android/Kotlin.
  Trigger on: "شاشة", "واجهة", "زر", "تصميم", "كومبوز", "لايوت", "مكون", "ViewModel",
  "UiState", "StateFlow", "MVI", "ديزاين", "واجهة المستخدم", "اضيف شاشة", "عدل الشاشة",
  "اضف زر", "اربط الـ ViewModel", "شاشة جديدة", "Composable", "SafeActionButton",
  "Glassmorphism", "Dark theme", "الزجاجي", "التصميم الداكن",
  "screen", "UI", "Compose", "Jetpack Compose", "ViewModel", "StateFlow", "UiState",
  "LaunchedEffect", "collectAsState", "remember", "mutableStateOf", "Scaffold",
  "TopAppBar", "BottomBar", "NavGraph", "navigation", "recomposition", "side effect",
  "layout", "modifier", "padding", "arrangement", "alignment", "Card", "Column", "Row".
---

# Compose MVI & Glassmorphism Architect (Surgical-Grade)

## Mandatory Protocol

```
Step 1 — Locate:   Read the target screen/component file (StartLine/EndLine <= 50 lines)
Step 2 — Diagnose: Identify the architectural violation or missing piece in ONE sentence
Step 3 — Diff:     [file:line] ❌ violation → ✅ fix  (1-2 lines max)
Step 4 — Verify:   Check: zero logic in @Composable, SafeActionButton used, file < 400 lines
Step 5 — Sync:     Update PROJECT_STATE.md if change was made
```

## Hard Rules
- Zero DB / network calls inside `@Composable` — delegate to ViewModel via StateFlow.
- `SafeActionButton` with debounce on ALL clickable elements — no raw `Button` or `clickable`.
- Phone inputs default to Yemen `+967` LTR via `PhoneInputWithCountrySelector`.
- Files > 400 lines must be split — extract dialogs/cards to `components/` sub-package.
- Theme: Dark Glassmorphism — zero white (`0xFFFFFFFF`) backgrounds — use `DarkSurface = 0xFF0F1A22`.
