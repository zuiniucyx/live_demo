package com.netease.nim.chatroom.zyly.entertainment.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netease.neliveplayer.NELivePlayer;
import com.netease.nim.chatroom.zyly.DemoCache;
import com.netease.nim.chatroom.zyly.R;
import com.netease.nim.chatroom.zyly.base.util.log.LogUtil;
import com.netease.nim.chatroom.zyly.config.DemoServers;
import com.netease.nim.chatroom.zyly.entertainment.constant.LiveType;
import com.netease.nim.chatroom.zyly.entertainment.constant.MicApplyEnum;
import com.netease.nim.chatroom.zyly.entertainment.constant.MicStateEnum;
import com.netease.nim.chatroom.zyly.entertainment.constant.PushLinkConstant;
import com.netease.nim.chatroom.zyly.entertainment.constant.PushMicNotificationType;
import com.netease.nim.chatroom.zyly.entertainment.helper.ChatRoomMemberCache;
import com.netease.nim.chatroom.zyly.entertainment.helper.MicHelper;
import com.netease.nim.chatroom.zyly.entertainment.helper.SimpleCallback;
import com.netease.nim.chatroom.zyly.entertainment.module.ConnectedAttachment;
import com.netease.nim.chatroom.zyly.im.config.AuthPreferences;
import com.netease.nim.chatroom.zyly.im.config.UserPreferences;
import com.netease.nim.chatroom.zyly.im.ui.dialog.EasyAlertDialogHelper;
import com.netease.nim.chatroom.zyly.permission.MPermission;
import com.netease.nim.chatroom.zyly.permission.annotation.OnMPermissionDenied;
import com.netease.nim.chatroom.zyly.permission.annotation.OnMPermissionGranted;
import com.netease.nim.chatroom.zyly.permission.annotation.OnMPermissionNeverAskAgain;
import com.netease.nim.chatroom.zyly.permission.util.MPermissionUtil;
import com.netease.nim.chatroom.zyly.thirdparty.video.NEVideoView;
import com.netease.nim.chatroom.zyly.thirdparty.video.VideoPlayer;
import com.netease.nim.chatroom.zyly.thirdparty.video.constant.VideoConstant;
import com.netease.nim.chatroom.zyly.zyly.LiveBean;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.avchat.AVChatCallback;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.constant.AVChatImageFormat;
import com.netease.nimlib.sdk.avchat.constant.AVChatResCode;
import com.netease.nimlib.sdk.avchat.constant.AVChatType;
import com.netease.nimlib.sdk.avchat.constant.AVChatVideoScalingType;
import com.netease.nimlib.sdk.avchat.model.AVChatAudioFrame;
import com.netease.nimlib.sdk.avchat.model.AVChatOptionalConfig;
import com.netease.nimlib.sdk.avchat.model.AVChatVideoFrame;
import com.netease.nimlib.sdk.avchat.model.AVChatVideoRender;
import com.netease.nimlib.sdk.chatroom.ChatRoomService;
import com.netease.nimlib.sdk.chatroom.model.ChatRoomMember;
import com.netease.nimlib.sdk.chatroom.model.ChatRoomMessage;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.CustomNotification;
import com.netease.nimlib.sdk.util.Entry;
import com.netease.nrtc.effect.video.GPUImage;
import com.netease.nrtc.effect.video.GPUImageBilateralFilter;
import com.netease.nrtc.effect.video.GPUImageBrightnessFilter;
import com.netease.nrtc.effect.video.GPUImageContrastFilter;
import com.netease.nrtc.effect.video.GPUImageSaturationFilter;
import com.squareup.okhttp.Request;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 观众端
 * Created by hzxuwen on 2016/3/18.
 */
public class AudienceActivity extends LivePlayerBaseActivity implements VideoPlayer.VideoPlayerProxy{
    private static final String TAG = "cyx";
    private final int BASIC_PERMISSION_REQUEST_CODE = 110;

    // view
    private AVChatVideoRender videoRender;
    private NEVideoView videoView;
    private View closeBtn;
//    private ImageButton likeBtn;
    private ViewGroup liveFinishLayout;
    private View liveFinishBtn;
    private TextView finishTipText;
    private TextView finishNameText;
    private TextView preparedText;
//    private Button sendGiftBtn;
//    private ImageButton switchBtn;
    private ViewGroup roomOwnerLayout; // master名称布局
//    private ViewGroup inputLayout; // 输入框布局
    /**
     * 互动
     **/
    private ViewGroup interationLayout; // 互动根布局
    private ViewGroup interationInitLayout; // 互动初始布局
    private Button videoLinkBtn; // 视频连接按钮
    private Button audioLinkBtn; // 音频链接按钮
    private ViewGroup applyingLayout; // 正在进行互动申请布局
    private TextView applyingTip; //  正在进行连线的文案
    private Button cancelLinkBtn; // 取消互动申请按钮
//    private ViewGroup audioInteractInitLayout; // 音频模式的互动初始布局
//    private Button audioInteractionLinkBtn; // 音频模式的音频连接按钮
    private TextView applyMasterNameText; // 主播名称text

    // 播放器
    private VideoPlayer videoPlayer;
    // 发送礼物频率控制使用
    private long lastClickTime = 0;
    // 选中的礼物
    private int giftPosition = -1;
    // 申请连麦的模式
    private MicApplyEnum micApplyEnum;

    // state
    private boolean isStartLive = false; // 推流是否开始
    private boolean isMyVideoLink = true; // 观众连麦模式，默认视频
    private boolean isMyAlreadyApply = false; // 我是否已经申请连麦
    private boolean isAgreeToLink = false; // 主播是否同意我连麦（为了确保权限时使用）

