package com.wheelly.sync;

import org.mozilla.gecko.sync.setup.Constants;
import org.mozilla.gecko.sync.setup.SyncAccounts;
import org.mozilla.gecko.sync.setup.SyncAccounts.SyncAccountParameters;
import org.mozilla.gecko.sync.setup.activities.AccountActivity;
import org.mozilla.gecko.sync.setup.auth.AccountAuthenticator;
import org.mozilla.gecko.sync.setup.auth.AuthenticationResult;
import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.SyncResult;
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
				}).authenticate("http://sync.wheelly.com/", "esteewhy", "n0p@ssw0rd");;
			}
		});
	}

	private void runSync() {
		AccountAuthenticator.runOnThread(new Runnable() {
			@Override
			public void run() {
				Account[] accounts = SyncAccounts.syncAccounts(MainActivity.this); 
				
				if(accounts.length == 0) {
					SyncAccounts.createSyncAccount(new SyncAccountParameters(
							MainActivity.this,
							AccountManager.get(MainActivity.this),
							"esteewhy",
							"aaaaa-bbbbb-ccccc-ddddd-ee",
							"n0p@ssw0rd",
							"http://sync.wheelly.com/"));
				}
				
				accounts = SyncAccounts.syncAccounts(MainActivity.this);
				
				Account a = accounts[0];
				Bundle extras = new Bundle();
				extras.putString(Constants.EXTRAS_KEY_STAGES_TO_SYNC, "{ \"history\" : true }");

				new WheellySyncAdapter(MainActivity.this, true)
					.onPerformSync(
							a,
							extras,
							"com.wheelly",
							MainActivity.this.getContentResolver().acquireContentProviderClient("com.wheelly"),
							new SyncResult());
			}
		});
	}
}