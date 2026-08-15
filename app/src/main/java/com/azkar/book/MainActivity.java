package com.azkar.book;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private JSONObject data;
    private JSONArray topics;
    private LinearLayout list;
    private EditText search;
    private boolean favoritesOnly = false;
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = DataStore.prefs(this);
        try {
            data = DataStore.load(this);
            topics = data.getJSONArray("topics");
            buildUi();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تحميل محتوى التطبيق", Toast.LENGTH_LONG).show();
        }
    }

    private void buildUi() throws Exception {
        getWindow().setStatusBarColor(DataStore.green(this));
        getWindow().setNavigationBarColor(DataStore.bg(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(DataStore.dp(this,14), DataStore.dp(this,10), DataStore.dp(this,14), DataStore.dp(this,10));
        root.setBackgroundColor(DataStore.bg(this));
        DataStore.rtl(root);

        TextView title = new TextView(this);
        title.setText(data.optString("bookTitle", "قبس مختار من صحيح الأذكار"));
        title.setTextSize(23);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(DataStore.green(this));
        title.setGravity(Gravity.CENTER);
        title.setPadding(8, 8, 8, 4);
        root.addView(title, new LinearLayout.LayoutParams(-1,-2));

        TextView sub = new TextView(this);
        sub.setText("أذكار وأدعية مرتبة • بحث • مفضلة • عداد للتكرار");
        sub.setTextSize(14);
        sub.setTextColor(DataStore.muted(this));
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(4,0,4,10);
        root.addView(sub,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        Button fav = smallButton("☆ المفضلة");
        Button theme = smallButton(DataStore.dark(this) ? "☀ نهاري" : "☾ ليلي");
        actions.addView(fav, new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,-2,1); tp.setMargins(8,0,0,0);
        actions.addView(theme,tp);
        root.addView(actions,new LinearLayout.LayoutParams(-1,-2));

        fav.setOnClickListener(v -> {
            favoritesOnly = !favoritesOnly;
            fav.setText(favoritesOnly ? "★ عرض المفضلة" : "☆ المفضلة");
            renderList(search.getText().toString());
        });
        theme.setOnClickListener(v -> {
            prefs.edit().putBoolean("dark", !DataStore.dark(this)).apply();
            recreate();
        });

        int last = prefs.getInt("last_topic", -1);
        if (last >= 0 && last < topics.length()) {
            Button resume = new Button(this);
            resume.setAllCaps(false);
            resume.setText("↩ متابعة من آخر موضع");
            resume.setTextColor(DataStore.green(this));
            resume.setTextSize(16);
            resume.setBackground(DataStore.rounded(DataStore.softGreen(this), DataStore.dp(this,12), DataStore.border(this),1));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1,-2); rp.setMargins(0,10,0,8);
            root.addView(resume,rp);
            final int lastIndex=last;
            resume.setOnClickListener(v -> openTopic(lastIndex));
        }

        search = new EditText(this);
        search.setHint("ابحث في الفهرس أو داخل الأذكار...");
        search.setTextSize(16);
        search.setSingleLine(true);
        search.setTextColor(DataStore.text(this));
        search.setHintTextColor(DataStore.muted(this));
        search.setPadding(18,14,18,14);
        search.setBackground(DataStore.rounded(DataStore.card(this), DataStore.dp(this,14), DataStore.border(this),1));
        DataStore.rtl(search);
        root.addView(search,new LinearLayout.LayoutParams(-1,-2));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0,10,0,20);
        DataStore.rtl(list);
        scroll.addView(list,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){ renderList(s.toString()); }
            public void afterTextChanged(Editable e){}
        });
        renderList("");
    }

    private Button smallButton(String text) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextSize(14);
        b.setTextColor(DataStore.text(this));
        b.setBackground(DataStore.rounded(DataStore.card(this),DataStore.dp(this,12),DataStore.border(this),1));
        return b;
    }

    private void renderList(String q) {
        if (list == null || topics == null) return;
        list.removeAllViews();
        String query = normalize(q);
        int shown=0;
        try {
            for (int i=0;i<topics.length();i++) {
                JSONObject t=topics.getJSONObject(i);
                if (favoritesOnly && !hasFavorite(t)) continue;
                if (!query.isEmpty() && !matches(t,query)) continue;
                list.addView(topicCard(t,i)); shown++;
            }
        } catch (Exception ignored) {}
        if (shown==0) {
            TextView empty=new TextView(this);
            empty.setText(favoritesOnly ? "لا توجد أذكار في المفضلة حتى الآن" : "لا توجد نتائج مطابقة");
            empty.setTextSize(17); empty.setTextColor(DataStore.muted(this)); empty.setGravity(Gravity.CENTER); empty.setPadding(10,50,10,30);
            list.addView(empty,new LinearLayout.LayoutParams(-1,-2));
        }
    }

    private View topicCard(JSONObject topic, int index) throws Exception {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18,15,18,13);
        card.setBackground(DataStore.rounded(DataStore.card(this),DataStore.dp(this,14),DataStore.border(this),1));
        DataStore.rtl(card);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,0,0,9); card.setLayoutParams(cp);

        TextView t=new TextView(this);
        t.setText(topic.getString("title"));
        t.setTextSize(18); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); t.setTextColor(DataStore.text(this));
        t.setGravity(Gravity.RIGHT); DataStore.rtl(t);
        card.addView(t,new LinearLayout.LayoutParams(-1,-2));

        TextView p=new TextView(this);
        p.setText("صفحة الكتاب: " + DataStore.arabicNumber(topic.optInt("page",1)) + (hasFavorite(topic) ? "   ★" : ""));
        p.setTextSize(12); p.setTextColor(DataStore.muted(this)); p.setGravity(Gravity.RIGHT); p.setPadding(0,5,0,0);
        card.addView(p,new LinearLayout.LayoutParams(-1,-2));
        card.setOnClickListener(v -> openTopic(index));
        return card;
    }

    private void openTopic(int index) {
        Intent i=new Intent(this,DetailActivity.class); i.putExtra("topicIndex",index); startActivity(i);
    }

    private boolean hasFavorite(JSONObject topic) {
        try {
            String id=topic.getString("id"); JSONArray blocks=topic.getJSONArray("blocks");
            for(int b=0;b<blocks.length();b++) {
                JSONArray entries=blocks.getJSONObject(b).getJSONArray("entries");
                for(int e=0;e<entries.length();e++) if(prefs.getBoolean("fav_"+id+"_"+b+"_"+e,false)) return true;
            }
        } catch(Exception ignored){}
        return false;
    }

    private boolean matches(JSONObject topic,String q) {
        try {
            if(normalize(topic.getString("title")).contains(q)) return true;
            JSONArray blocks=topic.getJSONArray("blocks");
            for(int b=0;b<blocks.length();b++) {
                JSONObject block=blocks.getJSONObject(b);
                if(normalize(block.optString("heading","")).contains(q)) return true;
                JSONArray entries=block.getJSONArray("entries");
                for(int e=0;e<entries.length();e++) if(normalize(entries.getJSONObject(e).getString("text")).contains(q)) return true;
            }
        } catch(Exception ignored){}
        return false;
    }

    private String normalize(String s) {
        if(s==null) return "";
        return s.trim().replace("أ","ا").replace("إ","ا").replace("آ","ا").replace("ى","ي").replace("ؤ","و").replace("ئ","ي").toLowerCase();
    }

    @Override protected void onResume(){ super.onResume(); if(list!=null && search!=null) renderList(search.getText().toString()); }
}
