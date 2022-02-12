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


CREATE TABLE IF NOT EXISTS reservation_by_month
(
    year int,
    month int,
    user_id uuid,
    date date,
    start_quant int,
    room_id int,
    end_quant int,
    PRIMARY KEY ( (year, month), user_id, date, start_quant, room_id)
) WITH CLUSTERING ORDER BY (user_id ASC, date ASC, start_quant ASC, room_id ASC);


CREATE TABLE IF NOT EXISTS reservation_log
(
    log_date date,
    timestamp timestamp,
    user_id uuid,
    room_id int,
    operation text,
    reservation_date date,
    start_quant int,
    end_quant int,
    PRIMARY KEY (log_date, timestamp, user_id)
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