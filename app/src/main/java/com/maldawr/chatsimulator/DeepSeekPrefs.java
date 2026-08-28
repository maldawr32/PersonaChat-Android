package com.maldawr.chatsimulator;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class DeepSeekPrefs {
    private static final String PREFS = "personachat_deepseek";
    private static final String API_CT = "api_ct";
    private static final String API_IV = "api_iv";
    private static final String MODEL = "model";
    private static final String KEY_ALIAS = "personachat_deepseek_api_key";
    public static final String DEFAULT_MODEL = "deepseek-v4-pro";

    private DeepSeekPrefs() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getModel(Context c) {
        return prefs(c).getString(MODEL, DEFAULT_MODEL);
    }

    public static void setModel(Context c, String model) {
        prefs(c).edit().putString(MODEL, model == null || model.trim().isEmpty() ? DEFAULT_MODEL : model.trim()).apply();
    }

    public static boolean hasApiKey(Context c) {
        return !getApiKey(c).isEmpty();
    }

    public static void clearApiKey(Context c) {
        prefs(c).edit().remove(API_CT).remove(API_IV).apply();
    }

    public static boolean saveApiKey(Context c, String key) {
        try {
            String clean = key == null ? "" : key.trim();
            if (clean.isEmpty()) {
                clearApiKey(c);
                return true;
            }
            SecretKey secretKey = getOrCreateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(clean.getBytes(StandardCharsets.UTF_8));
            prefs(c).edit()
                    .putString(API_CT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString(API_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getApiKey(Context c) {
        try {
            String ct = prefs(c).getString(API_CT, "");
            String iv = prefs(c).getString(API_IV, "");
            if (ct.isEmpty() || iv.isEmpty()) return "";
            SecretKey secretKey = getOrCreateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey,
                    new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
            byte[] plain = cipher.doFinal(Base64.decode(ct, Base64.NO_WRAP));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            return entry.getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }
}
