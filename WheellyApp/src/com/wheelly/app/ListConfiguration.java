package com.wheelly.app;

import java.util.Map;

import com.wheelly.fragments.InfoDialogFragment;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.SimpleCursorAdapter;

public class ListConfiguration {
	public int EmptyTextResourceId;
	public int ConfirmDeleteResourceId;
	
	public Class<?> ItemActivityClass;
	public String LocationFacetTable;
	public Uri ContentUri;
	public Map<String, String> FilterExpr;
	public String[] ListProjection;
}