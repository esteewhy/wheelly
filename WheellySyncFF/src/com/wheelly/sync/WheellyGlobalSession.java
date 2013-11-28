package com.wheelly.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.json.simple.parser.ParseException;
import org.mozilla.gecko.background.common.log.Logger;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.ClientsDataDelegate;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.stage.CheckPreconditionsStage;
import org.mozilla.gecko.sync.stage.CompletedStage;
import org.mozilla.gecko.sync.stage.EnsureClusterURLStage;
import org.mozilla.gecko.sync.stage.EnsureCrypto5KeysStage;
import org.mozilla.gecko.sync.stage.FetchInfoCollectionsStage;
import org.mozilla.gecko.sync.stage.FetchMetaGlobalStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage;
import org.mozilla.gecko.sync.stage.NoSuchStageException;
import org.mozilla.gecko.sync.stage.ServerSyncStage;
import org.mozilla.gecko.sync.stage.SyncClientsEngineStage;
import org.mozilla.gecko.sync.stage.UploadMetaGlobalStage;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;

import android.content.Context;
import android.os.Bundle;

public class WheellyGlobalSession extends GlobalSession {

	public WheellyGlobalSession(
			String userAPI,
			String serverURL,
			String username,
			String password,
			String prefsPath,
			KeyBundle syncKeyBundle,
			GlobalSessionCallback callback,
			Context context,
			Bundle extras,
			ClientsDataDelegate clientsDelegate)
			throws SyncConfigurationException, IllegalArgumentException,
			IOException, ParseException, NonObjectJSONException {
		super(userAPI, serverURL, username, password, prefsPath, syncKeyBundle,
				callback, context, extras, clientsDelegate);
		config.enabledEngineNames = new HashSet<String>(config.stagesToSync = Arrays.asList(new String[] { "events" }));
	}
	
	@Override
	protected void prepareStages() {
		HashMap<Stage, GlobalSyncStage> stages = new LinkedHashMap<Stage, GlobalSyncStage>();
		
		stages.put(Stage.checkPreconditions,	new CheckPreconditionsStage());
		stages.put(Stage.ensureClusterURL,		new EnsureClusterURLStage());
		stages.put(Stage.fetchInfoCollections,	new FetchInfoCollectionsStage());
		stages.put(Stage.fetchMetaGlobal,		new FetchMetaGlobalStage());
		stages.put(Stage.ensureKeysStage,		new EnsureCrypto5KeysStage());
		stages.put(Stage.syncClientsEngine,		new SyncClientsEngineStage());

		stages.put(Stage.syncHistory,			new EventSyncStage());

		stages.put(Stage.uploadMetaGlobal,		new UploadMetaGlobalStage());
		stages.put(Stage.completed,				new CompletedStage());

		this.stages = Collections.unmodifiableMap(stages);
	}
	
	protected Stage nextStageForThisSession(Stage current) {
		Iterator<Entry<Stage, GlobalSyncStage>> it = stages.entrySet().iterator();
		Stage first = null;
		while (it.hasNext()) {
			Stage next = it.next().getKey();
			if(null == first) {
				first = next;
			}
			
			if(next == current) {
				return it.hasNext() ? it.next().getKey() : first;
			}
		}
		return first;
	}
	
	private static final String LOG_TAG = "WheellyGlobalSession";
	
	@Override
	public void advance() {
		// If we have a backoff, request a backoff and don't advance to next
		// stage.
		long existingBackoff = largestBackoffObserved.get();
		if (existingBackoff > 0) {
			this.abort(null, "Aborting sync because of backoff of " + existingBackoff + " milliseconds.");
			return;
		}

		this.callback.handleStageCompleted(this.currentState, this);
		Stage next = nextStageForThisSession(this.currentState);
		GlobalSyncStage nextStage;
		try {
			nextStage = this.getSyncStageByName(next);
		} catch (NoSuchStageException e) {
			this.abort(e, "No such stage " + next);
			return;
		}
		this.currentState = next;
		Logger.info(LOG_TAG, "Running next stage " + next + " (" + nextStage + ")...");
		try {
			nextStage.execute(this);
		} catch (Exception ex) {
			Logger.warn(LOG_TAG, "Caught exception " + ex + " running stage " + next);
			this.abort(ex, "Uncaught exception in stage.");
			return;
		}
	}
	
	@Override
	public void wipeAllStages() {
		Logger.info(LOG_TAG, "Wiping all stages.");
		// Includes "clients".
		this.wipeStagesByEnum(Stage_getNamedStages());
	}
	
	@Override
	public void resetAllStages() {
		Logger.info(LOG_TAG, "Resetting all stages.");
		// Includes "clients".
		this.resetStagesByEnum(Stage_getNamedStages());
	}
	
	protected Collection<Stage> Stage_getNamedStages() {
		final List<Stage> named = new ArrayList<Stage>();
		for (Entry<Stage, GlobalSyncStage> s : stages.entrySet()) {
			if (s.getValue() instanceof ServerSyncStage) {
				named.add(s.getKey());
			}
		}
		return Collections.unmodifiableCollection(named);
	}
	@Override
	public GlobalSyncStage getSyncStageByName(String name) throws NoSuchStageException {
		return getSyncStageByName("events".equals(name) ? Stage.syncHistory : null);
	}
	
	@Override
	protected Set<String> enabledEngineNames() {
		if (config.enabledEngineNames != null) {
			return config.enabledEngineNames;
		}
		
		return new HashSet<String>() {{
			add("events");
		}};
	}
}