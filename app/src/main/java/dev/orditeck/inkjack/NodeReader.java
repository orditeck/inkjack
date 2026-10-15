package dev.orditeck.inkjack;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;

final class NodeReader {
    static List<TextRegion> read(AccessibilityNodeInfo root, Rect screen) {
        return readScreen(root,screen).regions;
    }
    static final class ScreenText {
        final List<TextRegion> regions=new ArrayList<>();
        boolean passwordPresent,truncated;
        boolean needsOcr(){return regions.isEmpty()&&!passwordPresent&&!truncated;}
    }
    static ScreenText readScreen(AccessibilityNodeInfo root, Rect screen) {
        var capture=new ScreenText();
        var result=capture.regions;
        if (root == null) return capture;
        var queue = new ArrayDeque<AccessibilityNodeInfo>();
        var visited = new HashSet<AccessibilityNodeInfo>();
        var duplicates = new HashSet<String>();
        queue.add(root);
        try {
            while (!queue.isEmpty() && visited.size() < 10_000) {
                var node = queue.remove();
                if (!visited.add(node)) { node.recycle(); continue; }
                if (!node.isVisibleToUser()) continue;
                if (node.isPassword()) {capture.passwordPresent=true;continue;}
                var bounds = new Rect(); node.getBoundsInScreen(bounds);
                // A zero-sized virtual container can still have visible descendants.
                boolean intersects = Rect.intersects(bounds, screen);
                var raw = node.getText();
                if ((raw == null || raw.toString().isBlank()) && node.getChildCount() == 0) raw = node.getContentDescription();
                if (raw != null && !raw.toString().isBlank() && intersects) {
                    String text = raw.toString().strip();
                    if(!bounds.intersect(screen)) continue;
                    String key = text + "\u0000" + bounds.flattenToString();
                    if (duplicates.add(key)) result.add(new TextRegion(text,bounds));
                }
                for (int i=0; i<node.getChildCount(); i++) { var child=node.getChild(i); if(child!=null) queue.add(child); }
            }
            capture.truncated=!queue.isEmpty();
        } finally {
            for (var node:visited) node.recycle();
            for (var node:queue) node.recycle();
        }
        result.sort(Comparator.comparingInt((TextRegion r)->r.bounds.top).thenComparingInt(r->r.bounds.left));
        return capture;
    }
}
