package com.wheelly.app;

import java.util.Map;


import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;

public abstract class ListConfiguration {
	public int EmptyTextResourceId;
	public int ConfirmDeleteResourceId;
	public int OptionsMenuResourceId;
	public int ContextMenuHeaderResourceId;
	
	public Class<?> ItemActivityClass;
	public String LocationFacetTable;
	public Uri ContentUri;
	public Map<String, String> FilterExpr;
	public String[] ListProjection;
	
	public abstract SimpleCursorAdapter createListAdapter(Context context);
	public abstract CursorLoader createViewItemCursorLoader(Context context, long id);
	public abstract InfoDialogFragment.Options configureViewItemDialog();
	
	public void onActivityCreated(Context context, Bundle savedInstanceState) {}
	public void onCreateOptionsMenu(Menu menu) {}
	public void onCreateContextMenu(ContextMenu menu, ContextMenuInfo menuInfo) {}
	public boolean onOptionsItemSelected(MenuItem item) { return false; }
	public void onSaveInstanceState(Bundle outState) {}
}