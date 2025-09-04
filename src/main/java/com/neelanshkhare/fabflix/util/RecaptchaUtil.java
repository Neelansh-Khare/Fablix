package com.neelanshkhare.fabflix.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

public class RecaptchaUtil {
    private static final Logger LOGGER = Logger.getLogger(RecaptchaUtil.class.getName());
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    // TODO: Replace with your actual secret key from Google reCAPTCHA console
    private static final String SECRET_KEY = "XYZ"; // Get this from https://www.google.com/recaptcha/admin

    // Site key for frontend (this should match what you use in auth.js)
    public static final String SITE_KEY = "CYZ"; // Get this from https://www.google.com/recaptcha/admin

    public static boolean verifyRecaptcha(String gRecaptchaResponse) {
        if (gRecaptchaResponse == null || gRecaptchaResponse.trim().isEmpty()) {
            LOGGER.warning("Empty reCAPTCHA response received");
            return false;
        }

        LOGGER.info("Verifying reCAPTCHA response: " + gRecaptchaResponse.substring(0, Math.min(20, gRecaptchaResponse.length())) + "...");

        try {
            URL url = new URL(RECAPTCHA_VERIFY_URL);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String postParams = "secret=" + SECRET_KEY + "&response=" + gRecaptchaResponse;
            LOGGER.info("Sending reCAPTCHA verification request");

            // Send request
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(postParams);
                wr.flush();
            }

            // Get response
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            // Parse JSON response
            JSONObject jsonResponse = new JSONObject(response.toString());
            boolean success = jsonResponse.getBoolean("success");

            if (success) {
                LOGGER.info("reCAPTCHA verification successful");
            } else {
                LOGGER.warning("reCAPTCHA verification failed");
                if (jsonResponse.has("error-codes")) {
                    LOGGER.warning("Error codes: " + jsonResponse.getJSONArray("error-codes").toString());
                }
                LOGGER.info("Full response: " + jsonResponse.toString());
            }

            return success;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error verifying reCAPTCHA", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during reCAPTCHA verification", e);
            return false;
        }
    }

    // Method to get the site key for frontend use
    public static String getSiteKey() {
        return SITE_KEY;
    }
}