package com.trustwell.ltiutil;

import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;

public enum MacAlgorithm {
    HMAC_MD5("HmacMD5", "HMAC-MD5"),
    HMAC_SHA1("HmacSHA1", "HMAC-SHA1"),
    HMAC_SHA224("HmacSHA224", "HMAC-SHA224"),
    HMAC_SHA256("HmacSHA256", "HMAC-SHA256"),
    HMAC_SHA384("HmacSHA384", "HMAC-SHA384"),
    HMAC_SHA512("HmacSHA512", "HMAC-SHA512");

    private final String algorithm;
    private final String signatureMethodString;

    MacAlgorithm(String algorithm, String signatureMethodString) {
        this.algorithm = algorithm;
        this.signatureMethodString = signatureMethodString;
    }

    public static String getAlgorithmFromSignatureMethodString(String signatureMethodString) {
        for (MacAlgorithm algorithm : MacAlgorithm.values()) {
            if (algorithm.signatureMethodString.equals(signatureMethodString)) {
                return algorithm.algorithm;
            }
        }
        return null;
    }

    public static boolean isValidAlgorithm(String signatureMethodString) {
        for (MacAlgorithm algorithm : MacAlgorithm.values()) {
            if (algorithm.signatureMethodString.equals(signatureMethodString)) {
                try {
                    Mac.getInstance(algorithm.algorithm);
                    return true;
                } catch (NoSuchAlgorithmException e) {
                    return false;
                }
            }
        }
        return false;
    }
}
