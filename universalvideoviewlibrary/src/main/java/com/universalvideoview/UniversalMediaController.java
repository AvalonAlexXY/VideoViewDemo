/*
* Copyright (C) 2015 Author <dictfb#gmail.com>
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package com.universalvideoview;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;

public class UniversalMediaController extends FrameLayout {


    private MediaPlayerControl mPlayer;

    private Context mContext;

    private ProgressBar mProgress;

    private TextView mEndTime, mCurrentTime;

    private TextView mTitle;

    private boolean mShowing = true;

    private boolean mDragging;

    private boolean mScalable = false;
    private boolean mIsFullScreen = false;
//    private boolean mFullscreenEnabled = false;


    private static final int sDefaultTimeout = 3000;

    private static final int STATE_PLAYING = 1;
    private static final int STATE_PAUSE = 2;
    private static final int STATE_LOADING = 3;
    private static final int STATE_ERROR = 4;
    private static final int STATE_COMPLETE = 5;

    private int mState = -1;


    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private static final int SHOW_LOADING = 3;
    private static final int HIDE_LOADING = 4;
    private static final int SHOW_ERROR = 5;
    private static final int HIDE_ERROR = 6;
    private static final int SHOW_COMPLETE = 7;
    private static final int HIDE_COMPLETE = 8;
    StringBuilder mFormatBuilder;

    Formatter mFormatter;

    private ImageButton mTurnButton;// 开启暂停按钮

    private ImageButton mScaleButton;

    private View mBackButton;// 返回按钮

    private ViewGroup loadingLayout;

    private ViewGroup errorLayout;

    private View mTitleLayout;
    private View mControlLayout;

    private View mCenterPlayButton;
    private TextView tv_time;
    private LinearLayout center_move;
    private ImageView iv_move;
    private TextView tv_move_time;
    private TextView tv_totla_time;
    private LinearLayout light_view;
    private SeekBar light_seekbar;
    private int currentBrightness;
    private int brightness;
    private int voiceness;
    private LinearLayout voice_view;
    private SeekBar voice_seekbar;
    private int voiceMax;
    private int currentVoice;
    private AudioManager mAudioManager;
    private ImageView iv_voice;

    public UniversalMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.UniversalMediaController);
        mScalable = a.getBoolean(R.styleable.UniversalMediaController_uvv_scalable, false);
        a.recycle();
        init(context);
    }

    public UniversalMediaController(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewRoot = inflater.inflate(R.layout.uvv_player_controller, this);
        viewRoot.setOnTouchListener(mTouchListener);
        initControllerView(viewRoot);
    }


    private void initControllerView(View v) {
        mTitleLayout = v.findViewById(R.id.title_part);
        mControlLayout = v.findViewById(R.id.control_layout);
        loadingLayout = (ViewGroup) v.findViewById(R.id.loading_layout);
        errorLayout = (ViewGroup) v.findViewById(R.id.error_layout);
        mTurnButton = (ImageButton) v.findViewById(R.id.turn_button);
        mScaleButton = (ImageButton) v.findViewById(R.id.scale_button);
        mCenterPlayButton = v.findViewById(R.id.center_play_btn);
        mBackButton = v.findViewById(R.id.back_btn);
        tv_time = (TextView) v.findViewById(R.id.tv_time);
        center_move = (LinearLayout) v.findViewById(R.id.center_move);
        //快速移动
        iv_move = (ImageView) v.findViewById(R.id.iv_move);
        tv_move_time = (TextView) v.findViewById(R.id.tv_move_time);
        tv_totla_time = (TextView) v.findViewById(R.id.tv_total_time);
        //音频
        voice_view = (LinearLayout) v.findViewById(R.id.voice_view);
        voice_seekbar = (SeekBar) v.findViewById(R.id.voice_seekbar);
        iv_voice = (ImageView) v.findViewById(R.id.iv_voice);
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        voiceMax = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_MUSIC );
        currentVoice = mAudioManager.getStreamVolume( AudioManager.STREAM_MUSIC );
        voice_seekbar.setMax(voiceMax);
        voice_seekbar.setProgress(currentVoice);
        //亮度
        light_view = (LinearLayout) v.findViewById(R.id.light_view);
        light_seekbar = (SeekBar) v.findViewById(R.id.light_seekbar);
        light_seekbar.setMax(255);
        currentBrightness = getSystemBrightness();
        light_seekbar.setProgress(currentBrightness);

        if (mTurnButton != null) {
            mTurnButton.requestFocus();
            mTurnButton.setOnClickListener(mPauseListener);
        }

        if (mScalable) {
            if (mScaleButton != null) {
                mScaleButton.setVisibility(VISIBLE);
                mScaleButton.setOnClickListener(mScaleListener);
            }
        } else {
            if (mScaleButton != null) {
                mScaleButton.setVisibility(GONE);
            }
        }

        if (mCenterPlayButton != null) {//重新开始播放
            mCenterPlayButton.setOnClickListener(mCenterPlayListener);
        }

        if (mBackButton != null) {//返回按钮仅在全屏状态下可见
            mBackButton.setOnClickListener(mBackListener);
        }

        View bar = v.findViewById(R.id.seekbar);
        mProgress = (ProgressBar) bar;
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }

        mEndTime = (TextView) v.findViewById(R.id.duration);
        mCurrentTime = (TextView) v.findViewById(R.id.has_played);
        mTitle = (TextView) v.findViewById(R.id.title);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        setSystemTime();
    }

    /**
     * 获取屏幕亮度
     * @return （0 - 255） 屏幕亮度值
     */
    private int getSystemBrightness() {
        int systemBrightness = 0;
        Activity activity = (Activity) mContext;
        try {
            systemBrightness = Settings.System.getInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return systemBrightness;
    }

    /**
     * 改变App当前Window亮度
     *
     * @param brightness
     */
    public void changeAppBrightness(int brightness) {
        Activity activity =  (Activity) mContext;
        Window window = activity.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        if (brightness == -1) {
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        } else {
            lp.screenBrightness = (brightness <= 0 ? 1 : brightness) / 255f;
        }
        window.setAttributes(lp);
    }

    private void setSystemTime() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        String hourStr = hour<10?"0" + hour : hour+"";
        String minuteStr = minute<10?"0" + minute : minute+"";
        tv_time.setText(hourStr + ":" + minuteStr);
    }


    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(sDefaultTimeout);
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        try {
            if (mTurnButton != null && mPlayer != null && !mPlayer.canPause()) {
                mTurnButton.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     *
     * @param timeout The timeout in milliseconds. Use 0 to show
     *                the controller until hide() is called.
     */
    public void show(int timeout) {//只负责上下两条bar的显示,不负责中央loading,error,playBtn的显示.
        if (!mShowing) {
            setProgress();
            if (mTurnButton != null) {
                mTurnButton.requestFocus();
            }
            disableUnsupportedButtons();
            mShowing = true;
        }
        updatePausePlay();
        updateBackButton();
        updateTitleBar();

        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        }

        if (mControlLayout.getVisibility() != VISIBLE) {
            mControlLayout.setVisibility(VISIBLE);
        }

        setSystemTime();
        // cause the progress bar to be updated even if mShowing
        // was already true. This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    public boolean isShowing() {
        return mShowing;
    }


    public void hide() {//只负责上下两条bar的隐藏,不负责中央loading,error,playBtn的隐藏
        if (mShowing) {
            mHandler.removeMessages(SHOW_PROGRESS);
            mTitleLayout.setVisibility(GONE);
            mControlLayout.setVisibility(GONE);
            mShowing = false;
        }
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
                case FADE_OUT: //1
                    hide();
                    break;
                case SHOW_PROGRESS: //2
                    pos = setProgress();
                    if (!mDragging && mShowing && mPlayer != null && mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
                case SHOW_LOADING: //3
                    show();
                    showCenterView(R.id.loading_layout);
                    mState = SHOW_LOADING;
                    break;
                case SHOW_COMPLETE: //7
                    showCenterView(R.id.center_play_btn);
                    break;
                case SHOW_ERROR: //5
                    show();
                    showCenterView(R.id.error_layout);
                    break;
                case HIDE_LOADING: //4
                case HIDE_ERROR: //6
                case HIDE_COMPLETE: //8
                    hide();
                    hideCenterView();
                    mState = HIDE_LOADING;
                    break;
            }
        }
    };

    private void showCenterView(int resId) {
        if (resId == R.id.loading_layout) {
            if (loadingLayout.getVisibility() != VISIBLE) {
                loadingLayout.setVisibility(VISIBLE);
            }
            if (mCenterPlayButton.getVisibility() == VISIBLE) {
                mCenterPlayButton.setVisibility(GONE);
            }
            if (errorLayout.getVisibility() == VISIBLE) {
                errorLayout.setVisibility(GONE);
            }
        } else if (resId == R.id.center_play_btn) {
            if (mCenterPlayButton.getVisibility() != VISIBLE) {
                mCenterPlayButton.setVisibility(VISIBLE);
            }
            if (loadingLayout.getVisibility() == VISIBLE) {
                loadingLayout.setVisibility(GONE);
            }
            if (errorLayout.getVisibility() == VISIBLE) {
                errorLayout.setVisibility(GONE);
            }

        } else if (resId == R.id.error_layout) {
            if (errorLayout.getVisibility() != VISIBLE) {
                errorLayout.setVisibility(VISIBLE);
            }
            if (mCenterPlayButton.getVisibility() == VISIBLE) {
                mCenterPlayButton.setVisibility(GONE);
            }
            if (loadingLayout.getVisibility() == VISIBLE) {
                loadingLayout.setVisibility(GONE);
            }

        }
    }


    private void hideCenterView() {
        if (mCenterPlayButton.getVisibility() == VISIBLE) {
            mCenterPlayButton.setVisibility(GONE);
        }
        if (errorLayout.getVisibility() == VISIBLE) {
            errorLayout.setVisibility(GONE);
        }
        if (loadingLayout.getVisibility() == VISIBLE) {
            loadingLayout.setVisibility(GONE);
        }
    }

    public void reset() {
        mCurrentTime.setText("00:00");
        mEndTime.setText("/00:00");
        mProgress.setProgress(0);
        mTurnButton.setImageResource(R.drawable.video_play);
        setVisibility(View.VISIBLE);
        hideLoading();
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null)
            mEndTime.setText("/" + stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));

        return position;
    }

    //todo
    private static final int MOVE_DEFAULT = -1,
            MOVE_PROGRESS = 10010,MOVE_LIGHT = 10011,MOVE_VOICE = 10012;
    int newposition = 0;
    int moveState = -1;
    float downX = 0, downY = 0, moveX = 0, moveY = 0,lastX = 0,lastY;
    boolean movingProgress = false,movingLight = false ,movingVoice = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                show(0); // show until hide is called
                handled = false;
                if(mIsFullScreen){
                    //获取当前点击的x，y坐标
                    downX = event.getX();
                    downY = event.getY();
                    lastX = downX;
                    lastY = downY;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(mIsFullScreen){
                    //获取移动的时候的x,y坐标
                    Log.i("move","移动的x坐标" + moveX);
                    Log.i("moveY","移动的y坐标" + moveY);
                    moveX = event.getX();
                    moveY = event.getY();

                    if(moveState == MOVE_DEFAULT){
                        if (Math.abs(moveX - downX) - Math.abs(moveY - downY) > 50 && downX>getWidth()/4  && downX<getWidth()/4*3){
                            moveState = MOVE_PROGRESS ;
                        }else if (Math.abs(moveX - downX) - Math.abs(moveY - downY)<50 && downX<getWidth()/4){
                            moveState = MOVE_LIGHT;
                        }else if(Math.abs(moveX - downX) - Math.abs(moveY - downY)<50 && downX>getWidth()/4*3) {
                            moveState = MOVE_VOICE;
                        }
                    }

                    //按照x坐标走动
                    if (!movingLight && !movingVoice && mState != SHOW_LOADING
                            && moveState==MOVE_PROGRESS) {
                        movingProgress = true;
                        int movedX = (int) (moveX - downX);

                        long duration = mPlayer.getDuration();
                        long nowPositon = mPlayer.getCurrentPosition();
                        newposition = (int) ((int) nowPositon + movedX*duration/getWidth()/2);

                        mPlayer.pause();
                        setImageViewCenter(moveX-lastX);
                        updateMovedText(newposition,duration);
                    }

                    //亮度设置
                    if (!movingProgress && !movingVoice && moveState==MOVE_LIGHT) {
                        movingLight = true;
                        upateLightView(true);
                        int movedY = (int) ( downY - moveY );
                        brightness = currentBrightness  + movedY/10;
                        if(brightness>255){
                            brightness = 255;
                        }else if(brightness<=0){
                            brightness = 0;
                        }
                        Log.i("brightness","当前设置的亮度" + brightness);

                        changeAppBrightness(brightness);
                        updateLightSeekBar(brightness);
                    }

                    //音频设置
                    if(!movingProgress && !movingLight && moveState == MOVE_VOICE){
                        movingVoice = true;
                        updateVoiceView(true);

                        int movedY =  (int) ( downY - moveY );
                        voiceness = currentVoice  + movedY/30;
                        if(voiceness>voiceMax){
                            voiceness = voiceMax;
                        }else if(voiceness<=0){
                            voiceness = 0;
                        }
                        Log.i("voiceness","当前设置的声音" + voiceness);

                        changeAppVoiceness(voiceness);
                        updateVoiceSeekBar(voiceness);
                    }

                    lastX = moveX;
                    lastY = moveY;
                }
                break;
            case MotionEvent.ACTION_UP:
                moveState = MOVE_DEFAULT;

                if (!handled) {
                    handled = false;
                    show(sDefaultTimeout); // start timeout
                }

                //亮度按钮去除
                if(movingLight && isFullScreen()){
                    currentBrightness = brightness;
                    upateLightView(false);
                    movingLight = false;
                }

                //音频设置
                if(movingVoice && mIsFullScreen){
                    currentVoice = voiceness;
                    updateVoiceView(false);
                    movingVoice = false;
                }

                //视频横轴移动位置
                if(mPlayer!=null && movingProgress && isFullScreen()){
                    mPlayer.start();
                    center_move.setVisibility(GONE);
                    mPlayer.seekTo(newposition);
                    if (mCurrentTime != null) {
                        mCurrentTime.setText(stringForTime(newposition));
                    }

                    setProgress();
                    updatePausePlay();
                    show(sDefaultTimeout);

                    movingProgress = false;
                    mShowing = true;
                    mHandler.sendEmptyMessage(SHOW_PROGRESS);
                }

                break;
            case MotionEvent.ACTION_CANCEL:
                hide();
                break;
            default:
                break;
        }
        return true;
    }

    private void updateVoiceSeekBar(int voiceness) {
        if(voiceness>0 && voiceness < voiceMax){
            voice_seekbar.setProgress(voiceness);
        }else if(voiceness < 0){
            voice_seekbar.setProgress(0);
        }else if(voiceness >= voiceMax){
            voice_seekbar.setProgress(voiceMax);
        }
    }

    /**
     * 改变app音量
     * @param voiceness
     */
    private void changeAppVoiceness(int voiceness) {
        iv_voice.setImageResource(R.drawable.video_volume);
        if(voiceness>voiceMax){
            voiceness = voiceMax;
        }else  if(voiceness <= 0){
            voiceness = 0;
            iv_voice.setImageResource(R.drawable.video_mute);
        }
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, voiceness, AudioManager.FLAG_PLAY_SOUND);
    }

    private void updateVoiceView(boolean isVoiceShow) {
        if(isVoiceShow){
            voice_view.setVisibility(VISIBLE);
        }else{
            voice_view.setVisibility(GONE);
        }
    }

    private void updateLightSeekBar(int brightness) {
        if(brightness>0 && brightness < 256){
            light_seekbar.setProgress(brightness);
        }else if(brightness < 0){
            light_seekbar.setProgress(0);
        }else if(brightness >= 256){
            light_seekbar.setProgress(255);
        }
    }

    private void upateLightView(boolean isLightShow) {
        if(isLightShow){
            light_view.setVisibility(VISIBLE);
        }else{
            light_view.setVisibility(GONE);
        }
    }

    private void updateMovedText(int newposition, long duration) {
        if(newposition>0 && newposition<duration)
            tv_move_time.setText(stringForTime(newposition));
        else if(newposition > duration)
            tv_move_time.setText(stringForTime((int) duration));

        if(duration>0)
            tv_totla_time.setText("/" + stringForTime((int) duration));
    }

    private void setImageViewCenter(float offsetX) {
        center_move.setVisibility(VISIBLE);
        if(offsetX>0){
            iv_move.setImageResource(R.drawable.video_fast);
        }else{
            iv_move.setImageResource(R.drawable.video_rewind);
        }
    }

    boolean handled = false;
    //如果正在显示,则使之消失
    private OnTouchListener mTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (mShowing) {
                    hide();
                    handled = true;
                    return true;
                }
            }
            return false;
        }
    };

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(sDefaultTimeout);
                if (mTurnButton != null) {
                    mTurnButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
                || keyCode == KeyEvent.KEYCODE_CAMERA) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        show(sDefaultTimeout);
        return super.dispatchKeyEvent(event);
    }

    private OnClickListener mPauseListener = new OnClickListener() {
        public void onClick(View v) {
            if (mPlayer != null) {
                doPauseResume();
                show(sDefaultTimeout);
            }
        }
    };

    private OnClickListener mScaleListener = new OnClickListener() {
        public void onClick(View v) {
            mIsFullScreen = !mIsFullScreen;
            updateScaleButton();
            updateBackButton();
            updateTitleBar();
            mPlayer.setFullscreen(mIsFullScreen);
        }
    };

    //仅全屏时才有返回按钮
    private OnClickListener mBackListener = new OnClickListener() {
        public void onClick(View v) {
            if (mIsFullScreen) {
                mIsFullScreen = false;
                updateScaleButton();
                updateBackButton();
                updateTitleBar();
                mPlayer.setFullscreen(false);
            }
        }
    };

    private OnClickListener mCenterPlayListener = new OnClickListener() {
        public void onClick(View v) {
            hideCenterView();
            mPlayer.start();
        }
    };

    private void updatePausePlay() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mTurnButton.setImageResource(R.drawable.video_suspend);
