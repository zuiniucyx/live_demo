package com.netease.nim.chatroom.zyly.im.session.emoji;

public interface IEmoticonSelectedListener {
	void onEmojiSelected(String key);

	void onStickerSelected(String categoryName, String stickerName);
}
