package com.netease.nim.chatroom.zyly.entertainment.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.netease.nim.chatroom.zyly.R;
import com.netease.nim.chatroom.zyly.base.ui.TActivity;
import com.netease.nim.chatroom.zyly.base.util.StringUtil;
import com.netease.nim.chatroom.zyly.base.util.log.LogUtil;
import com.netease.nim.chatroom.zyly.config.DemoServers;
import com.netease.nim.chatroom.zyly.entertainment.constant.PushLinkConstant;
import com.netease.nim.chatroom.zyly.im.config.AuthPreferences;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.avchat.AVChatCallback;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.constant.AVChatType;
import com.netease.nimlib.sdk.avchat.model.AVChatChannelInfo;
import com.squareup.okhttp.Request;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by hzxuwen on 2016/7/12.
 */
public class LiveModeChooseActivity extends TActivity {

    private static final String TAG = "cyx";
    @Bind(R.id.video_live_layout)
    RelativeLayout videoLiveLayout;
    @Bind(R.id.audio_live_layout)
    RelativeLayout audioLiveLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_mode_choose_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.setLogo(R.drawable.actionbar_logo_white);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.actionbar_white_back_icon);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        });

        ButterKnife.bind(this);
    }

    @OnClick({R.id.video_live_layout, R.id.audio_live_layout})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.video_live_layout:
                masterEnterRoom(true);
                break;
            case R.id.audio_live_layout:
                masterEnterRoom(false);
                break;
        }
    }

    /**
     * 推流开始
     * */
    private void masterEnterRoom(final boolean isVideoMode) {
        Map<String, Object> ext = new HashMap<>();
        ext.put("type", isVideoMode ? AVChatType.VIDEO.getValue() : AVChatType.AUDIO.getValue());
        final String meetingName = StringUtil.get36UUID();
        ext.put(PushLinkConstant.meetingName, meetingName);
        JSONObject jsonObject = null;
        try {
            jsonObject = parseMap(ext);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e("cyx",jsonObject.toString()+"--");

        String url= DemoServers.masterEnterRoom_url+AuthPreferences.getUserRoomId()+"&meetingname="+ meetingName;
        OkHttpUtils.get().url(url).build().execute(new StringCallback() {
            @Override
            public void onError(Request request, Exception e) {
                Toast.makeText(LiveModeChooseActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onResponse(String response) {
                createChannel(isVideoMode, meetingName, AuthPreferences.getUserRoomId(), AuthPreferences.getUserPushUrl());
            }
        });

//        ChatRoomHttpClient.getInstance().masterEnterRoom(DemoCache.getAccount(), jsonObject.toString(), isVideoMode,
//                new ChatRoomHttpClient.ChatRoomHttpCallback<ChatRoomHttpClient.EnterRoomParam>() {
//                    @Override
//                    public void onSuccess(ChatRoomHttpClient.EnterRoomParam enterRoomParam) {
//                        Log.e("cyx",enterRoomParam.getExt()+"--"+enterRoomParam.getRoomId()+"--"+enterRoomParam.getUrl());
//                        createChannel(isVideoMode, meetingName, enterRoomParam.getRoomId(), enterRoomParam.getUrl());
//                    }
//
//                    @Override
//                    public void onFailed(int code, String errorMsg) {
//                        Toast.makeText(LiveModeChooseActivity.this, "创建直播间失败，code:" + code + ", errorMsg:" + errorMsg, Toast.LENGTH_SHORT).show();
//                    }
//                });
    }

    private void createChannel(final boolean isVideoMode, final String meetingName, final String roomId, final String url) {
        // 这里用uuid，作为多人通话房间的名称
        AVChatManager.getInstance().createRoom(meetingName, null, new AVChatCallback<AVChatChannelInfo>() {
            @Override
            public void onSuccess(AVChatChannelInfo avChatChannelInfo) {
                Toast.makeText(LiveModeChooseActivity.this, "登录直播间：" + roomId, Toast.LENGTH_SHORT).show();
                LiveActivity.start(LiveModeChooseActivity.this, meetingName, roomId, url, isVideoMode, true);
            }

            @Override
            public void onFailed(int i) {
                if (i == ResponseCode.RES_EEXIST) {
                    // 417表示该频道已经存在
                    LogUtil.e(TAG, "create room 417, enter room");
                    Toast.makeText(LiveModeChooseActivity.this, "登录直播间：" + roomId, Toast.LENGTH_SHORT).show();
                    LiveActivity.start(LiveModeChooseActivity.this, meetingName, roomId, url, isVideoMode, true);
                } else {
                    LogUtil.e(TAG, "create room failed, code:" + i);
                    Toast.makeText(LiveModeChooseActivity.this, "create room failed, code:" + i, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onException(Throwable throwable) {

            }
        });
    }

    private JSONObject parseMap(Map map) throws JSONException {
        if (map == null) {
            return null;
        }

        JSONObject obj = new JSONObject();
        Iterator entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            obj.put(key, value);
        }

        return obj;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }
}