    public static void start(Context context, String roomId) {
        Intent intent = new Intent();
        intent.setClass(context, AudienceActivity.class);
        intent.putExtra(EXTRA_ROOM_ID, roomId);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findViews();
        updateRoomUI(true);
        requestBasicPermission(); // 申请APP基本权限.同意之后，请求拉流

        registerAudienceObservers(true);
    }

    @Override
    protected int getActivityLayout() {
        return R.layout.audience_activity;
    }

    @Override
    protected int getLayoutId() {
        return R.id.audience_layout;
    }

    @Override
    protected void initParam() {

    }

    @Override
    protected void onResume() {
        super.onResume();

        // 恢复播放
        if (videoPlayer != null) {
            videoPlayer.onActivityResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        // 释放资源
        if (videoPlayer != null) {
            videoPlayer.resetVideo();
        }
        if (mGPUEffect != null) {
            mGPUEffect.dispose();
        }
        doLeaveAVChatRoom();
        registerAudienceObservers(false);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishLive();
    }

    private void finishLive() {
        if (isStartLive) {
            logoutChatRoom();
        } else {
            NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
            clearChatRoom();
        }
    }

    // 离开聊天室
    private void logoutChatRoom() {
        EasyAlertDialogHelper.createOkCancelDiolag(this, null, getString(R.string.finish_confirm),
                getString(R.string.confirm), getString(R.string.cancel), true,
                new EasyAlertDialogHelper.OnDialogActionListener() {
                    @Override
                    public void doCancelAction() {

                    }

                    @Override
                    public void doOkAction() {
                        NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
                        clearChatRoom();
                    }
                }).show();
    }

    private void clearChatRoom() {
        doLeaveAVChatRoom();
        ChatRoomMemberCache.getInstance().clearRoomCache(roomId);
        finish();
    }

    @Override
    protected void onConnected() {

    }

    @Override
    protected void onDisconnected() {

    }

    /********************************
     * 初始化     拉流cyx
     *******************************/

    private void fetchLiveUrl() {
        Log.e("cyx","roomid="+roomId);
        String urls= DemoServers.pull_url+roomId;
        OkHttpUtils.get().url(urls).build().execute(new StringCallback() {
            @Override
            public void onError(Request request, Exception e) {
                Toast.makeText(AudienceActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onResponse(String response) {
                LiveBean liveBean= JSON.parseObject(response,LiveBean.class);
                url=liveBean.getHttppullurl();
                meetingName=liveBean.getMeetingname();
                initAudienceParam();
                liveType = LiveType.VIDEO_TYPE;
            }
        });

//                url = "http://flv8f1cb7c0.live.126.net/live/870c0f45db9b4613997202ded07184a2.flv?netease=flv8f1cb7c0.live.126.net";

//        ChatRoomHttpClient.getInstance().studentEnterRoom(DemoCache.getAccount(), roomId, new ChatRoomHttpClient.ChatRoomHttpCallback<ChatRoomHttpClient.EnterRoomParam>() {
//            @Override
//            public void onSuccess(ChatRoomHttpClient.EnterRoomParam enterRoomParam) {
//                url = enterRoomParam.getUrl();
//                Log.e("cyx",url+"-");
//                if (enterRoomParam.getExt().equals("AUDIO")) {
//                    liveType = liveType.AUDIO_TYPE;
//                } else if (enterRoomParam.getExt().equals("VIDEO")) {
//                    liveType = LiveType.VIDEO_TYPE;
//                }
//                initAudienceParam();
//            }
//
//            @Override
//            public void onFailed(int code, String errorMsg) {
//                if (code == -1) {
//                    Toast.makeText(AudienceActivity.this, "无法连接服务器, code=" + code, Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(AudienceActivity.this, "观众进入房间失败, code=" + code + ", errorMsg:" + errorMsg, Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
    }

    private void initAudienceParam() {
        // 初始化美颜参数
        mGPUEffect = GPUImage.create(this);
        mGPUEffect.setFilter(new GPUImageBilateralFilter(30f));
        mGPUEffect.setFilter(new GPUImageContrastFilter(1.5f));
        mGPUEffect.setFilter(new GPUImageSaturationFilter(1.4f));
        mGPUEffect.setFilter(new GPUImageBrightnessFilter(0.1f));

        // 初始化拉流
        videoView = findView(R.id.video_view);
        videoRender.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
        videoPlayer = new VideoPlayer(AudienceActivity.this, videoView, null, url,
                UserPreferences.getPlayerStrategy(), this, VideoConstant.VIDEO_SCALING_MODE_FILL_SCALE);

        videoPlayer.openVideo();
    }

    protected void findViews() {
        super.findViews();

        rootView = findView(R.id.audience_layout);
        videoRender = findView(R.id.video_render);
        videoRender.setZOrderMediaOverlay(false);
        videoRender.setVisibility(View.GONE);
        closeBtn = findView(R.id.close_btn);
        roomOwnerLayout = findView(R.id.room_owner_layout);
        controlLayout = findView(R.id.control_layout);
//        inputLayout = findView(R.id.messageActivityBottomLayout);
        interactionBtn = findView(R.id.interaction_btn);
//        interactionBtn.setVisibility(View.GONE);
//        likeBtn = findView(R.id.like_btn);
//        switchBtn = findView(R.id.switch_btn);
        beautyBtn = findView(R.id.beauty_btn);

        closeBtn.setOnClickListener(buttonClickListener);
        interactionBtn.setOnClickListener(buttonClickListener);
//        giftBtn.setOnClickListener(buttonClickListener);
//        likeBtn.setOnClickListener(buttonClickListener);
//        switchBtn.setOnClickListener(buttonClickListener);
        beautyBtn.setOnClickListener(buttonClickListener);

        // 互动
        interationLayout = findView(R.id.audience_interaction_layout);
        interationInitLayout = findView(R.id.init_layout);
        videoLinkBtn = findView(R.id.member_link_btn);
        audioLinkBtn = findView(R.id.audio_link_btn);
        applyingLayout = findView(R.id.applying_layout);
        applyingTip = findView(R.id.applying_tip);
        cancelLinkBtn = findView(R.id.cancel_link_btn);
//        audioInteractInitLayout = findView(R.id.audio_mode_init_layout);
//        audioInteractionLinkBtn = findView(R.id.audio_mode_link);
        applyMasterNameText = findView(R.id.apply_master_name);

        interationLayout.setOnClickListener(buttonClickListener);
        videoLinkBtn.setOnClickListener(buttonClickListener);
        audioLinkBtn.setOnClickListener(buttonClickListener);
        cancelLinkBtn.setOnClickListener(buttonClickListener);
//        audioInteractionLinkBtn.setOnClickListener(buttonClickListener);

        // 直播结束布局
        liveFinishLayout = findView(R.id.live_finish_layout);
        liveFinishBtn = findView(R.id.finish_close_btn);
        finishTipText = findView(R.id.finish_tip_text);
        finishNameText = findView(R.id.finish_master_name);
        finishTipText.setText(R.string.loading);

        liveFinishBtn.setOnClickListener(buttonClickListener);

        preparedText = findView(R.id.prepared_text);
    }

//    // 初始化礼物布局
//    protected void findGiftLayout() {
//        super.findGiftLayout();
//        sendGiftBtn = findView(R.id.send_gift_btn);
//        sendGiftBtn.setOnClickListener(buttonClickListener);
//
//        adapter = new GiftAdapter(this);
//        giftView.setAdapter(adapter);
//
//        giftLayout.setOnClickListener(buttonClickListener);
//        giftView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                giftPosition = position;
//            }
//        });
//
//    }

    protected void updateRoomUI(boolean isHide) {
        if (isHide) {
            controlLayout.setVisibility(View.GONE);
            roomOwnerLayout.setVisibility(View.GONE);
//            inputLayout.setVisibility(View.GONE);
        } else {
            controlLayout.setVisibility(View.VISIBLE);
            roomOwnerLayout.setVisibility(View.VISIBLE);
//            inputLayout.setVisibility(View.VISIBLE);
        }
    }

    protected void updateUI() {
        super.updateUI();
        ChatRoomMemberCache.getInstance().fetchMember(roomId, roomInfo.getCreator(), new SimpleCallback<ChatRoomMember>() {
            @Override
            public void onResult(boolean success, ChatRoomMember result) {
                if (success) {
                    masterNick = result.getNick();
                    String nick = TextUtils.isEmpty(masterNick) ? AuthPreferences.getUserLoginName() : masterNick;
                    masterNameText.setText(nick);
                    finishNameText.setText(nick);
                }
            }
        });
    }

    private void registerAudienceObservers(boolean register) {
        AVChatManager.getInstance().observeAVChatState(this, register);
    }

    private View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.close_btn:
                    finishLive();
                    break;
                case R.id.finish_close_btn:
                    NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
                    clearChatRoom();
                    break;
                case R.id.interaction_btn:
                    showInteractionLayout();
                    break;
//                case R.id.gift_btn:
//                    showGiftLayout();
//                    break;
//                case R.id.like_btn:
//                    periscopeLayout.addHeart();
//                    sendLike();
//                    break;
                case R.id.switch_btn:
                    AVChatManager.getInstance().switchCamera();
                    break;
                case R.id.beauty_btn:
                    openCloseBeauty();
                    break;
//                case R.id.gift_layout:
//                    giftLayout.setVisibility(View.GONE);
//                    giftPosition = -1;
//                    break;
//                case R.id.send_gift_btn:
//                    sendGift();
//                    break;
                case R.id.audience_interaction_layout:
                    interationLayout.setVisibility(View.GONE);
                    break;
                case R.id.member_link_btn:
                    micApplyEnum = MicApplyEnum.VIDEO_VIDEO;
                    requestLivePermission();
                    break;
                case R.id.audio_link_btn:
                    micApplyEnum = MicApplyEnum.VIDEO_AUDIO;
                    requestLivePermission();
                    break;
                case R.id.cancel_link_btn:
                    cancelLinking();
                    break;
//                case R.id.audio_mode_link:
//                    micApplyEnum = MicApplyEnum.AUDIO;
//                    requestLivePermission();
//                    break;
            }
        }
    };

