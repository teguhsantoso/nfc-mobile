package de.nfc.reader;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import de.nfc.reader.activities.MainActivity;

public class SplashActivity extends AppCompatActivity {
    private Context     cTxt;
    private Boolean     showAnimation = false;
    private ImageView   imageViewSplashIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Set context reference of this activity.
        cTxt = this;

        // Hide the screen keyboard during animation.
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Initialize all UI components.
        imageViewSplashIcon = (ImageView) findViewById(R.id.imageViewIcon);

        // Set default action and values for UI components.
        showAnimation = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startSplashAnimation();
    }

    @Override
    public void onBackPressed() {
        // Do nothing.
    }

    private void startSplashAnimation() {
        if (showAnimation) {
            final Animation animation_fade_in = AnimationUtils.loadAnimation(cTxt, R.anim.fadein);
            if(imageViewSplashIcon != null){
                imageViewSplashIcon.startAnimation(animation_fade_in);
                animation_fade_in
                        .setAnimationListener(new Animation.AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {
                                // Not yet implemented.
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                                // Not yet implemented.
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                launchNextActivity();
                            }
                        });
            }
        } else {
            launchNextActivity();
        }
    }

    private void launchNextActivity() {
        Intent intent = new Intent(cTxt, MainActivity.class);
        startActivity(intent);
    }

}
