package com.netease.nim.chatroom.zyly.zyly;

public class HttpResponseBean {
	 private String status;
	    private String errcode;
	    private String errmessage;
	    /**status 0表示调用接口成功
	     * errcode 0表示数据正确传输
	     *  errcode 1 表示 手机号码不存在
	     *  errcode 2 表示 手机短信验证码不正确
	     *  errcode 3 表示 手机短信验证码失效
	     *  errcode 4 表示 手机号码存在
	     *  errcode 5 表示 两次输入密码不一致
	     *  errmessage 用户id
	     * */
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public String getErrcode() {
			return errcode;
		}
		public void setErrcode(String errcode) {
			this.errcode = errcode;
		}
		public String getErrmessage() {
			return errmessage;
		}
		public void setErrmessage(String errmessage) {
			this.errmessage = errmessage;
		}
	    
}
