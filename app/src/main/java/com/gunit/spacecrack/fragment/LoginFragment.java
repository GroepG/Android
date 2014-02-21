package com.gunit.spacecrack.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.gunit.spacecrack.R;
import com.gunit.spacecrack.activity.LoginActivity;
import com.gunit.spacecrack.application.SpaceCrackApplication;
import com.gunit.spacecrack.activity.HomeActivity;
import com.gunit.spacecrack.model.Profile;
import com.gunit.spacecrack.restservice.RestService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Created by Dimitri on 20/02/14.
 */
public class LoginFragment extends Fragment {

    private EditText edtEmail;
    private EditText edtPassword;
    private Button login;
    private Button register;
    private LoginButton facebook;
    private SharedPreferences sharedPreferences;

    private LoginActivity loginActivity;

    private static final String TAG = "LoginFragment";
    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        loginActivity = (LoginActivity) getActivity();
        context = getActivity();
        sharedPreferences = loginActivity.getSharedPreferences("Login", 0);

        //Find the views
        edtEmail = (EditText) view.findViewById(R.id.edt_login_email);
        edtPassword = (EditText) view.findViewById(R.id.edt_login_password);
        login = (Button) view.findViewById(R.id.btn_login_login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!edtEmail.getText().toString().equals("") || !edtPassword.getText().toString().equals("")) {
                    new LoginTask(edtEmail.getText().toString(), edtPassword.getText().toString(), false).execute();
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.fill_in_fields), Toast.LENGTH_SHORT).show();
                }
            }
        });
        register = (Button) view.findViewById(R.id.btn_login_register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getFragmentManager().beginTransaction()
                        .replace(R.id.container, new RegisterFragment())
                        .addToBackStack("LoginFragment")
                        .commit();
            }
        });
        facebook = (LoginButton) view.findViewById(R.id.btn_login_facebook);
        //Set the permissions
        facebook.setReadPermissions(Arrays.asList("email", "user_birthday"));
        facebook.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                SpaceCrackApplication.graphUser = user;
                if (user != null) {
                    SpaceCrackApplication.graphUser = user;
                    new LoginTask((String) SpaceCrackApplication.graphUser.asMap().get("email"), "facebook" + user.getId(), true).execute(SpaceCrackApplication.URL_LOGIN);
                }
            }
        });

        return view;
    }

    //POST request to log the user in
    public class LoginTask extends AsyncTask<String, Void, String> {

        private JSONObject user;
        private boolean facebookLogin;
        private String email;
        private String password;

        public LoginTask (String email, String password, boolean facebookLogin)
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
            this.facebookLogin = facebookLogin;
        }

        @Override
        protected void onPreExecute() {
            login.setEnabled(false);
            register.setEnabled(false);
            facebook.setEnabled(false);
        }

        @Override
        protected String doInBackground (String...url)
        {
            return RestService.postRequest(SpaceCrackApplication.URL_LOGIN, user);
        }

        @Override
        protected void onPostExecute (String result)
        {
            Toast.makeText(getActivity(), result != null ? getResources().getString(R.string.login_succes) : getResources().getString(R.string.login_fail), Toast.LENGTH_SHORT).show();

            saveLoginCredentials(result, email, password);

            if (result != null) {
                new GetProfile().execute(SpaceCrackApplication.URL_PROFILE);
            } else if (facebookLogin) {
                new RegisterTask(SpaceCrackApplication.graphUser.getName(), "facebook" + SpaceCrackApplication.graphUser.getId(), "facebook" + SpaceCrackApplication.graphUser.getId(), (String) SpaceCrackApplication.graphUser.asMap().get("email")).execute(SpaceCrackApplication.DOMAIN + SpaceCrackApplication.URL_REGISTER);
            }

        }
    }

    //POST request to postRequest the user
    public class RegisterTask extends AsyncTask<String, Void, String> {

        private JSONObject newUser;
        private String email;
        private String password;

        public RegisterTask (String username, String password, String passwordRepeated, String email)
        {
            super();
            this.email = email;
            this.password = password;
            newUser = new JSONObject();
            try {
                newUser.put("username", username);
                newUser.put("password", password);
                newUser.put("passwordRepeated", passwordRepeated);
                newUser.put("email", email);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
            login.setEnabled(false);
            register.setEnabled(false);
            facebook.setEnabled(false);
        }

        @Override
        protected String doInBackground (String...url)
        {
            //Register newUser
            String accessToken = RestService.postRequest(SpaceCrackApplication.URL_REGISTER, newUser);
            //If newUser has been registered, login
            if (accessToken != null) {
                JSONObject loginUser = new JSONObject();
                try {
                    loginUser.put("email", newUser.get("email"));
                    loginUser.put("password", newUser.get("password"));
                    accessToken = RestService.postRequest(SpaceCrackApplication.URL_LOGIN, loginUser);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return accessToken;
        }

        @Override
        protected void onPostExecute (String result)
        {
            saveLoginCredentials(result, email, password);

            if (result != null && result.length() < 0) {
                new GetProfile().execute(SpaceCrackApplication.URL_PROFILE);
            } else {
                Toast.makeText(context, getResources().getString(R.string.email_register), Toast.LENGTH_SHORT).show();
                login.setEnabled(true);
                register.setEnabled(true);
                facebook.setEnabled(true);
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

                    if (SpaceCrackApplication.profile.image != null) {
                        //Get the image from the Data URI
                        String[] imageParts = SpaceCrackApplication.profile.image.split(",");
                        byte[] decodedString = Base64.decode(imageParts[1], 0);
                        SpaceCrackApplication.profilePicture = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    }

                    Intent intent = new Intent(loginActivity, HomeActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                } catch (JsonParseException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(loginActivity, getResources().getText(R.string.profile_fail), Toast.LENGTH_SHORT).show();
            }
            login.setEnabled(true);
            register.setEnabled(true);
            facebook.setEnabled(true);
        }
    }

    private void saveLoginCredentials(String accessToken, String email, String password) {
        SpaceCrackApplication.accessToken = accessToken;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("accessToken", accessToken);
        editor.putString("email", email);
        editor.putString("password", password);
        editor.commit();
    }
}