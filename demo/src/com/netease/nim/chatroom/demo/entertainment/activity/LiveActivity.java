package com.netease.nim.chatroom.demo.entertainment.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.chatroom.demo.DemoCache;
import com.netease.nim.chatroom.demo.R;
import com.netease.nim.chatroom.demo.base.util.log.LogUtil;
import com.netease.nim.chatroom.demo.entertainment.adapter.GiftAdapter;
import com.netease.nim.chatroom.demo.entertainment.adapter.InteractionAdapter;
import com.netease.nim.chatroom.demo.entertainment.constant.GiftConstant;
import com.netease.nim.chatroom.demo.entertainment.constant.GiftType;
import com.netease.nim.chatroom.demo.entertainment.constant.LiveType;
import com.netease.nim.chatroom.demo.entertainment.constant.MicStateEnum;
import com.netease.nim.chatroom.demo.entertainment.constant.PushLinkConstant;
import com.netease.nim.chatroom.demo.entertainment.helper.ChatRoomMemberCache;
import com.netease.nim.chatroom.demo.entertainment.helper.GiftCache;
import com.netease.nim.chatroom.demo.entertainment.helper.MicHelper;
import com.netease.nim.chatroom.demo.entertainment.model.Gift;
import com.netease.nim.chatroom.demo.entertainment.model.InteractionMember;
import com.netease.nim.chatroom.demo.im.ui.dialog.EasyAlertDialogHelper;
import com.netease.nim.chatroom.demo.im.ui.widget.SwitchButton;
import com.netease.nim.chatroom.demo.im.util.file.AttachmentStore;
import com.netease.nim.chatroom.demo.permission.MPermission;
import com.netease.nim.chatroom.demo.permission.annotation.OnMPermissionDenied;
import com.netease.nim.chatroom.demo.permission.annotation.OnMPermissionGranted;
import com.netease.nim.chatroom.demo.permission.annotation.OnMPermissionNeverAskAgain;
import com.netease.nim.chatroom.demo.permission.util.MPermissionUtil;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.constant.AVChatAudioMixingEvent;
import com.netease.nimlib.sdk.avchat.constant.AVChatImageFormat;
import com.netease.nimlib.sdk.avchat.constant.AVChatLivePIPMode;
import com.netease.nimlib.sdk.avchat.constant.AVChatNetworkQuality;
import com.netease.nimlib.sdk.avchat.constant.AVChatResCode;
import com.netease.nimlib.sdk.avchat.constant.AVChatType;
import com.netease.nimlib.sdk.avchat.constant.AVChatVideoFrameRate;
import com.netease.nimlib.sdk.avchat.constant.AVChatVideoQuality;
import com.netease.nimlib.sdk.avchat.constant.AVChatVideoScalingType;
import com.netease.nimlib.sdk.avchat.model.AVChatAudioFrame;
import com.netease.nimlib.sdk.avchat.model.AVChatOptionalConfig;
import com.netease.nimlib.sdk.avchat.model.AVChatParameters;
import com.netease.nimlib.sdk.avchat.model.AVChatVideoFrame;
import com.netease.nimlib.sdk.avchat.model.AVChatVideoRender;
import com.netease.nimlib.sdk.chatroom.ChatRoomService;
import com.netease.nimlib.sdk.chatroom.model.ChatRoomMember;
import com.netease.nimlib.sdk.chatroom.model.ChatRoomUpdateInfo;
import com.netease.nimlib.sdk.chatroom.model.EnterChatRoomData;
import com.netease.nimlib.sdk.msg.model.CustomNotification;
import com.netease.nrtc.effect.video.GPUImage;
import com.netease.nrtc.effect.video.GPUImageBilateralFilter;
import com.netease.nrtc.effect.video.GPUImageBrightnessFilter;
import com.netease.nrtc.effect.video.GPUImageContrastFilter;
import com.netease.nrtc.effect.video.GPUImageSaturationFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LiveActivity extends LivePlayerBaseActivity implements InteractionAdapter.MemberLinkListener {
    private static final String TAG = "LiveActivity";
    private final int USER_LEAVE_OVERTIME = 10 * 1000;
    private final int USER_JOIN_OVERTIME = 10 * 1000;

    // view
    private View backBtn;
    private View startLayout;
    private Button startBtn;
    private ImageButton switchBtn;
    private TextView noGiftText;
    private ViewGroup liveFinishLayout;
    private Button liveFinishBtn;
    private ViewGroup hdBtn;
    private ViewGroup hd_choose_layout;
    private TextView hdSelectedText;
    private TextView hdFirstText;
    private TextView hdSecondText;
    private ViewGroup interactionLayout; // 互动布局
    private TextView noApplyText; // 暂无互动申请
    private TextView applyCountText;
    private GridView interactionGridView;
    private AVChatVideoRender videoRender; // 主播画面
    private ImageButton musicBtn;
    private ViewGroup backgroundMusicLayout;
    private RelativeLayout musicBlankView;
    private LinearLayout musicContentView;
    private SwitchButton musicSwitchButton;
    private TextView musicSongFirstContent;
    private TextView musicSongSecondContent;
    private ImageView musicSongFirstControl;
    private ImageView musicSongSecondControl;
    private TextView musicSongVolumeContent;
    private SeekBar musicSongVolumeControl;
    /* 网络状态 */
    private ViewGroup networkStateLayout;
    private TextView netStateTipText; // 网络状态提示
    private ImageView netStateImage;
    private TextView netOperateText; // 网络操作提示

    // state
    private boolean disconnected = false; // 是否断网（断网重连用）
    private boolean isStartLive = false; // 是否开始直播推流
    private boolean isWaiting = false; // 是否正在等待连麦成功
    private boolean isMusicSwitchButtonEnable = false;//背景乐选择面板的开关状态
    private boolean isMusicFirstPlaying = false;
    private boolean isMusicFirstPause = false;
    private boolean isMusicSecondPlaying = false;
    private boolean isMusicSecondPause = false;

    // data
    private List<Gift> giftList = new ArrayList<>(); // 礼物列表数据
    private int interactionCount = 0; // 互动申请人数
    private InteractionAdapter interactionAdapter; // 互动人员adapter
    private List<InteractionMember> interactionDataSource; // 互动人员列表
    private String clickAccount; // 选择的互动人员帐号
    private InteractionMember currentInteractionMember; // 当前连麦者
    private InteractionMember nextInteractionMember; // 下一个选中连麦者

    public static void start(Context context, String meetingName, String roomId, String url, boolean isVideo, boolean isCreator) {
        Intent intent = new Intent();
        intent.setClass(context, LiveActivity.class);
        intent.putExtra(EXTRA_MEETING_NAME, meetingName);
        intent.putExtra(EXTRA_ROOM_ID, roomId);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_MODE, isVideo);
        intent.putExtra(EXTRA_CREATOR, isCreator);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadGift();
        registerLiveObservers(true);

        //目前伴音功能的音乐文件，nrtc只支持读取存储空间里面的音乐文件，不支持assets中的文件，所以这里将文件拷贝到存储空间里面
        if (Environment.getExternalStorageDirectory() != null) {
            AttachmentStore.copy(this, "music/first_song.mp3", Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getPackageName() + "/music", "/first_song.mp3");
            AttachmentStore.copy(this, "music/second_song.mp3", Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getPackageName() + "/music", "/second_song.mp3");
        }

    }

    @Override
    protected void setEnterRoomExtension(EnterChatRoomData enterChatRoomData) {
        Map<String, Object> notifyExt = new HashMap<>();
        if (liveType == LiveType.VIDEO_TYPE) {
            notifyExt.put(PushLinkConstant.type, AVChatType.VIDEO.getValue());
        } else if (liveType == LiveType.AUDIO_TYPE) {
            notifyExt.put(PushLinkConstant.type, AVChatType.AUDIO.getValue());
        }
        notifyExt.put(PushLinkConstant.meetingName, meetingName);
        enterChatRoomData.setNotifyExtension(notifyExt);
    }

    @Override
    protected void initParam() {
        mGPUEffect = GPUImage.create(this);
        mGPUEffect.setFilter(new GPUImageBilateralFilter(30f));
        mGPUEffect.setFilter(new GPUImageContrastFilter(1.5f));
        mGPUEffect.setFilter(new GPUImageSaturationFilter(1.4f));
        mGPUEffect.setFilter(new GPUImageBrightnessFilter(0.1f));
    }

    @Override
    protected int getActivityLayout() {
        return R.layout.live_player_activity;
    }

    @Override
    protected int getLayoutId() {
        return R.id.live_layout;
    }

    @Override
    protected void parseIntent() {
        super.parseIntent();
        boolean isVideo = getIntent().getBooleanExtra(EXTRA_MODE, true);
        liveType = isVideo ? LiveType.VIDEO_TYPE : LiveType.AUDIO_TYPE;
        meetingName = getIntent().getStringExtra(EXTRA_MEETING_NAME);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (isStartLive) {
            logoutChatRoom();
        } else {
            NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
            clearChatRoom();
        }
    }

    @Override
    protected void onDestroy() {
        // 释放资源
        giftList.clear();

        if (mGPUEffect != null) {
            mGPUEffect.dispose();
        }

        registerLiveObservers(false);
        super.onDestroy();
    }

    // 退出聊天室
    private void logoutChatRoom() {
        EasyAlertDialogHelper.createOkCancelDiolag(this, null, getString(R.string.finish_confirm),
                getString(R.string.confirm), getString(R.string.cancel), true,
                new EasyAlertDialogHelper.OnDialogActionListener() {
                    @Override
                    public void doCancelAction() {

                    }

                    @Override
                    public void doOkAction() {
                        doCompletelyFinish();
                    }
                }).show();
    }

    private void doCompletelyFinish() {
        isStartLive = false;
        showLiveFinishLayout();
        doUpdateRoomInfo();
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doLeaveAVChatRoom();
            }
        }, 50);
    }

    private void showLiveFinishLayout() {
        liveFinishLayout.setVisibility(View.VISIBLE);
        TextView masterNickText = findView(R.id.finish_master_name);
        masterNickText.setText(TextUtils.isEmpty(masterNick) ? roomInfo.getCreator() : masterNick);
    }


    private void doUpdateRoomInfo() {
        ChatRoomUpdateInfo chatRoomUpdateInfo = new ChatRoomUpdateInfo();
        Map<String, Object> map = new HashMap<>(1);
        map.put(PushLinkConstant.type, -1);
        chatRoomUpdateInfo.setExtension(map);
        NIMClient.getService(ChatRoomService.class)
                .updateRoomInfo(roomId, chatRoomUpdateInfo, true, map)
                .setCallback(new RequestCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        LogUtil.i(TAG, "leave room, update room info success");
                    }

                    @Override
                    public void onFailed(int i) {
                        LogUtil.i(TAG, "leave room, update room info failed, code:" + i);
                    }

                    @Override
                    public void onException(Throwable throwable) {

                    }
                });
    }

    // 清空聊天室缓存
    private void clearChatRoom() {
        ChatRoomMemberCache.getInstance().clearRoomCache(roomId);
        finish();
    }

    /***********************
     * join channel
     ***********************/
    AVChatOptionalConfig avChatOptionalConfig;

    protected void joinChannel(final LiveType liveType) {
        avChatOptionalConfig = new AVChatOptionalConfig();
        avChatOptionalConfig.enableLive(true);
        avChatOptionalConfig.setLiveUrl(url);
        avChatOptionalConfig.enableAudienceRole(false);
        avChatOptionalConfig.setVideoEncoderMode(AVChatParameters.MEDIA_CODEC_SOFTWARE);
        avChatOptionalConfig.setVideoQuality(AVChatVideoQuality.QUALITY_720P);
        avChatOptionalConfig.setVideoFrameRate(AVChatVideoFrameRate.FRAME_RATE_25);
        avChatOptionalConfig.setLivePIPMode(AVChatLivePIPMode.PIP_FLOATING_RIGHT_VERTICAL);
        MicHelper.getInstance().joinChannel(avChatOptionalConfig, meetingName, liveType == LiveType.VIDEO_TYPE, new MicHelper.ChannelCallback() {

            @Override
            public void onJoinChannelSuccess() {
                if (liveType == LiveType.AUDIO_TYPE) {
                    AVChatManager.getInstance().setSpeaker(true);
                }
                MicHelper.getInstance().sendBrokeMicMsg(roomId, null);
                dropQueue();
                // 初始化美颜
                updateBeautyIcon(false);
                setOpenCloseFilterParam(true);
            }

            @Override
            public void onJoinChannelFailed() {
                showLiveFinishLayout();
            }
        });
    }

    /*****************************
     * 初始化
     *****************************/

    protected void findViews() {
        super.findViews();
        rootView = findView(R.id.live_layout);
        videoRender = findView(R.id.video_render);
        videoRender.setZOrderMediaOverlay(false);
        backBtn = findView(R.id.BackBtn);
        startLayout = findViewById(R.id.start_layout);
        startBtn = (Button) findViewById(R.id.start_live_btn);
        switchBtn = (ImageButton) findViewById(R.id.switch_btn);
        beautyBtn = (ImageButton) findViewById(R.id.beauty_btn);
        noGiftText = findView(R.id.no_gift_tip);
        controlLayout = findView(R.id.control_layout);
        interactionLayout = findView(R.id.live_interaction_layout);
        noApplyText = findView(R.id.no_apply_tip);
        applyCountText = findView(R.id.apply_count_text);
        fakeListText = findView(R.id.fake_list_text);

        // 高清
        hdBtn = findView(R.id.hd_btn);
        hd_choose_layout = findView(R.id.hd_choose_layout);
        hdSelectedText = findView(R.id.hd_mode_selected);
        hdFirstText = findView(R.id.hd_first_mode);
        hdSecondText = findView(R.id.hd_second_mode);

        //背景乐
        musicBtn = findView(R.id.music_btn);
        findMusicLayout();

        // 直播结束
        liveFinishLayout = findView(R.id.live_finish_layout);
        liveFinishBtn = findView(R.id.finish_btn);
        liveFinishBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
                clearChatRoom();
            }
        });

        // 初始化连线人员布局
        findInteractionMemberLayout();

        setListener();

        // 视频/音频，布局设置
        if (liveType == LiveType.AUDIO_TYPE) {
            switchBtn.setVisibility(View.GONE);
            hdBtn.setVisibility(View.GONE);
            beautyBtn.setVisibility(View.GONE);
        } else if (liveType == LiveType.VIDEO_TYPE) {
            switchBtn.setVisibility(View.VISIBLE);
            hdBtn.setVisibility(View.VISIBLE);
            beautyBtn.setVisibility(View.VISIBLE);
        }

        // 网络状态
        networkStateLayout = findView(R.id.network_state_layout);
        netStateTipText = findView(R.id.net_state_tip);
        netStateImage = findView(R.id.network_image);
        netOperateText = findView(R.id.network_operation_tip);
    }

    // 初始化礼物布局
    protected void findGiftLayout() {
        super.findGiftLayout();
        adapter = new GiftAdapter(giftList, this);
        giftView.setAdapter(adapter);
    }

    protected void findMusicLayout() {
        backgroundMusicLayout = findView(R.id.background_music_layout);
        musicBlankView = findView(R.id.background_music_blank_view);
        musicContentView = findView(R.id.background_music_content_view);
        musicSwitchButton = findView(R.id.music_switch_button);
        musicSongFirstContent = findView(R.id.music_song_first_content);
        musicSongSecondContent = findView(R.id.music_song_second_content);
        musicSongFirstControl = findView(R.id.music_song_first_control);
        musicSongFirstControl.setOnClickListener(musicListener);
        musicSongSecondControl = findView(R.id.music_song_second_control);
        musicSongSecondControl.setOnClickListener(musicListener);
        musicSongVolumeContent = findView(R.id.music_song_volume_content);
        musicSongVolumeControl = findView(R.id.music_song_volume_control);
        updateMusicLayoutState();
        musicSwitchButton.setOnChangedListener(new SwitchButton.OnChangedListener() {
            @Override
            public void OnChanged(View v, boolean checkState) {
                isMusicSwitchButtonEnable = checkState;
                updateMusicLayoutState();
            }
        });
        musicSongVolumeControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                AVChatManager.getInstance().setParameter(AVChatParameters.KEY_AUDIO_MIXING_STREAM_VOLUME, seekBar.getProgress() * 1.0f / 100);
            }
        });
    }

    protected void updateMusicLayoutState() {
        musicSongFirstContent.setEnabled(isMusicSwitchButtonEnable);
        musicSongSecondContent.setEnabled(isMusicSwitchButtonEnable);
        musicSongFirstControl.setEnabled(isMusicSwitchButtonEnable);
        musicSongSecondControl.setEnabled(isMusicSwitchButtonEnable);
        musicSongVolumeContent.setEnabled(isMusicSwitchButtonEnable);
        musicSongVolumeControl.setEnabled(isMusicSwitchButtonEnable);
        if (!isMusicSwitchButtonEnable) {
            resetMusicLayoutViews(true, true);
            AVChatManager.getInstance().stopAudioMixing();
        }
    }

    private View.OnClickListener musicListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getPackageName();
            switch (v.getId()) {
                case R.id.music_song_first_control:
                    if (isMusicFirstPlaying) {
                        AVChatManager.getInstance().pauseAudioMixing();
                        musicSongFirstControl.setImageResource(R.drawable.background_music_control_play);
                        musicSongFirstContent.setText(R.string.background_music_song_first_play);
                        isMusicFirstPlaying = false;
                        isMusicFirstPause = true;
                    } else {
                        if (isMusicSecondPlaying) {
                            resetMusicLayoutViews(false, true);
                        }

                        isMusicFirstPlaying = true;
                        musicSongFirstControl.setImageResource(R.drawable.background_music_control_pause);
                        musicSongFirstContent.setText(R.string.background_music_song_first_pause);
                        if (isMusicFirstPause) {
                            AVChatManager.getInstance().resumeAudioMixing();
                        } else {
                            String songPath = rootPath + "/music/first_song.mp3";

                            AVChatManager.getInstance().startAudioMixing(songPath, true, false, 100, musicSongVolumeControl.getProgress() * 1.0f / 100);
                        }
                    }

                    break;
                case R.id.music_song_second_control:
                    if (isMusicSecondPlaying) {
                        AVChatManager.getInstance().pauseAudioMixing();
                        musicSongSecondControl.setImageResource(R.drawable.background_music_control_play);
                        musicSongSecondContent.setText(R.string.background_music_song_second_play);
                        isMusicSecondPlaying = false;
                        isMusicSecondPause = true;
                    } else {
                        if (isMusicFirstPlaying) {
                            resetMusicLayoutViews(true, false);
                        }

                        isMusicSecondPlaying = true;
                        musicSongSecondControl.setImageResource(R.drawable.background_music_control_pause);
                        musicSongSecondContent.setText(R.string.background_music_song_second_pause);
                        if (isMusicSecondPause) {
                            AVChatManager.getInstance().resumeAudioMixing();
                        } else {
                            String songPath = rootPath + "/music/second_song.mp3";
                            AVChatManager.getInstance().startAudioMixing(songPath, true, false, 100, musicSongVolumeControl.getProgress() * 1.0f / 100);
                        }
                    }
                    break;
            }
        }
    };

    private void resetMusicLayoutViews(boolean resetFirstSong, boolean resetSecondSong) {
        if (resetFirstSong) {
            musicSongFirstControl.setImageResource(R.drawable.background_music_control_play);
            musicSongFirstContent.setText(R.string.background_music_song_first_play);
            isMusicFirstPlaying = false;
            isMusicFirstPause = false;
        }
        if (resetSecondSong) {
            musicSongSecondControl.setImageResource(R.drawable.background_music_control_play);
            musicSongSecondContent.setText(R.string.background_music_song_second_play);
            isMusicSecondPlaying = false;
            isMusicSecondPause = false;
        }
    }


    private void findInteractionMemberLayout() {
        interactionGridView = findView(R.id.apply_grid_view);
        interactionDataSource = new ArrayList<>();
        interactionAdapter = new InteractionAdapter(interactionDataSource, this, this);
        interactionGridView.setAdapter(interactionAdapter);
        interactionGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InteractionMember member = (InteractionMember) interactionAdapter.getItem(position);
                member.setSelected(true);

                if (clickAccount != null && !clickAccount.equals(member.getAccount())) {
                    for (InteractionMember m : interactionDataSource) {
                        if (m.getAccount().equals(clickAccount)) {
                            m.setSelected(false);
                            break;
                        }
                    }
                }

                clickAccount = member.getAccount();
                interactionAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void updateUI() {
        super.updateUI();
        ChatRoomMember roomMember = ChatRoomMemberCache.getInstance().getChatRoomMember(roomId, roomInfo.getCreator());
        if (roomMember != null) {
            masterNick = roomMember.getNick();
        }
        masterNameText.setText(TextUtils.isEmpty(masterNick) ? roomInfo.getCreator() : masterNick);
    }

    // 主播进来清空队列
    private void dropQueue() {
        NIMClient.getService(ChatRoomService.class).dropQueue(roomId).setCallback(new RequestCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                LogUtil.d(TAG, "drop queue success");
            }

            @Override
            public void onFailed(int i) {
                LogUtil.d(TAG, "drop queue failed, code:" + i);
            }

            @Override
            public void onException(Throwable throwable) {

            }
        });
    }

    // 取出缓存的礼物
    private void loadGift() {
        Map gifts = GiftCache.getInstance().getGift(roomId);
        if (gifts == null) {
            return;
        }
        Iterator<Map.Entry<Integer, Integer>> it = gifts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> entry = it.next();
            int type = entry.getKey();
            int count = entry.getValue();
            giftList.add(new Gift(GiftType.typeOfValue(type), GiftConstant.titles[type], count, GiftConstant.images[type]));
        }
    }

    private void setListener() {
        startBtn.setOnClickListener(buttonClickListener);
        backBtn.setOnClickListener(buttonClickListener);
        switchBtn.setOnClickListener(buttonClickListener);
        beautyBtn.setOnClickListener(buttonClickListener);
        interactionBtn.setOnClickListener(buttonClickListener);
        interactionLayout.setOnClickListener(buttonClickListener);
        giftBtn.setOnClickListener(buttonClickListener);
        giftLayout.setOnClickListener(buttonClickListener);
        musicBtn.setOnClickListener(buttonClickListener);
        musicBlankView.setOnClickListener(buttonClickListener);
        musicContentView.setOnClickListener(buttonClickListener);
        hdSelectedText.setOnClickListener(buttonClickListener);
        hdFirstText.setOnClickListener(buttonClickListener);
        hdSecondText.setOnClickListener(buttonClickListener);
    }

    private void registerLiveObservers(boolean register) {
        AVChatManager.getInstance().observeAVChatState(this, register);
    }

    OnClickListener buttonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.start_live_btn:
                    if (disconnected) {
                        // 如果网络不通
                        Toast.makeText(LiveActivity.this, R.string.net_broken, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startBtn.setText("准备中...");
                    requestLivePermission(); // 请求权限
                    break;
                case R.id.BackBtn:
                    if (isStartLive) {
                        logoutChatRoom();
                    } else {
                        NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
                        clearChatRoom();
                    }
                    break;
                case R.id.switch_btn:
                    AVChatManager.getInstance().switchCamera();
                    break;
                case R.id.beauty_btn:
                    openCloseBeauty();
                    break;
                case R.id.interaction_btn:
                    showInteractionLayout();
                    break;
                case R.id.live_interaction_layout:
                    interactionLayout.setVisibility(View.GONE);
                    break;
                case R.id.gift_btn:
                    showGiftLayout();
                    break;
                case R.id.gift_layout:
                    giftLayout.setVisibility(View.GONE);
                    break;
                case R.id.hd_mode_selected:
                    hdSelectedText.setVisibility(View.GONE);
                    hd_choose_layout.setVisibility(View.VISIBLE);
                    break;
                case R.id.hd_first_mode:
                    hdSelectedText.setText("高清");
                    hd_choose_layout.setVisibility(View.GONE);
                    hdSelectedText.setVisibility(View.VISIBLE);
                    setVideoQuality(AVChatVideoQuality.QUALITY_720P);
                    break;
                case R.id.hd_second_mode:
                    hdSelectedText.setText("流畅");
                    hd_choose_layout.setVisibility(View.GONE);
                    hdSelectedText.setVisibility(View.VISIBLE);
                    setVideoQuality(AVChatVideoQuality.QUALITY_480P);
                    break;
                case R.id.music_btn:
                    showMusicLayout();
                    break;
                case R.id.background_music_blank_view:
                    backgroundMusicLayout.setVisibility(View.GONE);
                    break;
            }
        }
    };

    // set video quality
    private void setVideoQuality(int quality) {
        AVChatParameters parameters = new AVChatParameters();
        parameters.setInteger(AVChatParameters.KEY_VIDEO_QUALITY, quality);
        AVChatManager.getInstance().setParameters(parameters);
    }

    // 显示礼物布局
    private void showGiftLayout() {
        inputPanel.collapse(true);// 收起软键盘
        giftLayout.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        if (adapter.getCount() == 0) {
            // 暂无礼物
            noGiftText.setVisibility(View.VISIBLE);
        } else {
            noGiftText.setVisibility(View.GONE);
        }
    }

    protected void updateGiftList(GiftType type) {
        if (!updateGiftCount(type)) {
            giftList.add(new Gift(type, GiftConstant.titles[type.getValue()], 1, GiftConstant.images[type.getValue()]));
        }
        GiftCache.getInstance().saveGift(roomId, type.getValue());
    }

    // 更新收到礼物的数量
    private boolean updateGiftCount(GiftType type) {
        for (Gift gift : giftList) {
            if (type == gift.getGiftType()) {
                gift.setCount(gift.getCount() + 1);
                return true;
            }
        }
        return false;
    }

    private void showMusicLayout() {
        inputPanel.collapse(true);
        backgroundMusicLayout.setVisibility(View.VISIBLE);
    }

    /**
     * ********************************** 断网重连处理 **********************************
     */

    // 网络连接成功
    protected void onConnected() {
        if (disconnected == false) {
            return;
        }

        changeNetWorkTip(true);

        LogUtil.i(TAG, "live on connected");

        disconnected = false;
    }

    // 网络断开
    protected void onDisconnected() {
        LogUtil.i(TAG, "live on disconnected");
        disconnected = true;
        changeNetWorkTip(false);
    }

    private void changeNetWorkTip(boolean isShow) {
        if (networkStateLayout == null) {
            networkStateLayout = findView(R.id.network_state_layout);
        }
        networkStateLayout.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }


    /***********************
     * 录音摄像头权限申请
     *******************************/

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @OnMPermissionGranted(LIVE_PERMISSION_REQUEST_CODE)
    public void onLivePermissionGranted() {
        Toast.makeText(LiveActivity.this, "授权成功", Toast.LENGTH_SHORT).show();
        isStartLive = true;
        joinChannel(liveType);
        startLayout.setVisibility(View.GONE);
    }

    @OnMPermissionDenied(LIVE_PERMISSION_REQUEST_CODE)
    public void onLivePermissionDenied() {
        List<String> deniedPermissions = MPermission.getDeniedPermissions(this, LIVE_PERMISSIONS);
        String tip = "您拒绝了权限" + MPermissionUtil.toString(deniedPermissions) + "，无法开启直播";
        Toast.makeText(LiveActivity.this, tip, Toast.LENGTH_SHORT).show();
    }

    @OnMPermissionNeverAskAgain(LIVE_PERMISSION_REQUEST_CODE)
    public void onLivePermissionDeniedAsNeverAskAgain() {
        List<String> deniedPermissions = MPermission.getDeniedPermissionsWithoutNeverAskAgain(this, LIVE_PERMISSIONS);
        List<String> neverAskAgainPermission = MPermission.getNeverAskAgainPermissions(this, LIVE_PERMISSIONS);
        StringBuilder sb = new StringBuilder();
        sb.append("无法开启直播，请到系统设置页面开启权限");
        sb.append(MPermissionUtil.toString(neverAskAgainPermission));
        if (deniedPermissions != null && !deniedPermissions.isEmpty()) {
            sb.append(",下次询问请授予权限");
            sb.append(MPermissionUtil.toString(deniedPermissions));
        }

        Toast.makeText(LiveActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
    }


    /*********************** 连麦申请/取消处理 *******************/

    /***
     * 超时
     ***/

    // 主播让观众下麦的超时
    Runnable userLeaveRunnable = new Runnable() {
        @Override
        public void run() {
            isWaiting = false;
            Toast.makeText(LiveActivity.this, "超时，请重新连麦", Toast.LENGTH_SHORT).show();
            if (currentInteractionMember != null)
                currentInteractionMember.setMicStateEnum(MicStateEnum.LEAVING);
            updateMemberListUI(nextInteractionMember, MicStateEnum.NONE);
        }
    };

    // 主播选择观众连麦的超时
    Runnable userJoinRunnable = new Runnable() {
        @Override
        public void run() {
            isWaiting = false;
            Toast.makeText(LiveActivity.this, "连麦超时", Toast.LENGTH_SHORT).show();
            if (currentInteractionMember != null)
                currentInteractionMember.setMicStateEnum(MicStateEnum.NONE);
            interactionAdapter.notifyDataSetChanged();
        }
    };

    // 显示互动布局
    private void showInteractionLayout() {
        interactionLayout.setVisibility(View.VISIBLE);
        switchInteractionUI();
    }

    /**
     * 观众申请连麦
     */
    @Override
    protected void joinQueue(CustomNotification customNotification, JSONObject json) {
        // 已经在连麦队列中，修改连麦申请的模式
        for (InteractionMember dataSource : interactionDataSource) {
            if (dataSource.getAccount().equals(customNotification.getFromAccount())) {
                if (!json.containsKey(PushLinkConstant.style)) {
                    return;
                }
                dataSource.setAvChatType(AVChatType.typeOfValue(json.getIntValue(PushLinkConstant.style)));
                interactionAdapter.notifyDataSetChanged();
                return;
            }
        }
        interactionCount++;
        saveToLocalInteractionList(customNotification.getFromAccount(), json);
        updateQueueUI();
    }

    // 主播保存互动观众
    private void saveToLocalInteractionList(String account, JSONObject jsonObject) {
        JSONObject info = (JSONObject) jsonObject.get(PushLinkConstant.info);
        String nick = info.getString(PushLinkConstant.nick);
        AVChatType style = AVChatType.typeOfValue(jsonObject.getIntValue(PushLinkConstant.style));
        if (!TextUtils.isEmpty(account)) {
            interactionDataSource.add(new InteractionMember(account, nick, AVATAR_DEFAULT, style));
        }
        interactionAdapter.notifyDataSetChanged();
    }

    // 显示互动人数
    private void updateInteractionNumbers() {
        if (interactionCount <= 0) {
            interactionCount = 0;
            interactionBtn.setText("");
            interactionBtn.setBackgroundResource(R.drawable.ic_interaction_normal);
        } else {
            interactionBtn.setBackgroundResource(R.drawable.ic_interaction_numbers);
            interactionBtn.setText(String.valueOf(interactionCount));
        }
    }

    // 有无连麦人的布局切换
    private void switchInteractionUI() {
        if (interactionCount <= 0) {
            noApplyText.setVisibility(View.VISIBLE);
            applyCountText.setVisibility(View.GONE);
            interactionDataSource.clear();
        } else {
            noApplyText.setVisibility(View.GONE);
            applyCountText.setVisibility(View.VISIBLE);
            applyCountText.setText(String.format("有%d人想要连线", interactionCount));
        }
        interactionAdapter.notifyDataSetChanged();
    }

    /**
     * 观众取消连麦申请
     */
    @Override
    protected void exitQueue(CustomNotification customNotification) {
        cancelLinkMember(customNotification.getFromAccount());
    }

    // 取消连麦申请 界面变化
    private void cancelLinkMember(String account) {
        removeCancelLinkMember(account);
        updateQueueUI();
    }

    // 移除取消连麦人员
    private void removeCancelLinkMember(String account) {
        if (interactionDataSource == null || interactionDataSource.isEmpty()) {
            return;
        }
        for (InteractionMember m : interactionDataSource) {
            if (m.getAccount().equals(account)) {
                interactionDataSource.remove(m);
                interactionCount--;
                break;
            }
        }
    }

    // 更新连麦列表和连麦人数
    private void updateQueueUI() {
        updateInteractionNumbers();
        switchInteractionUI();
    }

    /**
     * MemberLinkListener
     **/
    @Override
    public void onClick(InteractionMember member) {
        // 选择某人进行视频连线
        if (currentInteractionMember == null || currentInteractionMember.getMicStateEnum() == MicStateEnum.NONE) {
            LogUtil.d(TAG, "link status: waiting. do link");
            doLink(member);
            getHandler().postDelayed(userJoinRunnable, USER_JOIN_OVERTIME);
        } else if (currentInteractionMember.getMicStateEnum() == MicStateEnum.CONNECTING) {
            LogUtil.d(TAG, "link status: connecting. can't click");
            // 不允许点击
        } else if (currentInteractionMember.getMicStateEnum() == MicStateEnum.CONNECTED) {
            LogUtil.d(TAG, "link status: connected. do another link");
            doAnotherLink(member);
            getHandler().postDelayed(userLeaveRunnable, USER_LEAVE_OVERTIME);
        } else if (currentInteractionMember.getMicStateEnum() == MicStateEnum.LEAVING) {
            LogUtil.d(TAG, "link status: leaving. wait delay");
            currentInteractionMember.setMicStateEnum(MicStateEnum.CONNECTING);
            nextInteractionMember = member;
            updateMemberListUI(nextInteractionMember, MicStateEnum.CONNECTING);
            getHandler().postDelayed(userLeaveRunnable, USER_LEAVE_OVERTIME);
        }
    }

    // 主播选择某人连麦
    private void doLink(InteractionMember member) {
        LogUtil.d(TAG, "do link");
        if (member == null) {
            return;
        }
        isWaiting = true;
        currentInteractionMember = member;
        updateMemberListUI(currentInteractionMember, MicStateEnum.CONNECTING);

        // 发送通知告诉被选中连麦的人
        MicHelper.getInstance().sendLinkNotify(roomId, member);
    }

    // 连麦列表显示正在连麦中
    private void updateMemberListUI(InteractionMember member, MicStateEnum micStateEnum) {
        member.setMicStateEnum(micStateEnum);
        interactionAdapter.notifyDataSetChanged();
        interactionLayout.setVisibility(View.GONE);
    }

    // 主播正在连麦, 选择其他人连麦
    private void doAnotherLink(InteractionMember member) {
        nextInteractionMember = member;
        currentInteractionMember.setMicStateEnum(MicStateEnum.LEAVING);
        showLoadingLayout(nextInteractionMember);
        updateMemberListUI(nextInteractionMember, MicStateEnum.CONNECTING);
        isWaiting = true;

        // 主播先断掉正在连麦的人, 注意顺序不能改
        MicHelper.getInstance().masterBrokeMic(roomId, currentInteractionMember.getAccount());
    }

    // 显示正在连接中的等待画面
    private void showLoadingLayout(InteractionMember member) {
        audienceLoadingLayout.setVisibility(View.VISIBLE);
        connectionViewCloseBtn.setVisibility(View.VISIBLE);
        loadingClosingText.setText(R.string.video_loading);
        if (member != null) {
            onMicNick = member.getName();
            loadingNameText.setText(!TextUtils.isEmpty(onMicNick) ? member.getName() : onMicNick);
        }
    }

    // 显示连麦画面
    @Override
    protected void showConnectionView(String account, String nick, int style) {
        super.showConnectionView(account, nick, style);
        connectionViewLayout.setVisibility(View.VISIBLE);
        audienceLoadingLayout.setVisibility(View.GONE);
        livingBg.setVisibility(View.VISIBLE);
        if (liveType == LiveType.VIDEO_TYPE && style == AVChatType.VIDEO.getValue()) {
            bypassVideoRender.setVisibility(View.VISIBLE);
            audienceLivingLayout.setVisibility(View.VISIBLE);
            audioModeBypassLayout.setVisibility(View.GONE);
            AVChatManager.getInstance().setupVideoRender(account, bypassVideoRender, false, AVChatVideoScalingType.SCALE_ASPECT_BALANCED);
        } else if (style == AVChatType.AUDIO.getValue()) {
            audienceLivingLayout.setVisibility(View.GONE);
            audioModeBypassLayout.setVisibility(View.VISIBLE);
        }
    }

    // 移除互动布局中的申请连麦成员
    private void removeMemberFromList(String account) {
        currentInteractionMember.setMicStateEnum(MicStateEnum.CONNECTED);
        nextInteractionMember = null;
        cancelLinkMember(account);
    }

    /**
     * 断开连麦
     **/

    // 断开连麦
    @Override
    protected void doCloseInteraction() {
        if (currentInteractionMember == null) {
            return;
        }

        if (currentInteractionMember.getMicStateEnum() == MicStateEnum.CONNECTED) {
            MicHelper.getInstance().masterBrokeMic(roomId, currentInteractionMember.getAccount());
        } else if (currentInteractionMember.getMicStateEnum() == MicStateEnum.CONNECTING) {
            // 正在连麦中被关闭了,从显示队列中删除，并刷新数字
            isWaiting = false;
            for (InteractionMember member : interactionDataSource) {
                if (member.getAccount().equals(currentInteractionMember.getAccount())) {
                    interactionDataSource.remove(member);
                    interactionAdapter.notifyDataSetChanged();
                    interactionCount--;
                    updateInteractionNumbers();
                    break;
                }
            }
        }
        currentInteractionMember.setMicStateEnum(MicStateEnum.LEAVING);
    }

    // 隐藏旁路直播.移除内存队列
    @Override
    protected void resetConnectionView() {
        super.resetConnectionView();
        bypassVideoRender.setVisibility(View.GONE);
    }

    // 被观众拒绝
    @Override
    protected void rejectConnecting() {
        Toast.makeText(LiveActivity.this, "被观众拒绝", Toast.LENGTH_SHORT).show();
        currentInteractionMember.setMicStateEnum(MicStateEnum.NONE);
        getHandler().removeCallbacks(userJoinRunnable);
        isWaiting = false;
        if (currentInteractionMember == null) {
            return;
        }
        cancelLinkMember(currentInteractionMember.getAccount());
        resetConnectionView();
    }

    /************************
     * AVChatStateObserver
     *****************************/

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
        LogUtil.d(TAG, "onJoinedChannel: " + i);
        if (i != AVChatResCode.JoinChannelCode.OK) {
            Toast.makeText(LiveActivity.this, "加入频道失败，code:" + i, Toast.LENGTH_SHORT).show();
            showLiveFinishLayout();
        }
    }

    @Override
    public void onLeaveChannel() {

    }

    @Override
    public void onUserJoined(String s) {
        // 1、主播显示旁路直播画面
        // 2、主播发送全局自定义消息告诉观众有人连麦拉
        if (currentInteractionMember == null) {
            return;
        }

        currentInteractionMember.setMicStateEnum(MicStateEnum.CONNECTED);
        isWaiting = false;
        getHandler().removeCallbacks(userJoinRunnable);

        MicHelper.getInstance().sendConnectedMicMsg(roomId, currentInteractionMember);
        MicHelper.getInstance().updateMemberInChatRoom(roomId, currentInteractionMember);
        removeMemberFromList(s);

        if (audienceLivingLayout.getVisibility() == View.VISIBLE && currentInteractionMember.getAvChatType() == AVChatType.VIDEO) {
            // 如果是已经有连麦的人，下一个连麦人上麦，不隐藏小窗口，直接切换画面
            LogUtil.d(TAG, "another one show on screen");
            AVChatManager.getInstance().setupVideoRender(s, bypassVideoRender, false, AVChatVideoScalingType.SCALE_ASPECT_BALANCED);
            updateOnMicName(currentInteractionMember.getName());
            audienceLoadingLayout.setVisibility(View.GONE);
            livingBg.setVisibility(View.VISIBLE);
        } else {
            LogUtil.d(TAG, "show someone on screen");
            showConnectionView(s, currentInteractionMember.getName(), currentInteractionMember.getAvChatType().getValue());
        }
    }

    @Override
    public void onUserLeave(String s, int i) {
        // 连麦者离开房间
        MicHelper.getInstance().popQueue(roomId, s);
        MicHelper.getInstance().sendBrokeMicMsg(roomId, s);

        getHandler().removeCallbacks(userLeaveRunnable);
        currentInteractionMember.setMicStateEnum(MicStateEnum.NONE);

        if (isWaiting) {
            LogUtil.d(TAG, "on user leave, someone is waiting, do link");
            currentInteractionMember = nextInteractionMember;
            doLink(nextInteractionMember);
        } else {
            LogUtil.d(TAG, "on user leave, do close view");
            doCloseInteractionView();
        }
    }

    @Override
    public void onProtocolIncompatible(int i) {

    }

    @Override
    public void onDisconnectServer() {
        MicHelper.getInstance().leaveChannel();
        NIMClient.getService(ChatRoomService.class).exitChatRoom(roomId);
        clearChatRoom();
    }

    private int networkQuality = -1;

    @Override
    public void onNetworkQuality(String s, int i) {
        if (liveType != LiveType.VIDEO_TYPE) {
            return;
        }
        if (s.equals(DemoCache.getAccount()) && s.equals(roomInfo.getCreator())) {
            if (networkQuality == -1) {
                networkQuality = i;
            }

            netStateImage.setVisibility(View.VISIBLE);
            switch (networkQuality) {
                case AVChatNetworkQuality.BAD:
                    netStateTipText.setText(R.string.network_bad);
                    netStateImage.setImageResource(R.drawable.ic_network_bad);
                    netOperateText.setVisibility(View.VISIBLE);
                    netOperateText.setText(R.string.switch_to_audio_live);
                    break;
                case AVChatNetworkQuality.POOR:
                    netStateTipText.setText(R.string.network_poor);
                    netStateImage.setImageResource(R.drawable.ic_network_poor);
                    AVChatParameters avChatParameters = new AVChatParameters();
                    avChatParameters.requestKey(AVChatParameters.KEY_VIDEO_QUALITY);
                    AVChatParameters parameters = AVChatManager.getInstance().getParameters(avChatParameters);
                    int quality = parameters.getInteger(AVChatParameters.KEY_VIDEO_QUALITY);
                    if (quality == AVChatVideoQuality.QUALITY_720P) {
                        netOperateText.setVisibility(View.VISIBLE);
                        netOperateText.setText(R.string.reduce_live_clarity);
                    } else {
                        netOperateText.setVisibility(View.GONE);
                    }
                    break;
                case AVChatNetworkQuality.GOOD:
                    netStateTipText.setText(R.string.network_good);
                    netStateImage.setImageResource(R.drawable.ic_network_good);
                    netOperateText.setVisibility(View.GONE);
                    break;
                case AVChatNetworkQuality.EXCELLENT:
                    netStateTipText.setText(R.string.network_excellent);
                    netStateImage.setImageResource(R.drawable.ic_network_excellent);
                    netOperateText.setVisibility(View.GONE);
                    break;
            }

            networkQuality = i;
        }
    }

    @Override
    public void onCallEstablished() {
        // 设置自己的画布
        LogUtil.d(TAG, "onCallEstablished");
        if (liveType == LiveType.VIDEO_TYPE) {
            AVChatManager.getInstance().setupVideoRender(DemoCache.getAccount(), videoRender, false, AVChatVideoScalingType.SCALE_ASPECT_BALANCED);
        }
    }

    @Override
    public void onDeviceEvent(int i, String s) {

    }

    @Override
    public void onFirstVideoFrameRendered(String s) {
        LogUtil.d(TAG, "onFirstVideoFrameRendered, account:" + s);
        if (!s.equals(DemoCache.getAccount())) {
            livingBg.setVisibility(View.GONE);
        }
    }

    @Override
    public void onVideoFrameResolutionChanged(String s, int i, int i1, int i2) {

    }

    @Override
    public int onVideoFrameFilter(AVChatVideoFrame avChatVideoFrame) {
        LogUtil.i(TAG, "on video frame filter, avchatVideoFrame:" + avChatVideoFrame + ", gpuEffect:" + mGPUEffect);
        if (mGPUEffect == null) {
            return -1;
        }
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
        switch (i) {
            case AVChatAudioMixingEvent.MIXING_FINISHED:
                resetMusicLayoutViews(true, true);
                break;
            case AVChatAudioMixingEvent.MIXING_ERROR:

                break;
        }
    }

    /************************ AVChatStateObserver end *****************************/
}

