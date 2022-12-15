# react-native-braintree-custom-ui
[![npm version](https://badge.fury.io/js/react-native-braintree-custom-ui.svg)](https://badge.fury.io/js/react-native-braintree-custom-ui)

An effort to update https://github.com/kraffslol/react-native-braintree-xplat.
Use Braintree Android SDK V4 & IOS V4, with 3DS 2.

Required RN 0.60+ for auto linking on iOS.

TODO : 
- Currency as a parameter

## Installation

Run `yarn install react-native-braintree-custom-ui`

## Usage

### Setup

This plugin uses only Tokenization Key for initialisation (https://developers.braintreepayments.com/guides/authorization/tokenization-key/android/v2)
Put your serverUrl where the plugin will be able to make a GET request and get the token from your server

```js
var BTClient = require('react-native-braintree-custom-ui');
BTClient.setup(<serverUrl>,'your.bundle.id.payments');
```


You will need the CardinalMobile.framework for 3DS 2 (https://developers.braintreepayments.com/guides/3d-secure/client-side/ios/v4). You can easily download it with that command

```
curl -L -ubraintree-team-sdk@cardinalcommerce:220cc9476025679c4e5c843666c27d97cfb0f951  "https://cardinalcommerce.bintray.com/ios/2.1.4-2/cardinalmobilesdk.zip" -o cardinalmobile2.1.4-2.zip
```

For Android you will need to add this to you build.gradle 

```
repositories {
    maven {
        url "https://cardinalcommerce.bintray.com/android"
        credentials {
            username 'braintree-team-sdk@cardinalcommerce'
            password '220cc9476025679c4e5c843666c27d97cfb0f951'
        }
    }
}
```

AndroidManifest.xml add:
```
<manifest ... >
  ...
  <application ...>
    ...
    <activity ...> 
      ...
      <intent-filter>
          <action android:name="android.intent.action.VIEW" />
          <category android:name="android.intent.category.DEFAULT" />
          <category android:name="android.intent.category.BROWSABLE" />
          <data android:scheme="${applicationId}.braintree" />
      </intent-filter>
```
## Get Card Nonce
If you only want to tokenize credit card information, you can use the following:

```js
const card = {
  number: "4111111111111111",
  expirationDate: "10/20", // or "10/2020" or any valid date
  cvv: "400",
}

BTClient.getCardNonce(card).then(function(nonce) {
  //payment succeeded, pass nonce to server
})
.catch(function(err) {
  //error handling
});

// Full list of card parameters:
type Card = {
  number: string,
  cvv: string,
  expirationDate: string,
  cardholderName: string,
  firstName: string,
  lastName: string,
  company: string,
  countryName: string,
  countryCodeAlpha2: string,
  countryCodeAlpha3: string,
  countryCodeNumeric: string,
  locality: string,
  postalCode: string,
  region: string,
  streetAddress: string,
  extendedAddress: string,
  amount: number
}
```

## Check 3D Secure for card nonce
If you only want to tokenize credit card information, you can use the following:

```js
const params = {
}

BTClient.check3DSecure(params).then(function(nonce) {
  //payment succeeded, pass nonce to server
})
.catch(function(err) {
  //error handling
});

// Full list of card parameters:
type Params = {
  nonce: string
  firstName: string,
  lastName: string,
  locality: string,
  postalCode: string,
  region: string,
  streetAddress: string,
  extendedAddress: string,
  amount: number
}
```

## PayPal Checkout 

This plugin implements Paypal Checkout https://developers.braintreepayments.com/guides/paypal/checkout-with-paypal/android/v2

You will need to provide an amount to make it works

```js
BTClient.showPayPalViewController(<amount>).then(function({nonce,
        email,
        firstName,
        lastName,
        billingAddress,
        shippingAddress}) {
  //payment succeeded, pass nonce to server
})
.catch(function(err) {
  //error handling
});
```

## One Touch on iOS
To take advantage of [One Touch](https://developers.braintreepayments.com/guides/one-touch/overview/ios/v3), there are additional setup required:

1. Register a URL scheme in Xcode (should always start with YOUR Bundle ID)
[More info here](https://developers.braintreepayments.com/guides/paypal/client-side/ios/v3#register-a-url-type) TL;DR


#### Add CFBundleURLTypes to Info.Plist
```js
	<key>CFBundleURLTypes</key>
	<array>
	<dict>
		<key>CFBundleTypeRole</key>
		<string>Editor</string>
		<key>CFBundleURLName</key>
		<string>your.bundle.id</string>
		<key>CFBundleURLSchemes</key>
		<array>
			<string>your.bundle.id.payments</string>
		</array>
	</dict>
	</array>
```
#### WhiteList
If your app is built using iOS 9 as its Base SDK, then you must add URLs to a whitelist in your app's info.plist
```js
   <key>LSApplicationQueriesSchemes</key>
   <array>
     <string>com.paypal.ppclient.touch.v1</string>
     <string>com.paypal.ppclient.touch.v2</string>
     <string>com.venmo.touch.v2</string>
   </array>
```

2. For iOS: Use setupWithURLScheme instead, passing the url scheme you have registered in previous step


3. Add this delegate method (callback) to your AppDelegate.m
  ```objc
#import "RCTBraintree.h"

- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options {
  return [[RCTBraintree sharedInstance] application:application openURL:url  options:options];
}
  ```
## Credits
Big thanks to [@kraffslol](https://github.com/kraffslol) for the original ios & android modules.
