package com.azkar.book;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private PdfRenderer renderer;
    private PdfRenderer.Page page;
    private ImageView image;
    private TextView counter;
    private EditText jump;
    private int current = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(244, 239, 226));
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        buildUi();
        try {
            File pdf = new File(getCacheDir(), "book.pdf");
            if (!pdf.exists() || pdf.length() == 0) copyAsset(pdf);
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(fd);
            render(0);
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح الكتاب", Toast.LENGTH_LONG).show();
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 18, 18, 18);
        root.setBackgroundColor(Color.rgb(250, 247, 239));

        TextView title = new TextView(this);
        title.setText("قبس مختار من صحيح الأذكار");
        title.setTextSize(22);
        title.setTextColor(Color.rgb(47, 78, 62));
        title.setGravity(Gravity.CENTER);
        title.setPadding(8, 10, 8, 14);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.CENTER);

        Button index = button("الفهرس");
        index.setOnClickListener(v -> showIndex());
        tools.addView(index);

        jump = new EditText(this);
        jump.setHint("صفحة");
        jump.setInputType(InputType.TYPE_CLASS_NUMBER);
        jump.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams jp = new LinearLayout.LayoutParams(170, -2);
        jp.setMargins(12,0,12,0);
        tools.addView(jump, jp);

        Button go = button("اذهب");
        go.setOnClickListener(v -> {
            try {
                int n = Integer.parseInt(jump.getText().toString().trim()) - 1;
                if (renderer != null && n >= 0 && n < renderer.getPageCount()) render(n);
                else Toast.makeText(this, "رقم الصفحة غير صحيح", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "اكتب رقم الصفحة", Toast.LENGTH_SHORT).show();
            }
        });
        tools.addView(go);
        root.addView(tools, new LinearLayout.LayoutParams(-1, -2));

        counter = new TextView(this);
        counter.setGravity(Gravity.CENTER);
        counter.setTextSize(15);
        counter.setPadding(4, 10, 4, 10);
        root.addView(counter, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        scroll.addView(image, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        Button next = button("التالي");
        Button prev = button("السابق");
        next.setOnClickListener(v -> { if (renderer != null && current + 1 < renderer.getPageCount()) render(current + 1); });
        prev.setOnClickListener(v -> { if (current > 0) render(current - 1); });
        nav.addView(next, new LinearLayout.LayoutParams(0, -2, 1));
        nav.addView(prev, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    private void showIndex() {
        final String[] items = {
                "فهرس الكتاب الكامل",
                "أذكار الصباح والمساء",
                "    ↳ سيد الاستغفار",
                "    ↳ مزيد من الحروز المضمونة"
        };
        new AlertDialog.Builder(this)
                .setTitle("الفهرس")
                .setItems(items, (dialog, which) -> {
                    if (renderer == null) return;
                    if (which == 0) render(1);
                    else render(Math.min(29, renderer.getPageCount() - 1));
                })
                .setNegativeButton("إغلاق", null)
                .show();
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        return b;
    }

    private void copyAsset(File out) throws Exception {
        try (InputStream in = getAssets().open("book.pdf"); FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
        }
    }

    private void render(int index) {
        if (renderer == null) return;
        if (page != null) page.close();
        current = index;
        page = renderer.openPage(index);
        int width = Math.max(720, getResources().getDisplayMetrics().widthPixels - 40);
        int height = Math.max(1, width * page.getHeight() / page.getWidth());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        image.setImageBitmap(bitmap);
        counter.setText("صفحة " + (index + 1) + " من " + renderer.getPageCount());
    }

    @Override
    protected void onDestroy() {
        if (page != null) page.close();
        if (renderer != null) renderer.close();
        super.onDestroy();
    }
}
