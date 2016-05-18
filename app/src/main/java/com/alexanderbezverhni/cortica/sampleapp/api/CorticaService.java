package com.alexanderbezverhni.cortica.sampleapp.api;

import com.alexanderbezverhni.cortica.sampleapp.api.model.Tags;
import com.alexanderbezverhni.cortica.sampleapp.api.model.UploadResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface CorticaService {

	public static final String CORTICA_API_ENDPOINT = "https://personalphotosdev.services.cortica.com/PersonalPhotosFE/V2/";
	public static final String BATCH_SIZE = "1";
	public static final String HEADER_API_KEY = "cortica";
	public static final String HEADER_CLIENT_VERSION = "postman";

	@Multipart
	@POST("photos/upload")
	Call<UploadResponse> upload(@Part MultipartBody.Part file, @Query("pId") String pId, @Query("batchSize") String batchSize);

	@GET("tags")
	Call<Tags> getTags();
}
