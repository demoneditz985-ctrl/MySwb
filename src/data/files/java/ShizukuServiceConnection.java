package rikka.shizuku;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

import moe.shizuku.server.IShizukuServiceConnection;

class ShizukuServiceConnection extends IShizukuServiceConnection.Stub {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private final Set<ServiceConnection> connections = new HashSet<ServiceConnection>();
    private final ComponentName componentName;

    public ShizukuServiceConnection(Shizuku.UserServiceArgs args) {
        this.componentName = args.componentName;
    }

    private boolean dead = false;

    public void addConnection(@Nullable ServiceConnection conn) {
        if (conn != null) {
            connections.add(conn);
        }
    }

    public void removeConnection(@Nullable ServiceConnection conn) {
        if (conn != null) {
            connections.remove(conn);
        }
    }

    @Override
    public void connected(final IBinder binder) {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                for (ServiceConnection conn : connections) {
                    conn.onServiceConnected(componentName, binder);
                }
            }
        });

        try {
            binder.linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    ShizukuServiceConnection.this.died();
                }
            }, 0);
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public void died() {
        if (dead) return;
        dead = true;

        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                for (ServiceConnection conn : connections) {
                    conn.onServiceDisconnected(componentName);
                }
            }
        });
    }
}
