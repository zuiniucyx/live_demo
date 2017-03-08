package com.netease.nim.chatroom.demo.entertainment.helper;

import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.chatroom.demo.DemoCache;
import com.netease.nim.chatroom.demo.base.util.log.LogUtil;
import com.netease.nim.chatroom.demo.entertainment.constant.MicStateEnum;
import com.netease.nim.chatroom.demo.entertainment.constant.PushLinkConstant;
import com.netease.nim.chatroom.demo.entertainment.constant.PushMicNotificationType;
import com.netease.nim.chatroom.demo.entertainment.http.ChatRoomHttpClient;
import com.netease.nim.chatroom.demo.entertainment.model.InteractionMember;
import com.netease.nim.chatroom.demo.entertainment.module.ConnectedAttachment;
import com.netease.nim.chatroom.demo.entertainment.module.DisconnectAttachment;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.avchat.AVChatCallback;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.constant.AVChatType;
import com.netease.nimlib.sdk.avchat.model.AVChatData;
import com.netease.nimlib.sdk.avchat.model.AVChatOptionalConfig;
import com.netease.nimlib.sdk.chatroom.ChatRoomMessageBuilder;
import com.netease.nimlib.sdk.chatroom.ChatRoomService;
import com.netease.nimlib.sdk.chatroom.model.ChatRoomMessage;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.CustomNotification;

/**
 * Created by hzxuwen on 2016/7/26.
 */
public class MicHelper {

    private static final String TAG = MicHelper.class.getSimpleName();

    public static MicHelper getInstance() {
        return InstanceHolder.instance;
    }

    public interface ChannelCallback {
        void onJoinChannelSuccess();
        void onJoinChannelFailed();
    }

    // 主播断开连麦
    public void masterBrokeMic(String roomId, String account) {
        // 主播通知连麦者断开
        sendCustomNotify(roomId, account, PushMicNotificationType.DISCONNECT_MIC.getValue(), null, false);
    }

    // 连麦者主动断开连麦
    public void audienceBrokeMic(String roomId, String account) {
        leaveChannel();
    }

    // 去应用服务器移除队列
    public void popQueue(String roomId, String account) {
        ChatRoomHttpClient.getInstance().popMicLink(roomId, account, new ChatRoomHttpClient.ChatRoomHttpCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                LogUtil.d(TAG, "pop queue success");
            }

            @Override
            public void onFailed(int code, String errorMsg) {
                LogUtil.d(TAG, "pop queue failed, code:" + code + ", errorMsg:" + errorMsg);
                Toast.makeText(DemoCache.getContext(), "pop queque failed, errorMsg:" + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 发送正在连麦通知
    public void sendLinkNotify(String roomId, InteractionMember member) {
        JSONObject json = new JSONObject();
        json.put(PushLinkConstant.style, member.getAvChatType().getValue());
        MicHelper.getInstance().sendCustomNotify(roomId, member.getAccount(), PushMicNotificationType.CONNECTING_MIC.getValue(), json, true);
    }

    public void sendCustomNotify(String roomId, String toAccount, final int command, JSONObject json, boolean isSendOnline) {
        CustomNotification notification = new CustomNotification();
        notification.setSessionId(toAccount);
        notification.setSessionType(SessionTypeEnum.P2P);

        if (json == null) {
            json = new JSONObject();
        }
        json.put(PushLinkConstant.roomid, roomId);
        json.put(PushLinkConstant.command, command);
        notification.setContent(json.toString());
        notification.setSendToOnlineUserOnly(isSendOnline);
        NIMClient.getService(MsgService.class).sendCustomNotification(notification).setCallback(new RequestCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                LogUtil.d(TAG, "send custom type:" + command);
            }

            @Override
            public void onFailed(int i) {
                LogUtil.d(TAG, "send custom notify failed, code:" + i);
            }

            @Override
            public void onException(Throwable throwable) {

            }
        });
    }

    // 连麦者成功连上麦后，主播全局通知
    public void sendConnectedMicMsg(String roomId, InteractionMember member) {
        if (member != null) {
            ConnectedAttachment attachment = new ConnectedAttachment(member.getAccount(), member.getName(), member.getAvatar(), member.getAvChatType().getValue());
            ChatRoomMessage message = ChatRoomMessageBuilder.createChatRoomCustomMessage(roomId, attachment);
            NIMClient.getService(ChatRoomService.class).sendMessage(message, false);
        }
    }

    // 发送断开连麦自定义消息通知全局
    public void sendBrokeMicMsg(String roomId, String account) {
        DisconnectAttachment attachment = new DisconnectAttachment(account);
        ChatRoomMessage message = ChatRoomMessageBuilder.createChatRoomCustomMessage(roomId, attachment);
        NIMClient.getService(ChatRoomService.class).sendMessage(message, false);
    }

    // 更新成员连麦状态
    public void updateMemberInChatRoom(String roomId, InteractionMember member) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(PushLinkConstant.style, member.getAvChatType().getValue());
        jsonObject.put(PushLinkConstant.state, MicStateEnum.CONNECTED.getValue());

        JSONObject info = new JSONObject();
        info.put(PushLinkConstant.nick, member.getName());
        info.put(PushLinkConstant.avatar, member.getAvatar());

        jsonObject.put(PushLinkConstant.info, info);

        NIMClient.getService(ChatRoomService.class).updateQueue(roomId, member.getAccount(), jsonObject.toString()).setCallback(new RequestCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                LogUtil.d(TAG, "update queue success");
            }

            @Override
            public void onFailed(int i) {
                LogUtil.d(TAG, "update queue failed, code:" + i);
            }

            @Override
            public void onException(Throwable throwable) {

            }
        });
    }

    /**************************** 音视频房间操作 **********************************/

    public void joinChannel(AVChatOptionalConfig avChatOptionalConfig, String meetingName, boolean isVideo, final ChannelCallback callback) {
        AVChatManager.getInstance().joinRoom(meetingName, isVideo ? AVChatType.VIDEO : AVChatType.AUDIO, avChatOptionalConfig, new AVChatCallback<AVChatData>() {
            @Override
            public void onSuccess(AVChatData avChatData) {
                LogUtil.d(TAG, "join channel success");
                callback.onJoinChannelSuccess();
            }

            @Override
            public void onFailed(int i) {
                LogUtil.e(TAG, "join channel failed, code:" + i);
                Toast.makeText(DemoCache.getContext(), "join channel failed, code:" + i, Toast.LENGTH_SHORT).show();
                callback.onJoinChannelFailed();
            }

            @Override
            public void onException(Throwable throwable) {

            }
        });
    }

    // 离开音视频房间
    public void leaveChannel() {
        AVChatManager.getInstance().leaveRoom(new AVChatCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                LogUtil.d(TAG, "leave channel success");
            }

            @Override
            public void onFailed(int i) {
                LogUtil.d(TAG, "leave channel failed, code:" + i);
            }

            @Override
            public void onException(Throwable throwable) {

            }
        });
    }

    /**
     * ************************************ 单例 ***************************************
     */
    static class InstanceHolder {
        final static MicHelper instance = new MicHelper();
    }
}
