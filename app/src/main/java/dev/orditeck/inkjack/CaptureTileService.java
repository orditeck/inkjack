package dev.orditeck.inkjack;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.*;

public final class CaptureTileService extends TileService {
    @Override public void onStartListening() {
        super.onStartListening();
        var tile=getQsTile();if(tile!=null){tile.setState(Tile.STATE_ACTIVE);tile.setLabel("Lift text");tile.updateTile();}
    }
    @Override public void onClick() {
        super.onClick();
        if(isLocked()) {unlockAndRun(this::launch);return;}
        launch();
    }
    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated") // The PendingIntent overload requires API 34.
    private void launch() {
        var intent=new Intent(this,CaptureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(Build.VERSION.SDK_INT>=34) startActivityAndCollapse(PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
        else startActivityAndCollapse(intent);
    }
}
