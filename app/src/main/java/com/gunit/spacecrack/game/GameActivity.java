package com.gunit.spacecrack.game;

import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.gunit.spacecrack.R;
import com.gunit.spacecrack.application.SpaceCrackApplication;
import com.gunit.spacecrack.game.manager.ResourcesManager;
import com.gunit.spacecrack.game.manager.SceneManager;
import com.gunit.spacecrack.model.Game;
import com.gunit.spacecrack.model.GameWrapper;
import com.gunit.spacecrack.model.Planet;
import com.gunit.spacecrack.model.Profile;
import com.gunit.spacecrack.model.SpaceCrackMap;
import com.gunit.spacecrack.restservice.RestService;

import org.andengine.engine.Engine;
import org.andengine.engine.LimitedFPSEngine;
import org.andengine.engine.camera.BoundCamera;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.SmoothCamera;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.WakeLockOptions;
import org.andengine.engine.options.resolutionpolicy.IResolutionPolicy;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.scene.Scene;
import org.andengine.input.touch.controller.MultiTouch;
import org.andengine.ui.activity.BaseGameActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dimitri on 24/02/14.
 */
public class GameActivity extends BaseGameActivity {

    private static final int WIDTH = 1600;
    private static final int HEIGHT = 1000;
    private SmoothCamera smoothCamera;
    public static final int CAMERA_WIDTH = 930;
    public static final int CAMERA_HEIGHT = 558;
    private static final int FPS_LIMIT = 60;
    private ResourcesManager resourceManager;

    public SpaceCrackMap spaceCrackMap;
    public GameWrapper gameWrapper;

    // Camera movement speeds
    final float maxVelocityX = 500;
    final float maxVelocityY = 500;
    // Camera zoom speed
    final float maxZoomFactorChange = 5;

    @Override
    protected void onCreate(Bundle pSavedInstanceState) {
        super.onCreate(pSavedInstanceState);
        new GetMap().execute(SpaceCrackApplication.URL_MAP);
//        new StartGameTask("test", "1").execute(SpaceCrackApplication.URL_GAME);
    }

    @Override
    public Engine onCreateEngine(EngineOptions pEngineOptions) {
        Engine engine = new LimitedFPSEngine(pEngineOptions, FPS_LIMIT);
        return engine;
    }

    @Override
    public EngineOptions onCreateEngineOptions() {
        smoothCamera = new SmoothCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT, maxVelocityX, maxVelocityY, maxZoomFactorChange);
        smoothCamera.setBounds(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        smoothCamera.setBoundsEnabled(true);
        IResolutionPolicy resolutionPolicy = new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT);
        EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, resolutionPolicy, smoothCamera);
        engineOptions.getAudioOptions().setNeedsMusic(true).setNeedsSound(true);
        engineOptions.setWakeLockOptions(WakeLockOptions.SCREEN_ON);
        engineOptions.getTouchOptions().setNeedsMultiTouch(true);

        if (!MultiTouch.isSupported(this)) {
            Toast.makeText(this, "Your device doesn't support multi touch", Toast.LENGTH_SHORT).show();
            finish();
        }
        return engineOptions;
    }

    @Override
    public void onCreateResources(OnCreateResourcesCallback pOnCreateResourcesCallback) throws Exception {
        ResourcesManager.getInstance().init(this);
        ResourcesManager.getInstance().loadFonts();
        pOnCreateResourcesCallback.onCreateResourcesFinished();
    }

    @Override
    public void onCreateScene(OnCreateSceneCallback pOnCreateSceneCallback) throws Exception {
        SceneManager.getInstance().createSplashScene(pOnCreateSceneCallback);
    }

    @Override
    public void onPopulateScene(Scene pScene, OnPopulateSceneCallback pOnPopulateSceneCallback) throws Exception {
        mEngine.registerUpdateHandler(new TimerHandler(2f, new ITimerCallback() {
            @Override
            public void onTimePassed(TimerHandler pTimerHandler) {
                mEngine.unregisterUpdateHandler(pTimerHandler);
                SceneManager.getInstance().createMenuScene();
            }
        }));
        pOnPopulateSceneCallback.onPopulateSceneFinished();
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        System.exit(0);
//    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            SceneManager.getInstance().getCurrentScene().onBackKeyPressed();
        }
        return false;
    }

    //GET request to map
    private class GetMap extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground (String...url)
        {
            return RestService.getRequest(url[0]);
        }

        @Override
        protected void onPostExecute (String result)
        {
            if (result != null) {
                try {
                    Gson gson = new Gson();
                    spaceCrackMap = gson.fromJson(result, SpaceCrackMap.class);

                } catch (JsonParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //POST request
    public class StartGameTask extends AsyncTask<String, Void, String> {

        private JSONObject game;
        private boolean facebookLogin;
        private String gameName;
        private String opponentId;

        public StartGameTask (String gameName, String opponentId)
        {
            super();
            this.gameName = gameName;
            this.opponentId = opponentId;
            //Create an user to log in
            game = new JSONObject();
            try {
                game.put("gameName", gameName);
                game.put("opponentProfileId", opponentId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            this.facebookLogin = facebookLogin;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected String doInBackground (String...url)
        {
            return RestService.postGame(url[0], game);
        }

        @Override
        protected void onPostExecute (String result)
        {
            if (result != null) {
                Toast.makeText(GameActivity.this, "Data received", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //GET request to the active game
    private class GetActiveGame extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground (String...url)
        {
            return RestService.getRequest(url[0]);
        }

        @Override
        protected void onPostExecute (String result)
        {
            if (result != null) {
                try {
                    Gson gson = new Gson();
                    gameWrapper = gson.fromJson(result, GameWrapper.class);

                } catch (JsonParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
