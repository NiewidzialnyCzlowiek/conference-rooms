package mapper

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.mapper.MapperBuilder
import com.datastax.oss.driver.api.mapper.annotations.DaoFactory
import com.datastax.oss.driver.api.mapper.annotations.Mapper

@Mapper
interface ConferenceRoomsMapper {
    @DaoFactory
    fun reservationDao(): ReservationDao

    @DaoFactory
    fun roomDao(): RoomDao

    companion object {
        fun builder(session: CqlSession): MapperBuilder<ConferenceRoomsMapper> {
            return ConferenceRoomsMapperBuilder(session)
        }
    }
}