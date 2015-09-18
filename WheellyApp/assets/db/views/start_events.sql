CREATE VIEW start_events AS
	SELECT h._id heartbeat_id, s.*
	FROM heartbeats h, heartbeats s
	WHERE h.odometer > s.odometer
		AND s.type = 1
		AND h.type = 2
		GROUP BY h._id
		HAVING s.odometer = MAX(s.odometer)