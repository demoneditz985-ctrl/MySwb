package com.shadow.injector;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Floating overlay menu for Shadow Injector.
 *
 *   - a draggable floating icon is shown as soon as the service starts
 *   - tapping the icon expands a menu with: CLOSE, HIDE, BUTTON 1..4
 *   - the Shizuku status row refreshes and toasts the result when tapped
 *
 * Declared in the manifest through
 * /Internal storage/.sketchware/data/&lt;sc_id&gt;/Injection/androidmanifest/app_components.txt
 */
public class FloatingService extends Service {

    private static final String CHANNEL_ID = "shadow_injector_overlay";
    private static final int NOTIFICATION_ID = 1017;

    private WindowManager windowManager;
    private View rootView;
    private WindowManager.LayoutParams params;

    private LinearLayout iconHolder;
    private LinearLayout panel;
    private TextView statusText;

    private float touchStartX;
    private float touchStartY;
    private int paramsStartX;
    private int paramsStartY;
    private long touchStartTime;
    private boolean dragged;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* ------------------------------------------------------------------ */
    /*  Public helpers used from the Sketchware blocks                     */
    /* ------------------------------------------------------------------ */

    public static boolean canDrawOverlays(Context context) {
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        try {
            return Settings.canDrawOverlays(context);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Opens the system "Display over other apps" page for this package.
     * <p>
     * IMPORTANT: from a click listener call
     * <code>FloatingService.requestOverlayPermission(MainActivity.this)</code> -
     * plain <code>this</code> inside an OnClickListener is the listener, not the Activity.
     */
    public static void requestOverlayPermission(Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Convenience overload: pass any view of the current screen. */
    public static void requestOverlayPermission(View view) {
        requestOverlayPermission(AndroidHelper.getActivity(view));
    }

    /**
     * Starts the floating icon. Does nothing when the overlay permission is missing.
     * <p>
     * IMPORTANT: from a click listener call
     * <code>FloatingService.start(MainActivity.this)</code>.
     */
    public static void start(Context context) {
        if (context == null || !canDrawOverlays(context)) {
            return;
        }
        Intent intent = new Intent(context, FloatingService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Throwable t) {
            try {
                context.startService(intent);
            } catch (Throwable ignored) {
            }
        }
    }

    /** Convenience overload: pass any view of the current screen. */
    public static void start(View view) {
        start(AndroidHelper.getContext(view));
    }

    /** Stops the floating icon completely. */
    public static void stop(Context context) {
        if (context == null) {
            return;
        }
        try {
            context.stopService(new Intent(context, FloatingService.class));
        } catch (Throwable ignored) {
        }
    }

    /** Convenience overload: pass any view of the current screen. */
    public static void stop(View view) {
        stop(AndroidHelper.getContext(view));
    }

    /** Convenience overload: pass any view of the current screen. */
    public static boolean canDrawOverlays(View view) {
        return canDrawOverlays(AndroidHelper.getContext(view));
    }

    /* ------------------------------------------------------------------ */
    /*  Lifecycle                                                          */
    /* ------------------------------------------------------------------ */

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundCompat();

        rootView = LayoutInflater.from(this).inflate(R.layout.floating, null);

        int overlayType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            overlayType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                flags,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 24;
        params.y = 180;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.addView(rootView, params);
        }

        bindViews();
        wireActions();
        refreshShizukuStatus();
    }

    @Override
    public void onDestroy() {
        if (windowManager != null && rootView != null) {
            try {
                windowManager.removeView(rootView);
            } catch (Throwable ignored) {
            }
        }
        rootView = null;
        super.onDestroy();
    }

    /* ------------------------------------------------------------------ */

    private void bindViews() {
        iconHolder = (LinearLayout) rootView.findViewById(R.id.icon_holder);
        panel = (LinearLayout) rootView.findViewById(R.id.panel);
        statusText = (TextView) rootView.findViewById(R.id.txt_status);
    }

    private void wireActions() {
        if (iconHolder != null) {
            iconHolder.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            touchStartX = event.getRawX();
                            touchStartY = event.getRawY();
                            paramsStartX = params.x;
                            paramsStartY = params.y;
                            touchStartTime = System.currentTimeMillis();
                            dragged = false;
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - touchStartX;
                            float dy = event.getRawY() - touchStartY;
                            if (Math.abs(dx) > 6f || Math.abs(dy) > 6f) {
                                dragged = true;
                            }
                            params.x = paramsStartX + (int) dx;
                            params.y = paramsStartY + (int) dy;
                            updateLayout();
                            return true;

                        case MotionEvent.ACTION_UP:
                            boolean quick = (System.currentTimeMillis() - touchStartTime) < 260L;
                            if (!dragged && quick) {
                                toggleMenu();
                            }
                            return true;

                        default:
                            return false;
                    }
                }
            });
        }

        setClick(R.id.btn_hide, new Runnable() {
            @Override
            public void run() {
                collapse();
            }
        });

        setClick(R.id.btn_close, new Runnable() {
            @Override
            public void run() {
                toast("Overlay closed");
                stopSelf();
            }
        });

        setClick(R.id.txt_status, new Runnable() {
            @Override
            public void run() {
                refreshShizukuStatus();
            }
        });

        wireMenuButton(R.id.btn1, "BUTTON 1");
        wireMenuButton(R.id.btn2, "BUTTON 2");
        wireMenuButton(R.id.btn3, "BUTTON 3");
        wireMenuButton(R.id.btn4, "BUTTON 4");
    }

    private void wireMenuButton(int id, final String label) {
        setClick(id, new Runnable() {
            @Override
            public void run() {
                toast(label + " activated");
            }
        });
    }

    private void setClick(int id, final Runnable action) {
        if (rootView == null) {
            return;
        }
        View view = rootView.findViewById(id);
        if (view == null) {
            return;
        }
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
    }

    private void toggleMenu() {
        if (iconHolder == null || panel == null) {
            return;
        }
        if (panel.getVisibility() == View.VISIBLE) {
            collapse();
        } else {
            iconHolder.setVisibility(View.GONE);
            panel.setVisibility(View.VISIBLE);
            refreshShizukuStatus();
        }
    }

    private void collapse() {
        if (panel != null) {
            panel.setVisibility(View.GONE);
        }
        if (iconHolder != null) {
            iconHolder.setVisibility(View.VISIBLE);
        }
    }

    private void refreshShizukuStatus() {
        ShizukuCheck.checkAsync(new ShizukuCheck.Callback() {
            @Override
            public void onResult(boolean running) {
                if (statusText != null) {
                    statusText.setText("SHIZUKU: " + (running ? "RUNNING" : "NOT RUNNING"));
                    statusText.setTextColor(ShizukuCheck.getStatusColor(running));
                }
                toast(ShizukuCheck.getStatusText(running));
            }
        });
    }

    private void toast(String message) {
        try {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    private void updateLayout() {
        if (windowManager == null || rootView == null) {
            return;
        }
        try {
            windowManager.updateViewLayout(rootView, params);
        } catch (Throwable ignored) {
        }
    }

    private void startForegroundCompat() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Floating menu", NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Keeps the Shadow Injector floating menu alive");
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(this);
            }
            builder.setContentTitle("Shadow Injector")
                    .setContentText("Floating menu is active")
                    .setSmallIcon(R.drawable.ic_stat_shadow)
                    .setOngoing(true);

            startForeground(NOTIFICATION_ID, builder.build());
        } catch (Throwable ignored) {
        }
    }
}
