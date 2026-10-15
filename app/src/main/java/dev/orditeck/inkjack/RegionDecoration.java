package dev.orditeck.inkjack;

import android.graphics.*;

/** Same highlight treatment for the capture canvas and the live settings preview. */
final class RegionDecoration {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds=new RectF();
    void draw(Canvas canvas,Rect region,boolean selected,OverlayPreferences.Snapshot settings,float density) {
        if(!selected&&!settings.outlines)return;
        bounds.set(region);float corner=5*density;
        if(selected){paint.setStyle(Paint.Style.FILL);paint.setColor((settings.accent&0x00FFFFFF)|0x38000000);canvas.drawRoundRect(bounds,corner,corner,paint);}
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth((selected?2f:1f)*density);
        paint.setColor(selected?settings.accent:0x668EA49A);canvas.drawRoundRect(bounds,corner,corner,paint);
    }
}
