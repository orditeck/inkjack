package dev.orditeck.inkjack;

import android.content.*;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class AndroidBehaviorTest {
    @Before public void reset() { Session.result="";Session.collection.clear(); }
    @Test public void startsHomeWithoutRequestingScreenAccessOrReadingAnything() {
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            var activity=controller.get();
            assertTrue(findText(activity.getWindow().getDecorView(),"Text isn't\ntrapped anymore."));
            assertTrue(findText(activity.getWindow().getDecorView(),"Enable screen capture"));
            assertEquals(0,Session.collection.size());
            assertNull(InkAccessibilityService.instance);
        }
    }
    @Test public void copyUsesSelectedSubstringAndSurvivesRecreation() {
        Session.result="First sentence. Second sentence.";
        try(var controller=Robolectric.buildActivity(ResultActivity.class).setup()) {
            var editor=controller.get().<EditText>findViewById(R.id.editor);editor.setSelection(16,32);
            findButton(controller.get().getWindow().getDecorView(),"Copy text").performClick();
            var clipboard=controller.get().getSystemService(ClipboardManager.class);
            assertEquals("Second sentence.",clipboard.getPrimaryClip().getItemAt(0).getText().toString());
            editor.setText("Edited draft");controller.recreate();
            assertEquals("Edited draft",controller.get().<EditText>findViewById(R.id.editor).getText().toString());
        }
    }
    @Test public void excludesPasswordTextAndOffscreenNodes() {
        var password=AccessibilityNodeInfo.obtain();password.setVisibleToUser(true);password.setPassword(true);password.setText("secret");password.setBoundsInScreen(new Rect(0,0,300,50));
        assertTrue(NodeReader.read(password,new Rect(0,0,500,500)).isEmpty());
        var offscreen=AccessibilityNodeInfo.obtain();offscreen.setVisibleToUser(true);offscreen.setText("offscreen");offscreen.setBoundsInScreen(new Rect(0,600,300,650));
        assertTrue(NodeReader.read(offscreen,new Rect(0,0,500,500)).isEmpty());
    }
    @Test public void clampsVisibleBoundsAndKeepsText() {
        var node=AccessibilityNodeInfo.obtain();node.setVisibleToUser(true);node.setText("  Québec  ");node.setBoundsInScreen(new Rect(-10,5,300,50));
        var regions=NodeReader.read(node,new Rect(0,0,200,200));
        assertEquals(1,regions.size());assertEquals("Québec",regions.get(0).text);assertEquals(new Rect(0,5,200,50),regions.get(0).bounds);
    }
    private static boolean findText(View view,String text) {
        if(view instanceof TextView t && t.getText().toString().equals(text))return true;
        if(view instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++)if(findText(g.getChildAt(i),text))return true;
        return false;
    }
    private static Button findButton(View view,String text) {
        if(view instanceof Button b && b.getText().toString().equals(text))return b;
        if(view instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++){var b=findButton(g.getChildAt(i),text);if(b!=null)return b;}
        return null;
    }
}
