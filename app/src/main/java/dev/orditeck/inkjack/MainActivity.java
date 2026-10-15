package dev.orditeck.inkjack;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.widget.*;

public final class MainActivity extends Activity {
    private static final int PICK=10, NOTIFICATIONS=11;
    private TextView progress;
    private boolean resumed;
    private final Handler refresh=new Handler(Looper.getMainLooper());
    private final Runnable poll=new Runnable(){@Override public void run(){
        if(!resumed)return;
        if(Session.importRunning){refresh.postDelayed(this,300);return;}
        if(Session.importReady){Session.importReady=false;startActivity(new Intent(MainActivity.this,ResultActivity.class));}
        else {showHome();if(Session.importError!=null){progress.setText(Session.importError);Session.importError=null;}}
    }};
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); showHome();
        if(state==null) handleShare(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); handleShare(intent); }
    @Override protected void onResume() { super.onResume(); resumed=true;showHome();refresh.post(poll); }
    @Override protected void onPause() {resumed=false;refresh.removeCallbacks(poll);super.onPause();}
    private void showHome() {
        var body=Ui.page(this);
        var header=new LinearLayout(this); header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        var icon=new ImageView(this); icon.setImageResource(R.drawable.ic_inkjack); header.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        var brand=Ui.text(this,"  inkjack",30,Ui.WHITE); brand.setTypeface(null,Typeface.BOLD); header.addView(brand); body.addView(header);
        Ui.gap(body,26); body.addView(Ui.label(this,"YOUR SCREEN. YOUR WORDS."));
        var title=Ui.text(this,"Text isn't\ntrapped anymore.",38,Ui.WHITE); title.setTypeface(null,Typeface.BOLD); title.setLineSpacing(0,1.02f); body.addView(title);
        body.addView(Ui.text(this,"Lift text from apps, screenshots, and documents. Keep it. Edit it. Take it anywhere.",16,Ui.MUTED));
        var status=Ui.card(this,body);
        boolean enabled=InkAccessibilityService.instance!=null;
        status.addView(Ui.label(this,enabled ? "●  READY TO LIFT" : "01  /  ONE-TIME SETUP"));
        status.addView(Ui.text(this,enabled ? "Screen capture is ready" : "Give text an escape route",21,Ui.WHITE));
        status.addView(Ui.text(this,enabled ? "Open another app, then use the Lift text Quick Settings tile, notification, or accessibility button." : "Enable Inkjack in Android's Accessibility settings to select text in other apps.",14,Ui.MUTED));
        status.addView(Ui.button(this,enabled ? "Accessibility settings" : "Enable screen capture",true,v->disclosure()));
        body.addView(Ui.button(this,"Add Lift text tile",false,v->addTile()));
        boolean notification=getSharedPreferences("settings",0).getBoolean("notification",false);
        body.addView(Ui.button(this,notification ? "Hide notification shortcut" : "Enable notification shortcut",false,v->{
            if(notification){getSharedPreferences("settings",0).edit().putBoolean("notification",false).apply();getSystemService(NotificationManager.class).cancel(1);showHome();}
            else enableNotification();
        }));
        Ui.gap(body,18); body.addView(Ui.label(this,"OR BRING SOMETHING IN"));
        body.addView(Ui.button(this,"Scan an image or PDF",true,v->pickDocument()));
        progress=Ui.text(this,"English + French OCR · fully offline",13,Ui.MUTED); body.addView(progress);
        if(Session.importRunning)progress.setText(R.string.reading_progress);
        body.addView(Ui.button(this,"Customize capture toolbar",false,v->startActivity(new Intent(this,OverlaySettingsActivity.class))));
        var collection=Ui.card(this,body); collection.addView(Ui.label(this,"COLLECTED TEXT"));
        collection.addView(Ui.text(this,Session.collection.size()+" saved selection"+(Session.collection.size()==1 ? "" : "s"),21,Ui.WHITE));
        collection.addView(Ui.text(this,"Combine selections from several screens. Collected text stays in memory until you clear it or the app process closes.",14,Ui.MUTED));
        collection.addView(Ui.button(this,"Open collection",false,v->{Session.result=Session.collection.text(); startActivity(new Intent(this,ResultActivity.class));}));
        collection.addView(Ui.button(this,"Clear collection",false,v->new AlertDialog.Builder(this).setTitle("Clear collected text?").setMessage("This removes all collected text from Inkjack's memory.").setNegativeButton("Keep it",null).setPositiveButton("Clear",(d,w)->{Session.collection.clear();showHome();}).show()));
        body.addView(Ui.button(this,"OCR language",false,v->chooseLanguage()));
        Ui.gap(body,16); body.addView(Ui.label(this,"NO ADS. NO ACCOUNTS. NO CLOUD."));
        body.addView(Ui.text(this,"Inkjack "+BuildConfig.VERSION_NAME+" · Open source · Apache 2.0\nOnly captures when you ask. No internet permission.",12,Ui.MUTED));
        body.addView(Ui.button(this,"Open-source licenses",false,v->startActivity(new Intent(this,LicensesActivity.class))));
    }
    private void disclosure() {
        new AlertDialog.Builder(this).setTitle("Let Inkjack read the screen?")
            .setMessage(getString(R.string.accessibility_description)+"\n\nAndroid will warn that accessibility services can read screen content. Enable Inkjack only if you're comfortable with that access.")
            .setNegativeButton("Cancel",null).setPositiveButton("Open settings",(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).show();
    }
    private void addTile() {
        if(Build.VERSION.SDK_INT>=33) {
            var manager=getSystemService(StatusBarManager.class);
            manager.requestAddTileService(new ComponentName(this,CaptureTileService.class),"Lift text",android.graphics.drawable.Icon.createWithResource(this,R.drawable.ic_tile),getMainExecutor(),result->{
                if(result==StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED) Ui.toast(this,"You can add Lift text from the Quick Settings edit menu.");
            });
        } else Ui.toast(this,"Swipe down twice, tap Edit, then drag Lift text into your tiles.");
    }
    private void enableNotification() {
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATIONS);
        else {
            getSharedPreferences("settings",0).edit().putBoolean("notification",true).apply();
            if(InkAccessibilityService.instance!=null) InkAccessibilityService.instance.showNotification();
            else Ui.toast(this,"Enable screen capture first. The shortcut will appear when connected.");
            showHome();
        }
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g) {
        super.onRequestPermissionsResult(r,p,g);
        if(r==NOTIFICATIONS && g.length>0 && g[0]==PackageManager.PERMISSION_GRANTED) enableNotification();
    }
    private void chooseLanguage() {
        var prefs=getSharedPreferences("settings",0); String lang=prefs.getString("language","eng+fra");
        String[] values={"eng+fra","eng","fra"};
        int selected=java.util.Arrays.asList(values).indexOf(lang);
        new AlertDialog.Builder(this).setTitle("Offline OCR language").setSingleChoiceItems(new String[]{"English + français","English","Français"},Math.max(0,selected),(d,n)->{prefs.edit().putString("language",values[n]).apply();d.dismiss();}).setNegativeButton("Cancel",null).show();
    }
    private void pickDocument() {
        var intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf"});
        startActivityForResult(intent,PICK);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==PICK && result==RESULT_OK && data!=null && data.getData()!=null) scan(data.getData());
    }
    private void handleShare(Intent intent) {
        if(intent!=null && Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri uri=Build.VERSION.SDK_INT>=33 ? intent.getParcelableExtra(Intent.EXTRA_STREAM,Uri.class) : intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if(uri!=null && "content".equals(uri.getScheme())) scan(uri);
            else Ui.toast(this,"Share an image or PDF file with Inkjack.");
        }
    }
    private void scan(Uri uri) {
        if(Session.importRunning) { Ui.toast(this,"An OCR job is already running."); return; }
        Session.importRunning=true;Session.importReady=false;Session.importError=null;progress.setText(R.string.reading_progress);
        var app=getApplicationContext();
        OcrEngine.document(app,uri,(regions,error)->{
            Session.importRunning=false;
            if(error!=null) Session.importError=error;
            else if(regions.isEmpty()) Session.importError=app.getString(R.string.empty_ocr);
            else {Session.result=TextTools.join(regions.stream().map(r->r.text).collect(java.util.stream.Collectors.toList()));Session.importReady=true;}
        });
        refresh.removeCallbacks(poll);refresh.post(poll);
    }
}
