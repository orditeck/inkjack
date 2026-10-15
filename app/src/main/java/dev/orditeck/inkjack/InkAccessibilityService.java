package dev.orditeck.inkjack;

import android.accessibilityservice.*;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.display.DisplayManager;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import java.util.*;

public final class InkAccessibilityService extends AccessibilityService {
    static InkAccessibilityService instance;
    private final Handler main=new Handler(Looper.getMainLooper());
    private SelectionOverlay overlay;
    private boolean busy;
    private int generation;
    private final AccessibilityButtonController.AccessibilityButtonCallback buttonCallback=new AccessibilityButtonController.AccessibilityButtonCallback(){
        @Override public void onClicked(AccessibilityButtonController controller) { capture(false); }
    };
    @Override protected void onServiceConnected() {
        instance=this;
        getAccessibilityButtonController().registerAccessibilityButtonCallback(buttonCallback);
        if(getSharedPreferences("settings",0).getBoolean("notification",false)) showNotification();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { /* No passive capture. */ }
    @Override public void onInterrupt() { cancel(); }
    @Override public void onConfigurationChanged(android.content.res.Configuration configuration) {
        super.onConfigurationChanged(configuration);
        // Screen coordinates become invalid after rotation or a fold/unfold.
        cancel();
    }
    @Override public void onDestroy() {
        cancel();getAccessibilityButtonController().unregisterAccessibilityButtonCallback(buttonCallback);
        getSystemService(NotificationManager.class).cancel(1);
        if(instance==this)instance=null;super.onDestroy();
    }
    void cancel() { generation++;busy=false;dismiss(); }
    void dismiss() { if(overlay!=null) { overlay.close();overlay=null; } }
    void showNotification() {
        var manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("shortcut","Lift text shortcut",NotificationManager.IMPORTANCE_LOW));
        var capture=PendingIntent.getActivity(this,1,new Intent(this,CaptureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        var settings=PendingIntent.getActivity(this,2,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        var notification=new Notification.Builder(this,"shortcut").setSmallIcon(R.drawable.ic_tile).setContentTitle("Text worth keeping?")
            .setContentText("Tap to lift text from this screen").setContentIntent(capture).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"Settings",settings).build()).build();
        manager.notify(1,notification);
    }
    void capture(boolean ocr) {
        if(busy) {Ui.toast(this,"Still reading the previous screen.");return;}
        dismiss();
        if(getSystemService(KeyguardManager.class).isKeyguardLocked()) { Ui.toast(this,"Unlock your phone before lifting text.");return; }
        if(ocr) { screenshot();return; }
        var root=getRootInActiveWindow();
        if(root!=null && getPackageName().equals(String.valueOf(root.getPackageName()))) {root.recycle();Ui.toast(this,"Open another app first, then tap Lift text.");return;}
        var metrics=getSystemService(WindowManager.class).getMaximumWindowMetrics();
        var regions=NodeReader.read(root,metrics.getBounds());
        show(regions,null);
    }
    private void screenshot() {
        busy=true; int token=++generation;
        Ui.toast(this,"Reading the screen on your device…");
        // No overlay is present when the screenshot is requested.
        takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){
            @Override public void onSuccess(ScreenshotResult result) {
                Bitmap image=null;
                try(var buffer=result.getHardwareBuffer()) {
                    var hardware=Bitmap.wrapHardwareBuffer(buffer,result.getColorSpace());
                    if(hardware!=null) {image=hardware.copy(Bitmap.Config.ARGB_8888,false);hardware.recycle();}
                } catch(RuntimeException ignored) { }
                if(image==null) {busy=false;Ui.toast(InkAccessibilityService.this,"Screenshot could not be read.");return;}
                if(token!=generation) {image.recycle();return;}
                // OCR owns a copy; the display copy stays in memory only until the overlay closes.
                Bitmap display=image;
                Bitmap input=image.copy(Bitmap.Config.ARGB_8888,false);
                if(input==null) {display.recycle();busy=false;return;}
                OcrEngine.bitmap(InkAccessibilityService.this,input,(regions,error)->{
                    if(token!=generation) {display.recycle();return;}
                    busy=false;
                    if(error!=null) {display.recycle();Ui.toast(InkAccessibilityService.this,error);return;}
                    show(regions,display);
                });
            }
            @Override public void onFailure(int error) {
                if(token!=generation)return;busy=false;
                Ui.toast(InkAccessibilityService.this,error==ERROR_TAKE_SCREENSHOT_SECURE_WINDOW ? "This app protects its screen. Inkjack can't scan it." : "Screenshot unavailable. Close protected content and try again in a moment.");
            }
        });
    }
    private void show(java.util.List<TextRegion> regions,Bitmap image) {
        dismiss();
        overlay=new SelectionOverlay(this,regions,image,()->{dismiss();main.postDelayed(()->capture(true),250);});
        try {overlay.open();}catch(RuntimeException e){dismiss();Ui.toast(this,"Couldn't show the selection overlay. Re-enable Inkjack in Accessibility settings.");}
    }
}
