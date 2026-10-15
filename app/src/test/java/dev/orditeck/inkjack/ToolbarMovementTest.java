package dev.orditeck.inkjack;

import android.graphics.Insets;
import android.view.*;
import android.widget.FrameLayout;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35,qualifiers="w800dp-h1000dp-mdpi")
public class ToolbarMovementTest {
    @Before public void reset(){new OverlayPreferences(RuntimeEnvironment.getApplication()).reset();}

    @Test public void headerDragsBothAxesAndRestoresWithoutActivatingCopy() {
        var f=new Fixture();int x=f.toolbar.getLeft(),y=f.toolbar.getTop();
        f.drag(100,-250);assertEquals(x+100,f.toolbar.getLeft());assertEquals(y-250,f.toolbar.getTop());
        assertTrue(f.prefs.read().freePosition);assertEquals(0,f.actions.copies);
        var restored=new Fixture();assertEquals(f.toolbar.getLeft(),restored.toolbar.getLeft());
        assertEquals(f.toolbar.getTop(),restored.toolbar.getTop());
        int left=restored.toolbar.getLeft(),top=restored.toolbar.getTop();
        restored.drag(2,2);assertEquals(left,restored.toolbar.getLeft());assertEquals(top,restored.toolbar.getTop());
    }
    @Test public void draggingCannotHideToolbarBehindInsetsOrOutsideTheScreen() {
        var f=new Fixture();
        f.root.dispatchApplyWindowInsets(new WindowInsets.Builder().setInsets(WindowInsets.Type.systemBars(),Insets.of(30,40,20,50)).build());
        f.layout(800,1000);f.drag(-5000,-5000);
        assertEquals(42,f.toolbar.getLeft());assertEquals(52,f.toolbar.getTop());
        assertEquals("Move toolbar to bottom",f.toolbar.findViewById(R.id.overlay_move).getContentDescription());
        f.drag(5000,5000);
        assertEquals(800-20-12,f.toolbar.getRight());assertEquals(1000-50-12,f.toolbar.getBottom());
        assertEquals("Move toolbar to top",f.toolbar.findViewById(R.id.overlay_move).getContentDescription());
    }
    @Test public void arrowMovesToOppositeEdgeAndClearsFreePosition() {
        var f=new Fixture();f.drag(50,-100);
        f.toolbar.findViewById(R.id.overlay_move).performClick();f.layout(800,1000);
        assertEquals(12,f.toolbar.getTop());assertTrue(f.prefs.read().atTop);assertFalse(f.prefs.read().freePosition);
        f.toolbar.findViewById(R.id.overlay_move).performClick();f.layout(800,1000);
        assertEquals(988,f.toolbar.getBottom());assertFalse(f.prefs.read().atTop);
    }
    @Test public void relativePositionStaysOnScreenAfterSizeChangeAndInvalidValuesAreClamped() {
        var f=new Fixture();f.drag(100,-300);var saved=f.prefs.read();f.layout(430,500);
        assertTrue(f.toolbar.getLeft()>=12);assertTrue(f.toolbar.getRight()<=418);
        assertTrue(f.toolbar.getTop()>=12);assertTrue(f.toolbar.getBottom()<=488);
        assertEquals(saved.positionX,f.prefs.read().positionX,0);assertEquals(saved.positionY,f.prefs.read().positionY,0);
        f.prefs.savePosition(Float.NaN,Float.POSITIVE_INFINITY);var safe=f.prefs.read();
        assertEquals(.5f,safe.positionX,0);assertEquals(.5f,safe.positionY,0);
    }
    private static final class Fixture {
        final OverlayPreferences prefs=new OverlayPreferences(RuntimeEnvironment.getApplication());
        final FrameLayout root=new FrameLayout(RuntimeEnvironment.getApplication());
        final ToolbarMover mover;
        final CaptureToolbar toolbar;
        final OverlayUiTest.Actions actions;
        Fixture() {
            var holder=new ToolbarMover[1];
            actions=new OverlayUiTest.Actions(){@Override public void move(){holder[0].moveToOppositeEdge();}};
            toolbar=new CaptureToolbar(root.getContext(),prefs.read(),false,actions);toolbar.update(1,3,"Selected text");
            root.addView(toolbar,new FrameLayout.LayoutParams(-1,-2));mover=new ToolbarMover(root,toolbar,prefs);holder[0]=mover;
            layout(800,1000);
        }
        void layout(int width,int height) {
            for(int i=0;i<3;i++){
                root.forceLayout();
                root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
                root.layout(0,0,width,height);
            }
        }
        void drag(float dx,float dy) {
            var handle=toolbar.findViewById(R.id.overlay_drag);
            send(handle,MotionEvent.ACTION_DOWN,50,25);send(handle,MotionEvent.ACTION_MOVE,50+dx,25+dy);send(handle,MotionEvent.ACTION_UP,50+dx,25+dy);
            layout(root.getWidth(),root.getHeight());
        }
        void send(View handle,int action,float x,float y) {
            var event=MotionEvent.obtain(0,10,action,x,y,0);handle.dispatchTouchEvent(event);event.recycle();
        }
    }
}
