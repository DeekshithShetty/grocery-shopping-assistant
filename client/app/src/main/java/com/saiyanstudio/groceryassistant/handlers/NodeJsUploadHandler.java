package com.saiyanstudio.groceryassistant.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.saiyanstudio.groceryassistant.MainActivity;
import com.saiyanstudio.groceryassistant.interfaces.NodeJsUploadService;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by MAHE on 2/10/2016.
 */
public class NodeJsUploadHandler {

    Context context;

    SharedPreferences settingsPrefs;

    public static String BASE_URL;

    public NodeJsUploadHandler(Context context){
        this.context = context;
        settingsPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        BASE_URL = "http://" + settingsPrefs.getString("ip_address", "192.168.2.4") + ":" + settingsPrefs.getString("port", "8080") + "/";
        //backgroundThreadShortToast(context,BASE_URL);
    }

    public void uploadGroceryImage(File imageFile) {

        OkHttpClient httpClient = new OkHttpClient();
        httpClient.setReadTimeout(60 * 1000, TimeUnit.MILLISECONDS);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();

        NodeJsUploadService service = retrofit.create(NodeJsUploadService.class);

        RequestBody requestBody =
                RequestBody.create(MediaType.parse("multipart/form-data"), imageFile);

        Call<JsonElement> call = service.uploadGroceryImage(requestBody);
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Response<JsonElement> response, Retrofit retrofit) {

                JsonObject jsonObj = response.body().getAsJsonObject();
                if(jsonObj.get("status").getAsInt() == 200){
                    backgroundThreadGroceryNutrientActivity(context,jsonObj.get("groceryName").getAsString());
                }else {
                    backgroundThreadDismissGroceryPicProgressBar(context);
                    backgroundThreadShortToast(context,jsonObj.get("error").getAsString());
                }

                Log.v("IMAGE_UPLOAD", String.valueOf(jsonObj.get("status")));
            }

