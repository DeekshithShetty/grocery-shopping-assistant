package com.saiyanstudio.groceryassistant.interfaces;

import com.google.gson.JsonElement;
import com.squareup.okhttp.RequestBody;

import retrofit.Call;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;

/**
 * Created by MAHE on 2/10/2016.
 */
public interface NodeJsUploadService {

    //public static final String BASE_URL = "http://192.168.2.4:8080/";

    @Multipart
    @POST("/uploadGroceryPicMobile")
    Call<JsonElement> uploadGroceryImage(
            @Part("myfile\"; filename=\"image.jpg\" ") RequestBody file);

    @Multipart
    @POST("/uploadProductPicMobile")
    Call<JsonElement> uploadProductImage(
            @Part("myProductImage\"; filename=\"image.jpg\" ") RequestBody file,
            @Part("user_id") String userId);

    @Multipart
    @POST("/uploadNFTPicMobile")
    Call<JsonElement> uploadNFTImage(
            @Part("myfile\"; filename=\"image.jpg\" ") RequestBody file,
            @Part("user_email") String userEmail,
            @Part("user_id") String userId,
            @Part("product_barcode") String productBarcode,
            @Part("product_pic_filename") String productPicFilename);

    @FormUrlEncoded
    @POST("/uploadNutrientInfoMobile")
    Call<JsonElement> uploadNutrientInfo(
            @Field("user_email") String userEmail,
            @Field("product_name") String productName,
            @Field("product_barcode") String productBarcode,
            @Field("product_energy") String productEnergy,
            @Field("product_carbohydrates") String productCarbohydrates,
            @Field("product_sugars") String productSugars,
            @Field("product_protein") String productProtein,
            @Field("product_fats") String productFats,
            @Field("product_imageURL") String productImageURL,
            @Field("product_uploadDate") String productUploadDate);
}