//            mCenterPlayButton.setVisibility(GONE);
        } else {
            mTurnButton.setImageResource(R.drawable.video_play);
//            mCenterPlayButton.setVisibility(VISIBLE);
        }
    }

    void updateScaleButton() {
        if (mIsFullScreen) {
            mScaleButton.setImageResource(R.drawable.video_narrow);
        } else {
            mScaleButton.setImageResource(R.drawable.video_enlarge);
        }
    }

    void toggleButtons(boolean isFullScreen) {
        mIsFullScreen = isFullScreen;
        updateScaleButton();
        updateBackButton();
        updateTitleBar();
    }

    private void updateTitleBar(){
        mTitleLayout.setVisibility(mIsFullScreen ? View.VISIBLE : View.INVISIBLE);
    }

    void updateBackButton() {
        mBackButton.setVisibility(mIsFullScreen ? View.VISIBLE : View.INVISIBLE);
    }

    boolean isFullScreen() {
        return mIsFullScreen;
    }

    private void doPauseResume() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    //TODO  这里需要查看一下
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        int newPosition = 0;

        boolean change = false;

        public void onStartTrackingTouch(SeekBar bar) {
            if (mPlayer == null) {
                return;
            }
            show(3600000);

            mDragging = true;
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (mPlayer == null || !fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            long duration = mPlayer.getDuration();
            long newposition = (duration * progress) / 1000L;
            newPosition = (int) newposition;
            change = true;
        }

        public void onStopTrackingTouch(SeekBar bar) {
            if (mPlayer == null) {
                return;
            }
            if (change) {
                mPlayer.seekTo(newPosition);
                if (mCurrentTime != null) {
                    mCurrentTime.setText(stringForTime(newPosition));
                }
            }
            mDragging = false;
            setProgress();
            updatePausePlay();
            show(sDefaultTimeout);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mShowing = true;
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
//        super.setEnabled(enabled);
        if (mTurnButton != null) {
            mTurnButton.setEnabled(enabled);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        if (mScalable) {
            mScaleButton.setEnabled(enabled);
        }
        mBackButton.setEnabled(true);// 全屏状态下右上角的返回键总是可用.
    }

    public void showLoading() {
        mHandler.sendEmptyMessage(SHOW_LOADING);
    }

    public void hideLoading() {
        mHandler.sendEmptyMessage(HIDE_LOADING);
    }

    public void showError() {
        mHandler.sendEmptyMessage(SHOW_ERROR);
    }

    public void hideError() {
        mHandler.sendEmptyMessage(HIDE_ERROR);
    }

    public void showComplete() {
        mHandler.sendEmptyMessage(SHOW_COMPLETE);
    }

    public void hideComplete() {
        mHandler.sendEmptyMessage(HIDE_COMPLETE);
    }

    public void setTitle(String titile) {
        mTitle.setText(titile);
    }

//    public void setFullscreenEnabled(boolean enabled) {
//        mFullscreenEnabled = enabled;
//        mScaleButton.setVisibility(mIsFullScreen ? VISIBLE : GONE);
//    }


    public void setOnErrorView(int resId) {
        errorLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(mContext);
        inflater.inflate(resId, errorLayout, true);
    }

    public void setOnErrorView(View onErrorView) {
        errorLayout.removeAllViews();
        errorLayout.addView(onErrorView);
    }

    public void setOnLoadingView(int resId) {
        loadingLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(mContext);
        inflater.inflate(resId, loadingLayout, true);
    }

    public void setOnLoadingView(View onLoadingView) {
        loadingLayout.removeAllViews();
        loadingLayout.addView(onLoadingView);
    }

    public void setOnErrorViewClick(OnClickListener onClickListener) {
        errorLayout.setOnClickListener(onClickListener);
    }

    public interface MediaPlayerControl {
        void start();

        void pause();

        int getDuration();

        int getCurrentPosition();

        void seekTo(int pos);

        boolean isPlaying();

        int getBufferPercentage();

        boolean canPause();

        boolean canSeekBackward();

        boolean canSeekForward();

        void closePlayer();//关闭播放视频,使播放器处于idle状态

        void setFullscreen(boolean fullscreen);

        /***
         *
         * @param fullscreen
         * @param screenOrientation valid only fullscreen=true.values should be one of
         *                          ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
         *                          ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
         *                          ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
         *                          ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
         */
        void setFullscreen(boolean fullscreen, int screenOrientation);
    }
}
