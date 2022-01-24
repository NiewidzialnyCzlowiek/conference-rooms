// User credentials, keyed by email address so we can authenticate
CREATE TABLE IF NOT EXISTS reservation_entry
(
    room_id int,
    date date,
    hour int,
    quant int,
    user_id uuid,
    PRIMARY KEY ((room_id, date), hour, quant)
) WITH CLUSTERING ORDER BY (hour ASC, quant ASC);


CREATE TABLE IF NOT EXISTS reservation_log
(
    room_id int,
    date date,
    timestamp timestamp,
    user_id uuid,
    operation text,
    start_hour int,
    start_quant int,
    end_hour int,
    end_quant int,
    PRIMARY KEY ((room_id, date), timestamp, user_id)
) WITH CLUSTERING ORDER BY (timestamp ASC, user_id ASC);


CREATE TABLE IF NOT EXISTS room
(
    room_id int,
    name text,
    capacity int,
    PRIMARY KEY ( room_id )
);