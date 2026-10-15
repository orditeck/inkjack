package dev.orditeck.inkjack;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/** Presentation preferences only. Never stores captured text or screen images. */
final class OverlayPreferences {
    static final String TOP="toolbar_top", COMPACT="toolbar_compact", PREVIEW="selection_preview",
        OCR="ocr_shortcut", COLLECT="collect_shortcut", CLOSE_ON_COPY="close_on_copy",
        OUTLINES="unselected_outlines", DIM="screen_dim", ACCENT="highlight_accent", WIDE="wide_toolbar";
    private static final String FREE="free_position", X="position_x", Y="position_y";
    static final String[] ACCENT_NAMES={"Lime","Sky","Lilac","Coral"};
    static final int[] ACCENTS={Ui.LIME,Color.rgb(137,206,255),Color.rgb(204,175,255),Color.rgb(255,178,153)};
    private final SharedPreferences prefs;

    OverlayPreferences(Context context) { prefs=context.getSharedPreferences("overlay",Context.MODE_PRIVATE); }
    Snapshot read() {
        return new Snapshot(prefs.getBoolean(TOP,false),prefs.getBoolean(COMPACT,true),
            prefs.getBoolean(PREVIEW,true),prefs.getBoolean(OCR,false),prefs.getBoolean(COLLECT,true),
            prefs.getBoolean(CLOSE_ON_COPY,true),prefs.getBoolean(OUTLINES,true),
            Math.max(0,Math.min(60,prefs.getInt(DIM,18))),
            Math.max(0,Math.min(ACCENTS.length-1,prefs.getInt(ACCENT,0))),
            prefs.getBoolean(WIDE,false),prefs.getBoolean(FREE,false),
            fraction(prefs.getFloat(X,.5f)),fraction(prefs.getFloat(Y,.5f)));
    }
    void set(String key,boolean value) { prefs.edit().putBoolean(key,value).apply(); }
    void set(String key,int value) { prefs.edit().putInt(key,value).apply(); }
    void dock(boolean top) { prefs.edit().putBoolean(TOP,top).putBoolean(FREE,false).apply(); }
    void savePosition(float x,float y) { prefs.edit().putBoolean(FREE,true).putFloat(X,fraction(x)).putFloat(Y,fraction(y)).apply(); }
    private static float fraction(float value) { return Float.isFinite(value)?Math.max(0,Math.min(1,value)):.5f; }
    void reset() { prefs.edit().clear().apply(); }

    static final class Snapshot {
        final boolean atTop,compact,preview,ocr,collect,closeOnCopy,outlines;
        final int dimPercent,accentIndex,accent;
        final boolean wide,freePosition;
        final float positionX,positionY;
        Snapshot(boolean top,boolean compact,boolean preview,boolean ocr,boolean collect,
                 boolean close,boolean outlines,int dim,int accentIndex,boolean wide,boolean free,float x,float y) {
            atTop=top;this.compact=compact;this.preview=preview;this.ocr=ocr;this.collect=collect;
            closeOnCopy=close;this.outlines=outlines;dimPercent=dim;this.accentIndex=accentIndex;
            accent=ACCENTS[accentIndex];
            this.wide=wide;freePosition=free;positionX=x;positionY=y;
        }
    }
}
