package no.oslo.torshov.pfb.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_notes")
data class DayNote(
    @PrimaryKey val date: String,
    val text: String = ""
)
