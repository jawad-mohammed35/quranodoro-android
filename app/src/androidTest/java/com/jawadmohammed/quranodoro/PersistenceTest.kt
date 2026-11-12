package com.jawadmohammed.quranodoro

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.jawadmohammed.quranodoro.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class) class PersistenceTest {
    private lateinit var db: QuranDatabase
    @Before fun setup() {db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,QuranDatabase::class.java).build()}
    @After fun close() {db.close()}
    @Test fun rapidDhikrTapsAreNotLostAndPresetKeepsCount()=runBlocking {
        val dao=db.dao();dao.save(TasbihState());repeat(5){dao.increment()};val old=dao.tasbih()!!;dao.save(old.copy(target=99));assertEquals(5,dao.tasbih()!!.count);assertEquals(99,dao.tasbih()!!.target)
        coroutineScope{repeat(94){launch(Dispatchers.IO){dao.increment()}}};assertEquals(99,dao.tasbih()!!.count);assertEquals(1,dao.dhikr().first().size)
    }
    @Test fun completionAndGrowthAreExactlyOnce()=runBlocking {
        val dao=db.dao();dao.plant("Barhi");val tree=dao.activeTree()!!;val t=TimerState(running=true,endElapsed=1000,endWall=10000,boot=1,treeId=tree.id);dao.save(t)
        assertTrue(dao.complete(t.token,10001,1001,1));assertFalse(dao.complete(t.token,10001,1001,1));assertEquals(10,dao.tree(tree.id)!!.gp);assertEquals(1,dao.sessions().first().size);assertEquals("Short break",dao.timer()!!.phase)
    }
    @Test fun staleWorkerCannotCompleteNewTimer()=runBlocking {val dao=db.dao();dao.save(TimerState(token="new",running=true,endElapsed=0));assertFalse(dao.complete("old",100,100,0));assertTrue(dao.sessions().first().isEmpty())}
    @Test fun extendingCompletedDhikrTargetAllowsNextMilestone()=runBlocking {
        val dao=db.dao();dao.save(TasbihState(target=3));repeat(3){dao.increment()};dao.setTarget(5);repeat(2){dao.increment()}
        assertEquals(5,dao.tasbih()!!.count);assertEquals(2,dao.dhikr().first().size)
    }
    @Test fun selectedTreeIsCapturedAtSessionStart()=runBlocking {val dao=db.dao();dao.plant("Ajwa");val first=dao.activeTree()!!;dao.plant("Barhi");val second=dao.trees().first().last();dao.save(TimerState(token="session",running=true,endElapsed=1,endWall=1,treeId=first.id));dao.selectTree(second.id);dao.complete("session",2,2,0);assertEquals(10,dao.tree(first.id)!!.gp);assertEquals(0,dao.tree(second.id)!!.gp)}
    @Test fun matureTreeStopsGrowing()=runBlocking {val dao=db.dao();val tree=FarmTree(type="Barhi",gp=300,active=true);dao.save(tree);val t=TimerState(running=true,endElapsed=1,endWall=1,treeId=tree.id);dao.save(t);dao.complete(t.token,2,2,0);assertNotNull(dao.tree(tree.id)!!.maturedAt);assertNull(dao.activeTree())}
}
