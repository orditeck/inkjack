package dev.orditeck.inkjack;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.*;
import static org.junit.Assert.*;

/** Actual Android view rendering with native Skia; these are not HTML mockups. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class RenderingTest {
    @Test public void rendersHomeAndEditorAtPhoneSize() throws Exception {
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            render(controller.get().getWindow().getDecorView(),"inkjack-home.png");
        }
        Session.result="Some words are worth keeping.\n\nLes mots sont à toi.\n\nhttps://example.ca\nhello@example.ca";
        try(var controller=Robolectric.buildActivity(ResultActivity.class).setup()) {
            render(controller.get().getWindow().getDecorView(),"inkjack-editor.png");
        }
    }
    @Test public void rendersCaptureToolbarAndPinnedCustomizationPreview() throws Exception {
        var context=org.robolectric.RuntimeEnvironment.getApplication();var prefs=new OverlayPreferences(context);prefs.reset();
        prefs.savePosition(.5f,.62f);var settings=prefs.read();var frame=new FrameLayout(context);frame.setBackgroundColor(0xFF26332E);
        frame.addView(new View(context){
            private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            private final android.graphics.Rect bounds=new android.graphics.Rect();
            private final RegionDecoration decoration=new RegionDecoration();
            @Override protected void onDraw(Canvas canvas){
                paint.setColor(Ui.WHITE);paint.setTextSize(25);paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);canvas.drawText("Weekend notes",26,72,paint);
                paint.setTypeface(android.graphics.Typeface.DEFAULT);paint.setTextSize(17);
                canvas.drawText("Coffee tomorrow?",26,160,paint);canvas.drawText("Meet by the river at 10:30.",26,213,paint);
                paint.setColor(Ui.MUTED);canvas.drawText("Bring a book and take your time.",26,294,paint);
                canvas.drawColor(Math.round(settings.dimPercent*255f/100f)<<24);
                bounds.set(18,131,273,176);decoration.draw(canvas,bounds,true,settings,1);
                bounds.set(18,184,335,229);decoration.draw(canvas,bounds,true,settings,1);
                bounds.set(18,266,350,310);decoration.draw(canvas,bounds,false,settings,1);
            }
        },new FrameLayout.LayoutParams(-1,-1));
        var toolbar=new CaptureToolbar(context,settings,false,new OverlayUiTest.Actions());
        toolbar.update(2,3,"Coffee tomorrow?\nMeet by the river at 10:30.");
        frame.addView(toolbar,new FrameLayout.LayoutParams(-1,-2));new ToolbarMover(frame,toolbar,prefs);
        render(frame,"inkjack-toolbar.png");
        try(var activity=Robolectric.buildActivity(OverlaySettingsActivity.class).setup()) {
            render(activity.get().getWindow().getDecorView(),"inkjack-customize.png");
        }
    }
    @Test public void compactToolbarFitsNarrowWidthAtLargeFontSize() {
        var context=org.robolectric.RuntimeEnvironment.getApplication();var configuration=context.getResources().getConfiguration();configuration.fontScale=1.5f;
        context.getResources().updateConfiguration(configuration,context.getResources().getDisplayMetrics());
        var prefs=new OverlayPreferences(context);prefs.reset();
        prefs.set(OverlayPreferences.OCR,true);
        var toolbar=new CaptureToolbar(context,prefs.read(),false,new OverlayUiTest.Actions());toolbar.update(1,3,"Selected words");
        toolbar.measure(View.MeasureSpec.makeMeasureSpec(296,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.AT_MOST));
        toolbar.layout(0,0,296,toolbar.getMeasuredHeight());
        assertTrue(toolbar.getMeasuredHeight()<330);
        for(int id:new int[]{R.id.overlay_copy,R.id.overlay_edit,R.id.overlay_select_all,R.id.overlay_scan,R.id.overlay_collect}){
            var button=toolbar.<Button>findViewById(id);assertTrue(button.getWidth()>0);assertTrue(button.getHeight()>=48);
            assertTrue(button.getLayout().getLineCount()<=2);
            for(int line=0;line<button.getLayout().getLineCount();line++)assertEquals(button.getText().toString(),0,button.getLayout().getEllipsisCount(line));
        }
    }
    private void render(View view,String name) throws Exception {
        for(int pass=0;pass<3;pass++){
            view.forceLayout();
            view.measure(View.MeasureSpec.makeMeasureSpec(411,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(891,View.MeasureSpec.EXACTLY));
            view.layout(0,0,411,891);
        }
        var bitmap=Bitmap.createBitmap(411,891,Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        var dir=new File("build/previews");assertTrue(dir.isDirectory()||dir.mkdirs());
        try(var output=new FileOutputStream(new File(dir,name))) {assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output));}
        var colors=new java.util.HashSet<Integer>();
        for(int y=0;y<891;y+=7)for(int x=0;x<411;x+=7)colors.add(bitmap.getPixel(x,y));
        assertTrue("The view must paint real content",colors.size()>20);
        bitmap.recycle();
    }
}
