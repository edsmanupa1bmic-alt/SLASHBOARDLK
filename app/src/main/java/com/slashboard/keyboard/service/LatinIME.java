package com.slashboard.keyboard.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import com.slashboard.keyboard.R;
import com.slashboard.keyboard.SettingsActivity;

/**
 * LatinIME: Base InputMethodService providing standardized HeliBoard/AOSP
 * bottom space padding persistence and insets computation.
 */
public class LatinIME extends InputMethodService {

    public static void launchSettingsActivity(Context context) {
        try {
            Intent intent = new Intent(context, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallbackIntent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallbackIntent);
            } catch (Exception ignored) {}
        }
    }

    public void applyBottomPadding(View rootView) {
        if (rootView == null) return;

        SharedPreferences defaultPrefs = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        int bottomPaddingDp = defaultPrefs.getInt("ime_bottom_padding_dp", -1);
        if (bottomPaddingDp < 0) {
            SharedPreferences customPrefs = getSharedPreferences("slashboard_prefs", Context.MODE_PRIVATE);
            bottomPaddingDp = customPrefs.getInt("ime_bottom_padding_dp", customPrefs.getInt("bottom_space", 0));
        }
        if (bottomPaddingDp < 0) {
            bottomPaddingDp = 0;
        }

        int bottomPaddingPx = (int) (bottomPaddingDp * getResources().getDisplayMetrics().density);

        View bottomSpaceView = rootView.findViewById(R.id.ime_bottom_space_insets);
        if (bottomSpaceView != null) {
            ViewGroup.LayoutParams lp = bottomSpaceView.getLayoutParams();
            if (lp != null) {
                lp.height = bottomPaddingPx;
                bottomSpaceView.setLayoutParams(lp);
            }
            bottomSpaceView.setVisibility(bottomPaddingPx > 0 ? View.VISIBLE : View.GONE);
            bottomSpaceView.requestLayout();
        } else {
            rootView.setPadding(
                    rootView.getPaddingLeft(),
                    rootView.getPaddingTop(),
                    rootView.getPaddingRight(),
                    bottomPaddingPx
            );
        }
    }
}
