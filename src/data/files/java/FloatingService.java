package com.shadow.injector;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Floating overlay menu for Shadow Injector.
 *
 * <ul>
 *   <li>A floating icon is shown as soon as the service starts.</li>
 *   <li>Tapping the icon expands the menu; tapping again is not needed because
 *       the menu has its own HIDE button.</li>
 *   <li><b>Both the icon and the expanded menu can be dragged anywhere on the
 *       screen</b> - drag the icon itself, or drag the menu by its title bar,
 *       by any empty part of the panel, or by starting the drag on any button
 *       (a press that does not move still counts as a tap).</li>
 *   <li>BUTTON 1..4 are ON/OFF toggles; the whole row changes colour with the
 *       state, and the state is remembered.</li>
 *   <li>The Shizuku status row refreshes when tapped.</li>
 * </ul>
 *
 * Declared in the manifest through
 * /Internal storage/.sketchware/data/&lt;sc_id&gt;/Injection/androidmanifest/app_components.txt
 */
public class FloatingService extends Service {

    private static final String CHANNEL_ID = "shadow_injector_overlay";
    private static final int NOTIFICATION_ID = 1017;
    private static final String PREFS = "shadow_prefs";
    private static final String PREF_TOGGLE = "toggle_";

    /** Movement (px) after which a press becomes a drag instead of a tap. */
    private static final float TOUCH_SLOP = 8f;

    private WindowManager windowManager;
    private View rootView;
    private WindowManager.LayoutParams params;

    private LinearLayout iconHolder;
    private LinearLayout panel;
    private TextView statusText;

    private final boolean[] toggles = new boolean[4];
    private final View[] rows = new View[4];
    private final TextView[] rowLabels = new TextView[4];
    private final TextView[] rowStates = new TextView[4];

    /* drag state - shared by every draggable view */
    private float touchStartX;
    private float touchStartY;
    private int paramsStartX;
    private int paramsStartY;
    private boolean dragging;

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
        loadToggles();
        wireActions();
        refreshShizukuStatus(false);
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

