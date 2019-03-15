package org.smartregister.p2p.presenter;

import android.content.DialogInterface;
import android.support.annotation.NonNull;

import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.sync.ISenderSyncLifecycleCallback;
import org.smartregister.p2p.sync.SenderSyncLifecycleCallback;
import org.smartregister.p2p.util.Constants;

import java.util.List;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 08/03/2019
 */

public class P2pModeSelectPresenter implements P2pModeSelectContract.Presenter {

    private P2pModeSelectContract.View view;
    private P2pModeSelectContract.Interactor interactor;

    private ISenderSyncLifecycleCallback senderSyncLifecycleCallback;

    public P2pModeSelectPresenter(@NonNull P2pModeSelectContract.View view, @NonNull P2pModeSelectContract.Interactor p2pModeSelectInteractor) {
        this.view = view;
        this.interactor = p2pModeSelectInteractor;

        // This will be added when issue https://github.com/OpenSRP/android-p2p-sync/issues/24
        // is being worked on
        //view.addOnResumeHandler(new AdvertisingResumeHandler(this, interactor));
        senderSyncLifecycleCallback = new SenderSyncLifecycleCallback(view, this, interactor);
    }

    @Override
    public void onSendButtonClicked() {
        prepareForDiscovering(false);
    }

    @Override
    public void onReceiveButtonClicked() {
        prepareForAdvertising(false);
    }

    @Override
    public void prepareForAdvertising(boolean returningFromRequestingPermissions) {
        List<String> unauthorisedPermissions = view.getUnauthorisedPermissions();
        // Are all required permissions given
        if (unauthorisedPermissions.size() == 0) {
            // Check if location is enabled
            if (view.isLocationEnabled()) {
                startAdvertisingMode();
            } else {
                view.requestEnableLocation(new P2pModeSelectContract.View.OnLocationEnabled() {
                    @Override
                    public void locationEnabled() {
                        startAdvertisingMode();
                    }
                });
            }
        } else {
            if (!returningFromRequestingPermissions) {
                view.addOnActivityRequestPermissionHandler(new OnActivityRequestPermissionHandler() {
                    @Override
                    public int getRequestCode() {
                        return Constants.RQ_CODE.PERMISSIONS;
                    }

                    @Override
                    public void handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
                        view.removeOnActivityRequestPermissionHandler(this);
                        P2pModeSelectPresenter.this.prepareForAdvertising(true);
                    }
                });
                view.requestPermissions(unauthorisedPermissions);
            }
        }
    }

    @Override
    public void startAdvertisingMode() {
        if (!interactor.isAdvertising()) {
            view.enableSendReceiveButtons(false);
            view.showReceiveProgressDialog(new P2pModeSelectContract.View.DialogCancelCallback() {
                @Override
                public void onCancelClicked(DialogInterface dialogInterface) {
                    interactor.stopAdvertising();
                    dialogInterface.dismiss();
                    view.enableSendReceiveButtons(true);
                }
            });
            interactor.startAdvertising();
        }
    }

    @Override
    public void prepareForDiscovering(boolean returningFromRequestingPermissions) {
        List<String> unauthorisedPermissions = view.getUnauthorisedPermissions();
        // Are all required permissions given
        if (unauthorisedPermissions.size() == 0) {
            // Check if location is enabled
            if (view.isLocationEnabled()) {
                startDiscoveringMode();
            } else {
                view.requestEnableLocation(new P2pModeSelectContract.View.OnLocationEnabled() {
                    @Override
                    public void locationEnabled() {
                        startDiscoveringMode();
                    }
                });
            }
        } else {
            if (!returningFromRequestingPermissions) {
                view.addOnActivityRequestPermissionHandler(new OnActivityRequestPermissionHandler() {
                    @Override
                    public int getRequestCode() {
                        return Constants.RQ_CODE.PERMISSIONS;
                    }

                    @Override
                    public void handlePermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
                        view.removeOnActivityRequestPermissionHandler(this);
                        P2pModeSelectPresenter.this.prepareForDiscovering(true);
                    }
                });
                view.requestPermissions(unauthorisedPermissions);
            }
        }
    }

    @Override
    public void startDiscoveringMode() {
        if (!interactor.isDiscovering()) {
            view.enableSendReceiveButtons(false);
            view.showDiscoveringProgressDialog (new P2pModeSelectContract.View.DialogCancelCallback() {
                @Override
                public void onCancelClicked(DialogInterface dialogInterface) {
                    interactor.stopDiscovering();
                    dialogInterface.dismiss();
                    view.enableSendReceiveButtons(true);
                }
            });
            interactor.startDiscovering(senderSyncLifecycleCallback);
        }
    }

    @Override
    public void onStop() {
        view.dismissAllDialogs();
        view.enableSendReceiveButtons(true);

        interactor.stopAdvertising();
        interactor.stopDiscovering();
        interactor.closeAllEndpoints();

        interactor.cleanupResources();
        interactor = null;
    }

}
