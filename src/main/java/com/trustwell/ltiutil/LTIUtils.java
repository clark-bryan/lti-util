package com.trustwell.ltiutil;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class LTIUtils {

    private LTIUtils() {}

    public static Map<String, String> getAuthorizationHeaders(HttpServletRequest request) {
        Map<String, String> authHeaders = new HashMap<>();
        String authHeader = request.getHeader("authorization");

        if (authHeader != null) {
            String[] parts = authHeader.split(" ");
            int expectedPartsLength = 2;

            if (parts.length == expectedPartsLength) {
                int typeIndex = 0;
                int pairIndex = 1;

                authHeaders.put("type", parts[typeIndex]);
                String[] keyValuePairs = parts[pairIndex].split(",");

                for (String keyValuePair : keyValuePairs) {
                    int expectedPairLength = 2;
                    int keyIndex = 0;
                    int valueIndex = 1;

                    String[] pair = keyValuePair.split("=", 2);
                    if (pair.length == expectedPairLength) {
                        authHeaders.put(pair[keyIndex].trim(), pair[valueIndex].trim().replace("\"", ""));
                    }
                }
            }
        } else {
            authHeaders.putAll(getAuthHeadersFromParametersMap(request.getParameterMap()));
        }
        return authHeaders;
    }

    public static Map<String, String> getAuthHeadersFromParametersMap(Map<String, String[]> parameterMap) {
        return parameterMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith("oauth_"))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()[0]));
    }

    public static Map<String, String> createRequestParametersMap(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        return parameterMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()[0]));
    }

    public static String getParameterMapAsJsonString(Map<String, String> params) {
        JSONObject json = new JSONObject();
        params.forEach(json::put);
        return json.toString();
    }

    public static Map<String, String> getLtiConsumers(final String consumers) {
        HashMap<String, String> map = new HashMap<>();
        if (consumers != null) {
            log.debug("consumers: {}",  consumers);
            String[] pairs = consumers.split(",");
            for (String pair : pairs) {
                pair = pair.replace(" ", "");
                log.debug("pair: {}", pair);
                String[] keySecret = pair.split("=");
                if (keySecret.length < 2) {
                    log.debug("Rejecting invalid consumer key/secret combo: {}", pair);
                    throw new IllegalArgumentException("Missing consumer secret: "+pair);
                }
                map.put(keySecret[0], keySecret[1]);
            }
        }
        if (map.isEmpty()) {
            log.debug("No consumers were configured for this instance.");
        }
        return Collections.unmodifiableMap(map);
    }
}
