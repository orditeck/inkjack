package dev.orditeck.inkjack;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;

public final class ResultActivity extends Activity {
    private EditText editor;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        var body=Ui.page(this); body.addView(Ui.label(this,"INKJACK  /  THE GETAWAY"));
        var title=Ui.text(this,"Make it yours.",32,Ui.WHITE); title.setTypeface(null,Typeface.BOLD); body.addView(title);
        body.addView(Ui.text(this,"Edit the text, or long-press to select just a piece.",14,Ui.MUTED));
        editor=new EditText(this); editor.setId(R.id.editor); editor.setTextColor(Ui.WHITE); editor.setHintTextColor(Ui.MUTED); editor.setTextSize(17); editor.setGravity(Gravity.TOP);
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editor.setBackground(Ui.shape(Ui.CARD,Ui.dp(this,18))); int p=Ui.dp(this,16);editor.setPadding(p,p,p,p);
        editor.setMinLines(7); editor.setMaxLines(16); editor.setHint("Nothing here yet. Capture some text to get started.");
        editor.setText(state==null ? Session.result : state.getString("draft",""));
        body.addView(editor,new LinearLayout.LayoutParams(-1,-2));
        body.addView(Ui.button(this,"Copy text",true,v->{String text=chosen();if(text.isBlank()){Ui.toast(this,"Select or enter some text first.");return;}getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Inkjack",text));Ui.toast(this,"Copied");}));
        body.addView(Ui.button(this,"Share text",false,v->{String text=chosen();if(text.isBlank())return;startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),"Share text"));}));
        body.addView(Ui.button(this,"Add to stash",false,v->{try{boolean added=Session.collection.add(chosen());Ui.toast(this,added ? "Added to stash" : "Already in stash, or empty");}catch(IllegalStateException e){Ui.toast(this,e.getMessage());}}));
        body.addView(Ui.button(this,"Extract links, emails, and more",false,v->entities()));
        body.addView(Ui.button(this,"Done",false,v->finish()));
    }
    private String chosen() { int a=editor.getSelectionStart(),b=editor.getSelectionEnd();return a>=0&&b>a ? editor.getText().subSequence(a,b).toString() : editor.getText().toString(); }
    private void entities() {
        String source=chosen();
        new AlertDialog.Builder(this).setTitle("Extract from text").setItems(new String[]{"Emails","Links","Phone numbers","@handles","#hashtags"},(d,n)->{
            var values=TextTools.entities(source,TextTools.Kind.values()[n]);
            if(values.isEmpty()) Ui.toast(this,"No matches found.");
            else new AlertDialog.Builder(this).setTitle(values.size()+" found").setMessage(String.join("\n",values)).setNegativeButton("Cancel",null).setPositiveButton("Use these",(dialog,w)->editor.setText(String.join("\n",values))).show();
        }).show();
    }
    @Override public void onSaveInstanceState(Bundle out) {out.putString("draft",editor.getText().toString());super.onSaveInstanceState(out);}
}
