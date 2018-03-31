package com.saiyanstudio.groceryassistant.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.saiyanstudio.groceryassistant.BarcodeScannerActivity;
import com.saiyanstudio.groceryassistant.MainActivity;
import com.saiyanstudio.groceryassistant.NutrientDialog;
import com.saiyanstudio.groceryassistant.NutrientInfoActivity;
import com.saiyanstudio.groceryassistant.R;
import com.saiyanstudio.groceryassistant.FragmentIntentIntegrator;
import com.saiyanstudio.groceryassistant.handlers.NodeJsUploadHandler;
import com.saiyanstudio.groceryassistant.models.GroceryItem;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by deeks on 11/4/2015.
 */
public class ScanFragment extends Fragment {

    View rootView;

    ScrollView scrollView;
    RelativeLayout relativeLayout_scan;

    Button productPicButton;
    Button groceryPicButton;
    Button barcodeButton;
    Button nftCameraButton;
    ImageView nftImageFromCamera;
    CardView uploadCard;
    TextView scanProductName;
    TextView scanBarcode;
    Button cancelButton;
    Button uploadButton;

    Boolean isCancelButtonClicked = false;

    String productName;
    String productBarcode;
    String productImageURL;
    String barcodeJsonData;
    public  final String TAG = "GROCERY ASSISTANT ";

    private static final int PRODUCT_PIC_CAMERA_REQUEST = 2888;
    private static final int GROCERY_PIC_CAMERA_REQUEST = 3888;
    private static final int CAMERA_REQUEST = 1888;

    File finalFile;
    Uri imageToUploadUri;

    Boolean wasProductPicButtonClicked = false;
    Boolean wasScanButtonClicked = false;
    String productPicFilename = "";

