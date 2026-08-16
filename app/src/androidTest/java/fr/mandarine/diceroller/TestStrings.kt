// app/src/androidTest/java/fr/mandarine/diceroller/TestStrings.kt
package fr.mandarine.diceroller

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry
import fr.mandarine.diceroller.domain.DieType

/**
 * The app's own strings, resolved the way the app resolves them.
 *
 * Every UI assertion that used to spell an English sentence out goes through here since issue #68.
 * That is not cosmetic: the same suite has to pass on a device set to French, where a literal
 * `"Add dice to roll"` matches nothing — and a translation that broke a screen would then show up
 * as a red test rather than as a bug report. What the assertions still pin is the *structure*: that
 * the button carries the pool's notation, that the header counts the entries, that a value and its
 * multiplier land in the same row.
 *
 * Resolved against the *target* context — the app under test — rather than the instrumentation
 * one, so these are the same resources the composables read.
 */
private val resources: Resources
    get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

/** The `<string>` [id], formatted with [args]. */
fun str(@StringRes id: Int, vararg args: Any): String = resources.getString(id, *args)

/** The `<plurals>` [id] for [count], formatted with [args] — which default to `[count]`. */
fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
    resources.getQuantityString(id, count, *(if (args.isEmpty()) arrayOf(count) else args))

/**
 * How [die] is named on screen: `"D6"`.
 *
 * Named `dieNotation` rather than `dieLabel` on purpose — the production composable of that name
 * lives in `presentation.component`, which is also the package half these tests sit in, and two
 * same-named functions there would resolve to whichever was in scope.
 */
fun dieNotation(die: DieType): String = str(R.string.die_label, die.faces)

/** The Roll button's label for a pool that notates as [notation]. */
fun rollLabel(notation: String): String = str(R.string.roll_button, notation)

/**
 * A result group's header, e.g. `"4×D6"`.
 *
 * [dieNotation] is passed as the plain notation it is: `D6` is the same in every locale, which is
 * exactly why `die_label` is `translatable="false"`.
 */
fun groupHeader(poolCount: Int, dieNotation: String): String =
    str(R.string.result_group_header, poolCount, dieNotation)
