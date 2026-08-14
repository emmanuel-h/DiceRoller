// app/src/main/java/fr/mandarine/diceroller/presentation/RollHistoryStore.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.RollRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * How many of the most recent rolls the log keeps; older records fall off the end.
 *
 * With no way to clear the log from the UI, this cap is the *only* thing that ever removes a
 * record, so it is also the app's storage bound.
 */
const val MAX_HISTORY_RECORDS: Int = 50

/**
 * Persists the user's recent rolls across app launches.
 *
 * Append-only by design: there is no `clear`, because the UI offers no way to erase the log.
 *
 * Declared as an interface for the same reason as [DiceColorStore]: it lets
 * [DiceRollerViewModel] be unit-tested against [InMemoryRollHistoryStore] with no DataStore and
 * no Android runtime, using constructor injection rather than a mocking framework.
 *
 * Implementations own the [MAX_HISTORY_RECORDS] cap, so no caller has to remember to trim.
 */
interface RollHistoryStore {

    /** Emits the stored records, newest first, at most [MAX_HISTORY_RECORDS] of them. */
    val history: Flow<List<RollRecord>>

    /** Prepends [record] to the log, dropping the oldest record once the cap is exceeded. */
    suspend fun record(record: RollRecord)
}

/**
 * Non-persistent [RollHistoryStore] used by previews and unit tests.
 *
 * @param initial the records the store starts with, newest first; trimmed to the cap
 */
class InMemoryRollHistoryStore(
    initial: List<RollRecord> = emptyList(),
) : RollHistoryStore {

    private val state = MutableStateFlow(initial.take(MAX_HISTORY_RECORDS))

    override val history: Flow<List<RollRecord>> = state.asStateFlow()

    override suspend fun record(record: RollRecord) {
        state.update { records -> (listOf(record) + records).take(MAX_HISTORY_RECORDS) }
    }
}