    /*************************
     * 点赞爱心
     ********************************/

//    // 发送点赞爱心
//    private void sendLike() {
//        if (!isFastClick()) {
//            LikeAttachment attachment = new LikeAttachment();
//            ChatRoomMessage message = ChatRoomMessageBuilder.createChatRoomCustomMessage(roomId, attachment);
//            setMemberType(message);
//            NIMClient.getService(ChatRoomService.class).sendMessage(message, false);
//        }
//    }

//    // 发送爱心频率控制
//    private boolean isFastClick() {
//        long currentTime = System.currentTimeMillis();
//        long time = currentTime - lastClickTime;
//        if (time > 0 && time < 1000) {
//            return true;
//        }
//        lastClickTime = currentTime;
//        return false;
//    }

    /***********************
     * 收发礼物
     ******************************/

    // 显示礼物列表
//    private void showGiftLayout() {
//        giftLayout.setVisibility(View.VISIBLE);
//        inputPanel.collapse(true);
//    }

    // 发送礼物
//    private void sendGift() {
//        if (giftPosition == -1) {
//            Toast.makeText(AudienceActivity.this, "请选择礼物", Toast.LENGTH_SHORT).show();
//            return;
//        }
////        giftLayout.setVisibility(View.GONE);
//        GiftAttachment attachment = new GiftAttachment(GiftType.typeOfValue(giftPosition), 1);
//        ChatRoomMessage message = ChatRoomMessageBuilder.createChatRoomCustomMessage(roomId, attachment);
//        setMemberType(message);
//        NIMClient.getService(ChatRoomService.class).sendMessage(message, false);
//        giftAnimation.showGiftAnimation(message);
//        giftPosition = -1; // 发送完毕，置空
//    }

