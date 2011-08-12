package com.wheelly.app;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
/**
 * http://stackoverflow.com/questions/3721358/preferenceactivity-save-value-as-integer
 */
public class EditNumericPreference  extends EditTextPreference {

	public EditNumericPreference(Context context) {
		super(context);
	}

	public EditNumericPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditNumericPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected String getPersistedString(String defaultReturnValue) {
		return String.valueOf(getPersistedInt(60));
	}

	@Override
	protected boolean persistString(String value) {
		return persistInt(Integer.valueOf(value));
	}
}