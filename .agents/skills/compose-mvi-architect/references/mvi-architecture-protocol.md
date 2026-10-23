# MVI Architecture Protocol — Full Reference

## Composable Purity Checklist
- [ ] Zero DB calls inside `@Composable` — use `ViewModel.uiState: StateFlow<UiState>`.
- [ ] Zero network calls inside `@Composable`.
- [ ] Zero `mutableStateOf` holding business data — only UI ephemeral state allowed.
- [ ] All side effects in `LaunchedEffect` or `ViewModel`.

## Component Standards
| Component | Rule |
|:---|:---|
| All buttons | `SafeActionButton` with debounce — no raw `Button` |
| Phone input | `PhoneInputWithCountrySelector` default `+967` LTR |
| Screen header | `AppScreenHeader` — unified across all tabs |
| Colors | No `0xFFFFFFFF` — use `DarkSurface = 0xFF0F1A22` |
| File size | Max 400 lines — split dialogs/cards to `components/` |

## File Extraction Rules
- Dialog > 30 lines → extract to `components/XxxDialog.kt`
- Card > 20 lines → extract to `components/XxxCard.kt`
- Screen > 400 lines → split by logical UI sections
