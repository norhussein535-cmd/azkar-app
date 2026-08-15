import fitz, json, unicodedata, re, difflib, os
PDF='app/src/main/assets/book.pdf'
OUT='app/src/main/assets/content.json'
MARKS=re.compile(r'[\u064b-\u065f\u0670\u06d6-\u06ed]')
def clean(s):
    s=unicodedata.normalize('NFKC',s)
    for a,b in {'ی':'ي','ھ':'ه','ہ':'ه','ۃ':'ة','ک':'ك','گ':'ك','ے':'ي','\u037f':'لله','\ufdf2':'الله','ـ':''}.items(): s=s.replace(a,b)
    s=s.replace('االله','الله').replace('اللله','الله'); s=MARKS.sub('',s)
    for a,b in {'م ن':'من','أر بعا':'أربعا','الأنصا ري':'الأنصاري','عل يه':'عليه','حت ى':'حتى','بالآي تين':'بالآيتين','إزار ه':'إزاره','كتاب ك':'كتابك','كع شر':'كعشر','ع شر':'عشر','انصر ف':'انصرف','أ يأمرنا':'يأمرنا'}.items(): s=s.replace(a,b)
    s='\n'.join(re.sub(r'[ \t]+',' ',x).strip() for x in s.splitlines()); return re.sub(r'\n{3,}','\n\n',s).strip()
def simple(s):
    s=clean(s).replace('ة','ه').replace('ى','ي').replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ؤ','و').replace('ئ','ي'); return re.sub(r'[^\u0600-\u06ff0-9]+','',s)
def page_lines(pg):
    out=[]
    for b in pg.get_text('dict').get('blocks',[]):
      for ln in b.get('lines',[]):
        spans=[sp for sp in ln.get('spans',[]) if sp.get('text','').strip()]
        if spans: out.append({'y0':ln['bbox'][1],'y1':ln['bbox'][3],'text':clean(' '.join(sp['text'] for sp in spans)),'red':any(sp.get('color')==0xff0000 for sp in spans)})
    return sorted(out,key=lambda x:x['y0'])
def find_heading(lines,topic):
    target=simple(topic); best=None
    for j in range(len(lines)):
      for k in (1,2,3):
        if j+k>len(lines) or (k>1 and lines[j+k-1]['y0']-lines[j]['y0']>55): continue
        cand=' '.join(lines[q]['text'] for q in range(j,j+k)); score=difflib.SequenceMatcher(None,target,simple(cand)).ratio(); bonus=.05 if any(lines[q]['red'] for q in range(j,j+k)) else 0
        if target and (target in simple(cand) or simple(cand) in target): bonus+=.08
        if best is None or score+bonus>best[0]: best=(score+bonus,score,j,k,cand)
    return best
def reflow(s):
    s=clean(s).replace('______________________________','\n────────────\n'); s=re.sub(r'\n+',' ',s); s=re.sub(r'\s+([،؛:,.؟!])',r'\1',s); return re.sub(r'\s+',' ',s).strip()
COUNT_PATTERNS=[(100,re.compile(r'(?:مائة|مئة)\s+مرة')),(34,re.compile(r'أربع(?:ا|اً)?\s+وثلاثين')),(33,re.compile(r'ثلاث(?:ا|اً)?\s+وثلاثين')),(15,re.compile(r'خمس\s+عشرة\s+مرة')),(11,re.compile(r'إحدى\s+عشرة\s+مرة')),(10,re.compile(r'عشر\s+مرات|عشرا(?=\s|،|,|؛|\.|$)')),(9,re.compile(r'تسع\s+مرات')),(8,re.compile(r'ثمان(?:ي|ية)?\s+مرات')),(7,re.compile(r'سبع\s+مرات')),(6,re.compile(r'ست\s+مرات')),(5,re.compile(r'خمس\s+مرات')),(4,re.compile(r'أربع\s+مرات')),(3,re.compile(r'ثلاث\s+مرات|ثلاثا(?=\s|،|,|؛|\.|$)')),(2,re.compile(r'مرتين|مرتان'))]
def counter_label(text,start,end,count):
    right=text[end:min(len(text),end+120)].lstrip()
    if right.startswith(':'):
        frag=re.split(r'[\.؛؟!"«»\n]',right[1:].lstrip())[0].strip(' ،,')
        if len(frag)>=3:return frag[:90]
    left=text[max(0,start-120):start]; frag=re.split(r'[\.؛:؟!"«»\n]',left)[-1].strip(' ،,-'); frag=re.sub(r'^[^\u0600-\u06ff0-9]+','',frag)
    if len(frag)>90:frag=frag[-90:]
    return frag if len(frag)>=3 else f'{count} مرات'
