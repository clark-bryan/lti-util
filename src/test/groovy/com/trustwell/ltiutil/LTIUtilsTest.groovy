package com.trustwell.ltiutil

import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification

class LTIUtilsTest extends Specification {

    def "CreateRequestParametersMap should correctly create a request parameters map"() {
        given:
        Map<String, String[]> requestParameters = new HashMap<>()
        requestParameters.put("param1",new String[] {"value1"})
        requestParameters.put("param2",new String[] {"value2"})

        HttpServletRequest request = Stub(HttpServletRequest) {
            getParameterMap() >> requestParameters
        }

        when:
        Map<String, String> parameters = LTIUtils.createRequestParametersMap(request)

        then:
        parameters.size() == 2
        parameters.containsKey("param1")
        parameters.containsKey("param2")
        parameters.get("param1") == "value1"
        parameters.get("param2") == "value2"
    }

    def "GetParameterMapAsJsonString should convert the parameter map to a JSON string"() {
        given:
        Map<String, String> params = [ "key1": "value1", "key2": "value2" ]
        String expectedJsonString = "{\"key1\":\"value1\",\"key2\":\"value2\"}"

        when:
        String jsonString = LTIUtils.getParameterMapAsJsonString(params)

        then:
        jsonString == expectedJsonString
    }

    def "GetRequestParameters should correctly extract request parameters from HttpServletRequest"() {
        given:
        Map<String, String[]> requestParameters = new HashMap<>()
        requestParameters.put("param1",new String[] {"value1"})
        requestParameters.put("param2",new String[] {"value2"})

        Map<String, String> requestHeaders = [
                "oauth_token": "token123",
                "oauth_consumer_key": "consumerKey456",
                "other_header": "value"
        ]
        HttpServletRequest request = Stub(HttpServletRequest) {
            getParameterMap() >> requestParameters
            getHeaderNames() >> Collections.enumeration(requestHeaders.keySet())
            getHeader("authorization") >> "OAuth oauth_consumer_key=\"consumerKey456\",oauth_token=\"token123\""
        }

        when:
        Map<String, String> parameters = LTIUtils.getRequestParameters(request)

        then:
        parameters.size() == 4
        parameters.containsKey("param1")
        parameters.containsKey("param2")
        parameters.containsKey("oauth_token")
        parameters.containsKey("oauth_consumer_key")
        parameters.get("param1") == "value1"
        parameters.get("param2") == "value2"
        parameters.get("oauth_token") == "token123"
        parameters.get("oauth_consumer_key") == "consumerKey456"
        !parameters.containsKey("oauth_signature")
        !parameters.containsKey("other_header")
    }

    def "GetLtiConsumers"() {
        given:
        String consumerPairs = "customer-key=secret, second-key=second-secret"

        when:
        Map<String, String> returnedMap = LTIUtils.getLtiConsumers(consumerPairs)

        then:
        returnedMap.get("customer-key") == "secret"
        returnedMap.get("second-key") == "second-secret"
    }
}
