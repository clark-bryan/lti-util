package com.trustwell.ltiutil;

import jakarta.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LTILaunchValidator {

    private static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
    private static final String OAUTH_TIMESTAMP_KEY = "oauth_timestamp";
    private static final String OAUTH_SIGNATURE_KEY = "oauth_signature";
    private static final String OAUTH_NONCE_KEY = "oauth_nonce";
    private static final String OAUTH_SIGNATURE_METHOD_KEY = "oauth_signature_method";
    private static final String OAUTH_VERSION_KEY = "oauth_version";

    private LTILaunchValidator() {
    }

    public static boolean validateLaunchRequest(HttpServletRequest request, String consumers) {

        Map<String, String> consumerPair = LTIUtils.getLtiConsumers(consumers);
        Map<String, String> authHeaders = LTIUtils.getAuthorizationHeaders(request);
        Map<String, String> requestParameters = LTIUtils.createRequestParametersMap(request);

        LTILaunchRequest launchRequest = new LTILaunchRequest(authHeaders, requestParameters, consumerPair, request);

        String key = authHeaders.get(OAUTH_CONSUMER_KEY);
        String secret = consumerPair.get(key);

        return validateOAuthSignature(launchRequest.getServletRequest(), secret);
    }

    private static boolean validateOAuthSignature(HttpServletRequest request, String secret) {
        Map<String, String> authorizationHeaders = LTIUtils.getAuthorizationHeaders(request);
        String baseString = generateBaseString(request, authorizationHeaders);
        log.debug("Generated OAuth base string: {}", baseString);
        return validateRequiredFields(authorizationHeaders)
                && validateTimeStampLessThanFiveMinutes(authorizationHeaders.get(OAUTH_TIMESTAMP_KEY))
                && validateSignature(authorizationHeaders.get(OAUTH_SIGNATURE_KEY), baseString, secret + "&", authorizationHeaders.get(OAUTH_SIGNATURE_METHOD_KEY));
    }

    private static boolean validateRequiredFields(Map<String, String> authorizationHeaders) {
        Set<String> requiredValues = Set.of(OAUTH_CONSUMER_KEY, OAUTH_NONCE_KEY, OAUTH_SIGNATURE_KEY, OAUTH_SIGNATURE_METHOD_KEY, OAUTH_TIMESTAMP_KEY, OAUTH_VERSION_KEY);
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

    private static String generateBaseString(HttpServletRequest request, Map<String, String> authorizationHeaders) {
        String httpMethod = request.getMethod();
        String baseUrl = normalizeUrl(request.getRequestURL().toString());

        Map<String, String> requestParameters = LTIUtils.createRequestParametersMap(request);
        requestParameters.putAll(authorizationHeaders);

        requestParameters = requestParameters.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals("realm"))
                .filter(entry -> !entry.getKey().equals("type"))
                .filter(entry -> !entry.getKey().equals(OAUTH_SIGNATURE_KEY))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        TreeMap<String, Object> sortedParameters = new TreeMap<>(requestParameters);

        StringBuilder parameterBuilder = new StringBuilder();

        sortedParameters.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode((String) entry.getValue(), StandardCharsets.UTF_8) + "&").forEachOrdered(parameterBuilder::append);

        String parameterString = parameterBuilder.toString();
        if (parameterString.length() > 0) {
            parameterString = parameterString.substring(0, parameterString.length() - 1);
        }

        return httpMethod.toUpperCase() + "&" + URLEncoder.encode(baseUrl, StandardCharsets.UTF_8) + "&" + URLEncoder.encode(parameterString, StandardCharsets.UTF_8);
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
}
