//
// DO NOT EDIT THIS FILE, IT HAS BEEN GENERATED USING AndroidAnnotations 3.0.
//


package com.picdora.player;

import android.content.Context;
import com.picdora.models.Channel;
import com.picdora.player.ChannelPlayer.OnReadyListener;
import org.androidannotations.api.BackgroundExecutor;

public final class ChannelPlayer_
    extends ChannelPlayer
{

    private Context context_;

    private ChannelPlayer_(Context context) {
        context_ = context;
        init_();
    }

    public static ChannelPlayer_ getInstance_(Context context) {
        return new ChannelPlayer_(context);
    }

    private void init_() {
    }

    public void rebind(Context context) {
        context_ = context;
        init_();
    }

    @Override
    public void loadChannel(final Channel channel, final OnReadyListener listener) {
        BackgroundExecutor.execute(new BackgroundExecutor.Task("", 0, "") {


            @Override
            public void execute() {
                try {
                    ChannelPlayer_.super.loadChannel(channel, listener);
                } catch (Throwable e) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }

        }
        );
    }

    @Override
    public void loadMoreImagesIfNeeded(final int index) {
        BackgroundExecutor.execute(new BackgroundExecutor.Task("", 0, "loadImagesInBackground") {


            @Override
            public void execute() {
                try {
                    ChannelPlayer_.super.loadMoreImagesIfNeeded(index);
                } catch (Throwable e) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }

        }
        );
    }

}
