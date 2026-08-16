// app/src/main/java/fr/mandarine/diceroller/presentation/AppLanguageStore.kt
package fr.mandarine.diceroller.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the language the user chose to read the app in, across launches.
 *
 * The app stores this itself rather than handing it to the platform, and that is the whole reason
 * changing language does not blink: the alternative — `AppCompatDelegate.setApplicationLocales` or
 * the framework's `LocaleManager` — applies a locale by *relaunching the activity*, which costs a
 * measured ~148ms of black screen. Keeping the choice here means the switch is a recomposition.
 * See `docs/features/language-and-settings.md` for what that trades away.
 *
 * Declared as an interface for the same reason as [DiceColorStore]: so [DiceRollerViewModel] can
 * be unit-tested against [InMemoryAppLanguageStore] rather than a real DataStore.
 */
interface AppLanguageStore {

    /** Emits the stored language, falling back to [AppLanguage.System] when none is set. */
    val language: Flow<AppLanguage>

    /** Stores [language] as the user's choice. */
    suspend fun setLanguage(language: AppLanguage)
}

/**
 * Non-persistent [AppLanguageStore] used by previews and unit tests.
 *
 * @param initial the language the store starts with
 */
class InMemoryAppLanguageStore(
    initial: AppLanguage = AppLanguage.System,
) : AppLanguageStore {

    private val state = MutableStateFlow(initial)

    override val language: Flow<AppLanguage> = state.asStateFlow()

    override suspend fun setLanguage(language: AppLanguage) {
        state.value = language
    }
}
