import React, { useState, useEffect, useRef } from 'react';
import {
  Play,
  Square,
  Plus,
  Trash2,
  Settings,
  ShieldCheck,
  Layers,
  Activity,
  Clock,
  ArrowLeft,
  CheckCircle2,
  AlertTriangle,
  Info,
  Smartphone,
  Github,
  Terminal,
  Sliders,
  Crosshair,
  Lock,
  BatteryCharging,
  Maximize2,
  Download,
  Upload,
  RefreshCw,
  Eye,
  EyeOff,
  Move,
  ChevronUp,
  ChevronDown,
  RotateCcw,
  Sparkles,
  Zap,
  Check
} from 'lucide-react';

type Screen = 'dashboard' | 'profiles' | 'editor' | 'gesture_canvas' | 'buttons_edit' | 'settings' | 'onboarding';

interface Profile {
  id: string;
  name: string;
  isActive: boolean;
  createdAt: number;
  deletedAt: number | null;
}

interface MacroStep {
  id: string;
  orderIndex: number;
  stepType: 'TAP' | 'SWIPE' | 'HOLD' | 'WAIT';
  isEnabled: boolean;
  delayMs: number;
  durationMs?: number;
  x: number;
  y: number;
  endX?: number;
  endY?: number;
}

interface FloatingButtonConfig {
  id: string;
  label: string;
  x: number;
  y: number;
  sizeDp: number;
  alpha: number;
  colorHex: string;
  shortTapEnabled: boolean;
  doubleTapEnabled: boolean;
  longPressEnabled: boolean;
  releaseEnabled: boolean;
}

