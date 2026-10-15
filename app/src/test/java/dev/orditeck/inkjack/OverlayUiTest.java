package dev.orditeck.inkjack;

import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class OverlayUiTest {
    @Before public void reset(){new OverlayPreferences(RuntimeEnvironment.getApplication()).reset();}

    @Test public void emptySelectionExplainsEditingAllAndPreventsEmptyCopy() {
        var context=RuntimeEnvironment.getApplication();var actions=new Actions();
        var toolbar=new CaptureToolbar(context,new OverlayPreferences(context).read(),false,actions);
        toolbar.update(0,4,"");
        var copy=toolbar.<Button>findViewById(R.id.overlay_copy);
        assertFalse(copy.isEnabled());
        assertEquals("Edit all text",toolbar.<Button>findViewById(R.id.overlay_edit).getText().toString());
        toolbar.<Button>findViewById(R.id.overlay_edit).performClick();assertEquals(1,actions.edits);
        toolbar.update(2,4,"First\nsecond");
        assertTrue(copy.isEnabled());assertEquals("Edit text",toolbar.<Button>findViewById(R.id.overlay_edit).getText().toString());
        copy.performClick();assertEquals(1,actions.copies);
        toolbar.update(4,4,"Everything");
        assertEquals("Clear selection",toolbar.<Button>findViewById(R.id.overlay_select_all).getText().toString());
    }
    @Test public void noDetectedTextLeavesOcrAvailableAndDoesNotOfferEmptyEditor() {
        var context=RuntimeEnvironment.getApplication();var toolbar=new CaptureToolbar(context,new OverlayPreferences(context).read(),true,new Actions());
        toolbar.update(0,0,"");
        assertFalse(toolbar.findViewById(R.id.overlay_copy).isEnabled());
        assertFalse(toolbar.findViewById(R.id.overlay_edit).isEnabled());
        assertTrue(toolbar.findViewById(R.id.overlay_scan).isEnabled());
        assertTrue(contains(toolbar,"No text found. Try Scan screen for text inside images."));
    }
    @Test public void hiddenShortcutAndPreviewPersistThroughSettingsRecreation() {
        try(var activity=Robolectric.buildActivity(OverlaySettingsActivity.class).setup()) {
            activity.get().<Switch>findViewById(R.id.overlay_toggle_ocr).setChecked(false);
            activity.get().<Switch>findViewById(R.id.overlay_toggle_preview).setChecked(false);
            assertFalse(new OverlayPreferences(activity.get()).read().ocr);
            var preview=activity.get().findViewById(R.id.overlay_settings_preview);
            assertNull(preview.findViewById(R.id.overlay_scan));
            activity.recreate();
            assertFalse(activity.get().<Switch>findViewById(R.id.overlay_toggle_ocr).isChecked());
            assertFalse(activity.get().<Switch>findViewById(R.id.overlay_toggle_preview).isChecked());
            assertNull(activity.get().findViewById(R.id.overlay_settings_preview).findViewById(R.id.overlay_scan));
        }
    }
    @Test public void preferencesCannotProduceInvalidDimmingOrColorIndices() {
        var context=RuntimeEnvironment.getApplication();
        context.getSharedPreferences("overlay",0).edit().putInt(OverlayPreferences.DIM,500).putInt(OverlayPreferences.ACCENT,-50).apply();
        var settings=new OverlayPreferences(context).read();assertEquals(60,settings.dimPercent);assertEquals(Ui.LIME,settings.accent);
    }
    private boolean contains(View view,String text){
        if(view instanceof TextView t&&t.getText().toString().equals(text))return true;
        if(view instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++)if(contains(g.getChildAt(i),text))return true;
        return false;
    }
    static class Actions implements CaptureToolbar.Actions {
        int edits,copies;
        @Override public void copy(){copies++;}@Override public void edit(){edits++;}
        @Override public void selectAll(){}@Override public void scan(){}@Override public void collect(){}
        @Override public void close(){}@Override public void more(View anchor){}
    }
}
