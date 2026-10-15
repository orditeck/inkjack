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
    private final OverlayPreferences preferences;
    private OverlayPreferences.Snapshot settings;
    private FrameLayout root;
    private RegionView regionView;
    private CaptureToolbar toolbar;
    private ToolbarMover mover;
    private boolean attached;

    SelectionOverlay(InkAccessibilityService service,java.util.List<TextRegion> regions,Bitmap screenshot,Runnable scan) {
        this.service=service;this.regions=regions;this.screenshot=screenshot;this.scan=scan;
        manager=service.getSystemService(WindowManager.class);preferences=new OverlayPreferences(service);settings=preferences.read();
    }
    void open() {
        root=new FrameLayout(service);root.setFocusableInTouchMode(true);
        root.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)service.cancel();return true;}return false;});
        regionView=new RegionView();root.addView(regionView,new FrameLayout.LayoutParams(-1,-1));
        toolbar=new CaptureToolbar(service,settings,screenshot!=null,new CaptureToolbar.Actions(){
            @Override public void copy(){copySelection();}
            @Override public void edit(){editText(selected.isEmpty()?allText():chosen());}
            @Override public void selectAll(){toggleAll();}
            @Override public void scan(){scan.run();}
            @Override public void collect(){collectSelection();}
            @Override public void close(){service.cancel();}
            @Override public void more(View anchor){showMore(anchor);}
            @Override public void move(){mover.moveToOppositeEdge();}
        });
        root.addView(toolbar,new FrameLayout.LayoutParams(-1,-2));
        mover=new ToolbarMover(root,toolbar,preferences);refresh();
        var params=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START;params.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        params.setTitle("Inkjack text selection");manager.addView(root,params);attached=true;root.requestFocus();
    }
    private void toggleAll(){if(selected.cardinality()==regions.size())selected.clear();else selected.set(0,regions.size());refresh();}
    private void copySelection() {
        String text=chosen();if(text.isBlank())return;
        service.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Inkjack",text));
        if(settings.closeOnCopy)service.dismiss();Ui.toast(service,"Copied");
    }
    private void collectSelection() {
        try {
            if(Session.collection.add(chosen())) {service.dismiss();Ui.toast(service,service.getString(R.string.overlay_added));}
            else Ui.toast(service,service.getString(R.string.overlay_not_added));
        } catch(IllegalStateException e) {Ui.toast(service,e.getMessage());}
    }
    private void showMore(View anchor) {
        var popup=new PopupMenu(service,anchor);var menu=popup.getMenu();
        menu.add(0,1,0,R.string.overlay_scan_help);
        var collect=menu.add(0,2,1,R.string.overlay_collect);collect.setEnabled(!selected.isEmpty());
        menu.add(0,4,3,R.string.overlay_customize);
        popup.setOnMenuItemClickListener(item->{
            switch(item.getItemId()) {
                case 1:scan.run();break;
                case 2:collectSelection();break;
                case 4:service.dismiss();service.startActivity(new Intent(service,OverlaySettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));break;
                default:return false;
            }return true;
        });popup.show();
    }
    private void editText(String text){if(text.isBlank())return;Session.result=text;service.dismiss();service.startActivity(new Intent(service,ResultActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    private String allText(){var chunks=new ArrayList<String>();for(var region:regions)chunks.add(region.text);return TextTools.join(chunks);}
    private String chosen(){var chunks=new ArrayList<String>();for(int i=selected.nextSetBit(0);i>=0;i=selected.nextSetBit(i+1))chunks.add(regions.get(i).text);return TextTools.join(chunks);}
    private void refresh(){toolbar.update(selected.cardinality(),regions.size(),chosen());regionView.invalidate();}
    void close(){if(attached){manager.removeView(root);attached=false;}if(screenshot!=null&&!screenshot.isRecycled())screenshot.recycle();}

    private final class RegionView extends View {
        private final Paint bitmapPaint=new Paint(Paint.FILTER_BITMAP_FLAG);
        private final RegionDecoration decoration=new RegionDecoration();
        private final int[] origin=new int[2];
        private final GestureDetector gestures;
        RegionView(){
            super(service);setContentDescription(service.getString(R.string.overlay_view_description));
            gestures=new GestureDetector(service,new GestureDetector.SimpleOnGestureListener(){
                @Override public boolean onDown(MotionEvent e){return true;}
                @Override public boolean onSingleTapUp(MotionEvent e){int i=hit(e.getX(),e.getY());if(i>=0){selected.flip(i);refresh();performClick();}return true;}
                @Override public void onLongPress(MotionEvent e){int i=hit(e.getX(),e.getY());if(i>=0)editText(regions.get(i).text);}
            });
        }
        private int hit(float x,float y){
            getLocationOnScreen(origin);int best=-1;long area=Long.MAX_VALUE;
            for(int i=0;i<regions.size();i++){var r=regions.get(i).bounds;if(r.contains((int)x+origin[0],(int)y+origin[1])){long a=(long)r.width()*r.height();if(a<area){best=i;area=a;}}}return best;
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);getLocationOnScreen(origin);canvas.save();canvas.translate(-origin[0],-origin[1]);
            if(screenshot!=null&&!screenshot.isRecycled())canvas.drawBitmap(screenshot,0,0,bitmapPaint);
            canvas.drawColor((Math.round(settings.dimPercent*255f/100f)<<24));
            for(int i=0;i<regions.size();i++)decoration.draw(canvas,regions.get(i).bounds,selected.get(i),settings,getResources().getDisplayMetrics().density);
            canvas.restore();
        }
        @android.annotation.SuppressLint("ClickableViewAccessibility") // GestureDetector's single-tap callback invokes performClick.
        @Override public boolean onTouchEvent(MotionEvent event){return gestures.onTouchEvent(event);}
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
