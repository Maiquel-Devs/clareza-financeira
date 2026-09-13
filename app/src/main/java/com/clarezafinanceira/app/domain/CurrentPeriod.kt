package com.clarezafinanceira.app.domain

import java.time.Clock
import java.time.Duration
import java.time.YearMonth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Calendar context only; never changes financial dates or form drafts. */
class CurrentPeriod(private val clock: Clock) {
    private val mutable = MutableStateFlow(YearMonth.now(clock))
    val period = mutable.asStateFlow()

    fun refresh() {
        mutable.value = YearMonth.now(clock)
    }

    /** Called by the foreground lifecycle; cancellation removes the pending wait. */
    suspend fun observeWhileActive() {
        while (true) {
            val now = clock.instant()
            val current = YearMonth.from(now.atZone(clock.zone))
            mutable.value = current
            val next = current.plusMonths(1).atDay(1).atStartOfDay(clock.zone).toInstant()
            delay(Duration.between(now, next).toMillis().coerceAtLeast(1))
        }
    }
}
