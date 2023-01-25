# Fcm push service

## Installation
````kotlin
// Append repository
repositories {
    mavenCentral()
}

// Append dependency
implementation("com.icerockdev.service:fcm-push-service:1.0.0")
````

## Koin configure

### include dependency

````kotlin

val fcmScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
single {
    PushService(
        coroutineScope = fcmScope,
        config = FCMConfig(
            serverKey = appConf.getString("fcm.serverKey")
        ),
        pushRepository = object : IPushRepository {
            override fun deleteByTokenList(tokenList: List<String>): Int {
                // remove wrong tokens here
                return 0
            }
        }
    )
}

````

````kotlin
class Container : KoinComponent {
    val pushService: PushService by inject()
}
````

````
// application.conf
fcm {
    serverKey = ${FCM_SERVER_KEY}
}
````

## Send data example
````kotlin
pushService.sendAsync(
    payLoad = FCMPayLoad(
        dataObject = data,
        notificationObject = notification,
        tokenList = tokens
    )
)
````

## Contributing
All development (both new features and bug fixes) is performed in the `develop` branch. This way `master` always contains the sources of the most recently released version. Please send PRs with bug fixes to the `develop` branch. Documentation fixes in the markdown files are an exception to this rule. They are updated directly in `master`.

The `develop` branch is pushed to `master` on release.

For more details on contributing please see the [contributing guide](CONTRIBUTING.md).

## License
        
    Copyright 2019 IceRock MAG Inc.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
