package dev.orditeck.inkjack;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

final class OcrEngine {
    static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    interface Callback { void done(List<TextRegion> regions, String error); }
    private static String language(Context c) { return c.getSharedPreferences("settings",0).getString("language","eng+fra"); }
    private static TessBaseAPI create(Context c) throws IOException {
        var dir = new File(c.getFilesDir(),"ocr/tessdata");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Could not prepare OCR storage.");
        for (String lang : new String[]{"eng","fra"}) {
            var dest = new File(dir,lang+".traineddata");
            if (!dest.isFile()) {
                var tmp=new File(dir,lang+".tmp");
                try (var input=c.getAssets().open("tessdata/"+lang+".traineddata")) { Files.copy(input,tmp.toPath(),StandardCopyOption.REPLACE_EXISTING); }
                Files.move(tmp.toPath(),dest.toPath(),StandardCopyOption.REPLACE_EXISTING);
            }
        }
        var api = new TessBaseAPI();
        if (!api.init(dir.getParent(),language(c))) { api.recycle(); throw new IOException("OCR could not load its language models."); }
        api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT);
        return api;
    }
    private static List<TextRegion> recognize(TessBaseAPI api, Bitmap original) {
        // Downsample large screens and composite transparency onto white before native OCR.
        float scale=Math.min(1f,3000f/Math.max(original.getWidth(),original.getHeight()));
        int w=Math.max(1,Math.round(original.getWidth()*scale)), h=Math.max(1,Math.round(original.getHeight()*scale));
        var input=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        var canvas=new Canvas(input); canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(original,null,new Rect(0,0,w,h),new Paint(Paint.FILTER_BITMAP_FLAG));
        try {
            api.setImage(input);
            var text=api.getUTF8Text();
            var out=new ArrayList<TextRegion>();
            var iterator=api.getResultIterator();
            if (iterator!=null) {
                try {
                    iterator.begin();
                    do {
                        String line=iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE);
                        int[] box=iterator.getBoundingBox(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE);
                        if (line!=null && !line.isBlank() && box!=null && box.length==4)
                            out.add(new TextRegion(line,new Rect(Math.round(box[0]/scale),Math.round(box[1]/scale),Math.round(box[2]/scale),Math.round(box[3]/scale))));
                    } while(iterator.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE));
                } finally { iterator.delete(); }
            }
            if (out.isEmpty() && text!=null && !text.isBlank()) out.add(new TextRegion(text,new Rect(0,0,original.getWidth(),original.getHeight())));
            return out;
        } finally { api.clear(); input.recycle(); }
    }
    /** Takes ownership of bitmap. Callback is delivered on the main thread. */
    static void bitmap(Context context, Bitmap bitmap, Callback callback) {
        var app=context.getApplicationContext();
        WORKER.execute(()->{
            TessBaseAPI api=null; List<TextRegion> result=List.of(); String error=null;
            try { api=create(app); result=recognize(api,bitmap); }
            catch(Exception e) { error="Couldn't read this image. Try a clearer screenshot."; }
            finally { if(api!=null) api.recycle(); bitmap.recycle(); }
            var finalResult=result; var finalError=error;
            MAIN.post(()->callback.done(finalResult,finalError));
        });
    }
    static void document(Context context, Uri uri, Callback callback) {
        var app=context.getApplicationContext();
        WORKER.execute(()->{
            TessBaseAPI api=null; List<TextRegion> result=new ArrayList<>(); String error=null;
            try {
                api=create(app);
                String type=app.getContentResolver().getType(uri);
                if ("application/pdf".equals(type)) {
                    try(var fd=app.getContentResolver().openFileDescriptor(uri,"r")) {
                        if(fd==null) throw new IOException("No file");
                        try(var pdf=new PdfRenderer(fd)) {
                            if(pdf.getPageCount()>20) throw new IOException("This PDF has more than 20 pages. Split it into smaller files first.");
                            for(int n=0;n<pdf.getPageCount();n++) {
                                try(var page=pdf.openPage(n)) {
                                    float scale=2000f/Math.max(page.getWidth(),page.getHeight());
                                    var image=Bitmap.createBitmap(Math.max(1,Math.round(page.getWidth()*scale)),Math.max(1,Math.round(page.getHeight()*scale)),Bitmap.Config.ARGB_8888);
                                    try {
                                        image.eraseColor(Color.WHITE); page.render(image,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                                        var lines=recognize(api,image);
                                        if(!lines.isEmpty()) result.add(new TextRegion("[Page "+(n+1)+"]",new Rect()));
                                        result.addAll(lines);
                                    } finally { image.recycle(); }
                                }
                            }
                        }
                    }
                } else {
                    var source=ImageDecoder.createSource(app.getContentResolver(),uri);
                    var image=ImageDecoder.decodeBitmap(source,(decoder,info,s)->{
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        int longest=Math.max(info.getSize().getWidth(),info.getSize().getHeight());
                        decoder.setTargetSampleSize(Math.max(1,(int)Math.ceil(longest/3000.0)));
                    });
                    try { result=recognize(api,image); } finally { image.recycle(); }
                }
            } catch(Exception e) { error=e instanceof IOException && e.getMessage()!=null && e.getMessage().contains("20 pages") ? e.getMessage() : "Couldn't open this file. Use an unencrypted PDF (up to 20 pages) or an image."; }
            finally { if(api!=null) api.recycle(); }
            var finalResult=result; var finalError=error;
            MAIN.post(()->callback.done(finalResult,finalError));
        });
    }
}
