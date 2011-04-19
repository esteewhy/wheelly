package com.wheelly.content;

import android.content.ContentValues;
import android.os.Bundle;
import android.os.Parcel;

public final class Marshaller {

	public static ContentValues Convert(Bundle bundle) {
		Parcel parcel = Parcel.obtain();
		try {
			bundle.writeToParcel(parcel, 0);
			return ContentValues.CREATOR.createFromParcel(parcel);
		} finally {
			parcel.recycle();
		}
	}
	
	public static Bundle Convert(ContentValues values) {
		Parcel parcel = Parcel.obtain();
		try {
			values.writeToParcel(parcel, 0);
			return Bundle.CREATOR.createFromParcel(parcel);
		} finally {
			parcel.recycle();
		}
	}
}
