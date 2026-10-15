package dev.orditeck.inkjack;

import android.graphics.Insets;
import android.view.*;
import android.widget.FrameLayout;

/** Moves only the toolbar. Text-region coordinates stay attached to the screen. */
final class ToolbarMover {
    private final FrameLayout root;
    private final CaptureToolbar toolbar;
    private final OverlayPreferences preferences;
    private final FrameLayout.LayoutParams params;
    private final int pad,slop;
    private Insets insets=Insets.NONE;
    private boolean tracking,moved;
    private float downX,downY;
    private int startLeft,startTop;

    ToolbarMover(FrameLayout root,CaptureToolbar toolbar,OverlayPreferences preferences) {
        this.root=root;this.toolbar=toolbar;this.preferences=preferences;
        pad=Ui.dp(root.getContext(),12);slop=ViewConfiguration.get(root.getContext()).getScaledTouchSlop();
        params=(FrameLayout.LayoutParams)toolbar.getLayoutParams();
        toolbar.setDragListener(this::touch);
        root.setOnApplyWindowInsetsListener((v,value)->{
            insets=value.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            finishDrag();layout();return value;
        });
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            if(r-l!=or-ol||b-t!=ob-ot){finishDrag();layout();}
        });
        toolbar.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            if(r-l!=or-ol||b-t!=ob-ot)layout();
        });
        layout();
    }
    private int minX(){return pad+insets.left;}
    private int minY(){return pad+insets.top;}
    private int maxX(){return Math.max(minX(),root.getWidth()-insets.right-pad-toolbar.getMeasuredWidth());}
    private int maxY(){return Math.max(minY(),root.getHeight()-insets.bottom-pad-toolbar.getMeasuredHeight());}
    private static int clamp(int value,int min,int max){return Math.max(min,Math.min(max,value));}

    @android.annotation.SuppressLint("RtlHardcoded") // Coordinates use physical screen insets in either layout direction.
    void layout() {
        var settings=preferences.read();
        int available=Math.max(1,root.getWidth()-insets.left-insets.right-2*pad);
        int width=root.getWidth()>0?Math.min(available,Ui.dp(root.getContext(),settings.wide?520:340)):-1;
        // Measure before computing bounds, including after rotation or font-size changes.
        if(width>0)toolbar.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(Math.max(1,root.getHeight()-minY()-insets.bottom-pad),View.MeasureSpec.AT_MOST));
        int left=minX()+Math.round((maxX()-minX())*(settings.freePosition?settings.positionX:.5f));
        int top=settings.freePosition?minY()+Math.round((maxY()-minY())*settings.positionY):settings.atTop?minY():maxY();
        if(params.width!=width||params.gravity!=(Gravity.TOP|Gravity.LEFT)||params.leftMargin!=left||params.topMargin!=top){
            params.width=width;params.gravity=Gravity.TOP|Gravity.LEFT;
            params.setMargins(left,top,pad+insets.right,pad+insets.bottom);toolbar.setLayoutParams(params);
        }
        updateArrow(top);
    }
    private void updateArrow(int top) {
        boolean toTop=maxY()==minY()?!preferences.read().atTop:top>=minY()+(maxY()-minY())/2f;
        toolbar.setMoveTarget(toTop);
    }
    void moveToOppositeEdge() {
        finishDrag();
        boolean toTop=maxY()==minY()?!preferences.read().atTop:params.topMargin>=minY()+(maxY()-minY())/2f;
        preferences.dock(toTop);layout();
    }
    private boolean touch(View handle,MotionEvent event) {
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                tracking=true;moved=false;downX=event.getRawX();downY=event.getRawY();
                startLeft=params.leftMargin;startTop=params.topMargin;return true;
            case MotionEvent.ACTION_MOVE:
                if(!tracking)return true;
                float dx=event.getRawX()-downX,dy=event.getRawY()-downY;
                if(!moved&&Math.hypot(dx,dy)<=slop)return true;
                moved=true;handle.getParent().requestDisallowInterceptTouchEvent(true);
                params.leftMargin=clamp(Math.round(startLeft+dx),minX(),maxX());
                params.topMargin=clamp(Math.round(startTop+dy),minY(),maxY());
                toolbar.setLayoutParams(params);updateArrow(params.topMargin);return true;
            case MotionEvent.ACTION_UP:
                boolean clicked=tracking&&!moved;finishDrag();if(clicked)handle.performClick();return true;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                finishDrag();return true;
            default:return true;
        }
    }
    private void finishDrag() {
        if(tracking&&moved)preferences.savePosition(
            maxX()>minX()?(params.leftMargin-minX())/(float)(maxX()-minX()):preferences.read().positionX,
            maxY()>minY()?(params.topMargin-minY())/(float)(maxY()-minY()):preferences.read().positionY);
        tracking=false;moved=false;
    }
}
