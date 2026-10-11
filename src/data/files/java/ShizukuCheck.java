package com.shadow.injector;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

/**
 * Shizuku status + permission helper for Shadow Injector.
 *
 * <p>The detection is done through the <b>real Shizuku API</b>
 * ({@code rikka.shizuku.Shizuku}, bundled in this project) instead of scanning
 * {@code /proc}. That matters, because a /proc scan reports "running" whenever
 * the Shizuku <i>manager</i> process merely exists - including when the manager
 * has been opened but the server was never started, and when the process is only
 * sitting cached in the background. That was the source of the false
 * "Shizuku is running" reports.</p>
 *
 * <p>{@link Shizuku#pingBinder()} is authoritative: the binder is only alive
 * while the Shizuku <b>server</b> is actually up, and it dies the moment the
 * server stops.</p>
 *
 * <p>The app shows up in Shizuku Manager's "authorized applications" list
 * because the manifest declares
 * {@code moe.shizuku.manager.permission.API_V23} and registers
 * {@code rikka.shizuku.ShizukuProvider} - see
 * {@code data/Injection/androidmanifest/app_components.txt}.</p>
 *
 * Usage from an Activity:
 * <pre>
 *     ShizukuCheck.showStatus(MainActivity.this);
 *     ShizukuCheck.requestPermission(InjectorActivity.this);
 * </pre>
 */
public class ShizukuCheck {

    /** Package name of the Shizuku manager / privileged API. */
    public static final String SHIZUKU_PACKAGE = "moe.shizuku.privileged.api";

    /** Any request code, only used to match the result callback. */
    public static final int REQUEST_CODE = 42317;

    /**
     * How long to wait for Shizuku to push its binder to our provider.
     * Shizuku hands the binder to apps a moment after they start, so a check
     * that runs too early would otherwise report a false "not running".
     */
    private static final long BINDER_WAIT_MS = 1500L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static boolean resultListenerAdded = false;

    /** Last context we were handed, so late callbacks can still toast. */
    private static volatile Context appContext;

    private ShizukuCheck() {
    }

    /** Remembers a context for callbacks that arrive without one. */
    public static void bind(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Status                                                             */
    /* ------------------------------------------------------------------ */

    /**
     * Blocking check - call from a background thread.
     *
     * @return true only when the Shizuku server is really up and talking to us.
     */
    public static boolean isRunning() {
        if (ping()) {
            return true;
        }
        // Shizuku delivers the binder shortly after the app process starts.
        final long deadline = System.currentTimeMillis() + BINDER_WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException ignored) {
                break;
            }
            if (ping()) {
                return true;
            }
        }
        return false;
    }

    private static boolean ping() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Off the main thread version of {@link #isRunning()}. */
    public static void checkAsync(final Callback callback) {
        if (callback == null) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean running = isRunning();
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(running);
                    }
                });
            }
        }).start();
    }

    /** Is Shizuku installed at all? */
    public static boolean isInstalled(Context context) {
        if (context == null) {
            return false;
        }
        try {
            context.getPackageManager().getPackageInfo(SHIZUKU_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Permission                                                         */
    /* ------------------------------------------------------------------ */

    /** true when Shizuku has already granted this app the API permission. */
    public static boolean isPermissionGranted() {
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Asks Shizuku to grant this app access. The request is only sent when the
     * server is up; otherwise a toast explains that Shizuku has to be started
     * first.
     */
    public static void requestPermission(final Activity activity) {
        if (activity == null) {
            return;
        }
        checkAsync(new Callback() {
            @Override
            public void onResult(final boolean running) {
                if (!running) {
                    show(activity, "Shizuku isn't running");
                    return;
                }
                if (isPermissionGranted()) {
                    show(activity, "Shizuku is running");
                    return;
                }
                addResultListener();
                try {
                    Shizuku.requestPermission(REQUEST_CODE);
                    show(activity, "Allow Shadow Injector in the Shizuku popup");
                } catch (Throwable t) {
                    show(activity, "Shizuku isn't running");
                }
            }
        });
    }

    private static void addResultListener() {
        if (resultListenerAdded) {
            return;
        }
        try {
            Shizuku.addRequestPermissionResultListener(
                    new Shizuku.OnRequestPermissionResultListener() {
                        @Override
                        public void onRequestPermissionResult(int requestCode, int result) {
                            if (requestCode != REQUEST_CODE) {
                                return;
                            }
                            boolean granted = result == PackageManager.PERMISSION_GRANTED;
                            ShizukuCheck.show(null, granted
                                    ? "Shizuku permission granted"
                                    : "Shizuku permission denied");
                        }
                    });
            resultListenerAdded = true;
        } catch (Throwable ignored) {
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Toasts                                                             */
    /* ------------------------------------------------------------------ */

    /**
     * Shows "Shizuku is running" / "Shizuku isn't running" as a toast.
     * <p>
     * IMPORTANT: from a click listener call
     * <code>ShizukuCheck.showStatus(MainActivity.this)</code> - plain
     * <code>this</code> inside an OnClickListener is the listener, not the Activity.
     */
    public static void showStatus(final Context context) {
        if (context == null) {
            return;
        }
        checkAsync(new Callback() {
            @Override
            public void onResult(boolean running) {
                show(context, getStatusText(running));
            }
        });
    }

    /** Convenience overload: pass any view of the current screen. */
    public static void showStatus(View view) {
        showStatus(AndroidHelper.getContext(view));
    }

    /** Convenience overload: pass any view of the current screen. */
    public static void requestPermission(View view) {
        requestPermission(AndroidHelper.getActivity(view));
    }

    public static void show(final Context context, final String message) {
        if (message == null) {
            return;
        }
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                if (context != null) {
                    bind(context);
                }
                Context target = appContext;
                if (target == null) {
                    return;
                }
                try {
                    Toast.makeText(target, message, Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {
                }
            }
        });
    }

    public static String getStatusText(boolean running) {
        return running ? "Shizuku is running" : "Shizuku isn't running";
    }

    public static int getStatusColor(boolean running) {
        return running ? 0xFF34D399 : 0xFFFF4D6D;
    }

    public interface Callback {
        void onResult(boolean running);
    }
}
