package com.wheelly.app;

import java.util.Map;

import android.net.Uri;

public class ListConfiguration {
	public int EmptyTextResourceId;
	public int ConfirmDeleteResourceId;
	
	public Class<?> ItemActivityClass;
	public String LocationFacetTable;
	public Uri ContentUri;
	public Map<String, String> FilterExpr;
	public String[] ListProjection;
}