    private ProgressDialog parseDBSearchProgressBar;
    public ProgressDialog productPicUploadProgressBar;
    public ProgressDialog nftUploadProgressBar;
    public ProgressDialog nutInfoUploadProgressBar;
    public ProgressDialog groceryPicUploadProgressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_scan, container, false);

        scrollView = (ScrollView) rootView.findViewById(R.id.scrollView);
        relativeLayout_scan = (RelativeLayout) rootView.findViewById(R.id.relativeLayout_scan);

        //making the upload card invisible initially
        uploadCard = (CardView) rootView.findViewById(R.id.card_view1);
        uploadCard.setVisibility(View.GONE);


        scanProductName = (TextView)rootView.findViewById(R.id.scanProductName);
        scanBarcode = (TextView)rootView.findViewById(R.id.scanBarcode);

        cancelButton = (Button) rootView.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                removeUploadCard();

            }
        });


        uploadButton = (Button) rootView.findViewById(R.id.upload_button);
        uploadButton.setTextColor(0xFFAAAAAA);

        productPicButton = (Button) rootView.findViewById(R.id.productPic_button);
        productPicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, PRODUCT_PIC_CAMERA_REQUEST);
            }
        });

        groceryPicButton = (Button) rootView.findViewById(R.id.groceryPic_Button);
        groceryPicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, GROCERY_PIC_CAMERA_REQUEST);
            }
        });

        barcodeButton = (Button) rootView.findViewById(R.id.barcode_button);
        barcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(wasProductPicButtonClicked){
                    wasProductPicButtonClicked = false;

                    //call the BarcodeScanActivity to capture barcode
                    FragmentIntentIntegrator integrator = new FragmentIntentIntegrator(ScanFragment.this);
                    integrator.setCaptureActivity(BarcodeScannerActivity.class);
                    integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
                    integrator.setPrompt("Scan a barcode");
                    integrator.setCameraId(0);  // Use a specific camera of the device
                    integrator.setBeepEnabled(false);
                    integrator.setBarcodeImageEnabled(true);
                    integrator.initiateScan();
                }else {
                    Snackbar.make(relativeLayout_scan, "You need to take the product pic first", Snackbar.LENGTH_INDEFINITE).show();
                    //Toast.makeText(getActivity(),"You need to take the product pic first",Toast.LENGTH_SHORT).show();
                }
            }
        });

        nftCameraButton = (Button) rootView.findViewById(R.id.nft_camera_button);
        nftCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(wasScanButtonClicked){
                    //call inbuilt camera for capturing NFT image
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    //set the location where the NFT image is stored
                    finalFile = new File(Environment.getExternalStorageDirectory(), "NFT_IMAGE.jpg");
                    //make the NFT image to be stored in the location finalFile
                    cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(finalFile));
                    imageToUploadUri = Uri.fromFile(finalFile);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }else {
                    Snackbar.make(relativeLayout_scan, "You need to scan the barcode first", Snackbar.LENGTH_INDEFINITE).show();
                    //Toast.makeText(getActivity(),"You need to scan the barcode first",Toast.LENGTH_SHORT).show();
                }
            }
        });

        nftImageFromCamera = (ImageView) rootView.findViewById(R.id.nft_image_view);

        parseDBSearchProgressBar = new ProgressDialog(getActivity());
        parseDBSearchProgressBar.setTitle("Please wait.. searching for product in ParseDB");
        parseDBSearchProgressBar.setIndeterminate(false);
        parseDBSearchProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        productPicUploadProgressBar = new ProgressDialog(getActivity());
        productPicUploadProgressBar.setTitle("Please wait.. product pic is being uploaded");
        productPicUploadProgressBar.setIndeterminate(false);
        productPicUploadProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        nftUploadProgressBar = new ProgressDialog(getActivity());
        nftUploadProgressBar.setTitle("Please wait.. nft pic is being uploaded");
        nftUploadProgressBar.setIndeterminate(false);
        nftUploadProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        nutInfoUploadProgressBar = new ProgressDialog(getActivity());
        nutInfoUploadProgressBar.setTitle("Please wait.. nut info is being uploaded");
        nutInfoUploadProgressBar.setIndeterminate(false);
        nutInfoUploadProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        groceryPicUploadProgressBar  = new ProgressDialog(getActivity());
        groceryPicUploadProgressBar.setTitle("Please wait.. uploading image and fetching info");
        groceryPicUploadProgressBar.setIndeterminate(false);
        groceryPicUploadProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){

            // do whatever you want

            case 49374:
                if (resultCode == Activity.RESULT_OK) {

                    //retrieve the barcode scanned
                    String _code = data.getStringExtra("SCAN_RESULT");

                    wasScanButtonClicked = true;
                    //retrieve details of product from the barcode
                    getProductDetailFromBarcode(_code);
                }
                break;
            case PRODUCT_PIC_CAMERA_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    try {
                        productPicUploadProgressBar.show();
                        uploadProductImageToNode(photo);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case GROCERY_PIC_CAMERA_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    try {
                        groceryPicUploadProgressBar.show();
                        uploadGroceryImageToNode(photo);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CAMERA_REQUEST:
                if (resultCode == Activity.RESULT_OK) {

                    wasScanButtonClicked = false;
                    scrollView.fullScroll(ScrollView.FOCUS_UP);
                    //set scale downed image bitmap for ImageView nftImageFromCamera
                    if(imageToUploadUri != null){
                        Uri selectedImage = imageToUploadUri;
                        getActivity().getContentResolver().notifyChange(selectedImage, null);
                        Bitmap reducedSizeBitmap = getBitmap(imageToUploadUri.getPath());
                        if(reducedSizeBitmap != null){
                            nftImageFromCamera.setImageBitmap(reducedSizeBitmap);
                        }else{
                            Snackbar.make(relativeLayout_scan, "Error while capturing Image", Snackbar.LENGTH_LONG).show();
                            //Toast.makeText(getActivity(),"Error while capturing Image",Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Snackbar.make(relativeLayout_scan, "Error while capturing Image", Snackbar.LENGTH_LONG).show();
                        //Toast.makeText(getActivity(),"Error while capturing Image",Toast.LENGTH_LONG).show();
                    }

                    uploadButton.setEnabled(true);
                    uploadButton.setTextColor(0xFF777777);

                    uploadButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                //uploading the image to Node.js server
                                //uploadImageToNode();
                                nftUploadProgressBar.show();
                                uploadNFTImageToNode();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                break;

        }
    }

    //function to get scaled down bitmap image from the location
    // where NFT image is stored
    private Bitmap getBitmap(String path) {

        Uri uri = Uri.fromFile(new File(path));
        InputStream in = null;
        try {
            final int IMAGE_MAX_SIZE = 1200000; // 1.2MP
            in = getActivity().getContentResolver().openInputStream(uri);

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();


            int scale = 1;
            while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) >
                    IMAGE_MAX_SIZE) {
                scale++;
            }
            Log.d("", "scale = " + scale + ", orig-width: " + o.outWidth + ", orig-height: " + o.outHeight);

            Bitmap b = null;
            in = getActivity().getContentResolver().openInputStream(uri);
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                o = new BitmapFactory.Options();
                o.inSampleSize = scale;
                b = BitmapFactory.decodeStream(in, null, o);

                // resize to desired dimensions
                int height = b.getHeight();
                int width = b.getWidth();
                Log.d("", "1th scale operation dimenions - width: " + width + ", height: " + height);

                double y = Math.sqrt(IMAGE_MAX_SIZE
                        / (((double) width) / height));
                double x = (y / height) * width;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, (int) x,
                        (int) y, true);
                b.recycle();
                b = scaledBitmap;

                System.gc();
            } else {
                b = BitmapFactory.decodeStream(in);
            }
            in.close();

            Log.d("", "bitmap size - width: " + b.getWidth() + ", height: " +
                    b.getHeight());
            return b;
        } catch (IOException e) {
            Log.e("", e.getMessage(), e);
            return null;
        }
    }

    //function that uses OkHttp to retrieve product details from searchupc.com
    private void getProductDetailFromBarcode(final String upc_code) {

        //https://api.outpan.com/v2/products/[GTIN]?apikey=[YOUR API KEY]
        String barcodeCheckURL = "https://api.outpan.com/v2/products/" + upc_code + "?apikey=a9dfdae8a446eb65602ad26560c8ea02";

        //String barcodeCheckURL = "http://www.searchupc.com/handlers/upcsearch.ashx?request_type=3&access_token=22A70151-4378-4ED7-A84F-9FD0374C4239&upc=" + upc_code;

        GroceryItem newItem = null;

        scanProductName = (TextView)rootView.findViewById(R.id.scanProductName);
        scanBarcode = (TextView)rootView.findViewById(R.id.scanBarcode);

        if (isNetworkAvailable()) {

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(barcodeCheckURL)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {

                @Override
                public void onFailure(Request request, IOException e) {
                    //errorAlert();
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    try {
                        barcodeJsonData = response.body().string();
                        Log.v(TAG, barcodeJsonData);

                        if (response.isSuccessful()) {

                            final JSONObject barcodeData = new JSONObject(barcodeJsonData);

                            Log.d("GROCERY SCAN BARCODE", "Product Data : " + barcodeData.toString());

                            JSONArray imageArray = barcodeData.getJSONArray("images");

                            productImageURL = imageArray.getString(0);
                            productImageURL = productImageURL.replace("\\/", "/");
                            productName = barcodeData.getString("name");
                            productBarcode = upc_code;

                            Log.i("GROCERY SCAN BARCODE", "Product Name : " + productName);
                            Log.i("GROCERY SCAN BARCODE", "Product Name : " + productImageURL);

                            /*
                            JSONObject _data = barcodeData.getJSONObject("0");
                            Log.d(TAG, "Product Data : " + _data.toString());
                            Log.d(TAG, "Product Name : " + _data.getString("productname"));

                            productImageURL = _data.getString("imageurl");
                            productName = _data.getString("productname");
                            productBarcode = upc_code;

                            */

                            final GroceryItem newItem = new GroceryItem(upc_code, productName, productImageURL);

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    if (newItem != null) {

                                        scanProductName.setText(newItem.getName());
                                        scanBarcode.setText(newItem.getBarcode());
                                        showUploadCard();
                                        //parseDBSearchProgressBar.show();

                                        //checkForProductInParse(newItem.getBarcode());

                                        //Toast.makeText(getActivity(), "Please take the NFT image", Toast.LENGTH_SHORT).show();
                                    } else {

                                        scanProductName.setText("N/A");
                                        scanBarcode.setText(upc_code);
                                        showUploadCard();
                                        Snackbar.make(relativeLayout_scan, "Could not find the product description", Snackbar.LENGTH_LONG).show();
                                        //Toast.makeText(getActivity(), "Could not find the product description", Toast.LENGTH_LONG).show();
                                        //parseDBSearchProgressBar.show();
                                        //checkForProductInParse(upc_code);
                                    }
                                }
                            });
                        } else {
                            Snackbar.make(relativeLayout_scan, "Sorry no product available", Snackbar.LENGTH_LONG).show();
                            //Toast.makeText(getActivity(), "Sorry no product available", Toast.LENGTH_SHORT).show();
                        }

                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }

                }
            });
        } else {
            Snackbar.make(relativeLayout_scan, "Network Unavialable", Snackbar.LENGTH_INDEFINITE).show();
            //Toast.makeText(getActivity(),"Network Unavialable", Toast.LENGTH_SHORT).show();
            scanProductName.setText("N/A");
            scanBarcode.setText(upc_code);
            showUploadCard();
        }
    }

    public void checkForProductInParseWithFoundTrue(final String barcode){

        productPicUploadProgressBar.dismiss();

        parseDBSearchProgressBar.show();

        Log.i("GROCERRY ASSISTANT","checkForProductInParseWithFoundTrue called");

        if(isNetworkAvailable()){

            final String userEmail;
            if(ParseUser.getCurrentUser() != null){
                userEmail = ParseUser.getCurrentUser().getEmail();
            }else {
                userEmail = "goku@dbz.com";
            }

            ParseQuery<ParseObject> query = ParseQuery.getQuery("NutrientInfo");
            query.whereEqualTo("username", userEmail);
            query.whereEqualTo("productBarcode", barcode);
            Log.i("GROCERRY ASSISTANT", "username : " + userEmail + " ,barcode : " + barcode);
            query.getFirstInBackground(new GetCallback<ParseObject>() {
                @Override
                public void done(final ParseObject parseObject, ParseException e) {

                    parseDBSearchProgressBar.dismiss();

                    if (e == null) {

                        //update profile attributes
                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                        String todaysDate = df.format(c.getTime());

                        int scansPerDay;

                        if(parseObject.get("uploadDate").equals(todaysDate)){
                            scansPerDay = parseObject.getInt("scansPerDay");
                            scansPerDay++;
                            parseObject.put("scansPerDay", scansPerDay);
                            parseObject.saveInBackground(new SaveCallback() {
                                public void done(ParseException e) {
                                    if (e == null) {
                                        //remove the upload card
                                        removeUploadCard();

                                        //refresh the graphs in profile Fragment to include the latest product
                                        MainActivity mainActivity = (MainActivity) getActivity();
                                        mainActivity.refreshGraphs();

                                        wasProductPicButtonClicked = false;

                                        Intent intent = new Intent(getActivity(), NutrientInfoActivity.class);
                                        intent.putExtra("productName", parseObject.getString("productName"));
                                        intent.putExtra("productImage", parseObject.getString("imageURL"));
                                        intent.putExtra("energy", parseObject.getString("energy"));
                                        intent.putExtra("carbohydrates", parseObject.getString("carbohydrates"));
                                        intent.putExtra("sugars", parseObject.getString("sugars"));
                                        intent.putExtra("protein", parseObject.getString("protein"));
                                        intent.putExtra("fats", parseObject.getString("fats"));
                                        startActivity(intent);
                                    } else {
                                        Snackbar.make(relativeLayout_scan, "Please authenicate..Couldnt update the upload Date" + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                        //Toast.makeText(getActivity(), "Please authenicate..Couldnt update the upload Date", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }else{
                            ParseObject newNutrientInfo = new ParseObject("NutrientInfo");
                            scansPerDay = 1;

                            newNutrientInfo.put("username", userEmail);
                            newNutrientInfo.put("productName", parseObject.getString("productName"));
                            newNutrientInfo.put("productBarcode", barcode);
                            newNutrientInfo.put("imageURL", parseObject.getString("imageURL"));
                            newNutrientInfo.put("energy", parseObject.getString("energy"));
                            newNutrientInfo.put("carbohydrates", parseObject.getString("carbohydrates"));
                            newNutrientInfo.put("sugars", parseObject.getString("sugars"));
                            newNutrientInfo.put("protein", parseObject.getString("protein"));
                            newNutrientInfo.put("fats", parseObject.getString("fats"));
                            newNutrientInfo.put("scansPerDay", scansPerDay);
                            newNutrientInfo.put("uploadDate", todaysDate);
                            newNutrientInfo.saveInBackground(new SaveCallback() {
                                public void done(ParseException e) {
                                    if (e == null) {
                                        //remove the upload card
                                        removeUploadCard();

                                        //refresh the graphs in profile Fragment to include the latest product
                                        MainActivity mainActivity = (MainActivity) getActivity();
                                        mainActivity.refreshGraphs();

                                        wasProductPicButtonClicked = false;

                                        Intent intent = new Intent(getActivity(), NutrientInfoActivity.class);
                                        intent.putExtra("productName", parseObject.getString("productName"));
                                        intent.putExtra("productImage", parseObject.getString("imageURL"));
                                        intent.putExtra("energy", parseObject.getString("energy"));
                                        intent.putExtra("carbohydrates", parseObject.getString("carbohydrates"));
                                        intent.putExtra("sugars", parseObject.getString("sugars"));
                                        intent.putExtra("protein", parseObject.getString("protein"));
                                        intent.putExtra("fats", parseObject.getString("fats"));
                                        startActivity(intent);
                                    } else {
                                        Snackbar.make(relativeLayout_scan, "Please authenicate..Couldnt update the upload Date", Snackbar.LENGTH_LONG).show();
                                        //Toast.makeText(getActivity(), "Please authenicate..Couldnt update the upload Date", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }


                    } else {
                        // couldnt find the object
                        wasProductPicButtonClicked = true;
                        Snackbar.make(relativeLayout_scan, "Please scan barcode and take the NFT image", Snackbar.LENGTH_INDEFINITE).show();
                        //Toast.makeText(getActivity(), "Please scan barcode and take the NFT image", Toast.LENGTH_SHORT).show();
                    }
                }
            });


        }else {
            Snackbar.make(relativeLayout_scan, "Network Unavialable", Snackbar.LENGTH_INDEFINITE).show();
            //Toast.makeText(getActivity(),"Network Unavialable", Toast.LENGTH_SHORT).show();
        }
    }

    public void checkForProductInParse(String barcode){

        Log.i("GROCERRY ASSISTANT", "checkForProductInParse called");
        if(isNetworkAvailable()){

            final String userEmail;
            if(ParseUser.getCurrentUser() != null){
                userEmail = ParseUser.getCurrentUser().getEmail();
            }else {
                userEmail = "goku@dbz.com";
            }

            ParseQuery<ParseObject> query = ParseQuery.getQuery("NutrientInfo");
            query.whereEqualTo("username", userEmail);
            query.whereEqualTo("productBarcode", barcode);
            query.getFirstInBackground(new GetCallback<ParseObject>() {
                @Override
                public void done(final ParseObject parseObject, ParseException e) {

                    if (e == null) {
                        //product is present in parse db
                        //remove the upload card
                        nutInfoUploadProgressBar.dismiss();
                        wasProductPicButtonClicked = false;
                        wasScanButtonClicked = false;
                        removeUploadCard();

                        //refresh the graphs in profile Fragment to include the latest product
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.refreshGraphs();

                        Intent intent = new Intent(getActivity(), NutrientInfoActivity.class);
                        intent.putExtra("productName", parseObject.getString("productName"));
                        intent.putExtra("productImage", parseObject.getString("imageURL"));
                        intent.putExtra("energy", parseObject.getString("energy"));
                        intent.putExtra("carbohydrates", parseObject.getString("carbohydrates"));
                        intent.putExtra("sugars", parseObject.getString("sugars"));
                        intent.putExtra("protein", parseObject.getString("protein"));
                        intent.putExtra("fats", parseObject.getString("fats"));
                        startActivity(intent);
                    } else {
                        // couldnt find the object
                        nutInfoUploadProgressBar.dismiss();
                        wasProductPicButtonClicked = false;
                        wasScanButtonClicked = true;
                        Snackbar.make(relativeLayout_scan, "Please take the NFT image", Snackbar.LENGTH_INDEFINITE).show();
                        //Toast.makeText(getActivity(), "Please take the NFT image", Toast.LENGTH_SHORT).show();
                    }
                }
            });


        }else {
            Snackbar.make(relativeLayout_scan, "Network Unavialable", Snackbar.LENGTH_INDEFINITE).show();
            //Toast.makeText(getActivity(),"Network Unavialable", Toast.LENGTH_SHORT).show();
        }
    }

    public void getGroceryItemInfoFromParse(String groceryName){

        Log.i("GROCERRY ASSISTANT", "getGroceryItemInfoFromParse called");
        if(isNetworkAvailable()){

            ParseQuery<ParseObject> query = ParseQuery.getQuery("GroceryItem");
            query.whereEqualTo("groceryName", groceryName);
            query.getFirstInBackground(new GetCallback<ParseObject>() {
                @Override
                public void done(final ParseObject parseObject, ParseException e) {

                    if (e == null) {
                        //product is present in parse db
                        groceryPicUploadProgressBar.dismiss();

                        Intent intent = new Intent(getActivity(), NutrientInfoActivity.class);
                        intent.putExtra("productName", parseObject.getString("groceryName") + " " + parseObject.getString("groceryQuantity"));
                        intent.putExtra("productImage", parseObject.getString("imageURL"));
                        intent.putExtra("energy", parseObject.getString("energy"));
                        intent.putExtra("carbohydrates", parseObject.getString("carbohydrates"));
                        intent.putExtra("sugars", parseObject.getString("sugars"));
                        intent.putExtra("protein", parseObject.getString("protein"));
                        intent.putExtra("fats", parseObject.getString("fats"));
                        startActivity(intent);
                    } else {
                        // couldnt find the object
                        groceryPicUploadProgressBar.dismiss();
                        Snackbar.make(relativeLayout_scan, "Sorry couldnt find the item in our database", Snackbar.LENGTH_LONG).show();
                        //Toast.makeText(getActivity(), "Sorry couldnt find the item in our database", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else {
            Snackbar.make(relativeLayout_scan, "Network Unavialable", Snackbar.LENGTH_INDEFINITE).show();
            //Toast.makeText(getActivity(),"Network Unavialable", Toast.LENGTH_SHORT).show();
        }
    }


    //making uploadCard visible with fading animations
    private void showUploadCard(){
        isCancelButtonClicked = false;
        uploadCard.setVisibility(View.VISIBLE);
        uploadCard.setAlpha(0.0f);
        uploadCard.animate().translationY(uploadCard.getHeight()).translationX(0);
        uploadCard.animate()
                .translationY(0)
                .alpha(1.0f)
                .setDuration(500);
    }

    //making uploadCard disapear with fading animations
    public void removeUploadCard() {

        uploadButton.setEnabled(false);
        uploadButton.setTextColor(0xFFAAAAAA);
        nftImageFromCamera.setImageDrawable(null);

        isCancelButtonClicked = true;
        //sliding animation for uploadCard when canceled
        uploadCard.animate()
                .translationX(uploadCard.getWidth())
                .alpha(0.0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (isCancelButtonClicked)
                            uploadCard.setVisibility(View.GONE);
                    }
                });
    }

    public void scanAndUploadNFT(String tempProductPicUploadsFilename){
        wasProductPicButtonClicked = true;
        productPicUploadProgressBar.dismiss();
        productPicFilename = tempProductPicUploadsFilename;
        Log.i("NODEJS", productPicFilename);
        Snackbar.make(relativeLayout_scan, "Scan the barcode followed by NFT pic", Snackbar.LENGTH_INDEFINITE).show();
        //Toast.makeText(getActivity(),"Scan the barcode followed by NFT pic", Toast.LENGTH_SHORT).show();
    }

    //show an alert dialog with editable nutrient info
    //it must also show the image , name and barcode
    public void showNutrientAlertDialog(String energy, String carbohydrates,String sugars, String protein, String fats){

        nftUploadProgressBar.dismiss();

        NutrientDialog nutrientDialog = new NutrientDialog(getActivity(),productName, productBarcode, productImageURL, energy, carbohydrates, sugars, protein, fats);
        nutrientDialog.show();
    }

    private void uploadGroceryImageToNode(Bitmap myBitmap) throws IOException {

        if (isNetworkAvailable()) {

            //Use retrofit to upload image to Node.js server
            NodeJsUploadHandler nodeJsUploadHandler = new NodeJsUploadHandler(getActivity());

            //create a file to write bitmap data
            File imageFile = new File(getActivity().getCacheDir(), "myGroceryImage");
            imageFile.createNewFile();

            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            byte[] bitmapdata = bos.toByteArray();

            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();

            nodeJsUploadHandler.uploadGroceryImage(imageFile);

        } else {
            productPicUploadProgressBar.dismiss();
            Snackbar.make(relativeLayout_scan, "Network Unavialable", Snackbar.LENGTH_INDEFINITE).show();
            //Toast.makeText(getActivity(),"Network Unavialable", Toast.LENGTH_SHORT).show();
        }
    }


    private void uploadProductImageToNode(Bitmap myBitmap) throws IOException {

        if (isNetworkAvailable()) {

            //Use retrofit to upload image to Node.js server
            NodeJsUploadHandler nodeJsUploadHandler = new NodeJsUploadHandler(getActivity());

            String userId;
            if(ParseUser.getCurrentUser() != null){
                userId = ParseUser.getCurrentUser().getObjectId();
            }else {
                userId = "124otE2H1G";
            }

            //create a file to write bitmap data
            File imageFile = new File(getActivity().getCacheDir(), "myProductImage");
            imageFile.createNewFile();

            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            byte[] bitmapdata = bos.toByteArray();

            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();

            nodeJsUploadHandler.uploadProductImage(imageFile,userId);

        } else {
            productPicUploadProgressBar.dismiss();
            Snackbar.make(relativeLayout_scan, "Network Unavialable", Snackbar.LENGTH_INDEFINITE).show();
           // Toast.makeText(getActivity(),"Network Unavialable", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadNFTImageToNode() throws IOException {
        if (isNetworkAvailable()) {

            //Use retrofit to upload image to Node.js server
            NodeJsUploadHandler nodeJsUploadHandler = new NodeJsUploadHandler(getActivity());

            String userEmail, userId;
            if(ParseUser.getCurrentUser() != null){
                userEmail = ParseUser.getCurrentUser().getEmail();
                userId = ParseUser.getCurrentUser().getObjectId();
            }else {
                userEmail = "goku@dbz.com";
                userId = "124otE2H1G";
            }

        //public void uploadImage(File imageFile, String userEmail, String userId, final String barcode, String productPicFilename )

            nodeJsUploadHandler.uploadNFTImage(finalFile, userEmail, userId, scanBarcode.getText().toString(), productPicFilename);

        } else {
            Snackbar.make(relativeLayout_scan, "Network Unavialable", Snackbar.LENGTH_INDEFINITE).show();
            //Toast.makeText(getActivity(),"Network Unavialable", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadNutrientInfoToNode(String productEnergy, String productCarbohydrates, String productSugars, String productProtein, String productFats) throws IOException {
        if (isNetworkAvailable()) {

            //Use retrofit to upload nut info to Node.js server
            NodeJsUploadHandler nodeJsUploadHandler = new NodeJsUploadHandler(getActivity());

            String userEmail;
            if(ParseUser.getCurrentUser() != null){
                userEmail = ParseUser.getCurrentUser().getEmail();
            }else {
                userEmail = "goku@dbz.com";
            }

            nodeJsUploadHandler.uploadNutrientInfo(userEmail, productName, productBarcode, productEnergy, productCarbohydrates, productSugars, productProtein, productFats, productImageURL);

        } else {
            Snackbar.make(relativeLayout_scan, "Network Unavialable", Snackbar.LENGTH_INDEFINITE).show();
            //Toast.makeText(getActivity(),"Network Unavialable", Toast.LENGTH_SHORT).show();
        }
    }

    //function to scheck wheter network is available
    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        boolean isAvailable = false;

        if (networkInfo != null && networkInfo.isConnected()) {

            isAvailable = true;
        }
        return isAvailable;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        uploadCard.setVisibility(View.GONE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }
}
