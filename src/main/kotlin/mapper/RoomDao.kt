package mapper

import com.datastax.oss.driver.api.mapper.annotations.*

@Entity
@CqlName("room")
data class Room(@PartitionKey val roomid: Int?,
                val name: String?,
                val capacity: Int?)

@Dao
interface RoomDao {
    @Insert
    fun create(room: Room)

    @Select
    fun get(roomid: Int?): Room?

    @Update
    fun update(room: Room)
}