package pl.freki.reactnativesmsandroid;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.UUID;

public class ReactNativeSmsAndroidModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;
    private String MESSAGE_SENT = "MESSAGE_SENT";
    private String MESSAGE_DELIVERED = "MESSAGE_DELIVERED";
    private String MESSAGE_SEND_FAILURE = "MESSAGE_SEND_FAILURE";

    public ReactNativeSmsAndroidModule(ReactApplicationContext reactContext) {
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

    @ReactMethod
    public void send(String phoneNumber, String message, final String messageId, final Promise promise) {
        try {
            String intentId = UUID.randomUUID().toString();
            PendingIntent messageSentIntent = PendingIntent.getBroadcast(reactContext, 0, new Intent(intentId + MESSAGE_SENT), 0);
            SmsManager smsManager = SmsManager.getDefault();

            reactContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (getResultCode() == Activity.RESULT_OK) {
                        promise.resolve(MESSAGE_SENT);
                    } else {
                        promise.reject(new Error(MESSAGE_SEND_FAILURE));
                    }
                }
            }, new IntentFilter(intentId + MESSAGE_SENT));

            if (messageId == null) {
                smsManager.sendTextMessage(phoneNumber, null, message, messageSentIntent, null);

                return;
            }

            PendingIntent messageDeliveredIntent = PendingIntent.getBroadcast(reactContext, 0, new Intent(intentId + MESSAGE_DELIVERED), 0);

            reactContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    sendDeliveryEvent(messageId);
                }
            }, new IntentFilter(intentId + MESSAGE_DELIVERED));

            smsManager.sendTextMessage(phoneNumber, null, message, messageSentIntent, messageDeliveredIntent);
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void send(String phoneNumber, String message, final Promise promise) {
        send(phoneNumber, message, null, promise);
    }
}
