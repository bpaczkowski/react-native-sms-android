## React Native Sms Android

React Native module for Android (4.1+) used for sending text messages. Supports multipart messages (longer than 160-chars) and optional delivery receipts.

### Installation

YARN is preferred for installation as React Native does not seem to work well with the latest version of NPM:

```
yarn add git@github.com:digimonkeys/modules.git#react-native-sms-android
```

### Usage

```javascript
import SmsAndroid from 'react-native-sms-android';
// only needed for delivery receipts
import { DeviceEventEmitter } from 'react-native';

// set up delivery receipt listener, messageId here is the same as when sending the message
// beware that the delivery event is not guranteed to fire, it might not be supported in some networks/devices
DeviceEventEmitter.addListener('MESSAGE_DELIVERED', messageId => console.log(messageId));

// send message, all arguments are strings
// messageId is only required if you need delivery receipts
SmsAndroid
  .send(phoneNumber, message, messageId)
  .then(() => console.log('Successfully sent message'))
  .catch(err => console.log(err));
```
