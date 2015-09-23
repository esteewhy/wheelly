CREATE VIEW next_event AS
	SELECT h._id heartbeat_id, next.*
	FROM heartbeats h
	INNER JOIN heartbeats next
		ON h.odometer = next.odometer
		AND h.place_id = next.place_id
		AND h._created < next._created
		AND SUBSTR(h._created, 1, 10) = SUBSTR(next._created, 1, 10)
		AND h._id != next._id
		AND h.type != 4