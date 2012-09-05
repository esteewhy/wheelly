package com.wheelly.app;

import java.util.Map;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.SimpleCursorAdapter;

public abstract class ListConfiguration {
	public int EmptyTextResourceId;
	public int ConfirmDeleteResourceId;
	
	public Class<?> ItemActivityClass;
	public String LocationFacetTable;
	public Uri ContentUri;
	public Map<String, String> FilterExpr;
	public String[] ListProjection;
	
	public abstract SimpleCursorAdapter createListAdapter(Context context);
	public abstract CursorLoader createViewItemCursorLoader(Context context, long id);
	public abstract InfoDialogFragment.Options configureViewItemDialog();
}