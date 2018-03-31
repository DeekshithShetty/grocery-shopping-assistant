package com.saiyanstudio.groceryassistant.models;

/**
 * Created by deeks on 11/5/2015.
 */
public class GroceryItem {
    private String barcode;
    private String name;
    private String imageURL;

    //calorie(energy) in kcal
    private float calorie;

    //nutrients per 100g(approx.) in g
    private float carbohydrate;
    private float sugar;
    private float protein;
    private float totalFat;
    private float saturatedFat;
    private float transFat;

    public GroceryItem(String barcode,String name,String imageURL){
        this.barcode = barcode;
        this.name = name;
        this.imageURL = imageURL;
    }

    public GroceryItem(String name,float calorie,String imageURL){
        this.name = name;
        this.calorie = calorie;
        this.imageURL = imageURL;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getBarcode() { return barcode;}

    public String getName() {
        return name;
    }

    public String getImageURL() {
        return imageURL;
    }


    //calories

    public void setCalorie(float calorie) {
        this.calorie = calorie;
    }

    public float getCalorie() {
        return calorie;
    }
}
