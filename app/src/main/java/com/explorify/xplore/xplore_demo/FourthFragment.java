package com.explorify.xplore.xplore_demo;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.google.android.gms.common.SignInButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


/**
 * Created by Nika on 11/9/2016.
 */

public class FourthFragment extends Fragment {

    private View myView;
    private ImageView b_join, b_create;
    private PopupWindow popupWindow;
    int appWidth, appHeight;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fourth_layout, container, false);
        return myView;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        InitDisplayMetrics();

        b_join = (ImageView) myView.findViewById(R.id.join_party);
        b_create = (ImageView) myView.findViewById(R.id.create_party);

        b_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isUserSignedIn()) {
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            new SearchGroupsFragment()).addToBackStack("4").commit();
                }
                else
                {
                    popSignInMenu(appWidth, appHeight, 0.8, 0.6);
                }
            }
        });

        b_create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isUserSignedIn()) {
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            new CreateGroupFragment()).addToBackStack("4").commit();
                }
                else
                {
                    popSignInMenu(appWidth, appHeight, 0.8, 0.6);
                }
            }
        });
    }

    private void InitDisplayMetrics()
    {
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        appWidth = dm.widthPixels;
        appHeight = dm.heightPixels;
    }

    private boolean isUserSignedIn()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return true;
        } else {
            return false;
        }
    }

    private void popSignInMenu(int appWidth, int appHeight, double xScale, double yScale)
    {
        int popWidth = (int) (appWidth * xScale);
        int popHeight = (int) (appHeight * yScale);

        int locationX = 0;
        int locationY = 0;

        View popupView = getActivity().getLayoutInflater().inflate(R.layout.pre_signin_layout, null);
        popupView.setBackgroundResource(R.drawable.mr_dialog_material_background_light);
        popupView.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_down_open));


        popupWindow = new PopupWindow(popupView, popWidth, popHeight, true);
        popupWindow.showAtLocation(myView, Gravity.CENTER, locationX, locationY);

        Button signin_btn = (Button) popupView.findViewById(R.id.signin_button);
        signin_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    Context context = getActivity();
                    if (General.isNetConnected(context)) {
                        popupWindow.dismiss();
                        Intent i = new Intent(context, SignInActivity.class);
                        context.startActivity(i);
                    } else
                        General.groups_DisplayNetErrorDialog(context);
            }
        });

        General.dimBehind(popupWindow, 0.5f);
    }
}
