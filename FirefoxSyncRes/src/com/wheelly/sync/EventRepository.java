package com.wheelly.sync;

import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;

import android.content.Context;

public class EventRepository extends Repository {
	@Override
	public void createSession(RepositorySessionCreationDelegate delegate,
			Context context) {
		try {
			delegate.onSessionCreated(new EventRepositorySession(this, context));
		} catch(Exception ex) {
			delegate.onSessionCreateFailed(ex);
		}
	}
}