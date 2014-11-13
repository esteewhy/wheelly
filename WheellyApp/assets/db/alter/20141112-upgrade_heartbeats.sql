INSERT INTO heartbeats_t
	SELECT
		h._id,
		h._created,
		h.odometer,
		h.fuel,
		h.place_id,
		h.sync_id,
		h.sync_etag,
		h.sync_state,
		h.sync_date,
		h.modified,
		(SELECT CASE
			WHEN m1._id IS NOT NULL THEN 1
			WHEN m2._id IS NOT NULL THEN 2
			WHEN r._id IS NOT NULL THEN 3
			ELSE 0 END),
		m2.name,
		m2.mileage,
		r.amount,
		r.unit_price,
		r.cost,
		0,
		m2.track_id,
		r.transaction_id
	FROM heartbeats h
	LEFT OUTER JOIN mileages m1 ON m1.start_heartbeat_id = h._id
	LEFT OUTER JOIN mileages m2 ON m2.stop_heartbeat_id = h._id
	LEFT OUTER JOIN refuels r ON r.heartbeat_id = h._id;

DROP TABLE heartbeats;

ALTER TABLE heartbeats_t RENAME TO heartbeats;