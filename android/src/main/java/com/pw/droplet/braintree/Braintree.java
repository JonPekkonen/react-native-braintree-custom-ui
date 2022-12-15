package com.pw.droplet.braintree;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.BraintreeError;
import com.braintreepayments.api.BraintreeRequestCodes;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.CardClient;
import com.braintreepayments.api.CardNonce;
import com.braintreepayments.api.CardTokenizeCallback;
import com.braintreepayments.api.Configuration;
import com.braintreepayments.api.ConfigurationCallback;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.DataCollectorCallback;
import com.braintreepayments.api.ErrorWithResponse;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PayPalBrowserSwitchResultCallback;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalFlowStartedCallback;
import com.braintreepayments.api.PayPalPaymentIntent;
import com.braintreepayments.api.PayPalRequest;
import com.braintreepayments.api.PayPalVaultRequest;
import com.braintreepayments.api.PostalAddress;
import com.braintreepayments.api.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.ThreeDSecureClient;
import com.braintreepayments.api.ThreeDSecureLookup;
import com.braintreepayments.api.ThreeDSecurePostalAddress;
import com.braintreepayments.api.ThreeDSecureRequest;
import com.braintreepayments.api.ThreeDSecureResult;
import com.braintreepayments.api.UserCanceledException;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.gson.Gson;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Braintree
  extends ReactContextBaseJavaModule
  implements ActivityEventListener, LifecycleEventListener {

  private static final int PAYMENT_REQUEST = 1706816330;
  private String token;
  private boolean mThreeDSecureRequested;
  private String mDeviceData;
  private final Context mContext;

  private Callback successCallback;
  private Callback errorCallback;

  private Context mActivityContext;
  private FragmentActivity mCurrentActivity;

  private BraintreeClient mBraintreeClient;
  private DataCollector mDataCollector;
  private PayPalClient mPayPalClient;
  private ThreeDSecureClient mThreeDSecureClient;

  public Braintree(ReactApplicationContext reactContext) {
    super(reactContext);
    mContext = reactContext;
    reactContext.addLifecycleEventListener(this);
    reactContext.addActivityEventListener(this);
  }

  @Override
  @Nonnull
  public String getName() {
    return "Braintree";
  }

  public String getToken() {
    return this.token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  @Override
  public void onActivityResult(
    Activity activity,
    int requestCode,
    int resultCode,
    Intent intent
  ) {
    switch (requestCode) {
      // case BraintreeRequestCodes.GOOGLE_PAY:
      //     if (mGooglePayClient != null) {
      //         mGooglePayClient.onActivityResult(
      //                 resultCode,
      //                 intent,
      //                 this::handleGooglePayResult
      //         );
      //     }
      //     break;
      case BraintreeRequestCodes.THREE_D_SECURE:
        if (mThreeDSecureClient != null) {
          mThreeDSecureClient.onActivityResult(
            resultCode,
            intent,
            this::handleThreeDSecureResult
          );
        }
        break;
    }
  }

  @Override
  public void onHostResume() {
    if (mBraintreeClient != null && mCurrentActivity != null) {
      BrowserSwitchResult browserSwitchResult = mBraintreeClient.deliverBrowserSwitchResult(
        mCurrentActivity
      );
      if (browserSwitchResult != null) {
        switch (browserSwitchResult.getRequestCode()) {
          // case BraintreeRequestCodes.PAYPAL:
          //   if (mPayPalClient != null) {
          //     mPayPalClient.onBrowserSwitchResult(
          //       browserSwitchResult,
          //       this::handlePayPalResult
          //     );
          //   }
          //   break;
          case BraintreeRequestCodes.THREE_D_SECURE:
            if (mThreeDSecureClient != null) {
              mThreeDSecureClient.onBrowserSwitchResult(
                browserSwitchResult,
                this::handleThreeDSecureResult
              );
            }
            break;
        }
      }
    }
  }

  @Override
  public void onHostPause() {
    //NOTE: empty implementation
  }

  @Override
  public void onHostDestroy() {
    //NOTE: empty implementation
  }

  @ReactMethod
  public void setupWithToken(
    final String token,
    final Callback successCallback,
    final Callback errorCallback
  ) {
    try {
      this.token = token;
      this.mCurrentActivity = (FragmentActivity) getCurrentActivity();
      this.mBraintreeClient = new BraintreeClient(mContext, this.token);

      this.mDataCollector = new DataCollector(this.mBraintreeClient);
      this.mDataCollector.collectDeviceData(
          mContext,
          (result, e) -> mDeviceData = result
        );
      this.mBraintreeClient.getConfiguration(
          new ConfigurationCallback() {
            @Override
            public void onResult(
              @androidx.annotation.Nullable Configuration configuration,
              @androidx.annotation.Nullable Exception error
            ) {
              if (error != null) {
                handleError(error);
              } else {
                successCallback.invoke();
              }
            }
          }
        );
    } catch (Exception e) {
      errorCallback.invoke(e.getMessage());
    }
  }

  @ReactMethod
  public void getCardNonce(
    final ReadableMap parameters,
    final Callback successCallback,
    final Callback errorCallback
  ) {
    mThreeDSecureRequested = false;
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;

    Card card = new Card();
    card.setShouldValidate(true);
    if (parameters.hasKey("number")) {
      card.setNumber(parameters.getString("number"));
    }

    if (parameters.hasKey("cvv")) {
      card.setCvv(parameters.getString("cvv"));
    }

    if (parameters.hasKey("expirationDate")) {
      card.setExpirationDate(parameters.getString("expirationDate"));
    } else {
      if (parameters.hasKey("expirationMonth")) {
        card.setExpirationMonth((parameters.getString("expirationMonth")));
      }

      if (parameters.hasKey("expirationYear")) {
        card.setExpirationYear(parameters.getString("expirationYear"));
      }
    }

    if (parameters.hasKey("cardholderName")) {
      card.setCardholderName(parameters.getString("cardholderName"));
    }

    if (parameters.hasKey("firstName")) {
      card.setFirstName(parameters.getString("firstName"));
    }

    if (parameters.hasKey("lastName")) {
      card.setLastName(parameters.getString("lastName"));
    }

    if (parameters.hasKey("countryCode")) {
      card.setCountryCode(parameters.getString("countryCode"));
    }

    if (parameters.hasKey("locality")) {
      card.setLocality(parameters.getString("locality"));
    }

    if (parameters.hasKey("postalCode")) {
      card.setPostalCode(parameters.getString("postalCode"));
    }

    if (parameters.hasKey("region")) {
      card.setRegion(parameters.getString("region"));
    }

    if (parameters.hasKey("streetAddress")) {
      card.setStreetAddress(parameters.getString("streetAddress"));
    }

    if (parameters.hasKey("extendedAddress")) {
      card.setExtendedAddress(parameters.getString("extendedAddress"));
    }

    CardClient cardClient = new CardClient(this.mBraintreeClient);
    cardClient.tokenize(
      card,
      new CardTokenizeCallback() {
        @Override
        public void onResult(
          @androidx.annotation.Nullable CardNonce cardNonce,
          @androidx.annotation.Nullable Exception error
        ) {
          if (error != null) {
            handleError(error);
          }
          if (cardNonce != null) {
            successCallback.invoke(cardNonce.getString());
          }
        }
      }
    );
  }

  @ReactMethod
  public void check3DSecure(
    final ReadableMap parameters,
    final Callback successCallback,
    final Callback errorCallback
  ) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;
    mThreeDSecureRequested = true;

    ThreeDSecurePostalAddress address = new ThreeDSecurePostalAddress();

    if (parameters.hasKey("firstname")) address.setGivenName(
      parameters.getString("firstname")
    );

    if (parameters.hasKey("lastname")) address.setSurname(
      parameters.getString("lastname")
    );

    if (parameters.hasKey("phone")) address.setPhoneNumber(
      parameters.getString("phone")
    );

    if (parameters.hasKey("locality")) address.setLocality(
      parameters.getString("locality")
    );

    if (parameters.hasKey("postalCode")) address.setPostalCode(
      parameters.getString("postalCode")
    );

    if (parameters.hasKey("region")) address.setRegion(
      parameters.getString("region")
    );

    if (parameters.hasKey("streetAddress")) address.setStreetAddress(
      parameters.getString("streetAddress")
    );

    if (parameters.hasKey("extendedAddress")) address.setExtendedAddress(
      parameters.getString("extendedAddress")
    );

    if (this.mCurrentActivity != null) {
      this.mThreeDSecureClient = new ThreeDSecureClient(this.mBraintreeClient);

      // For best results, provide as many additional elements as possible.
      ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation();
      additionalInformation.setShippingAddress(address);

      final ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
      threeDSecureRequest.setNonce(parameters.getString("nonce"));
      threeDSecureRequest.setEmail(parameters.getString("email"));
      threeDSecureRequest.setBillingAddress(address);
      threeDSecureRequest.setVersionRequested(ThreeDSecureRequest.VERSION_2);
      threeDSecureRequest.setAdditionalInformation(additionalInformation);
      threeDSecureRequest.setAmount(parameters.getString("amount"));

      this.mThreeDSecureClient.performVerification(
          this.mCurrentActivity,
          threeDSecureRequest,
          (threeDSecureResult, error) -> {
            if (error != null) {
              handleError(error);
              return;
            }
            if (threeDSecureResult != null) {
              this.mThreeDSecureClient.continuePerformVerification(
                  this.mCurrentActivity,
                  threeDSecureRequest,
                  threeDSecureResult,
                  this::handleThreeDSecureResult
                );
            }
          }
        );
    }
  }

  private void handleThreeDSecureResult(
    ThreeDSecureResult threeDSecureResult,
    Exception error
  ) {
    if (error != null) {
      handleError(error);
      return;
    }
    if (
      threeDSecureResult != null &&
      threeDSecureResult.getTokenizedCard() != null
    ) {
      nonceCallback(threeDSecureResult.getTokenizedCard().getString());
    }
  }

  private void handleError(Exception error) {
    if (errorCallback != null) {
      if (error instanceof UserCanceledException) {
        errorCallback.invoke("USER_CANCELLATION", "The user cancelled");
      }
      if (error instanceof ErrorWithResponse) {
        ErrorWithResponse errorWithResponse = (ErrorWithResponse) error;
        BraintreeError cardErrors = errorWithResponse.errorFor("creditCard");
        if (cardErrors != null) {
          Gson gson = new Gson();
          final Map<String, String> errors = new HashMap<>();
          BraintreeError numberError = cardErrors.errorFor("number");
          BraintreeError cvvError = cardErrors.errorFor("cvv");
          BraintreeError expirationDateError = cardErrors.errorFor(
            "expirationDate"
          );
          BraintreeError postalCode = cardErrors.errorFor("postalCode");

          if (numberError != null) {
            errors.put("card_number", numberError.getMessage());
          }

          if (cvvError != null) {
            errors.put("cvv", cvvError.getMessage());
          }

          if (expirationDateError != null) {
            errors.put("expiration_date", expirationDateError.getMessage());
          }

          // TODO add more fields
          if (postalCode != null) {
            errors.put("postal_code", postalCode.getMessage());
          }

          nonceErrorCallback(gson.toJson(errors));
        } else {
          nonceErrorCallback(errorWithResponse.getErrorResponse());
        }
      }
    }
  }

  public void nonceCallback(String nonce) {
    this.successCallback.invoke(nonce);
  }

  public void nonceErrorCallback(String error) {
    this.errorCallback.invoke(error);
  }

  // @ReactMethod
  // public void paypalRequest(
  //   final String amount,
  //   final Callback successCallback,
  //   final Callback errorCallback
  // ) {
  //   this.successCallback = successCallback;
  //   this.errorCallback = errorCallback;
  //   PayPalRequest request = new PayPalRequest(amount)
  //     .currencyCode("EUR")
  //     .intent(PayPalRequest.INTENT_AUTHORIZE);
  //   PayPal.requestOneTimePayment(this.mPayPalClient, request);
  // }

  public void onNewIntent(Intent intent) {}
}