def counters(text):
    hits=[]
    for rank,(count,pat) in enumerate(COUNT_PATTERNS):
      for m in pat.finditer(text): hits.append((m.start(),m.end(),rank,count,counter_label(text,m.start(),m.end(),count)))
    hits.sort(key=lambda x:(x[0],-(x[1]-x[0]),x[2])); out=[]; occ=[]
    for a,b,rank,c,l in hits:
      if any(not(b<=oa or a>=ob) for oa,ob in occ):continue
      out.append({'target':c,'label':l});occ.append((a,b))
    return out
def split_bullets(text):
    parts=[p for p in re.split(r'(?:^|\n)\s*·\s*',text) if p.strip()]; parts=parts or [text]; out=[]
    for p in parts:
      t=reflow(p)
      if t:out.append({'text':t,'counters':counters(t)})
    return out
topics=['المقدمة','الحث على الذكر','فضل الذكر','فضل الذكر خاليًا','فضل مجالس الذكر','ذكر الله في كل حين وعلى كل حال','فضل تلاوة القرآن وتعلمه وحفظه','فضيلة لأهل القرآن','علاج تفلت القرآن','حسن الصوت بالقراءة','أحب الكلام إلى الله عز وجل','فضل الحمد والتسبيح والتهليل والتكبير','كلمتان حبيبتان إلى الرحمن','كنز من كنوز الجنة','ما يقوله من تقلب على فراشه بالليل','ما يقوله من قام ليتهجد','ما يقال عند الاستيقاظ','ما يقال عند سماع صياح الديكة ونهيق الحمار','ما يقال عند سماع نباح الكلاب','ما يقال عند دخول الخلاء','ما يقال عند الخروج من الخلاء','فضل الذكر والصلاة بعد الوضوء','ما يدعى به في صلاة الليل','ما يقرأ به في الوتر وما يقال بعده','ما يدعي به في الوتر','ما يقال عند سماع المؤذن','ما يقال بعد الأذان','كيف تسأل الوسيلة لرسول الله صلى الله عليه وسلم','ومن أذكار الأذان أيضًا','ما يقال عند الخروج للصلاة','ما يقال عند دخول المسجد والخروج منه','أدعية افتتاح الصلاة','ما يقال في الركوع والسجود','الحث على الدعاء في السجود','ما يقال بين السجدتين','صفة من صفات التشهد','ما يقال بعد التشهد','الذكر بعد الصلاة','صلاة التسابيح','أذكار الصباح والمساء','سيد الاستغفار','مزيد من الحروز المضمونة','تعويذ الصبيان','كف الصبيان عند المساء','التعوذ بالله من الشيطان الرجيم عند الغضب','ترك قول لو','ذكر الله عند دخول البيت','ما يقوله من نزل منزلاً','فصل في الصلاة على النبي صلى الله عليه وسلم','باب في السلام','ومن صور إفشاء السلام','ولا يُبدأ اليهود والنصارى بالسلام','أكمل صيغة لإلقاء السلام','أذكار الطعام والشراب والتسمية','ما يقوله من نسي التسمية في أول الطعام','استحلال الشيطان للطعام الذي لم يذكر اسم الله عليه','ما يقال بعد الطعام والشراب','صفة الدعاء لمن قدم طعامًا','دعاء الاستخارة','ما يقال عند الجماع','ما يدعى به للمتزوج','ما يقوله من تزوج','ما يقال لمن لبس ثوبًا جديدًا','ما يقال من استجد ثوبًا','ما يقال للمريض','ما يقال عند رؤية المبتلى','ما يقال من شكا وجعًا في جسده','بعض صور الرقى','طرف من أذكار الجنائز','ما يقال عند المصيبة','الدعاء في الصلاة على الجنازة','ما يقال عند دخول المقابر','ما يقال عند وضع الميت في قبره','الاستغفار للميت بعد الدفن','أذكار النوم قراءة المعوذات والنفث بها','آداب الرؤيا وأقسامها','ما يقال عند لقاء العدو','ما يقوله من خاف قومًا','ما يقال عند التعجب','ما يقال عند الفزع','التكبير عند الأمر السار','أذكار السفر','التكبير عند الصعود والتسبيح عند النزول','ما يقال إذا عثرت الدابة','ما يقوله من رجع من غزو أو حج أو عمرة','مما يوصى به المسافر','ما يقال للمسافر','ما يقال إذا عصفت الريح','ما يقال عند رؤية المطر','دعاء الكرب','ما يقوله الصائم عند فطره','الدعاء بالبركة إذا خيفت العين وقول ما شاء الله لا قوة إلا بالله','لا يقال : ما شاء الله وشاء فلان','كيف يشمت العاطس وبما يجيب','ما يقال لغير المسلم إذا عطس','ما يفعله من تثاءب','أكثر دعاء النبي صلى الله عليه وسلم','الحث على الاستغفار','كفارة المجلس']
src=[8,9,9,10,10,11,11,11,13,13,13,14,15,15,15,16,16,17,17,18,18,18,19,19,19,20,20,20,21,21,22,22,23,25,25,25,25,26,29,30,30,34,35,35,36,36,36,37,37,39,40,41,41,41,42,42,42,43,43,44,44,44,44,45,45,45,46,46,47,47,48,48,49,49,49,51,53,54,54,54,55,55,56,56,56,56,57,57,57,58,58,58,58,59,59,59,59,60,60]
printed=[1,2,2,3,3,4,4,4,6,6,6,7,8,8,8,9,9,10,10,11,11,11,12,12,12,13,13,13,14,14,15,15,16,18,18,18,18,19,22,23,23,27,28,28,29,29,29,30,30,32,33,34,34,34,35,35,35,36,36,37,37,37,37,38,38,38,39,39,40,40,41,41,42,42,42,44,46,47,47,47,48,48,49,49,49,49,50,50,50,51,51,51,51,52,52,52,52,53,53]
D=fitz.open(PDF);pl={p+1:page_lines(D[p]) for p in range(len(D))};heads=[]
for topic,p in zip(topics,src):
  b=find_heading(pl[p],topic)
  if b[1]<.85:raise RuntimeError((topic,p,b))
  heads.append({'y0':pl[p][b[2]]['y0'],'y1':pl[p][b[2]+b[3]-1]['y1']})
