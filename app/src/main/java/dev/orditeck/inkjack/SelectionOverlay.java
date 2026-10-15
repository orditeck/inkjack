package dev.orditeck.inkjack;

import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Accessibility overlay needs no SYSTEM_ALERT_WINDOW permission. */
final class SelectionOverlay {
    private final InkAccessibilityService service;
    private final java.util.List<TextRegion> regions;
    private final BitSet selected=new BitSet();
    private final Bitmap screenshot;
    private final Runnable scan;
    private final WindowManager manager;
    private android.widget.FrameLayout root;
    private RegionView regionView;
    private TextView count;
    private boolean attached;
    SelectionOverlay(InkAccessibilityService service, java.util.List<TextRegion> regions, Bitmap screenshot,Runnable scan) {
        this.service=service;this.regions=regions;this.screenshot=screenshot;this.scan=scan;this.manager=service.getSystemService(WindowManager.class);
    }
    void open() {
        root=new FrameLayout(service);root.setFocusableInTouchMode(true);
        root.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)service.cancel();return true;}return false;});
        regionView=new RegionView();root.addView(regionView,new FrameLayout.LayoutParams(-1,-1));
        var panel=Ui.column(service);int pad=Ui.dp(service,14);panel.setPadding(pad,pad,pad,pad);panel.setBackground(Ui.shape(Ui.BG,Ui.dp(service,24)));
        panel.addView(Ui.label(service,screenshot==null ? "INKJACK  /  SCREEN TEXT" : "INKJACK  /  OFFLINE OCR"));
        count=Ui.text(service,regions.isEmpty() ? "No screen text found. Try Scan image." : "Tap a box, or review the text below.",14,Ui.WHITE);panel.addView(count);
        var row=new LinearLayout(service);
        add(row,"All",()->{if(selected.cardinality()==regions.size())selected.clear();else selected.set(0,regions.size());refresh();});
        add(row,"Review",()->review(selected.isEmpty() ? allText() : chosen()));
        add(row,"Scan image",scan);
        panel.addView(row);
        var actions=new LinearLayout(service);
        add(actions,"Copy",()->{String text=chosen();if(text.isBlank()){Ui.toast(service,"Tap some text, or tap All first.");return;}service.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Inkjack",text));service.dismiss();Ui.toast(service,"Copied");});
        add(actions,"Stash",()->{try{if(Session.collection.add(chosen())){service.dismiss();Ui.toast(service,"Added. Scroll or switch apps, then lift again.");}else Ui.toast(service,"Select some new text first.");}catch(IllegalStateException e){Ui.toast(service,e.getMessage());}});
        add(actions,"Close",service::cancel);
        panel.addView(actions);
        var panelParams=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);panelParams.setMargins(pad,pad,pad,pad);root.addView(panel,panelParams);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            var bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            panelParams.setMargins(pad+bars.left,pad,pad+bars.right,pad+bars.bottom);panel.setLayoutParams(panelParams);return insets;
        });
        var params=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START;params.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        params.setTitle("Inkjack text selection");
        manager.addView(root,params);attached=true;root.requestFocus();
    }
    private void add(LinearLayout row,String text,Runnable action) {
        var button=Ui.button(service,text,false,v->action.run());
        button.setTextSize(13);button.setPadding(Ui.dp(service,4),Ui.dp(service,8),Ui.dp(service,4),Ui.dp(service,8));
        var p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(Ui.dp(service,2),0,Ui.dp(service,2),0);row.addView(button,p);
    }
    private void review(String text) {Session.result=text;service.dismiss();service.startActivity(new Intent(service,ResultActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    private String allText() {var chunks=new ArrayList<String>();for(var region:regions)chunks.add(region.text);return TextTools.join(chunks);}
    private String chosen() {var chunks=new ArrayList<String>();for(int i=selected.nextSetBit(0);i>=0;i=selected.nextSetBit(i+1))chunks.add(regions.get(i).text);return TextTools.join(chunks);}
    private void refresh() {count.setText(service.getString(R.string.selection_count,selected.cardinality(),regions.size()));regionView.invalidate();}
    void close() {if(attached){manager.removeView(root);attached=false;}if(screenshot!=null&&!screenshot.isRecycled())screenshot.recycle();}
    private final class RegionView extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF drawBounds=new RectF();
        private final int[] origin=new int[2];
        private final GestureDetector gestures;
        RegionView(){
            super(service);setContentDescription("Detected text regions. Use Review for an accessible text editor.");
            gestures=new GestureDetector(service,new GestureDetector.SimpleOnGestureListener(){
                @Override public boolean onDown(MotionEvent e){return true;}
                @Override public boolean onSingleTapUp(MotionEvent e){int i=hit(e.getX(),e.getY());if(i>=0){selected.flip(i);refresh();performClick();}return true;}
                @Override public void onLongPress(MotionEvent e){int i=hit(e.getX(),e.getY());if(i>=0)review(regions.get(i).text);}
            });
        }
        private int hit(float x,float y){
            getLocationOnScreen(origin);int best=-1;long area=Long.MAX_VALUE;
            for(int i=0;i<regions.size();i++){var r=regions.get(i).bounds;if(r.contains((int)x+origin[0],(int)y+origin[1])){long a=(long)r.width()*r.height();if(a<area){best=i;area=a;}}}
            return best;
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);getLocationOnScreen(origin);canvas.save();canvas.translate(-origin[0],-origin[1]);
            if(screenshot!=null&&!screenshot.isRecycled())canvas.drawBitmap(screenshot,0,0,paint);
            canvas.drawColor(0x44000000);
            paint.setStrokeWidth(Ui.dp(service,1.5f));
            for(int i=0;i<regions.size();i++){
                var r=drawBounds;r.set(regions.get(i).bounds);paint.setStyle(Paint.Style.FILL);paint.setColor(selected.get(i)?0x66D9FF6A:0x20111111);canvas.drawRoundRect(r,5,5,paint);
                paint.setStyle(Paint.Style.STROKE);paint.setColor(selected.get(i)?Ui.LIME:0x99FFFFFF);canvas.drawRoundRect(r,5,5,paint);
            }
            paint.setStyle(Paint.Style.FILL);canvas.restore();
        }
        @android.annotation.SuppressLint("ClickableViewAccessibility") // GestureDetector's single-tap callback invokes performClick.
        @Override public boolean onTouchEvent(MotionEvent event){return gestures.onTouchEvent(event);}
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
