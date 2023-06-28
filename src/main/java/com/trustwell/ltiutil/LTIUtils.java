package com.trustwell.ltiutil;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LTIUtils {

    static Map<String, String> createRequestParametersMap(HttpServletRequest request) {
        return request.getParameterMap().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()[0]));
    }

    public static String getParameterMapAsJsonString(Map<String, String> params) {
        JSONObject json = new JSONObject();
        params.forEach(json::put);
        return json.toString();
    }

    public static Map<String, String> getRequestParameters(HttpServletRequest request) {
        // initialize a map and add all the parameters in the request
        Map<String, String> requestParameters = new HashMap<>(request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()[0])));

        // add oauth request headers to parameter list
        String authorizationHeader = request.getHeader("authorization");
        if (authorizationHeader != null) {
            requestParameters.putAll(extractOauthAuthorizationValues(authorizationHeader));
        }

        return requestParameters;
    }

    private static Map<String, String> extractOauthAuthorizationValues(String authHeader) {
        // Remove "OAuth" prefix
        String oauthString = authHeader.replace("OAuth ", "");

        // Split the string into key-value pairs
        String[] pairs = oauthString.split(",");

        // Create a map to store the key-value pairs
        Map<String, String> oauthMap = new HashMap<>();

        // Iterate through the pairs and add them to the map
        for (String pair : pairs) {
            // Split the pair into key and value accounting for values that may have an = in them
            int equalsIndex = pair.indexOf('=');
            String key = pair.substring(0, equalsIndex);
            String value = pair.substring(equalsIndex + 1);

            // Remove leading and trailing quotes from the value
            value = value.replaceAll("^\"|\"$", "");

            // Add the key-value pair to the map
            oauthMap.put(key, value);
        }

        return oauthMap;
    }

    public static Map<String, String> getLtiConsumers(final String consumers) {
        Map<String, String> map = new HashMap<>();

        Optional.ofNullable(consumers)
                .ifPresent(c -> Arrays.stream(c.split(","))
                        .map(pair -> pair.replace(" ", ""))
                        .map(pair -> pair.split("="))
                        .forEach(keySecret -> {
                            if (keySecret.length < 2) {
                                throw new IllegalArgumentException("Missing consumer secret: " + Arrays.toString(keySecret));
                            }
                            map.put(keySecret[0], keySecret[1]);
                        }));
        return map;
    }
}
