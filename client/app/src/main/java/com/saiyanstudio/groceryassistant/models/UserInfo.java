package com.saiyanstudio.groceryassistant.models;

/**
 * Created by deeks on 11/11/2015.
 */
public class UserInfo {

    private String infoKey;
    private String infoValue;

    public UserInfo(String infoKey, String infoValue){
        this.infoKey = infoKey;
        this.infoValue = infoValue;
    }

    public String getInfoKey() {
        return infoKey;
    }

    public String getInfoValue() {
        return infoValue;
    }

    public void setInfoKey(String infoKey) {
        this.infoKey = infoKey;
    }

    public void setInfoValue(String infoValue) {
        this.infoValue = infoValue;
    }
}