            @Override
            public void onFailure(Throwable t) {
                backgroundThreadDismissGroceryPicProgressBar(context);
                backgroundThreadShortToast(context,"Image Uploaded Failed!");
                Log.e("Upload", t.getMessage());
            }
        });
    }

    public void uploadProductImage(File myProductImage, String userId) {

        OkHttpClient httpClient = new OkHttpClient();
        httpClient.setReadTimeout(60 * 1000, TimeUnit.MILLISECONDS);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();

        NodeJsUploadService service = retrofit.create(NodeJsUploadService.class);

        RequestBody requestBody =
                RequestBody.create(MediaType.parse("multipart/form-data"), myProductImage);

        Call<JsonElement> call = service.uploadProductImage(requestBody, userId);
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Response<JsonElement> response, Retrofit retrofit) {

                JsonObject jsonObj = response.body().getAsJsonObject();
                if(jsonObj.get("status").getAsInt() == 200){
                    if(jsonObj.get("found").getAsBoolean()) {
                        Log.v("NODEJS", "found is true");
                        backgroundThreadShortToast(context,"Product is already in db!");
                        String barcode = jsonObj.get("barcode").getAsString();
                        barcode = barcode.replace("\"", "");
                        backgroundThreadNutrientActivityWithFoundTrue(context, barcode);
                    }else {
                        backgroundThreadShortToast(context,"Product Image Uploaded Yo!");
                        backgroundThreadScanAndUploadNFT(context, String.valueOf(jsonObj.get("tempProductPicUploadsfilename")));
                    }
                }else {
                    backgroundThreadDismissProductPicProgressBar(context);
                    backgroundThreadShortToast(context,jsonObj.get("error").getAsString());
                }

                Log.v("IMAGE_UPLOAD", String.valueOf(jsonObj.get("status")));
            }

            @Override
            public void onFailure(Throwable t) {
                backgroundThreadDismissProductPicProgressBar(context);
                backgroundThreadShortToast(context,"Image Uploaded Failed!");
                Log.e("Upload", t.getMessage());
            }
        });
    }

    public void uploadNFTImage(File imageFile, String userEmail, String userId, final String barcode, String productPicFilename ) {

        OkHttpClient httpClient = new OkHttpClient();
        httpClient.setReadTimeout(60 * 1000, TimeUnit.MILLISECONDS);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();

        NodeJsUploadService service = retrofit.create(NodeJsUploadService.class);

        RequestBody requestBody =
                RequestBody.create(MediaType.parse("multipart/form-data"), imageFile);

        Call<JsonElement> call = service.uploadNFTImage(requestBody,userEmail, userId, barcode, productPicFilename);
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Response<JsonElement> response, Retrofit retrofit) {

                JsonObject jsonObj = response.body().getAsJsonObject();
                backgroundThreadShortToast(context,"NFT Image Uploaded Yo!");
                if(jsonObj.get("status").getAsInt() == 200){
                    Log.i("NODEJS","onResponse success in NFTImageUplodHandler");
                    //JsonParser parser = new JsonParser();
                    //JsonObject jsonNutrient  = parser.parse(jsonObj.get("jsonNutrients").getAsString()).getAsJsonObject();
                    backgroundThreadNutrientAlertDialog(context,jsonObj.get("energy").getAsString(),jsonObj.get("carbohydrates").getAsString(),jsonObj.get("sugars").getAsString(),jsonObj.get("protein").getAsString(),jsonObj.get("fats").getAsString());
                }else {
                    backgroundThreadDismissNftUploadProgressBar(context);
                    backgroundThreadShortToast(context,jsonObj.get("error").getAsString());
                }

                Log.v("IMAGE_UPLOAD", String.valueOf(jsonObj.get("status")));
            }

            @Override
            public void onFailure(Throwable t) {
                backgroundThreadDismissNftUploadProgressBar(context);
                backgroundThreadShortToast(context,"Image Uploaded Failed!");
                Log.e("Upload", t.getMessage());
            }
        });
    }

    public void uploadNutrientInfo(String userEmail, String productName, final String productBarcode, String productEnergy, String productCarbohydrates, String productSugars, String productProtein, String productFats, String productImageURL) {

        OkHttpClient httpClient = new OkHttpClient();
        httpClient.setReadTimeout(60 * 1000, TimeUnit.MILLISECONDS);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();

        NodeJsUploadService service = retrofit.create(NodeJsUploadService.class);

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String todaysDate = df.format(c.getTime());

        Call<JsonElement> call = service.uploadNutrientInfo(userEmail, productName, productBarcode, productEnergy, productCarbohydrates, productSugars, productProtein, productFats, productImageURL, todaysDate);
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Response<JsonElement> response, Retrofit retrofit) {

                JsonObject jsonObj = response.body().getAsJsonObject();
                backgroundThreadShortToast(context,"Nutrient Info Uploaded Yo!");
                if(jsonObj.get("status").getAsInt() == 200){
                    backgroundThreadNutrientActivity(context,productBarcode);
                }else {
                    backgroundThreadDismissNutInfoProgressBar(context);
                    backgroundThreadShortToast(context,jsonObj.get("error").getAsString());
                }

                Log.v("IMAGE_UPLOAD", String.valueOf(jsonObj.get("success")));
            }

            @Override
            public void onFailure(Throwable t) {
                backgroundThreadDismissNutInfoProgressBar(context);
                backgroundThreadShortToast(context,"Nutrient Info Upload Failed!");
                Log.e("Upload", t.getMessage());
            }
        });
    }

    private void backgroundThreadGroceryNutrientActivity(final Context context, final String groceryName) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {

                MainActivity mainActivity = (MainActivity)context;
                mainActivity.callGroceryNutrientActivity(groceryName);
            }
        });
    }

    private void backgroundThreadShortToast(final Context context, final String msg) {
        if (context != null && msg != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void backgroundThreadDismissGroceryPicProgressBar(final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {

                MainActivity mainActivity = (MainActivity)context;
                mainActivity.dismissGroceryPicProgressBar();
            }
        });
    }

    private void backgroundThreadNutrientActivityWithFoundTrue(final Context context, final String barcode) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {

                MainActivity mainActivity = (MainActivity)context;
                mainActivity.callNutrientActivityWithFoundTrue(barcode);
            }
        });
    }

    private void backgroundThreadScanAndUploadNFT(final Context context, final String tempProductPicUploadsFilename) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                MainActivity mainActivity = (MainActivity)context;
                mainActivity.callScanAndUploadNFT(tempProductPicUploadsFilename);
            }
        });
    }

    private void backgroundThreadDismissProductPicProgressBar(final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {

                MainActivity mainActivity = (MainActivity)context;
                mainActivity.dismissProductPicProgressBar();
            }
        });
    }

    private void backgroundThreadNutrientAlertDialog(final Context context,final String energy, final String carbohydrates,final String sugars,final String protein,final String fats) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {

                MainActivity mainActivity = (MainActivity)context;
                mainActivity.callNutrientAlertDialog(energy, carbohydrates, sugars, protein, fats);
            }
        });
    }

    private void backgroundThreadDismissNftUploadProgressBar(final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {

                MainActivity mainActivity = (MainActivity)context;
                mainActivity.dismissNftUploadProgressBar();
            }
        });
    }

    private void backgroundThreadNutrientActivity(final Context context, final String barcode) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {

                MainActivity mainActivity = (MainActivity)context;
                mainActivity.callNutrientActivity(barcode);
            }
        });
    }

    private void backgroundThreadDismissNutInfoProgressBar(final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {

                MainActivity mainActivity = (MainActivity)context;
                mainActivity.dismissNutInfoProgressBar();
            }
        });
    }
}
