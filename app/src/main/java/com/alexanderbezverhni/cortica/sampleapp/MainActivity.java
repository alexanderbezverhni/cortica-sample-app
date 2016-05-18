package com.alexanderbezverhni.cortica.sampleapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

	private static final int REQUEST_STORAGE_PERMISSIONS_CODE = 13;
	private static final int REQUEST_PICK_AN_IMAGE = 9162;

	@BindView(R.id.toolbar)
	Toolbar toolbar;
	@BindView(R.id.image)
	ImageView image;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);

		setSupportActionBar(toolbar);
	}

	@OnClick(R.id.fab)
	public void onFabClick() {
		boolean permissionGranted = ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
		if (permissionGranted) {
			pickImage();
		} else {
			ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
					REQUEST_STORAGE_PERMISSIONS_CODE);
		}
	}

	private void pickImage() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
		try {
			startActivityForResult(intent, REQUEST_PICK_AN_IMAGE);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.no_images_found, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK && requestCode == REQUEST_PICK_AN_IMAGE) {
			Uri uri = data.getData();
			if (uri != null) {
				onImageSelected(uri);
			}
		}
	}

	private void onImageSelected(Uri uri) {
		Picasso.with(this).load(uri).into(image);
		// TODO: dispath requests
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == REQUEST_STORAGE_PERMISSIONS_CODE) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				pickImage();
			} else {
				Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_LONG).show();
			}
		}
	}
}
