package pl.freki.reactnativesms;

import android.telephony.SmsManager;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class ReactNativeSmsAndroidModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;
    private Callback callback = null;

    public ReactNativeSmsAndroidModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "SmsAndroid";
    }

    private void sendCallback(Integer messageId, String message) {
        if (callback != null) {
            callback.invoke(messageId, message);
            callback = null;
        }
    }

    @ReactMethod
    public void send(String phoneNumber, String message, Promise promise) {
        try {
            SmsManager smsManager = SmsManager.getDefault();

            sms.sendTextMessage(phoneNumber, null, message, null, null);

            promise.resolve();
        } catch (Exception e) {
            promise.reject(e);
        }
    }

}