    private void setMemberType(ChatRoomMessage message) {
        Map<String, Object> ext = new HashMap<>();
        ChatRoomMember chatRoomMember = ChatRoomMemberCache.getInstance().getChatRoomMember(roomId, DemoCache.getAccount());
        if (chatRoomMember != null && chatRoomMember.getMemberType() != null) {
            ext.put("type", chatRoomMember.getMemberType().getValue());
            message.setRemoteExtension(ext);
        }
    }


    /*******************************
     * 互动连麦
     **************************************/

    /**
     * 基本权限管理
     */
    private void requestBasicPermission() {
        MPermission.with(AudienceActivity.this)
                .addRequestCode(BASIC_PERMISSION_REQUEST_CODE)
                .permissions(
                        Manifest.permission.READ_PHONE_STATE)
                .request();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @OnMPermissionGranted(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionSuccess() {
        fetchLiveUrl();
        Log.e("cyx","onBasicPermissionSuccess");
    }

    @OnMPermissionDenied(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionFailed() {
        finish();
        Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
    }

    @OnMPermissionGranted(LIVE_PERMISSION_REQUEST_CODE)
    public void onLivePermissionGranted() {
        Log.e("cyx","onLivePermissionGranted");
        Toast.makeText(AudienceActivity.this, "授权成功", Toast.LENGTH_SHORT).show();
        if (isAgreeToLink) {
            doMicLinking();
        } else {
            if (micApplyEnum == MicApplyEnum.VIDEO_VIDEO) {
                doVideoLink();
            } else if (micApplyEnum == MicApplyEnum.VIDEO_AUDIO) {
                doAudioLink();
            } else {
                doAudioModeLink();
            }
        }
    }

    @OnMPermissionDenied(LIVE_PERMISSION_REQUEST_CODE)
    public void onLivePermissionDenied() {
        List<String> deniedPermissions = MPermission.getDeniedPermissions(this, LIVE_PERMISSIONS);
        String tip = "您拒绝了权限" + MPermissionUtil.toString(deniedPermissions) + "，无法开启直播";
        Toast.makeText(AudienceActivity.this, tip, Toast.LENGTH_SHORT).show();
        if (isAgreeToLink) {
            LogUtil.d(TAG, "permission denied, send reject");
            MicHelper.getInstance().sendCustomNotify(roomId, roomInfo.getCreator(), PushMicNotificationType.REJECT_CONNECTING.getValue(), null, true);
        }
    }

    @OnMPermissionNeverAskAgain(LIVE_PERMISSION_REQUEST_CODE)
    public void onLivePermissionDeniedAsNeverAskAgain() {
        List<String> deniedPermissions = MPermission.getDeniedPermissionsWithoutNeverAskAgain(this, LIVE_PERMISSIONS);
        List<String> neverAskAgainPermission = MPermission.getNeverAskAgainPermissions(this, LIVE_PERMISSIONS);
        StringBuilder sb = new StringBuilder();
        sb.append("无法开启直播，请到系统设置页面开启权限");
        sb.append(MPermissionUtil.toString(neverAskAgainPermission));
        if(deniedPermissions != null && !deniedPermissions.isEmpty()) {
            sb.append(",下次询问请授予权限");
            sb.append(MPermissionUtil.toString(deniedPermissions));
        }

        Toast.makeText(AudienceActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
        if (isAgreeToLink) {
            LogUtil.d(TAG, "permission denied as never ask again, send reject");
            MicHelper.getInstance().sendCustomNotify(roomId, roomInfo.getCreator(), PushMicNotificationType.REJECT_CONNECTING.getValue(), null, true);
        }
    }

    /******************** fetch mic list *************************/

    private void fetchMicList() {
        NIMClient.getService(ChatRoomService.class).fetchQueue(roomId).setCallback(new RequestCallback<List<Entry<String, String>>>() {
            @Override
            public void onSuccess(List<Entry<String, String>> entries) {
                showOnMicMember(entries);
            }

            @Override
            public void onFailed(int i) {

            }

            @Override
            public void onException(Throwable throwable) {

            }
        });
    }

    // 普通观众显示连麦者昵称
    private void showOnMicMember(List<Entry<String, String>> entries) {
        boolean isShowNick = false;
        for (Entry<String, String> entry : entries) {
            String ext = entry.value;
            String nick = null;
            MicStateEnum micStateEnum = null;

            try {
                JSONObject jsonObject = JSONObject.parseObject(ext);
                if (jsonObject != null) {
                    JSONObject info = (JSONObject) jsonObject.get(PushLinkConstant.info);
                    nick = info.getString(PushLinkConstant.nick);
                    micStateEnum = MicStateEnum.typeOfValue(jsonObject.getIntValue(PushLinkConstant.state));
                    style = jsonObject.getInteger(PushLinkConstant.style);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (micStateEnum == MicStateEnum.CONNECTED) {
                updateOnMicName(nick);
                showConnectionView(null, nick, style);
                isShowNick = true;
            }
        }

        if (!isShowNick) {
            onMicNameText.setVisibility(View.GONE);
        }
    }

    /********************** 观众连麦请求 ***************************/

    // 显示连麦布局
    private void showInteractionLayout() {
        interationLayout.setVisibility(View.VISIBLE);
//        if (liveType == LiveType.VIDEO_TYPE) {
            interationInitLayout.setVisibility(View.VISIBLE);
//            audioInteractInitLayout.setVisibility(View.GONE);
//        } else if (liveType == LiveType.AUDIO_TYPE) {
//            interationInitLayout.setVisibility(View.GONE);
//            audioInteractInitLayout.setVisibility(View.VISIBLE);
//        }
    }

    // 申请视频连接
    private void doVideoLink() {
        showPushLinkLayout(true, R.string.video_applying);
        // 加入连麦队列成功，发送自定义通知给主播
        sendPushMicLinkNotify(AVChatType.VIDEO.getValue(), PushMicNotificationType.JOIN_QUEUE.getValue());
//        String ext = getPushLinkExt(AVChatType.VIDEO.getValue());
//        ChatRoomHttpClient.getInstance().pushMicLink(roomId, DemoCache.getAccount(), ext, new ChatRoomHttpClient.ChatRoomHttpCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
//                // 加入连麦队列成功，发送自定义通知给主播
//                sendPushMicLinkNotify(AVChatType.VIDEO.getValue(), PushMicNotificationType.JOIN_QUEUE.getValue());
//            }
//
//            @Override
//            public void onFailed(int code, String errorMsg) {
//                LogUtil.d(TAG, "join queue failed, code:" + code);
//                if (code == 419) {
//                    Toast.makeText(AudienceActivity.this, "队列已满", Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(AudienceActivity.this, "join queue failed, code:" + code + ", errorMsg:" + errorMsg, Toast.LENGTH_SHORT).show();
//                }
//                revertPushUI();
//            }
//        });
    }

    // 申请语音连接
    private void doAudioLink() {
        showPushLinkLayout(true, R.string.audio_applying);
        // 加入连麦队列成功，发送自定义通知给主播
        sendPushMicLinkNotify(AVChatType.AUDIO.getValue(), PushMicNotificationType.JOIN_QUEUE.getValue());
//        String ext = getPushLinkExt(AVChatType.AUDIO.getValue());
//        ChatRoomHttpClient.getInstance().pushMicLink(roomId, DemoCache.getAccount(), ext, new ChatRoomHttpClient.ChatRoomHttpCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
//                // 加入连麦队列成功，发送自定义通知给主播
//                sendPushMicLinkNotify(AVChatType.AUDIO.getValue(), PushMicNotificationType.JOIN_QUEUE.getValue());
//            }
//
//            @Override
//            public void onFailed(int code, String errorMsg) {
//                LogUtil.d(TAG, "http push mic link errorMsg:" + errorMsg);
//                Toast.makeText(AudienceActivity.this, "http push mic link errorMsg:" + errorMsg, Toast.LENGTH_SHORT).show();
//                revertPushUI();
//            }
//        });
    }

    private void doAudioModeLink() {
        showPushLinkLayout(false, R.string.audio_applying);
        String ext = getPushLinkExt(AVChatType.AUDIO.getValue());
//        ChatRoomHttpClient.getInstance().pushMicLink(roomId, DemoCache.getAccount(), ext, new ChatRoomHttpClient.ChatRoomHttpCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
                // 加入连麦队列成功，发送自定义通知给主播
                sendPushMicLinkNotify(AVChatType.AUDIO.getValue(), PushMicNotificationType.JOIN_QUEUE.getValue());
//            }

//            @Override
//            public void onFailed(int code, String errorMsg) {
//                Toast.makeText(AudienceActivity.this, "http push mic link, errorMsg:" + errorMsg, Toast.LENGTH_SHORT).show();
//                revertPushUI();
//            }
//        });
    }

    // 显示连接申请布局
    private void showPushLinkLayout(boolean isVideoMode, int applyingMode) {
//        if (isVideoMode) {
//            interationInitLayout.setVisibility(View.GONE);
//        } else {
//            audioInteractInitLayout.setVisibility(View.GONE);
//        }
        applyingLayout.setVisibility(View.VISIBLE);
        applyMasterNameText.setText(TextUtils.isEmpty(masterNick) ? AuthPreferences.getUserLoginName() : masterNick);
        applyingTip.setText(applyingMode);
    }

    // 连麦申请的扩展字段
    private String getPushLinkExt(int style) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(PushLinkConstant.style, style);
        jsonObject.put(PushLinkConstant.state, MicStateEnum.WAITING.getValue());

        JSONObject infoJSON = new JSONObject();
        infoJSON.put(PushLinkConstant.nick, DemoCache.getUserInfo().getName());
        infoJSON.put(PushLinkConstant.avatar, AVATAR_DEFAULT);
        jsonObject.put(PushLinkConstant.info, infoJSON);

        return jsonObject.toString();
    }

    // 发送自定义通知给主播
    private void sendPushMicLinkNotify(int style, int command) {
        CustomNotification notification = new CustomNotification();
        notification.setSessionId(roomInfo.getCreator());
        notification.setSessionType(SessionTypeEnum.P2P);

        JSONObject json = new JSONObject();
        json.put(PushLinkConstant.roomid, roomId);
        json.put(PushLinkConstant.style, style);
        json.put(PushLinkConstant.command, command);
        JSONObject infoJSON = new JSONObject();
        infoJSON.put(PushLinkConstant.nick, DemoCache.getUserInfo().getName());
        infoJSON.put(PushLinkConstant.avatar, AVATAR_DEFAULT);
        json.put(PushLinkConstant.info, infoJSON);
        notification.setContent(json.toString());

        NIMClient.getService(MsgService.class).sendCustomNotification(notification).setCallback(new RequestCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.e(TAG, "send push mic success");
                isMyAlreadyApply = true;
            }

            @Override
            public void onFailed(int i) {
                Log.e(TAG, "send push mic failed, code:" + i);
                Toast.makeText(AudienceActivity.this, "申请失败, code:" + i, Toast.LENGTH_SHORT).show();
                revertPushUI();
            }

            @Override
            public void onException(Throwable throwable) {

            }
        });
    }

    // 撤销连麦申请布局
    private void revertPushUI() {
//        if (liveType == LiveType.VIDEO_TYPE) {
//            interationInitLayout.setVisibility(View.VISIBLE);
//            audioInteractInitLayout.setVisibility(View.GONE);
//        } else if (liveType == LiveType.AUDIO_TYPE) {
//            interationInitLayout.setVisibility(View.GONE);
//            audioInteractInitLayout.setVisibility(View.VISIBLE);
//        }
        applyingLayout.setVisibility(View.GONE);
    }

    // 取消连麦申请
    private void cancelLinking() {
        revertPushUI();
//        ChatRoomHttpClient.getInstance().popMicLink(roomId, DemoCache.getAccount(), new ChatRoomHttpClient.ChatRoomHttpCallback<Void>() {
//            @Override
//            public void onSuccess(Void aVoid) {
                // 取消连麦成功，发送自定义通知给主播
                MicHelper.getInstance().sendCustomNotify(roomId, roomInfo.getCreator(), PushMicNotificationType.EXIT_QUEUE.getValue(), null, true);
                isMyAlreadyApply = false;
//            }

//            @Override
//            public void onFailed(int code, String errorMsg) {
//                Log.e(TAG, "join queue failed, code:" + code);
//                Toast.makeText(AudienceActivity.this, "join queue failed, errorMsg:" + errorMsg, Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    /** 连麦 **/

    // 收到连麦通知
    @Override
    protected void onMicLinking(JSONObject jsonObject) {
        LogUtil.d(TAG, "on mic linking");
        if (!isMyAlreadyApply) {
            // 我是第一次进来，上次状态清空，所以不连麦
            Log.e(TAG, "first coming, send reject");
            MicHelper.getInstance().sendCustomNotify(roomId, roomInfo.getCreator(), PushMicNotificationType.REJECT_CONNECTING.getValue(), null, true);
            return;
        }

        if (!jsonObject.containsKey(PushLinkConstant.style)) {
            return;
        }

        isAgreeToLink = true;
        isMyVideoLink = jsonObject.getIntValue(PushLinkConstant.style) == AVChatType.VIDEO.getValue();
        style = jsonObject.getIntValue(PushLinkConstant.style);

        LogUtil.d(TAG, "audience request permission and join channel");
        requestLivePermission();
    }

    // 开始加入音视频房间，与主播连麦
    private void doMicLinking() {
        // 加入音视频房间
        joinChannel(isMyVideoLink);
    }

    /*********************** join channel ***********************/

    protected void joinChannel(final boolean  isVideo) {
        Log.e("cyx","joinChannel");
        AVChatOptionalConfig avChatOptionalConfig = new AVChatOptionalConfig();
        avChatOptionalConfig.enableLive(true);
        avChatOptionalConfig.enableAudienceRole(false);
        MicHelper.getInstance().joinChannel(avChatOptionalConfig, meetingName, isVideo, new MicHelper.ChannelCallback() {

            @Override
            public void onJoinChannelSuccess() {
                // 打开话筒
                AVChatManager.getInstance().setSpeaker(true);
                // 释放拉流
                releaseVideoPlayer();
                // 设置美颜模式
                updateBeautyIcon(false);
                setOpenCloseFilterParam(true);
                // 连麦者显示连麦画面
                showOnMicView(DemoCache.getAccount());

                preparedText.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.GONE);
                videoRender.setVisibility(View.VISIBLE);
                interationLayout.setVisibility(View.GONE);
            }

            @Override
            public void onJoinChannelFailed() {

            }
        });
    }

    // 释放拉流
    private void releaseVideoPlayer() {
        if (videoPlayer != null) {
            videoPlayer.resetVideo();
        }
        videoPlayer = null;
    }

    // 收到主播连麦成功通知
    @Override
    protected void onMicConnectedMsg(ChatRoomMessage message) {
        ConnectedAttachment attachment = (ConnectedAttachment) message.getAttachment();
        showConnectionView(attachment.getAccount(), attachment.getNick(), attachment.getStyle());
    }

    // 连麦者/非连麦者各自界面显示
    @Override
    protected void showConnectionView(String account, String nick, int style) {
        super.showConnectionView(account, nick, style);
        if (!DemoCache.getAccount().equals(account) && style == AVChatType.AUDIO.getValue()) {
            // 非连麦者的语音模式
            connectionViewLayout.setVisibility(View.VISIBLE);
            audienceLoadingLayout.setVisibility(View.GONE);
            connectionViewCloseBtn.setVisibility(View.GONE);
            audioModeBypassLayout.setVisibility(View.VISIBLE);
            bypassVideoRender.setVisibility(View.GONE);
        }
    }

    private void showOnMicView(String account) {
        if (DemoCache.getAccount().equals(account)) {
            // 我是连麦者
            isMeOnMic = true;
            connectionViewLayout.setVisibility(View.VISIBLE);
            audienceLoadingLayout.setVisibility(View.GONE);
            updateMicUI(style);
//            if (liveType == LiveType.VIDEO_TYPE && style == AVChatType.VIDEO.getValue()) {
                audienceLivingLayout.setVisibility(View.VISIBLE);
                audioModeBypassLayout.setVisibility(View.GONE);
                bypassVideoRender.setVisibility(View.VISIBLE);
                AVChatManager.getInstance().setupVideoRender(account, bypassVideoRender, false, AVChatVideoScalingType.SCALE_ASPECT_BALANCED);
//            } else if (style == AVChatType.AUDIO.getValue()){
//                bypassVideoRender.setVisibility(View.GONE);
//                audienceLivingLayout.setVisibility(View.GONE);
//                audioModeBypassLayout.setVisibility(View.VISIBLE);
//            }
        }
    }

    // 更新UI布局,包括输入框,按钮的变化
    private void updateMicUI(int style) {
        connectionViewCloseBtn.setVisibility(View.VISIBLE);
//        switchBtn.setVisibility(style == AVChatType.VIDEO.getValue() ? View.VISIBLE : View.GONE);
        beautyBtn.setVisibility(style == AVChatType.VIDEO.getValue() ? View.VISIBLE : View.GONE );
        interactionBtn.setVisibility(View.GONE);
//        inputPanel.hideInputPanel();
//        inputPanel.collapse(true);
        controlLayout.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);//设置置底
        controlLayout.setLayoutParams(lp);//动态改变布局
    }

    /** 断开连麦 **/

    // 主动断开连麦
    @Override
    protected void doCloseInteraction() {
        preparedText.setVisibility(View.VISIBLE);
        MicHelper.getInstance().audienceBrokeMic(roomId, DemoCache.getAccount());
        switchVideoPlayer();
    }

    // 收到主播断开连麦通知
    @Override
    protected void onMicCanceling() {
        // 隐藏布局（无论是否处于频道中，都要清理界面上的布局，如连麦者姓名）
        resetConnectionView();

        // 确保还处在频道中时，才要切换成拉流模式
        if (videoPlayer != null) {
            return;
        }

        preparedText.setVisibility(View.VISIBLE);

        // 离开频道
        if (liveType != LiveType.NOT_ONLINE) {
            MicHelper.getInstance().leaveChannel();
            // 切换拉流
            switchVideoPlayer();
        }
    }

    @Override
    protected void onMicDisConnectedMsg(String account) {
        if (isMeOnMic && !TextUtils.isEmpty(account) && !account.equals(DemoCache.getAccount())) {
            return;
        }
        resetConnectionView();
    }

    // 收到主播断开连麦全局消息
    protected void resetConnectionView() {
        LogUtil.i(TAG, "reset Connection view");
        livingBg.setVisibility(View.GONE);
        isAgreeToLink = false;
        isBeautyOpen = true;
        if (isMeOnMic) {
            isMeOnMic = false;
            isMyAlreadyApply = false;
            interactionBtn.setVisibility(View.VISIBLE);
//            switchBtn.setVisibility(View.GONE);
            beautyBtn.setVisibility(View.GONE);
//            inputPanel.showInputPanel();
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);//设置置底
            controlLayout.setLayoutParams(lp);//动态改变布局
            bypassVideoRender.setVisibility(View.GONE);
            resetApplyLayout();
        }
        super.resetConnectionView();
    }

    private void resetApplyLayout() {
        interationInitLayout.setVisibility(View.VISIBLE);
        applyingLayout.setVisibility(View.GONE);
    }

    private void switchVideoPlayer() {
        if (videoPlayer == null && liveType != LiveType.NOT_ONLINE) {
            initAudienceParam();
        }
    }

    @Override
    protected void onReceiveChatRoomInfoUpdate(Map<String, Object> extension) {
        if (extension != null && extension.get(PushLinkConstant.type) != null) {
            liveType = LiveType.typeOfValue((int)extension.get(PushLinkConstant.type));
            if(liveType == LiveType.NOT_ONLINE) {
                showFinishLayout();
                resetConnectionView();
                // videoPlayer不等于null 则属于拉流状况
                if (videoPlayer != null) {
                    videoPlayer.resetResource();
                } else {
                    // videoPlayer为null 则在连麦中。退出channel
                    AVChatManager.getInstance().leaveRoom(new AVChatCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            LogUtil.i(TAG, "master leave room, update info, audience leave room, success");
                        }

                        @Override
                        public void onFailed(int i) {
                            LogUtil.i(TAG, "master leave room, update info, audience leave room, failed code:" + i);
                        }

                        @Override
                        public void onException(Throwable throwable) {

                        }
                    });
                    videoPlayer = new VideoPlayer(AudienceActivity.this, videoView, null, url,
                            UserPreferences.getPlayerStrategy(), this, VideoConstant.VIDEO_SCALING_MODE_FILL_SCALE);
                }
                videoPlayer.postReopenVideoTask(VideoPlayer.VIDEO_COMPLETED_REOPEN_TIMEOUT);
            }

        }
    }

    /************************* AVChatStateObserver *****************************/

    @Override
    public void onTakeSnapshotResult(String s, boolean b, String s1) {

    }

    @Override
    public void onConnectionTypeChanged(int i) {

    }

    @Override
    public void onLocalRecordEnd(String[] strings, int i) {

    }

    @Override
    public void onFirstVideoFrameAvailable(String s) {

    }

    @Override
    public void onVideoFpsReported(String s, int i) {

    }

    @Override
    public void onJoinedChannel(int i, String s, String s1) {
        if (i == AVChatResCode.JoinChannelCode.OK && liveType == LiveType.AUDIO_TYPE) {
            AVChatManager.getInstance().setSpeaker(true);
        }
    }

    @Override
    public void onLeaveChannel() {

    }

    @Override
    public void onUserJoined(String s) {
        Log.e("cyx","--onUserJoined="+s);
//        if (liveType == LiveType.VIDEO_TYPE && s.equals(roomInfo.getCreator())) {
            Log.e("cyx","1--");
            if(s.equals(DemoCache.getAccount())) {
                Log.e("cyx","2--");
                livingBg.setVisibility(View.VISIBLE);
            }
            AVChatManager.getInstance().setupVideoRender(roomInfo.getCreator(), videoRender, false, AVChatVideoScalingType.SCALE_ASPECT_BALANCED);
//        }
        if (liveType != LiveType.VIDEO_TYPE) {
            preparedText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUserLeave(String s, int i) {
        if (s.equals(roomInfo.getCreator())) {
            MicHelper.getInstance().leaveChannel();
        }
        switchVideoPlayer();
        resetConnectionView();
    }

    @Override
    public void onProtocolIncompatible(int i) {

    }

    @Override
    public void onDisconnectServer() {

    }

    @Override
    public void onNetworkQuality(String s, int i) {

    }

    @Override
    public void onCallEstablished() {
        LogUtil.d(TAG, "audience onCallEstablished");
        AVChatManager.getInstance().enableAudienceRole(false);
    }

    @Override
    public void onDeviceEvent(int i, String s) {

    }

    @Override
    public void onFirstVideoFrameRendered(String s) {
        Log.e("cyx","onFirstVideoFrameRendered----"+s);
        if (liveFinishLayout.getVisibility() == View.VISIBLE) {
            liveFinishLayout.setVisibility(View.GONE);
        }
        if (s.equals(roomInfo.getCreator())) {
            preparedText.setVisibility(View.GONE);
        }
        if (s.equals(DemoCache.getAccount())) {
            livingBg.setVisibility(View.GONE);
        }
    }

    @Override
    public void onVideoFrameResolutionChanged(String s, int i, int i1, int i2) {

    }

    @Override
    public int onVideoFrameFilter(AVChatVideoFrame avChatVideoFrame) {
        avChatVideoFrame.format = AVChatImageFormat.I420;
        return mGPUEffect.apply(avChatVideoFrame.data, avChatVideoFrame.width, avChatVideoFrame.height);
    }

    @Override
    public int onAudioFrameFilter(AVChatAudioFrame avChatAudioFrame) {
        return 0;
    }

    @Override
    public void onAudioDeviceChanged(int i) {

    }

    @Override
    public void onReportSpeaker(Map<String, Integer> map, int i) {

    }

    @Override
    public void onStartLiveResult(int i) {

    }

    @Override
    public void onStopLiveResult(int i) {

    }

    @Override
    public void onAudioMixingEvent(int i) {

    }

    /*************************** VideoPlayer.VideoPlayerProxy ***************************/

    @Override
    public boolean isDisconnected() {
        return false;
    }

    @Override
    public void onError() {
        LogUtil.d(TAG, "on error, show finish layout");
        preparedText.setVisibility(View.VISIBLE);
        showFinishLayout();
    }

    @Override
    public void onCompletion() {
        LogUtil.d(TAG, "on completion, show finish layout");
        isStartLive = false;
        showFinishLayout();
        TextView masterNickText = findView(R.id.finish_master_name);
        masterNickText.setText(TextUtils.isEmpty(masterNick) ? AuthPreferences.getUserLoginName() : masterNick);
    }

    @Override
    public void onPrepared() {
        Log.e(TAG, "on prepared, hide preparedText");
        if (liveType == LiveType.NOT_ONLINE) {
            return;
        }
        isStartLive = true;
        preparedText.setVisibility(View.GONE);
        liveFinishLayout.setVisibility(View.GONE);
        videoRender.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
        showModeLayout();
        fetchMicList();
    }

    @Override
    public void onInfo(NELivePlayer mp, int what, int extra) {
        // web端推流的时候，不报onPrepared上来，只能用onInfo处理了。哼
        Log.e(TAG, "onInfo");
//        if ((what == NELivePlayer.NELP_FIRST_VIDEO_RENDERED || what == NELivePlayer.NELP_FIRST_AUDIO_RENDERED)
//                && liveType != LiveType.NOT_ONLINE) {
            Log.e(TAG, "on info NELP_FIRST_VIDEO_RENDERED, hide preparedText");
            isStartLive = true;
            preparedText.setVisibility(View.GONE);
            liveFinishLayout.setVisibility(View.GONE);
            videoRender.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            showModeLayout();
            fetchMicList();
//        }
    }

    // 显示直播已结束布局
    private void showFinishLayout() {
        liveFinishLayout.setVisibility(View.VISIBLE);
        finishTipText.setText(R.string.live_finish);
//        inputPanel.collapse(true);
    }

}
