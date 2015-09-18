package com.wheelly.content;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class TransactionRepository {
	
	public final static String FINANCISTO_ACTION = "ru.orangesoftware.financisto.NEW_TRANSACTION";
	
	private final Context context;
	
	public TransactionRepository(Context context) {
		this.context = context;
	}
	
	/**
	 * Checks if Financisto is available.
	 */
	public boolean checkAvailability() {
		return
			context
				.getPackageManager()
				.queryIntentActivities(
					new Intent(TransactionRepository.FINANCISTO_ACTION),
					PackageManager.MATCH_DEFAULT_ONLY
				).size() > 0;
	}
}