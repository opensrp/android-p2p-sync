# Android P2P Sync [![Build Status](https://travis-ci.org/OpenSRP/android-p2p-sync.svg?branch=master)](https://travis-ci.org/OpenSRP/android-p2p-sync) [![Coverage Status](https://coveralls.io/repos/github/OpenSRP/android-p2p-sync/badge.svg?branch=master)](https://coveralls.io/github/OpenSRP/android-p2p-sync?branch=master)


This library wraps on the Google Nearby Connections API to provide a simple UI and interfaces to be used to easily share records between host applications

## Table of Contents

 1. [Getting started](#1-getting-started)
     - [ReceiverDao](#receiverdao)
     - [SenderDao](#senderdao)
     - [AuthorizationService](#authorizationservice)
     - [How to start the peer-to-peer screen/activity](#how-to-start-the-peer-to-peer-screenactivity)
     - [How to communicate delay in records processing](#how-to-communicate-delay-in-records-processing)
 2. [More Information](#2-more-information)

## 1. Getting started

Add the module to your project as follows

 1. Add the repository to your project-root `build.gradle`
```groovy
allprojects {
    repositories {
        ...

        maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
        maven { url "https://s3.amazonaws.com/repo.commonsware.com" }
    }
}
```


```groovy

dependencies {

    ...

    implementation 'org.smartregister:android-p2p-sync:0.3.6-SNAPSHOT'
}
```


Initialise the library in the `onCreate` method of your `Application` class

```java

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ...

        P2PLibrary.init(new P2PLibrary.Options(this
                        , "db_password_here"
                        , "John Doe"
                        , new MyP2PAuthorizationService()
                        , new MyReceiverDao()
                        , new MySenderDao()));
    }
}

```

where you should have implemented your own:
 - `ReceiverDao` from `org.smartregister.p2p.sample.dao.ReceiverTransferDao`
 - `SenderDao` from the interface `org.smartregister.p2p.sample.dao.SenderTransferDao`
 - `AuthorizationService` from the interface `org.smartregister.p2p.authorizer.P2PAuthorizationService`

### ReceiverDao

This data access object is supposed to implement methods that receive and process any data that is shared. After processing the data, the host application should return the last record id so that this can be saved and used as the last sync point during the next sync with the same device.

### SenderDao

This provides data that is to be sent/shared. It implements methods that provide access to records from the given `lastRecordId`(not inclusive) and should return data with a max of the `batchSize` specified. The id that the host application provides here should be unique and cater for record updates. A simple example would be to use the default SQLite `rowid`

### AuthorizationService

This class provides the logic for performing authorization of the peer device. In case you want the peer app connecting to be of a certain app version, logged in by a certain role OR have access to specific information. It enables you to limit what kind of device can connect to or sync with.

The interface from which this is implemented provides two methods:

 - **`void getAuthorizationDetails(@NonNull OnAuthorizationDetailsProvidedCallback onAuthorizationDetailsProvidedCallback);`**

 This method implements providing the authorization details in the callback. This method is called on the UI Thread and therefore any long-running operations, DB operations or network operations should be performed on a separate thread and the callback should be called on the UI Thread.



 - **`void authorizeConnection(@NonNull Map<String, Object> authorizationDetails, @NonNull AuthorizationCallback authorizationCallback);`**

 This method is where you add your authorization logic for checking the conditions. A peer-device status is injected into the `authorizationDetails` as a constant `org.smartregister.p2p.util.Constants.AuthorizationKeys.PEER_STATUS` which can be any of two values `sender` or `receiver`. It's more advisable to use the provided constants `Constants.PeerStatus.SENDER` and `Constants.PeerStatus.SENDER` to know the state of the peer device that you are connecting to. The reason for this is that the `AuthorizationService` has no way to know it's current state or the other peer devices state(Is it a sender or a receiver?)


### How to start the peer-to-peer screen/activity
To start the sending and receiving activity:

```java

    ...
    startActivity(new Intent(this, P2pModeSelectActivity.class));
```


### How to communicate delay in records processing

This is disabled by default. When enabled, it shows up as text below the **X records received** text when the transfer is successful. This text only shows on the receiving device when the transfer completed successfully. It will therefore not show if records were transferred but the transfer failed.

To enable this text, override the `processing_disclaimer` string resource in your project like below.

```xml
<resources>

   ...
   <string name="processing_disclaimer">Records will be processed for up to 10 minutes before your records will be updated. Do not edit medical records until the process is complete.</string>
</resources>

```


## 2. More Information

You can get more general information about the library [here](https://smartregister.atlassian.net/wiki/spaces/Documentation/pages/1139212418/Android+Peer-to-peer+sync+library?atlOrigin=eyJpIjoiYWE5NmM1ZTk3MGQ2NGU4OWE0ZTdmM2U2YTFjODg2YTAiLCJwIjoiYyJ9)
