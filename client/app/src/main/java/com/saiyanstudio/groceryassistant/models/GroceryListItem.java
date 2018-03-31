package com.saiyanstudio.groceryassistant.models;

/**
 * Created by deeks on 1/29/2016.
 */
public class GroceryListItem implements Cloneable {

    private int id;
    private String name;
    private int isChecked;

    public GroceryListItem(){

    }

    public GroceryListItem(int id,String name, int isChecked){
        this.id = id;
        this.name = name;
        this.isChecked = isChecked;
    }

    public GroceryListItem(String name, int isChecked){
        this.name = name;
        this.isChecked = isChecked;
    }

    public Object clone() throws CloneNotSupportedException {
        GroceryListItem groceryListItemClone = (GroceryListItem)super.clone();
        return groceryListItemClone;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIsChecked(int isChecked) {
        this.isChecked = isChecked;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getIsChecked() {
        return isChecked;
    }
}
