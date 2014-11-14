CREATE VIEW stop_events AS
	SELECT h._id heartbeat_id, s.*
	FROM heartbeats h, heartbeats s
	WHERE h.odometer < s.odometer
		AND s.type = 2
		AND h.type = 1
		GROUP BY h._id
		HAVING s.odometer = MIN(s.odometer)