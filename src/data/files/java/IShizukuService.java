package moe.shizuku.server;

/*
 * Hand-written equivalent of the AIDL-generated stub for
 *   aidl/moe/shizuku/server/IShizukuService.aidl
 * (SketchWare cannot compile .aidl files, so the generated code is inlined here.)
 *
 * Transaction codes MUST stay in sync with Shizuku server - they are part of
 * the public binder contract.
 */

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IShizukuService extends IInterface {

    int getVersion() throws RemoteException;

    int getUid() throws RemoteException;

    int checkPermission(String permission) throws RemoteException;

    IRemoteProcess newProcess(String[] cmd, String[] env, String dir) throws RemoteException;

    String getSELinuxContext() throws RemoteException;

    String getSystemProperty(String name, String defaultValue) throws RemoteException;

    void setSystemProperty(String name, String value) throws RemoteException;

    int addUserService(IShizukuServiceConnection conn, Bundle args) throws RemoteException;

    int removeUserService(IShizukuServiceConnection conn, Bundle args) throws RemoteException;

    void attachApplication(IShizukuApplication application, String requestPackageName) throws RemoteException;

    void requestPermission(int requestCode) throws RemoteException;

    boolean checkSelfPermission() throws RemoteException;

    boolean shouldShowRequestPermissionRationale() throws RemoteException;

    void exit() throws RemoteException;

    void attachUserService(IBinder binder, Bundle options) throws RemoteException;

    void dispatchPackageChanged(Intent intent) throws RemoteException;

    boolean isHidden(int uid) throws RemoteException;

    void dispatchPermissionConfirmationResult(int requestUid, int requestPid, int requestCode, Bundle data) throws RemoteException;

    int getFlagsForUid(int uid, int mask) throws RemoteException;

    void updateFlagsForUid(int uid, int mask, int value) throws RemoteException;

    abstract class Stub extends Binder implements IShizukuService {

        private static final String DESCRIPTOR = "moe.shizuku.server.IShizukuService";

        static final int TRANSACTION_getVersion = 2;
        static final int TRANSACTION_getUid = 3;
        static final int TRANSACTION_checkPermission = 4;
        static final int TRANSACTION_newProcess = 7;
        static final int TRANSACTION_getSELinuxContext = 8;
        static final int TRANSACTION_getSystemProperty = 9;
        static final int TRANSACTION_setSystemProperty = 10;
        static final int TRANSACTION_addUserService = 11;
        static final int TRANSACTION_removeUserService = 12;
        static final int TRANSACTION_attachApplication = 13;
        static final int TRANSACTION_requestPermission = 14;
        static final int TRANSACTION_checkSelfPermission = 15;
        static final int TRANSACTION_shouldShowRequestPermissionRationale = 16;
        static final int TRANSACTION_exit = 100;
        static final int TRANSACTION_attachUserService = 101;
        static final int TRANSACTION_dispatchPackageChanged = 102;
        static final int TRANSACTION_isHidden = 103;
        static final int TRANSACTION_dispatchPermissionConfirmationResult = 104;
        static final int TRANSACTION_getFlagsForUid = 105;
        static final int TRANSACTION_updateFlagsForUid = 106;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IShizukuService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IShizukuService) {
                return (IShizukuService) iin;
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
                case TRANSACTION_getVersion: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = getVersion();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_getUid: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = getUid();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_checkPermission: {
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    int _result = checkPermission(_arg0);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_newProcess: {
                    data.enforceInterface(DESCRIPTOR);
                    String[] _arg0 = data.createStringArray();
                    String[] _arg1 = data.createStringArray();
                    String _arg2 = data.readString();
                    IRemoteProcess _result = newProcess(_arg0, _arg1, _arg2);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result != null ? _result.asBinder() : null);
                    return true;
                }
                case TRANSACTION_getSELinuxContext: {
                    data.enforceInterface(DESCRIPTOR);
                    String _result = getSELinuxContext();
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
                case TRANSACTION_getSystemProperty: {
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    String _arg1 = data.readString();
                    String _result = getSystemProperty(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
                case TRANSACTION_setSystemProperty: {
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    String _arg1 = data.readString();
                    setSystemProperty(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_addUserService: {
                    data.enforceInterface(DESCRIPTOR);
                    IShizukuServiceConnection _arg0 = IShizukuServiceConnection.Stub.asInterface(data.readStrongBinder());
                    Bundle _arg1;
                    if (data.readInt() != 0) {
                        _arg1 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    int _result = addUserService(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_removeUserService: {
                    data.enforceInterface(DESCRIPTOR);
                    IShizukuServiceConnection _arg0 = IShizukuServiceConnection.Stub.asInterface(data.readStrongBinder());
                    Bundle _arg1;
                    if (data.readInt() != 0) {
                        _arg1 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    int _result = removeUserService(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_attachApplication: {
                    data.enforceInterface(DESCRIPTOR);
                    IShizukuApplication _arg0 = IShizukuApplication.Stub.asInterface(data.readStrongBinder());
                    String _arg1 = data.readString();
                    attachApplication(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_requestPermission: {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    requestPermission(_arg0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_checkSelfPermission: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = checkSelfPermission();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_shouldShowRequestPermissionRationale: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = shouldShowRequestPermissionRationale();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_exit: {
                    data.enforceInterface(DESCRIPTOR);
                    exit();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_attachUserService: {
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg0 = data.readStrongBinder();
                    Bundle _arg1;
                    if (data.readInt() != 0) {
                        _arg1 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    attachUserService(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_dispatchPackageChanged: {
                    data.enforceInterface(DESCRIPTOR);
                    Intent _arg0;
                    if (data.readInt() != 0) {
                        _arg0 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    dispatchPackageChanged(_arg0);
                    return true;
                }
                case TRANSACTION_isHidden: {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    boolean _result = isHidden(_arg0);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_dispatchPermissionConfirmationResult: {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    int _arg1 = data.readInt();
                    int _arg2 = data.readInt();
                    Bundle _arg3;
                    if (data.readInt() != 0) {
                        _arg3 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg3 = null;
                    }
                    dispatchPermissionConfirmationResult(_arg0, _arg1, _arg2, _arg3);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getFlagsForUid: {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    int _arg1 = data.readInt();
                    int _result = getFlagsForUid(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_updateFlagsForUid: {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    int _arg1 = data.readInt();
                    int _arg2 = data.readInt();
                    updateFlagsForUid(_arg0, _arg1, _arg2);
                    reply.writeNoException();
                    return true;
                }
                default:
                    break;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IShizukuService {

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
            public int getVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getVersion, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public int getUid() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getUid, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public int checkPermission(String permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(permission);
                    mRemote.transact(TRANSACTION_checkPermission, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public IRemoteProcess newProcess(String[] cmd, String[] env, String dir) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                IRemoteProcess _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStringArray(cmd);
                    _data.writeStringArray(env);
                    _data.writeString(dir);
                    mRemote.transact(TRANSACTION_newProcess, _data, _reply, 0);
                    _reply.readException();
                    _result = IRemoteProcess.Stub.asInterface(_reply.readStrongBinder());
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public String getSELinuxContext() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getSELinuxContext, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public String getSystemProperty(String name, String defaultValue) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeString(defaultValue);
                    mRemote.transact(TRANSACTION_getSystemProperty, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void setSystemProperty(String name, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeString(value);
                    mRemote.transact(TRANSACTION_setSystemProperty, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int addUserService(IShizukuServiceConnection conn, Bundle args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(conn != null ? conn.asBinder() : null);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_addUserService, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public int removeUserService(IShizukuServiceConnection conn, Bundle args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(conn != null ? conn.asBinder() : null);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_removeUserService, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void attachApplication(IShizukuApplication application, String requestPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(application != null ? application.asBinder() : null);
                    _data.writeString(requestPackageName);
                    mRemote.transact(TRANSACTION_attachApplication, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void requestPermission(int requestCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(requestCode);
                    mRemote.transact(TRANSACTION_requestPermission, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean checkSelfPermission() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_checkSelfPermission, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public boolean shouldShowRequestPermissionRationale() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_shouldShowRequestPermissionRationale, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void exit() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_exit, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void attachUserService(IBinder binder, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(binder);
                    if (options != null) {
                        _data.writeInt(1);
                        options.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_attachUserService, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void dispatchPackageChanged(Intent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_dispatchPackageChanged, _data, null, IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public boolean isHidden(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    mRemote.transact(TRANSACTION_isHidden, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void dispatchPermissionConfirmationResult(int requestUid, int requestPid, int requestCode, Bundle data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(requestUid);
                    _data.writeInt(requestPid);
                    _data.writeInt(requestCode);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_dispatchPermissionConfirmationResult, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getFlagsForUid(int uid, int mask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(mask);
                    mRemote.transact(TRANSACTION_getFlagsForUid, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void updateFlagsForUid(int uid, int mask, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(mask);
                    _data.writeInt(value);
                    mRemote.transact(TRANSACTION_updateFlagsForUid, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
