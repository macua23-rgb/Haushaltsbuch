package de.haushaltsbuch.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromDate(d: LocalDate?): Long? = d?.toEpochDay()
    @TypeConverter fun toDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
}
