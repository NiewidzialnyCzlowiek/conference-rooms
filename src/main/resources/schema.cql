CREATE KEYSPACE IF NOT EXISTS conference_rooms WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
USE conference_rooms;
CREATE TABLE IF NOT EXISTS reservation_entry
(
    room_id int,
    date date,
    quant int,
    user_id uuid,
    PRIMARY KEY ((room_id, date), quant)
) WITH CLUSTERING ORDER BY (quant ASC);


CREATE TABLE IF NOT EXISTS reservation_log
(
    date date,
    timestamp timestamp,
    user_id uuid,
    room_id int,
    operation text,
    start_quant int,
    end_quant int,
    PRIMARY KEY (date, timestamp, user_id)
) WITH CLUSTERING ORDER BY (timestamp ASC, user_id ASC);


CREATE TABLE IF NOT EXISTS reservation_correction
(
    date date,
    timestamp timestamp,
    user_id uuid,
    operation text,
    PRIMARY KEY (date, timestamp, user_id)
) WITH CLUSTERING ORDER BY (timestamp ASC, user_id ASC);


CREATE TABLE IF NOT EXISTS room
(
    room_id int,
    name text,
    capacity int,
    PRIMARY KEY ( room_id )
);