package com.trustwell.ltiutil;

import jakarta.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LTILaunchValidator {

    private static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
    private static final String OAUTH_TIMESTAMP_KEY = "oauth_timestamp";
    private static final String OAUTH_SIGNATURE_KEY = "oauth_signature";
    private static final String OAUTH_NONCE_KEY = "oauth_nonce";
    private static final String OAUTH_SIGNATURE_METHOD_KEY = "oauth_signature_method";
    private static final String OAUTH_VERSION_KEY = "oauth_version";

    public static boolean validateLaunchRequest(LTILaunchRequest launchRequest, String secret) {
        return validateOAuthSignature(launchRequest, secret);
    }

    private static boolean validateOAuthSignature(LTILaunchRequest launchRequest, String secret) {
        HttpServletRequest request = launchRequest.request();
        String requestMethod = request.getMethod().toUpperCase();
        String requestUrl = normalizeUrl(request.getRequestURL().toString());
        Map<String, String> requestParameters = launchRequest.requestParameters();
        String requestSignature = requestParameters.get(OAUTH_SIGNATURE_KEY);

        String baseString = constructBaseString(requestMethod, requestUrl, requestParameters);

        log.debug("Generated base string from request: {}", baseString);

        return validateRequiredFields(requestParameters)
                && validateTimeStampLessThanFiveMinutes(requestParameters.get(OAUTH_TIMESTAMP_KEY))
                && validateSignature(requestSignature, baseString, secret + "&", requestParameters.get(OAUTH_SIGNATURE_METHOD_KEY));
    }

    private static String constructBaseString(String method, String url, Map<String, String> parameters) {
        parameters.remove("oauth_signature");
        String encodedParameters = encode(normalizeAndConcatenateParameters(parameters));
        String encodedMethod = encode(method);
        String encodedUrl = encode(url);

        return encodedMethod + "&" + encodedUrl + "&" + encodedParameters;
    }

    private static String normalizeAndConcatenateParameters(Map<String, String> parameters) {
        List<String> normalizedParameters = parameters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .toList();
        return String.join("&", normalizedParameters);
    }

    private static boolean validateRequiredFields(Map<String, String> authorizationHeaders) {
        Set<String> requiredValues = Set.of(OAUTH_CONSUMER_KEY, OAUTH_NONCE_KEY, OAUTH_SIGNATURE_METHOD_KEY, OAUTH_TIMESTAMP_KEY, OAUTH_VERSION_KEY);
        return authorizationHeaders.keySet().containsAll(requiredValues);
    }

    private static boolean validateTimeStampLessThanFiveMinutes(String timestampToValidate) {
        long currentTimeMillis = System.currentTimeMillis() / 1000;
        long timestampMillis = Long.parseLong(timestampToValidate);
        return (currentTimeMillis - timestampMillis) <= (5 * 60 * 1000);
    }

    private static boolean validateSignature(String signature, String message, String secret, String signatureMethod) {
        log.debug("Validating OAuth signature with values of: signature: {}, message {}, secret {}, signature method {}", signature, message, secret, signatureMethod);
        try {
            if (MacAlgorithm.isValidAlgorithm(signatureMethod)) {
                signatureMethod = MacAlgorithm.getAlgorithmFromSignatureMethodString(signatureMethod);
            } else {
                throw new NoSuchAlgorithmException();
            }

            assert signatureMethod != null;
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), signatureMethod);
            Mac mac = Mac.getInstance(signatureMethod);
            mac.init(keySpec);
            byte[] expectedSignature = Base64.getEncoder().encode(mac.doFinal(message.getBytes()));
            return MessageDigest.isEqual(expectedSignature, signature.getBytes());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }

    private static String normalizeUrl(String url) {
        try {
            URL u = new URL(url);
            if ((u.getProtocol().equals("http") && u.getPort() == 80) ||
                    (u.getProtocol().equals("https") && u.getPort() == 443)) {
                return u.getProtocol() + "://" + u.getHost() + u.getPath();
            } else {
                return url;
            }
        } catch (MalformedURLException e) {
            return "";
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }
}
