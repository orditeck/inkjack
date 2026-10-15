package dev.orditeck.inkjack;

import android.app.Activity;
import android.os.Bundle;
import java.nio.charset.StandardCharsets;

public final class LicensesActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        var body=Ui.page(this);body.addView(Ui.label(this,"INKJACK / OPEN SOURCE"));
        body.addView(Ui.button(this,"Back",true,v->finish()));
        try(var input=getAssets().open("THIRD_PARTY_NOTICES.txt")) {
            var bytes=new java.io.ByteArrayOutputStream();
            byte[] buffer=new byte[8192];int count;
            while((count=input.read(buffer))!=-1)bytes.write(buffer,0,count);
            var text=Ui.text(this,new String(bytes.toByteArray(),StandardCharsets.UTF_8),13,Ui.WHITE);
            text.setTextIsSelectable(true);body.addView(text);
        } catch(java.io.IOException e) {body.addView(Ui.text(this,"License notices are available in the source repository.",16,Ui.WHITE));}
    }
}
