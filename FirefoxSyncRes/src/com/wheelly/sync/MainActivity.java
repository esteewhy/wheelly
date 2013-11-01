package com.wheelly.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.EngineSettings;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.HTTPFailureException;
import org.mozilla.gecko.sync.InfoCollections;
import org.mozilla.gecko.sync.JSONRecordFetcher;
import org.mozilla.gecko.sync.MetaGlobal;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.PersistedMetaGlobal;
import org.mozilla.gecko.sync.PrefsSource;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SynchronizerConfiguration;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.crypto.PersistedCrypto5Keys;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.delegates.JSONRecordFetchDelegate;
import org.mozilla.gecko.sync.delegates.KeyUploadDelegate;
import org.mozilla.gecko.sync.delegates.MetaGlobalDelegate;
import org.mozilla.gecko.sync.middleware.Crypto5MiddlewareRepository;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.Resource;
import org.mozilla.gecko.sync.net.SyncStorageRecordRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequest;
import org.mozilla.gecko.sync.net.SyncStorageRequestDelegate;
import org.mozilla.gecko.sync.net.SyncStorageResponse;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.Server11Repository;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.auth.AccountAuthenticator;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import org.mozilla.gecko.sync.synchronizer.ServerLocalSynchronizer;
import org.mozilla.gecko.sync.synchronizer.Synchronizer;
import org.mozilla.gecko.sync.synchronizer.SynchronizerDelegate;
import org.mozilla.gecko.sync.synchronizer.SynchronizerSession;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.message.BasicHeader;
import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findViewById(R.id.auth_btn).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					config = new SyncConfiguration("wheelly",
							new PrefsSource() {
								@Override
								public SharedPreferences getPrefs(String name,
										int mode) {
									return getSharedPreferences(name, mode);
								}
							});

					config.username = "esteewhy";
					config.password = "n0p@ssw0rd";
					config.serverURL = new URI("http://wheelly.com:5000/");
					config.clusterURL = config.serverURL;
					config.syncKeyBundle = new KeyBundle(config.username, "aaaaa-bbbbb-ccccc-ddddd-ee");
					config.stagesToSync = Arrays.asList(new String[] { "events" });

					ensureUser();
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CryptoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	SyncConfiguration config;

	abstract class OptimisticResourceDelegate extends BaseResourceDelegate {
		public OptimisticResourceDelegate(Resource resource) {
			super(resource);
		}

		@Override
		public void handleHttpProtocolException(ClientProtocolException e) {
		}

		@Override
		public void handleHttpIOException(IOException e) {
		}

		@Override
		public void handleTransportException(GeneralSecurityException e) {
		}
	}

	private void ensureUser() throws URISyntaxException {
		String userRequestUrl = config.serverURL + Constants.AUTH_NODE_PATHNAME
				+ Constants.AUTH_NODE_VERSION + config.username;
		final BaseResource httpResource = new BaseResource(userRequestUrl);
		httpResource.delegate = new OptimisticResourceDelegate(httpResource) {

			@Override
			public void handleHttpResponse(HttpResponse response) {
				int statusCode = response.getStatusLine().getStatusCode();
				switch (statusCode) {
				case 200:
					try {
						InputStream content = response.getEntity().getContent();
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(content, "UTF-8"), 1024);
						String inUse = reader.readLine();
						BaseResource.consumeReader(reader);
						reader.close();

						if (inUse.equals("1")) { // Username exists.
							ensureClusterURL();
						} else { // User does not exist.
							regUser();
						}
					} catch (Exception e) {
					}
					break;
				default: // No other response is acceptable.
				}
				BaseResource.consumeEntity(response.getEntity());
			}
		};

		AccountAuthenticator.runOnThread(new Runnable() {
			@Override
			public void run() {
				httpResource.get();
			}
		});
	}

	private void regUser() throws URISyntaxException {
		String userRequestUrl = config.serverURL + Constants.AUTH_NODE_PATHNAME
				+ Constants.AUTH_NODE_VERSION + config.username;
		final BaseResource httpResource = new BaseResource(userRequestUrl);
		httpResource.delegate = new OptimisticResourceDelegate(httpResource) {

			@Override
			public void addHeaders(HttpRequestBase request,
					DefaultHttpClient client) {
				super.addHeaders(request, client);
				request.setHeader(new BasicHeader("X-Weave-Secret", "wheelly"));
			}

			@Override
			public void handleHttpResponse(HttpResponse response) {
				int statusCode = response.getStatusLine().getStatusCode();
				switch (statusCode) {
				case 400:
				case 200:
					try {
						InputStream content = response.getEntity().getContent();
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(content, "UTF-8"), 1024);
						String username = reader.readLine();
						BaseResource.consumeReader(reader);
						reader.close();
						if (200 == statusCode) {
							ensureClusterURL();
						}
					} catch (Exception e) {
					}
					break;
				default: // No other response is acceptable.
				}
				BaseResource.consumeEntity(response.getEntity());
			}
		};
		final JSONObject jAccount = new JSONObject();
		jAccount.put("email", "esteewhy@hotmail.com");
		jAccount.put(Constants.JSON_KEY_PASSWORD, config.password);
		jAccount.put("captcha-challenge", "");
		jAccount.put("captcha-response", "");

		// Make request.
		AccountAuthenticator.runOnThread(new Runnable() {
			@Override
			public void run() {
				try {
					httpResource.put(jAccount);
				} catch (UnsupportedEncodingException e) {
				}
			}
		});
	}

	public final GlobalSessionCallback callback = new GlobalSessionCallback() {
		@Override
		public boolean wantNodeAssignment() {
			return config.getPrefs().getBoolean(
					SyncConfiguration.PREF_CLUSTER_URL_IS_STALE, false);
		}

		@Override
		public boolean shouldBackOff() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void requestBackoff(long backoff) {
			// TODO Auto-generated method stub

		}

		@Override
		public void informUpgradeRequiredResponse(GlobalSession session) {
			// TODO Auto-generated method stub

		}

		@Override
		public void informUnauthorizedResponse(GlobalSession globalSession,
				URI oldClusterURL) {
			// TODO Auto-generated method stub

		}

		@Override
		public void informNodeAuthenticationFailed(GlobalSession globalSession,
				URI failedClusterURL) {
			setClusterURLIsStale(false);
		}

		@Override
		public void informNodeAssigned(GlobalSession globalSession,
				URI oldClusterURL, URI newClusterURL) {
			setClusterURLIsStale(false);
		}

		@Override
		public void handleSuccess(GlobalSession globalSession) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleStageCompleted(Stage currentState,
				GlobalSession globalSession) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleError(GlobalSession globalSession, Exception ex) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleAborted(GlobalSession globalSession, String reason) {
			// TODO Auto-generated method stub

		}

		public synchronized void setClusterURLIsStale(boolean clusterURLIsStale) {
			Editor edit = config.getPrefs().edit();
			edit.putBoolean(SyncConfiguration.PREF_CLUSTER_URL_IS_STALE,
					clusterURLIsStale);
			edit.commit();
		}
	};

	void ensureClusterURL() throws URISyntaxException {
		String nodeRequestUrl = config.nodeWeaveURL();
		final URI oldClusterURL = config.getClusterURL();

		if (!callback.wantNodeAssignment() && oldClusterURL != null) {
			fetchInfoCollection();
			return;
		}

		// Fetch node containing user.
		final BaseResource httpResource = new BaseResource(nodeRequestUrl);
		httpResource.delegate = new OptimisticResourceDelegate(httpResource) {
			@Override
			public void handleHttpResponse(HttpResponse response) {
				int statusCode = response.getStatusLine().getStatusCode();
				switch (statusCode) {
				case 200:
					String output = null;
					try {
						InputStream content = response.getEntity().getContent();
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(content, "UTF-8"), 1024);
						output = reader.readLine();
						BaseResource.consumeReader(reader);
						reader.close();
					} catch (IllegalStateException e) {
						BaseResource.consumeEntity(response);
					} catch (IOException e) {
						BaseResource.consumeEntity(response);
					}

					if (output == null || output.equals("null")) {
						return;
					}

					URI url;
					try {
						url = new URI(output);
					} catch (URISyntaxException e1) {
						return;
					}
					if (oldClusterURL != null && oldClusterURL.equals(url)) {
						// Our cluster URL is marked as stale and the fresh
						// cluster URL is the same -- this is the user's
						// problem.
						callback.informNodeAuthenticationFailed(null, url);
						return;
					}

					callback.informNodeAssigned(null, oldClusterURL, url); // No matter what, we're getting a new node/weave clusterURL.
					config.setClusterURL(url);

					try {
						fetchInfoCollection();
					} catch (URISyntaxException e) {
					}
					break;
				}
				BaseResource.consumeEntity(response.getEntity());
			}
		};
		// Make request on separate thread.
		AccountAuthenticator.runOnThread(new Runnable() {
			@Override
			public void run() {
				httpResource.get();
			}
		});
	}

	void fetchInfoCollection() throws URISyntaxException {
		final JSONRecordFetcher fetcher = new JSONRecordFetcher(config.infoCollectionsURL(), config.credentials());
		
		fetcher.fetch(new JSONRecordFetchDelegate() {
			@Override
			public void handleSuccess(ExtendedJSONObject body) {
				config.infoCollections = new InfoCollections(body);
				fetchMetaGlobal();
			}

			@Override
			public void handleFailure(SyncStorageResponse response) {
			}

			@Override
			public void handleError(Exception e) {
			}
		});
	}
	
	void freshStart() {
		final MetaGlobal mg = generateNewMetaGlobal();
		try {
			SyncStorageRequest request = new SyncStorageRequest(config
					.storageURL(false));

			request.delegate = new SyncStorageRequestDelegate() {
				@Override
				public String credentials() {
					return config.credentials();
				}

				@Override
				public String ifUnmodifiedSince() {
					return null;
				}

				@Override
				public void handleRequestSuccess(
						SyncStorageResponse response) {

					BaseResource.consumeEntity(response);

					config.purgeMetaGlobal();
					config.purgeCryptoKeys();
					config.persistToPrefs();

					// It would be good to set the X-If-Unmodified-Since
					// header to `timestamp`
					// for this PUT to ensure at least some level of
					// transactionality.
					// Unfortunately, the servers don't support it after
					// a wipe right now
					// (bug 693893), so we're going to defer this until
					// bug 692700.
					mg.upload(new MetaGlobalDelegate() {
						@Override
						public void handleSuccess(
								MetaGlobal uploadedGlobal,
								SyncStorageResponse uploadResponse) {
							// Generate new keys.
							CollectionKeys keys = null;
							try {
								keys = CollectionKeys
										.generateCollectionKeys();
							} catch (CryptoException e) {

							}
							if (keys == null) {

							}

							// Upload new keys.
							uploadKeys(keys, new KeyUploadDelegate() {
								@Override
								public void onKeysUploaded() {
									config.persistToPrefs();

								}

								@Override
								public void onKeyUploadFailed(
										Exception e) {

								}
							});
						}

						@Override
						public void handleMissing(MetaGlobal global,
								SyncStorageResponse response) {
							// Shouldn't happen on upload.
						}

						@Override
						public void handleFailure(
								SyncStorageResponse response) {
						}

						@Override
						public void handleError(Exception e) {
						}
					});
				}

				@Override
				public void handleRequestFailure(
						SyncStorageResponse response) {
				}

				@Override
				public void handleRequestError(Exception ex) {
				}
			};

			request.delete();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	int STORAGE_VERSION = 5;
	
	void processMetaGlobal(MetaGlobal global) {
		config.metaGlobal = global;
		Long storageVersion = global.getStorageVersion();
		if (storageVersion == null) {
			freshStart();
			return;
		}
		if (storageVersion < STORAGE_VERSION) {
			freshStart();
			return;
		}
		if (storageVersion > STORAGE_VERSION) {
			// requiresUpgrade();
			return;
		}
		String remoteSyncID = global.getSyncID();
		if (remoteSyncID == null) {
			freshStart();
			return;
		}
		String localSyncID = config.syncID;
		if (!remoteSyncID.equals(localSyncID)) {
			// resetAllStages();
			config.purgeCryptoKeys();
			config.syncID = remoteSyncID;
		}
		// Compare lastModified timestamps for remote/local engine selection times.
		if (config.userSelectedEnginesTimestamp < config.persistedMetaGlobal().lastModified()) {
			// Remote has later meta/global timestamp. Don't upload engine changes.
			config.userSelectedEngines = null;
		}
		// Persist enabled engine names.
		config.enabledEngineNames = global.getEnabledEngineNames();
		config.persistToPrefs();
		ensureCrypto5Keys();
	}
	
	void fetchMetaGlobal() {
		InfoCollections infoCollections = config.infoCollections;
		if (infoCollections == null) {
			return;
		}
		long lastModified = config.persistedMetaGlobal().lastModified();
		
		if (!infoCollections.updateNeeded("meta", lastModified)) {
			// Try to use our local collection keys for this session.
			MetaGlobal global = config.persistedMetaGlobal().metaGlobal(config.metaURL(), config.credentials());
			
			if (global != null) {
				processMetaGlobal(global);
				return;
			}
		}
		
		MetaGlobal global = new MetaGlobal(config.metaURL(), config.credentials());
		global.fetch(new MetaGlobalDelegate() {
			@Override
			public void handleSuccess(MetaGlobal global,
					SyncStorageResponse response) {
				PersistedMetaGlobal pmg = config.persistedMetaGlobal();
				pmg.persistMetaGlobal(global);
				// Take the timestamp from the response since it is later
				// than the timestamp from info/collections.
				pmg.persistLastModified(response.normalizedWeaveTimestamp());
				processMetaGlobal(global);
			}

			@Override
			public void handleMissing(MetaGlobal global,
					SyncStorageResponse response) {
				freshStart();
			}

			@Override
			public void handleFailure(SyncStorageResponse response) {
			}

			@Override
			public void handleError(Exception e) {
			}
		});
	}

	void uploadKeys(final CollectionKeys keys, final KeyUploadDelegate keyUploadDelegate) {
		SyncStorageRecordRequest request;

		try {
			request = new SyncStorageRecordRequest(this.config.keysURI());
		} catch (URISyntaxException e) {
			keyUploadDelegate.onKeyUploadFailed(e);
			return;
		}

		request.delegate = new SyncStorageRequestDelegate() {
			@Override
			public String ifUnmodifiedSince() {
				return null;
			}

			@Override
			public void handleRequestSuccess(SyncStorageResponse response) {
				BaseResource.consumeEntity(response); // We don't need the response at all.
				keyUploadDelegate.onKeysUploaded();
			}

			@Override
			public void handleRequestFailure(SyncStorageResponse response) {
				// self.interpretHTTPFailure(response.httpResponse());
				BaseResource.consumeEntity(response); // The exception thrown should not need the body of the response.
				keyUploadDelegate.onKeyUploadFailed(new HTTPFailureException(
						response));
			}

			@Override
			public void handleRequestError(Exception ex) {
				keyUploadDelegate.onKeyUploadFailed(ex);
			}

			@Override
			public String credentials() {
				return config.credentials();
			}
		};

		// Convert keys to an encrypted crypto record.
		CryptoRecord keysRecord;
		try {
			keysRecord = keys.asCryptoRecord();
			keysRecord.setKeyBundle(config.syncKeyBundle);
			keysRecord.encrypt();
		} catch (Exception e) {

			keyUploadDelegate.onKeyUploadFailed(e);
			return;
		}

		request.put(keysRecord);
	}

	public MetaGlobal generateNewMetaGlobal() {
		final String newSyncID = Utils.generateGuid();
		final String metaURL = config.metaURL();
		ExtendedJSONObject engines = new ExtendedJSONObject();
		for (String engineName : config.stagesToSync) {
			EngineSettings engineSettings = null;
			engineSettings = new EngineSettings(Utils.generateGuid(), 0);

			engines.put(engineName, engineSettings.toJSONObject());
		}

		MetaGlobal metaGlobal = new MetaGlobal(metaURL, config.credentials());
		metaGlobal.setSyncID(newSyncID);
		metaGlobal.setStorageVersion(5L);
		metaGlobal.setEngines(engines);

		return metaGlobal;
	}

	/**
	 * Return collections where either the individual key has changed, or if the
	 * new default key is not the same as the old default key, where the
	 * collection is using the default key.
	 */
	protected Set<String> collectionsToUpdate(CollectionKeys oldKeys,
			CollectionKeys newKeys) {
		// These keys have explicitly changed; they definitely need updating.
		Set<String> changedKeys = new HashSet<String>(CollectionKeys.differences(oldKeys, newKeys));

		boolean defaultKeyChanged = true; // Most pessimistic is to assume default key has changed.
		KeyBundle newDefaultKeyBundle = null;
		try {
			KeyBundle oldDefaultKeyBundle = oldKeys.defaultKeyBundle();
			newDefaultKeyBundle = newKeys.defaultKeyBundle();
			defaultKeyChanged = !oldDefaultKeyBundle
					.equals(newDefaultKeyBundle);
		} catch (NoCollectionKeysSetException e) {
		}

		if (newDefaultKeyBundle == null) {
			return changedKeys;
		}

		if (!defaultKeyChanged) {
			return changedKeys;
		}

		// New keys have a different default/sync key; check known collections
		// against the default key.
		for (Stage stage : Stage.getNamedStages()) {
			String name = stage.getRepositoryName();
			if (!newKeys.keyBundleForCollectionIsNotDefault(name)) {
				// Default key has changed, so this collection has changed.
				changedKeys.add(name);
			}
		}

		return changedKeys;
	}
	
	final String CRYPTO_COLLECTION = "crypto";
	
	void ensureCrypto5Keys() {
		final boolean retrying = false;
		
		InfoCollections infoCollections = config.infoCollections;
		if (infoCollections == null) {
			return;
		}
		
		PersistedCrypto5Keys pck = config.persistedCryptoKeys();
		long lastModified = pck.lastModified();
		if (retrying || !infoCollections.updateNeeded(CRYPTO_COLLECTION, lastModified)) {
			// Try to use our local collection keys for this session.
			CollectionKeys keys = pck.keys();
			if (keys != null) {
				config.setCollectionKeys(keys);
				sync();
				return;
			}
		}
		
		try {
			SyncStorageRecordRequest request = new SyncStorageRecordRequest(config.wboURI("crypto", "keys"));
			request.delegate = new SyncStorageRequestDelegate() {
				@Override
				public String ifUnmodifiedSince() {
					return null;
				}

				@Override
				public void handleRequestSuccess(SyncStorageResponse response) {
					// Take the timestamp from the response since it is later than the timestamp from info/collections.
					long responseTimestamp = response.normalizedWeaveTimestamp();
					CollectionKeys keys = new CollectionKeys();
					try {
						ExtendedJSONObject body = response.jsonObjectBody();
						keys.setKeyPairsFromWBO(CryptoRecord.fromJSONRecord(body), config.syncKeyBundle);
					} catch (Exception e) {
						return;
					}

					PersistedCrypto5Keys pck = config.persistedCryptoKeys();
					if (!pck.persistedKeysExist()) {
						// New keys, and no old keys! Persist keys and server timestamp.
						config.setCollectionKeys(keys);
						pck.persistKeys(keys);
						pck.persistLastModified(responseTimestamp);
						// session.advance();
						sync();
						return;
					}

					// New keys, but we had old keys. Check for differences.
					CollectionKeys oldKeys = pck.keys();
					Set<String> changedCollections = collectionsToUpdate(oldKeys, keys);
					if (!changedCollections.isEmpty()) {
						// New keys, different from old keys.
						config.setCollectionKeys(keys);
						pck.persistKeys(keys);
						pck.persistLastModified(responseTimestamp);
						// session.resetStagesByName(changedCollections);
						// session.abort(null, "crypto/keys changed on server.");
						return;
					}

					// New keys don't differ from old keys; persist timestamp and
					// move on.
					config.setCollectionKeys(oldKeys);
					pck.persistLastModified(response.normalizedWeaveTimestamp());
					sync();
				}

				@Override
				public void handleRequestFailure(SyncStorageResponse response) {
					if (retrying) {
						// Should happen very rarely -- this means we uploaded
						// our crypto/keys
						// successfully, but failed to re-download.
						return;
					}
					
					int statusCode = response.getStatusCode();
					if (statusCode == 404) {
						freshStart();
						return;
					}
				}

				@Override
				public void handleRequestError(Exception ex) {
				}

				@Override
				public String credentials() {
					return config.credentials();
				}
			};
			request.get();
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	void sync() {
		KeyBundle collectionKey;
		try {
			collectionKey = config.getCollectionKeys().keyBundleForCollection("events");
		} catch (NoCollectionKeysSetException e) {
			return;
		}
		
		Crypto5MiddlewareRepository cryptoRepo;
		try {
			cryptoRepo = new Crypto5MiddlewareRepository(new Server11Repository(config.getClusterURLString(),
			        config.username,
			        "events",
			        config), collectionKey);
		} catch (URISyntaxException e) {
			return;
		}
		cryptoRepo.recordFactory = new RecordFactory() {
		    @Override
		    public Record createRecord(Record record) {
		        EventRecord r = new EventRecord();
		        r.initFromEnvelope((CryptoRecord) record);
		        return r;
		    }
		};
		
		Repository remote = cryptoRepo;

		Synchronizer synchronizer = new ServerLocalSynchronizer();
		synchronizer.repositoryA = remote;
		synchronizer.repositoryB = new EventRepository();;
		try {
			synchronizer.load(new SynchronizerConfiguration(config.getBranch("events.")));
		} catch (NonObjectJSONException e) {
			return;
		} catch (IOException e) {
			return;
		} catch (ParseException e) {
			return;
		}
		synchronizer.synchronize(this, new SynchronizerDelegate() {
			
			@Override
			public void onSynchronized(Synchronizer synchronizer) {
				SynchronizerConfiguration newConfig = synchronizer.save();
			    if (newConfig != null) {
			    	newConfig.persist(config.getBranch("events."));
			    }

			    final SynchronizerSession synchronizerSession = synchronizer.getSynchronizerSession();
			    int inboundCount = synchronizerSession.getInboundCount();
			    int outboundCount = synchronizerSession.getOutboundCount();
			}
			
			@Override
			public void onSynchronizeFailed(Synchronizer synchronizer,
					Exception lastException, String reason) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	void uploadMetaGlobal() {
		
	}
}