package com.gunit.spacecrack.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.IconButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;
import com.gunit.spacecrack.R;
import com.gunit.spacecrack.activity.LoginActivity;
import com.gunit.spacecrack.activity.ProfileActivity;
import com.gunit.spacecrack.activity.SettingsActivity;
import com.gunit.spacecrack.application.SpaceCrackApplication;
import com.gunit.spacecrack.service.SpaceCrackService;

/**
 * Created by Dimitri on 28/02/14.
 */

/**
 * HomeFragment displays several buttons for navigation trough the app
 */
public class HomeFragment extends Fragment {
    private ProfilePictureView fbPictureView;
    private ImageView imgProfilePicture;
    private TextView txtName;
    private LinearLayout lltProfile;
    private IconButton btnNewGame;
    private IconButton btnLobby;
    private IconButton btnStatistics;
    private IconButton btnReplay;
    private IconButton btnHelp;
    private IconButton btnLogout;
    private IconButton btnSettings;
    private IconButton btnShare;

    /**
     * Build up the view of the Fragment and return it
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        fbPictureView = (ProfilePictureView) view.findViewById(R.id.ppv_home_fbpicture);
        imgProfilePicture = (ImageView) view.findViewById(R.id.img_home_profilepicture);
        txtName = (TextView) view.findViewById(R.id.txt_home_welcome);
        lltProfile = (LinearLayout) view.findViewById(R.id.llt_home_profile);

        btnNewGame = (IconButton) view.findViewById(R.id.btn_home_newgame);
        btnLobby = (IconButton) view.findViewById(R.id.btn_home_lobby);
        btnStatistics = (IconButton) view.findViewById(R.id.btn_home_statistics);
        btnReplay = (IconButton) view.findViewById(R.id.btn_home_replay);
        btnHelp = (IconButton) view.findViewById(R.id.btn_home_help);
        btnShare = (IconButton) view.findViewById(R.id.btn_home_share);
        btnSettings = (IconButton) view.findViewById(R.id.btn_home_settings);
        btnLogout = (IconButton) view.findViewById(R.id.btn_home_logout);

        addListeners();

        updateAccount();

        Intent intent = new Intent(getActivity(), SpaceCrackService.class);
        intent.putExtra("username", SpaceCrackApplication.user.username);
        getActivity().startService(intent);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAccount();
    }

    private void addListeners() {
        lltProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SpaceCrackApplication.user.profile != null) {
                    Intent intent = new Intent(getActivity(), ProfileActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), getResources().getText(R.string.profile_not_available), Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new HelpFragment(), "Help")
                        .addToBackStack("HomeFragment")
                        .commit();
            }
        });
        btnNewGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new NewGameFragment(), "NewGame")
                        .addToBackStack("HomeFragment")
                        .commit();
            }
        });
        btnLobby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new LobbyFragment(), "Lobby")
                        .addToBackStack("HomeFragment")
                        .commit();
            }
        });
        btnStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new StatisticsFragment(), "Statistics")
                        .addToBackStack("HomeFragment")
                        .commit();
            }
        });
        btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new ReplayFragment(), "Replay")
                        .addToBackStack("HomeFragment")
                        .commit();
            }
        });
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.incredible_strategy_game) + "\nhttp://spacecrack-groepg.rhcloud.com");
                startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
            }
        });
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            }
        });
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Session.getActiveSession() != null) {
                    Session.getActiveSession().closeAndClearTokenInformation();
                }
                SpaceCrackApplication.logout();
                getActivity().getSharedPreferences("Login", 0).edit().clear().commit();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });
    }

    /**
     * Update account with profile information
     */
    private void updateAccount() {
        final Session session = Session.getActiveSession();
        //Facebook profile
        if (session != null && session.isOpened()) {
            fbPictureView.setVisibility(View.VISIBLE);
            imgProfilePicture.setVisibility(View.GONE);
            if (SpaceCrackApplication.graphUser != null) {
                fbPictureView.setProfileId(SpaceCrackApplication.graphUser.getId());
                txtName.setText(SpaceCrackApplication.graphUser.getFirstName() + " " + SpaceCrackApplication.graphUser.getLastName());
            } else {
                Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        if (user != null && session == Session.getActiveSession()) {
                            fbPictureView.setProfileId(user.getId());
                            txtName.setText(user.getFirstName() + " " + user.getLastName());
                        }
                    }
                });
                request.executeAsync();
            }
        } else {
            //Regular profile
            if (SpaceCrackApplication.user != null && SpaceCrackApplication.user.profile != null) {
                if (SpaceCrackApplication.user.profile.firstname != null && SpaceCrackApplication.user.profile.lastname != null) {
                    txtName.setText(SpaceCrackApplication.user.profile.firstname + " " + SpaceCrackApplication.user.profile.lastname);
                }
                if (SpaceCrackApplication.profilePicture != null) {
                    imgProfilePicture.setImageBitmap(SpaceCrackApplication.profilePicture);
                }
            }
        }
    }
}
