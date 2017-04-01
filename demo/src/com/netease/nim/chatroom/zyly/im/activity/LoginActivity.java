package com.netease.nim.chatroom.zyly.im.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.netease.nim.chatroom.zyly.DemoCache;
import com.netease.nim.chatroom.zyly.R;
import com.netease.nim.chatroom.zyly.base.ui.TActivity;
import com.netease.nim.chatroom.zyly.base.util.NetworkUtil;
import com.netease.nim.chatroom.zyly.base.util.string.MD5;
import com.netease.nim.chatroom.zyly.config.DemoServers;
import com.netease.nim.chatroom.zyly.im.config.AuthPreferences;
import com.netease.nim.chatroom.zyly.im.config.UserPreferences;
import com.netease.nim.chatroom.zyly.im.ui.dialog.DialogMaker;
import com.netease.nim.chatroom.zyly.im.ui.dialog.EasyAlertDialogHelper;
import com.netease.nim.chatroom.zyly.im.ui.widget.ClearableEditTextWithIcon;
import com.netease.nim.chatroom.zyly.inject.FlavorDependent;
import com.netease.nim.chatroom.zyly.zyly.HttpResponseBean;
import com.netease.nim.chatroom.zyly.zyly.LiveBean;
import com.netease.nim.chatroom.zyly.zyly.Utils;
import com.netease.nimlib.sdk.AbortableFuture;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.squareup.okhttp.Request;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

/**
 * Created by hzxuwen on 2016/2/24.
 */
public class LoginActivity extends TActivity {
    private static final String TAG = "cyx";
    private static final String KICK_OUT = "KICK_OUT";

    private Button loginBtn;
    private Button registerBtn;
    private TextView switchModeBtn;  // 注册/登录切换按钮

    private ClearableEditTextWithIcon loginAccountEdit;
    private ClearableEditTextWithIcon loginPasswordEdit;

    private ClearableEditTextWithIcon registerAccountEdit;
//    private ClearableEditTextWithIcon registerNickNameEdit;
    private ClearableEditTextWithIcon registerPasswordEdit;

    private View loginLayout;
    private View registerLayout;

    private AbortableFuture<LoginInfo> loginRequest;
    private boolean registerMode = false; // 注册模式
    private boolean registerPanelInited = false; // 注册面板是否初始化

    public static void start(Context context) {
        start(context, false);
    }

    public static void start(Context context, boolean kickOut) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(KICK_OUT, kickOut);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.setLogo(R.drawable.actionbar_logo_white);
        setSupportActionBar(toolbar);

        parseIntent();
        setupLoginPanel();
        setupRegisterPanel();
    }

    private void parseIntent() {
        boolean isKickOut = getIntent().getBooleanExtra(KICK_OUT, false);
        if (isKickOut) {
            EasyAlertDialogHelper.showOneButtonDiolag(LoginActivity.this,
                    getString(R.string.kickout_notify),
                    getString(R.string.kickout_content),
                    getString(R.string.ok), true, null);
        }
    }


    /**
     * 登录面板
     */
    private void setupLoginPanel() {
        loginAccountEdit = (ClearableEditTextWithIcon) findViewById(R.id.edit_login_account);
        loginPasswordEdit = (ClearableEditTextWithIcon) findViewById(R.id.edit_login_password);
        loginBtn = (Button) findViewById(R.id.done);

        loginAccountEdit.setIconResource(R.drawable.user_account_icon);
        loginPasswordEdit.setIconResource(R.drawable.user_pwd_lock_icon);

        loginAccountEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        loginPasswordEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        loginAccountEdit.addTextChangedListener(textWatcher);
        loginPasswordEdit.addTextChangedListener(textWatcher);

        String loginName = AuthPreferences.getUserLoginName();
        loginAccountEdit.setText(loginName);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url= DemoServers.URL_LOGIN+loginAccountEdit.getText().toString()+"&password="+ Utils.stringMD5(loginPasswordEdit.getText().toString());
                OkHttpUtils.get().url(url).build().execute(new StringCallback() {
                    @Override
                    public void onError(Request request, Exception e) {
                        Toast.makeText(LoginActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT)
                                .show();
                    }

                    @Override
                    public void onResponse(String response) {
                        Log.e("cyx",response);
                        HttpResponseBean responseBean= JSON.parseObject(response,HttpResponseBean.class);
                        if(responseBean.getErrcode()!=null){   //与自己的服务器登录失败
                            Toast.makeText(LoginActivity.this,responseBean.getErrmessage(),Toast.LENGTH_SHORT).show();
                        }else{
                            LiveBean liveBean=JSON.parseObject(response,LiveBean.class);
                            login(liveBean.getAccid(),liveBean.getToken(),liveBean.getPhonenumber()
                                    ,liveBean.getHttppullurl(),liveBean.getPushurl(),liveBean.getRoomid());
                        }
                    }
                });
            }
        });
    }

    /**
     * 注册面板
     */
    private void setupRegisterPanel() {
        loginLayout = findViewById(R.id.login_layout);
        registerLayout = findViewById(R.id.register_layout);
        switchModeBtn = (TextView) findViewById(R.id.register_login_tip);
        registerBtn = (Button) findViewById(R.id.register_btn);

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

        switchModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode();
            }
        });
    }

    private TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            // 更新右上角按钮状态
            if (!registerMode) {
                // 登录模式
                boolean isEnable = loginAccountEdit.getText().length() > 0
                        && loginPasswordEdit.getText().length() > 0;
                updateBtn(loginBtn, isEnable);
            }
        }
    };

    private void updateBtn(TextView loginBtn, boolean isEnable) {
        loginBtn.setBackgroundResource(R.drawable.g_white_btn_selector);
        loginBtn.setEnabled(isEnable);
        loginBtn.setTextColor(getResources().getColor(R.color.color_blue_0888ff));
    }

    /**
     * ***************************************** 登录 **************************************
     */

    private void login(final String account,final String token,final String loginName,
                       final String pullUrl,final String pushUrl,final String roomId ) {
        DialogMaker.showProgressDialog(this, null, getString(R.string.logining), true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (loginRequest != null) {
                    loginRequest.abort();
                    onLoginDone();
                }
            }
        }).setCanceledOnTouchOutside(false);
