package com.saiyanstudio.groceryassistant;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by MAHE on 1/25/2016.
 */
public class NutrientDialog extends Dialog implements android.view.View.OnClickListener {

    public Activity activity;
    public Dialog d;
    public Button saveInfoButton;

    private String productName;
    private String productBarcode;
    private String productImageURL;
    private String energy;
    private String carbs;
    private String sugars;
    private String proteins;
    private String fats;

    private EditText productNameText;
    private TextView productBarcodeText;
    private EditText calorieText;
    private EditText carboText;
    private EditText sugarText;
    private EditText proteinText;
    private EditText totFatText;
    private ImageView foodPic;

    public NutrientDialog(Activity a, String productName, String productBarcode, String productImageURL, String energy, String carbohydrates,String sugars, String protein, String fats) {
        super(a);
        this.activity = a;
        this.productName = productName;
        this.productBarcode = productBarcode;
        this.productImageURL = productImageURL;
        this.energy = energy;
        this.carbs = carbohydrates;
        this.sugars = sugars;
        this.proteins = protein;
        this.fats = fats;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_nutrient_dialog);
        saveInfoButton = (Button) findViewById(R.id.saveInfoBtn);
        saveInfoButton.setOnClickListener(NutrientDialog.this);

        productNameText = (EditText) findViewById(R.id.productNameText);
        productBarcodeText = (TextView) findViewById(R.id.productBarcodeText);
        calorieText = (EditText) findViewById(R.id.calorieText);
        carboText = (EditText) findViewById(R.id.carboText);
        sugarText = (EditText) findViewById(R.id.sugarText);
        proteinText = (EditText) findViewById(R.id.proteinText);
        totFatText = (EditText) findViewById(R.id.totFatText);
        foodPic = (ImageView) findViewById(R.id.foodPic);

        productNameText.setText(productName);
        productBarcodeText.setText(productBarcode);
        calorieText.setText(energy);
        carboText.setText(carbs);
        sugarText.setText(sugars);
        proteinText.setText(proteins);
        totFatText.setText(fats);

        if(!productImageURL.equalsIgnoreCase("N/A"))
            Picasso.with(activity).load(productImageURL).into(foodPic);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.saveInfoBtn:

                productName = productNameText.getText().toString();
                productBarcode = productBarcodeText.getText().toString();
                energy = calorieText.getText().toString();
                carbs = carboText.getText().toString();
                sugars = sugarText.getText().toString();
                proteins = proteinText.getText().toString();
                fats = totFatText.getText().toString();

                try {
                    MainActivity mainActivity = (MainActivity)activity;
                    mainActivity.showNutInfoProgressBar();

                    uploadNutrientInfoToNode();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
        }
        dismiss();
    }

    private void uploadNutrientInfoToNode( ) throws IOException {
        if (isNetworkAvailable()) {

            MainActivity mainActivity = (MainActivity)activity;
            mainActivity.showNutInfoProgressBar();

            String userEmail;
            if(ParseUser.getCurrentUser() != null){
                userEmail = ParseUser.getCurrentUser().getEmail();
            }else {
                userEmail = "goku@dbz.com";
            }

            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
            String todaysDate = df.format(c.getTime());

            ParseObject nutrientInfo = new ParseObject("NutrientInfo");
            nutrientInfo.put("username", userEmail);
            nutrientInfo.put("productName", productName);
            nutrientInfo.put("productBarcode", productBarcode);
            nutrientInfo.put("energy", energy);
            nutrientInfo.put("carbohydrates", carbs);
            nutrientInfo.put("sugars", sugars);
            nutrientInfo.put("protein", proteins);
            nutrientInfo.put("fats", fats);
            nutrientInfo.put("imageURL", productImageURL);
            nutrientInfo.put("uploadDate", todaysDate);
            nutrientInfo.put("scansPerDay", 1);

            nutrientInfo.saveInBackground(new SaveCallback() {
                public void done(ParseException e) {

                    MainActivity mainActivity = (MainActivity)activity;
                    mainActivity.dismissNutInfoProgressBar();

                    Log.i("PARSE", e + "");

                    if (e == null) {
                        dismiss();
                        Toast.makeText(activity,"Nutrient Info Uploaded Yo!", Toast.LENGTH_SHORT).show();
                        mainActivity.callNutrientActivity(productBarcode);

                    } else {
                        dismiss();
                        Toast.makeText(activity,"Nutrient Info Upload Failed!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            //Use retrofit to upload nut info to Node.js server

            //NodeJsUploadHandler nodeJsUploadHandler = new NodeJsUploadHandler(activity);

           // nodeJsUploadHandler.uploadNutrientInfo(userEmail,productName ,productBarcode, energy, carbs, sugars, proteins, fats, productImageURL);
            //dismiss();

        } else {
            dismiss();
            Toast.makeText(activity,"Network Unavialable", Toast.LENGTH_SHORT).show();
        }
    }

    //function to scheck wheter network is available
    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        boolean isAvailable = false;

        if (networkInfo != null && networkInfo.isConnected()) {

            isAvailable = true;
        }
        return isAvailable;
    }

}
