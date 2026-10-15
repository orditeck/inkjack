package dev.orditeck.inkjack;

import android.app.Activity;
import android.content.Intent;
import android.os.*;

/** User-initiated trampoline collapses the notification shade before reading the app below. */
public final class CaptureActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        var service=InkAccessibilityService.instance;
        if(service==null) {
            startActivity(new Intent(this,MainActivity.class));
            Ui.toast(this,"Enable Inkjack in Accessibility settings first.");
        }
        finish();
        if(service!=null && state==null) new Handler(Looper.getMainLooper()).postDelayed(()->{
            if(InkAccessibilityService.instance==service) service.capture(false);
        },450);
    }
}
