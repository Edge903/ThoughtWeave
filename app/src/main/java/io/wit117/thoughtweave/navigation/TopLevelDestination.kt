package io.wit117.thoughtweave.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.wit117.thoughtweave.R
import kotlin.reflect.KClass

/** The three bottom-navigation destinations, in display order. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val selectedIconRes: Int,
) {
    MEMOS(
        route = MemoListRoute,
        routeClass = MemoListRoute::class,
        labelRes = R.string.nav_memos,
        iconRes = R.drawable.ic_description,
        selectedIconRes = R.drawable.ic_description_fill1,
    ),
    AI_MERGE(
        route = SynthesisRoute,
        routeClass = SynthesisRoute::class,
        labelRes = R.string.nav_ai_merge,
        iconRes = R.drawable.ic_auto_awesome,
        selectedIconRes = R.drawable.ic_auto_awesome_fill1,
    ),
    SCHEDULES(
        route = ScheduleListRoute,
        routeClass = ScheduleListRoute::class,
        labelRes = R.string.nav_schedules,
        iconRes = R.drawable.ic_event,
        selectedIconRes = R.drawable.ic_event_fill1,
    ),
}
