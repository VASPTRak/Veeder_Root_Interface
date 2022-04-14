package com.TrakEngineering.veeder_rootinterface;

import android.app.Activity;
import android.app.AlertDialog;

class ImportDialog {
	final CharSequence[] items = { "Take Photo From Gallery",
			"Take Photo From Camera" };
	private final Activity activity;
	private final AlertDialog.Builder builder;
	private final String detailProvader;
	AlertDialog dialog;

	public ImportDialog(Activity a, String detailProvader) {
		this.activity = a;
		this.detailProvader = detailProvader;
		builder = new AlertDialog.Builder(a);
	}

	public void showDialog() {

		builder.setTitle("wifi Provider Details");
		builder.setMessage(detailProvader);

		AlertDialog alert = builder.create();
		alert.show();
	}
}
