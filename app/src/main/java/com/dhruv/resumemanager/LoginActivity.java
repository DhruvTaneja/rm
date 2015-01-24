package com.dhruv.resumemanager;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class LoginActivity extends Activity {

    private String username, password, name;
    private boolean DEV_MODE = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //  for removing the title bar/action bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.login);

        final EditText editUsername = (EditText) findViewById(R.id.edit_username);
        final EditText editPassword = (EditText) findViewById(R.id.edit_password);
        final EditText editName = (EditText) findViewById(R.id.edit_name);
        final Button loginButton = (Button) findViewById(R.id.button_login);

        if(DEV_MODE) loginButton.callOnClick();

        editName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE)
                    loginButton.callOnClick();
                return false;
            }
        });

        loginButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                loginButton.setBackgroundResource(R.drawable.button_on_touch);
                return false;
            }
        });
        
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //  getting the values of the edit texts
                username = editUsername.getText().toString();
                password = editPassword.getText().toString();
                name = editName.getText().toString();
                if(DEV_MODE) {
                    username = "2K11/IT/026";
                    password = "saddahaqq101";
                    name = "Dhruv Taneja";
                }
                loginButton.setBackgroundResource(R.drawable.button);
                if(checkConnection() && validateData()) {
                    new AsyncLogin(username, password, name,getApplicationContext(), LoginActivity.this)
                            .execute();
                }
            }
        });
    }

    //  this method returns true if a network connection is available
    private boolean checkConnection() {
        ConnectivityManager connMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMan.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    private boolean validateData() {
        if (!(username.equals("") || password.equals("") || name.equals("")))
            return true;
        else {
            Toast.makeText(LoginActivity.this, "All the fields are mandatory", Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
