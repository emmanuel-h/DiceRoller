// app/src/test/java/fr/mandarine/diceroller/MainDispatcherRule.kt
package fr.mandarine.diceroller

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps `Dispatchers.Main` for a [TestDispatcher] around each test.
 *
 * `viewModelScope` dispatches on `Dispatchers.Main`, which has no
 * implementation on a plain JVM test runtime — without this rule any
 * ViewModel that launches a coroutine fails to construct.
 *
 * The default [UnconfinedTestDispatcher] runs launched work eagerly, so the
 * color restored in `DiceRollerViewModel`'s init block is already visible by
 * the time the constructor returns.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
