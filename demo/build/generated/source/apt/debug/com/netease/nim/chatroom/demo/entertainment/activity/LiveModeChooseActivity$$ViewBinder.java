// Generated code from Butter Knife. Do not modify!
package com.netease.nim.chatroom.demo.entertainment.activity;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class LiveModeChooseActivity$$ViewBinder<T extends com.netease.nim.chatroom.demo.entertainment.activity.LiveModeChooseActivity> implements ViewBinder<T> {
  @Override public void bind(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131427575, "field 'videoLiveLayout' and method 'onClick'");
    target.videoLiveLayout = finder.castView(view, 2131427575, "field 'videoLiveLayout'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onClick(p0);
        }
      });
    view = finder.findRequiredView(source, 2131427577, "field 'audioLiveLayout' and method 'onClick'");
    target.audioLiveLayout = finder.castView(view, 2131427577, "field 'audioLiveLayout'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onClick(p0);
        }
      });
  }

  @Override public void unbind(T target) {
    target.videoLiveLayout = null;
    target.audioLiveLayout = null;
  }
}
