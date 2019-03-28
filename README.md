# Android P2P Sync [![Build Status](https://travis-ci.org/OpenSRP/android-p2p-sync.svg?branch=master)](https://travis-ci.org/OpenSRP/android-p2p-sync) [![Coverage Status](https://coveralls.io/repos/github/OpenSRP/android-p2p-sync/badge.svg?branch=master)](https://coveralls.io/github/OpenSRP/android-p2p-sync?branch=master)


This library wraps on the Google Nearby Connections API to provide a simple UI and interfaces to be used to easily share records between host applications

## Table of Contents

1. [Getting started](#getting-started)
2. [Not supported!](#not-supported-(errors-you-might-encounter))

## Getting started

Add the module to your project(Publishing is not yet supported :worried:


Initialise the library in the `onCreate` method of your `Application` class

```java

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ...
        
        P2PLibrary.init(new P2PLibrary.ReceiverOptions("John Doe"));
    }
}

```


To start the sending and receiving activity:

```java

    ...
    startActivity(new Intent(this, P2pModeSelectActivity.class));
```




### NOT SUPPORTED (ERRORS YOU MIGHT ENCOUNTER)

1. A StackOverflow error or JsonParsingException error

- These errors will happen if you provide objects which are not easily parseable by GSON such as **Realm** objects

