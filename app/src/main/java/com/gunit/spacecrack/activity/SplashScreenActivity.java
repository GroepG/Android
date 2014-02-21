package com.gunit.spacecrack.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.gunit.spacecrack.R;
import com.gunit.spacecrack.application.SpaceCrackApplication;
import com.gunit.spacecrack.model.Profile;
import com.gunit.spacecrack.restservice.RestService;

import org.json.JSONException;
import org.json.JSONObject;

public class SplashScreenActivity extends Activity {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        //Check the network connection
        final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            sharedPreferences = getSharedPreferences("Login", 0);
            startApp();
        } else {
            Toast.makeText(SplashScreenActivity.this, getResources().getString(R.string.no_connection), Toast.LENGTH_LONG).show();
        }

    }

    private void startApp() {
        if (isLoggedIn()) {
            new LoginTask(sharedPreferences.getString("email", null), sharedPreferences.getString("password", null)).execute();
//            //Store the access token for authorisation
//            SpaceCrackApplication.accessToken = sharedPreferences.getString("accessToken", null);


        } else {
            goLogin();
        }
    }

    private boolean isLoggedIn() {
        //Facebook check
        Session session = Session.getActiveSession();
        if (session == null) {
            session = Session.openActiveSessionFromCache(SplashScreenActivity.this);
        }
        if (session != null && session.isOpened()) {
            final Session finalSession = session;
            Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user, Response response) {
                    if (user != null && finalSession == Session.getActiveSession()) {
                        SpaceCrackApplication.graphUser = user;
                    }
                }
            });
            request.executeAsync();
            return true;
        }
        //SpaceCrack user check
        return sharedPreferences.getString("accessToken", null) != null;
    }

    private void goHome () {
        Intent intent = new Intent(SplashScreenActivity.this, HomeActivity.class);
        startActivity(intent);
        //Close this activity so it won't show up again
        finish();
    }

    private void goLogin () {
        Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
        startActivity(intent);
        //Close this activity so it won't show up again
        finish();
    }

    //Login check
    private class LoginTask extends AsyncTask<String, Void, String> {

        private JSONObject user;
        private String email;
        private String password;

        public LoginTask (String email, String password)
        {
            super();
            this.email = email;
            this.password = password;
            //Create an user to log in
            user = new JSONObject();
            try {
                user.put("email", email);
                user.put("password", password);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground (String...url)
        {
            return RestService.postRequest(SpaceCrackApplication.URL_LOGIN, user);
        }

        @Override
        protected void onPostExecute (String result)
        {
            SpaceCrackApplication.accessToken = result;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("accessToken", result);
            editor.putString("email", email);
            editor.putString("password", password);
            editor.commit();
            if (result != null) {
                //Get the profile of the user
                new GetProfile().execute(SpaceCrackApplication.URL_PROFILE);
            } else {
                Toast.makeText(SplashScreenActivity.this, getResources().getText(R.string.login_fail), Toast.LENGTH_SHORT).show();
                goLogin();
            }
        }
    }

    //POST request to edit the profile
    private class GetProfile extends AsyncTask<String, Void, String> {

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
                    SpaceCrackApplication.profile = gson.fromJson(result, Profile.class);
                    //Get the image from the Data URI
                    if (SpaceCrackApplication.profile.image != null) {
                        String[] imageParts = SpaceCrackApplication.profile.image.split(",");
                        byte[] decodedString = Base64.decode(imageParts[1], 0);
                        SpaceCrackApplication.profilePicture = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    }

                    goHome();
                } catch (JsonParseException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SplashScreenActivity.this, getResources().getText(R.string.profile_fail), Toast.LENGTH_SHORT).show();
                goLogin();
            }
        }
    }

}
