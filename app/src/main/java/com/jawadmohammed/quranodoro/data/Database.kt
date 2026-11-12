package com.jawadmohammed.quranodoro.data

import androidx.room.*
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@Entity
data class FocusSession(
    @PrimaryKey val id: String,
    val completedAt: Long,
    val minutes: Int,
    val preset: String,
    val treeId: String?,
)

@Entity
data class FarmTree(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String,
    val gp: Int = 0,
    val plantedAt: Long = System.currentTimeMillis(),
    val maturedAt: Long? = null,
    val active: Boolean = false,
)

@Entity
data class TasbihState(
    @PrimaryKey val id: Int = 1,
    val count: Int = 0,
    val target: Int = 33,
    val phrase: String = "SubhanAllah",
    val awarded: Boolean = false,
)

@Entity
data class DhikrSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val completedAt: Long = System.currentTimeMillis(),
    val target: Int,
    val phrase: String,
)

@Entity
data class TimerState(
    @PrimaryKey val id: Int = 1,
    val token: String = UUID.randomUUID().toString(),
    val phase: String = "Focus",
    val running: Boolean = false,
    val remainingMs: Long = 1500000,
    val durationMs: Long = 1500000,
    val endWall: Long = 0,
    val endElapsed: Long = 0,
    val boot: Int = 0,
    val completed: Int = 0,
    val presetName: String = "Classic 25 / 5",
    val focus: Int = 25,
    val shortBreak: Int = 5,
    val longBreak: Int = 15,
    val cycles: Int = 4,
    val treeId: String? = null,
)

@Dao
interface QuranDao {
    @Query("SELECT * FROM FocusSession ORDER BY completedAt DESC")
    fun sessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM FarmTree ORDER BY plantedAt") fun trees(): Flow<List<FarmTree>>

    @Query("SELECT * FROM DhikrSession ORDER BY completedAt DESC")
    fun dhikr(): Flow<List<DhikrSession>>

    @Query("SELECT * FROM TasbihState WHERE id=1") fun tasbihFlow(): Flow<TasbihState?>

    @Query("SELECT * FROM TimerState WHERE id=1") fun timerFlow(): Flow<TimerState?>

    @Query("SELECT * FROM TimerState WHERE id=1") suspend fun timer(): TimerState?

    @Query("SELECT * FROM TasbihState WHERE id=1") suspend fun tasbih(): TasbihState?

    @Query("SELECT * FROM FarmTree WHERE id=:id") suspend fun tree(id: String): FarmTree?

    @Query("SELECT * FROM FarmTree WHERE active=1 LIMIT 1") suspend fun activeTree(): FarmTree?

    @Query("UPDATE FarmTree SET active=0") suspend fun clearActive()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(timer: TimerState)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(tasbih: TasbihState)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(tree: FarmTree)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun save(session: FocusSession): Long

    @Insert suspend fun save(session: DhikrSession)

    @Transaction
    suspend fun setTarget(target: Int) {
        val s = tasbih() ?: TasbihState()
        save(
            s.copy(
                target = target.coerceIn(1, 9999),
                awarded = if (target == s.target) s.awarded else false,
            ),
        )
    }

    @Transaction
    suspend fun setPhrase(phrase: String) {
        save(
            (tasbih() ?: TasbihState()).copy(
                phrase = phrase.trim().take(160).ifBlank { "SubhanAllah" },
            ),
        )
    }

    @Transaction
    suspend fun resetTasbih() {
        save((tasbih() ?: TasbihState()).copy(count = 0, awarded = false))
    }

    @Transaction
    suspend fun increment() {
        val s = tasbih() ?: TasbihState()
        val next = s.count + 1
        val reached = next == s.target && !s.awarded
        save(s.copy(count = next, awarded = s.awarded || reached))
        if (reached) save(DhikrSession(target = s.target, phrase = s.phrase))
    }

    @Transaction
    suspend fun selectTree(id: String) {
        val t = tree(id) ?: return
        if (t.maturedAt != null) return
        clearActive()
        save(t.copy(active = true))
    }

    @Transaction
    suspend fun plant(type: String) {
        save(FarmTree(type = type, active = activeTree() == null))
    }

    @Transaction
    suspend fun complete(token: String, wall: Long, elapsed: Long, boot: Int): Boolean {
        val t = timer() ?: return false
        if (t.token != token || !t.running || remaining(t, wall, elapsed, boot) > 0) return false
        val n = t.completed + if (t.phase == "Focus") 1 else 0
        if (t.phase == "Focus") {
            val tree = t.treeId?.let { tree(it) }
            val inserted = save(FocusSession(t.token, t.endWall, t.focus, t.presetName, t.treeId))
            if (inserted != -1L && tree != null && tree.maturedAt == null) {
                val gp = tree.gp + growthPoints(t.focus)
                val mature = stage(gp, treeTypes[tree.type] ?: 1.0) == 6
                save(
                    tree.copy(
                        gp = gp,
                        maturedAt = if (mature) wall else null,
                        active = tree.active && !mature,
                    ),
                )
            }
        }
        val phase =
            if (t.phase != "Focus") "Focus"
            else if (n % t.cycles == 0) "Long break" else "Short break"
        val duration =
            (when (phase) {
                "Focus" -> t.focus
                "Long break" -> t.longBreak
                else -> t.shortBreak
            }) * 60000L
        save(
            t.copy(
                token = UUID.randomUUID().toString(),
                phase = phase,
                running = false,
                remainingMs = duration,
                durationMs = duration,
                completed = n,
                treeId = null,
                endWall = 0,
                endElapsed = 0,
            ),
        )
        return true
    }
}

@Database(
    entities =
        [
            FocusSession::class,
            FarmTree::class,
            TasbihState::class,
            DhikrSession::class,
            TimerState::class,
        ],
    version = 1,
    exportSchema = true,
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun dao(): QuranDao
}
