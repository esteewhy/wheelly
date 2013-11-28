package com.wheelly.sync;

import java.net.URI;
import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.AlreadySyncingException;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.ClientsDataDelegate;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.activities.AccountActivity;
import org.mozilla.gecko.sync.setup.auth.AccountAuthenticator;
import org.mozilla.gecko.sync.setup.auth.AuthenticationResult;
import org.mozilla.gecko.sync.stage.GlobalSyncStage.Stage;
import android.os.Bundle;
import android.app.Activity;
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
				new WheellyAccountAuthenticator(new AccountActivity() {
					@Override
					public void authCallback(AuthenticationResult result) {
						runSync();
					}
				}).authenticate("http://wheelly.com:5000/", "esteewhy", "n0p@ssw0rd");;
			}
		});
	}

	private void runSync() {
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRAS_KEY_STAGES_TO_SYNC, "{ \"history\" : true }");
		// TODO: default serverURL.
		try {
		final KeyBundle keyBundle = new KeyBundle("esteewhy", "aaaaa-bbbbb-ccccc-ddddd-ee");
		
			final GlobalSession globalSession = new WheellyGlobalSession(
					SyncConfiguration.DEFAULT_USER_API,
					"http://wheelly.com:5000/",
					"esteewhy",
					"n0p@ssw0rd",
					"wheelly",
					keyBundle,
					new GlobalSessionCallback() {
						@Override public boolean wantNodeAssignment() { return false; }
						@Override public boolean shouldBackOff() { return false; }
						@Override public void requestBackoff(long backoff) { }
						@Override public void informUpgradeRequiredResponse(GlobalSession session) { }
						@Override public void informUnauthorizedResponse(GlobalSession globalSession, URI oldClusterURL) { }
						@Override public void informNodeAuthenticationFailed(GlobalSession globalSession, URI failedClusterURL) { }
						@Override public void informNodeAssigned(GlobalSession globalSession, URI oldClusterURL, URI newClusterURL) { }
						@Override public void handleSuccess(GlobalSession globalSession) { globalSession.config.persistToPrefs(); }
						@Override public void handleStageCompleted(Stage currentState, GlobalSession globalSession) { }
						@Override public void handleError(GlobalSession globalSession, Exception ex) { }
						@Override public void handleAborted(GlobalSession globalSession, String reason) { }
					},
					MainActivity.this,
					extras,
					new ClientsDataDelegate() {
						@Override public void setClientsCount(int clientsCount) { }
						@Override public boolean isLocalGUID(String guid) { return false; }
						@Override public int getClientsCount() { return 1; }
						@Override public String getClientName() { return "Wheelly"; }
						@Override public String getAccountGUID() { return null; }
					}
				);
			AccountAuthenticator.runOnThread(new Runnable() {
				@Override
				public void run() {
					try {
						globalSession.start();
					} catch (AlreadySyncingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}