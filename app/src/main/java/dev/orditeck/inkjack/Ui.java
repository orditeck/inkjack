package dev.orditeck.inkjack;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;

final class Ui {
    static final int BG = Color.rgb(17,22,20), CARD = Color.rgb(30,38,34), LIME = Color.rgb(217,255,106), WHITE = Color.rgb(244,246,236), MUTED = Color.rgb(169,184,172);
    static int dp(Context c, float n) { return (int) (n * c.getResources().getDisplayMetrics().density + .5f); }
    static GradientDrawable shape(int color, int radius) { var d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d; }
    static LinearLayout column(Context c) { var v = new LinearLayout(c); v.setOrientation(LinearLayout.VERTICAL); return v; }
    static TextView text(Context c, String value, int size, int color) {
        var t = new TextView(c); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setPadding(0,dp(c,5),0,dp(c,5)); return t;
    }
    static TextView label(Context c, String value) {
        var t = text(c,value,12,LIME); t.setTypeface(Typeface.MONOSPACE); t.setLetterSpacing(.12f); return t;
    }
    static Button button(Context c, String value, boolean primary, View.OnClickListener action) {
        var b = new Button(c); b.setText(value); b.setAllCaps(false); b.setTextSize(15); b.setTextColor(primary ? BG : WHITE);
        b.setBackground(shape(primary ? LIME : CARD,dp(c,16))); b.setMinHeight(dp(c,52)); b.setPadding(dp(c,16),dp(c,10),dp(c,16),dp(c,10)); b.setOnClickListener(action);
        var p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(c,6),0,dp(c,6)); b.setLayoutParams(p); return b;
    }
    static LinearLayout page(Activity a) {
        var scroll = new ScrollView(a); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        var body = column(a); int pad = dp(a,24); body.setPadding(pad,pad,pad,pad); scroll.addView(body);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{ var bars=insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()); v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return WindowInsets.CONSUMED; });
        a.setContentView(scroll); return body;
    }
    static LinearLayout card(Context c, LinearLayout parent) {
        var v=column(c); int p=dp(c,18); v.setPadding(p,p,p,p); v.setBackground(shape(CARD,dp(c,22)));
        var params=new LinearLayout.LayoutParams(-1,-2); params.setMargins(0,dp(c,14),0,dp(c,8)); parent.addView(v,params); return v;
    }
    static void gap(LinearLayout p,int n) { var v=new View(p.getContext()); p.addView(v,new LinearLayout.LayoutParams(1,dp(p.getContext(),n))); }
    static void toast(Context c,String text) { Toast.makeText(c,text,Toast.LENGTH_LONG).show(); }
}
