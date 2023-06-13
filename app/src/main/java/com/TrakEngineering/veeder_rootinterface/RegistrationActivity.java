package com.TrakEngineering.veeder_rootinterface;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.TrakEngineering.veeder_rootinterface.server.ServerHandler;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class RegistrationActivity extends AppCompatActivity {

    private EditText etFName;
    private EditText etMobile;
    private EditText etCompany;
    private AutoCompleteTextView etEmail;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        // ----------------------------------------------------------------------------------------------
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        // ----------------------------------------------------------------------------------------------

        getSupportActionBar().setTitle("New HUB Registration");

        etFName = findViewById(R.id.etFName);
        etMobile = findViewById(R.id.etMobile);
        etCompany = findViewById(R.id.etCompany);
        etEmail = findViewById(R.id.etEmail);
        btnSubmit = findViewById(R.id.btnSubmit);

        TextView tvVersionNum = findViewById(R.id.tvVersionNum);
        tvVersionNum.setText("Version " + CommonUtils.getVersionCode(RegistrationActivity.this));
        CommonUtils.LogMessage("RegistrationAct", "App Version: " + CommonUtils.getVersionCode(RegistrationActivity.this) + " " + AppConstants.getDeviceName() + " Android " + Build.VERSION.RELEASE + " ");

        try {
            TelephonyManager tMgr = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            String mPhoneNumber = tMgr.getLine1Number();
            etMobile.setText(mPhoneNumber);
        } catch (Exception e) {
            CommonUtils.LogMessage("RegistrationAct", "Exception while getting phone number: " + e.getMessage());
            System.out.println(e.getMessage());
        }

        //Pattern US_PHONE_PATTERN = Pattern.compile("^(?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:\\(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])\\s*\\)|([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?([2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+))?$", Pattern.CASE_INSENSITIVE);

        Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$", Pattern.CASE_INSENSITIVE);
        Account[] accounts = AccountManager.get(this).getAccounts();
        Set<String> emailSet = new HashSet<String>();
        for (Account account : accounts) {
            if (EMAIL_PATTERN.matcher(account.name).matches()) {
                emailSet.add(account.name);
            }
        }
        etEmail.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(emailSet)));

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (etFName.getText().toString().trim().isEmpty()) {
                    //redToast(RegistrationActivity.this, "Please enter HUB Name");
                    CommonUtils.LogMessage("RegistrationAct", getResources().getString(R.string.HUBNameRequired));
                    CommonUtils.showMessageDilaog(RegistrationActivity.this, "Error Message", getResources().getString(R.string.HUBNameRequired));
                    etFName.requestFocus();
                }/* else if (etMobile.getText().toString().trim().isEmpty()) {
                    redToast(RegistrationActivity.this, "Please enter Mobile");
                    etMobile.requestFocus();
                } else if (!US_PHONE_PATTERN.matcher(etMobile.getText().toString().trim()).matches()) {

                    redToast(RegistrationActivity.this, "Please enter valid US contact number in\n(xxx)-xxx-xxxx or xxx-xxx-xxxx format.");
                    etMobile.requestFocus();
                } else if (!etMobile.getText().toString().trim().contains("-")) {
                    redToast(RegistrationActivity.this, "Please enter valid US contact number in \n(xxx)-xxx-xxxx or xxx-xxx-xxxx format.");
                    etMobile.requestFocus();

                } else if (etEmail.getText().toString().trim().isEmpty()) {
                    redToast(RegistrationActivity.this, "Please enter Email");
                    etEmail.requestFocus();
                } else if (!isValidEmail(etEmail.getText().toString().trim())) {
                    redToast(RegistrationActivity.this, "Invalid Email");
                    etEmail.requestFocus();
                } else if (etCompany.getText().toString().trim().isEmpty()) {
                    redToast(RegistrationActivity.this, "Please enter Company");
                    etCompany.requestFocus();
                }*/
                else if (!ValidateHUBName(etFName.getText().toString().trim())) {
                    CommonUtils.LogMessage("RegistrationAct", getResources().getString(R.string.HUBNameInvalid));
                    CommonUtils.showMessageDilaog(RegistrationActivity.this, "Error Message", getResources().getString(R.string.HUBNameInvalid));
                    etFName.requestFocus();
                } else {
                    CommonUtils.LogMessage("RegistrationAct", "Entered HUB Name: " + etFName.getText().toString());
                    String hubName = etFName.getText().toString().trim();
                    //------------Collect information for Registration------------------------------
                    //------------------------------------------------------------------------------
                    storeINFO(RegistrationActivity.this, hubName, etMobile.getText().toString().trim(), etEmail.getText().toString().trim(), AppConstants.getOriginalUUID_IMEIFromFile(RegistrationActivity.this));

                    String userMobile = etMobile.getText().toString().trim();
                    String userEmail = etEmail.getText().toString().trim();
                    String userCompany = etCompany.getText().toString().trim();
                    String imeiNumber;

                    if (Build.VERSION.SDK_INT >= 29) {
                        String uUUID = UUID.randomUUID().toString();
                        imeiNumber = uUUID;
                    } else {
                        imeiNumber = AppConstants.getIMEIOnlyForBelowOS10(RegistrationActivity.this);
                    }

                    SplashActivity.writeIMEI_UUIDInFile(RegistrationActivity.this, imeiNumber); ;

                    new RegisterUser(hubName, userMobile, userEmail, imeiNumber, AppConstants.DEVICE_TYPE, userCompany).execute();

                    //------------------------------------------------------------------------------

                }
            }
        });
    }

    private boolean ValidateHUBName(String hubName) {
        boolean isValid = false;
        try {
            String number;
            if (hubName.toUpperCase().startsWith("HUB")) {
                number = hubName.toUpperCase().replace("HUB", "");
                //} else if (hubName.toUpperCase().startsWith("SPARE")) {
                //    number = hubName.toUpperCase().replace("SPARE", "");
            } else {
                number = hubName;
            }
            String regex = "[0-9]+";
            Pattern pattern = Pattern.compile(regex);
            Matcher m = pattern.matcher(number);

            isValid = m.matches();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isValid;
    }

    private static void redToast(Context ctx, String MSg) {
        Toast toast = Toast.makeText(ctx, " " + MSg + " ", Toast.LENGTH_LONG);
        toast.getView().setBackgroundColor(Color.RED);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private boolean isValidEmail(String email) {
        //String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

        Pattern pattern = Pattern.compile(emailPattern);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public void storeINFO(Context context, String name, String mobile, String email, String IMEInum) {
        SharedPreferences pref;

        SharedPreferences.Editor editor;
        pref = context.getSharedPreferences("storeINFO", 0);
        editor = pref.edit();

        // Storing
        editor.putString("name", name);
        editor.putString("mobile", mobile);
        editor.putString("email", email);
        editor.putString("IMEInum", IMEInum);

        editor.commit();

    }

    public void AlertDialogBox(final Context ctx, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {

                        dialog.dismiss();

                        finish();
                    }
                }

        );

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    public class RegisterUser extends AsyncTask<Void, Void, String> {

        private static final String TAG = "RegisterUser :";
        ProgressDialog pd;
        final String userName;
        final String userMobile;
        final String userEmail;
        final String imeiNumber;
        final String deviceType;
        final String userCompany;

        RegisterUser(String userName, String userMobile, String userEmail, String imeiNumber, String deviceType, String userCompany) {
            this.userName = userName;
            this.userMobile = userMobile;
            this.userEmail = userEmail;
            this.imeiNumber = imeiNumber;
            this.deviceType = deviceType;
            this.userCompany = userCompany;
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(RegistrationActivity.this);
            pd.setMessage("Please wait...");
            pd.setCancelable(false);
            pd.show();

        }

        protected String doInBackground(Void... arg0) {
            String resp = "";

            try {

               // String sendData = userName + "#:#" + userMobile + "#:#" + userEmail + "#:#" + imeiNumber + "#:#" + deviceType + "#:#" + userCompany + "#:#" + "AP";
                String sendData = userName + "#:#" + userMobile + "#:#" + "" + "#:#" + imeiNumber + "#:#" + deviceType + "#:#" + "" + "#:#" + "AP";
                CommonUtils.LogMessage(TAG, "Registration details => (" + sendData + ")");
                String AUTH_TOKEN = "Basic " + AppConstants.convertStingToBase64("123:abc:Register");
                ServerHandler serverHandler = new ServerHandler();

                resp = serverHandler.PostTextData(RegistrationActivity.this, AppConstants.webURL, sendData, AUTH_TOKEN);

            } catch (Exception e) {
                Log.d("Ex", e.getMessage());
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            try {
                //CommonUtils.LogMessage(TAG, "RegisterUser :" + result, new Exception("Test for All"));

                JSONObject jsonObj = new JSONObject(result);

                String ResponceMessage = jsonObj.getString(AppConstants.RES_MESSAGE);

                if (ResponceMessage.equalsIgnoreCase("success")) {
                    CommonUtils.SaveUserInPref(RegistrationActivity.this, userName, userMobile, userEmail, "", "", "", "", "", "", "", "", "", "", "","","");
                    CommonUtils.LogMessage(TAG, "Registration successful. Thank you for registering.");
                    AlertDialogBox(RegistrationActivity.this, getResources().getString(R.string.RegistrationSuccess));
                } else if (ResponceMessage.equalsIgnoreCase("fail")) {
                    String ResponseText = jsonObj.getString(AppConstants.RES_TEXT);

                    AppConstants.AlertDialogBox(RegistrationActivity.this, ResponseText);

                } else if (ResponceMessage.equalsIgnoreCase("exists")) {
                    CommonUtils.LogMessage(TAG, getResources().getString(R.string.IMEIAlreadyExist));
                    AlertDialogBox(RegistrationActivity.this, getResources().getString(R.string.IMEIAlreadyExist));
                } else {
                    CommonUtils.LogMessage(TAG, getResources().getString(R.string.CheckInternet));
                    AlertDialogBox(RegistrationActivity.this, getResources().getString(R.string.CheckInternet));
                }

            } catch (Exception e) {
                CommonUtils.LogMessage(TAG, "RegisterUser Exception: " + e.getMessage());
                CommonUtils.LogMessage(TAG, "RegisterUser :" + result, e);
            }

        }
    }



}
