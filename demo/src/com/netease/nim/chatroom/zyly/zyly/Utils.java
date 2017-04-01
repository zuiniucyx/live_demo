package com.netease.nim.chatroom.zyly.zyly;

import java.security.MessageDigest;

/**
 * Created by cyx on 17/3/27.
 */

public class Utils {
    // 字符串的MD5
    public static String stringMD5(String message) {
        String md5str = "";
        try {
            // 创建一个提供信息摘要算法的对象，初始化为md5算法对象
            MessageDigest md = MessageDigest.getInstance("MD5");

//			message = message.substring(0, 1) + ":)" + message.substring(2, 3)
//					+ "(:" + message.substring(4, message.length());

            // 将消息转化为byte数组
            byte[] input = message.getBytes();

            // 计算后获得字节数组，这就是那128位了
            byte[] buff = md.digest(input);

            // 把数组每一个字节（一个字节占八位）换成16进制连成md5字符串
            md5str = bytesToHex(buff);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return md5str;
    }
    /**
     * 二进制转十六进制
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuffer md5str = new StringBuffer();
        // 把数组每一字节转换成16进制连成md5字符串
        int digital;
        for (int i = 0; i < bytes.length; i++) {
            digital = bytes[i];
            if (digital < 0) {
                digital += 256;
            }
            if (digital < 16) {
                md5str.append("0");
            }
            md5str.append(Integer.toHexString(digital));
        }
        return md5str.toString().toUpperCase();
    }
}
