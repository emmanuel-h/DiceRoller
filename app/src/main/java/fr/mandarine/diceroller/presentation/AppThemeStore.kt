// app/src/main/java/fr/mandarine/diceroller/presentation/AppThemeStore.kt
package fr.mandarine.diceroller.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the light or dark theme the user chose, across launches.
 *
 * Declared as an interface for the same reason as [DiceColorStore]: so [DiceRollerViewModel] can
 * be unit-tested against [InMemoryAppThemeStore] rather than a real DataStore.
 */
interface AppThemeStore {

    /** Emits the stored theme, falling back to the device's ([AppTheme.forDevice]). */
    val theme: Flow<AppTheme>

    /** Stores [theme] as the user's choice. */
    suspend fun setTheme(theme: AppTheme)
}

/**
 * Non-persistent [AppThemeStore] used by previews and unit tests.
 *
 * @param initial the theme the store starts with
 */
class InMemoryAppThemeStore(
    initial: AppTheme = AppTheme.Light,
) : AppThemeStore {

    private val state = MutableStateFlow(initial)

    override val theme: Flow<AppTheme> = state.asStateFlow()

    override suspend fun setTheme(theme: AppTheme) {
        state.value = theme
    }
}
