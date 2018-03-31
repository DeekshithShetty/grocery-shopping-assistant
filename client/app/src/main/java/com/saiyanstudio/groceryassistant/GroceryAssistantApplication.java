package com.saiyanstudio.groceryassistant;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseUser;

/**
 * Created by deeks on 11/2/2015.
 */
public class GroceryAssistantApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

		/*
		 * Add Parse initialization code here
		 */

        // TODO: Add your own application ID and client key!
        //Parse.initialize(this, "APPLICATION_ID_GOES_HERE", "CLIENT_KEY_GOES_HERE");
        //Enable Local Datastore.
        Parse.enableLocalDatastore(this);

        Parse.initialize(this, "jSVbre0kUwZsqd0QBwlrvRuGPjVT4Vqi7n2y91EU", "mTY012m8ot3V18IMwGDWG3tUBefuem8t3pgdWuRd");

        ParseUser.enableRevocableSessionInBackground();

        ParseACL defaultACL = new ParseACL();

        // If you would like all objects to be private by default, remove this
        // line.
        defaultACL.setPublicReadAccess(true);

        ParseACL.setDefaultACL(defaultACL, true);
    }
}
