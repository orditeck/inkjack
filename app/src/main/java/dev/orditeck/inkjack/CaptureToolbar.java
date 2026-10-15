package dev.orditeck.inkjack;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** Shared by the real accessibility overlay and the settings preview. */
@android.annotation.SuppressLint("ViewConstructor") // Built programmatically with explicit settings and callbacks.
final class CaptureToolbar extends LinearLayout {
    interface Actions {
        void copy(); void edit(); void selectAll(); void scan(); void collect(); void close();
        void more(View anchor);
    }
    private final TextView count,preview;
    private final Button copy,edit,selectAll,scan,collect;
    private final OverlayPreferences.Snapshot settings;
    private final boolean ocrMode;

    CaptureToolbar(Context context,OverlayPreferences.Snapshot settings,boolean ocrMode,Actions actions) {
        super(context);this.settings=settings;this.ocrMode=ocrMode;
        setOrientation(VERTICAL);setElevation(Ui.dp(context,12));
        int inset=Ui.dp(context,settings.compact?12:16);
        setPadding(inset,Ui.dp(context,6),inset,Ui.dp(context,6));
        var background=Ui.shape(Ui.BG,Ui.dp(context,22));
        background.setStroke(Ui.dp(context,1),0xFF354239);setBackground(background);

        var header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);
        var headings=Ui.column(context);
        var title=Ui.text(context,context.getString(R.string.overlay_title),17,Ui.WHITE);
        title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);
        title.setTypeface(null,Typeface.BOLD);title.setPadding(Ui.dp(context,4),0,0,0);
        headings.addView(title);
        count=Ui.text(context,"",11,Ui.MUTED);count.setPadding(Ui.dp(context,4),Ui.dp(context,2),0,0);
        count.setSingleLine(true);count.setEllipsize(TextUtils.TruncateAt.END);
        headings.addView(count);header.addView(headings,new LayoutParams(0,-2,1));
        header.addView(icon(R.drawable.ic_more,R.string.overlay_more,v->actions.more(v)));
        header.addView(icon(R.drawable.ic_close,R.string.overlay_close,v->actions.close()));
        addView(header,new LayoutParams(-1,Ui.dp(context,Math.max(48,getResources().getConfiguration().fontScale*34+10))));

        preview=Ui.text(context,"",13,Ui.MUTED);preview.setMaxLines(settings.compact?1:2);
        preview.setEllipsize(TextUtils.TruncateAt.END);preview.setPadding(Ui.dp(context,4),Ui.dp(context,1),Ui.dp(context,4),Ui.dp(context,7));
        preview.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        if(settings.preview)addView(preview,new LayoutParams(-1,-2));

        var main=new LinearLayout(context);
        copy=button(R.string.overlay_copy,true,false,v->actions.copy());copy.setId(R.id.overlay_copy);
        edit=button(R.string.overlay_edit_all,false,false,v->actions.edit());edit.setId(R.id.overlay_edit);
        main.addView(copy,weighted(0,4));main.addView(edit,weighted(4,0));addView(main);

        var shortcuts=new LinearLayout(context);
        selectAll=button(R.string.overlay_select_all,false,true,v->actions.selectAll());selectAll.setId(R.id.overlay_select_all);
        scan=button(R.string.overlay_scan,false,true,v->actions.scan());scan.setId(R.id.overlay_scan);
        collect=button(R.string.overlay_collect,false,true,v->actions.collect());collect.setId(R.id.overlay_collect);
        selectAll.setTooltipText(context.getString(R.string.overlay_select_help));
        scan.setTooltipText(context.getString(R.string.overlay_scan_help));scan.setContentDescription(context.getString(R.string.overlay_scan_help));
        collect.setTooltipText(context.getString(R.string.overlay_collect_help));
        shortcuts.addView(selectAll,weighted(0,0));
        if(settings.ocr)shortcuts.addView(scan,weighted(0,0));
        if(settings.collect)shortcuts.addView(collect,weighted(0,0));
        addView(shortcuts);update(0,0,"");
    }
    private LayoutParams weighted(int left,int right) {
        var p=new LayoutParams(0,actionHeight(),1);
        p.setMargins(Ui.dp(getContext(),left),0,Ui.dp(getContext(),right),0);return p;
    }
    private int actionHeight() {
        float scale=getResources().getConfiguration().fontScale;
        return Ui.dp(getContext(),Math.max(settings.compact?48:54,scale*30+12));
    }
    private ImageButton icon(int drawable,int label,OnClickListener action) {
        var b=new ImageButton(getContext());var image=getContext().getDrawable(drawable).mutate();image.setTint(Ui.MUTED);
        b.setImageDrawable(image);b.setContentDescription(getContext().getString(label));b.setTooltipText(getContext().getString(label));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22FFFFFF),Ui.shape(0x00000000,Ui.dp(getContext(),24)),null));
        b.setPadding(Ui.dp(getContext(),13),Ui.dp(getContext(),13),Ui.dp(getContext(),13),Ui.dp(getContext(),13));
        b.setLayoutParams(new LayoutParams(Ui.dp(getContext(),48),Ui.dp(getContext(),48)));b.setOnClickListener(action);return b;
    }
    private Button button(int label,boolean primary,boolean quiet,OnClickListener action) {
        var b=new Button(getContext());b.setText(label);b.setAllCaps(false);b.setTypeface(null,Typeface.BOLD);
        b.setTextSize(quiet?12:14);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);
        b.setMaxLines(2);b.setEllipsize(TextUtils.TruncateAt.END);b.setGravity(Gravity.CENTER);b.setLetterSpacing(0);
        b.setPadding(Ui.dp(getContext(),6),Ui.dp(getContext(),4),Ui.dp(getContext(),6),Ui.dp(getContext(),4));
        b.setTextColor(primary?Ui.BG:quiet?settings.accent:Ui.WHITE);
        var shape=Ui.shape(primary?settings.accent:quiet?0x00000000:Ui.CARD,Ui.dp(getContext(),13));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(primary?0x22000000:0x22FFFFFF),shape,null));
        b.setStateListAnimator(null);b.setOnClickListener(action);return b;
    }
    void update(int selected,int total,String text) {
        count.setText(getContext().getString(ocrMode?R.string.overlay_count_ocr:R.string.overlay_count_screen,selected,total));
        preview.setText(total==0?getContext().getString(R.string.overlay_empty):selected==0?getContext().getString(R.string.overlay_hint):text.replace('\n',' '));
        preview.setTextColor(selected==0?Ui.MUTED:Ui.WHITE);
        copy.setEnabled(selected>0);copy.setAlpha(selected>0?1f:.38f);
        edit.setEnabled(total>0);edit.setAlpha(total>0?1f:.4f);
        edit.setText(selected==0?R.string.overlay_edit_all:R.string.overlay_edit);
        selectAll.setText(selected>0&&selected==total?R.string.overlay_clear:R.string.overlay_select_all);
        selectAll.setEnabled(total>0);selectAll.setAlpha(total>0?1f:.4f);
        collect.setEnabled(selected>0);collect.setAlpha(selected>0?1f:.4f);
        if(total==0)preview.setVisibility(VISIBLE);else preview.setVisibility(settings.preview?VISIBLE:GONE);
        // Keep the empty-state help visible even when selection previews are disabled.
        if(total==0&&preview.getParent()==null)addView(preview,1,new LayoutParams(-1,-2));
    }
}
