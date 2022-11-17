# Android_AdNetworks_PrioritiesManager_with_RemoteControl
  * ***How to control a multiple standalone ad networks in one place?***
  * ***Maybe you need to use multiple mediation SDKs? How to set network priority?***
  * ***You can mix mediated SDKs with standalone SDKs or with another mediated SDKs.***
  * ***Enable/disable ad network remotely. Configure display priorities.***
  * ***Target countries for each ad network.***
  * ***Have you been looking for a well documented sample of UnityAds & UnityMediation for Android native?***
  * *My library not a mediation. Without control of mediated ads price.*

Implemented ad types: interstitials, rewarded.
Ad networks: UnityAds (mediated or standalone), UnityMediation, SuperAwesome, Kidoz, IronSource.
Written in **Kotlin**. No UI sample. Android library.

**My library will organize your common use cases for each ad network implementation that you will add.**
## Use cases:
  1. Handle which ad network to enable/disable via a Firebase remote config.
  2. Define countries for each ad network. Currently separating LATAM region only.
  3. Define min/max Android version for each ad network.
  4. Processing data privacy: GDPR, CCPA, LGPD, ...
  5. Processing some user & device data for better targeting: app language, country, age, gender, phone/tablet..

[![](https://jitpack.io/v/Almaren/Android_AdNetworks_PrioritiesManager_with_RemoteControl.svg)](https://jitpack.io/#Almaren/Android_AdNetworks_PrioritiesManager_with_RemoteControl)

## Preparing:
* Add to your project build.gradle:
  ```
  allprojects {
    repositories {
      ...
      maven { url 'https://jitpack.io' }
    }
  }
  ```
  to the app build.gradle:
  ```
  dependencies {
        implementation 'com.github.Almaren:Android_AdNetworks_PrioritiesManager_with_RemoteControl:1.0.2'
  }
  ```
* Add to your /android/../AndroidManifest.xml in your <activity> where the ads will be used:
  ```
  <activity
     ...
     android:configChanges="keyboard|keyboardHidden|orientation|screenLayout|uiMode|screenSize|smallestScreenSize|mcc|mnc"
  />
  ```
  The necessary activities and overrides of ad networks are stored in my lib/AndroidManifest.xml.
  For better targeting some networks use coarse location, check if it required for you and complies with a Google policies, if so
  uncomment that line in the manifest.

## How to use:
**All methods of AdManager must be called from the main ui thread.**
1. Set app ID for each network in AdManager: APP_ID_YOUR_NET_AD.
   Set ad unit id for each network where marked "todo set your id".
   Check local.properties. 
2. Implement interface lib.ads.event.AdUserDeviceDataBinder, in my case implemented by MainActivity.
3. First init: call AdManager.initAds() in your Activity.create(..). 
   In my case calling it after age gate and consent dialog. At the second app launching I call it in a main page with already stored age and consent state.  
4. Implement your own logic in AdManager.getConsentUserData().
   Implement age gate and consent dialog. Detect if user consent given for GDPR/CCPA/LGPD/PIPL.
5. Enable/Disable test mode flags (marked as todo) in AdManager.createAdSources().
6. Whenever your got a user ads data & consent state -> call AdManager.setPrivacyMetaData() to update ad networks with a privacy/consent data.
7. Define default ad sources and their priority in AdSourcePriorityHandler.init().
   Define adSourceForLatam, or comment the content of method updatePrioritiesRegardingToUserRegion() if it's not relevant to you.
8. To load and display ad unit type just call a suitable methods from the AdManager. 
   Call dispose() on YourActivity.onDestroy().
9. Not mandatory: implement performing actions on each ad state in AdUnitBaseHandler.

**Establishing Firebase remote config. (not mandatory)** *You can use the lib without firebase remote.*
If your firebase project is configured skip to step #4.
1. Create project in Firebase if not exist yet.
2. Configure a project in Firebase Settings -> SDK setup and configuration.   
   add release and debug SHA keys.
3. Download google-services.json from a project settings and place to /YourProject/android folder.

4. Navigate in Firebase project to Remote config. Add parameter "adSourcesPriority" with the following value:
   ```
   {"IronSrc":[-1,-1], "AdmobMediation":[3,3], "UnityAdsMediation":[2,2],"UnityAds":[1,-1]}
   ```
   where array values indicates: [interstitial, rewarded], 
   -1 = disabled, 1+ - priorities, the highest is more prioritized to display first.
   
5. Fetch once the remote config from the server whenever required by creating RemoteConfigCoordinator object, passing
   AdManager and just calling RemoteConfigCoordinator.fetchAllData().
   

____________________________________________________________________________________________________
**NOTICE:** © On modifying and expanding lib be sure to attribute the link in your code headers: 
  https://github.com/Almaren
  Thank You!
