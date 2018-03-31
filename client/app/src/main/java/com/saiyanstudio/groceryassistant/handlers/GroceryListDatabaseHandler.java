package com.saiyanstudio.groceryassistant.handlers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.saiyanstudio.groceryassistant.models.GroceryListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by deeks on 1/29/2016.
 */
public class GroceryListDatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "groceryListManager";

    private static final String TABLE_GROCERY_LIST = "groceryList";

    private static final String KEY_ID = "id";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_IS_CLEARED = "isCleared";
    private static final String KEY_GROCERY_NAME = "groceryName";
    private static final String KEY_IS_CHECKED = "groceryIsChecked";

    public GroceryListDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_LOCATIONS_TABLE = "CREATE TABLE " + TABLE_GROCERY_LIST + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_USER_EMAIL + " TEXT,"
                + KEY_IS_CLEARED + " INTEGER,"
                + KEY_GROCERY_NAME + " TEXT,"
                + KEY_IS_CHECKED + " INTEGER" + ")";
        db.execSQL(CREATE_LOCATIONS_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROCERY_LIST);

        // Create tables again
        onCreate(db);
    }

    // Adding new grocery list item
    public void addGroceryListItem(String userEmail ,GroceryListItem item) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_USER_EMAIL, userEmail);
        values.put(KEY_IS_CLEARED, 0);
        values.put(KEY_GROCERY_NAME, item.getName());
        values.put(KEY_IS_CHECKED, item.getIsChecked());

        // Inserting Row
        db.insert(TABLE_GROCERY_LIST, null, values);
        db.close(); // Closing database connection

        Log.d("GROCERYLIST", "added a new grocery item ...... ");
        //Log.d("GROCERYLIST", "grocery list of size " + getGroceryListSize(userEmail) + " for current user....");
    }


    // Getting single grocery List item
    public GroceryListItem getGroceryItem(String userEmail,String groceryName) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_GROCERY_LIST + " WHERE " + KEY_USER_EMAIL + " = \"" + userEmail + "\" AND " + KEY_GROCERY_NAME + " = \"" + groceryName + "\" AND " + KEY_IS_CLEARED + " = " + 0;

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor != null)
            cursor.moveToFirst();

        GroceryListItem groceryListItem = new GroceryListItem(Integer.parseInt(cursor.getString(0)),cursor.getString(3), cursor.getInt(4));

        return groceryListItem;
    }

    // Getting All grocery List item os the user
    public List<GroceryListItem> getGroceryList(String userEmail) {
        List<GroceryListItem> groceryList = new ArrayList<GroceryListItem>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_GROCERY_LIST + " WHERE " + KEY_USER_EMAIL + " = \"" + userEmail + "\" AND " + KEY_IS_CLEARED + " = " + 0;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                GroceryListItem groceryListItem = new GroceryListItem();
                groceryListItem.setId(Integer.parseInt(cursor.getString(0)));
                groceryListItem.setName(cursor.getString(3));
                groceryListItem.setIsChecked(cursor.getInt(4));
                groceryList.add(groceryListItem);
            } while (cursor.moveToNext());
        }

        return groceryList;
    }

    // Getting grocery list size os the user
    public int getGroceryListSize(String userEmail) {
        String countQuery = "SELECT  * FROM " + TABLE_GROCERY_LIST + " WHERE " + KEY_USER_EMAIL + " = \"" + userEmail + "\" AND " + KEY_IS_CLEARED + " = " + 0;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        return cursor.getCount();
    }

    // Updating  a grocery item of a user
    public void updateUserGroceryListItem(String userEmail, int isCleared , GroceryListItem groceryListItem) {

        String updateQuery = "UPDATE " + TABLE_GROCERY_LIST
                + " SET " + KEY_IS_CLEARED + " = " + isCleared + " WHERE " + KEY_USER_EMAIL + " = \""
                + userEmail + "\" AND " + KEY_GROCERY_NAME + " = \""
                + groceryListItem.getName() + "\" AND " + KEY_IS_CLEARED + " = " + 0;

        Log.i("GROCERY_ASSIST","update db called");
        Log.i("GROCERY_ASSIST","updateQuery : " + updateQuery);
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(updateQuery, null);
        cursor.moveToFirst();
        cursor.close();
        db.close();


    }

    // Updating  a grocery item isChecked of a user
    public void updateIsCheckedOfUserGroceryListItem(String userEmail, int isCleared , int newIsChecked, GroceryListItem groceryListItem) {

        String updateQuery = "UPDATE " + TABLE_GROCERY_LIST
                + " SET " + KEY_IS_CHECKED + " = " + newIsChecked + " WHERE " + KEY_USER_EMAIL + " = \""
                + userEmail + "\" AND " + KEY_GROCERY_NAME + " = \""
                + groceryListItem.getName() + "\"";

        Log.i("GROCERY_ASSIST","update db called");
        Log.i("GROCERY_ASSIST","updateQuery : " + updateQuery);
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(updateQuery, null);
        cursor.moveToFirst();
        cursor.close();
        db.close();
    }


    // Deleting a grocery list item
    public void deleteGroceryListItem(GroceryListItem groceryListItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GROCERY_LIST, KEY_ID + " = ?",
                new String[] { String.valueOf(groceryListItem.getId()) });
        db.close();
    }
}
