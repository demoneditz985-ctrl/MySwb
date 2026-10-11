package moe.shizuku.server;

/*
 * Hand-written equivalent of the AIDL-generated stub for
 *   aidl/moe/shizuku/server/IShizukuServiceConnection.aidl
 * (SketchWare cannot compile .aidl files, so the generated code is inlined here.)
 */

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IShizukuServiceConnection extends IInterface {

    void connected(IBinder service) throws RemoteException;

    void died() throws RemoteException;

    abstract class Stub extends Binder implements IShizukuServiceConnection {

        private static final String DESCRIPTOR = "moe.shizuku.server.IShizukuServiceConnection";

        static final int TRANSACTION_connected = 1;
        static final int TRANSACTION_died = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IShizukuServiceConnection asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IShizukuServiceConnection) {
                return (IShizukuServiceConnection) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                case TRANSACTION_connected: {
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg0 = data.readStrongBinder();
                    connected(_arg0);
                    return true;
                }
                case TRANSACTION_died: {
                    data.enforceInterface(DESCRIPTOR);
                    died();
                    return true;
                }
                default:
                    break;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IShizukuServiceConnection {

            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public void connected(IBinder service) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(service);
                    mRemote.transact(TRANSACTION_connected, _data, null, IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void died() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_died, _data, null, IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
