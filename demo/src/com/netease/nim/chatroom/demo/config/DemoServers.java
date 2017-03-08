package com.netease.nim.chatroom.demo.config;

/**
 * 云信Demo应用服务器地址（第三方APP请不要使用）
 */
public class DemoServers {

    private static final String API_SERVER_TEST = "http://223.252.220.238:8080/api/"; // 测试
    private static final String API_SERVER = "https://app.netease.im/api/"; // 线上

    public static final String apiServer() {
        return ServerConfig.testServer() ? API_SERVER_TEST : API_SERVER;
    }

    public static final String chatRoomAPIServer() {
        return apiServer() + "chatroom/";
    }
}
