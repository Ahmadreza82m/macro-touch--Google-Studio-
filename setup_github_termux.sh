#!/data/data/com.termux/files/usr/bin/bash
# اسکریپت خودکار تنظیم و ارسال پروژه Macro Touch Pro از ترموکس به گیت‌هاب

echo "================================================="
echo "   Macro Touch Pro - راهنمای خودکار ترموکس به گیت‌هاب"
echo "================================================="

# ۱. نصب پکیج‌های لازم
echo "[۱/۵] بررسی و نصب ابزارها (git, unzip)..."
pkg update -y && pkg install git unzip -y

# ۲. دسترسی به حافظه گوشی
echo "[۲/۵] درخواست مجوز دسترسی به حافظه داخلی گوشی..."
termux-setup-storage
sleep 2

# ۳. تنظیم مشخصات کاربری گیت
read -p "نام کاربری گیت‌هاب خود را وارد کنید (مثال: AliReza): " GITHUB_USER
read -p "ایمیل گیت‌هاب خود را وارد کنید: " GITHUB_EMAIL
read -p "نام مخزنی که در سایت گیت‌هاب ساختید (مثال: MacroTouchPro): " REPO_NAME

git config --global user.name "$GITHUB_USER"
git config --global user.email "$GITHUB_EMAIL"

# ۴. اینیشیالایز و آماده‌سازی کامیت
echo "[۳/۵] آماده‌سازی کدهای پروژه..."
git init
git add .
git commit -m "Macro Touch Pro v2.0 - Complete Architecture & GitHub Actions"
git branch -M main

# ۵. پوش به گیت‌هاب
echo "[۴/۵] اتصال به گیت‌هاب..."
git remote remove origin 2>/dev/null
git remote add origin "https://github.com/$GITHUB_USER/$REPO_NAME.git"

echo "================================================="
echo "توجه مهم درباره رمز عبور:"
echo "هنگام پرسیدن پسورد، گیت‌هاب رمز عادی را قبول نمی‌کند."
echo "باید از Personal Access Token (کد دسترسی شخصی) استفاده کنید."
echo "ساخت توکن در: github.com > Settings > Developer Settings > Personal access tokens"
echo "================================================="

git push -u origin main

echo "[۵/۵] ارسال به پایان رسید!"
echo "اکنون در صفحه مخزن خود در گیت‌هاب به تب Actions بروید تا ساخت خودکار APK را مشاهده کنید."
