package no.oslo.torshov.pfb.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import no.oslo.torshov.pfb.data.model.DayNote

@Dao
interface DayNoteDao {

    @Query("SELECT * FROM day_notes WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DayNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: DayNote)
}
