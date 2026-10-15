package dev.orditeck.inkjack;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
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
    private void render(View view,String name) throws Exception {
        view.measure(View.MeasureSpec.makeMeasureSpec(411,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(891,View.MeasureSpec.EXACTLY));
        view.layout(0,0,411,891);
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
