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
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.alexanderbezverhni.cortica.sampleapp.api.CorticaService;
import com.alexanderbezverhni.cortica.sampleapp.api.model.Photo;
import com.alexanderbezverhni.cortica.sampleapp.api.model.Tag;
import com.alexanderbezverhni.cortica.sampleapp.api.model.Tags;
import com.alexanderbezverhni.cortica.sampleapp.api.model.UploadResponse;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

	private static final int REQUEST_STORAGE_PERMISSIONS_CODE = 13;
	private static final int REQUEST_PICK_AN_IMAGE = 9162;

	private static final long POLLING_PERIOD_MS = DateUtils.SECOND_IN_MILLIS * 5;

	@BindView(R.id.scroller)
	ScrollView scroller;
	@BindView(R.id.toolbar)
	Toolbar toolbar;
	@BindView(R.id.card_view)
	View imageContainer;
	@BindView(R.id.image)
	ImageView image;
	@BindView(R.id.tags)
	TextView tags;
	@BindView(R.id.tags_title)
	TextView tagsTitle;

	private Timer timer;
	private TimerTask timerTask;

	private CorticaService service;
	private String deviceId;
	private Uri selectedImageUri;
	private String imageServerId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);

		setSupportActionBar(toolbar);
		deviceId = Utils.getDeviceId(this);
		initRetrofit();
	}

	private void initRetrofit() {
		OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
			@Override
			public okhttp3.Response intercept(Chain chain) throws IOException {
				Request request = chain.request().newBuilder().addHeader("uId", deviceId).addHeader("apiKey",
						CorticaService.HEADER_API_KEY).addHeader("clientVersion", CorticaService.HEADER_CLIENT_VERSION).build();
				return chain.proceed(request);
			}
		}).build();

		Retrofit retrofit = new Retrofit.Builder().baseUrl(CorticaService.CORTICA_API_ENDPOINT).addConverterFactory(
				GsonConverterFactory.create()).client(httpClient).build();
		service = retrofit.create(CorticaService.class);
	}

	private void pickImage() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
		try {
			startActivityForResult(intent, REQUEST_PICK_AN_IMAGE);
		} catch (ActivityNotFoundException e) {
			showToast(R.string.no_images_found);
		}
	}

	private void onImageSelected(Uri uri) {
		scroller.fullScroll(View.FOCUS_UP);
		showToast(R.string.upload_start_mes);
		tagsTitle.setVisibility(View.INVISIBLE);
		tags.setVisibility(View.INVISIBLE);

		imageContainer.setVisibility(View.VISIBLE);
		selectedImageUri = uri;
		Picasso.with(this).load(selectedImageUri).into(image);

		uploadImage(selectedImageUri);
		startPollingForTags();
	}

	private void fetchTags() {
		Call<Tags> call = service.getTags();
		call.enqueue(new Callback<Tags>() {
			@Override
			public void onResponse(Call<Tags> call, Response<Tags> response) {
				List<String> tags = getTagsLabels(response.body());
				if (tags != null && !tags.isEmpty()) {
					onTagsReceived(tags);
				}
			}

			@Override
			public void onFailure(Call<Tags> call, Throwable t) {
				// do nothing
			}
		});
	}

	private void onTagsReceived(List<String> tags) {
		stopPolling();

		StringBuilder concatenated = new StringBuilder();
		for (String tag : tags) {
			concatenated.append(tag).append("\n");
		}

		tagsTitle.setVisibility(View.VISIBLE);
		this.tags.setVisibility(View.VISIBLE);
		this.tags.setText(concatenated);
	}

	private List<String> getTagsLabels(Tags payload) {
		List<String> tagsLabels = new ArrayList<>();

		if (payload == null) {
			return null;
		}

		List<Tag> tags = payload.getTags();
		if (tags == null || tags.isEmpty()) {
			return null;
		}

		for (Tag tag : tags) {
			List<Photo> photos = tag.getPhotos();
			if (photos != null && !photos.isEmpty()) {
				for (Photo photo : photos) {
					if (imageServerId.equals(photo.getImageId())) {
						tagsLabels.add(tag.getLabel());
						break;
					}
				}
			}
		}

		return tagsLabels;
	}

	private void startPollingForTags() {
		stopPolling();
		timer = new Timer("cortica_tags_poller");
		timerTask = new TimerTask() {
			@Override
			public void run() {
				fetchTags();
			}
		};
		timer.schedule(timerTask, 0, POLLING_PERIOD_MS);
	}

	private void stopPolling() {
		if (timer != null) {
			timerTask.cancel();
			timer.cancel();
			timer = null;
			timerTask = null;
		}
	}

	private void uploadImage(Uri fileUri) {
		// use the Utils to get the actual file by uri
		String filePath = Utils.getPath(this, fileUri);
		File file = new File(filePath);

		// create RequestBody instance from file
		RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);

		// MultipartBody.Part is used to send also the actual file name
		MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

		// finally, execute the request
		imageServerId = Utils.getImageId();
		Call<UploadResponse> call = service.upload(body, imageServerId, CorticaService.BATCH_SIZE);
		call.enqueue(new Callback<UploadResponse>() {
			@Override
			public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
				boolean success = !TextUtils.isEmpty(response.body().getImageId());
				if (success) {
					onUploadSuccess();
				} else {
					onUploadFailure();
				}
			}

			@Override
			public void onFailure(Call<UploadResponse> call, Throwable t) {
				onUploadFailure();
			}
		});
	}

	private void onUploadFailure() {
		showToast(R.string.upload_failure_mes);
	}

	private void onUploadSuccess() {
		showToast(R.string.upload_success_mes);
	}

	private void showToast(int textResId) {
		Toast.makeText(this, textResId, Toast.LENGTH_SHORT).show();
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

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == REQUEST_STORAGE_PERMISSIONS_CODE) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				pickImage();
			} else {
				showToast(R.string.storage_permission_denied);
			}
		}
	}
}
