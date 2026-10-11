package moe.shizuku.server;

/*
 * Hand-written equivalent of the AIDL-generated stub for
 *   aidl/moe/shizuku/server/IRemoteProcess.aidl
 * (SketchWare cannot compile .aidl files, so the generated code is inlined here.)
 */

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public interface IRemoteProcess extends IInterface {

    ParcelFileDescriptor getOutputStream() throws RemoteException;

    ParcelFileDescriptor getInputStream() throws RemoteException;

    ParcelFileDescriptor getErrorStream() throws RemoteException;

    int waitFor() throws RemoteException;

    int exitValue() throws RemoteException;

    void destroy() throws RemoteException;

    boolean alive() throws RemoteException;

    boolean waitForTimeout(long timeout, String unit) throws RemoteException;

    abstract class Stub extends Binder implements IRemoteProcess {

        private static final String DESCRIPTOR = "moe.shizuku.server.IRemoteProcess";

        static final int TRANSACTION_getOutputStream = 1;
        static final int TRANSACTION_getInputStream = 2;
        static final int TRANSACTION_getErrorStream = 3;
        static final int TRANSACTION_waitFor = 4;
        static final int TRANSACTION_exitValue = 5;
        static final int TRANSACTION_destroy = 6;
        static final int TRANSACTION_alive = 7;
        static final int TRANSACTION_waitForTimeout = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRemoteProcess asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IRemoteProcess) {
                return (IRemoteProcess) iin;
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
                case TRANSACTION_getOutputStream: {
                    data.enforceInterface(DESCRIPTOR);
                    ParcelFileDescriptor _result = getOutputStream();
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                }
                case TRANSACTION_getInputStream: {
                    data.enforceInterface(DESCRIPTOR);
                    ParcelFileDescriptor _result = getInputStream();
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                }
                case TRANSACTION_getErrorStream: {
                    data.enforceInterface(DESCRIPTOR);
                    ParcelFileDescriptor _result = getErrorStream();
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                }
                case TRANSACTION_waitFor: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = waitFor();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_exitValue: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = exitValue();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_destroy: {
                    data.enforceInterface(DESCRIPTOR);
                    destroy();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_alive: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = alive();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_waitForTimeout: {
                    data.enforceInterface(DESCRIPTOR);
                    long _arg0 = data.readLong();
                    String _arg1 = data.readString();
                    boolean _result = waitForTimeout(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                default:
                    break;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IRemoteProcess {

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
            public ParcelFileDescriptor getOutputStream() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                ParcelFileDescriptor _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getOutputStream, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public ParcelFileDescriptor getInputStream() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                ParcelFileDescriptor _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getInputStream, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public ParcelFileDescriptor getErrorStream() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                ParcelFileDescriptor _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getErrorStream, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public int waitFor() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_waitFor, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public int exitValue() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_exitValue, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void destroy() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_destroy, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean alive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_alive, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public boolean waitForTimeout(long timeout, String unit) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(timeout);
                    _data.writeString(unit);
                    mRemote.transact(TRANSACTION_waitForTimeout, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }
    }
}
