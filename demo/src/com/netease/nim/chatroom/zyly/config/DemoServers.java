package com.netease.nim.chatroom.zyly.config;

/**
 * 云信Demo应用服务器地址（第三方APP请不要使用）
 */
public class DemoServers {
    public static final String URL_REGIST="http://10.0.0.10:8080/videoconfere/app/appRegister?phonenumber=";
    public static final String URL_LOGIN="http://10.0.0.10:8080/videoconfere/app/applogin?phonenumber=";
    public static final String masterEnterRoom_url="http://10.0.0.10:8080/videoconfere/app/updateMeetingName?roomid=";
    public static final String pull_url="http://10.0.0.10:8080/videoconfere/app/getHttpPullUrl?roomid=";
//    private static final String API_SERVER_TEST = "http://223.252.220.238:8080/api/"; // 测试
//    private static final String API_SERVER = "https://app.netease.im/api/"; // 线上
    private static final String API_SERVER = "10.0.0.10:8080/videoconfere/app/"; // 线上

    public static final String apiServer() {
//        return ServerConfig.testServer() ? API_SERVER_TEST : API_SERVER;
        return API_SERVER;
    }

    public static final String chatRoomAPIServer() {
//        return apiServer() + "chatroom/";
        return API_SERVER + "chatroom/";
    }
}
