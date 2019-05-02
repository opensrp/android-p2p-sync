# Android P2P Sync [![Build Status](https://travis-ci.org/OpenSRP/android-p2p-sync.svg?branch=master)](https://travis-ci.org/OpenSRP/android-p2p-sync) [![Coverage Status](https://coveralls.io/repos/github/OpenSRP/android-p2p-sync/badge.svg?branch=master)](https://coveralls.io/github/OpenSRP/android-p2p-sync?branch=master)


This library wraps on the Google Nearby Connections API to provide a simple UI and interfaces to be used to easily share records between host applications

## Table of Contents

1. [Getting started](#getting-started)
2. [Not supported!](#not-supported-(errors-you-might-encounter))

## Getting started

Add the module to your project as follows

1. Add the repository to your project-root `build.gradle`

```groovy
allprojects {
    repositories {
        ...
        
        maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
    }
}
```


```groovy

dependencies {

    ...

    implementation ('org.smartregister:android-p2p-sync:0.1.0-SNAPSHOT'
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
                        , this
                        , new MyReceiverDao()
                        , new MySenderDao()));
    }
}

```

where you should have implemented your own `ReceiverDao` from `org.smartregister.p2p.sample.dao.ReceiverTransferDao` and you should have implemented your own `SenderDao` from the interface `org.smartregister.p2p.sample.dao.SenderTransferDao`

### ReceiverDao

This data access object is supposed to implement methods that receive and process any data that is shared. After processing the data, the host application should return the last record id so that this can be saved and used as the last sync point during the next sync with the same device.


### SenderDao

This provides data that is to be sent/shared. It implements methods that provide access to records from the given `lastRecordId`(not inclusive) and should return data with a max of the `batchSize` specified. The id that the host application provides here should be unique and cater for record updates. A simple example would be to use the default SQLite `rowid`


To start the sending and receiving activity:

```java

    ...
    startActivity(new Intent(this, P2pModeSelectActivity.class));
```




### NOT SUPPORTED (ERRORS YOU MIGHT ENCOUNTER)

1. A StackOverflow error or JsonParsingException error

- These errors will happen if you provide objects which are not easily parseable by GSON such as **Realm** objects

