package pl.freki.reactnativesmsandroid;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.UUID;

class ReactNativeSmsAndroidModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;
    private String MESSAGE_SENT = "MESSAGE_SENT";
    private String MESSAGE_DELIVERED = "MESSAGE_DELIVERED";
    private String MESSAGE_SEND_FAILURE = "MESSAGE_SEND_FAILURE";
    private String PARTS_CONFIRMED = ReactNativeSmsAndroidModule.class.getPackage().toString() + ".PartsConfirmed";

    ReactNativeSmsAndroidModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "SmsAndroid";
    }

    private void sendDeliveryEvent(String messageId) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(MESSAGE_DELIVERED, messageId);
    }

    private void sendWithoutMessageId(String phoneNumber, String message, final Promise promise) {
        if (phoneNumber == null || phoneNumber.isEmpty() || message == null || message.isEmpty()) {
            promise.reject(new Error("phoneNumber, message and messageId must not be empty"));

            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> messageParts = smsManager.divideMessage(message);
            final Integer messagePartsSize = messageParts.size();
            String intentId = UUID.randomUUID().toString();
            final PendingIntent messageSentIntent = PendingIntent.getBroadcast(reactContext, 0, new Intent(intentId + MESSAGE_SENT), 0);
            ArrayList<PendingIntent> messageSentIntents = new ArrayList<>(messagePartsSize);

            for (int i = 0; i < messagePartsSize; i++) {
                messageSentIntents.add(messageSentIntent);
            }

            reactContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (getResultCode() == Activity.RESULT_OK) {
                        Integer partsConfirmed = intent.getIntExtra(PARTS_CONFIRMED, 0);

                        partsConfirmed++;

                        if (partsConfirmed.equals(messagePartsSize)) {
                            promise.resolve(MESSAGE_SENT);
                            messageSentIntent.cancel();

                            return;
                        }

                        intent.putExtra(PARTS_CONFIRMED, partsConfirmed);
                        PendingIntent.getBroadcast(reactContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    } else {
                        PendingIntent.getBroadcast(reactContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT).cancel();
                        promise.reject(new Error(MESSAGE_SEND_FAILURE));
                    }
                }
            }, new IntentFilter(intentId + MESSAGE_SENT));

            smsManager.sendMultipartTextMessage(phoneNumber, null, messageParts, messageSentIntents, null);
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void send(String phoneNumber, String message, final String messageId, final Promise promise) {
        if (messageId == null || messageId.isEmpty()) {
            sendWithoutMessageId(phoneNumber, message, promise);

            return;
        }

        if (phoneNumber == null || phoneNumber.isEmpty() || message == null || message.isEmpty()) {
            promise.reject(new Error("phoneNumber, message and messageId must not be empty"));

            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> messageParts = smsManager.divideMessage(message);
            final Integer messagePartsSize = messageParts.size();
            String intentId = UUID.randomUUID().toString();
            final PendingIntent messageSentIntent = PendingIntent.getBroadcast(reactContext, 0, new Intent(intentId + MESSAGE_SENT), 0);
            final PendingIntent messageDeliveredIntent = PendingIntent.getBroadcast(reactContext, 0, new Intent(intentId + MESSAGE_DELIVERED), 0);
            ArrayList<PendingIntent> messageSentIntents = new ArrayList<>(messagePartsSize);
            ArrayList<PendingIntent> messageDeliveredIntents = new ArrayList<>(messagePartsSize);

            for (int i = 0; i < messagePartsSize; i++) {
                messageSentIntents.add(messageSentIntent);
                messageDeliveredIntents.add(messageDeliveredIntent);
            }

            reactContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (getResultCode() == Activity.RESULT_OK) {
                        Integer partsConfirmed = intent.getIntExtra(PARTS_CONFIRMED, 0);

                        partsConfirmed++;

                        if (partsConfirmed.equals(messagePartsSize)) {
                            promise.resolve(MESSAGE_SENT);
                            messageSentIntent.cancel();

                            return;
                        }

                        intent.putExtra(PARTS_CONFIRMED, partsConfirmed);
                        PendingIntent.getBroadcast(reactContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    } else {
                        PendingIntent.getBroadcast(reactContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT).cancel();
                        promise.reject(new Error(MESSAGE_SEND_FAILURE));
                    }
                }
            }, new IntentFilter(intentId + MESSAGE_SENT));

            reactContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Integer partsConfirmed = intent.getIntExtra(PARTS_CONFIRMED, 0);

                    partsConfirmed++;

                    if (partsConfirmed.equals(messagePartsSize)) {
                        sendDeliveryEvent(messageId);
                        PendingIntent.getBroadcast(reactContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT).cancel();

                        return;
                    }

                    intent.putExtra(PARTS_CONFIRMED, partsConfirmed);
                    PendingIntent.getBroadcast(reactContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                }
            }, new IntentFilter(intentId + MESSAGE_DELIVERED));

            smsManager.sendMultipartTextMessage(phoneNumber, null, messageParts, messageSentIntents, messageDeliveredIntents);
        } catch (Exception e) {
            promise.reject(e);
        }
    }
}
