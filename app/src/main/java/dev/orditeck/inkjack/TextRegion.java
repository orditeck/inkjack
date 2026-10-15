package dev.orditeck.inkjack;

import android.graphics.Rect;

public final class TextRegion {
    public final String text;
    public final Rect bounds;
    public TextRegion(String text, Rect bounds) {
        this.text = text.strip();
        this.bounds = new Rect(bounds);
    }
}
