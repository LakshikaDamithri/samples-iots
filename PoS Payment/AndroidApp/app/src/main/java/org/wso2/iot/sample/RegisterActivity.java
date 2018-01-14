/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.iot.sample;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.wso2.iot.sample.constants.DeviceConstants;
import org.wso2.iot.sample.util.APIClient;
import org.wso2.iot.sample.util.LocalRegistry;
import org.wso2.iot.sample.util.dto.RegisterInfo;

import java.net.MalformedURLException;
import java.net.URL;

public class RegisterActivity extends Activity {

    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mHostView;
    private View mProgressView;
    private View mLoginFormView;
    private Button deviceRegisterButton;
    private Handler mUiHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (LocalRegistry.isExist(getApplicationContext())) {
            Intent intent = new Intent(getApplicationContext(), SelectItemsActivity.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.activity_register);
        mUsernameView = findViewById(R.id.username);
        mPasswordView = findViewById(R.id.password);
        mHostView = findViewById(R.id.hostname);

        deviceRegisterButton = findViewById(R.id.device_register_button);

        deviceRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    public void attemptLogin() {
        showProgress(true);
        deviceRegisterButton.setVisibility(View.INVISIBLE);
        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String username = mUsernameView.getText().toString();
        final String password = mPasswordView.getText().toString();
        final String hostname = mHostView.getText().toString();
        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password)) {
            // mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            //cancel = true;
        }
        // Check for a valid username .
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }
        if (TextUtils.isEmpty(hostname)) {
            mHostView.setError(getString(R.string.error_field_required));
            focusView = mHostView;
            cancel = true;
        }

        URL hostUrl = null;
        try {
            hostUrl = new URL(hostname);
        } catch (MalformedURLException e) {
            mHostView.setError(e.getMessage());
            focusView = mHostView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            final URL serverUrl = hostUrl;
            Thread myThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    APIClient client = new APIClient(getApplicationContext());
                    LocalRegistry.addServerURL(getBaseContext(), hostname);
                    String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                    final RegisterInfo registerStatus = client.register(username, password, deviceId);
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), registerStatus.getMsg(), Toast.LENGTH_LONG).show();
                        }
                    });

                    if (registerStatus.isRegistered()) {
                        LocalRegistry.addUsername(getApplicationContext(), username);
                        LocalRegistry.addDeviceId(getApplicationContext(), deviceId);
                        LocalRegistry.addMqttEndpoint(getApplicationContext(),
                                "tcp://" + serverUrl.getHost() + ":" + DeviceConstants.MQTT_PORT);

                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(getApplicationContext(), SelectItemsActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });

                    }
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            deviceRegisterButton.setVisibility(View.VISIBLE);
                            showProgress(false);
                        }
                    });

                }
            });
            myThread.start();

        }
    }

    public void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

}