        int[] rowIds = {R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4};
        int[] labelIds = {R.id.lbl1, R.id.lbl2, R.id.lbl3, R.id.lbl4};
        int[] stateIds = {R.id.state1, R.id.state2, R.id.state3, R.id.state4};
        for (int i = 0; i < 4; i++) {
            rows[i] = rootView.findViewById(rowIds[i]);
            rowLabels[i] = (TextView) rootView.findViewById(labelIds[i]);
            rowStates[i] = (TextView) rootView.findViewById(stateIds[i]);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Dragging - the same listener is attached to the icon, the whole    */
    /*  panel and every panel child, so the overlay follows your finger    */
    /*  no matter where you grab it.                                       */
    /* ------------------------------------------------------------------ */

    private final View.OnTouchListener DRAG = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartX = event.getRawX();
                    touchStartY = event.getRawY();
                    paramsStartX = params.x;
                    paramsStartY = params.y;
                    dragging = false;
                    // Return false so children (buttons) can still take the
                    // press and turn it into a click.
                    return false;

                case MotionEvent.ACTION_MOVE: {
                    float dx = event.getRawX() - touchStartX;
                    float dy = event.getRawY() - touchStartY;
                    if (!dragging && (Math.abs(dx) > TOUCH_SLOP || Math.abs(dy) > TOUCH_SLOP)) {
                        dragging = true;
                    }
                    if (dragging) {
                        params.x = paramsStartX + (int) dx;
                        params.y = paramsStartY + (int) dy;
                        clampToScreen();
                        updateLayout();
                        return true;
                    }
                    return false;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    boolean wasDragging = dragging;
                    dragging = false;
                    // Swallow the event when we actually dragged, otherwise the
                    // button under the finger would also fire its click.
                    return wasDragging;
                }

                default:
                    return false;
            }
        }
    };

    private void makeDraggable(View view) {
        if (view != null) {
            view.setOnTouchListener(DRAG);
        }
    }

    private void clampToScreen() {
        if (windowManager == null) {
            return;
        }
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            int w = rootView != null ? rootView.getWidth() : 0;
            int h = rootView != null ? rootView.getHeight() : 0;
            if (w == 0) {
                w = (int) (64 * getResources().getDisplayMetrics().density);
            }
            if (h == 0) {
                h = w;
            }
            // let a third of the bubble hang off each edge, never lose it
            int minX = -w / 3;
            int maxX = metrics.widthPixels - w + w / 3;
            int minY = 0;
            int maxY = metrics.heightPixels - h / 3;
            if (params.x < minX) {
                params.x = minX;
            }
            if (params.x > maxX) {
                params.x = maxX;
            }
            if (params.y < minY) {
                params.y = minY;
            }
            if (params.y > maxY) {
                params.y = maxY;
            }
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

    /* ------------------------------------------------------------------ */
    /*  Wiring                                                             */
    /* ------------------------------------------------------------------ */

    private void wireActions() {
        if (iconHolder != null) {
            makeDraggable(iconHolder);
            iconHolder.setClickable(true);
            iconHolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    expand();
                }
            });
        }

        if (panel != null) {
            // dragging anywhere on the panel itself (title bar, gaps, hint text)
            makeDraggable(panel);
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
                toast("Checking Shizuku...");
                refreshShizukuStatus(true);
            }
        });

        for (int i = 0; i < 4; i++) {
            wireToggle(i);
        }
    }

    private void wireToggle(final int index) {
        if (rows[index] == null) {
            return;
        }
        // also draggable: press and slide on a button moves the whole menu
        makeDraggable(rows[index]);
        rows[index].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggles[index] = !toggles[index];
                applyToggle(index);
                saveToggle(index);
                toast((toggles[index] ? "BUTTON " : "BUTTON ") + (index + 1)
                        + (toggles[index] ? "  ON" : "  OFF"));
            }
        });
        applyToggle(index);
    }

    private void applyToggle(int index) {
        boolean on = toggles[index];
        if (rows[index] != null) {
            rows[index].setBackgroundResource(on ? R.drawable.toggle_on : R.drawable.toggle_off);
        }
        if (rowStates[index] != null) {
            rowStates[index].setText(on ? "ON" : "OFF");
            rowStates[index].setTextColor(on ? 0xFF052E26 : 0xFF7C6FA8);
        }
        if (rowLabels[index] != null) {
            rowLabels[index].setTextColor(on ? 0xFF052E26 : 0xFFC9BEE8);
        }
    }

    private void loadToggles() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        for (int i = 0; i < 4; i++) {
            toggles[i] = prefs.getBoolean(PREF_TOGGLE + (i + 1), false);
        }
    }

    private void saveToggle(int index) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            prefs.edit().putBoolean(PREF_TOGGLE + (index + 1), toggles[index]).apply();
        } catch (Throwable ignored) {
        }
    }

    private void setClick(int id, final Runnable action) {
        if (rootView == null) {
            return;
        }
        View view = rootView.findViewById(id);
        if (view == null) {
            return;
        }
        if (id == R.id.txt_status) {
            makeDraggable(view);
        }
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
    }

    private void expand() {
        if (iconHolder == null || panel == null) {
            return;
        }
        iconHolder.setVisibility(View.GONE);
        panel.setVisibility(View.VISIBLE);
        refreshShizukuStatus(false);
    }

    private void collapse() {
        if (panel != null) {
            panel.setVisibility(View.GONE);
        }
        if (iconHolder != null) {
            iconHolder.setVisibility(View.VISIBLE);
        }
    }

    private void refreshShizukuStatus(final boolean announce) {
        ShizukuCheck.checkAsync(new ShizukuCheck.Callback() {
            @Override
            public void onResult(boolean running) {
                if (statusText != null) {
                    statusText.setText("SHIZUKU: " + (running ? "RUNNING" : "NOT RUNNING"));
                    statusText.setTextColor(ShizukuCheck.getStatusColor(running));
                }
                if (announce) {
                    toast(ShizukuCheck.getStatusText(running));
                }
            }
        });
    }

    private void toast(String message) {
        try {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
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
