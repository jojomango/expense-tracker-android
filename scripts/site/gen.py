"""Generates docs/index.html (zh) and docs/en/index.html from one template."""
import sys, os
repo = sys.argv[1]
T = {
 'zh': dict(lang='zh-Hant-TW', title='記帳本', name='記帳本', other_href='en/', other='English', base='',
   tagline='打開就知道這週還能花多少。', sub='純本地、免登入、沒有廣告的 Android 記帳 App。',
   soon='即將在 Google Play 推出', privacy_href='privacy-policy.html', privacy='隱私權政策', contact='聯絡：',
   trust_h='你的記帳資料只在你的手機上', trust_p='沒有帳號、不用登入、沒有廣告。每一筆收支都只存在你自己的手機裡，不會上傳到任何地方。設定頁有一個預設關閉的「協助改善 App」，打開後也只會傳送匿名的使用情形與當機資訊，不含任何記帳內容。',
   features=[
    ('home','每週預算，一眼看懂','首頁最上方就是本週還能花多少、還剩幾天、平均每天能花多少。超支時清楚標示，每一週的收支淨額也列在週標題旁。'),
    ('add','幾秒記好一筆','自製數字鍵盤加上分類圖示，常用分類一次就看得到。可以選今天、昨天或任何日期，加上備註。'),
    ('stats','看懂錢花去哪','圓環圖顯示本週或本月各分類的支出比例，再用長條圖看近 8 週的趨勢。'),
    ('wallets','多錢包、多幣別','日常、旅行、家庭分開記。每個錢包有自己的幣別，日圓就是日圓、新台幣就是新台幣，彼此不混在一起。'),
    ('dark','深色模式與中英文','跟著系統切換深色模式；介面支援繁體中文與英文，也支援 TalkBack 螢幕閱讀器。'),
    ('settings','備份由你掌握','一鍵匯出備份檔，存到你想要的地方；換手機時匯入就能完整還原，App 每 7 天提醒你備份一次。'),
   ]),
 'en': dict(lang='en', title='Expense Tracker', name='Expense Tracker', other_href='../', other='繁體中文', base='../',
   tagline='Know what’s left to spend this week, the moment you open it.', sub='A private, offline budget tracker for Android. No sign-up, no ads.',
   soon='Coming soon to Google Play', privacy_href='../privacy-policy-en.html', privacy='Privacy policy', contact='Contact: ',
   trust_h='Your entries stay on your phone', trust_p='No account, no sign-in, no ads. Every entry is stored only on your own phone and never uploaded anywhere. Settings has an optional “Help improve the app” switch, off by default; even when it’s on, only anonymous usage and crash info is sent, never your entries.',
   features=[
    ('home','Your weekly budget at a glance','The top of the home screen shows what’s left this week, how many days remain and how much you can spend per day. Overspending is clearly marked, and each week shows its net total.'),
    ('add','Add an entry in seconds','A custom number pad and category icons, with your common categories all in view. Pick today, yesterday or any date, and add a note.'),
    ('stats','See where your money goes','A donut chart of this week’s or this month’s spending by category, plus an 8-week trend.'),
    ('wallets','Multiple wallets and currencies','Keep everyday spending, trips and household costs apart. Each wallet has its own currency, and they never get mixed up.'),
    ('dark','Dark mode, two languages','Follows your system’s dark mode. Available in English and Traditional Chinese, with TalkBack support.'),
    ('settings','You own your backups','Export a backup file with one tap and keep it wherever you like. Import it on a new phone to restore everything; the app reminds you every 7 days.'),
   ]),
}
CSS = '''
  /* 版面：App 自己的色票與圖示；左文右圖的功能列，手機上改成單欄 */
  :root { --bg:#F2F1EF; --surface:#FFFFFF; --fg:#1C1B1F; --fg2:#5F5C66; --line:#E2DFDA; --accent:#C1502E; --ink:#17171A; --gold:#F6C445; }
  @media (prefers-color-scheme: dark) { :root { --bg:#0F0F12; --surface:#1A1A1F; --fg:#ECEBEF; --fg2:#A7A4AE; --line:#2B2A30; --accent:#E2795A; } }
  * { box-sizing: border-box; }
  body { margin:0; background:var(--bg); color:var(--fg); font:17px/1.75 -apple-system,BlinkMacSystemFont,"PingFang TC","Noto Sans TC","Segoe UI",sans-serif; }
  a { color:var(--accent); }
  .wrap { max-width:1040px; margin:0 auto; padding-left:max(20px,env(safe-area-inset-left)); padding-right:max(20px,env(safe-area-inset-right)); }
  header.top { display:flex; justify-content:flex-end; padding-block:18px; font-size:15px; }
  .hero { display:grid; grid-template-columns:auto 1fr; gap:28px; align-items:center; padding-block:28px 56px; }
  .hero img { width:120px; height:120px; border-radius:28px; box-shadow:0 10px 30px rgba(23,23,26,.25); }
  .hero h1 { margin:0; font-size:clamp(34px,6vw,54px); line-height:1.15; letter-spacing:.01em; text-wrap:balance; }
  .hero .tag { margin:10px 0 4px; font-size:clamp(19px,2.6vw,24px); font-weight:600; text-wrap:balance; }
  .hero .sub { margin:0; color:var(--fg2); }
  .soon { display:inline-block; margin-top:18px; padding:8px 16px; border-radius:999px; background:var(--ink); color:var(--gold); font-size:15px; font-weight:600; }
  .trust { background:var(--ink); color:#F2F1EF; border-radius:24px; padding:32px clamp(22px,4vw,44px); }
  .trust h2 { margin:0 0 8px; font-size:24px; color:var(--gold); }
  .trust p { margin:0; max-width:64ch; color:#D9D6DE; }
  .feature { display:grid; grid-template-columns:1fr 280px; gap:clamp(24px,5vw,64px); align-items:center; padding-block:52px; border-bottom:1px solid var(--line); }
  .feature:nth-of-type(even) { grid-template-columns:280px 1fr; }
  .feature:nth-of-type(even) .text { order:2; }
  .feature h2 { margin:0 0 10px; font-size:clamp(22px,3vw,28px); line-height:1.3; text-wrap:balance; }
  .feature p { margin:0; color:var(--fg2); max-width:46ch; }
  .shot { width:100%; max-width:280px; justify-self:center; border-radius:28px; border:1px solid var(--line); box-shadow:0 12px 30px rgba(23,23,26,.12); background:var(--surface); }
  footer { display:flex; flex-wrap:wrap; gap:8px 24px; padding-block:40px 56px; color:var(--fg2); font-size:15px; }
  @media (max-width:720px) {
    .hero { grid-template-columns:1fr; text-align:left; padding-block:8px 40px; }
    .hero img { width:88px; height:88px; border-radius:22px; }
    .feature, .feature:nth-of-type(even) { grid-template-columns:1fr; padding-block:40px; }
    .feature:nth-of-type(even) .text { order:0; }
    .shot { max-width:240px; }
  }
'''
for code, t in T.items():
    b = t['base']
    feats = '\n'.join(f'''    <section class="feature">
      <div class="text"><h2>{h}</h2><p>{p}</p></div>
      <img class="shot" src="{b}assets/screens/{code}/{k}.webp" alt="{h}" width="540" height="1080" loading="lazy">
    </section>''' for k, h, p in t['features'])
    html = f'''<!doctype html>
<html lang="{t['lang']}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>{t['title']}</title>
<meta name="description" content="{t['sub']}">
<link rel="alternate" hreflang="zh-Hant" href="https://jojomango.github.io/expense-tracker-android/">
<link rel="alternate" hreflang="en" href="https://jojomango.github.io/expense-tracker-android/en/">
<link rel="icon" href="{b}assets/icon-192.png">
<!-- 由 scripts/site/gen.py 產生，請改那支程式，不要直接改這個檔案。沒有任何追蹤或分析程式碼。 -->
<style>{CSS}</style>
</head>
<body>
<div class="wrap">
  <header class="top"><a href="{t['other_href']}" hreflang="{'en' if code=='zh' else 'zh-Hant'}">{t['other']}</a></header>
  <section class="hero">
    <img src="{b}assets/icon-512.png" alt="" width="120" height="120">
    <div>
      <h1>{t['name']}</h1>
      <p class="tag">{t['tagline']}</p>
      <p class="sub">{t['sub']}</p>
      <span class="soon">{t['soon']}</span>
    </div>
  </section>
  <section class="trust"><h2>{t['trust_h']}</h2><p>{t['trust_p']}</p></section>
{feats}
  <footer><a href="{t['privacy_href']}">{t['privacy']}</a><span>{t['contact']}jojomango99@gmail.com</span><span>© 2026 jojomango</span></footer>
</div>
</body>
</html>
'''
    out = os.path.join(repo, 'docs', 'index.html' if code == 'zh' else 'en/index.html')
    os.makedirs(os.path.dirname(out), exist_ok=True)
    open(out, 'w').write(html)
print('ok')
