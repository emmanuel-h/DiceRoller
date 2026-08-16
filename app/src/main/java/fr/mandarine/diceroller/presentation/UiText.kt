// app/src/main/java/fr/mandarine/diceroller/presentation/UiText.kt
package fr.mandarine.diceroller.presentation

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources

/**
 * A piece of user-facing text named rather than spelled out: a resource id plus the arguments to
 * format it with, resolved against a locale only at the point it is displayed.
 *
 * Exists because three of this package's string builders — [rollButtonLabel], [relativeTimeLabel]
 * and [validateCustomFaces] — are pure functions covered by plain JVM unit tests, and issue #68
 * would otherwise have forced them either into `@Composable` (moving ~300 lines of fast tests onto
 * a device) or into taking a [Resources] (which the JVM source set has no way to build). Returning
 * a [UiText] keeps them pure *and* locale-independent: a test asserts which string was chosen and
 * with what arguments, which is the decision those functions actually make, and cannot be broken
 * by a translator rewording the English.
 *
 * Composables that merely *display* a fixed string do not need this — they call `stringResource`
 * directly. This is only for text a non-composable decides on.
 */
sealed interface UiText {

    /** A `<string>`, formatted with [args] in declaration order (`%1$s`, `%2$d`, …). */
    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /**
     * A `<plurals>` selected by [count], formatted with [args].
     *
     * [args] defaults to `[count]` because that is the overwhelmingly common shape — "3 rolls" —
     * but is separate from the selector so a phrase can be quantified by one number and mention
     * another, e.g. "value 4, rolled 2 times".
     */
    data class Plural(
        @param:PluralsRes val id: Int,
        val count: Int,
        val args: List<Any> = listOf(count),
    ) : UiText
}

/** Resolves this text against [resources], i.e. against whichever locale they were opened for. */
fun UiText.resolve(resources: Resources): String = when (this) {
    is UiText.Res -> resources.getString(id, *args.toTypedArray())
    is UiText.Plural -> resources.getQuantityString(id, count, *args.toTypedArray())
}

/** Resolves this text against the composition's own resources. */
@Composable
fun UiText.resolve(): String = resolve(LocalResources.current)
