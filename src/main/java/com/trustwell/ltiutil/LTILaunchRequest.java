package com.trustwell.ltiutil;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public record LTILaunchRequest(HttpServletRequest request, Map<String, String> requestParameters) {
}
