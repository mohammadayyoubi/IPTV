package com.example.iptv.activities.user;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;

import com.example.iptv.Prefs;
import com.example.iptv.R;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.UiUtils;
import com.google.android.material.button.MaterialButton;

/** First-launch introduction to TechWrap TV (four short pages). */
public class OnboardingActivity extends BaseActivity {

    private static final int[] ICONS = {0, R.drawable.ic_category, R.drawable.ic_hd, R.drawable.ic_link};
    private static final int[] TITLES = {R.string.intro_1_title, R.string.intro_2_title, R.string.intro_3_title, R.string.intro_4_title};
    private static final int[] BODIES = {R.string.intro_1_body, R.string.intro_2_body, R.string.intro_3_body, R.string.intro_4_body};

    private int page;
    private View pageView, iconFrame, logo;
    private ImageView icon;
    private TextView title, body;
    private LinearLayout dots;
    private MaterialButton next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        UiUtils.applyInsets(findViewById(R.id.root), true, true);

        pageView = findViewById(R.id.page);
        iconFrame = findViewById(R.id.iconFrame);
        logo = findViewById(R.id.pageLogo);
        icon = findViewById(R.id.pageIcon);
        title = findViewById(R.id.pageTitle);
        body = findViewById(R.id.pageBody);
        dots = findViewById(R.id.dots);
        next = findViewById(R.id.nextButton);

        int size = (int) (8 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < TITLES.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginStart(size / 2);
            lp.setMarginEnd(size / 2);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.bg_avatar_circle);
            dots.addView(dot);
        }

        findViewById(R.id.skipButton).setOnClickListener(v -> done());
        next.setOnClickListener(v -> {
            if (page < TITLES.length - 1) show(page + 1, true);
            else done();
        });
        // Back goes to the previous page instead of leaving
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (page > 0) show(page - 1, true);
                else done();
            }
        });
        show(savedInstanceState != null ? savedInstanceState.getInt("page") : 0, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("page", page);
    }

    private void show(int index, boolean animate) {
        page = index;
        Runnable bind = () -> {
            boolean first = index == 0;
            logo.setVisibility(first ? View.VISIBLE : View.GONE);
            iconFrame.setVisibility(first ? View.GONE : View.VISIBLE);
            if (!first) icon.setImageResource(ICONS[index]);
            title.setText(TITLES[index]);
            body.setText(BODIES[index]);
            next.setText(index == TITLES.length - 1 ? R.string.intro_start : R.string.intro_next);
            for (int i = 0; i < dots.getChildCount(); i++) {
                dots.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                        i == index ? R.color.accent : R.color.outline_strong)));
            }
        };
        if (!animate) {
            bind.run();
            return;
        }
        pageView.animate().alpha(0f).setDuration(120).withEndAction(() -> {
            bind.run();
            pageView.animate().alpha(1f).setDuration(180).start();
        }).start();
    }

    private void done() {
        Prefs.setOnboardingDone(this);
        finish();
    }
}
