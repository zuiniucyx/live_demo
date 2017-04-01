package com.netease.nim.chatroom.zyly.im.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.netease.nim.chatroom.zyly.DemoCache;

/**
 * Created by hzxuwen on 2015/4/13.
 */
public class AuthPreferences {
    private static final String KEY_USER_ACCOUNT = "account";
    private static final String KEY_USER_TOKEN = "token";
    private static final String KEY_USER_LOGINNAME = "loginname";
    private static final String KEY_USER_PULLURL = "pullurl";
    private static final String KEY_USER_PUSHURL = "pushurl";
    private static final String KEY_USER_ROOMID = "roomid";

    public static void saveUserLoginName(String loginName) {
        saveString(KEY_USER_LOGINNAME, loginName);
    }
    public static String getUserLoginName() {
        return getString(KEY_USER_LOGINNAME);
    }

    public static void saveUserPullUrl(String pullUrl) {
        saveString(KEY_USER_PULLURL, pullUrl);
    }
    public static String getUserPullUrl() {
        return getString(KEY_USER_PULLURL);
    }

    public static void saveUserPushUrl(String pushUrl) {
        saveString(KEY_USER_PUSHURL, pushUrl);
    }
    public static String getUserPushUrl() {
        return getString(KEY_USER_PUSHURL);
    }

    public static void saveUserRoomId(String roomId) {
        saveString(KEY_USER_ROOMID, roomId);
    }
    public static String getUserRoomId() {
        return getString(KEY_USER_ROOMID);
    }

    public static void saveUserAccount(String account) {
        saveString(KEY_USER_ACCOUNT, account);
    }
    public static String getUserAccount() {
        return getString(KEY_USER_ACCOUNT);
    }

    public static void saveUserToken(String token) {
        saveString(KEY_USER_TOKEN, token);
    }
    public static String getUserToken() {
        return getString(KEY_USER_TOKEN);
    }

    private static void saveString(String key, String value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(key, value);
        editor.commit();
    }

    private static String getString(String key) {
        return getSharedPreferences().getString(key, null);
    }

    static SharedPreferences getSharedPreferences() {
        return DemoCache.getContext().getSharedPreferences("zylyLive", Context.MODE_PRIVATE);
    }
}
