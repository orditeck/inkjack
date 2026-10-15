package dev.orditeck.inkjack;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.util.function.Consumer;

public final class OverlaySettingsActivity extends Activity {
    private OverlayPreferences preferences;
    private LinearLayout previewHost;
    private TextView position;
    private boolean previewVisible=true;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);preferences=new OverlayPreferences(this);
        var config=getResources().getConfiguration();
        previewVisible=state==null?config.screenHeightDp>=600&&config.fontScale<=1.6f:state.getBoolean("preview",true);showSettings();
    }
    @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("preview",previewVisible);super.onSaveInstanceState(state);}

    private void showSettings() {
        var root=Ui.column(this);root.setBackgroundColor(Ui.BG);int pad=Ui.dp(this,16);root.setPadding(pad,pad,pad,pad);
        root.setOnApplyWindowInsetsListener((v,insets)->{var bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());v.setPadding(pad+bars.left,pad+bars.top,pad+bars.right,pad+bars.bottom);return android.view.WindowInsets.CONSUMED;});
        setContentView(root);
        var heading=new LinearLayout(this);heading.setGravity(android.view.Gravity.CENTER_VERTICAL);
        var title=Ui.text(this,"Capture toolbar",24,Ui.WHITE);title.setTypeface(null,Typeface.BOLD);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        var done=Ui.button(this,"Done",false,v->finish());heading.addView(done,new LinearLayout.LayoutParams(Ui.dp(this,70),Ui.dp(this,48)));root.addView(heading);
        root.addView(Ui.text(this,"Changes apply to your next capture.",13,Ui.MUTED));
        var previewToggle=Ui.button(this,getString(previewVisible?R.string.overlay_hide_preview:R.string.overlay_show_preview),false,v->{previewVisible=!previewVisible;previewHost.setVisibility(previewVisible?View.VISIBLE:View.GONE);position.setVisibility(previewVisible?View.VISIBLE:View.GONE);((Button)v).setText(previewVisible?R.string.overlay_hide_preview:R.string.overlay_show_preview);});
        root.addView(previewToggle);
        previewHost=Ui.column(this);previewHost.setId(R.id.overlay_settings_preview);root.addView(previewHost);
        position=Ui.text(this,"",12,Ui.MUTED);root.addView(position);updatePreview();
        previewHost.setVisibility(previewVisible?View.VISIBLE:View.GONE);position.setVisibility(previewVisible?View.VISIBLE:View.GONE);
        var scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        var body=Ui.column(this);scroll.addView(body);

        var layout=Ui.card(this,body);layout.addView(Ui.label(this,"LAYOUT"));
        var current=preferences.read();
        layout.addView(Ui.text(this,getString(R.string.overlay_position_help),13,Ui.MUTED));
        choice(layout,"Dock position",new String[]{"Bottom","Top"},current.freePosition?-1:current.atTop?1:0,n->{preferences.dock(n==1);updatePreview();});
        choice(layout,"Width",new String[]{"Floating (more room to move)","Wide"},current.wide?1:0,n->{preferences.set(OverlayPreferences.WIDE,n==1);updatePreview();});
        choice(layout,"Spacing",new String[]{"Compact","Comfortable"},current.compact?0:1,n->{preferences.set(OverlayPreferences.COMPACT,n==0);updatePreview();});
        toggle(layout,"Selection preview","Show a snippet of the selected text",current.preview,R.id.overlay_toggle_preview,OverlayPreferences.PREVIEW);

        var actions=Ui.card(this,body);actions.addView(Ui.label(this,"SHORTCUTS & BEHAVIOR"));
        toggle(actions,getString(R.string.overlay_scan),getString(R.string.overlay_scan_explanation)+" Always available in the menu.",current.ocr,R.id.overlay_toggle_ocr,OverlayPreferences.OCR);
        toggle(actions,"Collect text","Combine selections from several screens",current.collect,R.id.overlay_toggle_collect,OverlayPreferences.COLLECT);
        toggle(actions,"Close after copying","Turn off to copy several selections in one capture",current.closeOnCopy,R.id.overlay_toggle_close,OverlayPreferences.CLOSE_ON_COPY);
        actions.addView(Ui.text(this,"Hidden shortcuts are still available in the toolbar's ⋮ menu.",12,Ui.MUTED));

        var highlights=Ui.card(this,body);highlights.addView(Ui.label(this,"TEXT HIGHLIGHTS"));
        choice(highlights,"Highlight color",OverlayPreferences.ACCENT_NAMES,current.accentIndex,n->{preferences.set(OverlayPreferences.ACCENT,n);updatePreview();});
        var dim=Ui.text(this,getString(R.string.overlay_dim_value,current.dimPercent),14,Ui.WHITE);highlights.addView(dim);
        var slider=new SeekBar(this);slider.setMax(60);slider.setProgress(current.dimPercent);slider.setContentDescription("Background dimming percentage");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){if(fromUser){preferences.set(OverlayPreferences.DIM,progress);dim.setText(getString(R.string.overlay_dim_value,progress));updatePreview();}}
            @Override public void onStartTrackingTouch(SeekBar bar){}
            @Override public void onStopTrackingTouch(SeekBar bar){}
        });highlights.addView(slider,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));
        toggle(highlights,"Show other text outlines","Keep unselected text outlined; selected text always stays highlighted",current.outlines,R.id.overlay_toggle_outlines,OverlayPreferences.OUTLINES);
        body.addView(Ui.button(this,"Reset toolbar defaults",false,v->new AlertDialog.Builder(this).setTitle("Reset toolbar?").setMessage("Your collected text will be kept.").setNegativeButton("Cancel",null).setPositiveButton("Reset",(d,w)->{preferences.reset();showSettings();}).show()));
        body.addView(Ui.button(this,"Done",true,v->finish()));
    }
    private void choice(LinearLayout parent,String title,String[] values,int selected,Consumer<Integer> changed) {
        parent.addView(Ui.text(this,title,15,Ui.WHITE));
        var group=new RadioGroup(this);group.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<values.length;i++) {
            var option=new RadioButton(this);option.setId(View.generateViewId());option.setText(values[i]);option.setTextColor(Ui.WHITE);option.setTextSize(14);
            option.setMinHeight(Ui.dp(this,48));group.addView(option);
            if(i==selected)group.check(option.getId());
        }
        group.setOnCheckedChangeListener((g,id)->{
            for(int i=0;i<g.getChildCount();i++)if(g.getChildAt(i).getId()==id){changed.accept(i);break;}
        });parent.addView(group);
    }
    private void toggle(LinearLayout parent,String title,String help,boolean value,int id,String key) {
        var row=new LinearLayout(this);row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        var label=Ui.column(this);label.addView(Ui.text(this,title,15,Ui.WHITE));label.addView(Ui.text(this,help,12,Ui.MUTED));
        row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        var toggle=new Switch(this);toggle.setId(id);toggle.setChecked(value);toggle.setContentDescription(title);toggle.setMinHeight(Ui.dp(this,48));
        toggle.setOnCheckedChangeListener((button,checked)->{preferences.set(key,checked);updatePreview();});row.addView(toggle);parent.addView(row);
    }
    private void updatePreview() {
        if(previewHost==null)return;
        var settings=preferences.read();previewHost.removeAllViews();
        var toolbar=new CaptureToolbar(this,settings,false,new CaptureToolbar.Actions(){
            private void sample(){Ui.toast(OverlaySettingsActivity.this,getString(R.string.overlay_preview_notice));}
            @Override public void copy(){sample();}@Override public void edit(){sample();}
            @Override public void selectAll(){sample();}@Override public void scan(){sample();}
            @Override public void collect(){sample();}@Override public void close(){sample();}
            @Override public void more(View anchor){sample();}
            @Override public void move(){sample();}
        });
        toolbar.update(2,8,getString(R.string.overlay_preview_sample));
        boolean showScene=getResources().getConfiguration().screenHeightDp>=700;
        var scene=new HighlightPreview(this,settings);
        if(showScene&&!settings.atTop)previewHost.addView(scene,new LinearLayout.LayoutParams(-1,Ui.dp(this,104)));
        var toolbarSize=new LinearLayout.LayoutParams(Math.min(getResources().getDisplayMetrics().widthPixels-Ui.dp(this,32),Ui.dp(this,settings.wide?520:340)),-2);
        toolbarSize.gravity=android.view.Gravity.CENTER_HORIZONTAL;previewHost.addView(toolbar,toolbarSize);
        if(showScene&&settings.atTop)previewHost.addView(scene,new LinearLayout.LayoutParams(-1,Ui.dp(this,104)));
        if(position!=null)position.setText(settings.freePosition?getString(R.string.overlay_free_position):getString(R.string.overlay_preview_position,settings.atTop?"top":"bottom"));
    }
    private static final class HighlightPreview extends View {
        private final OverlayPreferences.Snapshot settings;
        private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Rect bounds=new android.graphics.Rect();
        private final RegionDecoration decoration=new RegionDecoration();
        HighlightPreview(android.content.Context context,OverlayPreferences.Snapshot settings){super(context);this.settings=settings;setContentDescription("Preview of selected and unselected text highlights");}
        @Override protected void onDraw(android.graphics.Canvas canvas){
            super.onDraw(canvas);float d=getResources().getDisplayMetrics().density;
            canvas.drawColor(0xFF26332E);paint.setColor(Ui.WHITE);paint.setTextSize(16*d);
            canvas.drawText("Words worth keeping",18*d,34*d,paint);canvas.drawText("Des mots à garder.",18*d,78*d,paint);
            canvas.drawColor(Math.round(settings.dimPercent*255f/100f)<<24);
            bounds.set((int)(12*d),(int)(12*d),getWidth()-(int)(12*d),(int)(45*d));decoration.draw(canvas,bounds,false,settings,d);
            bounds.set((int)(12*d),(int)(57*d),getWidth()-(int)(12*d),(int)(88*d));decoration.draw(canvas,bounds,true,settings,d);
        }
    }
}
