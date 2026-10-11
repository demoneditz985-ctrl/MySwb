package com.shadow.injector;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Tiny, dependency free helper that reports whether Shizuku is currently running.
 *
 * No Shizuku API library is required - the check is done with:
 *   1. a scan of /proc/&lt;pid&gt;/cmdline   (works without any permission)
 *   2. shell fallbacks: pidof / ps -A / ps
 *
 * Usage from an Activity:
 * <pre>
 *     ShizukuCheck.showStatus(this);                  // toast the result
 *     ShizukuCheck.checkAsync(new ShizukuCheck.Callback() {
 *         public void onResult(boolean running) { ... }
 *     });
 * </pre>
 */
public class ShizukuCheck {

    /** Package name of the Shizuku app (both the manager and the privileged API). */
    public static final String SHIZUKU_PACKAGE = "moe.shizuku.privileged.api";

    /** Alternate package name used by older Shizuku builds. */
    public static final String SHIZUKU_PACKAGE_OLD = "moe.shizuku.privileged.api";

    private ShizukuCheck() {
    }

    /** Blocking check - call this from a background thread. */
    public static boolean isRunning() {
        if (scanProc()) {
            return true;
        }
        if (shellContains("pidof " + SHIZUKU_PACKAGE)) {
            return true;
        }
        if (shellContains("ps -A")) {
            return true;
        }
        return shellContains("ps");
    }

    /** Same as {@link #isRunning()} but off the main thread. */
    public static void checkAsync(final Callback callback) {
        if (callback == null) {
            return;
        }
        final Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean running = isRunning();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(running);
                    }
                });
            }
        }).start();
    }

    /**
     * Shows "Shizuku is running" / "Shizuku isn't running" as a toast.
     * <p>
     * IMPORTANT: when calling this from a click listener use
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
                show(context, running ? "Shizuku is running" : "Shizuku isn't running");
            }
        });
    }

    /** Convenience overload: pass any view of the current screen. */
    public static void showStatus(View view) {
        showStatus(AndroidHelper.getContext(view));
    }

    public static void show(final Context context, final String message) {
        if (context == null || message == null) {
            return;
        }
        try {
            Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    public static String getStatusText(boolean running) {
        return running ? "Shizuku is running" : "Shizuku isn't running";
    }

    public static int getStatusColor(boolean running) {
        return running ? 0xFF34D399 : 0xFFFF4D6D;
    }

    /* ------------------------------------------------------------------ */

    private static boolean scanProc() {
        try {
            File proc = new File("/proc");
            File[] dirs = proc.listFiles();
            if (dirs == null) {
                return false;
            }
            for (int i = 0; i < dirs.length; i++) {
                if (!dirs[i].isDirectory()) {
                    continue;
                }
                String cmdline = readSmallFile(new File(dirs[i], "cmdline"));
                if (cmdline != null && cmdline.contains(SHIZUKU_PACKAGE)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static String readSmallFile(File file) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] buffer = new byte[512];
            int read = in.read(buffer);
            if (read <= 0) {
                return null;
            }
            return new String(buffer, 0, read, "UTF-8");
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static boolean shellContains(String command) {
        Process process = null;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(SHIZUKU_PACKAGE)) {
                    return true;
                }
            }
            process.waitFor();
        } catch (Throwable ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {
                }
            }
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
    }

    public interface Callback {
        void onResult(boolean running);
    }
}
