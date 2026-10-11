package moe.shizuku.server;

/*
 * Hand-written equivalent of the AIDL-generated stub for
 *   aidl/moe/shizuku/server/IShizukuApplication.aidl
 * (SketchWare cannot compile .aidl files, so the generated code is inlined here.)
 */

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IShizukuApplication extends IInterface {

    void bindApplication(Bundle data) throws RemoteException;

    void dispatchRequestPermissionResult(int requestCode, Bundle data) throws RemoteException;

    void showPermissionConfirmation(int requestUid, int requestPid, String requestPackageName, int requestCode) throws RemoteException;

    abstract class Stub extends Binder implements IShizukuApplication {

        private static final String DESCRIPTOR = "moe.shizuku.server.IShizukuApplication";

        static final int TRANSACTION_bindApplication = 1;
        static final int TRANSACTION_dispatchRequestPermissionResult = 2;
        static final int TRANSACTION_showPermissionConfirmation = 10000;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IShizukuApplication asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IShizukuApplication) {
                return (IShizukuApplication) iin;
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
                case TRANSACTION_bindApplication: {
                    data.enforceInterface(DESCRIPTOR);
                    Bundle _arg0;
                    if (data.readInt() != 0) {
                        _arg0 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    bindApplication(_arg0);
                    return true;
                }
                case TRANSACTION_dispatchRequestPermissionResult: {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    Bundle _arg1;
                    if (data.readInt() != 0) {
                        _arg1 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    dispatchRequestPermissionResult(_arg0, _arg1);
                    return true;
                }
                case TRANSACTION_showPermissionConfirmation: {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    int _arg1 = data.readInt();
                    String _arg2 = data.readString();
                    int _arg3 = data.readInt();
                    showPermissionConfirmation(_arg0, _arg1, _arg2, _arg3);
                    reply.writeNoException();
                    return true;
                }
                default:
                    break;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IShizukuApplication {

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
            public void bindApplication(Bundle data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_bindApplication, _data, null, IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void dispatchRequestPermissionResult(int requestCode, Bundle data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(requestCode);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_dispatchRequestPermissionResult, _data, null, IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void showPermissionConfirmation(int requestUid, int requestPid, String requestPackageName, int requestCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(requestUid);
                    _data.writeInt(requestPid);
                    _data.writeString(requestPackageName);
                    _data.writeInt(requestCode);
                    mRemote.transact(TRANSACTION_showPermissionConfirmation, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