sections=[]
for i,topic in enumerate(topics):
  sp=src[i];ep=src[i+1] if i+1<len(topics) else len(D);sy=heads[i]['y1']+1;ey=heads[i+1]['y0']-1 if i+1<len(topics) else 780;parts=[]
  for p in range(sp,ep+1):
    pg=D[p-1];y0=sy if p==sp else 45;y1=ey if p==ep else 780
    if y1<=y0:continue
    t=clean(pg.get_textbox(fitz.Rect(0,y0,pg.rect.width,y1)));t=re.sub(r'\n\s*\d+\s*$','',t)
    if t:parts.append(t)
  sections.append({'title':topic,'page':printed[i],'sourcePage':sp,'entries':split_bullets('\n'.join(parts))})
mi=topics.index('أذكار الصباح والمساء');si=topics.index('سيد الاستغفار');xi=topics.index('مزيد من الحروز المضمونة');visible=[]
for i,sec in enumerate(sections):
  if i in (si,xi):continue
  if i==mi:
    say=sections[si]['entries'];blocks=[{'heading':'','entries':sections[mi]['entries']}]
    if say:
      blocks.append({'heading':'سيد الاستغفار','entries':[say[0]]})
      if len(say)>1:blocks.append({'heading':'','entries':say[1:]})
    blocks.append({'heading':'مزيد من الحروز المضمونة','entries':sections[xi]['entries']});visible.append({'id':f't{i}','title':sec['title'],'page':sec['page'],'sourcePage':sec['sourcePage'],'blocks':blocks})
  else:visible.append({'id':f't{i}','title':sec['title'],'page':sec['page'],'sourcePage':sec['sourcePage'],'blocks':[{'heading':'','entries':sec['entries']}]})
json.dump({'bookTitle':'قبس مختار من صحيح الأذكار','version':3,'topics':visible},open(OUT,'w',encoding='utf-8'),ensure_ascii=False,separators=(',',':'))
print('generated',OUT,os.path.getsize(OUT),'bytes',len(visible),'topics')
