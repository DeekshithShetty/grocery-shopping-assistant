package com.saiyanstudio.groceryassistant.models;

/**
 * Created by deeks on 1/29/2016.
 */
public class RecipeItem {

    private String name;
    private String imageURL;
    private int usedIngredients;
    private int missedIngredients;
    private int likes;

    public RecipeItem(String name ,String imageURL,int likes, int usedIngredients, int missedIngredients){
        this.name = name;
        this.imageURL = imageURL;
        this.likes = likes;
        this.usedIngredients = usedIngredients;
        this.missedIngredients = missedIngredients;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public void setUsedIngredients(int usedIngredients) {
        this.usedIngredients = usedIngredients;
    }

    public void setMissedIngredients(int missedIngredients) {
        this.missedIngredients = missedIngredients;
    }

    public String getName() {
        return name;
    }

    public String getImageURL() {
        return imageURL;
    }

    public int getLikes() {
        return likes;
    }

    public int getUsedIngredients() {
        return usedIngredients;
    }

    public int getMissedIngredients() {
        return missedIngredients;
    }
}
