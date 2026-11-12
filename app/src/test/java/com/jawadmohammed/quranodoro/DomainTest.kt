package com.jawadmohammed.quranodoro

import com.jawadmohammed.quranodoro.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DomainTest {
    @Test fun growthMatchesWebAnchors() { assertEquals(10,growthPoints(25));assertEquals(20,growthPoints(50));assertEquals(35,growthPoints(90));assertEquals(0,growthPoints(0)) }
    @Test fun allVarietiesCrossEveryStageAtThreshold() { treeTypes.values.forEach {m-> thresholds.forEachIndexed {i,gp->val threshold=kotlin.math.round(gp*m).toInt();assertEquals(i,stage(threshold,m));if(i>0)assertEquals(i-1,stage(threshold-1,m))} } }
    @Test fun growthProgressIsBounded() { assertEquals(1f,growthProgress(99999,1.5));assertEquals(0f,growthProgress(0,1.5));assertEquals(.5f,growthProgress(10,1.0)) }
    @Test fun elapsedClockIgnoresWallTimeEdits() { val t=TimerState(running=true,endElapsed=10000,endWall=100000,boot=4);assertEquals(5000,remaining(t,9999999,5000,4)) }
    @Test fun rebootFallsBackToWallClock() { val t=TimerState(running=true,endElapsed=10000,endWall=100000,boot=4);assertEquals(2000,remaining(t,98000,500,5));assertEquals(0,remaining(t,101000,500,5)) }
    @Test fun pauseFreezesRemainingTime() { assertEquals(3000,remaining(TimerState(remainingMs=3000),99999999,999999,4)) }
    @Test fun catalogueHasAllRecitersAndSurahs() { assertEquals(12,reciters.size);assertEquals((1..114).toList(),surahs.map{it.number});reciters.forEach {assertTrue(it.url(1).endsWith("/001.mp3"));assertTrue(it.url(114).endsWith("/114.mp3"))} }
    @Test(expected=IllegalArgumentException::class) fun rejectInvalidSurah() {reciters.first().url(115)}
    @Test fun everyWebReminderIsIncluded() {assertEquals(15,reminders.size)}
    @Test fun streakIncludesYesterdayButNotGaps() {
        val today=LocalDate.of(2026,11,11)
        fun session(d:LocalDate)=FocusSession(d.toString(),d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),25,"Classic",null)
        assertEquals(2,streak(listOf(session(today.minusDays(1)),session(today.minusDays(2))),today))
        assertEquals(0,streak(listOf(session(today.minusDays(2))),today))
    }
}
