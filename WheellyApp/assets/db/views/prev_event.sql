CREATE VIEW prev_event AS
	SELECT h._id heartbeat_id, prev.*
	FROM heartbeats h
	INNER JOIN heartbeats prev
		ON h.odometer = prev.odometer
		AND h.place_id = prev.place_id
		AND h._created > prev._created
		AND SUBSTR(h._created, 1, 10) = SUBSTR(prev._created, 1, 10)
		AND h._id != prev._id
		AND h.type != 4