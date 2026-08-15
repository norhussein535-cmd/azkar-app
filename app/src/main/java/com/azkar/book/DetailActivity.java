package com.azkar.book;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class DetailActivity extends Activity {
    private JSONObject data, topic;
    private JSONArray topics;
    private int topicIndex;
    private SharedPreferences prefs;
    private float fontSize;

    private static class CounterTag {
        final String key;
        final int target;
        CounterTag(String key, int target) { this.key = key; this.target = target; }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs=DataStore.prefs(this);
        fontSize=prefs.getFloat("font_size",19f);
        try {
            data=DataStore.load(this); topics=data.getJSONArray("topics");
            topicIndex=getIntent().getIntExtra("topicIndex",0);
            if(topicIndex<0 || topicIndex>=topics.length()) topicIndex=0;
            topic=topics.getJSONObject(topicIndex);
            prefs.edit().putInt("last_topic",topicIndex).apply();
            buildUi();
        } catch(Exception e){ Toast.makeText(this,"تعذر فتح الذكر",Toast.LENGTH_LONG).show(); finish(); }
    }

    private void buildUi() throws Exception {
        getWindow().setStatusBarColor(DataStore.green(this));
        getWindow().setNavigationBarColor(DataStore.bg(this));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(DataStore.bg(this)); DataStore.rtl(root);

        LinearLayout bar=new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(8,7,8,7); bar.setBackgroundColor(DataStore.green(this));
        Button back=barButton("‹"); back.setTextSize(28); bar.addView(back,new LinearLayout.LayoutParams(-2,-2)); back.setOnClickListener(v->finish());
        TextView title=new TextView(this); title.setText(topic.getString("title")); title.setTextColor(Color.WHITE); title.setTextSize(18); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setGravity(Gravity.CENTER); title.setMaxLines(2); DataStore.rtl(title); bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button minus=barButton("A−"); Button plus=barButton("A+"); Button theme=barButton(DataStore.dark(this)?"☀":"☾");
        bar.addView(minus);bar.addView(plus);bar.addView(theme); root.addView(bar,new LinearLayout.LayoutParams(-1,-2));
        minus.setOnClickListener(v->{fontSize=Math.max(15,fontSize-2);prefs.edit().putFloat("font_size",fontSize).apply();recreate();});
        plus.setOnClickListener(v->{fontSize=Math.min(30,fontSize+2);prefs.edit().putFloat("font_size",fontSize).apply();recreate();});
        theme.setOnClickListener(v->{prefs.edit().putBoolean("dark",!DataStore.dark(this)).apply();recreate();});

        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(14,14,14,24); content.setBackgroundColor(DataStore.bg(this)); DataStore.rtl(content);
        TextView source=new TextView(this); source.setText("صفحة الكتاب: "+DataStore.arabicNumber(topic.optInt("page",1))); source.setTextColor(DataStore.muted(this)); source.setTextSize(13); source.setGravity(Gravity.RIGHT); source.setPadding(4,0,4,10); content.addView(source);

        JSONArray blocks=topic.getJSONArray("blocks"); String tid=topic.getString("id");
        for(int b=0;b<blocks.length();b++) {
            JSONObject block=blocks.getJSONObject(b); String heading=block.optString("heading","").trim();
            if(!heading.isEmpty()) content.addView(sectionHeading(heading));
            JSONArray entries=block.getJSONArray("entries");
            for(int e=0;e<entries.length();e++) content.addView(entryCard(entries.getJSONObject(e),tid,b,e));
        }

        LinearLayout nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setPadding(0,10,0,10);
        Button prev=navButton("السابق"); Button next=navButton("التالي");
        nav.addView(prev,new LinearLayout.LayoutParams(0,-2,1)); LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-2,1);np.setMargins(8,0,0,0);nav.addView(next,np); content.addView(nav);
        prev.setEnabled(topicIndex>0); next.setEnabled(topicIndex<topics.length()-1);
        prev.setOnClickListener(v->open(topicIndex-1)); next.setOnClickListener(v->open(topicIndex+1));

        scroll.addView(content,new ScrollView.LayoutParams(-1,-2)); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }

    private TextView sectionHeading(String s){
        TextView v=new TextView(this); v.setText(s); v.setTextColor(DataStore.green(this)); v.setTextSize(fontSize+2); v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); v.setGravity(Gravity.RIGHT); v.setPadding(8,16,8,10); DataStore.rtl(v); return v;
    }

    private boolean isMorningEveningTopic(){
        return "أذكار الصباح والمساء".equals(topic.optString("title",""));
    }

    private JSONArray effectiveCounters(JSONObject entry) throws Exception {
        JSONArray original=entry.optJSONArray("counters");
        if(original!=null && original.length()>0) return original;
        if(isMorningEveningTopic()){
            JSONArray one=new JSONArray();
            JSONObject c=new JSONObject(); c.put("target",1); c.put("label","مرة واحدة"); one.put(c);
            return one;
        }
        return original;
    }

    private View entryCard(JSONObject entry,String tid,int bi,int ei) throws Exception {
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(16,16,16,12); card.setBackground(DataStore.rounded(DataStore.card(this),DataStore.dp(this,14),DataStore.border(this),1)); DataStore.rtl(card);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,12);card.setLayoutParams(cp);
        String text=entry.getString("text");
        JSONArray cs=effectiveCounters(entry);

        TextView body=new TextView(this);
        body.setText(styledBody(text));
        body.setTextSize(fontSize);
        body.setGravity(Gravity.RIGHT);
        body.setLineSpacing(0,1.42f);
        body.setTextIsSelectable(false);
        body.setPadding(4,4,4,6);
        body.setClickable(cs!=null && cs.length()>0);
        body.setFocusable(cs!=null && cs.length()>0);
        DataStore.rtl(body);
        card.addView(body,new LinearLayout.LayoutParams(-1,-2));

        if(cs!=null && cs.length()>0){
            TextView hint=new TextView(this);
            hint.setText("اضغط على نص الذكر نفسه ليزيد العداد • عند اكتمال العدد سيهتز الهاتف • ضغط مطوّل على العداد للتصفير");
            hint.setTextSize(12);
            hint.setTextColor(DataStore.muted(this));
            hint.setGravity(Gravity.RIGHT);
            hint.setPadding(0,10,0,6);
            card.addView(hint);
            for(int c=0;c<cs.length();c++) card.addView(counterView(cs.getJSONObject(c),tid,bi,ei,c));
            final JSONArray counters=cs;
            body.setOnClickListener(v->incrementFromBody(card,counters,tid,bi,ei,body));
        }

        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setPadding(0,10,0,0);
        String favKey="fav_"+tid+"_"+bi+"_"+ei;
        Button fav=actionButton(prefs.getBoolean(favKey,false)?"★":"☆"); Button copy=actionButton("نسخ"); Button share=actionButton("مشاركة");
        actions.addView(fav,new LinearLayout.LayoutParams(0,-2,1)); LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(0,-2,1);x.setMargins(6,0,0,0);actions.addView(copy,x); LinearLayout.LayoutParams y=new LinearLayout.LayoutParams(0,-2,1);y.setMargins(6,0,0,0);actions.addView(share,y); card.addView(actions);
        fav.setOnClickListener(v->{boolean nv=!prefs.getBoolean(favKey,false);prefs.edit().putBoolean(favKey,nv).apply();fav.setText(nv?"★":"☆");});
        copy.setOnClickListener(v->{ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("ذكر",text));Toast.makeText(this,"تم النسخ",Toast.LENGTH_SHORT).show();});
        share.setOnClickListener(v->{Intent s=new Intent(Intent.ACTION_SEND);s.setType("text/plain");s.putExtra(Intent.EXTRA_TEXT,text+"\n\n— قبس مختار من صحيح الأذكار");startActivity(Intent.createChooser(s,"مشاركة الذكر"));});
        return card;
    }

    private CharSequence styledBody(String text){
        SpannableString s=new SpannableString(text);
        int sanadColor=DataStore.dark(this)?Color.rgb(213,176,124):Color.rgb(132,92,45);
        int matnColor=DataStore.dark(this)?Color.rgb(154,220,180):Color.rgb(32,103,67);
        int start=findMatnStart(text);
        if(start>0 && start<text.length()){
            s.setSpan(new ForegroundColorSpan(sanadColor),0,start,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new ForegroundColorSpan(matnColor),start,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new StyleSpan(Typeface.BOLD),start,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }else{
            s.setSpan(new ForegroundColorSpan(matnColor),0,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    private int findMatnStart(String text){
        int best=-1;
        char[] quotes={'"','«','“','❝'};
        for(char q:quotes){ int p=text.indexOf(q); if(p>=0 && (best<0 || p<best)) best=p; }
        if(best>=0) return best;
        String[] markers={"قال رسول الله صلى الله عليه وسلم:","قال النبي صلى الله عليه وسلم:","قال صلى الله عليه وسلم:","فقال صلى الله عليه وسلم:"};
        for(String m:markers){
            int p=text.indexOf(m);
            if(p>=0){ int colon=text.indexOf(':',p); if(colon>=0 && colon+1<text.length()) return colon+1; }
        }
        return -1;
    }

    private void vibrateCompleted(){
        try{
            Vibrator vibrator=(Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
            if(vibrator==null || !vibrator.hasVibrator()) return;
            if(Build.VERSION.SDK_INT>=26) vibrator.vibrate(VibrationEffect.createOneShot(180,VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(180);
        }catch(Exception ignored){}
    }

    private void incrementFromBody(LinearLayout card,JSONArray cs,String tid,int bi,int ei,View hapticView){
        try{
            for(int c=0;c<cs.length();c++){
                JSONObject obj=cs.getJSONObject(c);
                int target=obj.optInt("target",1);
                String key="count_"+tid+"_"+bi+"_"+ei+"_"+c;
                int n=prefs.getInt(key,0);
                if(n<target){
                    n++;
                    prefs.edit().putInt(key,n).apply();
                    hapticView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    refreshCounterButtons(card);
                    if(n==target){
                        vibrateCompleted();
                        if(allCountersDone(cs,tid,bi,ei)) Toast.makeText(this,"تم الذكر بالكامل",Toast.LENGTH_SHORT).show();
                        else Toast.makeText(this,"اكتمل هذا العدد",Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
            }
            Toast.makeText(this,"اكتمل العدد",Toast.LENGTH_SHORT).show();
        }catch(Exception ignored){}
    }

    private boolean allCountersDone(JSONArray cs,String tid,int bi,int ei) throws Exception{
        for(int c=0;c<cs.length();c++){
            int target=cs.getJSONObject(c).optInt("target",1);
            String key="count_"+tid+"_"+bi+"_"+ei+"_"+c;
            if(prefs.getInt(key,0)<target) return false;
        }
        return true;
    }

    private void refreshCounterButtons(ViewGroup group){
        for(int i=0;i<group.getChildCount();i++){
            View v=group.getChildAt(i);
            if(v instanceof Button && v.getTag() instanceof CounterTag){
                CounterTag t=(CounterTag)v.getTag();
                updateCounterButton((Button)v,t.key,t.target);
            }
            if(v instanceof ViewGroup) refreshCounterButtons((ViewGroup)v);
        }
    }

    private View counterView(JSONObject c,String tid,int bi,int ei,int ci){
        int target=c.optInt("target",1); String label=c.optString("label",target+" مرات"); String key="count_"+tid+"_"+bi+"_"+ei+"_"+ci;
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(10,9,10,9);box.setBackground(DataStore.rounded(DataStore.softGreen(this),DataStore.dp(this,12),DataStore.border(this),1)); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.setMargins(0,0,0,7);box.setLayoutParams(bp);
        TextView l=new TextView(this);l.setText(label+" — "+DataStore.arabicNumber(target)+" "+(target==1?"مرة":"مرات"));l.setTextSize(Math.max(14,fontSize-3));l.setTextColor(DataStore.text(this));l.setGravity(Gravity.RIGHT);DataStore.rtl(l);box.addView(l);
        Button count=new Button(this);count.setAllCaps(false);count.setTextSize(20);count.setTypeface(Typeface.DEFAULT,Typeface.BOLD);count.setTag(new CounterTag(key,target));box.addView(count,new LinearLayout.LayoutParams(-1,-2));
        updateCounterButton(count,key,target);
        count.setOnClickListener(v->{int n=prefs.getInt(key,0); if(n<target){n++;prefs.edit().putInt(key,n).apply();v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);updateCounterButton(count,key,target);if(n==target){vibrateCompleted();Toast.makeText(this,"تم العدد",Toast.LENGTH_SHORT).show();}}else Toast.makeText(this,"اكتمل العدد — اضغط مطولا للتصفير",Toast.LENGTH_SHORT).show();});
        count.setOnLongClickListener(v->{prefs.edit().putInt(key,0).apply();updateCounterButton(count,key,target);Toast.makeText(this,"تم تصفير العداد",Toast.LENGTH_SHORT).show();return true;});
        return box;
    }

    private void updateCounterButton(Button b,String key,int target){
        int n=prefs.getInt(key,0); if(n>target)n=target;
        b.setText(DataStore.arabicNumber(n)+" / "+DataStore.arabicNumber(target));
        boolean done=n>=target; b.setTextColor(done?Color.WHITE:DataStore.green(this)); b.setBackground(DataStore.rounded(done?DataStore.green(this):DataStore.card(this),DataStore.dp(this,12),DataStore.green(this),1));
    }

    private Button barButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setBackgroundColor(Color.TRANSPARENT);b.setPadding(8,2,8,2);return b;}
    private Button actionButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(13);b.setTextColor(DataStore.green(this));b.setBackground(DataStore.rounded(DataStore.softGreen(this),DataStore.dp(this,10),DataStore.border(this),1));return b;}
    private Button navButton(String s){Button b=actionButton(s);b.setTextSize(15);return b;}
    private void open(int idx){Intent i=new Intent(this,DetailActivity.class);i.putExtra("topicIndex",idx);startActivity(i);finish();}
}