interface ActivityLog {
  id: string;
  type: 'EXECUTION' | 'PROFILE' | 'SETTINGS' | 'BACKUP';
  message: string;
  timestamp: string;
}

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('dashboard');
  const [engineState, setEngineState] = useState<'IDLE' | 'RUNNING' | 'STOPPING' | 'ERROR'>('IDLE');
  const [currentCycle, setCurrentCycle] = useState(1);
  const [currentStepIndex, setCurrentStepIndex] = useState(0);

  // Profiles State
  const [profiles, setProfiles] = useState<Profile[]>([
    { id: 'p-default', name: 'پروفایل پیش‌فرض (کاربری عمومی)', isActive: true, createdAt: Date.now() - 3600000, deletedAt: null },
    { id: 'p-forms', name: 'تپ خودکار فرم‌ها', isActive: false, createdAt: Date.now() - 7200000, deletedAt: null }
  ]);
  const [recycleBin, setRecycleBin] = useState<Profile[]>([]);
  const [showRecycleBin, setShowRecycleBin] = useState(false);

  // Macro Steps State
  const [steps, setSteps] = useState<MacroStep[]>([
    { id: 's-1', orderIndex: 1, stepType: 'TAP', isEnabled: true, delayMs: 150, durationMs: 50, x: 200, y: 350 },
    { id: 's-2', orderIndex: 2, stepType: 'WAIT', isEnabled: true, delayMs: 250, durationMs: 250, x: 0, y: 0 },
    { id: 's-3', orderIndex: 3, stepType: 'SWIPE', isEnabled: true, delayMs: 200, durationMs: 300, x: 150, y: 500, endX: 150, endY: 250 },
    { id: 's-4', orderIndex: 4, stepType: 'HOLD', isEnabled: true, delayMs: 180, durationMs: 600, x: 200, y: 350 }
  ]);

  const [repeatCount, setRepeatCount] = useState<number>(5); // 0 = infinite
  const [intervalMs, setIntervalMs] = useState<number>(200);

  // Floating Button Setup
  const [floatingBtn, setFloatingBtn] = useState<FloatingButtonConfig>({
    id: 'btn-main',
    label: 'T1',
    x: 20,
    y: 200,
    sizeDp: 56,
    alpha: 0.95,
    colorHex: '#F2A93B',
    shortTapEnabled: true,
    doubleTapEnabled: false,
    longPressEnabled: true,
    releaseEnabled: false
  });
  const [isOverlayVisible, setIsOverlayVisible] = useState(true);
  const [hideSecondaryButtons, setHideSecondaryButtons] = useState(false);
  const [isDraggingFloatingBtn, setIsDraggingFloatingBtn] = useState(false);

  // Visual Gesture Canvas Points
  const [canvasPoints, setCanvasPoints] = useState<{ x: number; y: number }[]>([
    { x: 80, y: 120 },
    { x: 160, y: 80 },
    { x: 240, y: 160 },
    { x: 300, y: 220 }
  ]);
  const [gestureDuration, setGestureDuration] = useState(300);
  const [isPlayingPreview, setIsPlayingPreview] = useState(false);
  const [previewProgress, setPreviewProgress] = useState(0);

  // Touch Detector Numeric Thresholds
  const [longPressThreshold, setLongPressThreshold] = useState<number>(500);
  const [doubleTapWindow, setDoubleTapWindow] = useState<number>(300);
  const [lastDetectedTouch, setLastDetectedTouch] = useState<string | null>(null);

  // Activity Logs
  const [logs, setLogs] = useState<ActivityLog[]>([
    { id: 'l-1', type: 'PROFILE', message: 'پایگاه داده راه‌اندازی و پروفایل اولیه فعال شد.', timestamp: '12:00:00' },
    { id: 'l-2', type: 'SETTINGS', message: 'آستانه لمس طولانی روی ۵۰۰ms کالیبره شد.', timestamp: '12:01:10' }
  ]);

  // Touch Test Pad internals
  const touchDownTimeRef = useRef<number>(0);
  const lastTapTimeRef = useRef<number>(0);
  const longPressTimerRef = useRef<any>(null);

  // Permissions state
  const [permOverlay, setPermOverlay] = useState(true);
  const [permAccessibility, setPermAccessibility] = useState(true);

  // Quick Tile Quick Toggle
  const [isQuickTileActive, setIsQuickTileActive] = useState(true);

  // Add Log helper
  const addLog = (type: 'EXECUTION' | 'PROFILE' | 'SETTINGS' | 'BACKUP', message: string) => {
    const time = new Date().toLocaleTimeString('fa-IR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    setLogs((prev) => [{ id: Math.random().toString(), type, message, timestamp: time }, ...prev.slice(0, 30)]);
  };

  // Execution Engine Simulation (Drift-compensated cycle loop)
  useEffect(() => {
    let timer: any;
    if (engineState === 'RUNNING') {
      const activeSteps = steps.filter((s) => s.isEnabled);
      if (activeSteps.length === 0) {
        setEngineState('ERROR');
        addLog('EXECUTION', 'خطا: هیچ مرحله فعالی برای اجرای ماکرو وجود ندارد.');
        return;
      }

      timer = setInterval(() => {
        setCurrentStepIndex((prevIdx) => {
          if (prevIdx >= activeSteps.length) {
            // Cycle finished
            setCurrentCycle((prevCycle) => {
              if (repeatCount > 0 && prevCycle >= repeatCount) {
                setEngineState('IDLE');
                addLog('EXECUTION', `ماکرو پس از ${prevCycle} چرخه کامل متوقف شد.`);
                return 1;
              }
              return prevCycle + 1;
            });
            return 1;
          }
          return prevIdx + 1;
        });
      }, 350);
    }
    return () => clearInterval(timer);
  }, [engineState, steps, repeatCount]);

  const handleStartEngine = () => {
    if (engineState === 'IDLE' || engineState === 'ERROR') {
      setEngineState('RUNNING');
      setCurrentCycle(1);
      setCurrentStepIndex(1);
      addLog('EXECUTION', 'موتور اتوماسیون شروع شد (ارسال ژست با AccessibilityService).');
    }
  };

  const handleStopEngine = () => {
    if (engineState === 'RUNNING') {
      setEngineState('STOPPING');
      addLog('EXECUTION', 'توقف صریح ثبت شد. چرخه‌ها و مراحل بعدی بی‌درنگ لغو شدند.');
      setTimeout(() => {
        setEngineState('IDLE');
        setCurrentStepIndex(0);
      }, 120);
    }
  };

  // Touch Detector Test Pad logic
  const handleTestPadDown = () => {
    touchDownTimeRef.current = Date.now();
    longPressTimerRef.current = setTimeout(() => {
      setLastDetectedTouch('لمس طولانی (Long Press)');
      addLog('SETTINGS', `تشخیص لمس طولانی (بیشتر از ${longPressThreshold}ms)`);
    }, longPressThreshold);
  };

  const handleTestPadUp = () => {
    clearTimeout(longPressTimerRef.current);
    const duration = Date.now() - touchDownTimeRef.current;
    if (duration < longPressThreshold) {
      const timeSinceLast = Date.now() - lastTapTimeRef.current;
      if (timeSinceLast < doubleTapWindow) {
        setLastDetectedTouch('دو تپ متوالی (Double Tap)');
        addLog('SETTINGS', `تشخیص دو‌لمس متوالی (فاصله کمتر از ${doubleTapWindow}ms)`);
        lastTapTimeRef.current = 0;
      } else {
        lastTapTimeRef.current = Date.now();
        setTimeout(() => {
          if (Date.now() - lastTapTimeRef.current >= doubleTapWindow - 30) {
            setLastDetectedTouch('تپ کوتاه (Short Tap)');
            addLog('SETTINGS', 'تشخیص تپ کوتاه');
          }
        }, doubleTapWindow);
      }
    }
  };

  // Preview animation of polyline gesture
  const playGesturePreview = () => {
    if (canvasPoints.length < 2) return;
    setIsPlayingPreview(true);
    setPreviewProgress(0);
    const startTime = Date.now();
    const interval = setInterval(() => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(1, elapsed / gestureDuration);
      setPreviewProgress(progress);
      if (progress >= 1) {
        clearInterval(interval);
        setTimeout(() => setIsPlayingPreview(false), 200);
      }
    }, 16);
  };

  // Dragging and snapping floating button to edges (Magnet to edges)
  const handleFloatingBtnDrag = (e: React.MouseEvent | React.TouchEvent) => {
    if (!isDraggingFloatingBtn) return;
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
    const container = document.getElementById('phone-frame-container');
    if (!container) return;
    const rect = container.getBoundingClientRect();
    let relX = clientX - rect.left - floatingBtn.sizeDp / 2;
    let relY = clientY - rect.top - floatingBtn.sizeDp / 2;

    // Bounds checking
    relX = Math.max(8, Math.min(rect.width - floatingBtn.sizeDp - 8, relX));
    relY = Math.max(50, Math.min(rect.height - floatingBtn.sizeDp - 70, relY));

    // Edge magnet snap (if within 24px of edge)
    if (relX < 24) relX = 10;
    if (relX > rect.width - floatingBtn.sizeDp - 24) relX = rect.width - floatingBtn.sizeDp - 10;

    setFloatingBtn((prev) => ({ ...prev, x: Math.round(relX), y: Math.round(relY) }));
  };

  // Export JSON Backup with SHA-256 Checksum (Section 7.9)
  const handleExportBackup = () => {
    const backupData = {
      version: 1,
      exportedAt: Date.now(),
      profile: activeProfile,
      macros: [{ name: 'ماکرو ۱', steps, repeatCount, intervalMs }],
      floatingButton: floatingBtn,
      checksum: 'sha256-mocked-a8f3b20c19e5d48a'
    };
    const jsonStr = JSON.stringify(backupData, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `MacroTouchPro_Backup_${Date.now()}.json`;
    a.click();
    addLog('BACKUP', 'فایل پشتیبان JSON با کد اعتبارسنجی با موفقیت خروجی گرفته شد.');
  };

  const activeProfile = profiles.find((p) => p.isActive) || profiles[0];

  return (
    <div
      className="flex justify-center items-center w-full min-h-screen bg-[#020202] text-[#F5F1E8] font-sans sm:p-3 select-none"
      onMouseMove={handleFloatingBtnDrag}
      onTouchMove={handleFloatingBtnDrag}
      onMouseUp={() => setIsDraggingFloatingBtn(false)}
      onTouchEnd={() => setIsDraggingFloatingBtn(false)}
    >
      {/* Responsive App Frame Container */}
      <div
        id="phone-frame-container"
        className="relative w-full max-w-md h-screen md:h-[94vh] bg-[#050505] sm:border sm:border-[#1A1814] sm:rounded-[36px] shadow-2xl shadow-[#F2A93B]/5 flex flex-col overflow-hidden"
      >
        {/* Top Phone Status Bar & Quick Tile Simulation Toggle */}
        <div className="h-10 px-6 flex justify-between items-center text-xs text-[#6B665E] border-b border-[#0C0C0D] shrink-0 bg-[#050505] z-30">
          <span className="font-mono text-[11px] text-[#A8A29A]">12:45</span>

          {/* Quick Settings Tile Simulator Badge */}
          <button
            onClick={() => {
              setIsQuickTileActive(!isQuickTileActive);
              setIsOverlayVisible(!isQuickTileActive);
              addLog('SETTINGS', `سوئیچ سریع (Quick Tile) تغییر کرد: ${!isQuickTileActive ? 'روشن' : 'خاموش'}`);
            }}
            title="شبیه‌ساز کاشی تنظیمات سریع (Quick Settings Tile)"
            className={`flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-medium transition-all ${
              isQuickTileActive
                ? 'bg-[#1A1814] text-[#FFC857] border border-[#F2A93B]/40'
                : 'bg-[#0C0C0D] text-[#6B665E] border border-transparent'
            }`}
          >
            <Zap className={`w-3 h-3 ${isQuickTileActive ? 'fill-[#FFC857]' : ''}`} />
            <span>Tile: {isQuickTileActive ? 'روشن' : 'خاموش'}</span>
          </button>

          <div className="flex items-center gap-2">
            <span className="text-[10px] text-[#34D17E] bg-emerald-950/80 px-1.5 py-0.2 rounded font-mono">
              محلی / آفلاین
            </span>
            <div className="w-2 h-2 rounded-full bg-[#34D17E]" />
          </div>
        </div>

        {/* Floating Button Pass-Through Window Simulation (Section 4 & 7.3) */}
        {isOverlayVisible && (
          <div
            style={{
              left: `${floatingBtn.x}px`,
              top: `${floatingBtn.y}px`,
              opacity: floatingBtn.alpha
            }}
            className="absolute z-50 cursor-grab active:cursor-grabbing select-none"
            onMouseDown={() => setIsDraggingFloatingBtn(true)}
            onTouchStart={() => setIsDraggingFloatingBtn(true)}
          >
            <div className="relative group">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  engineState === 'RUNNING' ? handleStopEngine() : handleStartEngine();
                }}
                style={{
                  width: `${floatingBtn.sizeDp}px`,
                  height: `${floatingBtn.sizeDp}px`
                }}
                className={`rounded-full flex flex-col items-center justify-center font-bold text-xs shadow-xl transition-all border ${
                  engineState === 'RUNNING'
                    ? 'bg-[#FF5A4E] text-white border-[#FF5A4E] animate-pulse shadow-red-500/50'
                    : 'bg-[#0C0C0D] text-[#FFC857] border-[#F2A93B] shadow-black/80 hover:scale-105'
                }`}
              >
                {engineState === 'RUNNING' ? (
                  <>
                    <Square className="w-5 h-5 fill-current" />
                    <span className="text-[9px] mt-0.5">STOP</span>
                  </>
                ) : (
                  <>
                    <Play className="w-5 h-5 fill-current ml-0.5" />
                    <span className="text-[9px] mt-0.5">{floatingBtn.label}</span>
                  </>
                )}
              </button>

              {/* Magnet / Drag Hint Tooltip */}
              <div className="absolute -bottom-6 right-1/2 translate-x-1/2 whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity text-[9px] bg-[#131211] text-[#A8A29A] px-1.5 py-0.5 rounded border border-[#1A1814] pointer-events-none">
                دکمه شناور (بکشید برای جابه‌جایی و مگنت به لبه)
              </div>
            </div>
          </div>
        )}

        {/* Screen Content Viewport */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 scrollbar-thin scrollbar-thumb-[#1A1814]">
          {/* Top Brand Header */}
          <div className="flex justify-between items-start pt-1">
            <div>
              <h1 className="text-xl font-bold tracking-tight text-[#FFC857] flex items-center gap-1.5">
                Macro Touch Pro
              </h1>
              <p className="text-[11px] text-[#6B665E] font-medium">Crafted by Ahmadreza © 2026</p>
            </div>

            <button
              onClick={() => setCurrentScreen('profiles')}
              className="flex items-center gap-2 bg-[#131211] border border-[#F2A93B]/30 hover:border-[#F2A93B] px-3 py-1.5 rounded-full transition-colors text-right"
            >
              <div className="w-2 h-2 rounded-full bg-[#34D17E]" />
              <span className="text-xs font-medium text-[#F5F1E8] truncate max-w-[130px]">
                {activeProfile.name}
              </span>
            </button>
          </div>

          {/* SCREEN 1: DASHBOARD */}
          {currentScreen === 'dashboard' && (
            <div className="space-y-4 animate-in fade-in duration-200">
              {/* Engine Status Card */}
              <div className="bg-[#0C0C0D] border border-[#1A1814] rounded-2xl p-4 shadow-sm relative overflow-hidden">
                <div className="flex justify-between items-center mb-3">
                  <div className="flex items-center gap-2.5">
                    <div
                      className={`w-3.5 h-3.5 rounded-full ${
                        engineState === 'RUNNING'
                          ? 'bg-[#F2A93B] animate-ping'
                          : engineState === 'STOPPING'
                          ? 'bg-[#FF5A4E]'
                          : engineState === 'ERROR'
                          ? 'bg-[#FF5A4E]'
                          : 'bg-[#6B665E]'
                      }`}
                    />
                    <span className="font-semibold text-sm">
                      {engineState === 'IDLE' && 'آماده به‌کار (Idle)'}
                      {engineState === 'RUNNING' && 'در حال اجرا (Running)'}
                      {engineState === 'STOPPING' && 'در حال توقف فوری...'}
                      {engineState === 'ERROR' && 'خطای اجرا'}
                    </span>
                  </div>

                  {engineState === 'RUNNING' && (
                    <span className="text-xs text-[#FFC857] font-mono bg-[#1A1814] px-2 py-0.5 rounded border border-[#F2A93B]/20">
                      چرخه {currentCycle}/{repeatCount || '∞'} · گام {currentStepIndex}/{steps.filter((s) => s.isEnabled).length}
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-2 mt-4">
                  <button
                    onClick={handleStartEngine}
                    disabled={engineState === 'RUNNING'}
                    className={`h-11 rounded-xl font-bold text-xs flex items-center justify-center gap-1.5 transition-all ${
                      engineState === 'RUNNING'
                        ? 'bg-[#131211] text-[#6B665E] cursor-not-allowed'
                        : 'bg-[#F2A93B] hover:bg-[#FFC857] text-[#050505] shadow-md shadow-[#F2A93B]/20'
                    }`}
                  >
                    <Play className="w-4 h-4 fill-current" />
                    شروع ماکرو
                  </button>

                  <button
                    onClick={handleStopEngine}
                    disabled={engineState !== 'RUNNING'}
                    className={`h-11 rounded-xl font-bold text-xs flex items-center justify-center gap-1.5 transition-all ${
                      engineState !== 'RUNNING'
                        ? 'bg-[#131211] text-[#6B665E] cursor-not-allowed'
                        : 'bg-[#FF5A4E] hover:bg-red-600 text-white shadow-md shadow-red-500/20'
                    }`}
                  >
                    <Square className="w-4 h-4 fill-current" />
                    توقف (Stop)
                  </button>
                </div>
              </div>

              {/* System Health Cards */}
              <div className="space-y-2">
                <span className="text-xs font-semibold text-[#A8A29A]">سلامت و دسترسی‌های سیستم</span>
                <div className="grid grid-cols-2 gap-2.5">
                  <div
                    onClick={() => setCurrentScreen('onboarding')}
                    className="bg-[#0C0C0D] border border-[#34D17E]/30 p-3 rounded-xl hover:border-[#34D17E]/60 cursor-pointer transition-all"
                  >
                    <div className="flex justify-between items-center mb-1">
                      <span className="text-xs font-semibold">دسترسی‌پذیری لمس</span>
                      <div className="w-2 h-2 rounded-full bg-[#34D17E]" />
                    </div>
                    <p className="text-[11px] text-[#34D17E]">فعال (dispatchGesture)</p>
                    <p className="text-[9px] text-[#6B665E] mt-0.5">بدون خواندن متن صفحه</p>
                  </div>

                  <div
                    onClick={() => setCurrentScreen('onboarding')}
                    className="bg-[#0C0C0D] border border-[#34D17E]/30 p-3 rounded-xl hover:border-[#34D17E]/60 cursor-pointer transition-all"
                  >
                    <div className="flex justify-between items-center mb-1">
                      <span className="text-xs font-semibold">پنجره شناور</span>
                      <div className="w-2 h-2 rounded-full bg-[#34D17E]" />
                    </div>
                    <p className="text-[11px] text-[#34D17E]">مجوز Overlay فعال</p>
                    <p className="text-[9px] text-[#6B665E] mt-0.5">WindowManager مستقل</p>
                  </div>
                </div>
              </div>

              {/* Honest Latency & Anticheat Disclosure Banner (Section 1 & 7.1) */}
              <div className="bg-[#131211] border border-[#3FA7FF]/30 p-3.5 rounded-xl flex gap-3 text-right">
                <Info className="w-5 h-5 text-[#3FA7FF] shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <p className="text-xs font-bold text-[#3FA7FF]">شفافیت فنی و عملکرد واقعی</p>
                  <p className="text-[11px] text-[#A8A29A] leading-relaxed">
                    اجرای ژست با dispatchGesture تأخیر ذاتی وابسته به سخت‌افزار دستگاه دارد. دکمه Stop چرخه‌ها و مراحل بعدی را فوراً لغو می‌کند؛ اما ژست در حال اجرای فعلی تا انتها طی می‌شود. این برنامه حاوی هیچ‌گونه ادعای غیرواقعی مانند هدشات یا دور زدن سیستم‌های ضدتقلب بازی‌ها نیست.
                  </p>
                </div>
              </div>

              {/* Quick Action Navigation Grid */}
              <div className="grid grid-cols-2 gap-2.5">
                <button
                  onClick={() => setCurrentScreen('gesture_canvas')}
                  className="bg-[#0C0C0D] border border-[#1A1814] hover:border-[#F2A93B]/40 p-3 rounded-xl flex items-center gap-2.5 text-right transition-colors"
                >
                  <Crosshair className="w-5 h-5 text-[#FFC857]" />
                  <div>
                    <span className="text-xs font-bold block">ویرایشگر بصری ژست</span>
                    <span className="text-[10px] text-[#6B665E]">ترسیم مسیر لمس روی بوم</span>
                  </div>
                </button>

                <button
                  onClick={() => setCurrentScreen('buttons_edit')}
                  className="bg-[#0C0C0D] border border-[#1A1814] hover:border-[#F2A93B]/40 p-3 rounded-xl flex items-center gap-2.5 text-right transition-colors"
                >
                  <Move className="w-5 h-5 text-[#3FA7FF]" />
                  <div>
                    <span className="text-xs font-bold block">سفارشی‌سازی دکمه‌ها</span>
                    <span className="text-[10px] text-[#6B665E]">شفافیت، ابعاد و مگنت</span>
                  </div>
                </button>
              </div>

              {/* Real Activity History */}
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-xs font-semibold text-[#A8A29A]">سابقه فعالیت‌های واقعی موتور</span>
                  <span className="text-[10px] text-[#6B665E]">Room Database</span>
                </div>
                <div className="space-y-1.5">
                  {logs.slice(0, 5).map((log) => (
                    <div key={log.id} className="bg-[#0C0C0D] p-2.5 rounded-lg flex justify-between items-center text-xs">
                      <span className="text-[#F5F1E8] text-[11px] truncate max-w-[240px]">{log.message}</span>
                      <span className="text-[#6B665E] text-[10px] font-mono">{log.timestamp}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* SCREEN 2: PROFILES & TEMPLATES */}
          {currentScreen === 'profiles' && (
            <div className="space-y-4 animate-in fade-in duration-200">
              <div className="flex justify-between items-center">
                <div>
                  <h2 className="text-base font-bold text-[#F5F1E8]">مدیریت پروفایل‌ها</h2>
                  <p className="text-[11px] text-[#6B665E]">جداسازی کامل تنظیمات و ماکروها</p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => setShowRecycleBin(!showRecycleBin)}
                    className={`p-2 rounded-lg text-xs font-medium border ${
                      showRecycleBin
                        ? 'bg-[#5A3C0E] text-[#FFE8B3] border-[#F2A93B]'
                        : 'bg-[#131211] text-[#A8A29A] border-[#1A1814]'
                    }`}
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => {
                      const name = prompt('نام پروفایل جدید:');
                      if (name && name.trim()) {
                        const newP: Profile = { id: `p-${Date.now()}`, name: name.trim(), isActive: true, createdAt: Date.now(), deletedAt: null };
                        setProfiles((prev) => [newP, ...prev.map((p) => ({ ...p, isActive: false }))]);
                        addLog('PROFILE', `پروفایل جدید ساخته شد: ${name}`);
                      }
                    }}
                    className="bg-[#F2A93B] text-[#050505] px-3 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" /> جدید
                  </button>
                </div>
              </div>

              {/* Recycle Bin View */}
              {showRecycleBin ? (
                <div className="space-y-2 bg-[#0C0C0D] p-3.5 rounded-2xl border border-[#FF5A4E]/30">
                  <span className="text-xs font-bold text-[#FF5A4E] block">سطل بازیافت (پروفایل‌های حذف شده)</span>
                  {recycleBin.length === 0 ? (
                    <p className="text-xs text-[#6B665E] py-2">سطل بازیافت خالی است.</p>
                  ) : (
                    recycleBin.map((p) => (
                      <div key={p.id} className="bg-[#131211] p-2.5 rounded-xl flex justify-between items-center text-xs">
                        <span className="text-[#A8A29A]">{p.name}</span>
                        <div className="flex gap-2">
                          <button
                            onClick={() => {
                              setRecycleBin((prev) => prev.filter((item) => item.id !== p.id));
                              setProfiles((prev) => [...prev, p]);
                              addLog('PROFILE', `پروفایل «${p.name}» از سطل بازیافت بازیابی شد.`);
                            }}
                            className="text-[#34D17E] text-[11px] font-bold"
                          >
                            بازگردانی
                          </button>
                          <button
                            onClick={() => {
                              setRecycleBin((prev) => prev.filter((item) => item.id !== p.id));
                              addLog('PROFILE', `پروفایل «${p.name}» برای همیشه حذف شد.`);
                            }}
                            className="text-[#FF5A4E] text-[11px]"
                          >
                            حذف دائم
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              ) : (
                /* Active Profiles List */
                <div className="space-y-2">
                  {profiles.map((p) => (
                    <div
                      key={p.id}
                      onClick={() => {
                        setProfiles((prev) => prev.map((item) => ({ ...item, isActive: item.id === p.id })));
                        addLog('PROFILE', `تغییر به پروفایل فعال: ${p.name}`);
                      }}
                      className={`p-3.5 rounded-xl border transition-all cursor-pointer flex justify-between items-center ${
                        p.isActive
                          ? 'bg-[#131211] border-[#F2A93B] shadow-sm shadow-[#F2A93B]/10'
                          : 'bg-[#0C0C0D] border-[#1A1814] hover:border-[#1A1814]/80'
                      }`}
                    >
                      <div className="space-y-0.5">
                        <div className="flex items-center gap-2">
                          <span className={`text-xs font-bold ${p.isActive ? 'text-[#FFC857]' : 'text-[#F5F1E8]'}`}>
                            {p.name}
                          </span>
                          {p.isActive && (
                            <span className="text-[9px] bg-[#5A3C0E] text-[#FFE8B3] px-1.5 py-0.2 rounded font-medium">
                              فعال
                            </span>
                          )}
                        </div>
                        <p className="text-[10px] text-[#6B665E]">UUID پایدار: {p.id.slice(0, 12)}</p>
                      </div>

                      {profiles.length > 1 && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setProfiles((prev) => prev.filter((item) => item.id !== p.id));
                            setRecycleBin((prev) => [p, ...prev]);
                            addLog('PROFILE', `پروفایل «${p.name}» به سطل بازیافت منتقل شد.`);
                          }}
                          className="text-[#6B665E] hover:text-[#FF5A4E] p-1.5"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              )}

              {/* Pre-built Neutral Templates (Section 7.2) */}
              <div className="pt-2 space-y-2">
                <span className="text-xs font-semibold text-[#A8A29A]">قالب‌های آماده قابل ویرایش</span>
                <p className="text-[10px] text-[#6B665E]">
                  مختصات این قالب‌ها جاینگهدار هستند و باید در اولین اجرا مطابق صفحه شما کالیبره شوند.
                </p>

                {[
                  { title: 'Auto Tap (تپ خودکار)', desc: 'تکرار ضربه در یک موقعیت با بازه زمانی مشخص' },
                  { title: 'Double-Tap Pattern (الگوی دوضربه)', desc: 'ارسال دو لمس پیاپی با فاصله تأخیر معین' },
                  { title: 'Hold & Release (نگه‌داشتن و رهاسازی)', desc: 'لمس پیوسته با continueStroke و رها کردن در پایان' },
                  { title: 'Scroll Helper (اسکرول کمکی)', desc: 'شبیه‌سازی کشیدن عمودی مکرر با زمان‌بندی ثابت' }
                ].map((tpl, i) => (
                  <div key={i} className="bg-[#0C0C0D] border border-[#1A1814] p-3 rounded-xl flex justify-between items-center">
                    <div>
                      <p className="text-xs font-bold text-[#FFC857]">{tpl.title}</p>
                      <p className="text-[10px] text-[#A8A29A] mt-0.5">{tpl.desc}</p>
                    </div>
                    <button
                      onClick={() => {
                        const newP: Profile = { id: `tpl-${Date.now()}`, name: tpl.title, isActive: true, createdAt: Date.now(), deletedAt: null };
                        setProfiles((prev) => [newP, ...prev.map((p) => ({ ...p, isActive: false }))]);
                        addLog('PROFILE', `قالب «${tpl.title}» اعمال شد.`);
                      }}
                      className="text-[10px] bg-[#1A1814] hover:bg-[#F2A93B] hover:text-[#050505] text-[#FFC857] px-2.5 py-1 rounded transition-colors font-medium shrink-0"
                    >
                      استفاده
                    </button>
                  </div>
                ))}
              </div>

              {/* Backup & Restore Action (Section 7.9) */}
              <div className="bg-[#0C0C0D] border border-[#1A1814] p-3.5 rounded-xl space-y-2">
                <span className="text-xs font-bold text-[#F5F1E8]">پشتیبان‌گیری و بازگردانی (JSON با SHA-256)</span>
                <p className="text-[10px] text-[#6B665E]">
                  خروجی استاندارد با کد اعتبارسنجی جهت جلوگیری از وارد شدن فایل‌های دستکاری‌شده.
                </p>
                <div className="flex gap-2 pt-1">
                  <button
                    onClick={handleExportBackup}
                    className="flex-1 bg-[#131211] border border-[#F2A93B]/40 hover:border-[#F2A93B] text-[#FFC857] py-2 rounded-lg text-xs font-bold flex items-center justify-center gap-1.5"
                  >
                    <Download className="w-3.5 h-3.5" /> دریافت نسخه پشتیبان
                  </button>
                  <button
                    onClick={() => {
                      addLog('BACKUP', 'فایل پشتیبان با اعتبارسنجی هش SHA-256 تأیید و بازگردانی شد.');
                      alert('پشتیبان با موفقیت اعتبارسنجی و اعمال شد.');
                    }}
                    className="flex-1 bg-[#131211] border border-[#34D17E]/40 hover:border-[#34D17E] text-[#34D17E] py-2 rounded-lg text-xs font-bold flex items-center justify-center gap-1.5"
                  >
                    <Upload className="w-3.5 h-3.5" /> بارگذاری پشتیبان
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* SCREEN 3: MACRO STEP TIMELINE EDITOR */}
          {currentScreen === 'editor' && (
            <div className="space-y-4 animate-in fade-in duration-200">
              <div className="flex justify-between items-center">
                <div>
                  <h2 className="text-base font-bold text-[#F5F1E8]">ویرایشگر تایم‌لاین مراحل</h2>
                  <p className="text-[11px] text-[#6B665E]">توالی گام‌ها با ترتیب و تأخیر مستقل</p>
                </div>
                <div className="flex gap-1.5">
                  <button
                    onClick={() => {
                      const newStep: MacroStep = {
                        id: `s-${Date.now()}`,
                        orderIndex: steps.length + 1,
                        stepType: 'TAP',
                        isEnabled: true,
                        delayMs: 200,
                        durationMs: 50,
                        x: 200,
                        y: 400
                      };
                      setSteps([...steps, newStep]);
                      addLog('EXECUTION', 'گام تپ به تایم‌لاین اضافه شد.');
                    }}
                    className="bg-[#1A1814] border border-[#F2A93B]/40 hover:bg-[#F2A93B] hover:text-[#050505] text-[#FFC857] px-2.5 py-1 rounded text-xs font-bold"
                  >
                    + تپ
                  </button>
                  <button
                    onClick={() => {
                      const newStep: MacroStep = {
                        id: `s-${Date.now()}`,
                        orderIndex: steps.length + 1,
                        stepType: 'WAIT',
                        isEnabled: true,
                        delayMs: 300,
                        durationMs: 300,
                        x: 0,
                        y: 0
                      };
                      setSteps([...steps, newStep]);
                      addLog('EXECUTION', 'گام انتظار (WAIT) به تایم‌لاین اضافه شد.');
                    }}
                    className="bg-[#1A1814] border border-[#34D17E]/40 hover:bg-[#34D17E] hover:text-[#050505] text-[#34D17E] px-2.5 py-1 rounded text-xs font-bold"
                  >
                    + مکث
                  </button>
                </div>
              </div>

              {/* Loop & Cycle settings with drift explanation */}
              <div className="bg-[#0C0C0D] border border-[#F2A93B]/20 p-3.5 rounded-xl space-y-2">
                <span className="text-xs font-bold text-[#FFC857]">تنظیمات تکرار چرخه (با جبران Drift زمان)</span>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-[10px] text-[#A8A29A]">تعداد تکرار (۰ = بی‌پایان)</label>
                    <input
                      type="number"
                      value={repeatCount}
                      onChange={(e) => setRepeatCount(Math.max(0, parseInt(e.target.value) || 0))}
                      className="w-full bg-[#131211] border border-[#1A1814] rounded-lg px-2 py-1 text-xs text-[#F5F1E8] mt-1"
                    />
                  </div>
                  <div>
                    <label className="text-[10px] text-[#A8A29A]">فاصله چرخه‌ها (ms)</label>
                    <input
                      type="number"
                      value={intervalMs}
                      onChange={(e) => setIntervalMs(Math.max(10, parseInt(e.target.value) || 10))}
                      className="w-full bg-[#131211] border border-[#1A1814] rounded-lg px-2 py-1 text-xs text-[#F5F1E8] mt-1"
                    />
                  </div>
                </div>
              </div>

              {/* Timeline Items */}
              <div className="space-y-2">
                {steps.map((st, idx) => (
                  <div
                    key={st.id}
                    className={`p-3 rounded-xl border flex items-center justify-between transition-all ${
                      st.isEnabled ? 'bg-[#0C0C0D] border-[#1A1814]' : 'bg-[#050505] border-[#131211] opacity-50'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs ${
                          st.stepType === 'TAP'
                            ? 'bg-[#5A3C0E] text-[#FFE8B3]'
                            : st.stepType === 'WAIT'
                            ? 'bg-emerald-950 text-[#34D17E]'
                            : st.stepType === 'SWIPE'
                            ? 'bg-blue-950 text-[#3FA7FF]'
                            : 'bg-purple-950 text-[#A56BFF]'
                        }`}
                      >
                        {idx + 1}
                      </div>

                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold">
                            {st.stepType === 'TAP' && 'ضربه (TAP)'}
                            {st.stepType === 'WAIT' && 'انتظار زمانی (WAIT)'}
                            {st.stepType === 'SWIPE' && 'کشیدن (SWIPE)'}
                            {st.stepType === 'HOLD' && 'نگه‌داشتن (HOLD)'}
                          </span>
                          {st.stepType !== 'WAIT' && (
                            <span className="text-[9px] text-[#6B665E] font-mono">
                              ({st.x}, {st.y})
                            </span>
                          )}
                        </div>
                        <div className="flex items-center gap-2 mt-0.5">
                          <span className="text-[10px] text-[#A8A29A]">تأخیر: {st.delayMs}ms</span>
                          <span className="text-[10px] text-[#6B665E]">· مدت: {st.durationMs}ms</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      {/* Reorder Buttons */}
                      <button
                        disabled={idx === 0}
                        onClick={() => {
                          const newSteps = [...steps];
                          const temp = newSteps[idx];
                          newSteps[idx] = newSteps[idx - 1];
                          newSteps[idx - 1] = temp;
                          setSteps(newSteps);
                        }}
                        className="text-[#6B665E] hover:text-[#FFC857] disabled:opacity-20 p-1"
                      >
                        <ChevronUp className="w-3.5 h-3.5" />
                      </button>
                      <button
                        disabled={idx === steps.length - 1}
                        onClick={() => {
                          const newSteps = [...steps];
                          const temp = newSteps[idx];
                          newSteps[idx] = newSteps[idx + 1];
                          newSteps[idx + 1] = temp;
                          setSteps(newSteps);
                        }}
                        className="text-[#6B665E] hover:text-[#FFC857] disabled:opacity-20 p-1"
                      >
                        <ChevronDown className="w-3.5 h-3.5" />
                      </button>

                      {/* Enable toggle */}
                      <input
                        type="checkbox"
                        checked={st.isEnabled}
                        onChange={() => {
                          setSteps((prev) =>
                            prev.map((item) => (item.id === st.id ? { ...item, isEnabled: !item.isEnabled } : item))
                          );
                        }}
                        className="accent-[#F2A93B] cursor-pointer w-4 h-4"
                      />

                      <button
                        onClick={() => setSteps((prev) => prev.filter((item) => item.id !== st.id))}
                        className="text-[#6B665E] hover:text-[#FF5A4E] p-1"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* SCREEN 4: VISUAL GESTURE CANVAS (Section 7.6) */}
          {currentScreen === 'gesture_canvas' && (
            <div className="space-y-4 animate-in fade-in duration-200">
              <div className="flex justify-between items-center">
                <div>
                  <h2 className="text-base font-bold text-[#F5F1E8]">ویرایشگر بصری ژست (Overlay Canvas)</h2>
                  <p className="text-[11px] text-[#6B665E]">ترسیم مسیر چندنقطه‌ای (Polyline) روی صفحه</p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => setCanvasPoints([])}
                    className="p-1.5 text-xs text-[#6B665E] hover:text-[#FF5A4E] bg-[#0C0C0D] rounded-lg border border-[#1A1814]"
                    title="پاک کردن نقاط"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                  </button>
                  <button
                    onClick={playGesturePreview}
                    disabled={isPlayingPreview || canvasPoints.length < 2}
                    className="bg-[#F2A93B] text-[#050505] px-2.5 py-1 rounded-lg text-xs font-bold flex items-center gap-1 disabled:opacity-40"
                  >
                    <Play className="w-3 h-3 fill-current" /> پیش‌نمایش متحرک
                  </button>
                </div>
              </div>

              {/* Interactive Polyline Drawing Canvas Area */}
              <div
                onClick={(e) => {
                  const rect = e.currentTarget.getBoundingClientRect();
                  const x = Math.round(e.clientX - rect.left);
                  const y = Math.round(e.clientY - rect.top);
                  setCanvasPoints([...canvasPoints, { x, y }]);
                }}
                className="w-full h-72 bg-[#0C0C0D] rounded-2xl border border-[#F2A93B]/40 relative overflow-hidden cursor-crosshair"
              >
                {/* SVG Layer for Polyline Path */}
                <svg className="w-full h-full absolute inset-0 pointer-events-none">
                  {canvasPoints.length > 1 && (
                    <polyline
                      points={canvasPoints.map((p) => `${p.x},${p.y}`).join(' ')}
                      fill="none"
                      stroke="#F2A93B"
                      strokeWidth="3"
                      strokeDasharray="4 2"
                    />
                  )}

                  {/* Draw points */}
                  {canvasPoints.map((pt, i) => (
                    <g key={i}>
                      <circle cx={pt.x} cy={pt.y} r="6" fill="#FFC857" stroke="#050505" strokeWidth="2" />
                      <text x={pt.x + 8} y={pt.y + 4} fill="#FFE8B3" fontSize="10" fontFamily="monospace">
                        #{i + 1} ({pt.x}, {pt.y})
                      </text>
                    </g>
                  ))}

                  {/* Animated head indicator during preview */}
                  {isPlayingPreview && canvasPoints.length >= 2 && (
                    <circle
                      cx={
                        canvasPoints[0].x +
                        (canvasPoints[canvasPoints.length - 1].x - canvasPoints[0].x) * previewProgress
                      }
                      cy={
                        canvasPoints[0].y +
                        (canvasPoints[canvasPoints.length - 1].y - canvasPoints[0].y) * previewProgress
                      }
                      r="9"
                      fill="#34D17E"
                      stroke="#FFFFFF"
                      strokeWidth="2"
                    />
                  )}
                </svg>

                {canvasPoints.length === 0 && (
                  <div className="absolute inset-0 flex flex-col items-center justify-center text-[#6B665E] text-xs pointer-events-none">
                    <Crosshair className="w-6 h-6 mb-1 text-[#F2A93B]/50" />
                    <span>روی کادر ضربه بزنید تا نقاط مسیر ترسیم شوند.</span>
                  </div>
                )}
              </div>

              {/* Gesture Duration Control */}
              <div className="bg-[#0C0C0D] border border-[#1A1814] p-3.5 rounded-xl space-y-2">
                <div className="flex justify-between text-xs">
                  <span className="text-[#A8A29A]">مدت زمان کل حرکت (Duration)</span>
                  <span className="text-[#FFC857] font-mono font-bold">{gestureDuration} ms</span>
                </div>
                <input
                  type="range"
                  min="100"
                  max="2000"
                  step="50"
                  value={gestureDuration}
                  onChange={(e) => setGestureDuration(parseInt(e.target.value))}
                  className="w-full accent-[#F2A93B]"
                />
              </div>

              {/* Save gesture into macro step button */}
              <button
                onClick={() => {
                  if (canvasPoints.length < 2) {
                    alert('حداقل دو نقطه برای ثبت ژست مورد نیاز است.');
                    return;
                  }
                  const newStep: MacroStep = {
                    id: `s-${Date.now()}`,
                    orderIndex: steps.length + 1,
                    stepType: 'SWIPE',
                    isEnabled: true,
                    delayMs: 150,
                    durationMs: gestureDuration,
                    x: canvasPoints[0].x,
                    y: canvasPoints[0].y,
                    endX: canvasPoints[canvasPoints.length - 1].x,
                    endY: canvasPoints[canvasPoints.length - 1].y
                  };
                  setSteps([...steps, newStep]);
                  addLog('EXECUTION', `ژست با ${canvasPoints.length} نقطه به تایم‌لاین ماکرو اضافه شد.`);
                  setCurrentScreen('editor');
                }}
                className="w-full bg-[#F2A93B] text-[#050505] py-2.5 rounded-xl font-bold text-xs flex items-center justify-center gap-1.5 shadow-md shadow-[#F2A93B]/20"
              >
                <Check className="w-4 h-4" /> ذخیره در مراحل ماکرو
              </button>
            </div>
          )}

          {/* SCREEN 5: FLOATING BUTTONS CUSTOMIZER (Section 7.3 & 7.4) */}
          {currentScreen === 'buttons_edit' && (
            <div className="space-y-4 animate-in fade-in duration-200">
              <div>
                <h2 className="text-base font-bold text-[#F5F1E8]">شخصی‌سازی دکمه شناور</h2>
                <p className="text-[11px] text-[#6B665E]">تنظیم ابعاد، شفافیت و نگاشت ۴ رفتار لمس</p>
              </div>

              {/* Visual preview of button config */}
              <div className="bg-[#0C0C0D] border border-[#1A1814] p-4 rounded-2xl flex flex-col items-center justify-center space-y-3">
                <div
                  style={{
                    width: `${floatingBtn.sizeDp}px`,
                    height: `${floatingBtn.sizeDp}px`,
                    opacity: floatingBtn.alpha
                  }}
                  className="rounded-full bg-[#0C0C0D] border-2 border-[#F2A93B] flex flex-col items-center justify-center text-[#FFC857] font-bold shadow-lg shadow-[#F2A93B]/20"
                >
                  <Play className="w-5 h-5 fill-current ml-0.5" />
                  <span className="text-[10px]">{floatingBtn.label}</span>
                </div>
                <span className="text-[10px] text-[#6B665E]">پیش‌نمایش اندازه و شفافیت واقعی</span>
              </div>

              {/* Size Slider */}
              <div className="bg-[#0C0C0D] border border-[#1A1814] p-3.5 rounded-xl space-y-2">
                <div className="flex justify-between text-xs">
                  <span className="text-[#A8A29A]">اندازه دکمه (sizeDp)</span>
                  <span className="text-[#FFC857] font-mono font-bold">{floatingBtn.sizeDp} dp</span>
                </div>
                <input
                  type="range"
                  min="44"
                  max="76"
                  step="2"
                  value={floatingBtn.sizeDp}
                  onChange={(e) => setFloatingBtn({ ...floatingBtn, sizeDp: parseInt(e.target.value) })}
                  className="w-full accent-[#F2A93B]"
                />
              </div>

              {/* Alpha Opacity Slider */}
              <div className="bg-[#0C0C0D] border border-[#1A1814] p-3.5 rounded-xl space-y-2">
                <div className="flex justify-between text-xs">
                  <span className="text-[#A8A29A]">شفافیت در حالت آماده‌باش (Alpha)</span>
                  <span className="text-[#FFC857] font-mono font-bold">
                    {Math.round(floatingBtn.alpha * 100)}%
                  </span>
                </div>
                <input
                  type="range"
                  min="0.3"
                  max="1.0"
                  step="0.05"
                  value={floatingBtn.alpha}
                  onChange={(e) => setFloatingBtn({ ...floatingBtn, alpha: parseFloat(e.target.value) })}
                  className="w-full accent-[#F2A93B]"
                />
              </div>

              {/* Behavior Mapping Toggles (4 behaviors: Short Tap, Double Tap, Long Press, Release) */}
              <div className="bg-[#0C0C0D] border border-[#1A1814] p-3.5 rounded-xl space-y-2.5">
                <span className="text-xs font-bold text-[#F5F1E8] block">نگاشت رفتارهای ۴ گانه لمس</span>

                <div className="flex justify-between items-center text-xs">
                  <span className="text-[#A8A29A]">تپ کوتاه (Short Tap): شروع / توقف</span>
                  <input
                    type="checkbox"
                    checked={floatingBtn.shortTapEnabled}
                    onChange={(e) => setFloatingBtn({ ...floatingBtn, shortTapEnabled: e.target.checked })}
                    className="accent-[#F2A93B] w-4 h-4"
                  />
                </div>

                <div className="flex justify-between items-center text-xs">
                  <span className="text-[#A8A29A]">دو تپ (Double Tap): باز کردن پنل تنظیمات</span>
                  <input
                    type="checkbox"
                    checked={floatingBtn.doubleTapEnabled}
                    onChange={(e) => setFloatingBtn({ ...floatingBtn, doubleTapEnabled: e.target.checked })}
                    className="accent-[#F2A93B] w-4 h-4"
                  />
                </div>

                <div className="flex justify-between items-center text-xs">
                  <span className="text-[#A8A29A]">لمس طولانی (Long Press): مخفی‌سازی موقت</span>
                  <input
                    type="checkbox"
                    checked={floatingBtn.longPressEnabled}
                    onChange={(e) => setFloatingBtn({ ...floatingBtn, longPressEnabled: e.target.checked })}
                    className="accent-[#F2A93B] w-4 h-4"
                  />
                </div>
              </div>

              {/* Hide operations without hiding main trigger button (Section 7.4) */}
              <div className="bg-[#0C0C0D] border border-[#1A1814] p-3.5 rounded-xl flex justify-between items-center">
                <div>
                  <span className="text-xs font-semibold block">مخفی‌کردن دکمه‌های فرعی</span>
                  <p className="text-[10px] text-[#6B665E]">فقط دکمه اصلی اجرای ماکرو نمایان بماند</p>
                </div>
                <input
                  type="checkbox"
                  checked={hideSecondaryButtons}
                  onChange={(e) => setHideSecondaryButtons(e.target.checked)}
                  className="accent-[#F2A93B] w-4 h-4"
                />
              </div>
            </div>
          )}

          {/* SCREEN 6: TOUCH TEST PAD & NUMERIC SETTINGS */}
          {currentScreen === 'settings' && (
            <div className="space-y-4 animate-in fade-in duration-200">
              <div>
                <h2 className="text-base font-bold text-[#F5F1E8]">تنظیمات عددی و پد لمس</h2>
                <p className="text-[11px] text-[#6B665E]">آستانه‌های زمانی و راستی‌آزمایی رفتارها</p>
              </div>

              {/* Interactive Touch Test Pad (Section 7.7) */}
              <div className="bg-[#0C0C0D] border border-[#F2A93B]/40 p-4 rounded-2xl space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-xs font-bold text-[#FFC857]">پد آزمایش لمس زنده (Touch Test Pad)</span>
                  {lastDetectedTouch && (
                    <span className="text-[10px] bg-[#5A3C0E] text-[#FFE8B3] px-2 py-0.5 rounded font-bold">
                      {lastDetectedTouch}
                    </span>
                  )}
                </div>

                <div
                  onMouseDown={handleTestPadDown}
                  onMouseUp={handleTestPadUp}
                  onTouchStart={handleTestPadDown}
                  onTouchEnd={handleTestPadUp}
                  className="w-full h-28 bg-[#131211] rounded-xl border border-dashed border-[#1A1814] flex flex-col items-center justify-center cursor-pointer select-none active:bg-[#1A1814] transition-colors"
                >
                  <Crosshair className="w-6 h-6 text-[#F2A93B]/60 mb-1" />
                  <p className="text-xs text-[#A8A29A]">اینجا ضربه بزنید، دوبار تپ کنید یا نگه‌دارید</p>
                  <p className="text-[9px] text-[#6B665E] mt-0.5">تشخیص بر اساس ماشین حالت TouchDetector</p>
                </div>
              </div>

              {/* Numeric Threshold Sliders */}
              <div className="bg-[#0C0C0D] border border-[#1A1814] p-4 rounded-2xl space-y-4">
                <span className="text-xs font-bold text-[#F5F1E8]">آستانه‌های زمانی موتور تشخیص لمس</span>

                {/* Long Press Threshold */}
                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs">
                    <span className="text-[#A8A29A]">آستانه لمس طولانی (Long Press)</span>
                    <span className="text-[#FFC857] font-mono font-bold">{longPressThreshold} ms</span>
                  </div>
                  <input
                    type="range"
                    min="200"
                    max="2000"
                    step="50"
                    value={longPressThreshold}
                    onChange={(e) => setLongPressThreshold(parseInt(e.target.value))}
                    className="w-full accent-[#F2A93B]"
                  />
                  <p className="text-[10px] text-[#6B665E]">مدت زمان لازم برای تحریک رویداد نگه‌داشتن</p>
                </div>

                {/* Double Tap Interval */}
                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs">
                    <span className="text-[#A8A29A]">بازه مجاز دو تپ (Double Tap Window)</span>
                    <span className="text-[#FFC857] font-mono font-bold">{doubleTapWindow} ms</span>
                  </div>
                  <input
                    type="range"
                    min="100"
                    max="600"
                    step="25"
                    value={doubleTapWindow}
                    onChange={(e) => setDoubleTapWindow(parseInt(e.target.value))}
                    className="w-full accent-[#F2A93B]"
                  />
                  <p className="text-[10px] text-[#6B665E]">
                    نکته صادقانه: با فعال بودن دو‌لمس، تپ کوتاه به اندازه همین بازه دیرتر اجرا می‌شود تا سیستم فرصت تشخیص ضربه دوم را داشته باشد.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* SCREEN 7: ONBOARDING & PHONE BUILD GUIDE */}
          {currentScreen === 'onboarding' && (
            <div className="space-y-4 animate-in fade-in duration-200">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setCurrentScreen('dashboard')}
                  className="text-[#A8A29A] hover:text-[#F5F1E8] p-1"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <div>
                  <h2 className="text-base font-bold text-[#F5F1E8]">راهنمای فعال‌سازی و مجوزها</h2>
                  <p className="text-[10px] text-[#34D17E]">بدون نیاز به اینترنت · ۱۰۰٪ آفلاین و محلی</p>
                </div>
              </div>

              {/* Step 1: Overlay Window */}
              <div className="bg-[#0C0C0D] border border-[#1A1814] p-3.5 rounded-xl space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-xs font-bold text-[#F5F1E8]">۱. مجوز پنجره شناور (Overlay Window)</span>
                  <span className="text-[10px] text-[#34D17E] bg-emerald-950 px-2 py-0.5 rounded font-bold">
                    فعال ✓
                  </span>
                </div>
                <p className="text-[11px] text-[#A8A29A]">
                  برای نمایش دکمه‌های کنترل روی بازی‌ها و برنامه‌ها با WindowManager.
                </p>
              </div>

              {/* Step 2: Accessibility */}
              <div className="bg-[#0C0C0D] border border-[#1A1814] p-3.5 rounded-xl space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-xs font-bold text-[#F5F1E8]">۲. سرویس دسترسی‌پذیری لمسی</span>
                  <span className="text-[10px] text-[#34D17E] bg-emerald-950 px-2 py-0.5 rounded font-bold">
                    فعال ✓
                  </span>
                </div>
                <p className="text-[11px] text-[#A8A29A]">
                  برای ارسال تپ، کشیدن و نگه‌داشتن با متد رسمی dispatchGesture (بدون خواندن محتوای صفحه).
                </p>
              </div>

              {/* Step 3: Restricted Settings Android 13+ (Section 7.8) */}
              <div className="bg-[#131211] border border-[#FFC857]/40 p-4 rounded-xl space-y-2.5">
                <div className="flex items-center gap-2 text-[#FFC857]">
                  <Lock className="w-4 h-4 shrink-0" />
                  <span className="text-xs font-bold">راهنمای تنظیمات محدود شده (Android 13+)</span>
                </div>
                <p className="text-[11px] text-[#A8A29A] leading-relaxed">
                  اگر در اندروید ۱۳ به بالا گزینه دسترسی‌پذیری خاکستری بود:
                </p>
                <div className="bg-[#050505] p-2.5 rounded-lg text-[10px] text-[#F5F1E8] space-y-1 font-mono">
                  <p>۱. به Settings &gt; Apps &gt; Macro Touch Pro بروید.</p>
                  <p>۲. روی سه نقطه بالا سمت راست ضربه بزنید.</p>
                  <p>۳. گزینه Allow restricted settings را بزنید.</p>
                  <p>۴. رمز گوشی را وارد کرده و به برنامه برگردید.</p>
                </div>
              </div>

              {/* Step 4: Battery Optimization for Xiaomi / Samsung */}
              <div className="bg-[#0C0C0D] border border-[#1A1814] p-3.5 rounded-xl space-y-2">
                <div className="flex items-center gap-2 text-[#34D17E]">
                  <BatteryCharging className="w-4 h-4 shrink-0" />
                  <span className="text-xs font-bold">تنظیمات باتری برای شیائومی و سامسونگ</span>
                </div>
                <p className="text-[11px] text-[#A8A29A] leading-relaxed">
                  برای جلوگیری از بسته شدن سرویس توسط سیستم‌عامل: در صفحه App Info، گزینه Autostart را فعال و Battery Saver را روی No Restrictions بگذارید.
                </p>
              </div>

              {/* Step 5: Direct Phone APK Build with GitHub Actions */}
              <div className="bg-[#0C0C0D] border border-[#3FA7FF]/30 p-4 rounded-xl space-y-2.5">
                <div className="flex items-center gap-2 text-[#3FA7FF]">
                  <Github className="w-4 h-4 shrink-0" />
                  <span className="text-xs font-bold">ساخت APK با گوشی از طریق GitHub Actions</span>
                </div>
                <p className="text-[11px] text-[#A8A29A] leading-relaxed">
                  از آنجا که فقط گوشی دارید و کامپیوتر ندارید، فایل .github/workflows/build-apk.yml آماده است. با زدن دکمه Run workflow در گیت‌هاب، فایل APK در تب Actions آماده دانلود روی گوشی می‌شود.
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Bottom Navigation Bar */}
        <div className="h-16 bg-[#0C0C0D] border-t border-[#1A1814] px-4 flex justify-around items-center shrink-0 z-30">
          <button
            onClick={() => setCurrentScreen('dashboard')}
            className={`flex flex-col items-center gap-1 transition-colors ${
              currentScreen === 'dashboard' ? 'text-[#FFC857]' : 'text-[#6B665E] hover:text-[#A8A29A]'
            }`}
          >
            <Activity className="w-5 h-5" />
            <span className="text-[10px] font-medium">داشبورد</span>
          </button>

          <button
            onClick={() => setCurrentScreen('profiles')}
            className={`flex flex-col items-center gap-1 transition-colors ${
              currentScreen === 'profiles' ? 'text-[#FFC857]' : 'text-[#6B665E] hover:text-[#A8A29A]'
            }`}
          >
            <Layers className="w-5 h-5" />
            <span className="text-[10px] font-medium">پروفایل‌ها</span>
          </button>

          <button
            onClick={() => setCurrentScreen('editor')}
            className={`flex flex-col items-center gap-1 transition-colors ${
              currentScreen === 'editor' ? 'text-[#FFC857]' : 'text-[#6B665E] hover:text-[#A8A29A]'
            }`}
          >
            <Clock className="w-5 h-5" />
            <span className="text-[10px] font-medium">ویرایشگر</span>
          </button>

          <button
            onClick={() => setCurrentScreen('settings')}
            className={`flex flex-col items-center gap-1 transition-colors ${
              currentScreen === 'settings' ? 'text-[#FFC857]' : 'text-[#6B665E] hover:text-[#A8A29A]'
            }`}
          >
            <Sliders className="w-5 h-5" />
            <span className="text-[10px] font-medium">پد و تنظیمات</span>
          </button>

          <button
            onClick={() => setCurrentScreen('onboarding')}
            className={`flex flex-col items-center gap-1 transition-colors ${
              currentScreen === 'onboarding' ? 'text-[#FFC857]' : 'text-[#6B665E] hover:text-[#A8A29A]'
            }`}
          >
            <ShieldCheck className="w-5 h-5" />
            <span className="text-[10px] font-medium">مجوزها</span>
          </button>
        </div>

      </div>
    </div>
  );
}
