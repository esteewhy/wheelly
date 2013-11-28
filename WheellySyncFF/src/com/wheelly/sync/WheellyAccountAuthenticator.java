package com.wheelly.sync;

import org.mozilla.gecko.sync.setup.activities.AccountActivity;
import org.mozilla.gecko.sync.setup.auth.AuthenticationResult;
import org.mozilla.gecko.sync.setup.auth.BetterAccountAuthenticator;

public class WheellyAccountAuthenticator extends BetterAccountAuthenticator {

	public WheellyAccountAuthenticator(AccountActivity activity) {
		super(activity);
	}
	
	@Override
	public void abort(AuthenticationResult result, Exception e) {
		if(result == AuthenticationResult.FAILURE_USERNAME) {
			try {
				new CreateUserStage().execute(this);
				return;
			} catch (Exception e1) {
				super.abort(result, e1);
				return;
			}
		}
		super.abort(result, e);
	}
}
