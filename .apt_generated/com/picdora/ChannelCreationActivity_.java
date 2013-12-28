//
// DO NOT EDIT THIS FILE, IT HAS BEEN GENERATED USING AndroidAnnotations.
//


package com.picdora;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import com.googlecode.androidannotations.api.SdkVersionHelper;
import com.picdora.R.id;
import com.picdora.R.layout;

public final class ChannelCreationActivity_
    extends ChannelCreationActivity
{


    @Override
    public void onCreate(Bundle savedInstanceState) {
        init_(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_channel_creation);
    }

    private void init_(Bundle savedInstanceState) {
        categoryListAdapter = CategoryListAdapter_.getInstance_(this);
    }

    private void afterSetContentView_() {
        categoryList = ((ListView) findViewById(id.categoryList));
        channelName = ((EditText) findViewById(id.channelName));
        gifSetting = ((RadioGroup) findViewById(id.gifSetting));
        {
            View view = findViewById(id.createButton);
            if (view!= null) {
                view.setOnClickListener(new OnClickListener() {


                    @Override
                    public void onClick(View view) {
                        ChannelCreationActivity_.this.createButtonClicked();
                    }

                }
                );
            }
        }
        ((CategoryListAdapter_) categoryListAdapter).afterSetContentView_();
        initViews();
        initClickListener();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        afterSetContentView_();
    }

    @Override
    public void setContentView(View view, LayoutParams params) {
        super.setContentView(view, params);
        afterSetContentView_();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        afterSetContentView_();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (((SdkVersionHelper.getSdkInt()< 5)&&(keyCode == KeyEvent.KEYCODE_BACK))&&(event.getRepeatCount() == 0)) {
            onBackPressed();
        }
        return super.onKeyDown(keyCode, event);
    }

    public static ChannelCreationActivity_.IntentBuilder_ intent(Context context) {
        return new ChannelCreationActivity_.IntentBuilder_(context);
    }

    public static class IntentBuilder_ {

        private Context context_;
        private final Intent intent_;

        public IntentBuilder_(Context context) {
            context_ = context;
            intent_ = new Intent(context, ChannelCreationActivity_.class);
        }

        public Intent get() {
            return intent_;
        }

        public ChannelCreationActivity_.IntentBuilder_ flags(int flags) {
            intent_.setFlags(flags);
            return this;
        }

        public void start() {
            context_.startActivity(intent_);
        }

        public void startForResult(int requestCode) {
            if (context_ instanceof Activity) {
                ((Activity) context_).startActivityForResult(intent_, requestCode);
            } else {
                context_.startActivity(intent_);
            }
        }

    }

}