//
//        final String account = loginAccountEdit.getEditableText().toString().toLowerCase();
//        final String token = tokenFromPassword(loginPasswordEdit.getEditableText().toString());

        // 登录
        loginRequest = NIMClient.getService(AuthService.class).login(new LoginInfo(account, token));
        loginRequest.setCallback(new RequestCallback<LoginInfo>() {
            @Override
            public void onSuccess(LoginInfo param) {
                Log.i(TAG, "login success");

                onLoginDone();
                DemoCache.setAccount(account);
                saveLoginInfo(account, token,loginName,pullUrl,pushUrl,roomId);

                // 初始化消息提醒
                NIMClient.toggleNotification(UserPreferences.getNotificationToggle());

                // 初始化免打扰
                NIMClient.updateStatusBarNotificationConfig(UserPreferences.getStatusConfig());

                // 进入主界面
                startActivity(new Intent(LoginActivity.this, FlavorDependent.getInstance().getMainClass()));
                finish();
            }

            @Override
            public void onFailed(int code) {
                onLoginDone();
                if (code == 302 || code == 404) {
                    Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "登录失败: " + code, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onException(Throwable exception) {
                onLoginDone();
            }
        });
    }


    private void onLoginDone() {
        loginRequest = null;
        DialogMaker.dismissProgressDialog();
    }

    private void saveLoginInfo(final String account,final String token,final String loginName,
                               final String pullUrl,final String pushUrl,final String roomId ) {
        AuthPreferences.saveUserAccount(account);
        AuthPreferences.saveUserToken(token);
        AuthPreferences.saveUserLoginName(loginName);
        AuthPreferences.saveUserPullUrl(pullUrl);
        AuthPreferences.saveUserPushUrl(pushUrl);
        AuthPreferences.saveUserRoomId(roomId);
    }

    //DEMO中使用 username 作为 NIM 的account ，md5(password) 作为 token
    //开发者需要根据自己的实际情况配置自身用户系统和 NIM 用户系统的关系
    private String tokenFromPassword(String password) {
        return MD5.getStringMD5(password);
    }

    /**
     * ***************************************** 注册 **************************************
     */

    private void register() {
        if (!registerMode || !registerPanelInited) {
            return;
        }

        if (!checkRegisterContentValid(true)) {
            return;
        }

        if (!NetworkUtil.isNetAvailable(LoginActivity.this)) {
            Toast.makeText(LoginActivity.this, R.string.network_is_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        DialogMaker.showProgressDialog(this, getString(R.string.registering), false);

        // 注册流程
        final String account = registerAccountEdit.getText().toString();
//        final String nickName = registerNickNameEdit.getText().toString();
        final String password = registerPasswordEdit.getText().toString();

        String url= DemoServers.URL_REGIST+account+"&password="+ Utils.stringMD5(password);
        OkHttpUtils.get().url(url).build().execute(new StringCallback() {
            @Override
            public void onError(Request request, Exception e) {
                Toast.makeText(LoginActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT)
                        .show();

                DialogMaker.dismissProgressDialog();
            }

            @Override
            public void onResponse(String response) {
                HttpResponseBean responseBean= JSON.parseObject(response,HttpResponseBean.class);
                Log.e("cyx",response+"-");
                if (responseBean.getErrcode().equals("0")) {
                    Toast.makeText(LoginActivity.this, R.string.register_success, Toast.LENGTH_SHORT).show();
                    switchMode();  // 切换回登录
                    loginAccountEdit.setText(account);
                    loginPasswordEdit.setText(password);

                    registerAccountEdit.setText("");
//                    registerNickNameEdit.setText("");
                    registerPasswordEdit.setText("");
                }else{
                    Toast.makeText(LoginActivity.this,responseBean.getErrmessage(),Toast.LENGTH_SHORT).show();
                }
                DialogMaker.dismissProgressDialog();
            }
        });

//        RegisterHttpClient.getInstance().register(account, nickName, password, new RegisterHttpClient.ContactHttpCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
//                Toast.makeText(LoginActivity.this, R.string.register_success, Toast.LENGTH_SHORT).show();
//                switchMode();  // 切换回登录
//                loginAccountEdit.setText(account);
//                loginPasswordEdit.setText(password);
//
//                registerAccountEdit.setText("");
//                registerNickNameEdit.setText("");
//                registerPasswordEdit.setText("");
//
//                DialogMaker.dismissProgressDialog();
//            }
//
//            @Override
//            public void onFailed(int code, String errorMsg) {
//                Toast.makeText(LoginActivity.this, getString(R.string.register_failed, code, errorMsg), Toast.LENGTH_SHORT)
//                        .show();
//
//                DialogMaker.dismissProgressDialog();
//            }
//        });
    }

    private boolean checkRegisterContentValid(boolean tipError) {
        if (!registerMode || !registerPanelInited) {
            return false;
        }

        // 帐号检查
        if (registerAccountEdit.length() <= 0 || registerAccountEdit.length() > 20) {
            if (tipError) {
                Toast.makeText(this, R.string.register_account_tip, Toast.LENGTH_SHORT).show();
            }

            return false;
        }

//        // 昵称检查
//        if (registerNickNameEdit.length() <= 0 || registerNickNameEdit.length() > 10
//                || registerNickNameEdit.getText().toString().trim().isEmpty()) {
//            if (tipError) {
//                Toast.makeText(this, R.string.register_nick_name_tip, Toast.LENGTH_SHORT).show();
//            }
//
//            return false;
//        }

        // 密码检查
        if (registerPasswordEdit.length() < 6 || registerPasswordEdit.length() > 20) {
            if (tipError) {
                Toast.makeText(this, R.string.register_password_tip, Toast.LENGTH_SHORT).show();
            }

            return false;
        }

        return true;
    }

    /**
     * ***************************************** 注册/登录切换 **************************************
     */
    private void switchMode() {
        registerMode = !registerMode;

        if (registerMode && !registerPanelInited) {
            registerAccountEdit = (ClearableEditTextWithIcon) findViewById(R.id.edit_register_account);
//            registerNickNameEdit = (ClearableEditTextWithIcon) findViewById(R.id.edit_register_nickname);
            registerPasswordEdit = (ClearableEditTextWithIcon) findViewById(R.id.edit_register_password);

            registerAccountEdit.setIconResource(R.drawable.user_account_icon);
//            registerNickNameEdit.setIconResource(R.drawable.nick_name_icon);
            registerPasswordEdit.setIconResource(R.drawable.user_pwd_lock_icon);

            registerAccountEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
//            registerNickNameEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
            registerPasswordEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

            registerAccountEdit.addTextChangedListener(textWatcher);
//            registerNickNameEdit.addTextChangedListener(textWatcher);
            registerPasswordEdit.addTextChangedListener(textWatcher);

            registerPanelInited = true;
        }

        setTitle(registerMode ? R.string.register : R.string.login);
        loginLayout.setVisibility(registerMode ? View.GONE : View.VISIBLE);
        registerLayout.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        switchModeBtn.setText(registerMode ? R.string.login_has_account : R.string.register);
        if (registerMode) {
            updateBtn(registerBtn, true);
        } else {
            boolean isEnable = loginAccountEdit.getText().length() > 0
                    && loginPasswordEdit.getText().length() > 0;
            updateBtn(registerBtn, isEnable);
        }
    }
}
